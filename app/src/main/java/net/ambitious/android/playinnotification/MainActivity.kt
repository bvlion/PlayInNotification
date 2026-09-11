package net.ambitious.android.playinnotification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val gameDifficultySettingsRepository by lazy {
    GameDifficultySettingsRepository(applicationContext)
  }

  private val notificationPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission(),
  ) { isGranted ->
    if (
      isGranted &&
      !CalculationGameService.isSessionActive &&
      !DifficultKanjiGameService.isSessionActive
    ) {
      lifecycleScope.launch {
        CalculationGameService.showGameSelection(this@MainActivity)
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    CalculationGameService.createNotificationChannel(this)
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    enableEdgeToEdge()
    setContent {
      val context = LocalContext.current
      val gameDifficultySettings by gameDifficultySettingsRepository.gameDifficultySettings
        .collectAsStateWithLifecycle(initialValue = GameDifficultySettings())
      var calculationDifficulty by remember(gameDifficultySettings.calculationDifficulty) {
        mutableFloatStateOf(gameDifficultySettings.calculationDifficulty.toFloat())
      }
      var difficultKanjiDifficulty by remember(gameDifficultySettings.difficultKanjiDifficulty) {
        mutableFloatStateOf(gameDifficultySettings.difficultKanjiDifficulty.toFloat())
      }
      val colorScheme = if (isSystemInDarkTheme()) {
        dynamicDarkColorScheme(context)
      } else {
        dynamicLightColorScheme(context)
      }

      MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .safeDrawingPadding()
              .verticalScroll(rememberScrollState())
              .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
          ) {
            Text(
              text = stringResource(R.string.game_difficulty_settings_title),
              style = MaterialTheme.typography.headlineMedium,
            )
            Card(modifier = Modifier.fillMaxWidth()) {
              Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = stringResource(R.string.calculation_game),
                  style = MaterialTheme.typography.titleLarge,
                )
                Text(
                  text = stringResource(
                    R.string.game_difficulty_level,
                    calculationDifficulty.roundToInt(),
                  ),
                  style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                  value = calculationDifficulty,
                  onValueChange = { calculationDifficulty = it },
                  onValueChangeFinished = {
                    lifecycleScope.launch {
                      gameDifficultySettingsRepository.setCalculationDifficulty(
                        calculationDifficulty.roundToInt(),
                      )
                      if (
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                          checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                          PackageManager.PERMISSION_GRANTED) &&
                        !CalculationGameService.isSessionActive &&
                        !DifficultKanjiGameService.isSessionActive
                      ) {
                        CalculationGameService.showGameSelection(this@MainActivity)
                      }
                    }
                  },
                  valueRange = 1f..5f,
                  steps = 3,
                )
              }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
              Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = stringResource(R.string.difficult_kanji_game),
                  style = MaterialTheme.typography.titleLarge,
                )
                Text(
                  text = stringResource(
                    R.string.game_difficulty_level,
                    difficultKanjiDifficulty.roundToInt(),
                  ),
                  style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                  value = difficultKanjiDifficulty,
                  onValueChange = { difficultKanjiDifficulty = it },
                  onValueChangeFinished = {
                    lifecycleScope.launch {
                      gameDifficultySettingsRepository.setDifficultKanjiDifficulty(
                        difficultKanjiDifficulty.roundToInt(),
                      )
                      if (
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                          checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                          PackageManager.PERMISSION_GRANTED) &&
                        !CalculationGameService.isSessionActive &&
                        !DifficultKanjiGameService.isSessionActive
                      ) {
                        CalculationGameService.showGameSelection(this@MainActivity)
                      }
                    }
                  },
                  valueRange = 1f..5f,
                  steps = 3,
                )
              }
            }
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (
      (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
      !CalculationGameService.isSessionActive &&
      !DifficultKanjiGameService.isSessionActive
    ) {
      lifecycleScope.launch {
        CalculationGameService.showGameSelection(this@MainActivity)
      }
    }
  }
}
