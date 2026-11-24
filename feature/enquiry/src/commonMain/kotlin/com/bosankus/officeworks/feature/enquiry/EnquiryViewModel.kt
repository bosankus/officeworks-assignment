package com.bosankus.officeworks.feature.enquiry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bosankus.officeworks.core.domain.enquiry.EnquiryField
import com.bosankus.officeworks.core.domain.enquiry.EnquiryForm
import com.bosankus.officeworks.core.domain.enquiry.FieldError
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.usecase.NewIdempotencyKeyUseCase
import com.bosankus.officeworks.core.domain.usecase.SubmitEnquiryResult
import com.bosankus.officeworks.core.domain.usecase.SubmitEnquiryUseCase
import com.bosankus.officeworks.core.ui.components.UiError
import com.bosankus.officeworks.core.ui.components.toUiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class EnquiryUiState(
    val form: EnquiryForm = EnquiryForm(),
    val fieldErrors: Map<EnquiryField, FieldError> = emptyMap(),
    val status: EnquiryStatus = EnquiryStatus.Editing,
)

internal sealed interface EnquiryStatus {
    data object Editing : EnquiryStatus
    data object Submitting : EnquiryStatus
    data object Submitted : EnquiryStatus
    data class Failed(val error: UiError) : EnquiryStatus
}

internal sealed interface EnquiryEvent {
    data class FieldChanged(val field: EnquiryField, val value: String) : EnquiryEvent
    data object Submit : EnquiryEvent
}

internal class EnquiryViewModel(
    private val productId: ProductId,
    private val submitEnquiry: SubmitEnquiryUseCase,
    newIdempotencyKey: NewIdempotencyKeyUseCase,
) : ViewModel() {

    // One key per form, reused on every retry, so the backend never emails sales twice.
    private val idempotencyKey = newIdempotencyKey()

    private val _state = MutableStateFlow(EnquiryUiState())
    val state: StateFlow<EnquiryUiState> = _state.asStateFlow()

    fun onEvent(event: EnquiryEvent) {
        when (event) {
            is EnquiryEvent.FieldChanged -> onFieldChanged(event.field, event.value)
            EnquiryEvent.Submit -> submit()
        }
    }

    private fun onFieldChanged(field: EnquiryField, value: String) {
        if (_state.value.status != EnquiryStatus.Editing && _state.value.status !is EnquiryStatus.Failed) return
        _state.update {
            it.copy(
                form = it.form.with(field, value),
                // Editing a field clears its error; the rest stay until the next submit.
                fieldErrors = it.fieldErrors - field,
                status = EnquiryStatus.Editing,
            )
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.status == EnquiryStatus.Submitting || current.status == EnquiryStatus.Submitted) return

        _state.update { it.copy(status = EnquiryStatus.Submitting) }
        viewModelScope.launch {
            val result = submitEnquiry(productId, current.form, idempotencyKey)
            _state.update {
                when (result) {
                    SubmitEnquiryResult.Sent -> it.copy(status = EnquiryStatus.Submitted)
                    is SubmitEnquiryResult.Invalid -> it.copy(fieldErrors = result.errors, status = EnquiryStatus.Editing)
                    is SubmitEnquiryResult.Failed -> it.copy(status = EnquiryStatus.Failed(result.error.toUiError()))
                }
            }
        }
    }

    private fun EnquiryForm.with(field: EnquiryField, value: String) = when (field) {
        EnquiryField.NAME -> copy(name = value)
        EnquiryField.EMAIL -> copy(email = value)
        EnquiryField.PHONE -> copy(phone = value)
        EnquiryField.MESSAGE -> copy(message = value)
    }
}
