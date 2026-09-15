package net.ambitious.android.playinnotification

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
}
