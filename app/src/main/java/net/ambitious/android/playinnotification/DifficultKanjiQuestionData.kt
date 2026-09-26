package net.ambitious.android.playinnotification

import java.io.InputStream

internal data class DifficultKanjiEntry(
  val writtenForm: String,
  val reading: String,
  val writtenFormWrongAnswers: List<String> = emptyList(),
  val readingWrongAnswers: List<String> = emptyList(),
)

internal object DifficultKanjiQuestionData {
  const val FILE_NAME = "difficult_kanji_questions.tsv"
  private const val TSV_COLUMN_COUNT = 5
  private const val DIFFICULTY_COLUMN_INDEX = 0
  private const val WRITTEN_FORM_COLUMN_INDEX = 1
  private const val READING_COLUMN_INDEX = 2
  private const val WRITTEN_FORM_WRONG_ANSWERS_COLUMN_INDEX = 3
  private const val READING_WRONG_ANSWERS_COLUMN_INDEX = 4
  private const val WRONG_ANSWER_SEPARATOR = '|'
  private const val NO_WRONG_ANSWERS = "-"

  fun load(inputStream: InputStream): Map<Int, List<DifficultKanjiEntry>> =
    inputStream.bufferedReader().use { reader ->
      require(
        reader.readLine() ==
          "difficulty\twrittenForm\treading\twrittenFormWrongAnswers\treadingWrongAnswers",
      ) {
        "問題データのヘッダーが不正です"
      }
      reader.lineSequence()
        .filter(String::isNotBlank)
        .map { line ->
          val columns = line.split('\t')
          require(columns.size == TSV_COLUMN_COUNT) { "問題データの形式が不正です: $line" }
          columns[DIFFICULTY_COLUMN_INDEX].toInt() to DifficultKanjiEntry(
            columns[WRITTEN_FORM_COLUMN_INDEX],
            columns[READING_COLUMN_INDEX],
            columns[WRITTEN_FORM_WRONG_ANSWERS_COLUMN_INDEX]
              .takeUnless { it == NO_WRONG_ANSWERS }?.split(WRONG_ANSWER_SEPARATOR).orEmpty(),
            columns[READING_WRONG_ANSWERS_COLUMN_INDEX]
              .takeUnless { it == NO_WRONG_ANSWERS }?.split(WRONG_ANSWER_SEPARATOR).orEmpty(),
          )
        }
        .groupBy({ it.first }, { it.second })
    }
}
