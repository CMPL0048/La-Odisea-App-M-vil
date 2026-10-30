package com.laodisea.app.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ResetPasswordScreen(
    onSignedIn: () -> Unit,
    onBack: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }

    AuthLayout(
        title = "Nueva contraseña",
        subtitle = "Escribe el código que llegó a ${state.email} y elige tu nueva contraseña.",
    ) {
        InfoText(state.info)
        AuthTextField(
            value = state.code,
            onValueChange = viewModel::onCodeChange,
            label = "Código de $RESET_CODE_LENGTH dígitos",
            keyboardType = KeyboardType.Number,
            enabled = !state.loading && !state.verified,
        )
        AuthTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Nueva contraseña (mínimo $MIN_PASSWORD_LENGTH)",
            isPassword = true,
            enabled = !state.loading,
        )
        AuthTextField(
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = "Confirmar contraseña",
            isPassword = true,
            imeAction = ImeAction.Done,
            onDone = viewModel::submit,
            enabled = !state.loading,
        )
        ErrorText(state.error)
        PrimaryButton(
            text = "Cambiar contraseña",
            onClick = viewModel::submit,
            enabled = state.canSubmit,
            loading = state.loading,
        )
        TextButton(onClick = viewModel::resend, enabled = state.canResend, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.resendSeconds > 0) "Reenviar código (${state.resendSeconds} s)" else "Reenviar código")
        }
        TextButton(onClick = onBack, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
            Text("Cambiar de correo")
        }
    }
}
