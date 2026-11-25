package com.bosankus.officeworks.feature.catalogue.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.ui.components.AppTopBar
import com.bosankus.officeworks.core.ui.components.ErrorView
import com.bosankus.officeworks.core.ui.components.LoadingView
import com.bosankus.officeworks.core.ui.components.ProductImage
import com.bosankus.officeworks.core.ui.components.RatingRow
import com.bosankus.officeworks.core.ui.theme.Sizes
import com.bosankus.officeworks.core.ui.theme.Spacing
import com.bosankus.officeworks.feature.catalogue.CatalogueTestTags
import com.bosankus.officeworks.feature.catalogue.model.ProductUi
import com.bosankus.officeworks.feature.catalogue.resources.Res
import com.bosankus.officeworks.feature.catalogue.resources.enquire
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Displays detailed information for a specific product.
 * Handles back navigation and product enquiry actions.
 */
@Composable
fun ProductDetailScreen(productId: Int, onBack: () -> Unit, onEnquire: (productId: Int, productTitle: String) -> Unit) {
    val viewModel = koinViewModel<ProductDetailViewModel> { parametersOf(ProductId(productId)) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProductDetailContent(state, viewModel::onEvent, onBack, onEnquire)
}

@Composable
internal fun ProductDetailContent(
    state: ProductDetailUiState,
    onEvent: (ProductDetailEvent) -> Unit,
    onBack: () -> Unit,
    onEnquire: (Int, String) -> Unit,
) {
    val product = (state as? ProductDetailUiState.Content)?.product
    Scaffold(
        topBar = {
            AppTopBar(
                title = String(),
                onBack = onBack,
                trailing = if (product != null) {
                    { PriceBadge(product.price) }
                } else {
                    null
                },
            )
        },
        bottomBar = {
            if (product != null) {
                Button(
                    onClick = { onEnquire(product.id, product.title) },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(Spacing.medium)
                        .height(Sizes.buttonHeight)
                        .testTag(CatalogueTestTags.ENQUIRE_BUTTON),
                ) {
                    Text(stringResource(Res.string.enquire), style = MaterialTheme.typography.titleMedium)
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (state) {
            ProductDetailUiState.Loading -> LoadingView(modifier)
            is ProductDetailUiState.Error -> ErrorView(state.error, { onEvent(ProductDetailEvent.Retry) }, modifier)
            is ProductDetailUiState.Content -> ProductDetails(state.product, modifier)
        }
    }
}

@Composable
private fun PriceBadge(price: String) {
    Text(
        text = price,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = Sizes.badgeCornerRadius, bottomStart = Sizes.badgeCornerRadius))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
    )
}

@Composable
private fun ProductDetails(product: ProductUi, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        ProductStage(product.imageUrl)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                product.categoryName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            RatingRow(product.rating, product.ratingCount)
        }
        Text(
            text = product.title,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = product.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.medium),
        )
    }
}

@Composable
private fun ProductStage(imageUrl: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = Spacing.small), contentAlignment = Alignment.Center) {
        ProductImage(
            url = imageUrl,
            modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f),
        )
    }
}
