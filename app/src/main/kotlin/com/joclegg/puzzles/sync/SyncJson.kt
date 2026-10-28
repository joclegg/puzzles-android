package com.joclegg.puzzles.sync

import com.joclegg.puzzles.core.sync.toEnumName
import com.joclegg.puzzles.core.sync.toJsonName
import org.json.JSONObject

fun JSONObject.putNullable(
    key: String,
    value: Any?,
): JSONObject = put(key, value ?: JSONObject.NULL)

fun JSONObject.optLongOrNull(key: String): Long? =
    if (isNull(key)) {
        null
    } else {
        getLong(key)
    }

fun JSONObject.enumName(key: String): String = getString(key).toEnumName()

fun JSONObject.optEnumName(key: String): String? =
    if (isNull(key)) {
        null
    } else {
        getString(key).toEnumName()
    }

fun String.toJsonEnum(): String = toJsonName()
