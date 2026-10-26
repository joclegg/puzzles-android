package com.joclegg.puzzles.fiveletters.core.data

import com.joclegg.puzzles.fiveletters.engine.WORD_LENGTH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

fun interface DailyWordSource {
    suspend fun word(date: LocalDate): String?
}

class NytWordSource(
    private val fetch: (String) -> String? = ::download,
) : DailyWordSource {
    override suspend fun word(date: LocalDate): String? =
        withContext(Dispatchers.IO) {
            runCatching { fetch("$BASE_URL$date.json") }
                .getOrNull()
                ?.let(::solutionOf)
        }

    companion object {
        private const val BASE_URL = "https://www.nytimes.com/svc/wordle/v2/"
        private const val TIMEOUT_MS = 5_000
        private val SOLUTION = Regex("\"solution\"\\s*:\\s*\"([A-Za-z]+)\"")

        fun solutionOf(json: String): String? =
            SOLUTION
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
