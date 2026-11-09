package br.app.divvup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.app.divvup.navigation.DivvupNavHost
import dagger.hilt.android.AndroidEntryPoint

/** Única Activity do app; toda navegação é Navigation Compose. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DivvupNavHost()
        }
    }
}
