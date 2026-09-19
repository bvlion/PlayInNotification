package net.ambitious.android.playinnotification

import kotlin.random.Random

private const val CORRECT_ANSWER_COUNT = 1
private const val WRONG_ANSWER_COUNT = 2
private const val ANSWER_CHOICE_COUNT = CORRECT_ANSWER_COUNT + WRONG_ANSWER_COUNT
private const val UNIFORM_SELECTION_WEIGHT_PER_OPERAND_PAIR = 1
private const val ADDITIVE_SELECTION_WEIGHT_PER_OPERAND_PAIR = 32
private const val MULTIPLICATION_SELECTION_WEIGHT_PER_OPERAND_PAIR = 81
private const val DIVISION_SELECTION_WEIGHT_PER_OPERAND_PAIR = 162
private const val LEVEL_ONE_EXCLUDED_CORRECT_ANSWER = 0
private const val EXCLUDED_MULTIPLICATIVE_OPERAND = 1
private const val EXACT_DIVISION_REMAINDER = 0
private const val MINIMUM_COMPOUND_RESULT = 0
private val BASIC_ADDITIVE_OPERAND_RANGE = 1..9
private val BASIC_MULTIPLICATIVE_FACTOR_RANGE = 2..9
private val COMPOUND_OPERAND_RANGE = 1..9
private val LEVEL_ONE_WRONG_ANSWER_RANGE = 0..18
private val LEVEL_TWO_AND_THREE_WRONG_ANSWER_RANGE = 0..81
private val LEVEL_FOUR_WRONG_ANSWER_RANGE = 0..90
private val LEVEL_FIVE_WRONG_ANSWER_RANGE = 0..9

private data class CalculationCandidate(
  val leftOperand: Int,
  val rightOperand: Int,
  val operator: CalculationOperator,
  val calculationResult: Int,
  val thirdOperand: Int? = null,
  val secondOperator: CalculationOperator? = null,
  val missingOperandIndex: Int? = null,
  val selectionWeight: Int = UNIFORM_SELECTION_WEIGHT_PER_OPERAND_PAIR,
) {
  val correctAnswer: Int = when (missingOperandIndex) {
    MISSING_LEFT_OPERAND_INDEX -> leftOperand
    MISSING_RIGHT_OPERAND_INDEX -> rightOperand
    MISSING_THIRD_OPERAND_INDEX -> requireNotNull(thirdOperand)
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
      val operandRange = if (operator.isMultiplicative) {
        BASIC_MULTIPLICATIVE_FACTOR_RANGE
      } else {
        BASIC_ADDITIVE_OPERAND_RANGE
      }
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
          if (
            difficulty != GameDifficulty.LEVEL_ONE ||
            calculationResult != LEVEL_ONE_EXCLUDED_CORRECT_ANSWER
          ) {
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

  // 元の演算子抽選比率を値の組ごとに配分し、引き算の順序違いも別の抽選枠として残す。
  private fun basicSelectionWeight(
    difficulty: GameDifficulty,
    operator: CalculationOperator,
  ): Int = when (difficulty) {
    GameDifficulty.LEVEL_ONE -> UNIFORM_SELECTION_WEIGHT_PER_OPERAND_PAIR
    GameDifficulty.LEVEL_TWO -> if (operator.isMultiplicative) {
      MULTIPLICATION_SELECTION_WEIGHT_PER_OPERAND_PAIR
    } else {
      ADDITIVE_SELECTION_WEIGHT_PER_OPERAND_PAIR
    }
    GameDifficulty.LEVEL_THREE -> when (operator) {
      CalculationOperator.DIVISION -> DIVISION_SELECTION_WEIGHT_PER_OPERAND_PAIR
      CalculationOperator.MULTIPLICATION -> MULTIPLICATION_SELECTION_WEIGHT_PER_OPERAND_PAIR
      else -> ADDITIVE_SELECTION_WEIGHT_PER_OPERAND_PAIR
    }
    else -> error("基本計算の難易度ではありません")
  }

  private fun compoundCandidates(difficulty: GameDifficulty): List<CalculationCandidate> =
    buildList {
      val missingOperandIndexes: List<Int?> = if (difficulty == GameDifficulty.LEVEL_FIVE) {
        listOf(
          MISSING_LEFT_OPERAND_INDEX,
          MISSING_RIGHT_OPERAND_INDEX,
          MISSING_THIRD_OPERAND_INDEX,
        )
      } else {
        listOf(null)
      }
      for (operator in CalculationOperator.entries) {
        for (secondOperator in CalculationOperator.entries.filter { it != operator }) {
          for (leftOperand in COMPOUND_OPERAND_RANGE) {
            for (rightOperand in COMPOUND_OPERAND_RANGE) {
              for (thirdOperand in COMPOUND_OPERAND_RANGE) {
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
      (operator.isMultiplicative &&
        (leftOperand == EXCLUDED_MULTIPLICATIVE_OPERAND ||
          rightOperand == EXCLUDED_MULTIPLICATIVE_OPERAND)) ||
      (secondOperator.isMultiplicative &&
        (rightOperand == EXCLUDED_MULTIPLICATIVE_OPERAND ||
          thirdOperand == EXCLUDED_MULTIPLICATIVE_OPERAND))
    ) {
      return null
    }
    val calculationResult = if (secondOperator.precedence > operator.precedence) {
      if (
        secondOperator == CalculationOperator.DIVISION &&
        rightOperand % thirdOperand != EXACT_DIVISION_REMAINDER
      ) {
        return null
      }
      operator.calculate(leftOperand, secondOperator.calculate(rightOperand, thirdOperand))
    } else {
      if (
        operator == CalculationOperator.DIVISION &&
        leftOperand % rightOperand != EXACT_DIVISION_REMAINDER
      ) {
        return null
      }
      val leftResult = operator.calculate(leftOperand, rightOperand)
      if (
        secondOperator == CalculationOperator.DIVISION &&
        leftResult % thirdOperand != EXACT_DIVISION_REMAINDER
      ) {
        return null
      }
      secondOperator.calculate(leftResult, thirdOperand)
    }
    return calculationResult.takeIf { it >= MINIMUM_COMPOUND_RESULT }
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
      GameDifficulty.LEVEL_ONE -> LEVEL_ONE_WRONG_ANSWER_RANGE
      GameDifficulty.LEVEL_TWO,
      GameDifficulty.LEVEL_THREE -> LEVEL_TWO_AND_THREE_WRONG_ANSWER_RANGE
      GameDifficulty.LEVEL_FOUR -> LEVEL_FOUR_WRONG_ANSWER_RANGE
      GameDifficulty.LEVEL_FIVE -> LEVEL_FIVE_WRONG_ANSWER_RANGE
    }
    val wrongAnswers = wrongAnswerRange
      .filter { it != candidate.correctAnswer }
      .shuffled(random)
      .take(WRONG_ANSWER_COUNT)
    return CalculationQuestion(
      leftOperand = candidate.leftOperand,
      rightOperand = candidate.rightOperand,
      operator = candidate.operator,
      choices = buildList(ANSWER_CHOICE_COUNT) {
        addAll(wrongAnswers)
        add(candidate.correctAnswer)
      }.shuffled(random),
      thirdOperand = candidate.thirdOperand,
      secondOperator = candidate.secondOperator,
      missingOperandIndex = candidate.missingOperandIndex,
    )
  }
}
