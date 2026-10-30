package com.joclegg.puzzles.core.sync

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EmailSignInTest {
    @Test
    fun `given the app when the email link URL is made then it redirects back to the app`() {
        // Act
        val url = SupabaseAuth.emailLinkUrl()

        // Assert
        assertEquals("${SupabaseConfig.URL}/auth/v1/otp?redirect_to=com.joclegg.puzzles%3A%2F%2Fauth", url)
    }

    @Test
    fun `given an email with spaces when the email link is asked for then it sends the trimmed email and the PKCE challenge`() {
        // Act
        val body = JSONObject(SupabaseAuth.emailLinkBody(" player@example.com ", "challenge"))

        // Assert
        assertEquals("player@example.com", body.getString("email"))
        assertEquals(true, body.getBoolean("create_user"))
        assertEquals("challenge", body.getString("code_challenge"))
        assertEquals("s256", body.getString("code_challenge_method"))
    }

    @Test
    fun `given an emailed code when it is checked then it asks Supabase to verify an email code`() {
        // Act
        val body = JSONObject(SupabaseAuth.emailCodeBody(" player@example.com ", "12345678"))

        // Assert
        assertEquals("email", body.getString("type"))
        assertEquals("player@example.com", body.getString("email"))
        assertEquals("12345678", body.getString("token"))
    }
}
