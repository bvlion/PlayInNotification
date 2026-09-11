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
        earnedPoints = (if (isCorrect) CORRECT_ANSWER_POINTS else INCORRECT_ANSWER_POINTS) *
          questionDifficulty,
      )
    }

    private const val CORRECT_ANSWER_POINTS = 3
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
