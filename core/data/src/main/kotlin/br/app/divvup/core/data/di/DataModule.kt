package br.app.divvup.core.data.di

import android.content.Context
import br.app.divvup.core.data.ConfiguracaoFirebase
import br.app.divvup.core.data.backend.FonteDeToken
import br.app.divvup.core.data.conexao.MonitorDeConexao
import br.app.divvup.core.data.conexao.MonitorDeConexaoAndroid
import br.app.divvup.core.data.conta.RepositorioDeContaFirebase
import br.app.divvup.core.domain.conta.RepositorioDeConta
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Portas do Firebase Emulator (firebase/firebase.json). */
private const val PORTA_AUTH = 9099
private const val PORTA_FIRESTORE = 8080

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Inicializa o Firebase a partir do `BuildConfig` (sem o plugin google-services). No flavor
     * `dev` liga Auth e Firestore ao Emulator: nada de dev toca `divvup-prod` (ADR 0002).
     */
    @Provides
    @Singleton
    fun firebaseApp(@ApplicationContext context: Context, config: ConfiguracaoFirebase): FirebaseApp =
        FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setProjectId(config.projectId)
                .setApiKey(config.apiKey)
                .setApplicationId(config.applicationId)
                .setGcmSenderId(config.gcmSenderId)
                .build(),
        )

    @Provides
    @Singleton
    fun firebaseAuth(app: FirebaseApp, config: ConfiguracaoFirebase): FirebaseAuth =
        FirebaseAuth.getInstance(app).apply {
            config.hostEmulador?.let { useEmulator(it, PORTA_AUTH) }
            setLanguageCode("pt-BR")
        }

    @Provides
    @Singleton
    fun firestore(app: FirebaseApp, config: ConfiguracaoFirebase): FirebaseFirestore =
        FirebaseFirestore.getInstance(app).apply {
            config.hostEmulador?.let { useEmulator(it, PORTA_FIRESTORE) }
        }

    @Provides
    @Singleton
    fun fonteDeToken(auth: FirebaseAuth): FonteDeToken =
        FonteDeToken { auth.currentUser?.getIdToken(false)?.await()?.token }

    @Provides
    @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoriosModule {
    @Binds
    abstract fun repositorioDeConta(impl: RepositorioDeContaFirebase): RepositorioDeConta

    @Binds
    abstract fun monitorDeConexao(impl: MonitorDeConexaoAndroid): MonitorDeConexao
}
