package com.bosankus.officeworks.feature.catalogue.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bosankus.officeworks.core.ui.components.EmptyView
import com.bosankus.officeworks.core.ui.components.ErrorView
import com.bosankus.officeworks.core.ui.components.LoadingView
import com.bosankus.officeworks.core.ui.components.message
import com.bosankus.officeworks.core.ui.theme.Sizes
import com.bosankus.officeworks.core.ui.theme.Spacing
import com.bosankus.officeworks.feature.catalogue.CatalogueTestTags
import com.bosankus.officeworks.feature.catalogue.components.ProductGrid
import com.bosankus.officeworks.feature.catalogue.components.SearchField
import com.bosankus.officeworks.feature.catalogue.components.SectionHeader
import com.bosankus.officeworks.feature.catalogue.components.fullWidthItem
import com.bosankus.officeworks.feature.catalogue.components.productItems
import com.bosankus.officeworks.feature.catalogue.model.CategoryUi
import com.bosankus.officeworks.feature.catalogue.resources.Res
import com.bosankus.officeworks.feature.catalogue.resources.category_empty
import com.bosankus.officeworks.feature.catalogue.resources.home_subtitle
import com.bosankus.officeworks.feature.catalogue.resources.home_title
import com.bosankus.officeworks.feature.catalogue.resources.more_in_category
import com.bosankus.officeworks.feature.catalogue.resources.top_rated
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(onSearch: (query: String) -> Unit, onProductClick: (productId: Int) -> Unit) {
    val viewModel = koinViewModel<HomeViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(state, viewModel::onEvent, onSearch, onProductClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeContent(state: HomeUiState, onEvent: (HomeEvent) -> Unit, onSearch: (String) -> Unit, onProductClick: (Int) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // A failed refresh keeps the products on screen, so it's reported in a snackbar rather than an error view.
    val refreshError = (state as? HomeUiState.Content)?.refreshError
    val refreshErrorMessage = refreshError?.message()
    LaunchedEffect(refreshError) {
        if (refreshErrorMessage != null) {
            snackbarHostState.showSnackbar(refreshErrorMessage)
            onEvent(HomeEvent.RefreshErrorShown)
        }
    }

    // Surface, not Modifier.background: it also sets the content colour, so text without an explicit
    // colour is light in dark theme instead of falling back to black.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Header()
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { onSearch(query.trim()) },
                    modifier = Modifier.padding(horizontal = Spacing.medium),
                )
                when (state) {
                    HomeUiState.Loading -> LoadingView()
                    is HomeUiState.Error -> ErrorView(state.error, onRetry = { onEvent(HomeEvent.Retry) })
                    is HomeUiState.Content -> {
                        CategoryChips(state.categories, state.selectedCategory, onEvent)
                        // Pull on the grid only: the chips above scroll sideways.
                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = { onEvent(HomeEvent.Refresh) },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            ProductGrid { productsSection(state.products, state.selectedCategory, onEvent, onProductClick) }
                        }
                    }
                }
            }
            SnackbarHost(
                snackbarHostState,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(Spacing.medium),
            )
        }
    }
}

@Composable
private fun Header() {
    Column(Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.medium)) {
        Text(
            stringResource(Res.string.home_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(Res.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun LazyGridScope.productsSection(
    section: ProductsSection,
    selected: CategoryUi?,
    onEvent: (HomeEvent) -> Unit,
    onProductClick: (Int) -> Unit,
) {
    when (section) {
        // The backend returns up to 5; with none, skip the section rather than show an empty header.
        is ProductsSection.Featured -> if (section.products.isNotEmpty()) {
            fullWidthItem("featured_header") { SectionHeader(stringResource(Res.string.top_rated)) }
            productItems(section.products, onProductClick)
        }
        ProductsSection.Loading -> fullWidthItem("section_loading") { LoadingView(Modifier.padding(Spacing.large)) }
        is ProductsSection.Error -> fullWidthItem("section_error") {
            ErrorView(section.error, onRetry = { onEvent(HomeEvent.Retry) })
        }
        is ProductsSection.Category -> if (section.topRated.isEmpty()) {
            fullWidthItem("category_empty") { EmptyView(stringResource(Res.string.category_empty)) }
        } else {
            fullWidthItem("category_top_header") { SectionHeader(stringResource(Res.string.top_rated)) }
            productItems(section.topRated, onProductClick)
            if (section.others.isNotEmpty() && selected != null) {
                fullWidthItem("category_others_header") {
                    SectionHeader(stringResource(Res.string.more_in_category, selected.name))
                }
                productItems(section.others, onProductClick)
            }
        }
    }
}

@Composable
private fun CategoryChips(categories: List<CategoryUi>, selected: CategoryUi?, onEvent: (HomeEvent) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.medium, vertical = Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        items(categories, key = { it.slug }) { category ->
            CategoryPill(
                label = category.name,
                icon = categoryIcon(category.slug),
                selected = category == selected,
                onClick = { onEvent(HomeEvent.CategoryTapped(category)) },
                modifier = Modifier.testTag(CatalogueTestTags.CATEGORY_CHIP),
            )
        }
    }
}

/** Pill with the category icon: outlined by default, filled when selected. Icon and label share a colour in both themes. */
@Composable
private fun CategoryPill(label: String, icon: CategoryIcon, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val contentColor = if (selected) colors.onPrimary else colors.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) colors.primary else colors.surfaceVariant)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = Sizes.touchTarget)
            .padding(horizontal = Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Icon(
            painter = painterResource(if (selected) icon.filled else icon.outlined),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(Sizes.iconMedium),
        )
        Text(label, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}
