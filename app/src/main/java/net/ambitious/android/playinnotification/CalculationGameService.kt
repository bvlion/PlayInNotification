package net.ambitious.android.playinnotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalculationGameService : Service() {
  private val handler = Handler(Looper.getMainLooper())
  private val gameStatisticsRepository by lazy { GameStatisticsRepository(applicationContext) }
  private val gameStatisticsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var sessionDeadline = 0L
  private var sessionDifficulty = INITIAL_DIFFICULTY
  private var questionNumber = 0
  private var currentQuestion: CalculationQuestion? = null
  private var sessionResult = GameSessionResult()
  private var sessionWakeLock: PowerManager.WakeLock? = null
  private var isCompletingSession = false

  private val finishSession = Runnable {
    if (!isSessionActive || isCompletingSession) {
      return@Runnable
    }
    isCompletingSession = true
    currentQuestion = null
    val completedSessionResult = sessionResult
    latestCompletedSessionResult = completedSessionResult
    val resultSummary = getString(
      R.string.session_result_summary,
      completedSessionResult.answerCount,
      completedSessionResult.correctAnswerCount,
      completedSessionResult.incorrectAnswerCount,
    )
    val notificationManager = getSystemService(NotificationManager::class.java)
    notificationManager.notify(
      NOTIFICATION_ID,
      Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(getString(R.string.session_finished_title))
        .setContentText(resultSummary)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(Notification.CATEGORY_STATUS)
        .setStyle(Notification.BigTextStyle().bigText(resultSummary))
        .build(),
    )
    sessionWakeLock?.takeIf { it.isHeld }?.release()
    sessionWakeLock = null
    gameStatisticsScope.launch {
      val (previousStatistics, updatedStatistics) = gameStatisticsRepository.recordCompletedSession(
        sessionResult = completedSessionResult,
        gameGenre = CALCULATION_GAME_GENRE,
        difficulty = sessionDifficulty,
        completedSessionDate = LocalDate.now(),
      )
      withContext(Dispatchers.Main) {
        val viewAnswersIntent = Intent(
          this@CalculationGameService,
          SessionAnswersActivity::class.java,
        )
          .putStringArrayListExtra(
            SessionAnswersActivity.EXTRA_QUESTIONS,
            ArrayList(completedSessionResult.answerResults.map { it.question }),
          )
          .putStringArrayListExtra(
            SessionAnswersActivity.EXTRA_SELECTED_ANSWERS,
            ArrayList(completedSessionResult.answerResults.map { it.selectedAnswer }),
          )
          .putExtra(
            SessionAnswersActivity.EXTRA_CORRECTNESS,
            completedSessionResult.answerResults.map { it.isCorrect }.toBooleanArray(),
          )
        val viewAnswersPendingIntent = PendingIntent.getActivity(
          this@CalculationGameService,
          0,
          viewAnswersIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val showGameSelectionPendingIntent = PendingIntent.getBroadcast(
          this@CalculationGameService,
          0,
          Intent(this@CalculationGameService, GameNotificationActionReceiver::class.java)
            .setAction(GameNotificationActionReceiver.ACTION_SHOW_GAME_SELECTION),
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val previousLevel = previousStatistics.growthLevel
        val updatedLevel = updatedStatistics.growthLevel
        val resultNotificationBuilder = Notification.Builder(
          this@CalculationGameService,
          NOTIFICATION_CHANNEL_ID,
        )
          .setSmallIcon(R.drawable.ic_launcher_foreground)
          .setContentTitle(getString(R.string.session_finished_title))
          .setContentText(resultSummary)
          .setOngoing(true)
          .setOnlyAlertOnce(true)
          .setCategory(Notification.CATEGORY_STATUS)
          .addExtras(
            Bundle().apply {
              putBoolean(EXTRA_IS_RESULT_NOTIFICATION, true)
            },
          )
          .addAction(
            Notification.Action.Builder(
              null,
              getString(R.string.view_session_answers),
              viewAnswersPendingIntent,
            ).build(),
          )
          .addAction(
            Notification.Action.Builder(
              null,
              getString(R.string.choose_game),
              showGameSelectionPendingIntent,
            ).build(),
          )
        if (updatedLevel > previousLevel) {
          val levelChange = getString(R.string.level_change, previousLevel, updatedLevel)
          val levelUpImage = Bitmap.createBitmap(1024, 512, Bitmap.Config.ARGB_8888)
          val canvas = Canvas(levelUpImage)
          val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          }
          canvas.drawColor(getColor(android.R.color.system_accent1_700))
          paint.color = getColor(android.R.color.system_accent1_50)
          paint.textSize = 72f
          canvas.drawText(getString(R.string.level_up), 512f, 190f, paint)
          paint.textSize = 104f
          canvas.drawText(levelChange, 512f, 350f, paint)
          resultNotificationBuilder
            .setStyle(
              Notification.BigPictureStyle()
                .bigPicture(levelUpImage)
                .setSummaryText(resultSummary),
            )
        } else {
          resultNotificationBuilder.setStyle(Notification.BigTextStyle().bigText(resultSummary))
        }
        notificationManager.notify(
          NOTIFICATION_ID,
          resultNotificationBuilder.build(),
        )
        isSessionActive = false
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> intent.getIntExtra(EXTRA_DIFFICULTY, INVALID_DIFFICULTY)
        .takeIf { it in DIFFICULTY_RANGE }
        ?.let(::startSession)
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

  private fun startSession(difficulty: Int) {
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
    sessionDifficulty = difficulty
    sessionDeadline = SystemClock.elapsedRealtime() + SESSION_DURATION_MILLISECONDS
    questionNumber = 0
    currentQuestion = null
    sessionResult = GameSessionResult()
    isCompletingSession = false
    latestCompletedSessionResult = null
    handler.postDelayed(
      finishSession,
      maxOf(0L, sessionDeadline - SystemClock.elapsedRealtime()),
    )
    showNextQuestion(isStartingForegroundService = true)
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

    val isCorrect = answer == question.correctAnswer
    val questionText = if (
      question.missingOperandIndex != null &&
      question.thirdOperand != null &&
      question.secondOperator != null
    ) {
      when (question.missingOperandIndex) {
        0 -> getString(
          R.string.calculation_missing_left_operand_question,
          question.operator.symbol,
          question.rightOperand,
          question.secondOperator.symbol,
          question.thirdOperand,
          question.calculationResult,
        )
        1 -> getString(
          R.string.calculation_missing_right_operand_question,
          question.leftOperand,
          question.operator.symbol,
          question.secondOperator.symbol,
          question.thirdOperand,
          question.calculationResult,
        )
        else -> getString(
          R.string.calculation_missing_third_operand_question,
          question.leftOperand,
          question.operator.symbol,
          question.rightOperand,
          question.secondOperator.symbol,
          question.calculationResult,
        )
      }
    } else if (question.thirdOperand == null || question.secondOperator == null) {
      getString(
        R.string.calculation_question,
        question.leftOperand,
        question.operator.symbol,
        question.rightOperand,
      )
    } else {
      getString(
        R.string.calculation_compound_question,
        question.leftOperand,
        question.operator.symbol,
        question.rightOperand,
        question.secondOperator.symbol,
        question.thirdOperand,
      )
    }
    sessionResult = sessionResult.addAnswerResult(
      GameAnswerResult.create(
        question = questionText,
        selectedAnswer = answer.toString(),
        isCorrect = isCorrect,
        questionDifficulty = sessionDifficulty,
      ),
    )
    showNextQuestion(isStartingForegroundService = false)
  }

  private fun showNextQuestion(isStartingForegroundService: Boolean) {
    questionNumber += 1
    currentQuestion = CalculationQuestion.create(
      difficulty = sessionDifficulty,
      previousQuestion = currentQuestion,
    )
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
        if (
          question.missingOperandIndex != null &&
          question.thirdOperand != null &&
          question.secondOperator != null
        ) {
          when (question.missingOperandIndex) {
            0 -> getString(
              R.string.calculation_missing_left_operand_question,
              question.operator.symbol,
              question.rightOperand,
              question.secondOperator.symbol,
              question.thirdOperand,
              question.calculationResult,
            )
            1 -> getString(
              R.string.calculation_missing_right_operand_question,
              question.leftOperand,
              question.operator.symbol,
              question.secondOperator.symbol,
              question.thirdOperand,
              question.calculationResult,
            )
            else -> getString(
              R.string.calculation_missing_third_operand_question,
              question.leftOperand,
              question.operator.symbol,
              question.rightOperand,
              question.secondOperator.symbol,
              question.calculationResult,
            )
          }
        } else if (question.thirdOperand == null || question.secondOperator == null) {
          getString(
            R.string.calculation_question,
            question.leftOperand,
            question.operator.symbol,
            question.rightOperand,
          )
        } else {
          getString(
            R.string.calculation_compound_question,
            question.leftOperand,
            question.operator.symbol,
            question.rightOperand,
            question.secondOperator.symbol,
            question.thirdOperand,
          )
        },
      )
      .setWhen(
        System.currentTimeMillis() +
          maxOf(0L, sessionDeadline - SystemClock.elapsedRealtime()),
      )
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
      builder.addAction(
        Notification.Action.Builder(null, choice.toString(), answerPendingIntent).build(),
      )
    }

    return builder.build()
  }

  companion object {
    internal const val NOTIFICATION_CHANNEL_ID = "game_notifications"
    private const val NOTIFICATION_ID = 1
    private const val ACTION_START =
      "net.ambitious.android.playinnotification.action.START_CALCULATION"
    private const val ACTION_ANSWER =
      "net.ambitious.android.playinnotification.action.ANSWER_CALCULATION"
    private const val EXTRA_DIFFICULTY = "difficulty"
    private const val EXTRA_QUESTION_NUMBER = "question_number"
    private const val EXTRA_ANSWER = "answer"
    internal const val EXTRA_IS_RESULT_NOTIFICATION = "is_result_notification"
    private const val SESSION_DURATION_MILLISECONDS = 30_000L
    private const val WAKE_LOCK_TIMEOUT_MARGIN_MILLISECONDS = 1_000L
    private const val CALCULATION_GAME_GENRE = "calculation"
    private const val INITIAL_DIFFICULTY = 1
    private const val INVALID_DIFFICULTY = 0
    private val DIFFICULTY_RANGE = 1..5

    @Volatile
    var isSessionActive = false
      private set

    @Volatile
    internal var latestCompletedSessionResult: GameSessionResult? = null
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

    suspend fun showGameSelection(
      context: Context,
      shouldReplaceResult: Boolean = false,
    ) {
      createNotificationChannel(context)
      val notificationManager = context.getSystemService(NotificationManager::class.java)
      val isResultNotificationShowing = notificationManager.activeNotifications.any {
        it.id == NOTIFICATION_ID &&
          it.notification.extras.getBoolean(EXTRA_IS_RESULT_NOTIFICATION)
      }
      if (
        CalculationGameService.isSessionActive ||
        DifficultKanjiGameService.isSessionActive ||
        (isResultNotificationShowing && !shouldReplaceResult) ||
        !notificationManager.areNotificationsEnabled() ||
        notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID).importance ==
        NotificationManager.IMPORTANCE_NONE
      ) {
        return
      }
      val gameDifficultySettings = GameDifficultySettingsRepository(context)
        .gameDifficultySettings
        .first()
      val startCalculationIntent = Intent(context, CalculationGameService::class.java)
        .setAction(ACTION_START)
        .putExtra(EXTRA_DIFFICULTY, gameDifficultySettings.calculationDifficulty)
      val startCalculationPendingIntent = PendingIntent.getForegroundService(
        context,
        0,
        startCalculationIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
      val startDifficultKanjiPendingIntent = PendingIntent.getForegroundService(
        context,
        0,
        DifficultKanjiGameService.createStartIntent(
          context,
          gameDifficultySettings.difficultKanjiDifficulty,
        ),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
      val notification = Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(context.getString(R.string.game_selection_title))
        .setContentText(context.getString(R.string.game_selection_description))
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .addAction(
          Notification.Action.Builder(
            null,
            context.getString(R.string.calculation_game),
            startCalculationPendingIntent,
          ).build(),
        )
        .addAction(
          Notification.Action.Builder(
            null,
            context.getString(R.string.difficult_kanji_game),
            startDifficultKanjiPendingIntent,
          ).build(),
        )
        .build()
      if (!CalculationGameService.isSessionActive && !DifficultKanjiGameService.isSessionActive) {
        notificationManager.notify(NOTIFICATION_ID, notification)
      }
    }
  }
}
