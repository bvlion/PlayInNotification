package net.ambitious.android.playinnotification

import androidx.annotation.StringRes
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import java.time.LocalDate
import kotlin.math.roundToInt

private val SCREEN_CONTENT_PADDING = 24.dp
private val SCREEN_SECTION_SPACING = 24.dp
private val CARD_CONTENT_PADDING = 24.dp
private val COMPACT_CARD_CONTENT_SPACING = 8.dp
private val STATISTICS_CARD_CONTENT_SPACING = 12.dp
private val LIST_ROW_SPACING = 24.dp
private const val TITLE_COLUMN_WEIGHT = 1f
private const val VALUE_COLUMN_WEIGHT = 2f
private const val SLIDER_ENDPOINT_COUNT = 2
private val AUXILIARY_LINK_TEXT_SIZE = 16.sp
private val APP_VERSION_HORIZONTAL_PADDING = 12.dp
private val APP_VERSION_VERTICAL_PADDING = 8.dp
private const val PREVIOUS_CALENDAR_DAY_OFFSET = 1L
private const val NO_ACTIVE_STREAK_DAY_COUNT = 0
private const val GRADIENT_START_POSITION = 0.0f
private const val GRADIENT_TOP_MIDDLE_POSITION = 0.7f
private const val GRADIENT_BOTTOM_MIDDLE_POSITION = 0.3f
private const val GRADIENT_END_POSITION = 1.0f
private const val GRADIENT_EDGE_ALPHA = 0.9f
private const val GRADIENT_MIDDLE_ALPHA = 0.72f
private const val GRADIENT_TRANSPARENT_ALPHA = 0.0f

@Composable
internal fun PlayInNotificationApp(
  isGameNotificationEnabled: Boolean,
  currentDate: LocalDate,
  difficultySettings: GameDifficultySettings,
  statistics: GameStatistics,
  onOpenNotificationSettings: () -> Unit,
  onDifficultyChanged: (GameType, GameDifficulty) -> Unit,
) {
  val context = LocalContext.current
  val colorScheme = if (isSystemInDarkTheme()) {
    dynamicDarkColorScheme(context)
  } else {
    dynamicLightColorScheme(context)
  }
  MaterialTheme(colorScheme = colorScheme) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background,
    ) {
      MainScreen(
        isGameNotificationEnabled = isGameNotificationEnabled,
        currentDate = currentDate,
        difficultySettings = difficultySettings,
        statistics = statistics,
        onOpenNotificationSettings = onOpenNotificationSettings,
        onDifficultyChanged = onDifficultyChanged,
      )
    }
  }
}

@Composable
private fun MainScreen(
  isGameNotificationEnabled: Boolean,
  currentDate: LocalDate,
  difficultySettings: GameDifficultySettings,
  statistics: GameStatistics,
  onOpenNotificationSettings: () -> Unit,
  onDifficultyChanged: (GameType, GameDifficulty) -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(SCREEN_CONTENT_PADDING),
      verticalArrangement = Arrangement.spacedBy(SCREEN_SECTION_SPACING),
    ) {
      if (!isGameNotificationEnabled) {
        NotificationDisabledCard(onOpenNotificationSettings)
      }
      StatisticsSection(statistics, currentDate)
      PersonalBestSection(statistics)
      DifficultySettingsSection(difficultySettings, onDifficultyChanged)
      AuxiliaryLinks()
    }
    SystemBarProtection()
  }
}

@Composable
private fun NotificationDisabledCard(onOpenNotificationSettings: () -> Unit) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(CARD_CONTENT_PADDING),
      verticalArrangement = Arrangement.spacedBy(COMPACT_CARD_CONTENT_SPACING),
    ) {
      Text(
        text = stringResource(R.string.notification_disabled_title),
        style = MaterialTheme.typography.titleLarge,
      )
      Text(
        text = stringResource(R.string.notification_disabled_description),
        style = MaterialTheme.typography.bodyLarge,
      )
      Button(onClick = onOpenNotificationSettings) {
        Text(text = stringResource(R.string.open_notification_settings))
      }
    }
  }
}

