package com.bosankus.officeworks.feature.catalogue.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.result.DataResult
import com.bosankus.officeworks.core.domain.usecase.GetProductUseCase
import com.bosankus.officeworks.core.ui.components.UiError
import com.bosankus.officeworks.core.ui.components.toUiError
import com.bosankus.officeworks.feature.catalogue.model.ProductUi
import com.bosankus.officeworks.feature.catalogue.model.toUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
internal sealed interface ProductDetailUiState {
    data object Loading : ProductDetailUiState
    data class Content(val product: ProductUi) : ProductDetailUiState
    data class Error(val error: UiError) : ProductDetailUiState
}

internal sealed interface ProductDetailEvent {
    data object Retry : ProductDetailEvent
}

internal class ProductDetailViewModel(private val productId: ProductId, private val getProduct: GetProductUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: ProductDetailEvent) {
        when (event) {
            ProductDetailEvent.Retry -> load()
        }
    }

    private fun load() {
        _uiState.value = ProductDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = getProduct(productId)) {
                is DataResult.Success -> ProductDetailUiState.Content(result.data.toUi())
                is DataResult.Failure -> ProductDetailUiState.Error(result.error.toUiError())
            }
        }
    }
}
