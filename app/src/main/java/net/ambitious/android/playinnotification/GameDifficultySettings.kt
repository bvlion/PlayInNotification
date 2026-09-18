package net.ambitious.android.playinnotification

internal data class GameDifficultySettings(
  val calculationDifficulty: GameDifficulty = GameDifficulty.initial,
  val difficultKanjiDifficulty: GameDifficulty = GameDifficulty.initial,
)
