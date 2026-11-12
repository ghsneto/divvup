package br.app.divvup.core.designsystem.icon

import androidx.annotation.StringRes
import br.app.divvup.core.designsystem.R
import br.app.divvup.core.model.Categoria
import br.app.divvup.core.model.Peso

/** Ícone, ilustração do topo do cartão e nome de cada categoria (R-11). */
val Categoria.icone: DivvupIcone
    get() = when (this) {
        Categoria.COZINHA -> DivvupIcone.Cozinha
        Categoria.LIMPEZA_GERAL -> DivvupIcone.Limpeza
        Categoria.BANHEIRO -> DivvupIcone.Banheiro
        Categoria.LAVANDERIA -> DivvupIcone.Lavanderia
        Categoria.LIXO -> DivvupIcone.Lixo
        Categoria.COMPRAS -> DivvupIcone.Compras
        Categoria.PETS -> DivvupIcone.Pets
        Categoria.PLANTAS -> DivvupIcone.Plantas
        Categoria.MANUTENCAO -> DivvupIcone.Manutencao
        Categoria.OUTROS -> DivvupIcone.Outros
    }

val Categoria.ilustracao: IlustracaoCategoria
    get() = when (this) {
        Categoria.COZINHA -> IlustracaoCategoria.Cozinha
        Categoria.LIMPEZA_GERAL -> IlustracaoCategoria.LimpezaGeral
        Categoria.BANHEIRO -> IlustracaoCategoria.Banheiro
        Categoria.LAVANDERIA -> IlustracaoCategoria.Lavanderia
        Categoria.LIXO -> IlustracaoCategoria.Lixo
        Categoria.COMPRAS -> IlustracaoCategoria.Compras
        Categoria.PETS -> IlustracaoCategoria.Pets
        Categoria.PLANTAS -> IlustracaoCategoria.Plantas
        Categoria.MANUTENCAO -> IlustracaoCategoria.Manutencao
        Categoria.OUTROS -> IlustracaoCategoria.Outros
    }

@get:StringRes
val Categoria.nome: Int
    get() = when (this) {
        Categoria.COZINHA -> R.string.categoria_cozinha
        Categoria.LIMPEZA_GERAL -> R.string.categoria_limpeza_geral
        Categoria.BANHEIRO -> R.string.categoria_banheiro
        Categoria.LAVANDERIA -> R.string.categoria_lavanderia
        Categoria.LIXO -> R.string.categoria_lixo
        Categoria.COMPRAS -> R.string.categoria_compras
        Categoria.PETS -> R.string.categoria_pets
        Categoria.PLANTAS -> R.string.categoria_plantas
        Categoria.MANUTENCAO -> R.string.categoria_manutencao
        Categoria.OUTROS -> R.string.categoria_outros
    }

/** "leve", "média", "pesada" (rótulo acima do selo de pontos). */
@get:StringRes
val Peso.nome: Int
    get() = when (this) {
        Peso.LEVE -> R.string.peso_leve
        Peso.MEDIA -> R.string.peso_media
        Peso.PESADA -> R.string.peso_pesada
    }
