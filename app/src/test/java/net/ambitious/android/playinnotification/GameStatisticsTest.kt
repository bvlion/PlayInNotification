package net.ambitious.android.playinnotification

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class GameStatisticsTest {
  @Test
  fun `完了したセッションは成績へ累積される`() {
    val statistics = GameStatistics().addCompletedSession(
      sessionResult = GameSessionResult(answerCount = 4, earnedPoints = 10),
      gameGenre = "calculation",
      difficulty = 1,
      completedSessionDate = LocalDate.of(2026, 9, 9),
    )

    assertEquals(4, statistics.answerCount)
    assertEquals(1, statistics.playCount)
    assertEquals(10, statistics.earnedPoints)
    assertEquals(1, statistics.streakDayCount)
  }

  @Test
  fun `自己ベストはゲームジャンルと難易度ごとに保持される`() {
    val firstStatistics = GameStatistics().addCompletedSession(
      sessionResult = GameSessionResult(earnedPoints = 10),
      gameGenre = "calculation",
      difficulty = 1,
      completedSessionDate = LocalDate.of(2026, 9, 9),
    )
    val statistics = firstStatistics
      .addCompletedSession(
        sessionResult = GameSessionResult(earnedPoints = 8),
        gameGenre = "calculation",
        difficulty = 1,
        completedSessionDate = LocalDate.of(2026, 9, 10),
      )
      .addCompletedSession(
        sessionResult = GameSessionResult(earnedPoints = 7),
        gameGenre = "calculation",
        difficulty = 2,
        completedSessionDate = LocalDate.of(2026, 9, 11),
      )
      .addCompletedSession(
        sessionResult = GameSessionResult(earnedPoints = 9),
        gameGenre = "calculation",
        difficulty = 3,
        completedSessionDate = LocalDate.of(2026, 9, 12),
      )
      .addCompletedSession(
        sessionResult = GameSessionResult(earnedPoints = 12),
        gameGenre = "calculation",
        difficulty = 4,
        completedSessionDate = LocalDate.of(2026, 9, 13),
      )
      .addCompletedSession(
        sessionResult = GameSessionResult(earnedPoints = 15),
        gameGenre = "calculation",
        difficulty = 5,
        completedSessionDate = LocalDate.of(2026, 9, 14),
      )

    assertEquals(10, statistics.bestPointsByGame.getValue("calculation:1"))
    assertEquals(7, statistics.bestPointsByGame.getValue("calculation:2"))
    assertEquals(9, statistics.bestPointsByGame.getValue("calculation:3"))
    assertEquals(12, statistics.bestPointsByGame.getValue("calculation:4"))
    assertEquals(15, statistics.bestPointsByGame.getValue("calculation:5"))
  }

  @Test
  fun `難読漢字の自己ベストはLv1からLv5で個別に保持される`() {
    var statistics = GameStatistics()

    (1..5).forEach { difficulty ->
      statistics = statistics.addCompletedSession(
        sessionResult = GameSessionResult(earnedPoints = difficulty * 3),
        gameGenre = "difficult_kanji",
        difficulty = difficulty,
        completedSessionDate = LocalDate.of(2026, 9, 9),
      )
    }

    (1..5).forEach { difficulty ->
      assertEquals(
        (difficulty * 3).toLong(),
        statistics.bestPointsByGame.getValue("difficult_kanji:$difficulty"),
      )
    }
  }

  @Test
  fun `同じ日に複数回完了してもStreakは一日だけ進む`() {
    val firstStatistics = GameStatistics().addCompletedSession(
      sessionResult = GameSessionResult(),
      gameGenre = "calculation",
      difficulty = 1,
      completedSessionDate = LocalDate.of(2026, 9, 9),
    )
    val statistics = firstStatistics.addCompletedSession(
      sessionResult = GameSessionResult(),
      gameGenre = "calculation",
      difficulty = 1,
      completedSessionDate = LocalDate.of(2026, 9, 9),
    )

    assertEquals(1, statistics.streakDayCount)
  }

  @Test
  fun `連続しない完了日はStreakを一日からやり直す`() {
    val firstStatistics = GameStatistics(
      streakDayCount = 3,
      lastCompletedSessionDate = LocalDate.of(2026, 9, 7),
    )
    val statistics = firstStatistics.addCompletedSession(
      sessionResult = GameSessionResult(),
      gameGenre = "calculation",
      difficulty = 1,
      completedSessionDate = LocalDate.of(2026, 9, 9),
    )

    assertEquals(1, statistics.streakDayCount)
  }
}
