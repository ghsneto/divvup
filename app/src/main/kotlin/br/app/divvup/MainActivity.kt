package br.app.divvup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.navigation.DivvupNavHost
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.divvup.core.domain.conta.PreferenciasDoAparelho
import br.app.divvup.core.model.Tema
import javax.inject.Inject

/** Única Activity do app; toda navegação é Navigation Compose. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var aparelho: PreferenciasDoAparelho

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // E5: sistema, claro ou escuro.
            val tema by aparelho.tema.collectAsStateWithLifecycle()
            val escuro = when (tema) {
                Tema.SISTEMA -> isSystemInDarkTheme()
                Tema.CLARO -> false
                Tema.ESCURO -> true
            }
            DivvupTheme(darkTheme = escuro) {
                DivvupNavHost()
            }
        }
    }
}
