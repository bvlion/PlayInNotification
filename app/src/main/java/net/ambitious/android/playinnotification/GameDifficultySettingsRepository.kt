package net.ambitious.android.playinnotification

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.gameDifficultySettingsDataStore by preferencesDataStore(
  name = "game_difficulty_settings",
)

internal class GameDifficultySettingsRepository(
  private val context: Context,
) {
  val gameDifficultySettings = context.gameDifficultySettingsDataStore.data.map { preferences ->
    GameDifficultySettings(
      calculationDifficulty = preferences[CALCULATION_DIFFICULTY_KEY] ?: INITIAL_DIFFICULTY,
      difficultKanjiDifficulty = preferences[DIFFICULT_KANJI_DIFFICULTY_KEY] ?: INITIAL_DIFFICULTY,
    )
  }

  suspend fun setCalculationDifficulty(difficulty: Int) {
    require(difficulty in DIFFICULTY_RANGE)
    context.gameDifficultySettingsDataStore.edit { preferences ->
      preferences[CALCULATION_DIFFICULTY_KEY] = difficulty
    }
  }

  suspend fun setDifficultKanjiDifficulty(difficulty: Int) {
    require(difficulty in DIFFICULTY_RANGE)
    context.gameDifficultySettingsDataStore.edit { preferences ->
      preferences[DIFFICULT_KANJI_DIFFICULTY_KEY] = difficulty
    }
  }

  private companion object {
    const val INITIAL_DIFFICULTY = 1
    val DIFFICULTY_RANGE = 1..5
    val CALCULATION_DIFFICULTY_KEY = intPreferencesKey("calculation_difficulty")
    val DIFFICULT_KANJI_DIFFICULTY_KEY = intPreferencesKey("difficult_kanji_difficulty")
  }
}
