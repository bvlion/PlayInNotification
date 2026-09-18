package net.ambitious.android.playinnotification

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.os.Build
import android.os.SystemClock
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowService
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(
  sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE],
  shadows = [DelayedStartForegroundShadowService::class],
)
class GameServiceSessionDeadlineTest {
  @Test
  fun `計算は終了処理が遅れても問題通知に負の残り時間を表示しない`() {
    verifyCountdownDoesNotBecomeNegative(CalculationGameService::class.java)
  }

  @Test
  fun `難読漢字は終了処理が遅れても問題通知に負の残り時間を表示しない`() {
    verifyCountdownDoesNotBecomeNegative(DifficultKanjiGameService::class.java)
  }

  @Test
  fun `初回通知生成に時間がかかってもセッション開始から30秒後に終了処理を予約する`() {
    val serviceController = Robolectric.buildService(CalculationGameService::class.java).create()

    try {
      val service = serviceController.get()
      val sessionStartedAt = SystemClock.elapsedRealtime()
      service.onStartCommand(
        GameSessionService.createStartIntent(
          context = service,
          serviceClass = CalculationGameService::class.java,
          difficulty = GameDifficulty.LEVEL_ONE,
        ),
        0,
        1,
      )
      ShadowLooper.shadowMainLooper().idle()

      assertEquals(
        sessionStartedAt + SESSION_DURATION_MILLISECONDS,
        ShadowLooper.shadowMainLooper().lastScheduledTaskTime.toMillis(),
      )
    } finally {
      serviceController.destroy()
    }
  }

  private fun <ServiceType : GameSessionService<*>> verifyCountdownDoesNotBecomeNegative(
    serviceClass: Class<ServiceType>,
  ) {
    val serviceController = Robolectric.buildService(serviceClass).create()

    try {
      val service = serviceController.get()
      service.onStartCommand(
        GameSessionService.createStartIntent(
          context = service,
          serviceClass = serviceClass,
          difficulty = GameDifficulty.LEVEL_ONE,
        ),
        0,
        1,
      )
      ShadowLooper.shadowMainLooper().idle()
      val notificationManager = service.getSystemService(NotificationManager::class.java)

      assertEquals(
        service.getString(R.string.game_time_remaining, 25L),
        notificationManager.currentRemainingTime(),
      )

      ShadowLooper.shadowMainLooper().idleFor(Duration.ofSeconds(1))

      assertEquals(
        service.getString(R.string.game_time_remaining, 24L),
        notificationManager.currentRemainingTime(),
      )

      ShadowLooper.shadowMainLooper().idleFor(Duration.ofSeconds(23))

      assertEquals(
        service.getString(R.string.game_time_remaining, 1L),
        notificationManager.currentRemainingTime(),
      )

      ShadowSystemClock.advanceBy(Duration.ofSeconds(2))

      val notification = notificationManager.activeNotifications.single().notification
      assertFalse(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
      assertEquals(
        service.getString(R.string.game_time_remaining, 1L),
        notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
      )
      assertTrue(SystemClock.elapsedRealtime() >= SESSION_DURATION_MILLISECONDS)
    } finally {
      serviceController.destroy()
    }
  }

  private fun NotificationManager.currentRemainingTime(): CharSequence? =
    activeNotifications.single().notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)

  private companion object {
    const val SESSION_DURATION_MILLISECONDS = 30_000L
  }
}

@Implements(Service::class)
class DelayedStartForegroundShadowService : ShadowService() {
  @Implementation
  override fun startForeground(id: Int, notification: Notification) {
    ShadowSystemClock.advanceBy(SIMULATED_INITIAL_NOTIFICATION_DURATION)
    super.startForeground(id, notification)
  }

  @Implementation(minSdk = Build.VERSION_CODES.Q)
  override fun startForeground(
    id: Int,
    notification: Notification,
    foregroundServiceType: Int,
  ) {
    super.startForeground(id, notification, foregroundServiceType)
  }

  private companion object {
    val SIMULATED_INITIAL_NOTIFICATION_DURATION: Duration = Duration.ofSeconds(5)
  }
}
