package net.ambitious.android.playinnotification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultKanjiQuestionDataTest {
  @Test
  fun `Lv1からLv5に100語ずつ収録されている`() {
    assertEquals((1..5).toSet(), DifficultKanjiQuestionData.entriesByDifficulty.keys)
    DifficultKanjiQuestionData.entriesByDifficulty.values.forEach { entries ->
      assertEquals(100, entries.size)
    }
    assertEquals(500, DifficultKanjiQuestionData.entriesByDifficulty.values.flatten().size)
  }

  @Test
  fun `表記と読みは空でなく重複していない`() {
    val allEntries = DifficultKanjiQuestionData.entriesByDifficulty.values.flatten()

    assertTrue(allEntries.all { it.writtenForm.isNotBlank() })
    assertTrue(allEntries.all { it.reading.isNotBlank() })
    assertEquals(allEntries.size, allEntries.map { it.writtenForm }.distinct().size)
    assertEquals(allEntries.size, allEntries.map { it.reading }.distinct().size)
  }

  @Test
  fun `各Lvで両方向の3択に異なる誤答候補を構成できる`() {
    DifficultKanjiQuestionData.entriesByDifficulty.values.forEach { entries ->
      entries.forEach { correctEntry ->
        assertTrue(entries.count { it.writtenForm != correctEntry.writtenForm } >= 2)
        assertTrue(entries.map { it.reading }.distinct().count { it != correctEntry.reading } >= 2)
      }
    }
  }
}
