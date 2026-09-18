package net.ambitious.android.playinnotification

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultKanjiQuestionTest {
  private val entriesByDifficulty =
    DifficultKanjiQuestionData.load(
      checkNotNull(javaClass.classLoader)
        .getResourceAsStream(DifficultKanjiQuestionData.FILE_NAME)
        .let(::checkNotNull),
    )

  @Test
  fun `Lv1からLv5で同一セッション内の過去の語を再出題しない`() {
    GameDifficulty.entries.forEach { difficulty ->
      val random = Random(difficulty.level + 20)
      val askedEntries = mutableListOf<DifficultKanjiEntry>()
      var previousQuestion: DifficultKanjiQuestion? = null

      repeat(30) {
        val question = DifficultKanjiQuestion.create(
          entriesByDifficulty = entriesByDifficulty,
          difficulty = difficulty,
          previousQuestion = previousQuestion,
          askedEntries = askedEntries,
          random = random,
        )
        assertTrue(question.entry !in askedEntries)
        askedEntries += question.entry
        previousQuestion = question
      }
    }
  }

  @Test
  fun `新しいセッションでは以前に出た難読漢字の語を再出題できる`() {
    GameDifficulty.entries.forEach { difficulty ->
      val firstQuestion = DifficultKanjiQuestion.create(
        entriesByDifficulty = entriesByDifficulty,
        difficulty = difficulty,
        random = Random(difficulty.level + 30),
      )
      val nextSessionQuestion = DifficultKanjiQuestion.create(
        entriesByDifficulty = entriesByDifficulty,
        difficulty = difficulty,
        random = Random(difficulty.level + 30),
        askedEntries = emptyList(),
      )

      assertEquals(firstQuestion, nextSessionQuestion)
    }
  }

  @Test
  fun `Lv1からLv5で表記から読みを選ぶ3択問題を生成できる`() {
    GameDifficulty.entries.forEach { difficulty ->
      val question = DifficultKanjiQuestion.create(
        entriesByDifficulty = entriesByDifficulty,
        difficulty = difficulty,
        random = Random(difficulty.level),
        previousQuestion = DifficultKanjiQuestion(
          direction = DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM,
          entry = DifficultKanjiEntry("", ""),
          choices = emptyList(),
        ),
      )
      val entry = entriesByDifficulty.getValue(difficulty.level)
        .single { it.writtenForm == question.prompt }

      assertEquals(entry.reading, question.correctAnswer)
      assertEquals(3, question.choices.size)
      assertEquals(3, question.choices.distinct().size)
      assertTrue(question.correctAnswer in question.choices)
      assertTrue(question.choices.all { choice ->
        entriesByDifficulty.getValue(difficulty.level).any { it.reading == choice }
      })
    }
  }

  @Test
  fun `Lv1からLv5で読みから表記を選ぶ3択問題を生成できる`() {
    GameDifficulty.entries.forEach { difficulty ->
      val question = DifficultKanjiQuestion.create(
        entriesByDifficulty = entriesByDifficulty,
        difficulty = difficulty,
        random = Random(difficulty.level),
        previousQuestion = DifficultKanjiQuestion(
          direction = DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING,
          entry = DifficultKanjiEntry("", ""),
          choices = emptyList(),
        ),
      )
      val entry = entriesByDifficulty.getValue(difficulty.level)
        .single { it.reading == question.prompt }

      assertEquals(entry.writtenForm, question.correctAnswer)
      assertEquals(3, question.choices.size)
      assertEquals(3, question.choices.distinct().size)
      assertTrue(question.correctAnswer in question.choices)
      assertTrue(question.choices.all { choice ->
        entriesByDifficulty.getValue(difficulty.level).any { it.writtenForm == choice }
      })
    }
  }

  @Test
  fun `表記から読みでは正解の読みと近い候補を誤答にする`() {
    val question = DifficultKanjiQuestion.create(
      entriesByDifficulty = mapOf(
        1 to listOf(
          DifficultKanjiEntry("鶯", "うぐいす"),
          DifficultKanjiEntry("鶉", "うずら"),
          DifficultKanjiEntry("鰻", "うなぎ"),
          DifficultKanjiEntry("山茶花", "さざんか"),
          DifficultKanjiEntry("鸚鵡", "おうむ"),
        ),
      ),
      difficulty = GameDifficulty.LEVEL_ONE,
      random = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
      },
      previousQuestion = DifficultKanjiQuestion(
        direction = DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM,
        entry = DifficultKanjiEntry("", ""),
        choices = emptyList(),
      ),
    )

    assertEquals("うぐいす", question.correctAnswer)
    assertEquals(setOf("うずら", "うなぎ"), question.choices.filter { it != question.correctAnswer }.toSet())
  }

  @Test
  fun `読みから表記では正解の読みと近い候補の表記を誤答にする`() {
    val question = DifficultKanjiQuestion.create(
      entriesByDifficulty = mapOf(
        1 to listOf(
          DifficultKanjiEntry("鶯", "うぐいす"),
          DifficultKanjiEntry("鶉", "うずら"),
          DifficultKanjiEntry("鰻", "うなぎ"),
          DifficultKanjiEntry("山茶花", "さざんか"),
          DifficultKanjiEntry("鸚鵡", "おうむ"),
        ),
      ),
      difficulty = GameDifficulty.LEVEL_ONE,
      random = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
      },
      previousQuestion = DifficultKanjiQuestion(
        direction = DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING,
        entry = DifficultKanjiEntry("", ""),
        choices = emptyList(),
      ),
    )

    assertEquals("鶯", question.correctAnswer)
    assertEquals(setOf("鶉", "鰻"), question.choices.filter { it != question.correctAnswer }.toSet())
  }

  @Test
  fun `連続して生成した問題は表記から読みと読みから表記を混在させる`() {
    GameDifficulty.entries.forEach { difficulty ->
      val firstQuestion = DifficultKanjiQuestion.create(
        entriesByDifficulty = entriesByDifficulty,
        difficulty = difficulty,
        random = Random(difficulty.level),
      )
      val secondQuestion = DifficultKanjiQuestion.create(
        entriesByDifficulty = entriesByDifficulty,
        difficulty = difficulty,
        random = Random(difficulty.level),
        previousQuestion = firstQuestion,
      )

      assertTrue(firstQuestion.direction != secondQuestion.direction)
    }
  }

  @Test
  fun `連続して生成した問題は同じ語を出題しない`() {
    GameDifficulty.entries.forEach { difficulty ->
      val firstQuestion = DifficultKanjiQuestion.create(
        entriesByDifficulty = entriesByDifficulty,
        difficulty = difficulty,
        random = Random(difficulty.level),
      )
      val secondQuestion = DifficultKanjiQuestion.create(
        entriesByDifficulty = entriesByDifficulty,
        difficulty = difficulty,
        random = Random(difficulty.level),
        previousQuestion = firstQuestion,
      )
      assertTrue(firstQuestion.entry != secondQuestion.entry)
    }
  }

}
