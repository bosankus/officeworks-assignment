package com.bosankus.officeworks.feature.enquiry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bosankus.officeworks.core.domain.enquiry.EnquiryField
import com.bosankus.officeworks.core.domain.enquiry.EnquiryFormValidator
import com.bosankus.officeworks.core.domain.enquiry.FieldError
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.ui.components.AppTopBar
import com.bosankus.officeworks.core.ui.components.ProductImage
import com.bosankus.officeworks.core.ui.components.UiError
import com.bosankus.officeworks.core.ui.components.message
import com.bosankus.officeworks.core.ui.theme.Sizes
import com.bosankus.officeworks.core.ui.theme.Spacing
import com.bosankus.officeworks.feature.enquiry.resources.Res
import com.bosankus.officeworks.feature.enquiry.resources.back_to_product
import com.bosankus.officeworks.feature.enquiry.resources.enquiry_intro
import com.bosankus.officeworks.feature.enquiry.resources.enquiry_title
import com.bosankus.officeworks.feature.enquiry.resources.error_invalid_format
import com.bosankus.officeworks.feature.enquiry.resources.error_required
import com.bosankus.officeworks.feature.enquiry.resources.error_too_long
import com.bosankus.officeworks.feature.enquiry.resources.field_email
import com.bosankus.officeworks.feature.enquiry.resources.field_message
import com.bosankus.officeworks.feature.enquiry.resources.field_name
import com.bosankus.officeworks.feature.enquiry.resources.field_phone
import com.bosankus.officeworks.feature.enquiry.resources.ic_check
import com.bosankus.officeworks.feature.enquiry.resources.ic_email
import com.bosankus.officeworks.feature.enquiry.resources.ic_error
import com.bosankus.officeworks.feature.enquiry.resources.ic_person
import com.bosankus.officeworks.feature.enquiry.resources.ic_phone
import com.bosankus.officeworks.feature.enquiry.resources.message_counter
import com.bosankus.officeworks.feature.enquiry.resources.section_details
import com.bosankus.officeworks.feature.enquiry.resources.section_message
import com.bosankus.officeworks.feature.enquiry.resources.submit
import com.bosankus.officeworks.feature.enquiry.resources.thank_you_body
import com.bosankus.officeworks.feature.enquiry.resources.thank_you_title
import com.bosankus.officeworks.feature.enquiry.resources.view_product
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Displays the product enquiry form for submitting customer enquiries.
 * Handles form validation, submission state, and navigation.
 */
