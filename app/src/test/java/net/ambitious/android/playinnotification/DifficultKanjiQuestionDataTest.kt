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
  fun `Lv1からLv5に300語ずつ収録されている`() {
    assertEquals((1..5).toSet(), entriesByDifficulty.keys)
    assertEquals(300, entriesByDifficulty.getValue(1).size)
    assertEquals(300, entriesByDifficulty.getValue(2).size)
    assertEquals(300, entriesByDifficulty.getValue(3).size)
    assertEquals(300, entriesByDifficulty.getValue(4).size)
    assertEquals(300, entriesByDifficulty.getValue(5).size)
    assertEquals(1500, entriesByDifficulty.values.flatten().size)
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
  fun `各問題は両方向に5件の異なる誤答候補を持つ`() {
    entriesByDifficulty.values.flatten().forEach { entry ->
      assertEquals(5, entry.writtenFormWrongAnswers.size)
      assertEquals(5, entry.writtenFormWrongAnswers.distinct().size)
      assertTrue(entry.writtenForm !in entry.writtenFormWrongAnswers)
      assertTrue(entry.writtenFormWrongAnswers.all(String::isNotBlank))

      assertEquals(5, entry.readingWrongAnswers.size)
      assertEquals(5, entry.readingWrongAnswers.distinct().size)
      assertTrue(entry.reading !in entry.readingWrongAnswers)
      assertTrue(entry.readingWrongAnswers.all(String::isNotBlank))
    }
  }

  @Test
  fun `追加したLv1問題は出題方向ごとに固有の誤答候補を持つ`() {
    val addedEntries = entriesByDifficulty.getValue(1).drop(100)

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
