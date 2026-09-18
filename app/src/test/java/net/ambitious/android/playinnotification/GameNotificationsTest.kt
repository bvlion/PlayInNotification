package net.ambitious.android.playinnotification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class GameNotificationsTest {
  private lateinit var context: Context
  private lateinit var notificationManager: NotificationManager

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    notificationManager = context.getSystemService(NotificationManager::class.java)
    notificationManager.cancelAll()
    GameNotifications.createChannel(context)
  }

  @Test
  fun `ゲーム選択通知の本体はメイン画面を開く`() = runBlocking {
    GameNotifications.showGameSelection(context)

    assertOpensMainActivity(activeGameNotification())
  }

  @Test
  fun `出題中の通知の本体には遷移を設定しない`() {
    val answerIntent = PendingIntent.getBroadcast(
      context,
      1,
      Intent(context, GameNotificationActionReceiver::class.java),
      PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = GameNotifications.createQuestionNotification(
      context = context,
      title = "1 + 1 = ?",
      remainingSeconds = 30,
      answerActions = listOf("2" to answerIntent),
    )

    assertNull(notification.contentIntent)
    assertEquals(1, notification.actions.size)
    assertEquals("2", notification.actions.single().title)
  }

  @Test
  fun `セッション終了処理中の通知の本体には遷移を設定しない`() {
    GameNotifications.showSessionCompletionStarted(context, GameSessionResult())

    assertNull(activeGameNotification().contentIntent)
  }

  @Test
  fun `結果通知の本体には遷移を設定せず既存アクションを維持する`() {
    GameNotifications.showCompletedSession(
      context = context,
      sessionResult = GameSessionResult(),
      previousStatistics = GameStatistics(),
      updatedStatistics = GameStatistics(),
    )

    val notification = activeGameNotification()
    assertNull(notification.contentIntent)
    assertEquals(2, notification.actions.size)
    assertEquals(
      context.getString(R.string.view_session_answers),
      notification.actions[0].title,
    )
    assertEquals(context.getString(R.string.choose_game), notification.actions[1].title)
  }

  private fun activeGameNotification(): Notification = notificationManager.activeNotifications
    .single { it.id == GameNotifications.NOTIFICATION_ID }
    .notification

  private fun assertOpensMainActivity(notification: Notification) {
    assertNotNull(notification.contentIntent)
    val contentIntent = checkNotNull(notification.contentIntent)
    assertEquals(
      ComponentName(context, MainActivity::class.java),
      shadowOf(contentIntent).savedIntent.component,
    )
  }
}
