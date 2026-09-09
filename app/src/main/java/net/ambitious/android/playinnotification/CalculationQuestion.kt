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
    CalculationOperator.MULTIPLICATION -> leftOperand * rightOperand
    CalculationOperator.DIVISION -> leftOperand / rightOperand
  }

  companion object {
    fun create(
      random: Random = Random.Default,
      difficulty: Int = 1,
      previousQuestion: CalculationQuestion? = null,
    ): CalculationQuestion {
      var question: CalculationQuestion
      do {
        val operator = if (difficulty == 3 && random.nextBoolean()) {
          CalculationOperator.DIVISION
        } else if (difficulty in 2..3 && random.nextBoolean()) {
          CalculationOperator.MULTIPLICATION
        } else if (random.nextBoolean()) {
          CalculationOperator.ADDITION
        } else {
          CalculationOperator.SUBTRACTION
        }
        val operandRange = if (
          operator == CalculationOperator.MULTIPLICATION ||
          operator == CalculationOperator.DIVISION
        ) {
          2..9
        } else {
          if (difficulty in 2..3) 1..9 else 0..9
        }
        val firstOperand = operandRange.random(random)
        val secondOperand = operandRange.random(random)
        val leftOperand = when (operator) {
          CalculationOperator.SUBTRACTION -> maxOf(firstOperand, secondOperand)
          CalculationOperator.DIVISION -> firstOperand * secondOperand
          else -> firstOperand
        }
        val rightOperand = if (operator == CalculationOperator.SUBTRACTION) {
          minOf(firstOperand, secondOperand)
        } else {
          secondOperand
        }
        val correctAnswer = when (operator) {
          CalculationOperator.ADDITION -> leftOperand + rightOperand
          CalculationOperator.SUBTRACTION -> leftOperand - rightOperand
          CalculationOperator.MULTIPLICATION -> leftOperand * rightOperand
          CalculationOperator.DIVISION -> leftOperand / rightOperand
        }
        val wrongAnswerRange = if (difficulty in 2..3) 0..81 else 0..18
        val wrongAnswers = wrongAnswerRange
          .filter { it != correctAnswer }
          .shuffled(random)
          .take(2)

        question = CalculationQuestion(
          leftOperand = leftOperand,
          rightOperand = rightOperand,
          operator = operator,
          choices = (wrongAnswers + correctAnswer).shuffled(random),
        )
      } while (
        difficulty in 2..3 &&
        question.leftOperand == previousQuestion?.leftOperand &&
        question.rightOperand == previousQuestion.rightOperand &&
        question.operator == previousQuestion.operator
      )

      return question
    }
  }
}

internal enum class CalculationOperator(val symbol: String) {
  ADDITION("+"),
  SUBTRACTION("−"),
  MULTIPLICATION("×"),
  DIVISION("÷"),
}
