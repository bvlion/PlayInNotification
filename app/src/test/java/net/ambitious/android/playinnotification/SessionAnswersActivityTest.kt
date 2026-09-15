package net.ambitious.android.playinnotification

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
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
  fun `最前面で新しいセッションの回答を開くと表示元の回答内容が更新される`() {
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
    ).create()

    try {
      val activity = activityController.get()
      assertEquals(
        arrayListOf("1 + 1 = ?"),
        activity.answersIntent.getStringArrayListExtra(SessionAnswersActivity.EXTRA_QUESTIONS),
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
        activity.answersIntent.getStringArrayListExtra(SessionAnswersActivity.EXTRA_QUESTIONS),
      )
      assertEquals(
        arrayListOf("ひとで", "なまこ"),
        activity.answersIntent.getStringArrayListExtra(
          SessionAnswersActivity.EXTRA_SELECTED_ANSWERS,
        ),
      )
      assertArrayEquals(
        booleanArrayOf(true, false),
        activity.answersIntent.getBooleanArrayExtra(SessionAnswersActivity.EXTRA_CORRECTNESS),
      )
    } finally {
      activityController.destroy()
    }
  }
}
