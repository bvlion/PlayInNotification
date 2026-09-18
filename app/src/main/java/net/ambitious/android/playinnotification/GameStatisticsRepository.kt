package net.ambitious.android.playinnotification

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDate
import kotlinx.coroutines.flow.map

private val Context.gameStatisticsDataStore by preferencesDataStore(name = "game_statistics")

internal class GameStatisticsRepository(
  private val context: Context,
) {
  val gameStatistics = context.gameStatisticsDataStore.data.map { preferences ->
    preferences.toGameStatistics()
  }

  suspend fun recordCompletedSession(
    sessionResult: GameSessionResult,
    gameType: GameType,
    completedSessionDate: LocalDate,
  ): Pair<GameStatistics, GameStatistics> {
    var statisticsChange: Pair<GameStatistics, GameStatistics>? = null
    context.gameStatisticsDataStore.edit { preferences ->
      val previousStatistics = preferences.toGameStatistics()
      val updatedStatistics = previousStatistics.addCompletedSession(
        sessionResult = sessionResult,
        gameType = gameType,
        completedSessionDate = completedSessionDate,
      )
      statisticsChange = previousStatistics to updatedStatistics
      preferences[ANSWER_COUNT_KEY] = updatedStatistics.answerCount
      preferences[PLAY_COUNT_KEY] = updatedStatistics.playCount
      preferences[EARNED_POINTS_KEY] = updatedStatistics.earnedPoints
      preferences[TOTAL_DAY_COUNT_KEY] = updatedStatistics.totalDayCount
      preferences[STREAK_DAY_COUNT_KEY] = updatedStatistics.streakDayCount
      preferences[LAST_COMPLETED_SESSION_DATE_KEY] = completedSessionDate.toString()
      preferences[bestPointsKey(gameType)] = updatedStatistics.bestPointsByGame.getValue(gameType)
    }
    return checkNotNull(statisticsChange)
  }

  private fun Preferences.toGameStatistics(): GameStatistics {
    return GameStatistics(
      answerCount = this[ANSWER_COUNT_KEY] ?: 0,
      playCount = this[PLAY_COUNT_KEY] ?: 0,
      earnedPoints = this[EARNED_POINTS_KEY] ?: 0,
      totalDayCount = this[TOTAL_DAY_COUNT_KEY] ?: 0,
      streakDayCount = this[STREAK_DAY_COUNT_KEY] ?: 0,
      lastCompletedSessionDate = this[LAST_COMPLETED_SESSION_DATE_KEY]?.let(LocalDate::parse),
      bestPointsByGame = GameType.entries.associateWith { gameType ->
        this[bestPointsKey(gameType)] ?: 0
      },
    )
  }

  private fun bestPointsKey(gameType: GameType) =
    longPreferencesKey("$BEST_POINTS_KEY_PREFIX${gameType.storageKey}")

  private companion object {
    val ANSWER_COUNT_KEY = longPreferencesKey("answer_count")
    val PLAY_COUNT_KEY = longPreferencesKey("play_count")
    val EARNED_POINTS_KEY = longPreferencesKey("earned_points")
    val TOTAL_DAY_COUNT_KEY = intPreferencesKey("total_day_count")
    val STREAK_DAY_COUNT_KEY = intPreferencesKey("streak_day_count")
    val LAST_COMPLETED_SESSION_DATE_KEY = stringPreferencesKey("last_completed_session_date")
    const val BEST_POINTS_KEY_PREFIX = "best_points_"
  }
}
