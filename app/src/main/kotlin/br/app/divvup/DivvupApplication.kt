package br.app.divvup

import android.app.Application
import br.app.divvup.notificacoes.criarCanalDeAvisos
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DivvupApplication : Application() {
    /** Inicializa o Firebase na abertura: o push pode chegar com o app fechado. */
    @Inject lateinit var firebase: FirebaseApp

    override fun onCreate() {
        super.onCreate()
        criarCanalDeAvisos(this)
    }
}
