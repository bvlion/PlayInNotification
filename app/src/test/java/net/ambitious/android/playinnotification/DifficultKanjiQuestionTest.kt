package net.ambitious.android.playinnotification

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultKanjiQuestionTest {
  private val entriesByDifficulty =
    DifficultKanjiQuestionData.load(
      checkNotNull(javaClass.classLoader)
        .getResourceAsStream(DifficultKanjiQuestionData.fileName)
        .let(::checkNotNull),
    )

  @Test
  fun `Lv1からLv5で表記から読みを選ぶ3択問題を生成できる`() {
    (1..5).forEach { difficulty ->
      val question = DifficultKanjiQuestion.create(
        entriesByDifficulty = entriesByDifficulty,
        difficulty = difficulty,
        random = Random(difficulty),
        previousQuestion = DifficultKanjiQuestion(
          direction = DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM,
          prompt = "",
          choices = emptyList(),
          correctAnswer = "",
        ),
      )
      val entry = entriesByDifficulty.getValue(difficulty).single { it.writtenForm == question.prompt }

      assertEquals(entry.reading, question.correctAnswer)
      assertEquals(3, question.choices.size)
      assertEquals(3, question.choices.distinct().size)
      assertTrue(question.correctAnswer in question.choices)
      assertTrue(question.choices.all { choice ->
        entriesByDifficulty.getValue(difficulty).any { it.reading == choice }
      })
    }
  }

  @Test
  fun `Lv1からLv5で読みから表記を選ぶ3択問題を生成できる`() {
    (1..5).forEach { difficulty ->
      val question = DifficultKanjiQuestion.create(
        entriesByDifficulty = entriesByDifficulty,
        difficulty = difficulty,
        random = Random(difficulty),
        previousQuestion = DifficultKanjiQuestion(
          direction = DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING,
          prompt = "",
          choices = emptyList(),
          correctAnswer = "",
        ),
      )
      val entry = entriesByDifficulty.getValue(difficulty).single { it.reading == question.prompt }

      assertEquals(entry.writtenForm, question.correctAnswer)
      assertEquals(3, question.choices.size)
      assertEquals(3, question.choices.distinct().size)
      assertTrue(question.correctAnswer in question.choices)
      assertTrue(question.choices.all { choice ->
        entriesByDifficulty.getValue(difficulty).any { it.writtenForm == choice }
      })
    }
  }

  @Test
  fun `連続して生成した問題は表記から読みと読みから表記を混在させる`() {
    (1..5).forEach { difficulty ->
      val firstQuestion = DifficultKanjiQuestion.create(
        entriesByDifficulty = entriesByDifficulty,
        difficulty = difficulty,
        random = Random(difficulty),
      )
      val secondQuestion = DifficultKanjiQuestion.create(
        entriesByDifficulty = entriesByDifficulty,
        difficulty = difficulty,
        random = Random(difficulty),
        previousQuestion = firstQuestion,
      )

      assertTrue(firstQuestion.direction != secondQuestion.direction)
    }
  }

  @Test
  fun `連続して生成した問題は同じ語を出題しない`() {
    (1..5).forEach { difficulty ->
      val firstQuestion = DifficultKanjiQuestion.create(
        entriesByDifficulty = entriesByDifficulty,
        difficulty = difficulty,
        random = Random(difficulty),
      )
      val secondQuestion = DifficultKanjiQuestion.create(
        entriesByDifficulty = entriesByDifficulty,
        difficulty = difficulty,
        random = Random(difficulty),
        previousQuestion = firstQuestion,
      )
      val firstEntry = when (firstQuestion.direction) {
        DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> {
          DifficultKanjiEntry(firstQuestion.prompt, firstQuestion.correctAnswer)
        }
        DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> {
          DifficultKanjiEntry(firstQuestion.correctAnswer, firstQuestion.prompt)
        }
      }
      val secondEntry = when (secondQuestion.direction) {
        DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> {
          DifficultKanjiEntry(secondQuestion.prompt, secondQuestion.correctAnswer)
        }
        DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> {
          DifficultKanjiEntry(secondQuestion.correctAnswer, secondQuestion.prompt)
        }
      }

      assertTrue(firstEntry != secondEntry)
    }
  }

}
