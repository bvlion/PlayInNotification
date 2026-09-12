package net.ambitious.android.playinnotification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionResultTest {
  @Test
  fun `Lv1からLv5の正解は難易度ごとのポイントを獲得する`() {
    val expectedPoints = listOf(3, 4, 6, 7, 9)

    expectedPoints.forEachIndexed { index, points ->
      val answerResult = GameAnswerResult.create(
        isCorrect = true,
        questionDifficulty = index + 1,
      )

      assertTrue(answerResult.isCorrect)
      assertEquals(points, answerResult.earnedPoints)
    }
  }

  @Test
  fun `Lv1からLv5の不正解は1ポイントを獲得する`() {
    (1..5).forEach { difficulty ->
      val answerResult = GameAnswerResult.create(
        isCorrect = false,
        questionDifficulty = difficulty,
      )

      assertFalse(answerResult.isCorrect)
      assertEquals(1, answerResult.earnedPoints)
    }
  }

  @Test
  fun `回答結果から回答数と獲得ポイントを集計する`() {
    val sessionResult = GameSessionResult()
      .addAnswerResult(
        GameAnswerResult.create(
          isCorrect = true,
          questionDifficulty = 1,
        ),
      )
      .addAnswerResult(
        GameAnswerResult.create(
          isCorrect = false,
          questionDifficulty = 1,
        ),
      )

    assertEquals(2, sessionResult.answerCount)
    assertEquals(4, sessionResult.earnedPoints)
  }
}
