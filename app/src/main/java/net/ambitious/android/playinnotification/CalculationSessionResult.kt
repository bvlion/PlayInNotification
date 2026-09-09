package net.ambitious.android.playinnotification

internal data class CalculationAnswerResult(
  val isCorrect: Boolean,
  val earnedPoints: Int,
) {
  companion object {
    fun create(
      question: CalculationQuestion,
      answer: Int,
      questionDifficulty: Int,
    ): CalculationAnswerResult {
      val isCorrect = answer == question.correctAnswer
      return CalculationAnswerResult(
        isCorrect = isCorrect,
        earnedPoints = (if (isCorrect) CORRECT_ANSWER_POINTS else INCORRECT_ANSWER_POINTS) *
          questionDifficulty,
      )
    }

    private const val CORRECT_ANSWER_POINTS = 3
    private const val INCORRECT_ANSWER_POINTS = 1
  }
}

internal data class CalculationSessionResult(
  val answerCount: Int = 0,
  val earnedPoints: Int = 0,
) {
  fun addAnswerResult(answerResult: CalculationAnswerResult): CalculationSessionResult = copy(
    answerCount = answerCount + 1,
    earnedPoints = earnedPoints + answerResult.earnedPoints,
  )
}
