package com.bosankus.officeworks.core.ui.components

import androidx.compose.runtime.Composable
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.ui.resources.Res
import com.bosankus.officeworks.core.ui.resources.error_generic
import com.bosankus.officeworks.core.ui.resources.error_network
import com.bosankus.officeworks.core.ui.resources.error_not_found
import com.bosankus.officeworks.core.ui.resources.error_rate_limited
import com.bosankus.officeworks.core.ui.resources.error_server
import org.jetbrains.compose.resources.stringResource

/** The few error messages a customer needs. */
enum class UiError { Network, Server, RateLimited, NotFound, Generic }

/** One mapping for every feature, so the same failure always reads the same way. */
fun DataError.toUiError(): UiError = when (this) {
    DataError.Network -> UiError.Network
    DataError.Server -> UiError.Server
    DataError.RateLimited -> UiError.RateLimited
    DataError.NotFound -> UiError.NotFound
    // Validation is shown inline on the form; reaching a generic message means nothing more specific applies.
    is DataError.Validation, DataError.Unknown -> UiError.Generic
}

@Composable
fun UiError.message(): String = stringResource(
    when (this) {
        UiError.Network -> Res.string.error_network
        UiError.Server -> Res.string.error_server
        UiError.RateLimited -> Res.string.error_rate_limited
        UiError.NotFound -> Res.string.error_not_found
        UiError.Generic -> Res.string.error_generic
    },
)
