package net.ambitious.android.playinnotification

private const val ADDITIVE_OPERATOR_PRECEDENCE = 1
private const val MULTIPLICATIVE_OPERATOR_PRECEDENCE = 2
internal const val MISSING_LEFT_OPERAND_INDEX = 0
internal const val MISSING_RIGHT_OPERAND_INDEX = 1
internal const val MISSING_THIRD_OPERAND_INDEX = 2

internal data class CalculationQuestion(
  val leftOperand: Int,
  val rightOperand: Int,
  val operator: CalculationOperator,
  val choices: List<Int>,
  val thirdOperand: Int? = null,
  val secondOperator: CalculationOperator? = null,
  val missingOperandIndex: Int? = null,
) {
  val calculationResult = if (thirdOperand == null || secondOperator == null) {
    operator.calculate(leftOperand, rightOperand)
  } else if (secondOperator.precedence > operator.precedence) {
    operator.calculate(
      leftOperand,
      secondOperator.calculate(rightOperand, thirdOperand),
    )
  } else {
    secondOperator.calculate(
      operator.calculate(leftOperand, rightOperand),
      thirdOperand,
    )
  }

  val correctAnswer = when (missingOperandIndex) {
    MISSING_LEFT_OPERAND_INDEX -> leftOperand
    MISSING_RIGHT_OPERAND_INDEX -> rightOperand
    MISSING_THIRD_OPERAND_INDEX -> requireNotNull(thirdOperand)
    else -> calculationResult
  }
}

internal enum class CalculationOperator(
  val symbol: String,
  val precedence: Int,
) {
  ADDITION("+", ADDITIVE_OPERATOR_PRECEDENCE),
  SUBTRACTION("−", ADDITIVE_OPERATOR_PRECEDENCE),
  MULTIPLICATION("×", MULTIPLICATIVE_OPERATOR_PRECEDENCE),
  DIVISION("÷", MULTIPLICATIVE_OPERATOR_PRECEDENCE),
  ;

  val isMultiplicative: Boolean
    get() = precedence == MULTIPLICATIVE_OPERATOR_PRECEDENCE

  fun calculate(leftOperand: Int, rightOperand: Int): Int = when (this) {
    ADDITION -> leftOperand + rightOperand
    SUBTRACTION -> leftOperand - rightOperand
    MULTIPLICATION -> leftOperand * rightOperand
    DIVISION -> leftOperand / rightOperand
  }
}
