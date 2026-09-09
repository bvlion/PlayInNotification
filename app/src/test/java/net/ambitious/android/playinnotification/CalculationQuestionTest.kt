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
      }
    }

    assertEquals(CalculationOperator.entries.toSet(), generatedOperators)
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
}
