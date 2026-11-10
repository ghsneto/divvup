package br.app.divvup.di

import br.app.divvup.core.data.conexao.MonitorDeConexao
import br.app.divvup.core.data.di.RepositoriosModule
import br.app.divvup.core.domain.conta.RepositorioDeConta
import br.app.divvup.core.testing.fakes.FakeMonitorDeConexao
import br.app.divvup.core.testing.fakes.FakeRepositorioDeConta
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/** Nos testes do app, nada fala com o Firebase: repositórios em memória. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositoriosModule::class])
object RepositoriosDeTesteModule {
    @Provides @Singleton
    fun conta(): RepositorioDeConta = FakeRepositorioDeConta()

    @Provides @Singleton
    fun conexao(): MonitorDeConexao = FakeMonitorDeConexao()
}
