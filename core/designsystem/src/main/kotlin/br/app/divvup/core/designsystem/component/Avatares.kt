package br.app.divvup.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import br.app.divvup.core.designsystem.R
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.domain.perfil.GaleriaDeAvatares

/** Imagens da galeria (R-32), na ordem de [GaleriaDeAvatares.ids]. Geradas por docs/design/ferramentas/avatares. */
private val IMAGENS = intArrayOf(
    R.drawable.avatar_01,
    R.drawable.avatar_02,
    R.drawable.avatar_03,
    R.drawable.avatar_04,
    R.drawable.avatar_05,
    R.drawable.avatar_06,
    R.drawable.avatar_07,
    R.drawable.avatar_08,
    R.drawable.avatar_09,
    R.drawable.avatar_10,
    R.drawable.avatar_11,
    R.drawable.avatar_12,
    R.drawable.avatar_13,
    R.drawable.avatar_14,
    R.drawable.avatar_15,
    R.drawable.avatar_16,
    R.drawable.avatar_17,
    R.drawable.avatar_18,
    R.drawable.avatar_19,
    R.drawable.avatar_20,
    R.drawable.avatar_21,
    R.drawable.avatar_22,
    R.drawable.avatar_23,
    R.drawable.avatar_24,
)

@DrawableRes
fun imagemDoAvatar(avatarId: String?): Int = IMAGENS[GaleriaDeAvatares.indice(avatarId)]

/** Avatar da galeria: círculo (`.av`) com borda opcional de 1dp (ex.: lima no avatar grande de B1/E3). */
@Composable
fun DivvupAvatarDaGaleria(
    avatarId: String?,
    tamanho: Dp,
    modifier: Modifier = Modifier,
    descricao: String? = null,
    corDaBorda: Color? = null,
) {
    DivvupAvatar(
        imagem = painterResource(imagemDoAvatar(avatarId)),
        tamanho = tamanho,
        descricao = descricao,
        modifier = if (corDaBorda != null) {
            modifier.border(DivvupTheme.spacing.borda, corDaBorda, DivvupTheme.shapes.pill)
        } else {
            modifier
        },
    )
}

/** Imagem da casa no topo de C1 (ilustração padrão do desenho; foto de capa só na v1.1). */
@Composable
fun DivvupAvatarDaCasa(tamanho: Dp, modifier: Modifier = Modifier, descricao: String? = null) {
    DivvupAvatar(painterResource(R.drawable.ilu_casa_padrao), tamanho, modifier, descricao)
}
