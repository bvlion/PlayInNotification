package net.ambitious.android.playinnotification

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class GameStatisticsTest {
  @Test
  fun `累積獲得ポイントが次の必要ポイント未満なら成長レベルは上がらない`() {
    assertEquals(1, GameStatistics(earnedPoints = 99).growthLevel)
    assertEquals(2, GameStatistics(earnedPoints = 299).growthLevel)
    assertEquals(3, GameStatistics(earnedPoints = 599).growthLevel)
  }

  @Test
  fun `累積獲得ポイントが必要ポイントに達すると成長レベルが上がる`() {
    assertEquals(2, GameStatistics(earnedPoints = 100).growthLevel)
    assertEquals(3, GameStatistics(earnedPoints = 300).growthLevel)
    assertEquals(4, GameStatistics(earnedPoints = 600).growthLevel)
    assertEquals(5, GameStatistics(earnedPoints = 1_000).growthLevel)
  }

  @Test
  fun `大きな累積獲得ポイントから成長レベルを算出できる`() {
    assertEquals(100_000, GameStatistics(earnedPoints = 499_995_000_000).growthLevel)
  }

  @Test
  fun `完了したセッションは成績へ累積される`() {
    val statistics = GameStatistics().addCompletedSession(
      sessionResult = sessionResult(3, 3, 3, 1),
      gameType = GameType.CALCULATION,
      completedSessionDate = LocalDate.of(2026, 9, 9),
    )

    assertEquals(4, statistics.answerCount)
    assertEquals(1, statistics.playCount)
    assertEquals(10, statistics.earnedPoints)
    assertEquals(1, statistics.totalDayCount)
    assertEquals(1, statistics.streakDayCount)
  }

  @Test
  fun `計算の自己ベストは全難易度を通じて最高記録だけが保持される`() {
    val statistics = listOf(10, 8, 7, 9, 12, 15).foldIndexed(GameStatistics()) {
      index, currentStatistics, earnedPoints ->
      currentStatistics.addCompletedSession(
        sessionResult = sessionResult(earnedPoints),
        gameType = GameType.CALCULATION,
        completedSessionDate = LocalDate.of(2026, 9, 9).plusDays(index.toLong()),
      )
    }

    assertEquals(15, statistics.bestPointsByGame.getValue(GameType.CALCULATION))
  }

  @Test
  fun `ゲームごとに自己ベストを保持する`() {
    var statistics = GameStatistics().addCompletedSession(
      sessionResult = sessionResult(11),
      gameType = GameType.CALCULATION,
      completedSessionDate = LocalDate.of(2026, 9, 9),
    )
    listOf(3, 6, 9, 12, 15).forEach { earnedPoints ->
      statistics = statistics.addCompletedSession(
        sessionResult = sessionResult(earnedPoints),
        gameType = GameType.DIFFICULT_KANJI,
        completedSessionDate = LocalDate.of(2026, 9, 9),
      )
    }

    assertEquals(15, statistics.bestPointsByGame.getValue(GameType.DIFFICULT_KANJI))
    assertEquals(11, statistics.bestPointsByGame.getValue(GameType.CALCULATION))
  }

  @Test
  fun `同じ日に複数回完了しても日数は一日だけ進む`() {
    val firstStatistics = GameStatistics().addCompletedSession(
      sessionResult = GameSessionResult(),
      gameType = GameType.CALCULATION,
      completedSessionDate = LocalDate.of(2026, 9, 9),
    )
    val statistics = firstStatistics.addCompletedSession(
      sessionResult = GameSessionResult(),
      gameType = GameType.DIFFICULT_KANJI,
      completedSessionDate = LocalDate.of(2026, 9, 9),
    )

    assertEquals(1, statistics.totalDayCount)
    assertEquals(1, statistics.streakDayCount)
  }

  @Test
  fun `別の日に完了すると累計日数が増える`() {
    val firstStatistics = GameStatistics().addCompletedSession(
      sessionResult = GameSessionResult(),
      gameType = GameType.CALCULATION,
      completedSessionDate = LocalDate.of(2026, 9, 9),
    )
    val statistics = firstStatistics.addCompletedSession(
      sessionResult = GameSessionResult(),
      gameType = GameType.CALCULATION,
      completedSessionDate = LocalDate.of(2026, 9, 10),
    )

    assertEquals(2, statistics.totalDayCount)
  }

  @Test
  fun `連続する完了日は連続日数が進む`() {
    val firstStatistics = GameStatistics(
      streakDayCount = 3,
      lastCompletedSessionDate = LocalDate.of(2026, 9, 8),
    )
    val statistics = firstStatistics.addCompletedSession(
      sessionResult = GameSessionResult(),
      gameType = GameType.CALCULATION,
      completedSessionDate = LocalDate.of(2026, 9, 9),
    )

    assertEquals(4, statistics.streakDayCount)
  }

  @Test
  fun `連続しない完了日は連続日数を一日からやり直す`() {
    val firstStatistics = GameStatistics(
      streakDayCount = 3,
      lastCompletedSessionDate = LocalDate.of(2026, 9, 7),
    )
    val statistics = firstStatistics.addCompletedSession(
      sessionResult = GameSessionResult(),
      gameType = GameType.CALCULATION,
      completedSessionDate = LocalDate.of(2026, 9, 9),
    )

    assertEquals(1, statistics.streakDayCount)
  }

  private fun sessionResult(vararg earnedPoints: Int): GameSessionResult = GameSessionResult(
    answerResults = earnedPoints.mapIndexed { index, points ->
      GameAnswerResult(
        question = "question $index",
        selectedAnswer = "answer $index",
        isCorrect = true,
        earnedPoints = points,
      )
    },
  )
}
