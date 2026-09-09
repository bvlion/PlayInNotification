package net.ambitious.android.playinnotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalculationGameService : Service() {
  private val handler = Handler(Looper.getMainLooper())
  private val gameStatisticsRepository by lazy { GameStatisticsRepository(applicationContext) }
  private val gameStatisticsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var sessionDeadline = 0L
  private var questionNumber = 0
  private var currentQuestion: CalculationQuestion? = null
  private var sessionResult = CalculationSessionResult()
  private var sessionWakeLock: PowerManager.WakeLock? = null
  private var isCompletingSession = false

  private val finishSession = Runnable {
    if (!isSessionActive || isCompletingSession) {
      return@Runnable
    }
    isCompletingSession = true
    currentQuestion = null
    latestCompletedSessionResult = sessionResult
    sessionWakeLock?.takeIf { it.isHeld }?.release()
    sessionWakeLock = null
    gameStatisticsScope.launch {
      gameStatisticsRepository.recordCompletedSession(
        sessionResult = sessionResult,
        gameGenre = CALCULATION_GAME_GENRE,
        difficulty = CALCULATION_LEVEL_ONE_DIFFICULTY,
        completedSessionDate = LocalDate.now(),
      )
      withContext(Dispatchers.Main) {
        isSessionActive = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        showGameSelection(this@CalculationGameService)
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> startSession()
      ACTION_ANSWER -> handleAnswer(intent)
    }
    return START_NOT_STICKY
  }

  override fun onDestroy() {
    handler.removeCallbacks(finishSession)
    isSessionActive = false
    sessionWakeLock?.takeIf { it.isHeld }?.release()
    sessionWakeLock = null
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun startSession() {
    handler.removeCallbacks(finishSession)
    sessionWakeLock?.takeIf { it.isHeld }?.release()
    sessionWakeLock = getSystemService(PowerManager::class.java).newWakeLock(
      PowerManager.PARTIAL_WAKE_LOCK,
      "$packageName:calculation-game",
    ).apply {
      setReferenceCounted(false)
      acquire(SESSION_DURATION_MILLISECONDS + WAKE_LOCK_TIMEOUT_MARGIN_MILLISECONDS)
    }
    isSessionActive = true
    sessionDeadline = SystemClock.elapsedRealtime() + SESSION_DURATION_MILLISECONDS
    questionNumber = 0
    sessionResult = CalculationSessionResult()
    isCompletingSession = false
    latestCompletedSessionResult = null
    showNextQuestion(isStartingForegroundService = true)
    handler.postDelayed(finishSession, SESSION_DURATION_MILLISECONDS)
  }

  private fun handleAnswer(intent: Intent) {
    val question = currentQuestion ?: return
    val answer = intent.getIntExtra(EXTRA_ANSWER, Int.MIN_VALUE)
    if (
      intent.getIntExtra(EXTRA_QUESTION_NUMBER, -1) != questionNumber ||
      answer !in question.choices
    ) {
      return
    }
    if (SystemClock.elapsedRealtime() >= sessionDeadline) {
      finishSession.run()
      return
    }

    sessionResult = sessionResult.addAnswerResult(
      CalculationAnswerResult.create(
        question = question,
        answer = answer,
        questionDifficulty = CALCULATION_LEVEL_ONE_DIFFICULTY,
      ),
    )
    showNextQuestion(isStartingForegroundService = false)
  }

  private fun showNextQuestion(isStartingForegroundService: Boolean) {
    questionNumber += 1
    currentQuestion = CalculationQuestion.create()
    val notification = createQuestionNotification(currentQuestion!!)
    if (isStartingForegroundService) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
        )
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
    } else {
      getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }
  }

  private fun createQuestionNotification(question: CalculationQuestion): Notification {
    val builder = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(
        getString(
          R.string.calculation_question,
          question.leftOperand,
          question.operator.symbol,
          question.rightOperand,
        ),
      )
      .setContentText(getString(R.string.calculation_time_remaining))
      .setWhen(System.currentTimeMillis() + (sessionDeadline - SystemClock.elapsedRealtime()))
      .setUsesChronometer(true)
      .setChronometerCountDown(true)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setCategory(Notification.CATEGORY_SERVICE)

    question.choices.forEachIndexed { index, choice ->
      val answerIntent = Intent(this, CalculationGameService::class.java)
        .setAction(ACTION_ANSWER)
        .putExtra(EXTRA_QUESTION_NUMBER, questionNumber)
        .putExtra(EXTRA_ANSWER, choice)
      val answerPendingIntent = PendingIntent.getService(
        this,
        questionNumber * 3 + index,
        answerIntent,
        PendingIntent.FLAG_CANCEL_CURRENT or
          PendingIntent.FLAG_ONE_SHOT or
          PendingIntent.FLAG_IMMUTABLE,
      )
      builder.addAction(Notification.Action.Builder(null, choice.toString(), answerPendingIntent).build())
    }

    return builder.build()
  }

  companion object {
    private const val NOTIFICATION_CHANNEL_ID = "game_notifications"
    private const val NOTIFICATION_ID = 1
    private const val ACTION_START =
      "net.ambitious.android.playinnotification.action.START_CALCULATION_LEVEL_ONE"
    private const val ACTION_ANSWER =
      "net.ambitious.android.playinnotification.action.ANSWER_CALCULATION"
    private const val EXTRA_QUESTION_NUMBER = "question_number"
    private const val EXTRA_ANSWER = "answer"
    private const val SESSION_DURATION_MILLISECONDS = 30_000L
    private const val WAKE_LOCK_TIMEOUT_MARGIN_MILLISECONDS = 1_000L
    private const val CALCULATION_GAME_GENRE = "calculation"
    private const val CALCULATION_LEVEL_ONE_DIFFICULTY = 1

    @Volatile
    var isSessionActive = false
      private set

    @Volatile
    internal var latestCompletedSessionResult: CalculationSessionResult? = null
      private set

    fun createNotificationChannel(context: Context) {
      val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        context.getString(R.string.game_notification_channel_name),
        NotificationManager.IMPORTANCE_LOW,
      ).apply {
        setSound(null, null)
        enableVibration(false)
      }
      context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showGameSelection(context: Context) {
      createNotificationChannel(context)
      val startIntent = Intent(context, CalculationGameService::class.java)
        .setAction(ACTION_START)
      val startPendingIntent = PendingIntent.getForegroundService(
        context,
        0,
        startIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
      val notification = Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(context.getString(R.string.game_selection_title))
        .setContentText(context.getString(R.string.calculation_level_one_description))
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .addAction(
          Notification.Action.Builder(
            null,
            context.getString(R.string.calculation_level_one),
            startPendingIntent,
          ).build(),
        )
        .build()
      context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }
  }
}
