package net.ambitious.android.playinnotification

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class SessionAnswersActivityTest {
  @Test
  fun `今回の回答画面は最前面での再起動時に積み増さない`() {
    val application = RuntimeEnvironment.getApplication()

    val activityInfo = application.packageManager.getActivityInfo(
      ComponentName(application, SessionAnswersActivity::class.java),
      0,
    )

    assertEquals(ActivityInfo.LAUNCH_SINGLE_TOP, activityInfo.launchMode)
  }

  @Test
  fun `今回の回答画面の初回起動と最前面での再起動は通知切り替えを要求する`() {
    val application = RuntimeEnvironment.getApplication()
    val broadcastIntents = shadowOf(application).broadcastIntents
    val initialBroadcastCount = broadcastIntents.size
    val activityController = Robolectric.buildActivity(
      SessionAnswersActivity::class.java,
      Intent(application, SessionAnswersActivity::class.java),
    ).create()

    try {
      activityController.newIntent(Intent(application, SessionAnswersActivity::class.java))
      val broadcastIntentsAfterOpening = broadcastIntents.drop(initialBroadcastCount)

      assertEquals(2, broadcastIntentsAfterOpening.size)
      broadcastIntentsAfterOpening.forEach { broadcastIntent ->
        assertEquals(
          ComponentName(application, GameNotificationActionReceiver::class.java),
          broadcastIntent.component,
        )
        assertEquals(
          GameNotificationActionReceiver.ACTION_SHOW_GAME_SELECTION,
          broadcastIntent.action,
        )
      }
    } finally {
      activityController.destroy()
    }
  }

  @Test
  fun `最前面で新しいセッションの回答を開き再生成しても回答内容を維持する`() {
    val application = RuntimeEnvironment.getApplication()
    val previousAnswers = listOf(answerResult("1 + 1 = ?", "2", isCorrect = true))
    val activityController = Robolectric.buildActivity(
      SessionAnswersActivity::class.java,
      SessionAnswersActivity.createIntent(application, previousAnswers),
    ).setup()

    try {
      assertEquals(previousAnswers, activityController.get().answers)

      val newAnswers = listOf(
        answerResult("「海星」の読みは？", "ひとで", isCorrect = true),
        answerResult("「海月」の読みは？", "なまこ", isCorrect = false),
      )
      activityController.newIntent(SessionAnswersActivity.createIntent(application, newAnswers))

      assertEquals(newAnswers, activityController.get().answers)

      activityController.recreate()

      assertEquals(newAnswers, activityController.get().answers)
    } finally {
      activityController.destroy()
    }
  }

  @Test
  fun `今回の回答画面の再生成だけでは新しい結果通知を置き換えない`() {
    val application = RuntimeEnvironment.getApplication()
    val broadcastIntents = shadowOf(application).broadcastIntents
    val initialBroadcastCount = broadcastIntents.size
    val activityController = Robolectric.buildActivity(
      SessionAnswersActivity::class.java,
      SessionAnswersActivity.createIntent(application, emptyList()),
    ).setup()

    try {
      assertEquals(1, broadcastIntents.size - initialBroadcastCount)
      GameNotifications.createChannel(application)
      GameNotifications.showCompletedSession(
        context = application,
        sessionResult = GameSessionResult(),
        previousStatistics = GameStatistics(),
        updatedStatistics = GameStatistics(),
      )
      val notificationManager = application.getSystemService(NotificationManager::class.java)

      activityController.recreate()

      assertEquals(1, broadcastIntents.size - initialBroadcastCount)
      val activeNotification = notificationManager.activeNotifications
        .single { it.id == GameNotifications.NOTIFICATION_ID }
        .notification
      assertEquals(2, activeNotification.actions.size)
      assertEquals(
        application.getString(R.string.view_session_answers),
        activeNotification.actions.first().title,
      )
    } finally {
      activityController.destroy()
    }
  }

  private fun answerResult(
    question: String,
    selectedAnswer: String,
    isCorrect: Boolean,
  ): GameAnswerResult = GameAnswerResult(
    question = question,
    selectedAnswer = selectedAnswer,
    isCorrect = isCorrect,
    earnedPoints = 1,
  )
}