@Composable
fun EnquiryScreen(productId: Int, productTitle: String, onBack: () -> Unit) {
    val viewModel = koinViewModel<EnquiryViewModel> { parametersOf(ProductId(productId)) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    EnquiryContent(productTitle, state, viewModel::onEvent, onBack)
}

@Composable
internal fun EnquiryContent(productTitle: String, state: EnquiryUiState, onEvent: (EnquiryEvent) -> Unit, onBack: () -> Unit) {
    val submitted = state.status == EnquiryStatus.Submitted
    Scaffold(
        topBar = {
            if (!submitted) {
                AppTopBar(stringResource(Res.string.enquiry_title), onBack)
            }
        },
        bottomBar = {
            if (!submitted) {
                SendBar(submitting = state.status == EnquiryStatus.Submitting, onSend = { onEvent(EnquiryEvent.Submit) })
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        if (submitted) {
            ThankYou(productTitle, state.form.email.trim(), onBack, modifier)
        } else {
            EnquiryForm(productTitle, state, onEvent, onBack, modifier)
        }
    }
}

@Composable
private fun EnquiryForm(
    productTitle: String,
    state: EnquiryUiState,
    onEvent: (EnquiryEvent) -> Unit,
    onViewProduct: () -> Unit,
    modifier: Modifier,
) {
    val submitting = state.status == EnquiryStatus.Submitting
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        ProductSummary(productTitle, state.product, onViewProduct)
        Text(
            stringResource(Res.string.enquiry_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FormSection(Res.string.section_details) {
            FormField(
                field = EnquiryField.NAME,
                label = Res.string.field_name,
                icon = Res.drawable.ic_person,
                value = state.form.name,
                error = state.fieldErrors[EnquiryField.NAME],
                enabled = !submitting,
                onEvent = onEvent,
                tag = EnquiryTestTags.NAME_FIELD,
                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            FormField(
                field = EnquiryField.EMAIL,
                label = Res.string.field_email,
                icon = Res.drawable.ic_email,
                value = state.form.email,
                error = state.fieldErrors[EnquiryField.EMAIL],
                enabled = !submitting,
                onEvent = onEvent,
                tag = EnquiryTestTags.EMAIL_FIELD,
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false, imeAction = ImeAction.Next),
            )
            FormField(
                field = EnquiryField.PHONE,
                label = Res.string.field_phone,
                icon = Res.drawable.ic_phone,
                value = state.form.phone,
                error = state.fieldErrors[EnquiryField.PHONE],
                enabled = !submitting,
                onEvent = onEvent,
                tag = EnquiryTestTags.PHONE_FIELD,
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            )
        }

        FormSection(Res.string.section_message) {
            FormField(
                field = EnquiryField.MESSAGE,
                label = Res.string.field_message,
                value = state.form.message,
                error = state.fieldErrors[EnquiryField.MESSAGE],
                enabled = !submitting,
                onEvent = onEvent,
                tag = EnquiryTestTags.MESSAGE_FIELD,
                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = false,
                counter = stringResource(
                    Res.string.message_counter,
                    state.form.message.length.toString(),
                    EnquiryFormValidator.MAX_MESSAGE.toString(),
                ),
            )
        }

        val status = state.status
        if (status is EnquiryStatus.Failed) ErrorBanner(status.error)
    }
}

@Composable
private fun ProductSummary(title: String, product: EnquiryProductUi?, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().testTag(EnquiryTestTags.PRODUCT_SUMMARY),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = stringResource(Res.string.view_product), role = Role.Button, onClick = onClick)
                .padding(Spacing.small + Spacing.extraSmall),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (product != null) {
                Box(
                    modifier = Modifier
                        .size(Sizes.thumbnail)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    ProductImage(product.imageUrl, Modifier.fillMaxSize().padding(Spacing.extraSmall))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (product != null) {
                    Text(product.price, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun FormSection(title: StringResource, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        content()
    }
}

/** Takes only its own value and error, so typing in one field doesn't redraw the others. */
@Composable
private fun FormField(
    field: EnquiryField,
    label: StringResource,
    value: String,
    error: FieldError?,
    enabled: Boolean,
    onEvent: (EnquiryEvent) -> Unit,
    tag: String,
    keyboard: KeyboardOptions,
    icon: DrawableResource? = null,
    singleLine: Boolean = true,
    counter: String? = null,
) {
    val supporting = error?.label() ?: counter
    OutlinedTextField(
        value = value,
        onValueChange = { onEvent(EnquiryEvent.FieldChanged(field, it)) },
        label = { Text(stringResource(label)) },
        leadingIcon = if (icon != null) {
            { Icon(painterResource(icon), contentDescription = null) }
        } else {
            null
        },
        isError = error != null,
        supportingText = if (supporting != null) {
            { Text(supporting) }
        } else {
            null
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 4,
        keyboardOptions = keyboard,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
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
private fun ErrorBanner(error: UiError) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(EnquiryTestTags.SUBMIT_ERROR)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.padding(Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(Res.drawable.ic_error), contentDescription = null, modifier = Modifier.size(Sizes.iconMedium))
            Text(error.message(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SendBar(submitting: Boolean, onSend: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Button(
            onClick = onSend,
            enabled = !submitting,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(Spacing.medium)
                .height(Sizes.buttonHeight)
                .testTag(EnquiryTestTags.SUBMIT_BUTTON),
        ) {
            if (submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Sizes.iconMedium),
                    strokeWidth = Sizes.progressStroke,
                    color = LocalContentColor.current,
                )
            } else {
                Text(stringResource(Res.string.submit), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ThankYou(productTitle: String, email: String, onBack: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.large)
            .testTag(EnquiryTestTags.THANK_YOU),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(Sizes.successBadge).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(Res.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(Sizes.successIcon),
            )
        }
        Text(
            stringResource(Res.string.thank_you_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            stringResource(Res.string.thank_you_body, productTitle, email),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onBack,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.small)
                .height(Sizes.buttonHeight)
                .testTag(EnquiryTestTags.BACK_TO_PRODUCT),
        ) {
            Text(stringResource(Res.string.back_to_product), style = MaterialTheme.typography.titleMedium)
        }
    }
}
