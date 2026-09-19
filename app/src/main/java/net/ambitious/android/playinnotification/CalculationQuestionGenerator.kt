package net.ambitious.android.playinnotification

import kotlin.random.Random

private const val ANSWER_CHOICE_COUNT = 3

private data class CalculationCandidate(
  val leftOperand: Int,
  val rightOperand: Int,
  val operator: CalculationOperator,
  val calculationResult: Int,
  val thirdOperand: Int? = null,
  val secondOperator: CalculationOperator? = null,
  val missingOperandIndex: Int? = null,
  val selectionWeight: Int = 1,
) {
  val correctAnswer: Int = when (missingOperandIndex) {
    0 -> leftOperand
    1 -> rightOperand
    2 -> requireNotNull(thirdOperand)
    else -> calculationResult
  }

  fun hasSameExpression(question: CalculationQuestion): Boolean =
    leftOperand == question.leftOperand &&
      rightOperand == question.rightOperand &&
      operator == question.operator &&
      thirdOperand == question.thirdOperand &&
      secondOperator == question.secondOperator &&
      missingOperandIndex == question.missingOperandIndex
}

internal object CalculationQuestionGenerator {
  private val candidatesByDifficulty by lazy {
    GameDifficulty.entries.associateWith { difficulty ->
      when (difficulty) {
        GameDifficulty.LEVEL_ONE,
        GameDifficulty.LEVEL_TWO,
        GameDifficulty.LEVEL_THREE -> basicCandidates(difficulty)
        GameDifficulty.LEVEL_FOUR,
        GameDifficulty.LEVEL_FIVE -> compoundCandidates(difficulty)
      }
    }
  }

  fun create(
    difficulty: GameDifficulty = GameDifficulty.initial,
    askedQuestions: Collection<CalculationQuestion> = emptyList(),
    random: Random = Random.Default,
  ): CalculationQuestion {
    val candidates = candidatesByDifficulty.getValue(difficulty)
    val availableCandidates = if (askedQuestions.isEmpty()) {
      candidates
    } else {
      candidates.filter { candidate -> askedQuestions.none(candidate::hasSameExpression) }
    }
    val selectedCandidate = selectCandidate(availableCandidates, random)
    return completeQuestion(selectedCandidate, difficulty, random)
  }

  private fun basicCandidates(difficulty: GameDifficulty): List<CalculationCandidate> = buildList {
    val operators = when (difficulty) {
      GameDifficulty.LEVEL_ONE -> listOf(
        CalculationOperator.ADDITION,
        CalculationOperator.SUBTRACTION,
      )
      GameDifficulty.LEVEL_TWO -> listOf(
        CalculationOperator.ADDITION,
        CalculationOperator.SUBTRACTION,
        CalculationOperator.MULTIPLICATION,
      )
      GameDifficulty.LEVEL_THREE -> CalculationOperator.entries
      else -> error("基本計算の難易度ではありません")
    }
    for (operator in operators) {
      val operandRange = if (operator.isMultiplicative) 2..9 else 1..9
      for (firstOperand in operandRange) {
        for (secondOperand in operandRange) {
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
          val calculationResult = operator.calculate(leftOperand, rightOperand)
          if (difficulty != GameDifficulty.LEVEL_ONE || calculationResult != 0) {
            add(
              CalculationCandidate(
                leftOperand = leftOperand,
                rightOperand = rightOperand,
                operator = operator,
                calculationResult = calculationResult,
                selectionWeight = basicSelectionWeight(difficulty, operator),
              ),
            )
          }
        }
      }
    }
  }

  // 元の演算子抽選確率を値の組数（加減算81通り、乗除算64通り）で割った比を整数化する。
  private fun basicSelectionWeight(
    difficulty: GameDifficulty,
    operator: CalculationOperator,
  ): Int = when (difficulty) {
    GameDifficulty.LEVEL_ONE -> 1
    GameDifficulty.LEVEL_TWO -> if (operator.isMultiplicative) 81 else 32
    GameDifficulty.LEVEL_THREE -> when (operator) {
      CalculationOperator.DIVISION -> 162
      CalculationOperator.MULTIPLICATION -> 81
      else -> 32
    }
    else -> error("基本計算の難易度ではありません")
  }

