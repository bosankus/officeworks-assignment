package com.bosankus.officeworks.core.domain.result

/** Typed alternative to kotlin.Result: callers handle known errors, and cancellation is never swallowed. */
sealed interface DataResult<out T> {
    data class Success<out T>(val data: T) : DataResult<T>
    data class Failure(val error: DataError) : DataResult<Nothing>
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(data))
    is DataResult.Failure -> this
}

inline fun <T> DataResult<T>.onSuccess(action: (T) -> Unit): DataResult<T> {
    if (this is DataResult.Success) action(data)
    return this
}

inline fun <T> DataResult<T>.onFailure(action: (DataError) -> Unit): DataResult<T> {
    if (this is DataResult.Failure) action(error)
    return this
}
