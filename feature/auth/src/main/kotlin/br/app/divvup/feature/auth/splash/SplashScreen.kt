package br.app.divvup.feature.auth.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.app.divvup.core.designsystem.component.DivvupLogo
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.conta.DestinoInicial
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.domain.conta.ResultadoConta
import br.app.divvup.core.domain.conta.Sessao
import br.app.divvup.core.domain.conta.destinoInicial
import br.app.divvup.feature.auth.AuthMedidas
import br.app.divvup.feature.auth.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** A1: decide o destino pela sessão e pelo perfil (TELAS.md A1). */
@HiltViewModel
class SplashViewModel @Inject constructor(conta: RepositorioDeConta) : ViewModel() {
    val destino: StateFlow<DestinoInicial?> = flow<DestinoInicial?> {
        val sessao = conta.sessao.first()
        val perfil = (sessao as? Sessao.Conectado)?.let { (conta.perfil(it.uid) as? ResultadoConta.Ok)?.valor }
        emit(destinoInicial(sessao, perfil))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@Composable
fun SplashRota(aoDecidir: (DestinoInicial) -> Unit, viewModel: SplashViewModel = hiltViewModel()) {
    val destino by viewModel.destino.collectAsStateWithLifecycle()
    LaunchedEffect(destino) { destino?.let(aoDecidir) }
    SplashScreen()
}

/** A1 Splash: logo (símbolo + nome, 60dp de altura) centralizado sobre `background`. */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(DivvupTheme.colors.background), contentAlignment = Alignment.Center) {
        DivvupLogo(descricao = stringResource(R.string.auth_logo), altura = AuthMedidas.logoSplash)
    }
}
