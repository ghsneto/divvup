package br.app.divvup.core.data

/**
 * Configuração do Firebase e do backend por flavor (ADR 0002). Vem do `BuildConfig` do app:
 * `dev` aponta para o Firebase Emulator e o Worker local; `prod` para `divvup-prod`.
 */
data class ConfiguracaoFirebase(
    val projectId: String,
    val apiKey: String,
    val applicationId: String,
    val gcmSenderId: String,
    /** Host do Emulator visto do aparelho (10.0.2.2 no emulador Android); `null` em prod. */
    val hostEmulador: String?,
    /** URL base do Worker, sem barra final. */
    val urlBackend: String,
    /** ID do cliente OAuth web do projeto (login com Google); vazio se não configurado. */
    val googleWebClientId: String = "",
) {
    val usaEmulador: Boolean get() = hostEmulador != null
}
