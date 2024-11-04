package io.github.tsioam.mirror.util

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object Rpc {
    private val client = OkHttpClient()
    suspend fun call(address: String, port: Int, method: String, body: String): String? {
        val requestBody = (body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("http://${address}:${port}/rpc/${method}")
            .post(requestBody)
            .build()

        val response = client.newCall(request).await()
        return response.body?.string()
    }
}