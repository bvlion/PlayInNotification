package net.ambitious.android.playinnotification

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDate

private val Context.gameStatisticsDataStore by preferencesDataStore(name = "game_statistics")

internal class GameStatisticsRepository(
  private val context: Context,
) {
  suspend fun recordCompletedSession(
    sessionResult: CalculationSessionResult,
    gameGenre: String,
    difficulty: Int,
    completedSessionDate: LocalDate,
  ) {
    context.gameStatisticsDataStore.edit { preferences ->
      val currentStatistics = preferences.toGameStatistics(gameGenre, difficulty)
      val updatedStatistics = currentStatistics.addCompletedSession(
        sessionResult = sessionResult,
        gameGenre = gameGenre,
        difficulty = difficulty,
        completedSessionDate = completedSessionDate,
      )
      preferences[ANSWER_COUNT_KEY] = updatedStatistics.answerCount
      preferences[PLAY_COUNT_KEY] = updatedStatistics.playCount
      preferences[EARNED_POINTS_KEY] = updatedStatistics.earnedPoints
      preferences[STREAK_DAY_COUNT_KEY] = updatedStatistics.streakDayCount
      preferences[LAST_COMPLETED_SESSION_DATE_KEY] = completedSessionDate.toString()
      preferences[bestPointsKey(gameGenre, difficulty)] =
        updatedStatistics.bestPointsByGame.getValue("$gameGenre:$difficulty")
    }
  }

  private fun Preferences.toGameStatistics(
    gameGenre: String,
    difficulty: Int,
  ): GameStatistics {
    val gameKey = "$gameGenre:$difficulty"
    return GameStatistics(
      answerCount = this[ANSWER_COUNT_KEY] ?: 0,
      playCount = this[PLAY_COUNT_KEY] ?: 0,
      earnedPoints = this[EARNED_POINTS_KEY] ?: 0,
      streakDayCount = this[STREAK_DAY_COUNT_KEY] ?: 0,
      lastCompletedSessionDate = this[LAST_COMPLETED_SESSION_DATE_KEY]?.let(LocalDate::parse),
      bestPointsByGame = mapOf(gameKey to (this[bestPointsKey(gameGenre, difficulty)] ?: 0)),
    )
  }

  private fun bestPointsKey(gameGenre: String, difficulty: Int) =
    longPreferencesKey("$BEST_POINTS_KEY_PREFIX${gameGenre}_$difficulty")

  private companion object {
    val ANSWER_COUNT_KEY = longPreferencesKey("answer_count")
    val PLAY_COUNT_KEY = longPreferencesKey("play_count")
    val EARNED_POINTS_KEY = longPreferencesKey("earned_points")
    val STREAK_DAY_COUNT_KEY = intPreferencesKey("streak_day_count")
    val LAST_COMPLETED_SESSION_DATE_KEY = stringPreferencesKey("last_completed_session_date")
    const val BEST_POINTS_KEY_PREFIX = "best_points_"
  }
}
