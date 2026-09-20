package net.ambitious.android.playinnotification

import java.time.LocalDate

internal data class GameStatistics(
  val answerCount: Long = 0,
  val playCount: Long = 0,
  val earnedPoints: Long = 0,
  val totalDayCount: Int = 0,
  val streakDayCount: Int = 0,
  val lastCompletedSessionDate: LocalDate? = null,
  val bestPointsByGame: Map<GameType, Long> = emptyMap(),
) {
  val growthLevel: Int
    get() {
      val earnedPointFactor = earnedPoints / POINTS_PER_GROWTH_LEVEL_STEP
      var lowestGrowthLevel = INITIAL_GROWTH_LEVEL
      var highestGrowthLevel = Int.MAX_VALUE

      while (lowestGrowthLevel < highestGrowthLevel) {
        val growthLevelRangeSize = highestGrowthLevel - lowestGrowthLevel + 1
        val growthLevel = lowestGrowthLevel + growthLevelRangeSize / 2
        val previousGrowthLevel = growthLevel - 1
        val requiredPointFactor = growthLevel.toLong() * previousGrowthLevel
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
    gameType: GameType,
    completedSessionDate: LocalDate,
  ): GameStatistics {
    val currentBestPoints = bestPointsByGame[gameType] ?: 0
    val nextTotalDayCount = if (lastCompletedSessionDate == completedSessionDate) {
      totalDayCount
    } else {
      totalDayCount + 1
    }
    val nextStreakDayCount = when {
      lastCompletedSessionDate == completedSessionDate -> streakDayCount
      lastCompletedSessionDate == completedSessionDate.minusDays(PREVIOUS_CALENDAR_DAY_OFFSET) ->
        streakDayCount + 1
      else -> INITIAL_STREAK_DAY_COUNT
    }

    return copy(
      answerCount = answerCount + sessionResult.answerCount,
      playCount = playCount + 1,
      earnedPoints = earnedPoints + sessionResult.earnedPoints,
      totalDayCount = nextTotalDayCount,
      streakDayCount = nextStreakDayCount,
      lastCompletedSessionDate = completedSessionDate,
      bestPointsByGame = bestPointsByGame + (
        gameType to maxOf(currentBestPoints, sessionResult.earnedPoints.toLong())
      ),
    )
  }

  private companion object {
    const val POINTS_PER_GROWTH_LEVEL_STEP = 50
    private const val INITIAL_GROWTH_LEVEL = 1
    private const val INITIAL_STREAK_DAY_COUNT = 1
    private const val PREVIOUS_CALENDAR_DAY_OFFSET = 1L
  }
}
