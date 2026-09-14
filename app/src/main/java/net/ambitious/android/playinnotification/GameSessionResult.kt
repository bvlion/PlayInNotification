package net.ambitious.android.playinnotification

internal data class GameAnswerResult(
  val question: String,
  val selectedAnswer: String,
  val isCorrect: Boolean,
  val earnedPoints: Int,
) {
  companion object {
    fun create(
      question: String,
      selectedAnswer: String,
      isCorrect: Boolean,
      questionDifficulty: Int,
    ): GameAnswerResult {
      return GameAnswerResult(
        question = question,
        selectedAnswer = selectedAnswer,
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
  val correctAnswerCount: Int = 0,
  val incorrectAnswerCount: Int = 0,
  val earnedPoints: Int = 0,
  val answerResults: List<GameAnswerResult> = emptyList(),
) {
  fun addAnswerResult(answerResult: GameAnswerResult): GameSessionResult = copy(
    answerCount = answerCount + 1,
    correctAnswerCount = correctAnswerCount + if (answerResult.isCorrect) 1 else 0,
    incorrectAnswerCount = incorrectAnswerCount + if (answerResult.isCorrect) 0 else 1,
    earnedPoints = earnedPoints + answerResult.earnedPoints,
    answerResults = answerResults + answerResult,
  )
}
