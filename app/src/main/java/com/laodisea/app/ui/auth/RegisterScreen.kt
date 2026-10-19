package com.laodisea.app.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
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
fun RegisterScreen(
    onSignedIn: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }

    if (state.confirmEmailSent) {
        AlertDialog(
            onDismissRequest = onBack,
            confirmButton = { TextButton(onClick = onBack) { Text("Ir a iniciar sesión") } },
            title = { Text("Revisa tu correo") },
            text = { Text("Te enviamos un enlace a ${state.email.trim()} para confirmar tu cuenta. Después inicia sesión.") },
        )
    }

    AuthLayout(
        title = "Crea tu cuenta",
        subtitle = "Tu avance se comparte entre la app y el juego de PC.",
    ) {
        AuthTextField(
            value = state.displayName,
            onValueChange = viewModel::onDisplayNameChange,
            label = "Nombre de jugador",
            enabled = !state.loading,
        )
        AuthTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = "Correo",
            keyboardType = KeyboardType.Email,
            enabled = !state.loading,
        )
        AuthTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Contraseña (mínimo 6)",
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
            text = "Crear cuenta",
            onClick = viewModel::submit,
            enabled = state.canSubmit,
            loading = state.loading,
        )
        TextButton(onClick = onBack, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
            Text("Ya tengo cuenta")
        }
    }
}
