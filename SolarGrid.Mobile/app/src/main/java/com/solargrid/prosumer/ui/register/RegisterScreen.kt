/*
 * File: RegisterScreen.kt
 * Description: Designed Prosumer sign-up UI
 */
package com.solargrid.prosumer.ui.register

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
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
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
import androidx.compose.ui.graphics.vector.ImageVector
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
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegistered: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val canSubmit = state.fullName.isNotBlank() &&
        state.nic.isNotBlank() &&
        state.phoneNumber.isNotBlank() &&
        state.email.isNotBlank() &&
        state.password.length >= 6 &&
        !state.loading

    LaunchedEffect(state.registered) {
        if (state.registered) onRegistered()
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
            painter = painterResource(R.drawable.register_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(WavyBottomShape()),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(top = 4.dp, bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.register_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111),
            )

            Spacer(Modifier.height(18.dp))

            AuthLabeledField(
                label = stringResource(R.string.full_name_label),
                value = state.fullName,
                onValueChange = viewModel::onFullNameChange,
                placeholder = stringResource(R.string.full_name_hint),
                leadingIcon = Icons.Outlined.Person,
                enabled = !state.loading,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(12.dp))

            AuthLabeledField(
                label = stringResource(R.string.nic_label),
                value = state.nic,
                onValueChange = viewModel::onNicChange,
                placeholder = stringResource(R.string.nic_hint),
                leadingIcon = Icons.Outlined.Badge,
                enabled = !state.loading,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(12.dp))

            AuthLabeledField(
                label = stringResource(R.string.phone_label),
                value = state.phoneNumber,
                onValueChange = viewModel::onPhoneChange,
                placeholder = stringResource(R.string.phone_hint),
                leadingIcon = Icons.Outlined.Phone,
                enabled = !state.loading,
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(12.dp))

            AuthLabeledField(
                label = stringResource(R.string.email_label),
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = stringResource(R.string.email_hint),
                leadingIcon = Icons.Outlined.Email,
                enabled = !state.loading,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(12.dp))

            AuthLabeledField(
                label = stringResource(R.string.password_label),
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = stringResource(R.string.password_hint),
                leadingIcon = Icons.Outlined.Lock,
                enabled = !state.loading,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                isPassword = true,
                passwordVisible = state.passwordVisible,
                onTogglePassword = viewModel::togglePasswordVisible,
                onDone = {
                    focusManager.clearFocus()
                    if (canSubmit) viewModel.register()
                },
            )

            if (!state.error.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = state.error.orEmpty(),
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp,
                )
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.register()
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
                        text = stringResource(R.string.create_account),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            AuthOrDivider()
            Spacer(Modifier.height(16.dp))
            AuthSocialRow(onComingSoon = ::comingSoon)

            Spacer(Modifier.height(24.dp))
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.have_account_prefix))
                    append(" ")
                    withStyle(
                        SpanStyle(
                            color = AccentGold,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) {
                        append(stringResource(R.string.login_now))
                    }
                },
                fontSize = 14.sp,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !state.loading, onClick = onBackToLogin)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun AuthLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    enabled: Boolean,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
) {
    Text(
        text = label,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = Color(0xFF111111),
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = placeholder, color = FieldHint) },
        leadingIcon = {
            Icon(imageVector = leadingIcon, contentDescription = null, tint = FieldHint)
        },
        trailingIcon = if (isPassword && onTogglePassword != null) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = null,
                        tint = FieldHint,
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        enabled = enabled,
        shape = AuthPillShape,
        colors = authFieldColors(),
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDone?.invoke() },
        ),
    )
}
