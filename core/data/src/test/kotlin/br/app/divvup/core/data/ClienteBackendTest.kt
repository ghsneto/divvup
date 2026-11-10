package br.app.divvup.core.data

import br.app.divvup.core.data.backend.ClienteBackend
import br.app.divvup.core.data.backend.ErroDoBackend
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ClienteBackendTest {
    private val servidor = MockWebServer()

    @Before fun iniciar() = servidor.start()

    @After fun parar() = servidor.close()

    private fun cliente(token: String? = "tok-123") = ClienteBackend(
        config = ConfiguracaoFirebase("demo-divvup", "k", "a", "s", "10.0.2.2", servidor.url("/").toString().trimEnd('/')),
        http = OkHttpClient(),
        token = { token },
    )

    @Test
    fun `envia o ID token e o corpo para a operacao`() = runTest {
        servidor.enqueue(MockResponse.Builder().body("""{"criado":true}""").build())
        val resposta = cliente().chamar("criarPerfil", buildJsonObject { put("nome", JsonPrimitive("Iane")) })

        val req = servidor.takeRequest()
        assertEquals("/v1/criarPerfil", req.url.encodedPath)
        assertEquals("Bearer tok-123", req.headers["authorization"])
        assertEquals("""{"nome":"Iane"}""", req.body?.utf8())
        assertEquals(JsonPrimitive(true), resposta["criado"])
    }

    @Test
    fun `erro do backend vira ErroDoBackend com o codigo`() = runTest {
        servidor.enqueue(MockResponse.Builder().code(400).body("""{"erro":"nome-invalido"}""").build())
        val erro = assertFailsWith<ErroDoBackend> { cliente().chamar("criarPerfil") }
        assertEquals(400, erro.status)
        assertEquals("nome-invalido", erro.codigo)
    }

    @Test
    fun `sem usuario conectado nao chama o servidor`() = runTest {
        val erro = assertFailsWith<ErroDoBackend> { cliente(token = null).chamar("criarPerfil") }
        assertEquals("nao-autenticado", erro.codigo)
        assertEquals(0, servidor.requestCount)
    }
}
