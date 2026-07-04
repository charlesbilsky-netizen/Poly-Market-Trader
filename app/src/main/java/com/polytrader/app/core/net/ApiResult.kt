package com.polytrader.app.core.net

/** Minimal result wrapper for network calls surfaced to the UI. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : ApiResult<Nothing>
}

inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) block(data)
    return this
}

inline fun <T> ApiResult<T>.onError(block: (String) -> Unit): ApiResult<T> {
    if (this is ApiResult.Error) block(message)
    return this
}

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data

suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Network request failed", e)
    }
