package net.ambitious.android.playinnotification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class SessionAnswersActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val questions = intent.getStringArrayListExtra(EXTRA_QUESTIONS).orEmpty()
    val selectedAnswers = intent.getStringArrayListExtra(EXTRA_SELECTED_ANSWERS).orEmpty()
    val correctness = intent.getBooleanArrayExtra(EXTRA_CORRECTNESS) ?: booleanArrayOf()
    val answerCount = minOf(questions.size, selectedAnswers.size, correctness.size)

    enableEdgeToEdge()
    setContent {
      val colorScheme = if (isSystemInDarkTheme()) {
        dynamicDarkColorScheme(this)
      } else {
        dynamicLightColorScheme(this)
      }

      MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .safeDrawingPadding()
              .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            item {
              Text(
                text = stringResource(R.string.session_answers_title),
                style = MaterialTheme.typography.headlineMedium,
              )
            }
            if (answerCount == 0) {
              item {
                Text(
                  text = stringResource(R.string.no_session_answers),
                  style = MaterialTheme.typography.bodyLarge,
                )
              }
            } else {
              items(answerCount) { index ->
                Card(modifier = Modifier.fillMaxWidth()) {
                  Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(
                      text = questions[index],
                      style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                      text = stringResource(R.string.selected_answer, selectedAnswers[index]),
                      style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                      text = stringResource(
                        if (correctness[index]) {
                          R.string.correct_answer_result
                        } else {
                          R.string.incorrect_answer_result
                        },
                      ),
                      color = if (correctness[index]) {
                        MaterialTheme.colorScheme.primary
                      } else {
                        MaterialTheme.colorScheme.error
                      },
                      style = MaterialTheme.typography.titleMedium,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  companion object {
    internal const val EXTRA_QUESTIONS = "questions"
    internal const val EXTRA_SELECTED_ANSWERS = "selected_answers"
    internal const val EXTRA_CORRECTNESS = "correctness"
  }
}
