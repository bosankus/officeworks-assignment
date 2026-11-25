package com.bosankus.officeworks.feature.catalogue.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bosankus.officeworks.core.domain.model.CategoryPage
import com.bosankus.officeworks.core.domain.model.CategorySlug
import com.bosankus.officeworks.core.domain.result.DataResult
import com.bosankus.officeworks.core.domain.usecase.GetCategoriesUseCase
import com.bosankus.officeworks.core.domain.usecase.GetCategoryPageUseCase
import com.bosankus.officeworks.core.domain.usecase.GetFeaturedProductsUseCase
import com.bosankus.officeworks.core.ui.components.UiError
import com.bosankus.officeworks.core.ui.components.toUiError
import com.bosankus.officeworks.feature.catalogue.model.CategoryUi
import com.bosankus.officeworks.feature.catalogue.model.ProductUi
import com.bosankus.officeworks.feature.catalogue.model.toUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
internal sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val error: UiError) : HomeUiState
    data class Content(
        val categories: List<CategoryUi>,
        val selectedCategory: CategoryUi? = null,
        val products: ProductsSection,
        val isRefreshing: Boolean = false,
        val refreshError: UiError? = null,
    ) : HomeUiState
}

@Immutable
internal sealed interface ProductsSection {
    data class Featured(val products: List<ProductUi>) : ProductsSection
    data object Loading : ProductsSection
    data class Category(val topRated: List<ProductUi>, val others: List<ProductUi>) : ProductsSection
    data class Error(val error: UiError) : ProductsSection
}

internal sealed interface HomeEvent {
    data class CategoryTapped(val category: CategoryUi) : HomeEvent
    data object Retry : HomeEvent
    data object Refresh : HomeEvent
    data object RefreshErrorShown : HomeEvent
}

internal class HomeViewModel(
    private val getFeaturedProducts: GetFeaturedProductsUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getCategoryPage: GetCategoryPageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var featured: List<ProductUi> = emptyList()
    private var categoryJob: Job? = null

    init {
        loadHome()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.CategoryTapped -> onCategoryTapped(event.category)
            HomeEvent.Retry -> retry()
            HomeEvent.Refresh -> refresh()
            HomeEvent.RefreshErrorShown -> _uiState.update { (it as? HomeUiState.Content)?.copy(refreshError = null) ?: it }
        }
    }

    private fun retry() {
        val content = _uiState.value as? HomeUiState.Content
        val selected = content?.selectedCategory
        // Only the category failed: reload just that, keeping the chips on screen.
        if (selected != null && content.products is ProductsSection.Error) loadCategory(selected) else loadHome()
    }

    /** Reloads what's on screen (featured, categories, and the selected category) without blanking it. */
    private fun refresh() {
        // Nothing loaded yet: a pull is just a full load.
        val content = _uiState.value as? HomeUiState.Content ?: return loadHome()
        if (content.isRefreshing) return
        // The refresh reloads the selected category too, so an in-flight load of it is redundant.
        categoryJob?.cancel()
        val selected = content.selectedCategory
        _uiState.value = content.copy(isRefreshing = true, refreshError = null)

        viewModelScope.launch {
            val featuredResult = async { getFeaturedProducts() }
            val categoriesResult = async { getCategories() }
            val pageResult = selected?.let { async { getCategoryPage(CategorySlug(it.slug)) } }
            val products = featuredResult.await()
            val categories = categoriesResult.await()
            val page = pageResult?.await()

            _uiState.update { current ->
                val latest = current as? HomeUiState.Content ?: return@update current
                val failure = listOfNotNull(products, categories, page).filterIsInstance<DataResult.Failure>().firstOrNull()
                if (failure != null) {
                    // Keep what's on screen: a failed refresh shouldn't take the page away.
                    return@update latest.copy(isRefreshing = false, refreshError = failure.error.toUiError())
                }
                featured = (products as DataResult.Success).data.map { it.toUi() }
                latest.refreshedWith((categories as DataResult.Success).data.map { it.toUi() }, selected, page)
            }
        }
    }

    private fun HomeUiState.Content.refreshedWith(
        categories: List<CategoryUi>,
        refreshed: CategoryUi?,
        page: DataResult<CategoryPage>?,
    ): HomeUiState.Content {
        val current = selectedCategory
        return when {
            // No category, or the selected one has gone from the catalogue: show the featured products.
            current == null || categories.none { it.slug == current.slug } ->
                copy(categories = categories, selectedCategory = null, products = ProductsSection.Featured(featured), isRefreshing = false)

            current.slug == refreshed?.slug && page is DataResult.Success ->
                copy(categories = categories, products = page.data.toSection(), isRefreshing = false)

            // The customer picked another category mid-refresh; its own load fills the section.
            else -> copy(categories = categories, isRefreshing = false)
        }
    }

    private fun onCategoryTapped(category: CategoryUi) {
        val content = _uiState.value as? HomeUiState.Content ?: return
        if (content.selectedCategory == category) showFeatured() else loadCategory(category)
    }

    private fun showFeatured() {
        categoryJob?.cancel()
        _uiState.update { current ->
            (current as? HomeUiState.Content)
                ?.copy(selectedCategory = null, products = ProductsSection.Featured(featured))
                ?: current
        }
    }

    private fun loadHome() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            val featuredResult = async { getFeaturedProducts() }
            val categoriesResult = async { getCategories() }
            val products = featuredResult.await()
            val categories = categoriesResult.await()

            _uiState.value = when {
                products is DataResult.Failure -> HomeUiState.Error(products.error.toUiError())

                categories is DataResult.Failure -> HomeUiState.Error(categories.error.toUiError())

                else -> {
                    featured = (products as DataResult.Success).data.map { it.toUi() }
                    HomeUiState.Content(
                        categories = (categories as DataResult.Success).data.map { it.toUi() },
                        products = ProductsSection.Featured(featured),
                    )
                }
            }
        }
    }

    private fun loadCategory(category: CategoryUi) {
        categoryJob?.cancel()
        _uiState.update { current ->
            (current as? HomeUiState.Content)?.copy(selectedCategory = category, products = ProductsSection.Loading) ?: current
        }
        categoryJob = viewModelScope.launch {
            val section = when (val result = getCategoryPage(CategorySlug(category.slug))) {
                is DataResult.Success -> result.data.toSection()
                is DataResult.Failure -> ProductsSection.Error(result.error.toUiError())
            }
            _uiState.update { current -> (current as? HomeUiState.Content)?.copy(products = section) ?: current }
        }
    }
}

private fun CategoryPage.toSection() = ProductsSection.Category(
    topRated = topRated.map { it.toUi() },
    others = others.map { it.toUi() },
)
