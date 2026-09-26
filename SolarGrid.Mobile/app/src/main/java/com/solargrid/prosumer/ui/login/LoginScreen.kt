/*
 * File: LoginScreen.kt
 * Description: Designed Prosumer sign-in UI
 */
package com.solargrid.prosumer.ui.login

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solargrid.prosumer.R
import com.solargrid.prosumer.ui.auth.AuthOrDivider
import com.solargrid.prosumer.ui.auth.AuthPillShape
import com.solargrid.prosumer.ui.auth.AuthSocialRow
import com.solargrid.prosumer.ui.auth.WavyBottomShape
import com.solargrid.prosumer.ui.auth.authFieldColors
import com.solargrid.prosumer.ui.theme.AccentGold
import com.solargrid.prosumer.ui.theme.FieldHint
import com.solargrid.prosumer.ui.theme.SignInButtonDisabled
import com.solargrid.prosumer.ui.theme.SignInButtonDisabledText
import com.solargrid.prosumer.ui.theme.SignInButtonEnabled
import com.solargrid.prosumer.ui.theme.SignInButtonText

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoggedIn: () -> Unit,
    onCreateAccount: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val canSubmit = state.identifier.isNotBlank() &&
        state.password.isNotBlank() &&
        !state.loading

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLoggedIn()
    }

    fun comingSoon() {
        Toast.makeText(context, context.getString(R.string.coming_soon), Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
        Image(
            painter = painterResource(R.drawable.login_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(WavyBottomShape()),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(top = 8.dp, bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.login_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111),
            )

            Spacer(Modifier.height(22.dp))

            Text(
                text = stringResource(R.string.identifier_label),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFF111111),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.identifier,
                onValueChange = viewModel::onIdentifierChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.identifier_hint),
                        color = FieldHint,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = FieldHint,
                    )
                },
                singleLine = true,
                enabled = !state.loading,
                shape = AuthPillShape,
                colors = authFieldColors(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.password_label),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFF111111),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.password_hint),
                        color = FieldHint,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = FieldHint,
                    )
                },
                trailingIcon = {
                    IconButton(onClick = viewModel::togglePasswordVisible) {
                        Icon(
                            imageVector = if (state.passwordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = null,
                            tint = FieldHint,
                        )
                    }
                },
                singleLine = true,
                enabled = !state.loading,
                shape = AuthPillShape,
                colors = authFieldColors(),
                visualTransformation = if (state.passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (canSubmit) viewModel.login()
                    },
                ),
            )

            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.forgot_password),
                color = AccentGold,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(enabled = !state.loading) { comingSoon() }
                    .padding(vertical = 4.dp),
            )

            if (!state.error.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.error.orEmpty(),
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp,
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.login()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = canSubmit,
                shape = AuthPillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SignInButtonEnabled,
                    contentColor = SignInButtonText,
                    disabledContainerColor = SignInButtonDisabled,
                    disabledContentColor = SignInButtonDisabledText,
                ),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = SignInButtonText,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.sign_in),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            AuthOrDivider()
            Spacer(Modifier.height(18.dp))
            AuthSocialRow(onComingSoon = ::comingSoon)

            Spacer(Modifier.height(28.dp))
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.no_account_prefix))
                    append(" ")
                    withStyle(
                        SpanStyle(
                            color = AccentGold,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) {
                        append(stringResource(R.string.register_now))
                    }
                },
                fontSize = 14.sp,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !state.loading, onClick = onCreateAccount)
                    .padding(8.dp),
            )
        }
    }
}
