package net.ambitious.android.playinnotification

import android.os.Build
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class GameStatisticsRepositoryTest {
  @Test
  fun `保存した累計日数は成績保存処理を作り直しても読み込める`() = runBlocking {
    val context = RuntimeEnvironment.getApplication()
    val repository = GameStatisticsRepository(context)
    repository.recordCompletedSession(
      sessionResult = GameSessionResult(),
      gameType = GameType.CALCULATION,
      completedSessionDate = LocalDate.of(2026, 9, 16),
    )
    repository.recordCompletedSession(
      sessionResult = GameSessionResult(),
      gameType = GameType.DIFFICULT_KANJI,
      completedSessionDate = LocalDate.of(2026, 9, 17),
    )

    val restoredStatistics = GameStatisticsRepository(context).gameStatistics.first()

    assertEquals(2, restoredStatistics.totalDayCount)
    assertEquals(2, restoredStatistics.streakDayCount)
  }
}
