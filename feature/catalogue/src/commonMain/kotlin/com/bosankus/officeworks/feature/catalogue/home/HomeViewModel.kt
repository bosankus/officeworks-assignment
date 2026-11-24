package com.bosankus.officeworks.feature.catalogue.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    data class Content(val categories: List<CategoryUi>, val selectedCategory: CategoryUi? = null, val products: ProductsSection) :
        HomeUiState
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
}

internal class HomeViewModel(
    private val getFeaturedProducts: GetFeaturedProductsUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getCategoryPage: GetCategoryPageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var featured: List<ProductUi> = emptyList()
    private var categoryJob: Job? = null

    init {
        loadHome()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.CategoryTapped -> onCategoryTapped(event.category)
            HomeEvent.Retry -> retry()
        }
    }

    private fun retry() {
        val content = _state.value as? HomeUiState.Content
        val selected = content?.selectedCategory
        // Only the category failed: reload just that, keeping the chips on screen.
        if (selected != null && content.products is ProductsSection.Error) loadCategory(selected) else loadHome()
    }

    private fun onCategoryTapped(category: CategoryUi) {
        val content = _state.value as? HomeUiState.Content ?: return
        if (content.selectedCategory == category) showFeatured() else loadCategory(category)
    }

    private fun showFeatured() {
        categoryJob?.cancel()
        _state.update { current ->
            (current as? HomeUiState.Content)
                ?.copy(selectedCategory = null, products = ProductsSection.Featured(featured))
                ?: current
        }
    }

    private fun loadHome() {
        _state.value = HomeUiState.Loading
        viewModelScope.launch {
            val featuredResult = async { getFeaturedProducts() }
            val categoriesResult = async { getCategories() }
            val products = featuredResult.await()
            val categories = categoriesResult.await()

            _state.value = when {
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
        _state.update { current ->
            (current as? HomeUiState.Content)?.copy(selectedCategory = category, products = ProductsSection.Loading) ?: current
        }
        categoryJob = viewModelScope.launch {
            val section = when (val result = getCategoryPage(CategorySlug(category.slug))) {
                is DataResult.Success -> ProductsSection.Category(
                    topRated = result.data.topRated.map { it.toUi() },
                    others = result.data.others.map { it.toUi() },
                )

                is DataResult.Failure -> ProductsSection.Error(result.error.toUiError())
            }
            _state.update { current -> (current as? HomeUiState.Content)?.copy(products = section) ?: current }
        }
    }
}