@Composable
private fun StatisticsSection(statistics: GameStatistics, currentDate: LocalDate) {
  val lastCompletedSessionDate = statistics.lastCompletedSessionDate
  val displayedStreakDayCount = if (
    lastCompletedSessionDate == null ||
      lastCompletedSessionDate < currentDate.minusDays(PREVIOUS_CALENDAR_DAY_OFFSET)
  ) {
    NO_ACTIVE_STREAK_DAY_COUNT
  } else {
    statistics.streakDayCount
  }
  Text(
    text = stringResource(R.string.statistics_title),
    style = MaterialTheme.typography.headlineMedium,
  )
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(CARD_CONTENT_PADDING),
      verticalArrangement = Arrangement.spacedBy(STATISTICS_CARD_CONTENT_SPACING),
    ) {
      Text(
        text = stringResource(R.string.growth_level_value, statistics.growthLevel),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.headlineSmall,
      )
      StatisticRow(
        titleResource = R.string.answer_title,
        values = listOf(
          stringResource(R.string.play_count, statistics.playCount),
          stringResource(R.string.answer_count, statistics.answerCount),
        ),
      )
      HorizontalDivider()
      StatisticRow(
        titleResource = R.string.day_count_title,
        values = listOf(
          stringResource(R.string.total_day_count, statistics.totalDayCount),
          stringResource(R.string.streak_day_count, displayedStreakDayCount),
        ),
      )
    }
  }
}

@Composable
private fun StatisticRow(
  @StringRes titleResource: Int,
  values: List<String>,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(LIST_ROW_SPACING),
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      text = stringResource(titleResource),
      modifier = Modifier.weight(TITLE_COLUMN_WEIGHT),
      style = MaterialTheme.typography.titleMedium,
    )
    Column(
      modifier = Modifier.weight(VALUE_COLUMN_WEIGHT),
      verticalArrangement = Arrangement.spacedBy(COMPACT_CARD_CONTENT_SPACING),
    ) {
      values.forEach { value ->
        Text(
          text = value,
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.titleLarge,
        )
      }
    }
  }
}

