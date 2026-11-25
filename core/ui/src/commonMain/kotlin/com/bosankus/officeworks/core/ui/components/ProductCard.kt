package com.bosankus.officeworks.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.bosankus.officeworks.core.ui.resources.Res
import com.bosankus.officeworks.core.ui.resources.ic_star
import com.bosankus.officeworks.core.ui.resources.rating_description
import com.bosankus.officeworks.core.ui.resources.rating_value
import com.bosankus.officeworks.core.ui.theme.Sizes
import com.bosankus.officeworks.core.ui.theme.Spacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

const val PRODUCT_CARD_TAG = "product_card"

/** Takes plain values, not domain models, so this module stays free of business types. */
@Composable
fun ProductCard(
    title: String,
    price: String,
    imageUrl: String,
    rating: Double,
    ratingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .testTag(PRODUCT_CARD_TAG),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
    ) {
        ProductTile(imageUrl, Modifier.fillMaxWidth().aspectRatio(0.85f))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Spacing.extraSmall),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.extraSmall, vertical = Spacing.extraSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                price,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            RatingRow(rating, ratingCount, showCount = false)
        }
    }
}

/** Grid tile: product photos are shot on white, so they sit on a white, bordered tile in both themes. */
@Composable
private fun ProductTile(url: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(Color.White)
            .border(Sizes.border, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large),
        contentAlignment = Alignment.Center,
    ) {
        ProductImage(url, Modifier.fillMaxSize().padding(Spacing.medium))
    }
}

/** Just the product photo, with no frame, so each screen decides how to present it. */
@Composable
fun ProductImage(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        // Decorative: the product title on the same screen already describes it.
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Composable
fun RatingRow(rating: Double, count: Int, modifier: Modifier = Modifier, showCount: Boolean = true) {
    val description = stringResource(Res.string.rating_description, "$rating", "$count")
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_star),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(Sizes.iconSmall),
        )
        Text(
            if (showCount) stringResource(Res.string.rating_value, "$rating", "$count") else rating.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
