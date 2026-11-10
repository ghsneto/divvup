package br.app.divvup.core.data.conexao

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/** Há internet? Alimenta os estados "sem conexão" das telas (TELAS.md). */
interface MonitorDeConexao {
    val online: Flow<Boolean>
}

@Singleton
class MonitorDeConexaoAndroid @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : MonitorDeConexao {

    override val online: Flow<Boolean> = callbackFlow {
        val gerente = context.getSystemService(ConnectivityManager::class.java)
        fun atual() = gerente.getNetworkCapabilities(gerente.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(atual()) }
            override fun onLost(network: Network) { trySend(atual()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(atual()) }
        }
        trySend(atual())
        gerente.registerNetworkCallback(
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            callback,
        )
        awaitClose { gerente.unregisterNetworkCallback(callback) }
    }.conflate().distinctUntilChanged()
}
