package br.app.divvup.feature.auth

import br.app.divvup.core.domain.conta.ErroDeConta
import br.app.divvup.core.domain.conta.FormularioCriarConta
import br.app.divvup.core.testing.capturarTela
import br.app.divvup.feature.auth.boasvindas.BoasVindasScreen
import br.app.divvup.feature.auth.criarconta.CriarContaEstado
import br.app.divvup.feature.auth.criarconta.CriarContaEventos
import br.app.divvup.feature.auth.criarconta.CriarContaScreen
import br.app.divvup.feature.auth.entrar.EntrarEstado
import br.app.divvup.feature.auth.entrar.EntrarScreen
import br.app.divvup.feature.auth.recuperar.RecuperarSenhaEstado
import br.app.divvup.feature.auth.recuperar.RecuperarSenhaScreen
import br.app.divvup.feature.auth.splash.SplashScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Telas A1–A5 nos estados do desenho (dados de exemplo de docs/design/fonte). Compare cada
 * imagem com docs/design/png/<Artboard>.png e <Artboard>__escuro.png.
 */
@RunWith(RobolectricTestRunner::class)
class AcessoScreenshotTest {

    @Test
    fun splash() = capturarTela("A1_splash") { SplashScreen() }

    @Test
    fun boasVindas() = capturarTela("A2_boasvindas") { BoasVindasScreen({}, {}, {}, {}, {}) }

    private val entrar = EntrarEstado(email = "iane@email.com", senha = "cajueiro2025")

    @Test
    fun entrarComErro() = capturarTela("A3_entrar_senha_incorreta") {
        EntrarScreen(entrar.copy(erro = ErroDeConta.SenhaIncorreta), {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test
    fun entrarSemConexao() = capturarTela("A3_entrar_sem_conexao") {
        EntrarScreen(entrar.copy(online = false), {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test
    fun entrarCarregando() = capturarTela("A3_entrar_carregando") {
        EntrarScreen(entrar.copy(carregando = true), {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test
    fun criarConta() = capturarTela("A4_criarconta") {
        CriarContaScreen(
            CriarContaEstado(FormularioCriarConta("Iane", "iane@email.com", "caju", "caju2", aceitouTermos = false)),
            CriarContaEventos(),
        )
    }

    @Test
    fun criarContaEmailDuplicado() = capturarTela("A4_criarconta_email_duplicado") {
        CriarContaScreen(
            CriarContaEstado(
                FormularioCriarConta("Iane", "iane@email.com", "Cajueiro25", "Cajueiro25", aceitouTermos = true),
                erro = ErroDeConta.EmailJaCadastrado,
            ),
            CriarContaEventos(),
        )
    }

    @Test
    fun recuperar() = capturarTela("A5_recuperar") {
        RecuperarSenhaScreen(RecuperarSenhaEstado(email = "iane@email.com"), {}, {}, {}, {})
    }

    @Test
    fun recuperarEnviado() = capturarTela("A5_recuperar_enviado") {
        RecuperarSenhaScreen(RecuperarSenhaEstado(email = "iane@email.com", enviado = true), {}, {}, {}, {})
    }
}
