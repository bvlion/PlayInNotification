package net.ambitious.android.playinnotification

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val SESSION_ANSWERS_CONTENT_PADDING = 24.dp
private val SESSION_ANSWERS_LIST_ITEM_SPACING = 16.dp
private val SESSION_ANSWER_CARD_CONTENT_SPACING = 8.dp

class SessionAnswersActivity : ComponentActivity() {
  internal var answers by mutableStateOf(emptyList<GameAnswerResult>())
    private set

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    answers = savedInstanceState?.getAnswerResults(SAVED_ANSWERS)
      ?: intent.extras?.getAnswerResults(EXTRA_ANSWERS)
      .orEmpty()
    if (savedInstanceState == null) {
      sendBroadcast(
        Intent(this, GameNotificationActionReceiver::class.java)
          .setAction(GameNotificationActionReceiver.ACTION_SHOW_GAME_SELECTION),
      )
    }
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
              .padding(SESSION_ANSWERS_CONTENT_PADDING),
            contentPadding = PaddingValues(bottom = SESSION_ANSWERS_CONTENT_PADDING),
            verticalArrangement = Arrangement.spacedBy(SESSION_ANSWERS_LIST_ITEM_SPACING),
          ) {
            item {
              Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                  text = stringResource(R.string.session_answers_title),
                  modifier = Modifier.weight(1f),
                  style = MaterialTheme.typography.headlineMedium,
                )
                IconButton(
                  onClick = { finish() },
                  modifier = Modifier.semantics {
                    contentDescription = getString(R.string.close_session_answers)
                  },
                ) {
                  Text(
                    text = "×",
                    style = MaterialTheme.typography.headlineMedium,
                  )
                }
              }
            }
            if (answers.isEmpty()) {
              item {
                Text(
                  text = stringResource(R.string.no_session_answers),
                  style = MaterialTheme.typography.bodyLarge,
                )
              }
            } else {
              items(answers) { answer ->
                Card(modifier = Modifier.fillMaxWidth()) {
                  Column(
                    modifier = Modifier.padding(SESSION_ANSWERS_CONTENT_PADDING),
                    verticalArrangement = Arrangement.spacedBy(SESSION_ANSWER_CARD_CONTENT_SPACING),
                  ) {
                    Text(
                      text = answer.question,
                      style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                      text = stringResource(R.string.selected_answer, answer.selectedAnswer),
                      style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                      text = stringResource(
                        if (answer.isCorrect) {
                          R.string.correct_answer_result
                        } else {
                          R.string.incorrect_answer_result
                        },
                      ),
                      color = if (answer.isCorrect) {
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

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putAnswerResults(SAVED_ANSWERS, answers)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    answers = intent.extras?.getAnswerResults(EXTRA_ANSWERS).orEmpty()
    sendBroadcast(
      Intent(this, GameNotificationActionReceiver::class.java)
        .setAction(GameNotificationActionReceiver.ACTION_SHOW_GAME_SELECTION),
    )
  }

  companion object {
    private const val SAVED_ANSWERS = "saved_answers"
    private const val EXTRA_ANSWERS = "answers"
    private const val ANSWER_QUESTION = "question"
    private const val ANSWER_SELECTED_ANSWER = "selected_answer"
    private const val ANSWER_IS_CORRECT = "is_correct"
    private const val ANSWER_EARNED_POINTS = "earned_points"

    internal fun createIntent(
      context: Context,
      answers: List<GameAnswerResult>,
    ): Intent = Intent(context, SessionAnswersActivity::class.java)
      .putExtras(Bundle().apply { putAnswerResults(EXTRA_ANSWERS, answers) })

    private fun Bundle.putAnswerResults(key: String, answers: List<GameAnswerResult>) {
      putParcelableArrayList(
        key,
        ArrayList(answers.map { answer ->
          Bundle().apply {
            putString(ANSWER_QUESTION, answer.question)
            putString(ANSWER_SELECTED_ANSWER, answer.selectedAnswer)
            putBoolean(ANSWER_IS_CORRECT, answer.isCorrect)
            putInt(ANSWER_EARNED_POINTS, answer.earnedPoints)
          }
        }),
      )
    }

    private fun Bundle.getAnswerResults(key: String): List<GameAnswerResult>? {
      val answerBundles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayList(key, Bundle::class.java)
      } else {
        @Suppress("DEPRECATION")
        getParcelableArrayList<Bundle>(key)
      } ?: return null
      return answerBundles.mapNotNull { answerBundle ->
        val question = answerBundle.getString(ANSWER_QUESTION) ?: return@mapNotNull null
        val selectedAnswer = answerBundle.getString(ANSWER_SELECTED_ANSWER)
          ?: return@mapNotNull null
        GameAnswerResult(
          question = question,
          selectedAnswer = selectedAnswer,
          isCorrect = answerBundle.getBoolean(ANSWER_IS_CORRECT),
          earnedPoints = answerBundle.getInt(ANSWER_EARNED_POINTS),
        )
      }
    }
  }
}
