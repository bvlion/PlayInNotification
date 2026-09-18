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
      calculationDifficulty = GameDifficulty.fromLevel(
        preferences[CALCULATION_DIFFICULTY_KEY] ?: GameDifficulty.initial.level,
      ),
      difficultKanjiDifficulty = GameDifficulty.fromLevel(
        preferences[DIFFICULT_KANJI_DIFFICULTY_KEY] ?: GameDifficulty.initial.level,
      ),
    )
  }

  suspend fun setDifficulty(gameType: GameType, difficulty: GameDifficulty) {
    context.gameDifficultySettingsDataStore.edit { preferences ->
      preferences[when (gameType) {
        GameType.CALCULATION -> CALCULATION_DIFFICULTY_KEY
        GameType.DIFFICULT_KANJI -> DIFFICULT_KANJI_DIFFICULTY_KEY
      }] = difficulty.level
    }
  }

  private companion object {
    val CALCULATION_DIFFICULTY_KEY = intPreferencesKey("calculation_difficulty")
    val DIFFICULT_KANJI_DIFFICULTY_KEY = intPreferencesKey("difficult_kanji_difficulty")
  }
}
