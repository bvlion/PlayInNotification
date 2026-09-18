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
      questionDifficulty: GameDifficulty,
    ): GameAnswerResult {
      return GameAnswerResult(
        question = question,
        selectedAnswer = selectedAnswer,
        isCorrect = isCorrect,
        earnedPoints = if (isCorrect) {
          questionDifficulty.correctAnswerPoints
        } else {
          INCORRECT_ANSWER_POINTS
        },
      )
    }

    private const val INCORRECT_ANSWER_POINTS = 1
  }
}

internal data class GameSessionResult(
  val answerResults: List<GameAnswerResult> = emptyList(),
) {
  val answerCount: Int
    get() = answerResults.size

  val correctAnswerCount: Int
    get() = answerResults.count(GameAnswerResult::isCorrect)

  val incorrectAnswerCount: Int
    get() = answerCount - correctAnswerCount

  val earnedPoints: Int
    get() = answerResults.sumOf(GameAnswerResult::earnedPoints)

  fun addAnswerResult(answerResult: GameAnswerResult): GameSessionResult = copy(
    answerResults = answerResults + answerResult,
  )
}
