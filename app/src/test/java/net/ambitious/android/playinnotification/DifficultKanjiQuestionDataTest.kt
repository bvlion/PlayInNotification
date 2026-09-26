package net.ambitious.android.playinnotification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultKanjiQuestionDataTest {
  private val entriesByDifficulty =
    DifficultKanjiQuestionData.load(
      checkNotNull(javaClass.classLoader)
        .getResourceAsStream(DifficultKanjiQuestionData.FILE_NAME)
        .let(::checkNotNull),
    )

  @Test
  fun `Lv1は両方向に300問ずつ収録されている`() {
    assertEquals((1..5).toSet(), entriesByDifficulty.keys)
    val levelOneEntries = entriesByDifficulty.getValue(1)
    assertEquals(600, levelOneEntries.size)
    assertEquals(300, levelOneEntries.count { it.readingWrongAnswers.isNotEmpty() })
    assertEquals(300, levelOneEntries.count { it.writtenFormWrongAnswers.isNotEmpty() })
    assertTrue(levelOneEntries.all {
      it.readingWrongAnswers.isEmpty() != it.writtenFormWrongAnswers.isEmpty()
    })
    assertEquals(300, levelOneEntries.map { it.writtenForm }.distinct().size)
    assertEquals(300, levelOneEntries.map { it.reading }.distinct().size)
    assertEquals(300, entriesByDifficulty.getValue(2).size)
    assertEquals(300, entriesByDifficulty.getValue(3).size)
    assertEquals(300, entriesByDifficulty.getValue(4).size)
    assertEquals(300, entriesByDifficulty.getValue(5).size)
    assertEquals(1800, entriesByDifficulty.values.flatten().size)
  }

  @Test
  fun `表記と読みは空でなく重複していない`() {
    val allEntries = entriesByDifficulty.values.flatten()

    assertTrue(allEntries.all { it.writtenForm.isNotBlank() })
    assertTrue(allEntries.all { it.reading.isNotBlank() })
    assertEquals(1500, allEntries.map { it.writtenForm }.distinct().size)
    assertEquals(1500, allEntries.map { it.reading }.distinct().size)
  }

  @Test
  fun `各問題は出題方向に3件以上の異なる誤答候補を持つ`() {
    entriesByDifficulty.values.flatten().forEach { entry ->
      if (entry.writtenFormWrongAnswers.isNotEmpty()) {
        assertTrue(entry.writtenFormWrongAnswers.size >= 3)
      }
      assertEquals(entry.writtenFormWrongAnswers.size, entry.writtenFormWrongAnswers.distinct().size)
      assertTrue(entry.writtenForm !in entry.writtenFormWrongAnswers)
      assertTrue(entry.writtenFormWrongAnswers.all(String::isNotBlank))

      if (entry.readingWrongAnswers.isNotEmpty()) {
        assertTrue(entry.readingWrongAnswers.size >= 3)
      }
      assertEquals(entry.readingWrongAnswers.size, entry.readingWrongAnswers.distinct().size)
      assertTrue(entry.reading !in entry.readingWrongAnswers)
      assertTrue(entry.readingWrongAnswers.all(String::isNotBlank))
    }
  }

  @Test
  fun `Lv1問題は出題方向ごとに固有の誤答候補を持つ`() {
    val writtenFormEntries = entriesByDifficulty.getValue(1)
      .filter { it.writtenFormWrongAnswers.isNotEmpty() }
    val readingEntries = entriesByDifficulty.getValue(1)
      .filter { it.readingWrongAnswers.isNotEmpty() }

    assertEquals(
      writtenFormEntries.size,
      writtenFormEntries.map { entry -> entry.writtenFormWrongAnswers.sorted() }.distinct().size,
    )
    assertEquals(
      readingEntries.size,
      readingEntries.map { entry -> entry.readingWrongAnswers.sorted() }.distinct().size,
    )
  }

  @Test
  fun `Lv2問題は出題方向ごとに固有の誤答候補を持つ`() {
    val levelTwoEntries = entriesByDifficulty.getValue(2)

    assertEquals(
      levelTwoEntries.size,
      levelTwoEntries.map { entry -> entry.writtenFormWrongAnswers.sorted() }.distinct().size,
    )
    assertEquals(
      levelTwoEntries.size,
      levelTwoEntries.map { entry -> entry.readingWrongAnswers.sorted() }.distinct().size,
    )
  }

  @Test
  fun `追加したLv3問題は出題方向ごとに固有の誤答候補を持つ`() {
    val addedEntries = entriesByDifficulty.getValue(3).drop(100)

    assertEquals(
      addedEntries.size,
      addedEntries.map { entry -> entry.writtenFormWrongAnswers.sorted() }.distinct().size,
    )
    assertEquals(
      addedEntries.size,
      addedEntries.map { entry -> entry.readingWrongAnswers.sorted() }.distinct().size,
    )
  }

  @Test
  fun `追加したLv4問題は出題方向ごとに固有の誤答候補を持つ`() {
    val addedEntries = entriesByDifficulty.getValue(4).drop(100)

    assertEquals(
      addedEntries.size,
      addedEntries.map { entry -> entry.writtenFormWrongAnswers.sorted() }.distinct().size,
    )
    assertEquals(
      addedEntries.size,
      addedEntries.map { entry -> entry.readingWrongAnswers.sorted() }.distinct().size,
    )
  }

  @Test
  fun `追加したLv5問題は出題方向ごとに固有の誤答候補を持つ`() {
    val addedEntries = entriesByDifficulty.getValue(5).drop(100)

    assertEquals(
      addedEntries.size,
      addedEntries.map { entry -> entry.writtenFormWrongAnswers.sorted() }.distinct().size,
    )
    assertEquals(
      addedEntries.size,
      addedEntries.map { entry -> entry.readingWrongAnswers.sorted() }.distinct().size,
    )
  }
}
