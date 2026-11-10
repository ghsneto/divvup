package br.app.divvup.di

import br.app.divvup.BuildConfig
import br.app.divvup.core.data.ConfiguracaoFirebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfiguracaoModule {
    @Provides
    @Singleton
    fun configuracaoFirebase() = ConfiguracaoFirebase(
        projectId = BuildConfig.FIREBASE_PROJECT_ID,
        apiKey = BuildConfig.FIREBASE_API_KEY,
        applicationId = BuildConfig.FIREBASE_APP_ID,
        gcmSenderId = BuildConfig.FIREBASE_SENDER_ID,
        hostEmulador = BuildConfig.FIREBASE_EMULADOR_HOST.ifEmpty { null },
        urlBackend = BuildConfig.URL_BACKEND.trimEnd('/'),
    )
}
