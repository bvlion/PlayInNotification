package net.ambitious.android.playinnotification

import kotlin.random.Random

private const val CORRECT_ANSWER_COUNT = 1
private const val WRONG_ANSWER_COUNT = 2
private const val ANSWER_CHOICE_COUNT = CORRECT_ANSWER_COUNT + WRONG_ANSWER_COUNT

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
      val eligibleEntries = entries
        .filter { entry ->
          entry !in askedEntries &&
            entry.writtenForm != previousQuestion?.entry?.writtenForm && when (direction) {
            DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING ->
              entry.readingWrongAnswers.size >= WRONG_ANSWER_COUNT
            DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM ->
              entry.writtenFormWrongAnswers.size >= WRONG_ANSWER_COUNT
          }
        }
      val correctEntry = eligibleEntries.random(random)
      val correctAnswer = when (direction) {
        DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> correctEntry.reading
        DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> correctEntry.writtenForm
      }
      val wrongAnswers = when (direction) {
        DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> correctEntry.readingWrongAnswers
        DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> correctEntry.writtenFormWrongAnswers
      }.shuffled(random).take(WRONG_ANSWER_COUNT)

      require(wrongAnswers.size == WRONG_ANSWER_COUNT) {
        "3択の誤答候補を構成できません"
      }
      return DifficultKanjiQuestion(
        direction = direction,
        entry = correctEntry,
        choices = buildList(ANSWER_CHOICE_COUNT) {
          addAll(wrongAnswers)
          add(correctAnswer)
        }.shuffled(random),
      )
    }
  }
}

internal enum class DifficultKanjiQuestionDirection {
  WRITTEN_FORM_TO_READING,
  READING_TO_WRITTEN_FORM,
}
