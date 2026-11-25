package com.bosankus.officeworks.feature.catalogue.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.bosankus.officeworks.core.ui.components.ProductCard
import com.bosankus.officeworks.core.ui.resources.ic_arrow_forward
import com.bosankus.officeworks.core.ui.resources.ic_search
import com.bosankus.officeworks.core.ui.theme.Sizes
import com.bosankus.officeworks.core.ui.theme.Spacing
import com.bosankus.officeworks.feature.catalogue.CatalogueTestTags
import com.bosankus.officeworks.feature.catalogue.model.ProductUi
import com.bosankus.officeworks.feature.catalogue.resources.Res
import com.bosankus.officeworks.feature.catalogue.resources.ic_close
import com.bosankus.officeworks.feature.catalogue.resources.search_button
import com.bosankus.officeworks.feature.catalogue.resources.search_clear
import com.bosankus.officeworks.feature.catalogue.resources.search_hint
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.bosankus.officeworks.core.ui.resources.Res as CoreRes

// Content types let the grid reuse item compositions of the same kind while scrolling.
private const val TYPE_PRODUCT = "product"
private const val TYPE_STATUS = "status"

/**
 * Adaptive columns: two on a phone, more on wider screens.
 * Pass [bottomInset] when no Scaffold already pads for the navigation bar, so the last row can scroll clear of it.
 */
@Composable
internal fun ProductGrid(
    modifier: Modifier = Modifier,
    bottomInset: WindowInsets = WindowInsets(0),
    content: LazyGridScope.() -> Unit,
) {
    val bottom = Spacing.large + bottomInset.asPaddingValues().calculateBottomPadding()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = Sizes.productTileMinWidth),
        contentPadding = PaddingValues(start = Spacing.medium, end = Spacing.medium, bottom = bottom),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = modifier.fillMaxSize().testTag(CatalogueTestTags.PRODUCT_GRID),
        content = content,
    )
}

internal fun LazyGridScope.fullWidthItem(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = TYPE_STATUS) { content() }
}

internal fun LazyGridScope.productItems(products: List<ProductUi>, onProductClick: (Int) -> Unit) {
    items(products, key = { "product_${it.id}" }, contentType = { TYPE_PRODUCT }) { product ->
        ProductCard(
            title = product.title,
            price = product.price,
            imageUrl = product.imageUrl,
            rating = product.rating,
            ratingCount = product.ratingCount,
            onClick = { onProductClick(product.id) },
        )
    }
}

//
@Composable
internal fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = Spacing.small).semantics { heading() },
    )
}

/**
 * Search field with a round submit button. Empty, it shows a search icon; with text, a clear button
 * instead. Never both.
 */
@Composable
internal fun SearchField(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    val canSearch = query.isNotBlank()
    val hasText = query.isNotEmpty()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            shape = CircleShape,
            placeholder = { Text(stringResource(Res.string.search_hint)) },
            leadingIcon = if (hasText) {
                null
            } else {
                {
                    Icon(
                        painterResource(CoreRes.drawable.ic_search),
                        contentDescription = null,
                        modifier = Modifier.testTag(CatalogueTestTags.SEARCH_ICON),
                    )
                }
            },
            trailingIcon = if (hasText) {
                {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.testTag(CatalogueTestTags.SEARCH_CLEAR)) {
                        Icon(painterResource(Res.drawable.ic_close), contentDescription = stringResource(Res.string.search_clear))
                    }
                }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (canSearch) onSearch() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.weight(1f).testTag(CatalogueTestTags.SEARCH_FIELD),
        )
        FilledIconButton(
            onClick = onSearch,
            enabled = canSearch,
            modifier = Modifier.size(Sizes.buttonHeight).testTag(CatalogueTestTags.SEARCH_BUTTON),
        ) {
            Icon(
                painterResource(CoreRes.drawable.ic_arrow_forward),
                contentDescription = stringResource(Res.string.search_button),
            )
        }
    }
}
