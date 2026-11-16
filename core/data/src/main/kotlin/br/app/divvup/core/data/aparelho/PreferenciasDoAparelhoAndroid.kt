package br.app.divvup.core.data.aparelho

import android.content.Context
import androidx.core.content.edit
import br.app.divvup.core.domain.conta.PreferenciasDoAparelho
import br.app.divvup.core.model.Tema
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** E5: tema em SharedPreferences (não vai para o Firestore). */
@Singleton
class PreferenciasDoAparelhoAndroid @Inject constructor(@ApplicationContext context: Context) : PreferenciasDoAparelho {
    private val prefs = context.getSharedPreferences("aparelho", Context.MODE_PRIVATE)
    private val _tema = MutableStateFlow(Tema.entries.firstOrNull { it.name == prefs.getString(TEMA, null) } ?: Tema.SISTEMA)
    override val tema: StateFlow<Tema> = _tema.asStateFlow()

    override fun escolherTema(tema: Tema) {
        prefs.edit { putString(TEMA, tema.name) }
        _tema.value = tema
    }

    private companion object {
        const val TEMA = "tema"
    }
}
