package br.app.divvup.core.data.casa

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Usuário conectado e id da casa atual dele, em tempo real (`users/{uid}.currentHouseId`). */
@Singleton
class CasaAtualFirebase @Inject constructor(private val auth: FirebaseAuth, val firestore: FirebaseFirestore) {

    val uid: Flow<String?> = callbackFlow {
        val ouvinte = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(ouvinte)
        awaitClose { auth.removeAuthStateListener(ouvinte) }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val casaId: Flow<String?> = uid
        .flatMapLatest { uid ->
            if (uid == null) flowOf(null) else observar(firestore.collection("users").document(uid)).map { it?.getString("currentHouseId") }
        }
        .distinctUntilChanged()

    /** Documentos da subcoleção `houses/{casa}/<colecao>` da casa atual; lista vazia sem casa. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> daCasa(consulta: (FirebaseFirestore, String) -> Query, converter: (DocumentSnapshot) -> T?): Flow<List<T>> =
        casaId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else observar(consulta(firestore, id)).map { q -> q?.documents.orEmpty().mapNotNull(converter) }
        }
}

/** Documento em tempo real; `null` se não existe ou se a leitura é negada (ex.: saiu da casa). */
fun observar(ref: DocumentReference): Flow<DocumentSnapshot?> = callbackFlow {
    val registro = ref.addSnapshotListener { doc, erro ->
        trySend(if (erro != null || doc == null || !doc.exists()) null else doc)
    }
    awaitClose { registro.remove() }
}

/** Consulta em tempo real; `null` se a leitura é negada. */
fun observar(consulta: Query): Flow<QuerySnapshot?> = callbackFlow {
    val registro = consulta.addSnapshotListener { q, erro -> trySend(if (erro != null) null else q) }
    awaitClose { registro.remove() }
}
