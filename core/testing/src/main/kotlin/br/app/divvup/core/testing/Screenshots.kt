package br.app.divvup.core.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
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
            // Modo de inspeção: animações infinitas (barra de progresso) ficam paradas.
            CompositionLocalProvider(LocalInspectionMode provides true) {
                DivvupTheme(darkTheme = escuro) {
                    Box(Modifier.background(DivvupTheme.colors.background).padding(margem.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

/** Captura uma tela inteira em 360×800 dp (a área do artboard de docs/design), nos dois temas. */
fun capturarTela(nome: String, content: @Composable () -> Unit) {
    capturarNosDoisTemas(nome, margem = 0) {
        Box(Modifier.size(LARGURA_ARTBOARD.dp, ALTURA_ARTBOARD.dp)) { content() }
    }
}

private const val LARGURA_ARTBOARD = 360
private const val ALTURA_ARTBOARD = 800
