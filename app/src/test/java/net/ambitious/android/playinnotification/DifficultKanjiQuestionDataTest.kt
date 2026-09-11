package net.ambitious.android.playinnotification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultKanjiQuestionDataTest {
  private val entriesByDifficulty =
    DifficultKanjiQuestionData.load(
      checkNotNull(javaClass.classLoader)
        .getResourceAsStream(DifficultKanjiQuestionData.fileName)
        .let(::checkNotNull),
    )

  @Test
  fun `Lv1からLv5に100語ずつ収録されている`() {
    assertEquals((1..5).toSet(), entriesByDifficulty.keys)
    entriesByDifficulty.values.forEach { entries ->
      assertEquals(100, entries.size)
    }
    assertEquals(500, entriesByDifficulty.values.flatten().size)
  }

  @Test
  fun `表記と読みは空でなく重複していない`() {
    val allEntries = entriesByDifficulty.values.flatten()

    assertTrue(allEntries.all { it.writtenForm.isNotBlank() })
    assertTrue(allEntries.all { it.reading.isNotBlank() })
    assertEquals(allEntries.size, allEntries.map { it.writtenForm }.distinct().size)
    assertEquals(allEntries.size, allEntries.map { it.reading }.distinct().size)
  }

  @Test
  fun `各Lvで両方向の3択に異なる誤答候補を構成できる`() {
    entriesByDifficulty.values.forEach { entries ->
      entries.forEach { correctEntry ->
        assertTrue(entries.count { it.writtenForm != correctEntry.writtenForm } >= 2)
        assertTrue(entries.map { it.reading }.distinct().count { it != correctEntry.reading } >= 2)
      }
    }
  }
}
