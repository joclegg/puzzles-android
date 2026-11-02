package com.joclegg.puzzles.fiveletters.core.data

import com.joclegg.puzzles.fiveletters.engine.WORD_LENGTH
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

fun interface DailyWordSource {
    suspend fun word(date: LocalDate): String?
}

class Daily2WordSource(
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val fetch: (String) -> String? = ::download,
) : DailyWordSource {
    override suspend fun word(date: LocalDate): String? =
        withContext(io) {
            runCatching { fetch("$BASE_URL$date") }
                .getOrNull()
                ?.let(::wordOf)
        }

    companion object {
        private const val BASE_URL = "https://puzzles-daily.joclegg.dev/word/"
        private const val TIMEOUT_MS = 5_000
        private val WORD = Regex("\"word\"\\s*:\\s*\"([A-Za-z]+)\"")

        fun wordOf(json: String): String? =
            WORD
                .find(json)
                ?.groupValues
                ?.get(1)
                ?.uppercase()
                ?.takeIf { it.length == WORD_LENGTH }

        private fun download(url: String): String? {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            return try {
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    null
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}
