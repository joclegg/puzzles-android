package com.joclegg.puzzles.core.sync

enum class SignInProvider(
    val id: String,
    val scopes: String?,
) {
    GOOGLE("google", null),
    MICROSOFT("azure", "email"),
}
