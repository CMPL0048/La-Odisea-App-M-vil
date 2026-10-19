package com.laodisea.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laodisea.app.data.repository.SettingsRepository
import com.laodisea.app.data.repository.ThemePreference
import com.laodisea.app.ui.navigation.AppNavHost
import com.laodisea.app.ui.theme.LaOdiseaTheme
import com.laodisea.app.ui.theme.Papel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preference by settings.theme.collectAsStateWithLifecycle(initialValue = ThemePreference.Sistema)
            val dark = when (preference) {
                ThemePreference.Sistema -> isSystemInDarkTheme()
                ThemePreference.Claro -> false
                ThemePreference.Oscuro -> true
            }
            // La barra superior siempre es verde: íconos de estado claros; la de navegación sigue al tema.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                    navigationBarStyle = if (dark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Papel.toArgb(), Papel.toArgb())
                    },
                )
                onDispose {}
            }
            LaOdiseaTheme(darkTheme = dark) {
                AppNavHost()
            }
        }
    }
}
