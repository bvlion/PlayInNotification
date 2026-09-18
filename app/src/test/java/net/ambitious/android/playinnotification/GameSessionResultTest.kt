package net.ambitious.android.playinnotification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionResultTest {
  @Test
  fun `正解は難易度ごとのポイントを獲得する`() {
    GameDifficulty.entries.forEach { difficulty ->
      val answerResult = GameAnswerResult.create(
        question = "1 + 1 = ?",
        selectedAnswer = "2",
        isCorrect = true,
        questionDifficulty = difficulty,
      )

      assertTrue(answerResult.isCorrect)
      assertEquals(difficulty.correctAnswerPoints, answerResult.earnedPoints)
    }
  }

  @Test
  fun `不正解は難易度によらず1ポイントを獲得する`() {
    GameDifficulty.entries.forEach { difficulty ->
      val answerResult = GameAnswerResult.create(
        question = "1 + 1 = ?",
        selectedAnswer = "3",
        isCorrect = false,
        questionDifficulty = difficulty,
      )

      assertFalse(answerResult.isCorrect)
      assertEquals(1, answerResult.earnedPoints)
    }
  }

  @Test
  fun `回答一覧から回答数と獲得ポイントを集計する`() {
    val sessionResult = GameSessionResult()
      .addAnswerResult(
        GameAnswerResult.create(
          question = "1 + 1 = ?",
          selectedAnswer = "2",
          isCorrect = true,
          questionDifficulty = GameDifficulty.LEVEL_ONE,
        ),
      )
      .addAnswerResult(
        GameAnswerResult.create(
          question = "2 + 2 = ?",
          selectedAnswer = "5",
          isCorrect = false,
          questionDifficulty = GameDifficulty.LEVEL_ONE,
        ),
      )

    assertEquals(2, sessionResult.answerCount)
    assertEquals(1, sessionResult.correctAnswerCount)
    assertEquals(1, sessionResult.incorrectAnswerCount)
    assertEquals(4, sessionResult.earnedPoints)
    assertEquals("1 + 1 = ?", sessionResult.answerResults[0].question)
    assertEquals("2", sessionResult.answerResults[0].selectedAnswer)
    assertTrue(sessionResult.answerResults[0].isCorrect)
    assertEquals("2 + 2 = ?", sessionResult.answerResults[1].question)
    assertEquals("5", sessionResult.answerResults[1].selectedAnswer)
    assertFalse(sessionResult.answerResults[1].isCorrect)
  }
}
