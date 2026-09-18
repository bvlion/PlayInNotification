package net.ambitious.android.playinnotification

internal enum class GameDifficulty(
  val level: Int,
  val correctAnswerPoints: Int,
) {
  LEVEL_ONE(level = 1, correctAnswerPoints = 3),
  LEVEL_TWO(level = 2, correctAnswerPoints = 4),
  LEVEL_THREE(level = 3, correctAnswerPoints = 6),
  LEVEL_FOUR(level = 4, correctAnswerPoints = 7),
  LEVEL_FIVE(level = 5, correctAnswerPoints = 9),
  ;

  companion object {
    val initial = LEVEL_ONE

    fun fromLevel(level: Int): GameDifficulty = requireNotNull(fromLevelOrNull(level)) {
      "Unsupported game difficulty: $level"
    }

    fun fromLevelOrNull(level: Int): GameDifficulty? = entries.find { it.level == level }
  }
}
