package br.app.divvup.core.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.app.divvup.core.designsystem.theme.DivvupTheme
import com.github.takahirom.roborazzi.captureRoboImage

/** Pasta das imagens aprovadas, relativa ao módulo (mesma do `roborazzi.outputDir`). */
const val PASTA_SCREENSHOTS = "src/test/screenshots"

/**
 * Captura `content` no tema claro (`<nome>.png`) e no escuro (`<nome>__escuro.png`), com o fundo
 * `background` do tema. `margem` dá respiro em volta de componentes isolados; telas usam 0.
 */
fun capturarNosDoisTemas(
    nome: String,
    margem: Int = 16,
    content: @Composable () -> Unit,
) {
    for (escuro in listOf(false, true)) {
        val sufixo = if (escuro) "__escuro" else ""
        captureRoboImage("$PASTA_SCREENSHOTS/$nome$sufixo.png") {
            DivvupTheme(darkTheme = escuro) {
                Box(Modifier.background(DivvupTheme.colors.background).padding(margem.dp)) {
                    content()
                }
            }
        }
    }
}
