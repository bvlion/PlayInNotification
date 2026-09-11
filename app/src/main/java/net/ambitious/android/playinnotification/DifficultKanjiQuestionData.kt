package net.ambitious.android.playinnotification

import java.io.InputStream

internal data class DifficultKanjiEntry(
  val writtenForm: String,
  val reading: String,
)

internal object DifficultKanjiQuestionData {
  const val fileName = "difficult_kanji_questions.tsv"

  fun load(inputStream: InputStream): Map<Int, List<DifficultKanjiEntry>> =
    inputStream.bufferedReader().use { reader ->
      require(reader.readLine() == "difficulty\twrittenForm\treading") {
        "問題データのヘッダーが不正です"
      }
      reader.lineSequence()
        .filter(String::isNotBlank)
        .map { line ->
          val columns = line.split('\t')
          require(columns.size == 3) { "問題データの形式が不正です: $line" }
          columns[0].toInt() to DifficultKanjiEntry(columns[1], columns[2])
        }
        .groupBy({ it.first }, { it.second })
    }
}
