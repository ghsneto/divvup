package br.app.divvup.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.app.divvup.core.designsystem.R
import br.app.divvup.core.designsystem.theme.DivvupTheme

/**
 * Logo principal (símbolo + nome) com os vetores originais de `docs/brand/logo`.
 * Cores do tema: cartão em `primary`, nome em `text`. Sobre ultramarino ou lima, passe
 * a mesma cor nos dois (versão de uma cor). Abaixo de 20dp de altura, use [DivvupSimbolo].
 */
@Composable
fun DivvupLogo(
    descricao: String,
    modifier: Modifier = Modifier,
    altura: Dp = 28.dp,
    corSimbolo: Color = DivvupTheme.colors.primary,
    corNome: Color = DivvupTheme.colors.text,
) {
    Box(modifier.semantics { contentDescription = descricao }) {
        Image(
            painterResource(R.drawable.logo_simbolo),
            contentDescription = null,
            modifier = Modifier.height(altura),
            contentScale = ContentScale.FillHeight,
            colorFilter = ColorFilter.tint(corSimbolo),
        )
        Image(
            painterResource(R.drawable.logo_nome),
            contentDescription = null,
            modifier = Modifier.height(altura),
            contentScale = ContentScale.FillHeight,
            colorFilter = ColorFilter.tint(corNome),
        )
    }
}

/** Só o cartão partido (símbolo da marca). */
@Composable
fun DivvupSimbolo(
    modifier: Modifier = Modifier,
    altura: Dp = 24.dp,
    cor: Color = DivvupTheme.colors.primary,
    descricao: String? = null,
) {
    Image(
        painterResource(R.drawable.logo_simbolo_isolado),
        contentDescription = descricao,
        modifier = modifier.height(altura),
        contentScale = ContentScale.FillHeight,
        colorFilter = ColorFilter.tint(cor),
    )
}
