package com.joclegg.puzzles.core.sync

import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

data class HttpResponse(
    val code: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap(),
) {
    val ok: Boolean get() = code in 200..299

    fun header(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
}

fun interface Http {
    fun send(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): HttpResponse

    fun download(
        url: String,
        headers: Map<String, String>,
        target: OutputStream,
    ): HttpResponse {
        val response = send("GET", url, headers, null)
        if (response.ok) {
            target.write(response.body.toByteArray())
        }
        return response.copy(body = "")
    }
}

object UrlConnectionHttp : Http {
    private const val TIMEOUT_MS = 10_000

    override fun send(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): HttpResponse {
        val connection = open(method, url, headers)
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
            HttpResponse(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty(), connection.responseHeaders())
        } finally {
            connection.disconnect()
        }
    }

    override fun download(
        url: String,
        headers: Map<String, String>,
        target: OutputStream,
    ): HttpResponse {
        val connection = open("GET", url, headers)
        return try {
            val code = connection.responseCode
            if (code in 200..299) {
                connection.inputStream.use { it.copyTo(target) }
            } else {
                connection.errorStream?.close()
            }
            HttpResponse(code, "", connection.responseHeaders())
        } finally {
            connection.disconnect()
        }
    }

    private fun open(
        method: String,
        url: String,
        headers: Map<String, String>,
    ): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
        return connection
    }

    private fun HttpURLConnection.responseHeaders(): Map<String, String> =
        headerFields.entries
            .mapNotNull { (name, values) -> name?.let { it to values.firstOrNull().orEmpty() } }
            .toMap()
}
