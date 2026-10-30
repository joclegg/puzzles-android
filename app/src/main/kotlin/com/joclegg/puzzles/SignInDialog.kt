package com.joclegg.puzzles

import android.util.Patterns
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joclegg.puzzles.core.sync.SignInProvider
import com.joclegg.puzzles.core.sync.SupabaseAuth
import com.joclegg.puzzles.sudoku.core.ui.dialogTestTags
import kotlinx.coroutines.launch

@Composable
fun SignInDialog(
    darkTheme: Boolean,
    onProviderClick: (SignInProvider) -> Unit,
    onSendEmailLink: suspend (String) -> Boolean,
    onVerifyEmailCode: suspend (String, String) -> Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var askingForCode by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    fun sendLink() {
        scope.launch {
            busy = true
            failed = false
            val sent = onSendEmailLink(email)
            busy = false
            if (sent) {
                askingForCode = true
            } else {
                failed = true
            }
        }
    }

    fun verifyCode() {
        scope.launch {
            busy = true
            failed = false
            val verified = onVerifyEmailCode(email, code)
            busy = false
            if (verified) {
                onDismiss()
            } else {
                failed = true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.dialogTestTags(),
        title = { Text(if (askingForCode) "Check your email" else "Sign in to sync") },
        text = {
            if (askingForCode) {
                CodeStep(email = email, code = code, failed = failed, onCodeChange = { code = cleanCode(it) })
            } else {
                ChooseStep(
                    darkTheme = darkTheme,
                    email = email,
                    failed = failed,
                    onProviderClick = onProviderClick,
                    onEmailChange = { email = it },
                )
            }
        },
        confirmButton = {
            if (askingForCode) {
                TextButton(
                    onClick = ::verifyCode,
                    enabled = !busy && code.length == SupabaseAuth.EMAIL_CODE_LENGTH,
                    modifier = Modifier.testTag("sign_in_code_submit"),
                ) {
                    Text("Sign in")
                }
            } else {
                TextButton(
                    onClick = ::sendLink,
                    enabled = !busy && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches(),
                    modifier = Modifier.testTag("sign_in_email_send"),
                ) {
                    Text("Email me a link")
                }
            }
        },
        dismissButton = {
            if (askingForCode) {
                TextButton(
                    onClick = {
                        askingForCode = false
                        code = ""
                        failed = false
                    },
                    modifier = Modifier.testTag("sign_in_code_back"),
                ) {
                    Text("Use a different email")
                }
            } else {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("sign_in_cancel")) {
                    Text("Cancel")
                }
            }
        },
    )
}

@Composable
private fun ChooseStep(
    darkTheme: Boolean,
    email: String,
    failed: Boolean,
    onProviderClick: (SignInProvider) -> Unit,
    onEmailChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Sync your games and settings with the website and other devices.")
        ProviderButton(
            style = if (darkTheme) GOOGLE_DARK else GOOGLE_LIGHT,
            onClick = { onProviderClick(SignInProvider.GOOGLE) },
            modifier = Modifier.testTag("sign_in_google"),
        )
        ProviderButton(
            style = if (darkTheme) MICROSOFT_DARK else MICROSOFT_LIGHT,
            onClick = { onProviderClick(SignInProvider.MICROSOFT) },
            modifier = Modifier.testTag("sign_in_microsoft"),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider(Modifier.weight(1f))
            Text("or", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(Modifier.weight(1f))
        }
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().testTag("sign_in_email"),
        )
        if (failed) {
            Text("We couldn't send the email. Try again in a minute.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun CodeStep(
    email: String,
    code: String,
    failed: Boolean,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("We sent a sign-in link and a code to ${email.trim()}. Open the link on this phone, or enter the code.")
        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            label = { Text("Code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth().testTag("sign_in_code"),
        )
        if (failed) {
            Text("That code didn't work. Check it, or send a new one.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ProviderButton(
    style: ProviderStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = style.shape,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = style.background, contentColor = style.text),
        border = BorderStroke(1.dp, style.border),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = modifier.fillMaxWidth().height(style.height),
    ) {
        Image(painterResource(style.logo), contentDescription = null)
        Spacer(Modifier.width(style.gap))
        Text(style.label, fontFamily = FontFamily.SansSerif, fontWeight = style.weight, fontSize = style.size)
    }
}

private fun cleanCode(input: String): String = input.filter(Char::isDigit).take(SupabaseAuth.EMAIL_CODE_LENGTH)

private data class ProviderStyle(
    val label: String,
    @param:DrawableRes val logo: Int,
    val background: Color,
    val border: Color,
    val text: Color,
    val shape: Shape,
    val height: Dp,
    val gap: Dp,
    val size: TextUnit,
    val weight: FontWeight,
)

private val GOOGLE_LIGHT =
    ProviderStyle(
        label = "Sign in with Google",
        logo = R.drawable.ic_google_logo,
        background = Color(0xFFFFFFFF),
        border = Color(0xFF747775),
        text = Color(0xFF1F1F1F),
        shape = RoundedCornerShape(4.dp),
        height = 40.dp,
        gap = 10.dp,
        size = 14.sp,
        weight = FontWeight.Medium,
    )

private val GOOGLE_DARK = GOOGLE_LIGHT.copy(background = Color(0xFF131314), border = Color(0xFF8E918F), text = Color(0xFFE3E3E3))

private val MICROSOFT_LIGHT =
    ProviderStyle(
        label = "Sign in with Microsoft",
        logo = R.drawable.ic_microsoft_logo,
        background = Color(0xFFFFFFFF),
        border = Color(0xFF8C8C8C),
        text = Color(0xFF5E5E5E),
        shape = RectangleShape,
        height = 41.dp,
        gap = 12.dp,
        size = 15.sp,
        weight = FontWeight.SemiBold,
    )

private val MICROSOFT_DARK = MICROSOFT_LIGHT.copy(background = Color(0xFF2F2F2F), border = Color(0xFF2F2F2F), text = Color(0xFFFFFFFF))
