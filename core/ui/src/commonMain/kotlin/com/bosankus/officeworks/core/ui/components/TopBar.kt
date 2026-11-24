package com.bosankus.officeworks.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.bosankus.officeworks.core.ui.resources.Res
import com.bosankus.officeworks.core.ui.resources.back
import com.bosankus.officeworks.core.ui.resources.ic_arrow_back
import com.bosankus.officeworks.core.ui.theme.Sizes
import com.bosankus.officeworks.core.ui.theme.Spacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

const val BACK_BUTTON_TAG = "back_button"

// Long product titles wrap once, then ellipsize; the bar grows to fit.
private const val TITLE_MAX_LINES = 2

/**
 * Shared top bar: round back button, centred title, optional trailing content (e.g. a price badge).
 * Pass `onBack` for every screen except the home page.
 */
@Composable
fun AppTopBar(title: String, onBack: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Box(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = Sizes.topBarHeight).padding(start = Spacing.medium),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (onBack != null) BackButton(onBack)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = TITLE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                // Leave room for the back button and the trailing slot so the title stays centred.
                .padding(horizontal = Sizes.touchTarget + Spacing.large, vertical = Spacing.small)
                .semantics { heading() },
        )
        if (trailing != null) {
            Row(Modifier.align(Alignment.CenterEnd)) { trailing() }
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(
        onClick = onBack,
        modifier = Modifier
            .size(Sizes.touchTarget)
            .border(Sizes.border, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .testTag(BACK_BUTTON_TAG),
    ) {
        Icon(
            painterResource(Res.drawable.ic_arrow_back),
            contentDescription = stringResource(Res.string.back),
            modifier = Modifier.size(Sizes.iconMedium),
        )
    }
}
