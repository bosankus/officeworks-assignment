package com.bosankus.officeworks.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.bosankus.officeworks.core.ui.resources.Res
import com.bosankus.officeworks.core.ui.resources.retry
import com.bosankus.officeworks.core.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

object StatusTestTags {
    const val LOADING = "status_loading"
    const val ERROR = "status_error"
    const val EMPTY = "status_empty"
    const val RETRY = "status_retry"
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().testTag(StatusTestTags.LOADING), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorView(error: UiError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    CenteredMessage(error.message(), modifier.testTag(StatusTestTags.ERROR)) {
        Button(onClick = onRetry, modifier = Modifier.testTag(StatusTestTags.RETRY)) {
            Text(stringResource(Res.string.retry))
        }
    }
}

@Composable
fun EmptyView(message: String, modifier: Modifier = Modifier) {
    CenteredMessage(message, modifier.testTag(StatusTestTags.EMPTY))
}

@Composable
private fun CenteredMessage(message: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        action()
    }
}
