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
      assertTrue(question.choices.filter { it != question.correctAnswer }.all { choice ->
        choice in entry.readingWrongAnswers
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
      assertTrue(question.choices.filter { it != question.correctAnswer }.all { choice ->
        choice in entry.writtenFormWrongAnswers
      })
    }
  }

  @Test
  fun `表記から読みでは問題固有の誤答候補から選ぶ`() {
    val question = DifficultKanjiQuestion.create(
      entriesByDifficulty = mapOf(
        1 to listOf(
          DifficultKanjiEntry(
            "鶯",
            "うぐいす",
            readingWrongAnswers = listOf("うずら", "うなぎ", "おうむ", "かもめ", "つぐみ"),
          ),
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
    assertTrue(
      question.choices.filter { it != question.correctAnswer }
        .all { it in question.entry.readingWrongAnswers },
    )
  }

  @Test
  fun `読みから表記では問題固有の誤答候補から選ぶ`() {
    val question = DifficultKanjiQuestion.create(
      entriesByDifficulty = mapOf(
        1 to listOf(
          DifficultKanjiEntry(
            "鶯",
            "うぐいす",
            writtenFormWrongAnswers = listOf("鶉", "鰻", "鴎", "鷺", "鵯"),
          ),
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
    assertTrue(
      question.choices.filter { it != question.correctAnswer }
        .all { it in question.entry.writtenFormWrongAnswers },
    )
  }

  @Test
  fun `問題固有の誤答候補は抽選される`() {
    val correctEntry = DifficultKanjiEntry(
      "甲",
      "あまさ",
      readingWrongAnswers = listOf("あかさ", "あたさ", "あなさ", "あはさ", "あまし"),
    )
    val selectedPairs = mutableSetOf<Set<String>>()

    repeat(100) { seed ->
      val question = DifficultKanjiQuestion.create(
        entriesByDifficulty = mapOf(1 to listOf(correctEntry)),
        difficulty = GameDifficulty.LEVEL_ONE,
        random = Random(seed),
        previousQuestion = DifficultKanjiQuestion(
          direction = DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM,
          entry = DifficultKanjiEntry("", ""),
          choices = emptyList(),
        ),
      )
      selectedPairs += question.choices.filter { it != correctEntry.reading }.toSet()
    }

    assertTrue(selectedPairs.size > 1)
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
