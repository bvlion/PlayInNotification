package net.ambitious.android.playinnotification

import android.app.Notification
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

private data class GameSessionState<Question>(
  val difficulty: GameDifficulty,
  val deadline: Long,
  val questionNumber: Int = 0,
  val askedQuestions: List<Question> = emptyList(),
  val result: GameSessionResult = GameSessionResult(),
  val questionNotification: Notification? = null,
  val isCompleting: Boolean = false,
)

internal object ActiveGameSessions {
  private val lock = Any()
  private val gameTypes = mutableSetOf<GameType>()

  fun activate(gameType: GameType) {
    synchronized(lock) {
      gameTypes += gameType
    }
  }

  fun deactivate(gameType: GameType) {
    synchronized(lock) {
      gameTypes -= gameType
    }
  }

  fun hasActiveSession(): Boolean = synchronized(lock) {
    gameTypes.isNotEmpty()
  }
}

internal abstract class GameSessionService<Question> : Service() {
  protected abstract val gameType: GameType
  protected abstract val wakeLockName: String

  private val handler = Handler(Looper.getMainLooper())
  private val gameStatisticsRepository by lazy { GameStatisticsRepository(applicationContext) }
  private val gameStatisticsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var sessionState: GameSessionState<Question>? = null
  private var sessionWakeLock: PowerManager.WakeLock? = null

  private val updateCountdownNotification = object : Runnable {
    override fun run() {
      val state = sessionState?.takeUnless { it.isCompleting } ?: return
      val questionNotification = state.questionNotification ?: return
      val remainingMilliseconds = remainingMilliseconds(state)
      val updatedNotification = GameNotifications.updateRemainingTime(
        context = this@GameSessionService,
        notification = questionNotification,
        remainingSeconds = remainingSecondsRoundedUp(remainingMilliseconds),
      )
      sessionState = state.copy(questionNotification = updatedNotification)
      GameNotifications.notify(this@GameSessionService, updatedNotification)
      if (remainingMilliseconds > 0L) {
        handler.postDelayed(this, minOf(COUNTDOWN_UPDATE_INTERVAL_MILLISECONDS, remainingMilliseconds))
      }
    }
  }

  private val finishSession = Runnable {
    val state = sessionState?.takeUnless { it.isCompleting } ?: return@Runnable
    sessionState = state.copy(
      questionNotification = null,
      isCompleting = true,
    )
    handler.removeCallbacks(updateCountdownNotification)
    GameNotifications.showSessionCompletionStarted(this, state.result)
    releaseWakeLock()
    gameStatisticsScope.launch {
      val (previousStatistics, updatedStatistics) =
        gameStatisticsRepository.recordCompletedSession(
          sessionResult = state.result,
          gameType = gameType,
          completedSessionDate = LocalDate.now(),
        )
      withContext(Dispatchers.Main) {
        GameNotifications.showCompletedSession(
          context = this@GameSessionService,
          sessionResult = state.result,
          previousStatistics = previousStatistics,
          updatedStatistics = updatedStatistics,
        )
        sessionState = null
        ActiveGameSessions.deactivate(gameType)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
      }
    }
  }

