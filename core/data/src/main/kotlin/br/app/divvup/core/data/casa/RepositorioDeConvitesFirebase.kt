package br.app.divvup.core.data.casa

import br.app.divvup.core.data.backend.ClienteBackend
import br.app.divvup.core.data.backend.ErroDoBackend
import br.app.divvup.core.domain.casa.ErroDeConvite
import br.app.divvup.core.domain.casa.RepositorioDeConvites
import br.app.divvup.core.domain.casa.ResultadoConvite
import br.app.divvup.core.model.Convite
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.Papel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositorioDeConvitesFirebase @Inject constructor(
    private val atual: CasaAtualFirebase,
    private val backend: ClienteBackend,
) : RepositorioDeConvites {

    override val convites: Flow<List<Convite>> = atual.daCasa(
        { db, casa -> db.collection("houses").document(casa).collection("invites").orderBy("createdAt", Query.Direction.DESCENDING) },
        { it.paraConvite() },
    )

    override val membros: Flow<List<Membro>> = atual.daCasa(
        { db, casa -> db.collection("houses").document(casa).collection("members").whereEqualTo("status", "ACTIVE") },
        { it.paraMembro() },
    )

    override suspend fun criar(): ResultadoConvite<Convite> = executar {
        val r = backend.chamar("criarConvite")
        val codigo = checkNotNull(r["codigo"]).jsonPrimitive.content
        val expira = Instant.parse(checkNotNull(r["expiraEm"]).jsonPrimitive.content)
        Convite(codigo, "", "", expira.minusSeconds(SEGUNDOS_24H), expira, null, null, false, null)
    }

    override suspend fun revogar(codigo: String): ResultadoConvite<Unit> = executar {
        backend.chamar("revogarConvite", buildJsonObject { put("codigo", JsonPrimitive(codigo)) }).let { }
    }

    override suspend fun entrar(codigo: String): ResultadoConvite<String> = executar {
        val r = backend.chamar("entrarComCodigo", buildJsonObject { put("codigo", JsonPrimitive(codigo)) })
        checkNotNull(r["casaId"]).jsonPrimitive.content
    }

    private inline fun <T> executar(bloco: () -> T): ResultadoConvite<T> = try {
        ResultadoConvite.Ok(bloco())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ResultadoConvite.Falha(erroDeConvite(e))
    }

    private companion object {
        const val SEGUNDOS_24H = 24L * 60 * 60
    }
}

/** Traduz a resposta do backend (campo `erro`) ou a falta de rede. */
internal fun erroDeConvite(e: Exception): ErroDeConvite = when {
    e is ErroDoBackend -> when (e.codigo) {
        "codigo-invalido" -> ErroDeConvite.Invalido
        "codigo-expirado" -> ErroDeConvite.Expirado
        "codigo-usado" -> ErroDeConvite.Usado
        "ja-em-casa" -> ErroDeConvite.JaEmCasa
        "nao-admin" -> ErroDeConvite.NaoAdmin
        else -> ErroDeConvite.Desconhecido
    }
    e is IOException -> ErroDeConvite.SemConexao
    else -> ErroDeConvite.Desconhecido
}

private fun DocumentSnapshot.instante(campo: String): Instant? = getTimestamp(campo)?.let(Timestamp::toInstant)

private fun DocumentSnapshot.paraConvite(): Convite? {
    val criado = instante("createdAt") ?: return null
    val expira = instante("expiresAt") ?: return null
    return Convite(
        codigo = id,
        criadoPor = getString("createdBy").orEmpty(),
        criadoPorNome = getString("createdByName").orEmpty(),
        criadoEm = criado,
        expiraEm = expira,
        usadoPorNome = getString("usedByName"),
        usadoEm = instante("usedAt"),
        revogado = getBoolean("revoked") == true,
        revogadoEm = instante("revokedAt"),
    )
}

private fun DocumentSnapshot.paraMembro(): Membro = Membro(
    uid = id,
    nome = getString("displayName").orEmpty(),
    avatarId = getString("avatarId"),
    papel = if (getString("role") == "ADMIN") Papel.ADMIN else Papel.MEMBER,
    ativo = getString("status") == "ACTIVE",
    pontosDoMes = getLong("seasonPoints")?.toInt() ?: 0,
    entrouEm = instante("joinedAt"),
)
