package br.app.divvup.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.app.divvup.core.designsystem.icon.DivvupIcone

/** Tamanhos de ícone do estilo das telas: `.ic` 24, `.ic-s` 18, `.ic-xs` 14, `.ic-l` 32. */
enum class TamanhoIcone(val dp: Dp) {
    Padrao(24.dp),
    Pequeno(18.dp),
    MuitoPequeno(14.dp),
    Grande(32.dp),
}

/** Ícone na cor do texto (ou `tint`). Decorativo quando `descricao` é null. */
@Composable
fun DivvupIcon(
    icone: DivvupIcone,
    modifier: Modifier = Modifier,
    descricao: String? = null,
    tamanho: Dp = TamanhoIcone.Padrao.dp,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(icone.res),
        contentDescription = descricao,
        modifier = modifier.size(tamanho),
        tint = tint,
    )
}
