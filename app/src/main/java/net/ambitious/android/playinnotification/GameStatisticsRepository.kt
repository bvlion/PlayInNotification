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
    gameGenre: String,
    difficulty: Int,
    completedSessionDate: LocalDate,
  ): Pair<GameStatistics, GameStatistics> {
    lateinit var previousStatistics: GameStatistics
    lateinit var updatedStatistics: GameStatistics
    context.gameStatisticsDataStore.edit { preferences ->
      previousStatistics = preferences.toGameStatistics()
      updatedStatistics = previousStatistics.addCompletedSession(
        sessionResult = sessionResult,
        gameGenre = gameGenre,
        difficulty = difficulty,
        completedSessionDate = completedSessionDate,
      )
      preferences[ANSWER_COUNT_KEY] = updatedStatistics.answerCount
      preferences[PLAY_COUNT_KEY] = updatedStatistics.playCount
      preferences[EARNED_POINTS_KEY] = updatedStatistics.earnedPoints
      preferences[TOTAL_DAY_COUNT_KEY] = updatedStatistics.totalDayCount
      preferences[STREAK_DAY_COUNT_KEY] = updatedStatistics.streakDayCount
      preferences[LAST_COMPLETED_SESSION_DATE_KEY] = completedSessionDate.toString()
      preferences[bestPointsKey(gameGenre)] = updatedStatistics.bestPointsByGame.getValue(gameGenre)
    }
    return previousStatistics to updatedStatistics
  }

  private fun Preferences.toGameStatistics(): GameStatistics {
    return GameStatistics(
      answerCount = this[ANSWER_COUNT_KEY] ?: 0,
      playCount = this[PLAY_COUNT_KEY] ?: 0,
      earnedPoints = this[EARNED_POINTS_KEY] ?: 0,
      totalDayCount = this[TOTAL_DAY_COUNT_KEY] ?: 0,
      streakDayCount = this[STREAK_DAY_COUNT_KEY] ?: 0,
      lastCompletedSessionDate = this[LAST_COMPLETED_SESSION_DATE_KEY]?.let(LocalDate::parse),
      bestPointsByGame = GAME_GENRES.associateWith { gameGenre ->
        this[bestPointsKey(gameGenre)] ?: 0
      },
    )
  }

  private fun bestPointsKey(gameGenre: String) = longPreferencesKey("$BEST_POINTS_KEY_PREFIX$gameGenre")

  private companion object {
    val ANSWER_COUNT_KEY = longPreferencesKey("answer_count")
    val PLAY_COUNT_KEY = longPreferencesKey("play_count")
    val EARNED_POINTS_KEY = longPreferencesKey("earned_points")
    val TOTAL_DAY_COUNT_KEY = intPreferencesKey("total_day_count")
    val STREAK_DAY_COUNT_KEY = intPreferencesKey("streak_day_count")
    val LAST_COMPLETED_SESSION_DATE_KEY = stringPreferencesKey("last_completed_session_date")
    const val BEST_POINTS_KEY_PREFIX = "best_points_"
    val GAME_GENRES = listOf("calculation", "difficult_kanji")
  }
}
