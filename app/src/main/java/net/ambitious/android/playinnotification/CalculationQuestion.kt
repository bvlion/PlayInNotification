package net.ambitious.android.playinnotification

import kotlin.random.Random

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
    when (operator) {
      CalculationOperator.ADDITION -> leftOperand + rightOperand
      CalculationOperator.SUBTRACTION -> leftOperand - rightOperand
      CalculationOperator.MULTIPLICATION -> leftOperand * rightOperand
      CalculationOperator.DIVISION -> leftOperand / rightOperand
    }
  } else if (secondOperator.precedence > operator.precedence) {
    val rightResult = when (secondOperator) {
      CalculationOperator.ADDITION -> rightOperand + thirdOperand
      CalculationOperator.SUBTRACTION -> rightOperand - thirdOperand
      CalculationOperator.MULTIPLICATION -> rightOperand * thirdOperand
      CalculationOperator.DIVISION -> rightOperand / thirdOperand
    }
    when (operator) {
      CalculationOperator.ADDITION -> leftOperand + rightResult
      CalculationOperator.SUBTRACTION -> leftOperand - rightResult
      CalculationOperator.MULTIPLICATION -> leftOperand * rightResult
      CalculationOperator.DIVISION -> leftOperand / rightResult
    }
  } else {
    val leftResult = when (operator) {
      CalculationOperator.ADDITION -> leftOperand + rightOperand
      CalculationOperator.SUBTRACTION -> leftOperand - rightOperand
      CalculationOperator.MULTIPLICATION -> leftOperand * rightOperand
      CalculationOperator.DIVISION -> leftOperand / rightOperand
    }
    when (secondOperator) {
      CalculationOperator.ADDITION -> leftResult + thirdOperand
      CalculationOperator.SUBTRACTION -> leftResult - thirdOperand
      CalculationOperator.MULTIPLICATION -> leftResult * thirdOperand
      CalculationOperator.DIVISION -> leftResult / thirdOperand
    }
  }
  val correctAnswer = when (missingOperandIndex) {
    0 -> leftOperand
    1 -> rightOperand
    2 -> requireNotNull(thirdOperand)
    else -> calculationResult
  }

  companion object {
    fun create(
      random: Random = Random.Default,
      difficulty: Int = 1,
      previousQuestion: CalculationQuestion? = null,
    ): CalculationQuestion {
      if (difficulty in 4..5) {
        while (true) {
          val operator = CalculationOperator.entries.random(random)
          val secondOperator = CalculationOperator.entries
            .filter { it != operator }
            .random(random)
          val leftOperand = (1..9).random(random)
          val rightOperand = (1..9).random(random)
          val thirdOperand = (1..9).random(random)
          if (
            (operator.precedence == MULTIPLICATIVE_PRECEDENCE &&
              (leftOperand == 1 || rightOperand == 1)) ||
            (secondOperator.precedence == MULTIPLICATIVE_PRECEDENCE &&
              (rightOperand == 1 || thirdOperand == 1))
          ) {
            continue
          }

          val correctAnswer = if (secondOperator.precedence > operator.precedence) {
            if (
              secondOperator == CalculationOperator.DIVISION &&
              rightOperand % thirdOperand != 0
            ) {
              continue
            }
            val rightResult = when (secondOperator) {
              CalculationOperator.ADDITION -> rightOperand + thirdOperand
              CalculationOperator.SUBTRACTION -> rightOperand - thirdOperand
              CalculationOperator.MULTIPLICATION -> rightOperand * thirdOperand
              CalculationOperator.DIVISION -> rightOperand / thirdOperand
            }
            when (operator) {
              CalculationOperator.ADDITION -> leftOperand + rightResult
              CalculationOperator.SUBTRACTION -> leftOperand - rightResult
              CalculationOperator.MULTIPLICATION -> leftOperand * rightResult
              CalculationOperator.DIVISION -> leftOperand / rightResult
            }
          } else {
            if (operator == CalculationOperator.DIVISION && leftOperand % rightOperand != 0) {
              continue
            }
            val leftResult = when (operator) {
              CalculationOperator.ADDITION -> leftOperand + rightOperand
              CalculationOperator.SUBTRACTION -> leftOperand - rightOperand
              CalculationOperator.MULTIPLICATION -> leftOperand * rightOperand
              CalculationOperator.DIVISION -> leftOperand / rightOperand
            }
            if (
              secondOperator == CalculationOperator.DIVISION &&
              leftResult % thirdOperand != 0
            ) {
              continue
            }
            when (secondOperator) {
              CalculationOperator.ADDITION -> leftResult + thirdOperand
              CalculationOperator.SUBTRACTION -> leftResult - thirdOperand
              CalculationOperator.MULTIPLICATION -> leftResult * thirdOperand
              CalculationOperator.DIVISION -> leftResult / thirdOperand
            }
          }
          if (correctAnswer < 0) {
            continue
          }
          val missingOperandIndex = if (difficulty == 5) (0..2).random(random) else null
          val answer = when (missingOperandIndex) {
            0 -> leftOperand
            1 -> rightOperand
            2 -> thirdOperand
            else -> correctAnswer
          }
          val wrongAnswerRange = if (difficulty == 5) 0..9 else 0..90
          val wrongAnswers = wrongAnswerRange
            .filter { it != answer }
            .shuffled(random)
            .take(2)
          val question = CalculationQuestion(
            leftOperand = leftOperand,
            rightOperand = rightOperand,
            operator = operator,
            choices = (wrongAnswers + answer).shuffled(random),
            thirdOperand = thirdOperand,
            secondOperator = secondOperator,
            missingOperandIndex = missingOperandIndex,
          )
          if (
            question.leftOperand == previousQuestion?.leftOperand &&
            question.rightOperand == previousQuestion.rightOperand &&
            question.operator == previousQuestion.operator &&
            question.thirdOperand == previousQuestion.thirdOperand &&
            question.secondOperator == previousQuestion.secondOperator &&
            question.missingOperandIndex == previousQuestion.missingOperandIndex
          ) {
            continue
          }
          return question
        }
      }

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
          1..9
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
        (difficulty == 1 && question.correctAnswer == 0) ||
        (difficulty in 1..3 &&
          question.leftOperand == previousQuestion?.leftOperand &&
          question.rightOperand == previousQuestion.rightOperand &&
          question.operator == previousQuestion.operator)
      )

      return question
    }

    private const val MULTIPLICATIVE_PRECEDENCE = 2
  }
}

internal enum class CalculationOperator(
  val symbol: String,
  val precedence: Int,
) {
  ADDITION("+", 1),
  SUBTRACTION("−", 1),
  MULTIPLICATION("×", 2),
  DIVISION("÷", 2),
}
