package com.bosankus.officeworks.feature.enquiry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.bosankus.officeworks.core.domain.enquiry.EnquiryField
import com.bosankus.officeworks.core.domain.enquiry.EnquiryForm
import com.bosankus.officeworks.core.domain.enquiry.FieldError
import com.bosankus.officeworks.core.ui.components.UiError
import com.bosankus.officeworks.core.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class EnquiryContentTest {

    private val title = "Mens Cotton Jacket"

    @Test
    fun typing_and_submitting_send_events() = runComposeUiTest {
        val events = mutableListOf<EnquiryEvent>()
        setContent { AppTheme { EnquiryContent(title, EnquiryUiState(), { events += it }, {}) } }

        onNodeWithTag(EnquiryTestTags.NAME_FIELD).performTextInput("Jane")
        onNodeWithTag(EnquiryTestTags.SUBMIT_BUTTON).performClick()

        assertEquals(EnquiryEvent.FieldChanged(EnquiryField.NAME, "Jane"), events.first())
        assertEquals(EnquiryEvent.Submit, events.last())
    }

    @Test
    fun field_errors_are_shown_inline() = runComposeUiTest {
        val state = EnquiryUiState(
            fieldErrors = mapOf(EnquiryField.NAME to FieldError.REQUIRED, EnquiryField.EMAIL to FieldError.INVALID_FORMAT),
        )
        setContent { AppTheme { EnquiryContent(title, state, {}, {}) } }

        onNodeWithText("Required").assertIsDisplayed()
        onNodeWithText("Please check the format").assertIsDisplayed()
    }

    @Test
    fun submit_is_disabled_while_sending() = runComposeUiTest {
        setContent { AppTheme { EnquiryContent(title, EnquiryUiState(status = EnquiryStatus.Submitting), {}, {}) } }

        onNodeWithTag(EnquiryTestTags.SUBMIT_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun failure_is_explained() = runComposeUiTest {
        setContent { AppTheme { EnquiryContent(title, EnquiryUiState(status = EnquiryStatus.Failed(UiError.Network)), {}, {}) } }

        onNodeWithTag(EnquiryTestTags.SUBMIT_ERROR).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sent_enquiry_shows_thank_you_and_goes_back() = runComposeUiTest {
        var back = false
        setContent { AppTheme { EnquiryContent(title, EnquiryUiState(status = EnquiryStatus.Submitted), {}, { back = true }) } }

        onNodeWithTag(EnquiryTestTags.THANK_YOU).assertIsDisplayed()
        onNodeWithText("Thank you").assertIsDisplayed()
        onNodeWithTag(EnquiryTestTags.BACK_TO_PRODUCT).performClick()

        assertTrue(back)
    }

    @Test
    fun thank_you_renders_in_dark_theme() = runComposeUiTest {
        setContent {
            AppTheme(darkTheme = true) { EnquiryContent(title, EnquiryUiState(status = EnquiryStatus.Submitted), {}, {}) }
        }

        onNodeWithText("Thank you").assertIsDisplayed()
    }

    @Test
    fun summary_shows_the_product_title_then_its_price_once_loaded() = runComposeUiTest {
        var state by mutableStateOf(EnquiryUiState())
        setContent { AppTheme { EnquiryContent(title, state, {}, {}) } }

        onNodeWithTag(EnquiryTestTags.PRODUCT_SUMMARY).assertIsDisplayed()
        onNodeWithText(title).assertIsDisplayed()

        state = EnquiryUiState(product = EnquiryProductUi("https://example.com/7.png", "A$55.99"))
        onNodeWithText("A$55.99").assertIsDisplayed()
    }

    @Test
    fun send_button_stays_visible_without_scrolling() = runComposeUiTest {
        setContent { AppTheme { EnquiryContent(title, EnquiryUiState(), {}, {}) } }

        onNodeWithTag(EnquiryTestTags.SUBMIT_BUTTON).assertIsDisplayed()
    }

    @Test
    fun thank_you_says_where_the_reply_will_go() = runComposeUiTest {
        val state = EnquiryUiState(form = EnquiryForm(name = "Jane", email = " jane@example.com "), status = EnquiryStatus.Submitted)
        setContent { AppTheme { EnquiryContent(title, state, {}, {}) } }

        onNodeWithText("reply to jane@example.com", substring = true).assertIsDisplayed()
    }
}
