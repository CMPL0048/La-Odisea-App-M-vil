package com.laodisea.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    onRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }

    AuthLayout(
        title = "Inicia sesión",
        subtitle = "Usa la misma cuenta que en el juego de PC.",
    ) {
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
            label = "Contraseña",
            isPassword = true,
            imeAction = ImeAction.Done,
            onDone = viewModel::submit,
            enabled = !state.loading,
        )
        ErrorText(state.error)
        PrimaryButton(
            text = "Entrar",
            onClick = viewModel::submit,
            enabled = state.canSubmit,
            loading = state.loading,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("¿Aún no tienes cuenta?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRegister, enabled = !state.loading) { Text("Regístrate") }
        }
    }
}
