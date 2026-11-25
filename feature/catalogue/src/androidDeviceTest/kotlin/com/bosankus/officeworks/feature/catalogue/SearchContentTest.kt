package com.bosankus.officeworks.feature.catalogue

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.bosankus.officeworks.core.ui.components.StatusTestTags
import com.bosankus.officeworks.core.ui.theme.AppTheme
import com.bosankus.officeworks.feature.catalogue.search.SearchContent
import com.bosankus.officeworks.feature.catalogue.search.SearchEvent
import com.bosankus.officeworks.feature.catalogue.search.SearchResults
import com.bosankus.officeworks.feature.catalogue.search.SearchUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SearchContentTest {

    @Test
    fun search_shows_only_the_matching_products() = runComposeUiTest {
        val state = SearchUiState("bag", SearchResults.Found(listOf(productUi(1))))
        setContent { AppTheme { SearchContent(state, {}, {}, {}) } }

        onNodeWithText("Product 1").assertIsDisplayed()
        onNodeWithText("Product 2").assertDoesNotExist()
    }

    @Test
    fun search_with_no_matches_says_so() = runComposeUiTest {
        setContent { AppTheme { SearchContent(SearchUiState("zzz", SearchResults.NoMatches("zzz")), {}, {}, {}) } }

        onNodeWithTag(StatusTestTags.EMPTY).assertIsDisplayed()
        onNode(hasText("No products match", substring = true)).assertIsDisplayed()
    }

    @Test
    fun typing_and_searching_send_events() = runComposeUiTest {
        val events = mutableListOf<SearchEvent>()
        setContent { AppTheme { SearchContent(SearchUiState("bag"), { events += it }, {}, {}) } }

        onNodeWithTag(CatalogueTestTags.SEARCH_FIELD).performTextInput("s")
        onNodeWithTag(CatalogueTestTags.SEARCH_BUTTON).performClick()

        assertTrue(events.first() is SearchEvent.QueryChanged)
        assertEquals(SearchEvent.Submit, events.last())
    }

    @Test
    fun text_shows_only_the_clear_button_which_empties_the_query() = runComposeUiTest {
        val events = mutableListOf<SearchEvent>()
        setContent { AppTheme { SearchContent(SearchUiState("bag"), { events += it }, {}, {}) } }

        onNodeWithTag(CatalogueTestTags.SEARCH_ICON).assertDoesNotExist()
        onNodeWithTag(CatalogueTestTags.SEARCH_CLEAR).assertIsDisplayed().performClick()

        assertEquals(listOf<SearchEvent>(SearchEvent.QueryChanged("")), events)
    }

    @Test
    fun empty_field_shows_the_search_icon_and_no_clear_button() = runComposeUiTest {
        setContent { AppTheme { SearchContent(SearchUiState(""), {}, {}, {}) } }

        onNodeWithTag(CatalogueTestTags.SEARCH_ICON).assertExists()
        onNodeWithTag(CatalogueTestTags.SEARCH_CLEAR).assertDoesNotExist()
    }
}
