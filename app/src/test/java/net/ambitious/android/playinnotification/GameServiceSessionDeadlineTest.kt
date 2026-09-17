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
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(
  sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE],
  shadows = [DelayedStartForegroundShadowService::class],
)
class GameServiceSessionDeadlineTest {
  @Test
  fun `計算は終了処理が遅れても問題通知に負の残り時間を表示しない`() {
    val serviceController = Robolectric.buildService(CalculationGameService::class.java).create()

    try {
      val service = serviceController.get()
      CalculationGameService.createNotificationChannel(service)
      ReflectionHelpers.callInstanceMethod<Unit>(
        service,
        "startSession",
        ClassParameter.from(Int::class.javaPrimitiveType, 1),
      )
      ShadowLooper.shadowMainLooper().idle()
      val sessionDeadline = ReflectionHelpers.getField<Long>(service, "sessionDeadline")
      val notificationManager = service.getSystemService(NotificationManager::class.java)

      assertEquals(
        service.getString(R.string.game_time_remaining, 25L),
        notificationManager.activeNotifications.single().notification.extras.getCharSequence(
          Notification.EXTRA_SUB_TEXT,
        ),
      )

      ShadowLooper.shadowMainLooper().idleFor(Duration.ofSeconds(1))

      assertEquals(
        service.getString(R.string.game_time_remaining, 24L),
        notificationManager.activeNotifications.single().notification.extras.getCharSequence(
          Notification.EXTRA_SUB_TEXT,
        ),
      )

      ShadowLooper.shadowMainLooper().idleFor(Duration.ofSeconds(23))

      assertEquals(
        service.getString(R.string.game_time_remaining, 1L),
        notificationManager.activeNotifications.single().notification.extras.getCharSequence(
          Notification.EXTRA_SUB_TEXT,
        ),
      )

      ShadowSystemClock.advanceBy(Duration.ofSeconds(2))

      val notification = notificationManager.activeNotifications
        .single()
        .notification

      assertTrue(SystemClock.elapsedRealtime() - sessionDeadline >= 1_000L)
      assertFalse(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
      assertEquals(
        service.getString(R.string.game_time_remaining, 1L),
        notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
      )
    } finally {
      serviceController.destroy()
    }
  }

  @Test
  fun `難読漢字は終了処理が遅れても問題通知に負の残り時間を表示しない`() {
    val serviceController = Robolectric.buildService(DifficultKanjiGameService::class.java).create()

    try {
      val service = serviceController.get()
      ReflectionHelpers.callInstanceMethod<Unit>(
        service,
        "startSession",
        ClassParameter.from(Int::class.javaPrimitiveType, 1),
      )
      ShadowLooper.shadowMainLooper().idle()
      val sessionDeadline = ReflectionHelpers.getField<Long>(service, "sessionDeadline")
      val notificationManager = service.getSystemService(NotificationManager::class.java)

      assertEquals(
        service.getString(R.string.game_time_remaining, 25L),
        notificationManager.activeNotifications.single().notification.extras.getCharSequence(
          Notification.EXTRA_SUB_TEXT,
        ),
      )

      ShadowLooper.shadowMainLooper().idleFor(Duration.ofSeconds(1))

      assertEquals(
        service.getString(R.string.game_time_remaining, 24L),
        notificationManager.activeNotifications.single().notification.extras.getCharSequence(
          Notification.EXTRA_SUB_TEXT,
        ),
      )

      ShadowLooper.shadowMainLooper().idleFor(Duration.ofSeconds(23))

      assertEquals(
        service.getString(R.string.game_time_remaining, 1L),
        notificationManager.activeNotifications.single().notification.extras.getCharSequence(
          Notification.EXTRA_SUB_TEXT,
        ),
      )

      ShadowSystemClock.advanceBy(Duration.ofSeconds(2))

      val notification = notificationManager.activeNotifications
        .single()
        .notification

      assertTrue(SystemClock.elapsedRealtime() - sessionDeadline >= 1_000L)
      assertFalse(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
      assertEquals(
        service.getString(R.string.game_time_remaining, 1L),
        notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
      )
    } finally {
      serviceController.destroy()
    }
  }

  @Test
  fun `計算は初回通知生成に時間がかかっても終了時刻に終了処理を予約する`() {
    val serviceController = Robolectric.buildService(CalculationGameService::class.java).create()

    try {
      val service = serviceController.get()
      ReflectionHelpers.callInstanceMethod<Unit>(
        service,
        "startSession",
        ClassParameter.from(Int::class.javaPrimitiveType, 1),
      )
      ShadowLooper.shadowMainLooper().idle()

      val sessionDeadline = ReflectionHelpers.getField<Long>(service, "sessionDeadline")

      assertEquals(
        sessionDeadline,
        ShadowLooper.shadowMainLooper().lastScheduledTaskTime.toMillis(),
      )
    } finally {
      serviceController.destroy()
    }
  }

  @Test
  fun `難読漢字は初回通知生成に時間がかかっても終了時刻に終了処理を予約する`() {
    val serviceController = Robolectric.buildService(DifficultKanjiGameService::class.java).create()

    try {
      val service = serviceController.get()
      ReflectionHelpers.callInstanceMethod<Unit>(
        service,
        "startSession",
        ClassParameter.from(Int::class.javaPrimitiveType, 1),
      )
      ShadowLooper.shadowMainLooper().idle()

      val sessionDeadline = ReflectionHelpers.getField<Long>(service, "sessionDeadline")

      assertEquals(
        sessionDeadline,
        ShadowLooper.shadowMainLooper().lastScheduledTaskTime.toMillis(),
      )
    } finally {
      serviceController.destroy()
    }
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
