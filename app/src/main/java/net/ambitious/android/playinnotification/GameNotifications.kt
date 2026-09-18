package net.ambitious.android.playinnotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import kotlinx.coroutines.flow.first

internal object GameNotifications {
  const val CHANNEL_ID = "game_notifications"
  const val NOTIFICATION_ID = 1
  private const val EXTRA_IS_RESULT_NOTIFICATION = "is_result_notification"

  fun createChannel(context: Context) {
    val channel = NotificationChannel(
      CHANNEL_ID,
      context.getString(R.string.game_notification_channel_name),
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      setSound(null, null)
      enableVibration(false)
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  }

  fun isEnabled(context: Context): Boolean {
    createChannel(context)
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    return notificationManager.areNotificationsEnabled() &&
      notificationManager.getNotificationChannel(CHANNEL_ID).importance !=
      NotificationManager.IMPORTANCE_NONE
  }

  suspend fun showGameSelection(
    context: Context,
    shouldReplaceResult: Boolean = false,
  ) {
    createChannel(context)
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val isResultNotificationShowing = notificationManager.activeNotifications.any {
      it.id == NOTIFICATION_ID &&
        it.notification.extras.getBoolean(EXTRA_IS_RESULT_NOTIFICATION)
    }
    if (
      ActiveGameSessions.hasActiveSession() ||
      (isResultNotificationShowing && !shouldReplaceResult) ||
      !isEnabled(context)
    ) {
      return
    }

    val difficultySettings = GameDifficultySettingsRepository(context)
      .gameDifficultySettings
      .first()
    val startCalculationPendingIntent = PendingIntent.getForegroundService(
      context,
      0,
      GameSessionService.createStartIntent(
        context = context,
        serviceClass = CalculationGameService::class.java,
        difficulty = difficultySettings.calculationDifficulty,
      ),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val startDifficultKanjiPendingIntent = PendingIntent.getForegroundService(
      context,
      0,
      GameSessionService.createStartIntent(
        context = context,
        serviceClass = DifficultKanjiGameService::class.java,
        difficulty = difficultySettings.difficultKanjiDifficulty,
      ),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = Notification.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(context.getString(R.string.game_selection_title))
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
    if (!ActiveGameSessions.hasActiveSession()) {
      notificationManager.notify(NOTIFICATION_ID, notification)
    }
  }

  fun createQuestionNotification(
    context: Context,
    title: String,
    remainingSeconds: Long,
    answerActions: List<Pair<String, PendingIntent>>,
  ): Notification {
    return Notification.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(title)
      .setSubText(context.getString(R.string.game_time_remaining, remainingSeconds))
      .setShowWhen(false)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setCategory(Notification.CATEGORY_SERVICE)
      .apply {
        answerActions.forEach { (answer, pendingIntent) ->
          addAction(Notification.Action.Builder(null, answer, pendingIntent).build())
        }
      }
      .build()
  }

  fun updateRemainingTime(
    context: Context,
    notification: Notification,
    remainingSeconds: Long,
  ): Notification {
    return Notification.Builder.recoverBuilder(context, notification)
      .setSubText(context.getString(R.string.game_time_remaining, remainingSeconds))
      .build()
  }

  fun showSessionCompletionStarted(context: Context, sessionResult: GameSessionResult) {
    val resultSummary = sessionResult.summary(context)
    notify(
      context,
      Notification.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(context.getString(R.string.session_finished_title))
        .setContentText(resultSummary)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(Notification.CATEGORY_STATUS)
        .setStyle(Notification.BigTextStyle().bigText(resultSummary))
        .build(),
    )
  }

  fun showCompletedSession(
    context: Context,
    sessionResult: GameSessionResult,
    previousStatistics: GameStatistics,
    updatedStatistics: GameStatistics,
  ) {
    val viewAnswersPendingIntent = PendingIntent.getActivity(
      context,
      0,
      SessionAnswersActivity.createIntent(context, sessionResult.answerResults),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val showGameSelectionPendingIntent = PendingIntent.getBroadcast(
      context,
      0,
      Intent(context, GameNotificationActionReceiver::class.java)
        .setAction(GameNotificationActionReceiver.ACTION_SHOW_GAME_SELECTION),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val resultSummary = sessionResult.summary(context)
    val previousLevel = previousStatistics.growthLevel
    val updatedLevel = updatedStatistics.growthLevel
    val notificationBuilder = Notification.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(context.getString(R.string.session_finished_title))
      .setContentText(resultSummary)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setCategory(Notification.CATEGORY_STATUS)
      .addExtras(Bundle().apply { putBoolean(EXTRA_IS_RESULT_NOTIFICATION, true) })
      .addAction(
        Notification.Action.Builder(
          null,
          context.getString(R.string.view_session_answers),
          viewAnswersPendingIntent,
        ).build(),
      )
      .addAction(
        Notification.Action.Builder(
          null,
          context.getString(R.string.choose_game),
          showGameSelectionPendingIntent,
        ).build(),
      )
    if (updatedLevel > previousLevel) {
      val levelChange = context.getString(R.string.level_change, previousLevel, updatedLevel)
      notificationBuilder.setStyle(
        Notification.BigPictureStyle()
          .bigPicture(createLevelUpImage(context, levelChange))
          .setContentDescription(
            context.getString(R.string.level_up_with_change, previousLevel, updatedLevel),
          )
          .setSummaryText(resultSummary),
      )
    } else {
      notificationBuilder.setStyle(Notification.BigTextStyle().bigText(resultSummary))
    }
    notify(context, notificationBuilder.build())
  }

  fun notify(context: Context, notification: Notification) {
    context.getSystemService(NotificationManager::class.java).notify(
      NOTIFICATION_ID,
      notification,
    )
  }

  private fun GameSessionResult.summary(context: Context): String = context.getString(
    R.string.session_result_summary,
    answerCount,
    correctAnswerCount,
    incorrectAnswerCount,
  )

  private fun createLevelUpImage(context: Context, levelChange: String): Bitmap {
    val levelUpImage = Bitmap.createBitmap(1024, 512, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(levelUpImage)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textAlign = Paint.Align.CENTER
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawColor(context.getColor(android.R.color.system_accent1_700))
    paint.color = context.getColor(android.R.color.system_accent1_50)
    paint.textSize = 72f
    canvas.drawText(context.getString(R.string.level_up), 512f, 190f, paint)
    paint.textSize = 104f
    canvas.drawText(levelChange, 512f, 350f, paint)
    return levelUpImage
  }
}
