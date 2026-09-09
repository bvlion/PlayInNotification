package net.ambitious.android.playinnotification

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculationQuestionTest {
  @Test
  fun `生成した問題は1桁の数値による足し算または引き算になる`() {
    val random = Random(1)
    val generatedOperators = mutableSetOf<CalculationOperator>()

    repeat(1_000) {
      val question = CalculationQuestion.create(random)

      assertTrue(question.leftOperand in 0..9)
      assertTrue(question.rightOperand in 0..9)
      generatedOperators += question.operator
      when (question.operator) {
        CalculationOperator.ADDITION -> {
          assertEquals(question.leftOperand + question.rightOperand, question.correctAnswer)
        }
        CalculationOperator.SUBTRACTION -> {
          assertTrue(question.leftOperand >= question.rightOperand)
          assertEquals(question.leftOperand - question.rightOperand, question.correctAnswer)
        }
        CalculationOperator.MULTIPLICATION -> throw AssertionError("Lv1で掛け算が生成された")
        CalculationOperator.DIVISION -> throw AssertionError("Lv1で割り算が生成された")
      }
    }

    assertEquals(
      setOf(CalculationOperator.ADDITION, CalculationOperator.SUBTRACTION),
      generatedOperators,
    )
  }

  @Test
  fun `選択肢は正解を含む重複しない3択になる`() {
    val random = Random(2)

    repeat(1_000) {
      val question = CalculationQuestion.create(random)

      assertEquals(3, question.choices.size)
      assertEquals(3, question.choices.distinct().size)
      assertTrue(question.correctAnswer in question.choices)
    }
  }

  @Test
  fun `Lv2は約半数の掛け算と残りの足し算または引き算になる`() {
    val random = Random(3)
    val generatedOperators = mutableListOf<CalculationOperator>()

    repeat(10_000) {
      generatedOperators += CalculationQuestion.create(difficulty = 2, random = random).operator
    }

    assertTrue(generatedOperators.count { it == CalculationOperator.MULTIPLICATION } in 4_800..5_200)
    assertTrue(CalculationOperator.ADDITION in generatedOperators)
    assertTrue(CalculationOperator.SUBTRACTION in generatedOperators)
  }

  @Test
  fun `Lv2の数値範囲と答えは出題条件に従う`() {
    val random = Random(4)

    repeat(1_000) {
      val question = CalculationQuestion.create(difficulty = 2, random = random)

      when (question.operator) {
        CalculationOperator.ADDITION -> {
          assertTrue(question.leftOperand in 1..9)
          assertTrue(question.rightOperand in 1..9)
          assertEquals(question.leftOperand + question.rightOperand, question.correctAnswer)
        }
        CalculationOperator.SUBTRACTION -> {
          assertTrue(question.leftOperand in 1..9)
          assertTrue(question.rightOperand in 1..9)
          assertTrue(question.correctAnswer >= 0)
          assertEquals(question.leftOperand - question.rightOperand, question.correctAnswer)
        }
        CalculationOperator.MULTIPLICATION -> {
          assertTrue(question.leftOperand in 2..9)
          assertTrue(question.rightOperand in 2..9)
          assertEquals(question.leftOperand * question.rightOperand, question.correctAnswer)
        }
        CalculationOperator.DIVISION -> throw AssertionError("Lv2で割り算が生成された")
      }
      assertEquals(3, question.choices.size)
      assertEquals(3, question.choices.distinct().size)
      assertTrue(question.correctAnswer in question.choices)
    }
  }

  @Test
  fun `Lv2は直前と同じ問題を連続して出題しない`() {
    val random = Random(5)
    var previousQuestion: CalculationQuestion? = null

    repeat(1_000) {
      val question = CalculationQuestion.create(
        difficulty = 2,
        previousQuestion = previousQuestion,
        random = random,
      )

      if (previousQuestion != null) {
        assertTrue(
          question.leftOperand != previousQuestion.leftOperand ||
            question.rightOperand != previousQuestion.rightOperand ||
            question.operator != previousQuestion.operator,
        )
      }
      previousQuestion = question
    }
  }

  @Test
  fun `Lv3は約半数の割り算と残りの足し算引き算掛け算になる`() {
    val random = Random(6)
    val generatedOperators = mutableListOf<CalculationOperator>()

    repeat(10_000) {
      generatedOperators += CalculationQuestion.create(difficulty = 3, random = random).operator
    }

    assertTrue(generatedOperators.count { it == CalculationOperator.DIVISION } in 4_800..5_200)
    assertTrue(CalculationOperator.ADDITION in generatedOperators)
    assertTrue(CalculationOperator.SUBTRACTION in generatedOperators)
    assertTrue(CalculationOperator.MULTIPLICATION in generatedOperators)
  }

  @Test
  fun `Lv3の数値範囲と答えは出題条件に従う`() {
    val random = Random(7)

    repeat(1_000) {
      val question = CalculationQuestion.create(difficulty = 3, random = random)

      when (question.operator) {
        CalculationOperator.ADDITION -> {
          assertTrue(question.leftOperand in 1..9)
          assertTrue(question.rightOperand in 1..9)
          assertEquals(question.leftOperand + question.rightOperand, question.correctAnswer)
        }
        CalculationOperator.SUBTRACTION -> {
          assertTrue(question.leftOperand in 1..9)
          assertTrue(question.rightOperand in 1..9)
          assertTrue(question.correctAnswer >= 0)
          assertEquals(question.leftOperand - question.rightOperand, question.correctAnswer)
        }
        CalculationOperator.MULTIPLICATION -> {
          assertTrue(question.leftOperand in 2..9)
          assertTrue(question.rightOperand in 2..9)
          assertEquals(question.leftOperand * question.rightOperand, question.correctAnswer)
        }
        CalculationOperator.DIVISION -> {
          assertTrue(question.correctAnswer in 2..9)
          assertTrue(question.rightOperand in 2..9)
          assertEquals(0, question.leftOperand % question.rightOperand)
          assertEquals(question.leftOperand / question.rightOperand, question.correctAnswer)
        }
      }
      assertEquals(3, question.choices.size)
      assertEquals(3, question.choices.distinct().size)
      assertTrue(question.correctAnswer in question.choices)
    }
  }

  @Test
  fun `Lv3は直前と同じ問題を連続して出題しない`() {
    val random = Random(8)
    var previousQuestion: CalculationQuestion? = null

    repeat(1_000) {
      val question = CalculationQuestion.create(
        difficulty = 3,
        previousQuestion = previousQuestion,
        random = random,
      )

      if (previousQuestion != null) {
        assertTrue(
          question.leftOperand != previousQuestion.leftOperand ||
            question.rightOperand != previousQuestion.rightOperand ||
            question.operator != previousQuestion.operator,
        )
      }
      previousQuestion = question
    }
  }
}
