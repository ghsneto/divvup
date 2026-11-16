package br.app.divvup.core.domain.conta

import br.app.divvup.core.model.Tema
import kotlinx.coroutines.flow.StateFlow

/** E5: preferências que ficam só no aparelho. */
interface PreferenciasDoAparelho {
    val tema: StateFlow<Tema>

    fun escolherTema(tema: Tema)
}
