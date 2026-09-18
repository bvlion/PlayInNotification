package net.ambitious.android.playinnotification

import kotlin.random.Random

internal data class DifficultKanjiQuestion(
  val direction: DifficultKanjiQuestionDirection,
  val entry: DifficultKanjiEntry,
  val choices: List<String>,
) {
  val prompt: String
    get() = when (direction) {
      DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> entry.writtenForm
      DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> entry.reading
    }

  val correctAnswer: String
    get() = when (direction) {
      DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> entry.reading
      DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> entry.writtenForm
    }

  companion object {
    fun create(
      entriesByDifficulty: Map<Int, List<DifficultKanjiEntry>>,
      difficulty: GameDifficulty,
      random: Random = Random.Default,
      previousQuestion: DifficultKanjiQuestion? = null,
      askedEntries: Collection<DifficultKanjiEntry> = emptyList(),
    ): DifficultKanjiQuestion {
      val entries = requireNotNull(entriesByDifficulty[difficulty.level]) {
        "難易度${difficulty.level} の問題データがありません"
      }
      val direction = if (previousQuestion == null) {
        DifficultKanjiQuestionDirection.entries.random(random)
      } else {
        when (previousQuestion.direction) {
          DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> {
            DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM
          }
          DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> {
            DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING
          }
        }
      }
      val correctEntry = entries
        .filter { entry ->
          entry !in askedEntries && entry != previousQuestion?.entry
        }
        .random(random)
      val correctAnswer = when (direction) {
        DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> correctEntry.reading
        DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> correctEntry.writtenForm
      }
      val wrongAnswers = entries
        .filter { it != correctEntry }
        .shuffled(random)
        .sortedWith(
          compareByDescending<DifficultKanjiEntry> {
            it.reading.commonPrefixWith(correctEntry.reading).length +
              it.reading.commonSuffixWith(correctEntry.reading).length
          }.thenByDescending {
            it.reading.count(correctEntry.reading::contains)
          }.thenBy {
            kotlin.math.abs(it.reading.length - correctEntry.reading.length)
          }.thenByDescending {
            when (direction) {
              DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> 0
              DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> {
                it.writtenForm.commonPrefixWith(correctEntry.writtenForm).length +
                  it.writtenForm.commonSuffixWith(correctEntry.writtenForm).length
              }
            }
          },
        )
        .take(2)
        .map {
          when (direction) {
            DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> it.reading
            DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> it.writtenForm
          }
        }

      require(wrongAnswers.size == 2) { "3択の誤答候補を構成できません" }
      return DifficultKanjiQuestion(
        direction = direction,
        entry = correctEntry,
        choices = (wrongAnswers + correctAnswer).shuffled(random),
      )
    }
  }
}

internal enum class DifficultKanjiQuestionDirection {
  WRITTEN_FORM_TO_READING,
  READING_TO_WRITTEN_FORM,
}
