package com.bosankus.officeworks.feature.catalogue.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bosankus.officeworks.core.ui.components.AppTopBar
import com.bosankus.officeworks.core.ui.components.EmptyView
import com.bosankus.officeworks.core.ui.components.ErrorView
import com.bosankus.officeworks.core.ui.components.LoadingView
import com.bosankus.officeworks.core.ui.theme.Spacing
import com.bosankus.officeworks.feature.catalogue.components.ProductGrid
import com.bosankus.officeworks.feature.catalogue.components.SearchField
import com.bosankus.officeworks.feature.catalogue.components.productItems
import com.bosankus.officeworks.feature.catalogue.resources.Res
import com.bosankus.officeworks.feature.catalogue.resources.search_no_matches
import com.bosankus.officeworks.feature.catalogue.resources.search_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Displays the product search screen allowing users to query and view matching products.
 * Manages search query updates, state transitions, and product detail navigation.
 */
@Composable
fun SearchScreen(query: String, onBack: () -> Unit, onProductClick: (productId: Int) -> Unit) {
    val viewModel = koinViewModel<SearchViewModel> { parametersOf(query) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchContent(state, viewModel::onEvent, onBack, onProductClick)
}

@Composable
internal fun SearchContent(state: SearchUiState, onEvent: (SearchEvent) -> Unit, onBack: () -> Unit, onProductClick: (Int) -> Unit) {
    Scaffold(topBar = { AppTopBar(stringResource(Res.string.search_title), onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            SearchField(
                query = state.query,
                onQueryChange = { onEvent(SearchEvent.QueryChanged(it)) },
                onSearch = { onEvent(SearchEvent.Submit) },
                modifier = Modifier.padding(horizontal = Spacing.medium),
            )
            when (val results = state.results) {
                SearchResults.Idle -> Unit
                SearchResults.Loading -> LoadingView()
                is SearchResults.Error -> ErrorView(results.error, onRetry = { onEvent(SearchEvent.Retry) })
                is SearchResults.NoMatches -> EmptyView(stringResource(Res.string.search_no_matches, results.query))
                is SearchResults.Found -> ProductGrid { productItems(results.products, onProductClick) }
            }
        }
    }
}
