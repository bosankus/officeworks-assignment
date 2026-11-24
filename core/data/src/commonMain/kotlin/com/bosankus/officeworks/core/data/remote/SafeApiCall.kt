package com.bosankus.officeworks.core.data.remote

import co.touchlab.kermit.Logger
import com.bosankus.officeworks.core.data.remote.dto.ErrorDto
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.domain.result.DataResult
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Turns a Ktor call into a DataResult. This is the only place transport errors become domain errors.
 * A 400 is [DataError.Unknown] unless the endpoint knows how to read its field errors ([onBadRequest]).
 */
internal suspend inline fun <reified T> safeApiCall(
    logger: Logger,
    endpoint: String,
    noinline onBadRequest: (ErrorDto?) -> DataError = { DataError.Unknown },
    call: () -> HttpResponse,
): DataResult<T> {
    val response = try {
        call()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        logger.w(e) { "$endpoint: network error" }
        return DataResult.Failure(DataError.Network)
    } catch (e: Exception) {
        logger.e(e) { "$endpoint: unexpected error" }
        return DataResult.Failure(DataError.Unknown)
    }

    if (!response.status.isSuccess()) {
        val error = response.toDataError(onBadRequest)
        if (response.status == HttpStatusCode.Conflict) {
            // Same idempotency key, different body: only a client bug can cause this.
            logger.e { "$endpoint: idempotency key reused with a different body" }
        } else {
            logger.w { "$endpoint: HTTP ${response.status.value} -> $error" }
        }
        return DataResult.Failure(error)
    }

    return try {
        DataResult.Success(response.body<T>())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.e(e) { "$endpoint: response didn't match the contract" }
        DataResult.Failure(DataError.Unknown)
    }
}

internal suspend fun HttpResponse.toDataError(onBadRequest: (ErrorDto?) -> DataError): DataError = when (status) {
    HttpStatusCode.NotFound -> DataError.NotFound
    HttpStatusCode.TooManyRequests -> DataError.RateLimited
    HttpStatusCode.BadRequest -> onBadRequest(errorBody())
    // 409 means the app reused an idempotency key for a different enquiry; nothing the customer can fix.
    HttpStatusCode.Conflict -> DataError.Unknown
    // 500 INTERNAL_ERROR and 502 UPSTREAM_ERROR (catalogue or mail server down) are both retryable.
    else -> if (status.value >= 500) DataError.Server else DataError.Unknown
}

private suspend fun HttpResponse.errorBody(): ErrorDto? = try {
    body<ErrorDto>()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}
