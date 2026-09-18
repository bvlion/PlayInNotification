package net.ambitious.android.playinnotification

internal class DifficultKanjiGameService : GameSessionService<DifficultKanjiQuestion>() {
  override val gameType = GameType.DIFFICULT_KANJI
  override val wakeLockName = "difficult-kanji-game"

  private val entriesByDifficulty by lazy {
    assets.open(DifficultKanjiQuestionData.FILE_NAME).let(DifficultKanjiQuestionData::load)
  }
  override fun createNextQuestion(
    difficulty: GameDifficulty,
    askedQuestions: List<DifficultKanjiQuestion>,
  ): DifficultKanjiQuestion {
    return DifficultKanjiQuestion.create(
      entriesByDifficulty = entriesByDifficulty,
      difficulty = difficulty,
      previousQuestion = askedQuestions.lastOrNull(),
      askedEntries = askedQuestions.map(DifficultKanjiQuestion::entry),
    )
  }

  override fun questionText(question: DifficultKanjiQuestion): String = when (question.direction) {
    DifficultKanjiQuestionDirection.WRITTEN_FORM_TO_READING -> getString(
      R.string.difficult_kanji_written_form_to_reading_question,
      question.prompt,
    )
    DifficultKanjiQuestionDirection.READING_TO_WRITTEN_FORM -> getString(
      R.string.difficult_kanji_reading_to_written_form_question,
      question.prompt,
    )
  }

  override fun answerChoices(question: DifficultKanjiQuestion): List<String> = question.choices

  override fun correctAnswer(question: DifficultKanjiQuestion): String = question.correctAnswer
}
