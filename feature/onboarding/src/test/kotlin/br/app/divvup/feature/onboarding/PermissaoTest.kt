package br.app.divvup.feature.onboarding

import br.app.divvup.core.testing.capturarTela
import br.app.divvup.feature.onboarding.permissao.PermissaoScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PermissaoTest {
    @Test
    fun screenshotB7() = capturarTela("B7_permissao") { PermissaoScreen({}, {}) }
}
