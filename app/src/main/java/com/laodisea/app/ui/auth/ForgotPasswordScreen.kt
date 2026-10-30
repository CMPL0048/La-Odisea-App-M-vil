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
fun ForgotPasswordScreen(
    onCodeSent: (email: String) -> Unit,
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.codeSent) {
        if (state.codeSent) {
            viewModel.onCodeSentHandled()
            onCodeSent(state.email.trim())
        }
    }

    AuthLayout(
        title = "Recuperar acceso",
        subtitle = "Escribe el correo de tu cuenta y te mandaremos un código de $RESET_CODE_LENGTH dígitos.",
    ) {
        AuthTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = "Correo",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            onDone = viewModel::submit,
            enabled = !state.loading,
        )
        ErrorText(state.error)
        PrimaryButton(
            text = "Enviar código",
            onClick = viewModel::submit,
            enabled = state.canSubmit,
            loading = state.loading,
        )
        TextButton(onClick = onBack, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
            Text("Volver a iniciar sesión")
        }
    }
}
