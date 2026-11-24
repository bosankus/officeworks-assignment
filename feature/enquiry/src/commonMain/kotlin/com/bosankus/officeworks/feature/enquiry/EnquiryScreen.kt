package com.bosankus.officeworks.feature.enquiry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bosankus.officeworks.core.domain.enquiry.EnquiryField
import com.bosankus.officeworks.core.domain.enquiry.EnquiryFormValidator
import com.bosankus.officeworks.core.domain.enquiry.FieldError
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.ui.components.AppTopBar
import com.bosankus.officeworks.core.ui.components.message
import com.bosankus.officeworks.core.ui.theme.Sizes
import com.bosankus.officeworks.core.ui.theme.Spacing
import com.bosankus.officeworks.feature.enquiry.resources.Res
import com.bosankus.officeworks.feature.enquiry.resources.back_to_product
import com.bosankus.officeworks.feature.enquiry.resources.enquiry_about
import com.bosankus.officeworks.feature.enquiry.resources.enquiry_intro
import com.bosankus.officeworks.feature.enquiry.resources.enquiry_title
import com.bosankus.officeworks.feature.enquiry.resources.error_invalid_format
import com.bosankus.officeworks.feature.enquiry.resources.error_required
import com.bosankus.officeworks.feature.enquiry.resources.error_too_long
import com.bosankus.officeworks.feature.enquiry.resources.field_email
import com.bosankus.officeworks.feature.enquiry.resources.field_message
import com.bosankus.officeworks.feature.enquiry.resources.field_name
import com.bosankus.officeworks.feature.enquiry.resources.field_phone
import com.bosankus.officeworks.feature.enquiry.resources.message_counter
import com.bosankus.officeworks.feature.enquiry.resources.submit
import com.bosankus.officeworks.feature.enquiry.resources.thank_you_body
import com.bosankus.officeworks.feature.enquiry.resources.thank_you_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EnquiryScreen(productId: Int, productTitle: String, onBack: () -> Unit) {
    val viewModel = koinViewModel<EnquiryViewModel> { parametersOf(ProductId(productId)) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    EnquiryContent(productTitle, state, viewModel::onEvent, onBack)
}

@Composable
internal fun EnquiryContent(productTitle: String, state: EnquiryUiState, onEvent: (EnquiryEvent) -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { AppTopBar(stringResource(Res.string.enquiry_title), onBack) }) { padding ->
        val modifier = Modifier.padding(padding)
        if (state.status == EnquiryStatus.Submitted) {
            ThankYou(productTitle, onBack, modifier)
        } else {
            EnquiryForm(productTitle, state, onEvent, modifier)
        }
    }
}

@Composable
private fun EnquiryForm(productTitle: String, state: EnquiryUiState, onEvent: (EnquiryEvent) -> Unit, modifier: Modifier) {
    val submitting = state.status == EnquiryStatus.Submitting
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(stringResource(Res.string.enquiry_about, productTitle), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.enquiry_intro), style = MaterialTheme.typography.bodyMedium)

        FormField(
            field = EnquiryField.NAME,
            label = Res.string.field_name,
            value = state.form.name,
            state = state,
            onEvent = onEvent,
            tag = EnquiryTestTags.NAME_FIELD,
            keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        FormField(
            field = EnquiryField.EMAIL,
            label = Res.string.field_email,
            value = state.form.email,
            state = state,
            onEvent = onEvent,
            tag = EnquiryTestTags.EMAIL_FIELD,
            keyboard = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        FormField(
            field = EnquiryField.PHONE,
            label = Res.string.field_phone,
            value = state.form.phone,
            state = state,
            onEvent = onEvent,
            tag = EnquiryTestTags.PHONE_FIELD,
            keyboard = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        )
        FormField(
            field = EnquiryField.MESSAGE, label = Res.string.field_message, value = state.form.message, state = state,
            onEvent = onEvent, tag = EnquiryTestTags.MESSAGE_FIELD,
            keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            singleLine = false,
            counter = stringResource(
                Res.string.message_counter,
                state.form.message.length.toString(),
                EnquiryFormValidator.MAX_MESSAGE.toString(),
            ),
        )

        val status = state.status
        if (status is EnquiryStatus.Failed) {
            Text(
                text = status.error.message(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(EnquiryTestTags.SUBMIT_ERROR).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        Button(
            onClick = { onEvent(EnquiryEvent.Submit) },
            enabled = !submitting,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().padding(
                top = Spacing.small,
            ).height(Sizes.buttonHeight).testTag(EnquiryTestTags.SUBMIT_BUTTON),
        ) {
            if (submitting) {
                CircularProgressIndicator(Modifier.size(Sizes.iconMedium), strokeWidth = Sizes.progressStroke)
            } else {
                Text(stringResource(Res.string.submit))
            }
        }
    }
}

@Composable
private fun FormField(
    field: EnquiryField,
    label: StringResource,
    value: String,
    state: EnquiryUiState,
    onEvent: (EnquiryEvent) -> Unit,
    tag: String,
    keyboard: KeyboardOptions,
    singleLine: Boolean = true,
    counter: String? = null,
) {
    val error = state.fieldErrors[field]
    val supporting = error?.label() ?: counter
    OutlinedTextField(
        value = value,
        onValueChange = { onEvent(EnquiryEvent.FieldChanged(field, it)) },
        label = { Text(stringResource(label)) },
        isError = error != null,
        supportingText = if (supporting != null) {
            { Text(supporting) }
        } else {
            null
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = keyboard,
        enabled = state.status != EnquiryStatus.Submitting,
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}

@Composable
private fun FieldError.label(): String = stringResource(
    when (this) {
        FieldError.REQUIRED -> Res.string.error_required
        FieldError.INVALID_FORMAT -> Res.string.error_invalid_format
        FieldError.TOO_LONG -> Res.string.error_too_long
    },
)

@Composable
private fun ThankYou(productTitle: String, onBack: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.large).testTag(EnquiryTestTags.THANK_YOU),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(Res.string.thank_you_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            stringResource(Res.string.thank_you_body, productTitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onBack, shape = CircleShape, modifier = Modifier.testTag(EnquiryTestTags.BACK_TO_PRODUCT)) {
            Text(stringResource(Res.string.back_to_product))
        }
    }
}
