package br.app.divvup.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.app.divvup.R
import kotlinx.serialization.Serializable

/** Rota provisória do esqueleto (issue #1). A Splash (A1) a substitui na issue #5. */
@Serializable
data object Placeholder

@Composable
fun DivvupNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Placeholder) {
        composable<Placeholder> { PlaceholderScreen() }
    }
}

@Composable
private fun PlaceholderScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.placeholder_titulo))
    }
}
