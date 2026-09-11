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
}
