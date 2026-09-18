package net.ambitious.android.playinnotification

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import java.time.LocalDate
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private var isGameNotificationEnabled by mutableStateOf(false)
  private var currentDate by mutableStateOf(LocalDate.now())

  private val gameDifficultySettingsRepository by lazy {
    GameDifficultySettingsRepository(applicationContext)
  }
  private val gameStatisticsRepository by lazy {
    GameStatisticsRepository(applicationContext)
  }

  private val notificationPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission(),
  ) { isGranted ->
    refreshNotificationState()
    if (isGranted) {
      showGameSelection()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    GameNotifications.createChannel(this)
    refreshNotificationState()
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    enableEdgeToEdge()
    setContent {
      val difficultySettings by gameDifficultySettingsRepository.gameDifficultySettings
        .collectAsStateWithLifecycle(initialValue = GameDifficultySettings())
      val statistics by gameStatisticsRepository.gameStatistics
        .collectAsStateWithLifecycle(initialValue = GameStatistics())
      PlayInNotificationApp(
        isGameNotificationEnabled = isGameNotificationEnabled,
        currentDate = currentDate,
        difficultySettings = difficultySettings,
        statistics = statistics,
        onOpenNotificationSettings = {
          startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
              .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
          )
        },
        onDifficultyChanged = ::saveDifficulty,
      )
    }
  }

  override fun onResume() {
    super.onResume()
    currentDate = LocalDate.now()
    refreshNotificationState()
    if (isGameNotificationEnabled) {
      showGameSelection()
    }
  }

  private fun saveDifficulty(gameType: GameType, difficulty: GameDifficulty) {
    lifecycleScope.launch {
      gameDifficultySettingsRepository.setDifficulty(gameType, difficulty)
      GameNotifications.showGameSelection(this@MainActivity)
    }
  }

  private fun refreshNotificationState() {
    isGameNotificationEnabled = GameNotifications.isEnabled(this)
  }

  private fun showGameSelection() {
    lifecycleScope.launch {
      GameNotifications.showGameSelection(this@MainActivity)
    }
  }
}
