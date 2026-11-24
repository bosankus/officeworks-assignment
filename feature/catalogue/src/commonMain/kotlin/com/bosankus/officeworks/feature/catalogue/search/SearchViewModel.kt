package com.bosankus.officeworks.feature.catalogue.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bosankus.officeworks.core.domain.result.DataResult
import com.bosankus.officeworks.core.domain.usecase.SearchProductsUseCase
import com.bosankus.officeworks.core.ui.components.UiError
import com.bosankus.officeworks.core.ui.components.toUiError
import com.bosankus.officeworks.feature.catalogue.model.ProductUi
import com.bosankus.officeworks.feature.catalogue.model.toUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class SearchUiState(val query: String = "", val results: SearchResults = SearchResults.Idle)

internal sealed interface SearchResults {
    data object Idle : SearchResults
    data object Loading : SearchResults
    data class Found(val products: List<ProductUi>) : SearchResults
    data class NoMatches(val query: String) : SearchResults
    data class Error(val error: UiError) : SearchResults
}

internal sealed interface SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data object Submit : SearchEvent
    data object Retry : SearchEvent
}

internal class SearchViewModel(initialQuery: String, private val searchProducts: SearchProductsUseCase) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState(query = initialQuery))
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var lastSubmitted = ""

    init {
        if (initialQuery.isNotBlank()) search(initialQuery)
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> _state.update { it.copy(query = event.query) }
            SearchEvent.Submit -> search(_state.value.query)
            SearchEvent.Retry -> search(lastSubmitted)
        }
    }

    private fun search(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isEmpty()) return
        lastSubmitted = query
        // A newer search wins; don't let a slow older response overwrite it.
        searchJob?.cancel()
        _state.update { it.copy(results = SearchResults.Loading) }
        searchJob = viewModelScope.launch {
            val results = when (val result = searchProducts(query)) {
                is DataResult.Success ->
                    if (result.data.isEmpty()) {
                        SearchResults.NoMatches(query)
                    } else {
                        SearchResults.Found(result.data.map { it.toUi() })
                    }
                is DataResult.Failure -> SearchResults.Error(result.error.toUiError())
            }
            _state.update { it.copy(results = results) }
        }
    }
}
