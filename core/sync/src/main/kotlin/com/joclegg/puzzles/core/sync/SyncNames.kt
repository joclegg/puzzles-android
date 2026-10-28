package com.joclegg.puzzles.core.sync

import java.time.Instant
import java.time.OffsetDateTime

fun String.toJsonName(): String =
    lowercase()
        .split('_')
        .mapIndexed { index, part ->
            if (index == 0) {
                part
            } else {
                part.replaceFirstChar(Char::uppercaseChar)
            }
        }.joinToString("")

fun String.toEnumName(): String = replace(Regex("([A-Z])"), "_$1").uppercase()

inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? = enumValues<T>().find { it.name == toEnumName() }

fun Enum<*>.toJsonName(): String = name.toJsonName()

fun parseTimestamp(text: String): Long =
    OffsetDateTime
        .parse(text)
        .toInstant()
        .toEpochMilli()

fun formatTimestamp(ms: Long): String = Instant.ofEpochMilli(ms).toString()
