package com.laodisea.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemePreference(val label: String) {
    Sistema("Igual que el teléfono"),
    Claro("Claro"),
    Oscuro("Oscuro"),
}

/** Preferencias del dispositivo; no dependen de la cuenta y no se borran al cerrar sesión. */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val theme: Flow<ThemePreference> = dataStore.data.map { prefs ->
        prefs[THEME]?.let { saved -> ThemePreference.entries.firstOrNull { it.name == saved } } ?: ThemePreference.Sistema
    }

    suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[THEME] = theme.name }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
    }
}
