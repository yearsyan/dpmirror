package io.github.tsioam.mirror.util

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(object : okhttp3.Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }

        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }
    })

    continuation.invokeOnCancellation {
        cancel()
    }
}