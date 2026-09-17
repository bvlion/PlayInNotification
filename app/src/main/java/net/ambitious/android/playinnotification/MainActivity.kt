package net.ambitious.android.playinnotification

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import java.time.LocalDate
import kotlin.math.roundToInt
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
    val notificationManager = getSystemService(NotificationManager::class.java)
    isGameNotificationEnabled = notificationManager.areNotificationsEnabled() &&
      notificationManager.getNotificationChannel(CalculationGameService.NOTIFICATION_CHANNEL_ID)
        .importance != NotificationManager.IMPORTANCE_NONE
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
    val notificationManager = getSystemService(NotificationManager::class.java)
    isGameNotificationEnabled = notificationManager.areNotificationsEnabled() &&
      notificationManager.getNotificationChannel(CalculationGameService.NOTIFICATION_CHANNEL_ID)
        .importance != NotificationManager.IMPORTANCE_NONE
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    enableEdgeToEdge()
    setContent {
      val context = LocalContext.current
      val uriHandler = LocalUriHandler.current
      val isDarkTheme = isSystemInDarkTheme()
      val gameDifficultySettings by gameDifficultySettingsRepository.gameDifficultySettings
        .collectAsStateWithLifecycle(initialValue = GameDifficultySettings())
      val gameStatistics by gameStatisticsRepository.gameStatistics
        .collectAsStateWithLifecycle(initialValue = GameStatistics())
      val lastCompletedSessionDate = gameStatistics.lastCompletedSessionDate
      val displayedStreakDayCount = if (
        lastCompletedSessionDate == null ||
        lastCompletedSessionDate < currentDate.minusDays(1)
      ) {
        0
      } else {
        gameStatistics.streakDayCount
      }
      var calculationDifficulty by remember(gameDifficultySettings.calculationDifficulty) {
        mutableFloatStateOf(gameDifficultySettings.calculationDifficulty.toFloat())
      }
      var difficultKanjiDifficulty by remember(gameDifficultySettings.difficultKanjiDifficulty) {
        mutableFloatStateOf(gameDifficultySettings.difficultKanjiDifficulty.toFloat())
      }
      val colorScheme = if (isDarkTheme) {
        dynamicDarkColorScheme(context)
      } else {
        dynamicLightColorScheme(context)
      }

      MaterialTheme(colorScheme = colorScheme) {
        val auxiliaryLinkTextStyle = MaterialTheme.typography.labelLarge.copy(
          fontSize = 16.sp,
        )
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background,
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
              verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
              if (!isGameNotificationEnabled) {
                Card(modifier = Modifier.fillMaxWidth()) {
                  Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(
                      text = stringResource(R.string.notification_disabled_title),
                      style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                      text = stringResource(R.string.notification_disabled_description),
                      style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(
                      onClick = {
                        startActivity(
                          Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
                        )
                      },
                    ) {
                      Text(text = stringResource(R.string.open_notification_settings))
                    }
                  }
                }
              }
              Text(
                text = stringResource(R.string.statistics_title),
                style = MaterialTheme.typography.headlineMedium,
              )
              Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                  modifier = Modifier.padding(24.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Text(
                    text = stringResource(
                      R.string.growth_level_value,
                      gameStatistics.growthLevel,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall,
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top,
                  ) {
                    Text(
                      text = stringResource(R.string.answer_title),
                      modifier = Modifier.weight(1f),
                      style = MaterialTheme.typography.titleMedium,
                    )
                    Column(
                      modifier = Modifier.weight(2f),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Text(
                        text = stringResource(
                          R.string.play_count,
                          gameStatistics.playCount,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                      )
                      Text(
                        text = stringResource(
                          R.string.answer_count,
                          gameStatistics.answerCount,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                      )
                    }
                  }
                  HorizontalDivider()
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top,
                  ) {
                    Text(
                      text = stringResource(R.string.day_count_title),
                      modifier = Modifier.weight(1f),
                      style = MaterialTheme.typography.titleMedium,
                    )
                    Column(
                      modifier = Modifier.weight(2f),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Text(
                        text = stringResource(
                          R.string.total_day_count,
                          gameStatistics.totalDayCount,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                      )
                      Text(
                        text = stringResource(
                          R.string.streak_day_count,
                          displayedStreakDayCount,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                      )
                    }
                  }
                }
              }
              Text(
                text = stringResource(R.string.personal_best_title),
                style = MaterialTheme.typography.headlineMedium,
              )
              Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                  modifier = Modifier.padding(24.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  listOf(
                    R.string.calculation_game to "calculation",
                    R.string.difficult_kanji_game to "difficult_kanji",
                  ).forEachIndexed { index, (gameNameResource, gameGenre) ->
                    if (index > 0) {
                      HorizontalDivider()
                    }
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(24.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Text(
                        text = stringResource(gameNameResource),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                      )
                      Text(
                        text = stringResource(
                          R.string.personal_best_points,
                          gameStatistics.bestPointsByGame[gameGenre] ?: 0,
                        ),
                        modifier = Modifier.weight(2f),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                      )
                    }
                  }
                }
              }
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
              Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider()
                TextButton(
                  onClick = {
                    uriHandler.openUri(
                      getString(R.string.google_play_url, packageName),
                    )
                  },
                ) {
                  Text(
                    text = stringResource(R.string.write_supportive_review),
                    style = auxiliaryLinkTextStyle,
                  )
                }
                TextButton(
                  onClick = {
                    uriHandler.openUri(getString(R.string.feedback_url))
                  },
                ) {
                  Text(
                    text = stringResource(R.string.send_feedback),
                    style = auxiliaryLinkTextStyle,
                  )
                }
                TextButton(
                  onClick = {
                    uriHandler.openUri(getString(R.string.privacy_policy_url))
                  },
                ) {
                  Text(
                    text = stringResource(R.string.privacy_policy),
                    style = auxiliaryLinkTextStyle,
                  )
                }
                Text(
                  text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  style = MaterialTheme.typography.bodySmall,
                )
              }
            }
            val systemBarProtectionColor = MaterialTheme.colorScheme.background
            Box(
              modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(
                  Brush.verticalGradient(
                    0.0f to systemBarProtectionColor.copy(alpha = 0.9f),
                    0.7f to systemBarProtectionColor.copy(alpha = 0.72f),
                    1.0f to systemBarProtectionColor.copy(alpha = 0.0f),
                  ),
                ),
            )
            Box(
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(
                  Brush.verticalGradient(
                    0.0f to systemBarProtectionColor.copy(alpha = 0.0f),
                    0.3f to systemBarProtectionColor.copy(alpha = 0.72f),
                    1.0f to systemBarProtectionColor.copy(alpha = 0.9f),
                  ),
                ),
            )
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    currentDate = LocalDate.now()
    val notificationManager = getSystemService(NotificationManager::class.java)
    isGameNotificationEnabled = notificationManager.areNotificationsEnabled() &&
      notificationManager.getNotificationChannel(CalculationGameService.NOTIFICATION_CHANNEL_ID)
        .importance != NotificationManager.IMPORTANCE_NONE
    if (
      isGameNotificationEnabled &&
      !CalculationGameService.isSessionActive &&
      !DifficultKanjiGameService.isSessionActive
    ) {
      lifecycleScope.launch {
        CalculationGameService.showGameSelection(this@MainActivity)
      }
    }
  }
}
