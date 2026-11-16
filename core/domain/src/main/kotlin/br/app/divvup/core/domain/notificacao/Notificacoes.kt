package br.app.divvup.core.domain.notificacao

import br.app.divvup.core.model.Notificacao
import kotlinx.coroutines.flow.Flow
import java.time.Duration

/** R-37: a central mostra 30 dias. */
val VALIDADE_DOS_AVISOS: Duration = Duration.ofDays(30)

/** Central de avisos (F1) e aparelho que recebe push (R-36). */
interface RepositorioDeNotificacoes {
    /** Últimos 30 dias, mais novos primeiro. */
    val avisos: Flow<List<Notificacao>>

    suspend fun marcarLida(id: String)

    suspend fun marcarTodas()

    /** Manda o token FCM deste aparelho para o backend (ao entrar na casa e quando o token muda). */
    suspend fun registrarAparelho(token: String? = null)
}
