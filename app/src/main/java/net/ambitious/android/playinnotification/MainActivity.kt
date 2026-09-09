package net.ambitious.android.playinnotification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val context = LocalContext.current
      val colorScheme = if (isSystemInDarkTheme()) {
        dynamicDarkColorScheme(context)
      } else {
        dynamicLightColorScheme(context)
      }

      MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .safeDrawingPadding(),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.headlineMedium,
            )
          }
        }
      }
    }
  }
}
