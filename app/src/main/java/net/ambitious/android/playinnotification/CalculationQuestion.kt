package net.ambitious.android.playinnotification

import kotlin.random.Random

internal data class CalculationQuestion(
  val leftOperand: Int,
  val rightOperand: Int,
  val operator: CalculationOperator,
  val choices: List<Int>,
) {
  val correctAnswer = when (operator) {
    CalculationOperator.ADDITION -> leftOperand + rightOperand
    CalculationOperator.SUBTRACTION -> leftOperand - rightOperand
  }

  companion object {
    fun create(random: Random = Random.Default): CalculationQuestion {
      val firstOperand = random.nextInt(10)
      val secondOperand = random.nextInt(10)
      val operator = if (random.nextBoolean()) {
        CalculationOperator.ADDITION
      } else {
        CalculationOperator.SUBTRACTION
      }
      val leftOperand = if (operator == CalculationOperator.SUBTRACTION) {
        maxOf(firstOperand, secondOperand)
      } else {
        firstOperand
      }
      val rightOperand = if (operator == CalculationOperator.SUBTRACTION) {
        minOf(firstOperand, secondOperand)
      } else {
        secondOperand
      }
      val correctAnswer = when (operator) {
        CalculationOperator.ADDITION -> leftOperand + rightOperand
        CalculationOperator.SUBTRACTION -> leftOperand - rightOperand
      }
      val wrongAnswers = (0..18)
        .filter { it != correctAnswer }
        .shuffled(random)
        .take(2)

      return CalculationQuestion(
        leftOperand = leftOperand,
        rightOperand = rightOperand,
        operator = operator,
        choices = (wrongAnswers + correctAnswer).shuffled(random),
      )
    }
  }
}

internal enum class CalculationOperator(val symbol: String) {
  ADDITION("+"),
  SUBTRACTION("−"),
}
