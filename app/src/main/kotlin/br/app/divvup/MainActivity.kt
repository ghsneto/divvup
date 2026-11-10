package br.app.divvup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.navigation.DivvupNavHost
import dagger.hilt.android.AndroidEntryPoint

/** Única Activity do app; toda navegação é Navigation Compose. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DivvupTheme {
                DivvupNavHost()
            }
        }
    }
}
