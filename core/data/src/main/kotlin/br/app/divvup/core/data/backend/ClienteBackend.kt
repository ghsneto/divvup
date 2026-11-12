package br.app.divvup.core.data.backend

import br.app.divvup.core.data.ConfiguracaoFirebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Falha de uma operação do backend: `codigo` é o campo `erro` da resposta (ou rede/interno). */
class ErroDoBackend(val status: Int, val codigo: String, val detalhe: String? = null) : IOException("backend: $status $codigo")

/** Fornece o ID token do usuário conectado (Firebase Auth). */
fun interface FonteDeToken {
    suspend fun idToken(): String?
}

/**
 * Chama as operações protegidas do Worker (`POST /v1/<operacao>`) com o ID token
 * (SPEC §4.2: o cliente nunca grava pontos, atribuição, papéis nem vínculo com casa).
 */
@Singleton
class ClienteBackend @Inject constructor(
    private val config: ConfiguracaoFirebase,
    private val http: OkHttpClient,
    private val token: FonteDeToken,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun chamar(operacao: String, corpo: JsonObject = JsonObject(emptyMap())): JsonObject =
        withContext(Dispatchers.IO) {
            val idToken = token.idToken() ?: throw ErroDoBackend(401, "nao-autenticado")
            val requisicao = Request.Builder()
                .url("${config.urlBackend}/v1/$operacao")
                .header("authorization", "Bearer $idToken")
                .post(corpo.toString().toRequestBody(TIPO_JSON))
                .build()
            http.newCall(requisicao).execute().use { resposta ->
                val texto = resposta.body.string()
                val conteudo = runCatching { json.parseToJsonElement(texto).jsonObject }.getOrNull()
                if (!resposta.isSuccessful) {
                    val codigo = conteudo?.get("erro")?.textoOuNull() ?: "http-${resposta.code}"
                    throw ErroDoBackend(resposta.code, codigo, conteudo?.get("detalhe")?.textoOuNull())
                }
                conteudo ?: JsonObject(emptyMap())
            }
        }

    private fun JsonElement.textoOuNull(): String? = runCatching { jsonPrimitive.content }.getOrNull()

    private companion object {
        val TIPO_JSON = "application/json; charset=utf-8".toMediaType()
    }
}
