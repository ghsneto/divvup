package br.app.divvup.core.domain.notificacao

import br.app.divvup.core.model.Notificacao
import br.app.divvup.core.model.TipoDeNotificacao
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.LocalTime

/** R-37: a central mostra 30 dias. */
val VALIDADE_DOS_AVISOS: Duration = Duration.ofDays(30)

/** R-33: o que vem ligado de fábrica (só "um morador concluiu" começa desligado). */
fun ligadoPorPadrao(tipo: TipoDeNotificacao): Boolean = tipo != TipoDeNotificacao.MEMBER_COMPLETED

/** E4: tipos ligados e horário silencioso (R-34, R-35; padrão 22:00–08:00, ligado). */
data class Preferencias(
    val tipos: Map<TipoDeNotificacao, Boolean> = emptyMap(),
    val silencioLigado: Boolean = true,
    val inicio: LocalTime = LocalTime.of(INICIO_PADRAO, 0),
    val fim: LocalTime = LocalTime.of(FIM_PADRAO, 0),
) {
    fun ligado(tipo: TipoDeNotificacao): Boolean = tipos[tipo] ?: ligadoPorPadrao(tipo)
}

private const val INICIO_PADRAO = 22
private const val FIM_PADRAO = 8

/** Central de avisos (F1) e aparelho que recebe push (R-36). */
interface RepositorioDeNotificacoes {
    /** Últimos 30 dias, mais novos primeiro. */
    val avisos: Flow<List<Notificacao>>

    suspend fun marcarLida(id: String)

    suspend fun marcarTodas()

    /** Manda o token FCM deste aparelho para o backend (ao entrar na casa e quando o token muda). */
    suspend fun registrarAparelho(token: String? = null)

    /** E4: preferências da conta. */
    val preferencias: Flow<Preferencias>

    suspend fun salvar(preferencias: Preferencias): Boolean
}
