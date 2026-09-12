package net.ambitious.android.playinnotification

internal data class GameAnswerResult(
  val isCorrect: Boolean,
  val earnedPoints: Int,
) {
  companion object {
    fun create(
      isCorrect: Boolean,
      questionDifficulty: Int,
    ): GameAnswerResult {
      return GameAnswerResult(
        isCorrect = isCorrect,
        earnedPoints = if (isCorrect) {
          when (questionDifficulty) {
            1 -> 3
            2 -> 4
            3 -> 6
            4 -> 7
            5 -> 9
            else -> error("Unsupported question difficulty: $questionDifficulty")
          }
        } else {
          INCORRECT_ANSWER_POINTS
        },
      )
    }

    private const val INCORRECT_ANSWER_POINTS = 1
  }
}

internal data class GameSessionResult(
  val answerCount: Int = 0,
  val earnedPoints: Int = 0,
) {
  fun addAnswerResult(answerResult: GameAnswerResult): GameSessionResult = copy(
    answerCount = answerCount + 1,
    earnedPoints = earnedPoints + answerResult.earnedPoints,
  )
}
