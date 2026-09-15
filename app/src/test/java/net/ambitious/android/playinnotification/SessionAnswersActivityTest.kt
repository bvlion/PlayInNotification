package net.ambitious.android.playinnotification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import org.junit.Assert.assertArrayEquals
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
    val previousAnswersIntent = Intent(application, SessionAnswersActivity::class.java)
      .putStringArrayListExtra(
        SessionAnswersActivity.EXTRA_QUESTIONS,
        arrayListOf("1 + 1 = ?"),
      )
      .putStringArrayListExtra(
        SessionAnswersActivity.EXTRA_SELECTED_ANSWERS,
        arrayListOf("2"),
      )
      .putExtra(SessionAnswersActivity.EXTRA_CORRECTNESS, booleanArrayOf(true))
    val activityController = Robolectric.buildActivity(
      SessionAnswersActivity::class.java,
      previousAnswersIntent,
    ).setup()

    try {
      val activity = activityController.get()
      assertEquals(
        arrayListOf("1 + 1 = ?"),
        activity.answers.getStringArrayList(SessionAnswersActivity.EXTRA_QUESTIONS),
      )

      val newAnswersIntent = Intent(application, SessionAnswersActivity::class.java)
        .putStringArrayListExtra(
          SessionAnswersActivity.EXTRA_QUESTIONS,
          arrayListOf("「海星」の読みは？", "「海月」の読みは？"),
        )
        .putStringArrayListExtra(
          SessionAnswersActivity.EXTRA_SELECTED_ANSWERS,
          arrayListOf("ひとで", "なまこ"),
        )
        .putExtra(SessionAnswersActivity.EXTRA_CORRECTNESS, booleanArrayOf(true, false))

      activityController.newIntent(newAnswersIntent)

      assertEquals(
        arrayListOf("「海星」の読みは？", "「海月」の読みは？"),
        activity.answers.getStringArrayList(SessionAnswersActivity.EXTRA_QUESTIONS),
      )
      assertEquals(
        arrayListOf("ひとで", "なまこ"),
        activity.answers.getStringArrayList(
          SessionAnswersActivity.EXTRA_SELECTED_ANSWERS,
        ),
      )
      assertArrayEquals(
        booleanArrayOf(true, false),
        activity.answers.getBooleanArray(SessionAnswersActivity.EXTRA_CORRECTNESS),
      )

      activityController.recreate()

      assertEquals(
        arrayListOf("「海星」の読みは？", "「海月」の読みは？"),
        activityController.get().answers.getStringArrayList(
          SessionAnswersActivity.EXTRA_QUESTIONS,
        ),
      )
      assertEquals(
        arrayListOf("ひとで", "なまこ"),
        activityController.get().answers.getStringArrayList(
          SessionAnswersActivity.EXTRA_SELECTED_ANSWERS,
        ),
      )
      assertArrayEquals(
        booleanArrayOf(true, false),
        activityController.get().answers.getBooleanArray(SessionAnswersActivity.EXTRA_CORRECTNESS),
      )
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
      Intent(application, SessionAnswersActivity::class.java),
    ).setup()

    try {
      assertEquals(1, broadcastIntents.size - initialBroadcastCount)
      CalculationGameService.createNotificationChannel(application)
      val notificationManager = application.getSystemService(NotificationManager::class.java)
      val viewAnswersPendingIntent = PendingIntent.getActivity(
        application,
        0,
        Intent(application, SessionAnswersActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
      val resultNotification = Notification.Builder(
        application,
        CalculationGameService.NOTIFICATION_CHANNEL_ID,
      )
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(application.getString(R.string.session_finished_title))
        .addExtras(Bundle().apply {
          putBoolean(CalculationGameService.EXTRA_IS_RESULT_NOTIFICATION, true)
        })
        .addAction(
          Notification.Action.Builder(
            null,
            application.getString(R.string.view_session_answers),
            viewAnswersPendingIntent,
          ).build(),
        )
        .build()
      notificationManager.notify(1, resultNotification)

      activityController.recreate()

      assertEquals(1, broadcastIntents.size - initialBroadcastCount)
      val activeNotification = notificationManager.activeNotifications.single { it.id == 1 }
        .notification
      assertEquals(
        true,
        activeNotification.extras.getBoolean(CalculationGameService.EXTRA_IS_RESULT_NOTIFICATION),
      )
      assertEquals(
        application.getString(R.string.view_session_answers),
        activeNotification.actions.single().title,
      )
    } finally {
      activityController.destroy()
    }
  }
}
