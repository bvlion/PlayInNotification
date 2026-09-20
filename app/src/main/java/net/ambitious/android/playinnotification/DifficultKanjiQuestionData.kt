package net.ambitious.android.playinnotification

import java.io.InputStream

internal data class DifficultKanjiEntry(
  val writtenForm: String,
  val reading: String,
)

internal object DifficultKanjiQuestionData {
  const val FILE_NAME = "difficult_kanji_questions.tsv"
  private const val TSV_COLUMN_COUNT = 3
  private const val DIFFICULTY_COLUMN_INDEX = 0
  private const val WRITTEN_FORM_COLUMN_INDEX = 1
  private const val READING_COLUMN_INDEX = 2

  fun load(inputStream: InputStream): Map<Int, List<DifficultKanjiEntry>> =
    inputStream.bufferedReader().use { reader ->
      require(reader.readLine() == "difficulty\twrittenForm\treading") {
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
          )
        }
        .groupBy({ it.first }, { it.second })
    }
}
