package com.joclegg.puzzles.core.sync

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SignInUrlTest(
    private val provider: SignInProvider,
    private val query: String,
) {
    @Test
    fun `given a provider when its sign-in URL is made then it names the provider and its scopes`() {
        // Act
        val url = SupabaseAuth.authorizeUrl(provider, "challenge")

        // Assert
        assertEquals(
            "${SupabaseConfig.URL}/auth/v1/authorize?$query&redirect_to=com.joclegg.puzzles%3A%2F%2Fauth" +
                "&code_challenge=challenge&code_challenge_method=s256",
            url,
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun providers() =
            listOf(
                arrayOf<Any>(SignInProvider.GOOGLE, "provider=google"),
                arrayOf<Any>(SignInProvider.MICROSOFT, "provider=azure&scopes=email"),
            )
    }
}