@Composable
private fun PersonalBestSection(statistics: GameStatistics) {
  Text(
    text = stringResource(R.string.personal_best_title),
    style = MaterialTheme.typography.headlineMedium,
  )
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(CARD_CONTENT_PADDING),
      verticalArrangement = Arrangement.spacedBy(STATISTICS_CARD_CONTENT_SPACING),
    ) {
      listOf(
        GameType.CALCULATION to R.string.calculation_game,
        GameType.DIFFICULT_KANJI to R.string.difficult_kanji_game,
      ).forEachIndexed { index, (gameType, gameNameResource) ->
        if (index > 0) {
          HorizontalDivider()
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(LIST_ROW_SPACING),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(gameNameResource),
            modifier = Modifier.weight(TITLE_COLUMN_WEIGHT),
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = stringResource(
              R.string.personal_best_points,
              statistics.bestPointsByGame[gameType] ?: 0,
            ),
            modifier = Modifier.weight(VALUE_COLUMN_WEIGHT),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun DifficultySettingsSection(
  difficultySettings: GameDifficultySettings,
  onDifficultyChanged: (GameType, GameDifficulty) -> Unit,
) {
  Text(
    text = stringResource(R.string.game_difficulty_settings_title),
    style = MaterialTheme.typography.headlineMedium,
  )
  DifficultyCard(
    gameType = GameType.CALCULATION,
    gameNameResource = R.string.calculation_game,
    savedDifficulty = difficultySettings.calculationDifficulty,
    onDifficultyChanged = onDifficultyChanged,
  )
  DifficultyCard(
    gameType = GameType.DIFFICULT_KANJI,
    gameNameResource = R.string.difficult_kanji_game,
    savedDifficulty = difficultySettings.difficultKanjiDifficulty,
    onDifficultyChanged = onDifficultyChanged,
  )
}

@Composable
private fun DifficultyCard(
  gameType: GameType,
  @StringRes gameNameResource: Int,
  savedDifficulty: GameDifficulty,
  onDifficultyChanged: (GameType, GameDifficulty) -> Unit,
) {
  var selectedLevel by remember(savedDifficulty) {
    mutableFloatStateOf(savedDifficulty.level.toFloat())
  }
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(CARD_CONTENT_PADDING),
      verticalArrangement = Arrangement.spacedBy(COMPACT_CARD_CONTENT_SPACING),
    ) {
      Text(
        text = stringResource(gameNameResource),
        style = MaterialTheme.typography.titleLarge,
      )
      Text(
        text = stringResource(R.string.game_difficulty_level, selectedLevel.roundToInt()),
        style = MaterialTheme.typography.bodyLarge,
      )
      Slider(
        value = selectedLevel,
        onValueChange = { selectedLevel = it },
        onValueChangeFinished = {
          onDifficultyChanged(gameType, GameDifficulty.fromLevel(selectedLevel.roundToInt()))
        },
        valueRange = GameDifficulty.entries.first().level.toFloat()..
          GameDifficulty.entries.last().level.toFloat(),
        steps = GameDifficulty.entries.size - SLIDER_ENDPOINT_COUNT,
      )
    }
  }
}

@Composable
private fun AuxiliaryLinks() {
  val context = LocalContext.current
  val uriHandler = LocalUriHandler.current
  val linkTextStyle = MaterialTheme.typography.labelLarge.copy(
    fontSize = AUXILIARY_LINK_TEXT_SIZE,
  )
  val googlePlayUrl = stringResource(R.string.google_play_url, context.packageName)
  val feedbackUrl = stringResource(R.string.feedback_url)
  val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
  Column(modifier = Modifier.fillMaxWidth()) {
    HorizontalDivider()
    TextButton(
      onClick = { uriHandler.openUri(googlePlayUrl) },
    ) {
      Text(
        text = stringResource(R.string.write_supportive_review),
        style = linkTextStyle,
      )
    }
    TextButton(
      onClick = { uriHandler.openUri(feedbackUrl) },
    ) {
      Text(text = stringResource(R.string.send_feedback), style = linkTextStyle)
    }
    TextButton(
      onClick = { uriHandler.openUri(privacyPolicyUrl) },
    ) {
      Text(text = stringResource(R.string.privacy_policy), style = linkTextStyle)
    }
    Text(
      text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
      modifier = Modifier.padding(
        horizontal = APP_VERSION_HORIZONTAL_PADDING,
        vertical = APP_VERSION_VERTICAL_PADDING,
      ),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun SystemBarProtection() {
  val protectionColor = MaterialTheme.colorScheme.background
  Box(
    modifier = Modifier
      .fillMaxSize(),
  ) {
    Box(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .windowInsetsTopHeight(WindowInsets.statusBars)
        .background(
          Brush.verticalGradient(
            GRADIENT_START_POSITION to protectionColor.copy(alpha = GRADIENT_EDGE_ALPHA),
            GRADIENT_TOP_MIDDLE_POSITION to
              protectionColor.copy(alpha = GRADIENT_MIDDLE_ALPHA),
            GRADIENT_END_POSITION to protectionColor.copy(alpha = GRADIENT_TRANSPARENT_ALPHA),
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
            GRADIENT_START_POSITION to protectionColor.copy(alpha = GRADIENT_TRANSPARENT_ALPHA),
            GRADIENT_BOTTOM_MIDDLE_POSITION to
              protectionColor.copy(alpha = GRADIENT_MIDDLE_ALPHA),
            GRADIENT_END_POSITION to protectionColor.copy(alpha = GRADIENT_EDGE_ALPHA),
          ),
        ),
    )
  }
}
