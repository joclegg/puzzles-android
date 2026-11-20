package com.joclegg.puzzles.core.ui

import android.content.Context
import android.content.Intent

fun Context.shareText(text: String) {
    val send =
        Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, null))
}
