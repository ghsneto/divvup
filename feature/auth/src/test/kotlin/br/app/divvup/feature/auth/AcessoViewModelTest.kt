package br.app.divvup.feature.auth

import br.app.divvup.core.domain.conta.DestinoInicial
import br.app.divvup.core.domain.conta.ErroDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.model.Usuario
import br.app.divvup.core.testing.RegraDispatcherPrincipal
import br.app.divvup.core.testing.fakes.FakeMonitorDeConexao
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConta
import br.app.divvup.feature.auth.criarconta.CriarContaViewModel
import br.app.divvup.feature.auth.entrar.EntrarViewModel
import br.app.divvup.feature.auth.google.LoginGoogleViewModel
import br.app.divvup.feature.auth.recuperar.RecuperarSenhaViewModel
import br.app.divvup.feature.auth.splash.SplashViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AcessoViewModelTest {
    @get:Rule val principal = RegraDispatcherPrincipal()

    /** Mantém o `stateIn(WhileSubscribed)` ativo, como a tela faria. */
    private fun TestScope.observar(fluxo: Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { fluxo.collect {} }
    }

    @Test
    fun `splash sem sessao vai para boas-vindas`() = runTest {
        val vm = SplashViewModel(FakeRepositorioDeConta())
        assertEquals(DestinoInicial.BoasVindas, vm.destino.first { it != null })
    }

    @Test
    fun `splash com sessao e casa vai para o deck`() = runTest {
        val repo = FakeRepositorioDeConta(Sessao.Conectado("u"), Usuario("u", "Iane", null, null, "h1"))
        assertEquals(DestinoInicial.Deck, SplashViewModel(repo).destino.first { it != null })
    }

    @Test
    fun `entrar valida o e-mail antes de chamar o Firebase`() = runTest {
        val repo = FakeRepositorioDeConta()
        val vm = EntrarViewModel(repo, FakeMonitorDeConexao())
        observar(vm.estado)
        vm.aoMudarEmail("iane@")
        vm.aoMudarSenha("cajueiro2025")
        vm.entrar()
        assertEquals(ErroDeConta.EmailInvalido, vm.estado.value.erro)
        assertTrue(vm.estado.value.erroNoEmail)
        assertTrue(repo.chamadas.isEmpty())
    }

    @Test
    fun `entrar com senha errada mostra o erro e corrigir a senha limpa`() = runTest {
        val repo = FakeRepositorioDeConta().apply { resultadoEntrar = ResultadoConta.Falha(ErroDeConta.SenhaIncorreta) }
        val vm = EntrarViewModel(repo, FakeMonitorDeConexao())
        observar(vm.estado)
        vm.aoMudarEmail("iane@email.com")
        vm.aoMudarSenha("errada")
        vm.entrar()
        assertEquals(ErroDeConta.SenhaIncorreta, vm.estado.value.erro)
        vm.aoMudarSenha("errada2")
        assertNull(vm.estado.value.erro)
    }

    @Test
    fun `muitas tentativas e sem conexao desabilitam o botao`() = runTest {
        val conexao = FakeMonitorDeConexao()
        val repo = FakeRepositorioDeConta().apply { resultadoEntrar = ResultadoConta.Falha(ErroDeConta.MuitasTentativas) }
        val vm = EntrarViewModel(repo, conexao)
        observar(vm.estado)
        vm.aoMudarEmail("iane@email.com")
        vm.entrar()
        assertFalse(vm.estado.value.botaoHabilitado)

        val vm2 = EntrarViewModel(FakeRepositorioDeConta(), conexao)
        observar(vm2.estado)
        conexao.estado.value = false
        assertFalse(vm2.estado.value.botaoHabilitado)
    }

    @Test
    fun `entrar com sucesso sinaliza entrou`() = runTest {
        val vm = EntrarViewModel(FakeRepositorioDeConta(), FakeMonitorDeConexao())
        observar(vm.estado)
        vm.aoMudarEmail("iane@email.com")
        vm.aoMudarSenha("cajueiro2025")
        vm.entrar()
        assertTrue(vm.estado.value.entrou)
    }

    @Test
    fun `criar conta so envia formulario valido e mostra e-mail duplicado`() = runTest {
        val repo = FakeRepositorioDeConta().apply { resultadoCriar = ResultadoConta.Falha(ErroDeConta.EmailJaCadastrado) }
        val vm = CriarContaViewModel(repo)
        vm.aoMudarNome("Iane")
        vm.aoMudarEmail("iane@email.com")
        vm.aoMudarSenha("cajueiro25")
        vm.aoMudarConfirmacao("cajueiro25")
        vm.criar()
        assertTrue(repo.chamadas.isEmpty(), "sem aceitar os termos não envia")

        vm.aoMudarTermos(true)
        vm.criar()
        assertEquals(listOf("criar:Iane:iane@email.com"), repo.chamadas)
        assertTrue(vm.estado.value.emailDuplicado)
        assertFalse(vm.estado.value.botaoHabilitado)

        vm.aoMudarEmail("iane2@email.com")
        assertFalse(vm.estado.value.emailDuplicado)
        assertTrue(vm.estado.value.botaoHabilitado)
    }

    @Test
    fun `login com Google entra e, se falhar, mostra o erro (R-01)`() = runTest {
        val config = br.app.divvup.core.data.ConfiguracaoFirebase("demo-divvup", "k", "a", "s", "10.0.2.2", "http://x", "web-id")
        val repo = FakeRepositorioDeConta()
        val vm = LoginGoogleViewModel(repo, config)
        vm.aoToken("token-google")
        assertTrue(vm.estado.value.entrou)
        assertEquals(listOf("google:token-google"), repo.chamadas)

        val repoFalha = FakeRepositorioDeConta().apply { resultadoGoogle = ResultadoConta.Falha(ErroDeConta.SemConexao) }
        val vm2 = LoginGoogleViewModel(repoFalha, config)
        vm2.aoToken("t")
        assertEquals(ErroDeConta.SemConexao, vm2.estado.value.erro)
        vm2.limparErro()
        assertNull(vm2.estado.value.erro)
    }

    @Test
    fun `recuperar senha confirma o envio e permite usar outro e-mail`() = runTest {
        val repo = FakeRepositorioDeConta()
        val vm = RecuperarSenhaViewModel(repo)
        vm.aoMudarEmail("iane@email.com")
        vm.enviar()
        assertTrue(vm.estado.value.enviado)
        vm.usarOutroEmail()
        assertFalse(vm.estado.value.enviado)
        assertEquals(listOf("recuperar:iane@email.com"), repo.chamadas)
    }
}
