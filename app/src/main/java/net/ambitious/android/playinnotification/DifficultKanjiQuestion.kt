package net.ambitious.android.playinnotification

import kotlin.random.Random

internal data class DifficultKanjiQuestion(
  val direction: DifficultKanjiQuestionDirection,
  val prompt: String,
  val choices: List<String>,
  val correctAnswer: String,
) {
  companion object {
    fun create(
      entriesByDifficulty: Map<Int, List<DifficultKanjiEntry>>,
      difficulty: Int,
      random: Random = Random.Default,
      previousQuestion: DifficultKanjiQuestion? = null,
    ): DifficultKanjiQuestion {
      val entries = requireNotNull(entriesByDifficulty[difficulty]) {
        "難易度$difficulty の問題データがありません"
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
          previousQuestion == null || when (previousQuestion.direction) {
            DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> {
              entry.writtenForm != previousQuestion.prompt ||
                entry.reading != previousQuestion.correctAnswer
            }
            DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> {
              entry.writtenForm != previousQuestion.correctAnswer ||
                entry.reading != previousQuestion.prompt
            }
          }
        }
        .random(random)
      val correctAnswer = when (direction) {
        DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> correctEntry.reading
        DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> correctEntry.writtenForm
      }
      val wrongAnswers = entries
        .map {
          when (direction) {
            DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> it.reading
            DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> it.writtenForm
          }
        }
        .filter { it != correctAnswer }
        .shuffled(random)
        .take(2)

      require(wrongAnswers.size == 2) { "3択の誤答候補を構成できません" }
      return DifficultKanjiQuestion(
        direction = direction,
        prompt = when (direction) {
          DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> correctEntry.writtenForm
          DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> correctEntry.reading
        },
        choices = (wrongAnswers + correctAnswer).shuffled(random),
        correctAnswer = correctAnswer,
      )
    }
  }
}

internal enum class DifficultKanjiQuestionDirection {
  WRITTEN_FORM_TO_READING,
  READING_TO_WRITTEN_FORM,
}
