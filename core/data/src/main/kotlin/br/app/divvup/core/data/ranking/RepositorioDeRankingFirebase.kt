package br.app.divvup.core.data.ranking

import br.app.divvup.core.data.casa.CasaAtualFirebase
import br.app.divvup.core.data.casa.paraMembro
import br.app.divvup.core.domain.ranking.RepositorioDeRanking
import br.app.divvup.core.model.Categoria
import br.app.divvup.core.model.Colocacao
import br.app.divvup.core.model.Evento
import br.app.divvup.core.model.Membro
import br.app.divvup.core.model.Temporada
import br.app.divvup.core.model.TipoDeEvento
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** D1/D2/D3: leituras em tempo real; tudo é gravado pelo backend. */
@Singleton
class RepositorioDeRankingFirebase @Inject constructor(private val atual: CasaAtualFirebase) : RepositorioDeRanking {

    override val moradores: Flow<List<Membro>> = atual.daCasa(
        { db, casa -> db.collection("houses").document(casa).collection("members") },
        { it.paraMembro() },
    )

    override val historico: Flow<List<Evento>> = atual.daCasa(
        { db, casa ->
            db.collection("houses").document(casa).collection("activity")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(LIMITE_DO_HISTORICO)
        },
        { it.paraEvento() },
    )

    override val temporadas: Flow<List<Temporada>> = atual.daCasa(
        { db, casa -> db.collection("houses").document(casa).collection("seasons").orderBy(FieldPath.documentId(), Query.Direction.DESCENDING) },
        { it.paraTemporada() },
    )

    private companion object {
        const val LIMITE_DO_HISTORICO = 100L
    }
}

private fun DocumentSnapshot.paraEvento(): Evento? {
    val tipo = TipoDeEvento.entries.firstOrNull { it.name == getString("type") } ?: return null
    val quando = getTimestamp("createdAt")?.toDate()?.toInstant() ?: return null
    return Evento(
        id = id,
        tipo = tipo,
        autorId = getString("actorId").orEmpty(),
        autorNome = getString("actorName").orEmpty(),
        autorAvatar = getString("actorAvatar"),
        quando = quando,
        tarefaTitulo = getString("taskTitle"),
        categoria = getString("category")?.let { c -> Categoria.entries.firstOrNull { it.name == c } },
        pontos = getLong("points")?.toInt(),
        atrasada = getBoolean("late") == true,
    )
}

private fun DocumentSnapshot.paraTemporada(): Temporada {
    val colocacoes = (get("standings") as? List<*>).orEmpty().mapNotNull { item ->
        val c = item as? Map<*, *> ?: return@mapNotNull null
        Colocacao(
            uid = c["uid"] as? String ?: return@mapNotNull null,
            nome = c["name"] as? String ?: "",
            avatarId = c["avatarId"] as? String,
            pontos = (c["points"] as? Number)?.toInt() ?: 0,
        )
    }
    return Temporada(id, colocacoes, getString("championId"))
}
