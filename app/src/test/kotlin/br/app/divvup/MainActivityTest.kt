package br.app.divvup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Issue #1: o app abre e mostra a tela provisória (roda em dev e em prod). */
@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun abreATelaProvisoria() {
        val titulo = compose.activity.getString(R.string.placeholder_titulo)
        compose.onNodeWithContentDescription(titulo).assertIsDisplayed()
    }
}
