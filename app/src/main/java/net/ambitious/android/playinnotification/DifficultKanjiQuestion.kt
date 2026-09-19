package net.ambitious.android.playinnotification

import kotlin.random.Random

private const val CORRECT_ANSWER_COUNT = 1
private const val WRONG_ANSWER_COUNT = 2
private const val ANSWER_CHOICE_COUNT = CORRECT_ANSWER_COUNT + WRONG_ANSWER_COUNT
private const val IGNORED_WRITTEN_FORM_SIMILARITY_SCORE = 0

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
          entry !in askedEntries && entry != previousQuestion?.entry
        }
      val correctEntry = eligibleEntries.random(random)
      val correctAnswer = when (direction) {
        DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> correctEntry.reading
        DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> correctEntry.writtenForm
      }
      // 読みの近さを両方向で優先し、同点の候補は事前のシャッフル順で選ぶ。
      val wrongAnswerEntries = entries
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
              DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING ->
                IGNORED_WRITTEN_FORM_SIMILARITY_SCORE
              DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> {
                it.writtenForm.commonPrefixWith(correctEntry.writtenForm).length +
                  it.writtenForm.commonSuffixWith(correctEntry.writtenForm).length
              }
            }
          },
        )
        .take(WRONG_ANSWER_COUNT)
      val wrongAnswers = wrongAnswerEntries.map {
        when (direction) {
          DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> it.reading
          DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> it.writtenForm
        }
      }

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
