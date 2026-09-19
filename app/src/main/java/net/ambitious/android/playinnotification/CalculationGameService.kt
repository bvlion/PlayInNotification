package net.ambitious.android.playinnotification

internal class CalculationGameService : GameSessionService<CalculationQuestion>() {
  override val gameType = GameType.CALCULATION
  override val wakeLockName = "calculation-game"

  override fun createNextQuestion(
    difficulty: GameDifficulty,
    askedQuestions: List<CalculationQuestion>,
  ): CalculationQuestion {
    return CalculationQuestionGenerator.create(
      difficulty = difficulty,
      askedQuestions = askedQuestions,
    )
  }

  override fun questionText(question: CalculationQuestion): String {
    return if (
      question.missingOperandIndex != null &&
      question.thirdOperand != null &&
      question.secondOperator != null
    ) {
      when (question.missingOperandIndex) {
        MISSING_LEFT_OPERAND_INDEX -> getString(
          R.string.calculation_missing_left_operand_question,
          question.operator.symbol,
          question.rightOperand,
          question.secondOperator.symbol,
          question.thirdOperand,
          question.calculationResult,
        )
        MISSING_RIGHT_OPERAND_INDEX -> getString(
          R.string.calculation_missing_right_operand_question,
          question.leftOperand,
          question.operator.symbol,
          question.secondOperator.symbol,
          question.thirdOperand,
          question.calculationResult,
        )
        MISSING_THIRD_OPERAND_INDEX -> getString(
          R.string.calculation_missing_third_operand_question,
          question.leftOperand,
          question.operator.symbol,
          question.rightOperand,
          question.secondOperator.symbol,
          question.calculationResult,
        )
        else -> error("欠損位置が不正です")
      }
    } else if (question.thirdOperand == null || question.secondOperator == null) {
      getString(
        R.string.calculation_question,
        question.leftOperand,
        question.operator.symbol,
        question.rightOperand,
      )
    } else {
      getString(
        R.string.calculation_compound_question,
        question.leftOperand,
        question.operator.symbol,
        question.rightOperand,
        question.secondOperator.symbol,
        question.thirdOperand,
      )
    }
  }

  override fun answerChoices(question: CalculationQuestion): List<String> =
    question.choices.map(Int::toString)

  override fun correctAnswer(question: CalculationQuestion): String =
    question.correctAnswer.toString()
}
