package br.app.divvup.feature.auth.google

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.data.ConfiguracaoFirebase
import br.app.divvup.core.designsystem.component.DURACAO_SNACKBAR_MS
import br.app.divvup.core.designsystem.component.DivvupSnackbar
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.domain.conta.ErroDeConta
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.feature.auth.AuthMedidas
import br.app.divvup.feature.auth.comum.mensagem
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado do login com Google (A2–A4, R-01). */
data class LoginGoogleEstado(
    val carregando: Boolean = false,
    val erro: ErroDeConta? = null,
    val entrou: Boolean = false,
)

@HiltViewModel
class LoginGoogleViewModel @Inject constructor(
    private val conta: RepositorioDeConta,
    private val config: ConfiguracaoFirebase,
) : ViewModel() {
    private val _estado = MutableStateFlow(LoginGoogleEstado())
    val estado: StateFlow<LoginGoogleEstado> = _estado.asStateFlow()

    val clienteWeb: String get() = config.googleWebClientId

    fun aoToken(idToken: String) {
        _estado.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            when (val r = conta.entrarComGoogle(idToken)) {
                is ResultadoConta.Ok -> _estado.update { it.copy(carregando = false, entrou = true) }
                is ResultadoConta.Falha -> _estado.update { it.copy(carregando = false, erro = r.erro) }
            }
        }
    }

    fun aoFalhar(erro: ErroDeConta) = _estado.update { it.copy(carregando = false, erro = erro) }

    fun limparErro() = _estado.update { it.copy(erro = null) }
}

/**
 * Abre o seletor de contas do Google (Credential Manager) e entrega o ID token ao ViewModel.
 * Cancelar não mostra erro. Sem cliente web configurado, mostra "Google indisponível".
 */
suspend fun pedirTokenGoogle(context: Context, clienteWeb: String): Result<String?> {
    if (clienteWeb.isBlank()) return Result.failure(IllegalStateException("cliente web do Google ausente"))
    val pedido = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(clienteWeb).build())
        .build()
    return try {
        val credencial = CredentialManager.create(context).getCredential(context, pedido).credential
        if (credencial is CustomCredential && credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            Result.success(GoogleIdTokenCredential.createFrom(credencial.data).idToken)
        } else {
            Result.failure(IllegalStateException("credencial inesperada: ${credencial.type}"))
        }
    } catch (_: GetCredentialCancellationException) {
        Result.success(null)
    } catch (e: NoCredentialException) {
        // Nenhuma conta Google no aparelho.
        Result.failure(e)
    } catch (e: GetCredentialException) {
        Result.failure(e)
    }
}

/**
 * Envolve uma tela de acesso com o login com Google: passa `aoGoogle` para a tela e mostra o erro
 * num snackbar (o desenho não reserva lugar para ele). `aoEntrar` é chamado depois do login.
 */
@Composable
fun ComLoginGoogle(
    aoEntrar: () -> Unit,
    viewModel: LoginGoogleViewModel = hiltViewModel(),
    tela: @Composable BoxScope.(aoGoogle: () -> Unit) -> Unit,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    LaunchedEffect(estado.entrou) { if (estado.entrou) aoEntrar() }
    LaunchedEffect(estado.erro) {
        if (estado.erro != null) {
            delay(DURACAO_SNACKBAR_MS.toLong())
            viewModel.limparErro()
        }
    }
    Box(Modifier.fillMaxSize()) {
        tela {
            if (!estado.carregando) {
                escopo.launch {
                    pedirTokenGoogle(context, viewModel.clienteWeb).fold(
                        onSuccess = { token -> token?.let(viewModel::aoToken) },
                        onFailure = { viewModel.aoFalhar(ErroDeConta.GoogleIndisponivel) },
                    )
                }
            }
        }
        estado.erro?.let {
            DivvupSnackbar(
                mensagem = stringResource(it.mensagem()),
                icone = DivvupIcone.Alerta,
                contagem = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = AuthMedidas.snackLateral, end = AuthMedidas.snackLateral, bottom = AuthMedidas.snackFundo),
            )
        }
    }
}
