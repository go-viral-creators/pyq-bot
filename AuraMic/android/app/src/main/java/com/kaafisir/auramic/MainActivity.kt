package com.kaafisir.auramic

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaafisir.auramic.core.LocaleManager
import com.kaafisir.auramic.domain.model.ThemeMode
import com.kaafisir.auramic.ui.AppViewModel
import com.kaafisir.auramic.ui.navigation.AppNavHost
import com.kaafisir.auramic.ui.theme.AuraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(LocaleManager.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val s = settings
            val dark = when (s?.theme ?: ThemeMode.SYSTEM) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            // Bar icon colours follow the in-app theme, which may differ from the system one.
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            AuraTheme(darkTheme = dark) {
                // Wait for the first DataStore read so we pick the right start screen.
                if (s != null) AppNavHost(settings = s, vm = vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshPermissions()
    }
}