  final override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> GameDifficulty.fromLevelOrNull(
        intent.getIntExtra(EXTRA_DIFFICULTY, INVALID_DIFFICULTY_LEVEL),
      )?.let(::startSession)
      ACTION_ANSWER -> handleAnswer(intent)
    }
    return START_NOT_STICKY
  }

  final override fun onDestroy() {
    handler.removeCallbacks(finishSession)
    handler.removeCallbacks(updateCountdownNotification)
    sessionState = null
    ActiveGameSessions.deactivate(gameType)
    releaseWakeLock()
    super.onDestroy()
  }

  final override fun onBind(intent: Intent?): IBinder? = null

  protected abstract fun createNextQuestion(
    difficulty: GameDifficulty,
    askedQuestions: List<Question>,
  ): Question

  protected abstract fun questionText(question: Question): String

  protected abstract fun answerChoices(question: Question): List<String>

  protected abstract fun correctAnswer(question: Question): String

  private fun startSession(difficulty: GameDifficulty) {
    handler.removeCallbacks(finishSession)
    handler.removeCallbacks(updateCountdownNotification)
    releaseWakeLock()
    sessionWakeLock = getSystemService(PowerManager::class.java).newWakeLock(
      PowerManager.PARTIAL_WAKE_LOCK,
      "$packageName:$wakeLockName",
    ).apply {
      setReferenceCounted(false)
      acquire(SESSION_DURATION_MILLISECONDS + WAKE_LOCK_TIMEOUT_MARGIN_MILLISECONDS)
    }
    GameNotifications.createChannel(this)
    ActiveGameSessions.activate(gameType)
    sessionState = GameSessionState(
      difficulty = difficulty,
      deadline = SystemClock.elapsedRealtime() + SESSION_DURATION_MILLISECONDS,
    )
    handler.postDelayed(finishSession, SESSION_DURATION_MILLISECONDS)
    showNextQuestion(isStartingForegroundService = true)
    handler.post(updateCountdownNotification)
  }

  private fun handleAnswer(intent: Intent) {
    val state = sessionState?.takeUnless { it.isCompleting } ?: return
    val question = state.askedQuestions.lastOrNull() ?: return
    val answer = intent.getStringExtra(EXTRA_ANSWER) ?: return
    if (
      intent.getIntExtra(EXTRA_QUESTION_NUMBER, INVALID_QUESTION_NUMBER) != state.questionNumber ||
      answer !in answerChoices(question)
    ) {
      return
    }
    if (SystemClock.elapsedRealtime() >= state.deadline) {
      finishSession.run()
      return
    }

    sessionState = state.copy(
      result = state.result.addAnswerResult(
        GameAnswerResult.create(
          question = questionText(question),
          selectedAnswer = answer,
          isCorrect = answer == correctAnswer(question),
          questionDifficulty = state.difficulty,
        ),
      ),
    )
    showNextQuestion(isStartingForegroundService = false)
  }

  private fun showNextQuestion(isStartingForegroundService: Boolean) {
    val state = checkNotNull(sessionState)
    val questionNumber = state.questionNumber + 1
    val question = createNextQuestion(state.difficulty, state.askedQuestions)
    val firstAnswerRequestCode = questionNumber * ANSWER_CHOICE_COUNT
    val answerActions = answerChoices(question).mapIndexed { index, answer ->
      val answerIntent = Intent(this, javaClass)
        .setAction(ACTION_ANSWER)
        .putExtra(EXTRA_QUESTION_NUMBER, questionNumber)
        .putExtra(EXTRA_ANSWER, answer)
      val answerPendingIntent = PendingIntent.getService(
        this,
        firstAnswerRequestCode + index,
        answerIntent,
        PendingIntent.FLAG_CANCEL_CURRENT or
          PendingIntent.FLAG_ONE_SHOT or
          PendingIntent.FLAG_IMMUTABLE,
      )
      answer to answerPendingIntent
    }
    val notification = GameNotifications.createQuestionNotification(
      context = this,
      title = questionText(question),
      remainingSeconds = remainingSecondsRoundedUp(remainingMilliseconds(state)),
      answerActions = answerActions,
    )
    sessionState = state.copy(
      questionNumber = questionNumber,
      askedQuestions = state.askedQuestions + question,
      questionNotification = notification,
    )
    if (isStartingForegroundService) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(
          GameNotifications.NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
        )
      } else {
        startForeground(GameNotifications.NOTIFICATION_ID, notification)
      }
    } else {
      GameNotifications.notify(this, notification)
    }
  }

  private fun remainingMilliseconds(state: GameSessionState<Question>): Long =
    maxOf(0L, state.deadline - SystemClock.elapsedRealtime())

  private fun remainingSecondsRoundedUp(remainingMilliseconds: Long): Long =
    (remainingMilliseconds + MILLISECONDS_TO_ROUND_UP_TO_NEXT_SECOND) /
      MILLISECONDS_PER_SECOND

  private fun releaseWakeLock() {
    sessionWakeLock?.takeIf { it.isHeld }?.release()
    sessionWakeLock = null
  }

  companion object {
    private const val ACTION_START =
      "net.ambitious.android.playinnotification.action.START_GAME"
    private const val ACTION_ANSWER =
      "net.ambitious.android.playinnotification.action.ANSWER_GAME"
    private const val EXTRA_DIFFICULTY = "difficulty"
    private const val EXTRA_QUESTION_NUMBER = "question_number"
    private const val EXTRA_ANSWER = "answer"
    private const val INVALID_DIFFICULTY_LEVEL = 0
    private const val INVALID_QUESTION_NUMBER = -1
    private const val SESSION_DURATION_MILLISECONDS = 30_000L
    private const val WAKE_LOCK_TIMEOUT_MARGIN_MILLISECONDS = 1_000L
    private const val COUNTDOWN_UPDATE_INTERVAL_MILLISECONDS = 1_000L
    private const val MILLISECONDS_PER_SECOND = 1_000L
    private const val MILLISECONDS_TO_ROUND_UP_TO_NEXT_SECOND = MILLISECONDS_PER_SECOND - 1L
    private const val ANSWER_CHOICE_COUNT = 3

    internal fun createStartIntent(
      context: Context,
      serviceClass: Class<out GameSessionService<*>>,
      difficulty: GameDifficulty,
    ): Intent = Intent(context, serviceClass)
      .setAction(ACTION_START)
      .putExtra(EXTRA_DIFFICULTY, difficulty.level)
  }
}
