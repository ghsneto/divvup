package br.app.divvup.feature.onboarding

import br.app.divvup.core.testing.capturarTela
import br.app.divvup.feature.onboarding.tutorial.TutorialScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TutorialTest {
    private fun tela(passo: Int) = capturarTela("B6_tutorial_${passo + 1}") { TutorialScreen(passo, {}, {}, {}, {}, {}, semAnimacao = true) }

    @Test fun screenshotB6Assumir() = tela(0)

    @Test fun screenshotB6Passar() = tela(1)

    @Test fun screenshotB6Desfazer() = tela(2)

    @Test fun screenshotB6Pronto() = tela(3)
}