  private fun compoundCandidates(difficulty: GameDifficulty): List<CalculationCandidate> =
    buildList {
      val missingOperandIndexes: List<Int?> = if (difficulty == GameDifficulty.LEVEL_FIVE) {
        listOf(0, 1, 2)
      } else {
        listOf(null)
      }
      for (operator in CalculationOperator.entries) {
        for (secondOperator in CalculationOperator.entries.filter { it != operator }) {
          for (leftOperand in 1..9) {
            for (rightOperand in 1..9) {
              for (thirdOperand in 1..9) {
                val calculationResult = compoundCalculationResultOrNull(
                  operator,
                  secondOperator,
                  leftOperand,
                  rightOperand,
                  thirdOperand,
                )
                if (calculationResult != null) {
                  for (missingOperandIndex in missingOperandIndexes) {
                    add(
                      CalculationCandidate(
                        leftOperand = leftOperand,
                        rightOperand = rightOperand,
                        operator = operator,
                        calculationResult = calculationResult,
                        thirdOperand = thirdOperand,
                        secondOperator = secondOperator,
                        missingOperandIndex = missingOperandIndex,
                      ),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

  private fun compoundCalculationResultOrNull(
    operator: CalculationOperator,
    secondOperator: CalculationOperator,
    leftOperand: Int,
    rightOperand: Int,
    thirdOperand: Int,
  ): Int? {
    if (
      (operator.isMultiplicative && (leftOperand == 1 || rightOperand == 1)) ||
      (secondOperator.isMultiplicative && (rightOperand == 1 || thirdOperand == 1))
    ) {
      return null
    }
    val calculationResult = if (secondOperator.precedence > operator.precedence) {
      if (secondOperator == CalculationOperator.DIVISION && rightOperand % thirdOperand != 0) {
        return null
      }
      operator.calculate(leftOperand, secondOperator.calculate(rightOperand, thirdOperand))
    } else {
      if (operator == CalculationOperator.DIVISION && leftOperand % rightOperand != 0) {
        return null
      }
      val leftResult = operator.calculate(leftOperand, rightOperand)
      if (secondOperator == CalculationOperator.DIVISION && leftResult % thirdOperand != 0) {
        return null
      }
      secondOperator.calculate(leftResult, thirdOperand)
    }
    return calculationResult.takeIf { it >= 0 }
  }

  private fun selectCandidate(
    candidates: List<CalculationCandidate>,
    random: Random,
  ): CalculationCandidate {
    var remainingWeight = random.nextInt(candidates.sumOf(CalculationCandidate::selectionWeight))
    for (candidate in candidates) {
      remainingWeight -= candidate.selectionWeight
      if (remainingWeight < 0) return candidate
    }
    error("出題可能な計算問題の候補を選べませんでした")
  }

  private fun completeQuestion(
    candidate: CalculationCandidate,
    difficulty: GameDifficulty,
    random: Random,
  ): CalculationQuestion {
    val wrongAnswerRange = when (difficulty) {
      GameDifficulty.LEVEL_ONE -> 0..18
      GameDifficulty.LEVEL_TWO,
      GameDifficulty.LEVEL_THREE -> 0..81
      GameDifficulty.LEVEL_FOUR -> 0..90
      GameDifficulty.LEVEL_FIVE -> 0..9
    }
    val wrongAnswers = wrongAnswerRange
      .filter { it != candidate.correctAnswer }
      .shuffled(random)
      .take(ANSWER_CHOICE_COUNT - 1)
    return CalculationQuestion(
      leftOperand = candidate.leftOperand,
      rightOperand = candidate.rightOperand,
      operator = candidate.operator,
      choices = (wrongAnswers + candidate.correctAnswer).shuffled(random),
      thirdOperand = candidate.thirdOperand,
      secondOperator = candidate.secondOperator,
      missingOperandIndex = candidate.missingOperandIndex,
    )
  }
}
