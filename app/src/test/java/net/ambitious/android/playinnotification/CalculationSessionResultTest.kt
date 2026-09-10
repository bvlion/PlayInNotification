package net.ambitious.android.playinnotification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculationSessionResultTest {
  private val question = CalculationQuestion(
    leftOperand = 4,
    rightOperand = 2,
    operator = CalculationOperator.ADDITION,
    choices = listOf(5, 6, 7),
  )

  @Test
  fun `正解の回答は問題難易度の3倍のポイントを獲得する`() {
    val answerResult = CalculationAnswerResult.create(
      question = question,
      answer = question.correctAnswer,
      questionDifficulty = 2,
    )

    assertTrue(answerResult.isCorrect)
    assertEquals(6, answerResult.earnedPoints)
  }

  @Test
  fun `不正解の回答は問題難易度と同じポイントを獲得する`() {
    val answerResult = CalculationAnswerResult.create(
      question = question,
      answer = 5,
      questionDifficulty = 2,
    )

    assertFalse(answerResult.isCorrect)
    assertEquals(2, answerResult.earnedPoints)
  }

  @Test
  fun `Lv3の回答はLv3の倍率でポイントを獲得する`() {
    val answerResult = CalculationAnswerResult.create(
      question = question,
      answer = question.correctAnswer,
      questionDifficulty = 3,
    )

    assertTrue(answerResult.isCorrect)
    assertEquals(9, answerResult.earnedPoints)
  }

  @Test
  fun `Lv4の回答はLv4の倍率でポイントを獲得する`() {
    val answerResult = CalculationAnswerResult.create(
      question = question,
      answer = question.correctAnswer,
      questionDifficulty = 4,
    )

    assertTrue(answerResult.isCorrect)
    assertEquals(12, answerResult.earnedPoints)
  }

  @Test
  fun `回答結果から回答数と獲得ポイントを集計する`() {
    val sessionResult = CalculationSessionResult()
      .addAnswerResult(
        CalculationAnswerResult.create(
          question = question,
          answer = question.correctAnswer,
          questionDifficulty = 1,
        ),
      )
      .addAnswerResult(
        CalculationAnswerResult.create(
          question = question,
          answer = 5,
          questionDifficulty = 1,
        ),
      )

    assertEquals(2, sessionResult.answerCount)
    assertEquals(4, sessionResult.earnedPoints)
  }
}
