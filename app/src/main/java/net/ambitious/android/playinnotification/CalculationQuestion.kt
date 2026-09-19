package net.ambitious.android.playinnotification

import kotlin.random.Random

private const val ADDITIVE_OPERATOR_PRECEDENCE = 1
private const val MULTIPLICATIVE_OPERATOR_PRECEDENCE = 2
private const val ANSWER_CHOICE_COUNT = 3

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
    0 -> leftOperand
    1 -> rightOperand
    2 -> requireNotNull(thirdOperand)
    else -> calculationResult
  }

  private fun hasSameExpression(other: CalculationQuestion): Boolean =
    leftOperand == other.leftOperand &&
      rightOperand == other.rightOperand &&
      operator == other.operator &&
      thirdOperand == other.thirdOperand &&
      secondOperator == other.secondOperator &&
      missingOperandIndex == other.missingOperandIndex

  companion object {
    fun create(
      difficulty: GameDifficulty = GameDifficulty.initial,
      askedQuestions: Collection<CalculationQuestion> = emptyList(),
      random: Random = Random.Default,
    ): CalculationQuestion {
      return when (difficulty) {
        GameDifficulty.LEVEL_ONE,
        GameDifficulty.LEVEL_TWO,
        GameDifficulty.LEVEL_THREE -> createBasicQuestion(difficulty, askedQuestions, random)
        GameDifficulty.LEVEL_FOUR,
        GameDifficulty.LEVEL_FIVE -> createCompoundQuestion(difficulty, askedQuestions, random)
      }
    }

    private fun createBasicQuestion(
      difficulty: GameDifficulty,
      askedQuestions: Collection<CalculationQuestion>,
      random: Random,
    ): CalculationQuestion {
      var question: CalculationQuestion? = null
      while (question == null) {
        val operator = when {
          difficulty == GameDifficulty.LEVEL_THREE && random.nextBoolean() -> {
            CalculationOperator.DIVISION
          }
          (difficulty == GameDifficulty.LEVEL_TWO ||
            difficulty == GameDifficulty.LEVEL_THREE) &&
            random.nextBoolean() -> CalculationOperator.MULTIPLICATION
          random.nextBoolean() -> CalculationOperator.ADDITION
          else -> CalculationOperator.SUBTRACTION
        }
        val operandRange = if (operator.isMultiplicative) 2..9 else 1..9
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
        val correctAnswer = operator.calculate(leftOperand, rightOperand)
        val wrongAnswerRange = if (difficulty == GameDifficulty.LEVEL_ONE) 0..18 else 0..81
        val wrongAnswers = wrongAnswerRange
          .filter { it != correctAnswer }
          .shuffled(random)
          .take(ANSWER_CHOICE_COUNT - 1)
        val candidate = CalculationQuestion(
          leftOperand = leftOperand,
          rightOperand = rightOperand,
          operator = operator,
          choices = (wrongAnswers + correctAnswer).shuffled(random),
        )
        val isExcludedZeroAnswer =
          difficulty == GameDifficulty.LEVEL_ONE && candidate.correctAnswer == 0
        if (!isExcludedZeroAnswer && askedQuestions.none(candidate::hasSameExpression)) {
          question = candidate
        }
      }
      return question
    }

    private fun createCompoundQuestion(
      difficulty: GameDifficulty,
      askedQuestions: Collection<CalculationQuestion>,
      random: Random,
    ): CalculationQuestion {
      var question: CalculationQuestion? = null
      while (question == null) {
        val operator = CalculationOperator.entries.random(random)
        val secondOperator = CalculationOperator.entries
          .filter { it != operator }
          .random(random)
        val leftOperand = (1..9).random(random)
        val rightOperand = (1..9).random(random)
        val thirdOperand = (1..9).random(random)
        val hasOneAsMultiplicativeOperand =
          (operator.isMultiplicative && (leftOperand == 1 || rightOperand == 1)) ||
          (secondOperator.isMultiplicative && (rightOperand == 1 || thirdOperand == 1))

        val calculationResult = when {
          hasOneAsMultiplicativeOperand -> null
          secondOperator.precedence > operator.precedence -> {
            if (
              secondOperator == CalculationOperator.DIVISION &&
              rightOperand % thirdOperand != 0
            ) {
              null
            } else {
              operator.calculate(
                leftOperand,
                secondOperator.calculate(rightOperand, thirdOperand),
              )
            }
          }
          operator == CalculationOperator.DIVISION && leftOperand % rightOperand != 0 -> null
          else -> {
            val leftResult = operator.calculate(leftOperand, rightOperand)
            if (
              secondOperator == CalculationOperator.DIVISION &&
              leftResult % thirdOperand != 0
            ) {
              null
            } else {
              secondOperator.calculate(leftResult, thirdOperand)
            }
          }
        }

        if (calculationResult != null && calculationResult >= 0) {
          val missingOperandIndex = if (difficulty == GameDifficulty.LEVEL_FIVE) {
            (0..2).random(random)
          } else {
            null
          }
          val correctAnswer = when (missingOperandIndex) {
            0 -> leftOperand
            1 -> rightOperand
            2 -> thirdOperand
            else -> calculationResult
          }
          val wrongAnswerRange = if (difficulty == GameDifficulty.LEVEL_FIVE) 0..9 else 0..90
          val wrongAnswers = wrongAnswerRange
            .filter { it != correctAnswer }
            .shuffled(random)
            .take(ANSWER_CHOICE_COUNT - 1)
          val candidate = CalculationQuestion(
            leftOperand = leftOperand,
            rightOperand = rightOperand,
            operator = operator,
            choices = (wrongAnswers + correctAnswer).shuffled(random),
            thirdOperand = thirdOperand,
            secondOperator = secondOperator,
            missingOperandIndex = missingOperandIndex,
          )
          if (askedQuestions.none(candidate::hasSameExpression)) {
            question = candidate
          }
        }
      }
      return question
    }
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
