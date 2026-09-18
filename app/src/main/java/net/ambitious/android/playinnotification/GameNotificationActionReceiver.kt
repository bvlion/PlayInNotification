package net.ambitious.android.playinnotification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameNotificationActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_SHOW_GAME_SELECTION) {
      return
    }

    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        GameNotifications.showGameSelection(
          context = context.applicationContext,
          shouldReplaceResult = true,
        )
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    internal const val ACTION_SHOW_GAME_SELECTION =
      "net.ambitious.android.playinnotification.action.SHOW_GAME_SELECTION"
  }
}
