package net.ambitious.android.playinnotification

import java.time.LocalDate

internal data class GameStatistics(
  val answerCount: Long = 0,
  val playCount: Long = 0,
  val earnedPoints: Long = 0,
  val streakDayCount: Int = 0,
  val lastCompletedSessionDate: LocalDate? = null,
  val bestPointsByGame: Map<String, Long> = emptyMap(),
) {
  val growthLevel: Int
    get() {
      val earnedPointFactor = earnedPoints / POINTS_PER_GROWTH_LEVEL_STEP
      var lowestGrowthLevel = 1
      var highestGrowthLevel = Int.MAX_VALUE

      while (lowestGrowthLevel < highestGrowthLevel) {
        val growthLevel = lowestGrowthLevel + (highestGrowthLevel - lowestGrowthLevel + 1) / 2
        val requiredPointFactor = growthLevel.toLong() * (growthLevel - 1)
        if (requiredPointFactor <= earnedPointFactor) {
          lowestGrowthLevel = growthLevel
        } else {
          highestGrowthLevel = growthLevel - 1
        }
      }

      return lowestGrowthLevel
    }

  fun addCompletedSession(
    sessionResult: GameSessionResult,
    gameGenre: String,
    difficulty: Int,
    completedSessionDate: LocalDate,
  ): GameStatistics {
    val gameKey = "$gameGenre:$difficulty"
    val currentBestPoints = bestPointsByGame[gameKey] ?: 0
    val nextStreakDayCount = when {
      lastCompletedSessionDate == completedSessionDate -> streakDayCount
      lastCompletedSessionDate == completedSessionDate.minusDays(1) -> streakDayCount + 1
      else -> 1
    }

    return copy(
      answerCount = answerCount + sessionResult.answerCount,
      playCount = playCount + 1,
      earnedPoints = earnedPoints + sessionResult.earnedPoints,
      streakDayCount = nextStreakDayCount,
      lastCompletedSessionDate = completedSessionDate,
      bestPointsByGame = bestPointsByGame + (gameKey to maxOf(currentBestPoints, sessionResult.earnedPoints.toLong())),
    )
  }

  private companion object {
    const val POINTS_PER_GROWTH_LEVEL_STEP = 50
  }
}
