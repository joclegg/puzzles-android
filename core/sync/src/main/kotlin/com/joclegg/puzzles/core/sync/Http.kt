package com.joclegg.puzzles.core.sync

import java.net.HttpURLConnection
import java.net.URL

data class HttpResponse(
    val code: Int,
    val body: String,
) {
    val ok: Boolean get() = code in 200..299
}

fun interface Http {
    fun send(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): HttpResponse
}

object UrlConnectionHttp : Http {
    private const val TIMEOUT_MS = 10_000

    override fun send(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
        return try {
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.bufferedWriter().use { it.write(body) }
            }
            val code = connection.responseCode
            val stream =
                if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
            HttpResponse(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }
}
