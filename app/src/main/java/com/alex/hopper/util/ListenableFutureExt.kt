package com.alex.hopper.util

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

private val directExecutor = Executor { command -> command.run() }

suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addListener(
        {
            try {
                continuation.resume(get())
            } catch (exception: ExecutionException) {
                continuation.resumeWithException(exception.cause ?: exception)
            } catch (exception: Exception) {
                continuation.resumeWithException(exception)
            }
        },
        directExecutor,
    )

    continuation.invokeOnCancellation {
        cancel(true)
    }
}
