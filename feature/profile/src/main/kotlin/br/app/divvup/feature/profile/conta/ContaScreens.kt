package br.app.divvup.feature.profile.conta

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import br.app.divvup.core.designsystem.component.DivvupAvatarDaGaleria
import br.app.divvup.core.designsystem.component.DivvupButton
import br.app.divvup.core.designsystem.component.DivvupDivider
import br.app.divvup.core.designsystem.component.DivvupIcon
import br.app.divvup.core.designsystem.component.DivvupIconButton
import br.app.divvup.core.designsystem.component.DivvupLabel
import br.app.divvup.core.designsystem.component.DivvupLogo
import br.app.divvup.core.designsystem.component.DivvupText
import br.app.divvup.core.designsystem.component.DivvupTextField
import br.app.divvup.core.designsystem.component.DivvupTopBar
import br.app.divvup.core.designsystem.component.MensagemErro
import br.app.divvup.core.designsystem.component.TamanhoIcone
import br.app.divvup.core.designsystem.component.VarianteBotao
import br.app.divvup.core.designsystem.icon.DivvupIcone
import br.app.divvup.core.designsystem.theme.DivvupTheme
import br.app.divvup.core.model.Tema
import br.app.divvup.feature.profile.ProfileMedidas
import br.app.divvup.feature.profile.R

@Composable
private fun Topo(aoVoltar: () -> Unit) {
    DivvupTopBar(paddingInicio = false) {
        DivvupIconButton(DivvupIcone.ChevL, stringResource(R.string.prefs_voltar), aoVoltar)
        DivvupLabel(stringResource(R.string.prefs_rotulo), cor = DivvupTheme.colors.textMuted)
    }
}

/** E5 Aparência (docs/design/fonte/Aparencia.dc.html): muda o tema na hora. */
@Composable
fun AparenciaScreen(tema: Tema, aoVoltar: () -> Unit, aoEscolher: (Tema) -> Unit, modifier: Modifier = Modifier) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(modifier.fillMaxSize().background(c.background)) {
        Topo(aoVoltar)
        Column(Modifier.padding(horizontal = e.s4)) {
            DivvupText(stringResource(R.string.aparencia_titulo), t.h1.copy(fontSize = ProfileMedidas.tituloGrande))
            Text(stringResource(R.string.aparencia_texto), style = t.small, color = c.textMuted, modifier = Modifier.padding(top = e.s2))
            Column(Modifier.padding(top = e.s5).selectableGroup(), verticalArrangement = Arrangement.spacedBy(e.s2)) {
                Opcao(Tema.SISTEMA, tema, R.string.aparencia_sistema, R.string.aparencia_sistema_texto, aoEscolher)
                Opcao(Tema.CLARO, tema, R.string.aparencia_claro, R.string.aparencia_claro_texto, aoEscolher)
                Opcao(Tema.ESCURO, tema, R.string.aparencia_escuro, R.string.aparencia_escuro_texto, aoEscolher)
            }
        }
    }
}

@Composable
private fun Opcao(esta: Tema, atual: Tema, nome: Int, texto: Int, aoEscolher: (Tema) -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    val escolhida = esta == atual
    val forma = DivvupTheme.shapes.md
    val corBorda = if (escolhida) c.primary else c.border
    Row(
        Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(c.surface)
            .border(if (escolhida) ProfileMedidas.bordaEscolhida else e.borda, corBorda, forma)
            .selectable(selected = escolhida, role = Role.RadioButton) { aoEscolher(esta) }
            .padding(e.s3),
        horizontalArrangement = Arrangement.spacedBy(e.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Miniatura(esta)
        Column(Modifier.weight(1f)) {
            Text(stringResource(nome), style = t.ttl, color = c.text)
            Text(stringResource(texto), style = t.small, color = c.textMuted)
        }
        Box(
            Modifier.size(ProfileMedidas.radio).clip(DivvupTheme.shapes.pill).border(e.borda, corBorda, DivvupTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(ProfileMedidas.radioPonto).clip(DivvupTheme.shapes.pill).background(if (escolhida) c.primary else Color.Transparent))
        }
    }
}

/** Telinha do tema: metade papel/metade tinta no "sistema". */
@Composable
private fun Miniatura(tema: Tema) {
    val c = DivvupTheme.colors
    val e = DivvupTheme.spacing
    val forma = DivvupTheme.shapes.md
    val claro = c.papel
    val escuro = c.tinta
    Row(
        Modifier
            .width(ProfileMedidas.miniaturaLargura)
            .height(ProfileMedidas.miniaturaAltura)
            .clip(forma)
            .border(e.borda, c.border, forma),
    ) {
        val (a, b) = when (tema) {
            Tema.SISTEMA -> claro to escuro
            Tema.CLARO -> claro to claro
            Tema.ESCURO -> escuro to escuro
        }
        listOf(a, b).forEachIndexed { i, fundo ->
            val texto = if (fundo == escuro) c.sobreTinta.texto else escuro
            Column(Modifier.weight(1f).fillMaxHeight().background(fundo).padding(start = if (i == 0) e.s2 else ProfileMedidas.semEspaco, top = e.s2, end = if (i == 0) e.s2 else ProfileMedidas.semEspaco), verticalArrangement = Arrangement.spacedBy(e.s1)) {
                Box(Modifier.fillMaxWidth(ProfileMedidas.FRACAO_LINHA).height(ProfileMedidas.miniaturaBarra).clip(DivvupTheme.shapes.sm).background(texto))
                Box(Modifier.fillMaxWidth().height(ProfileMedidas.miniaturaBloco).clip(DivvupTheme.shapes.sm).background(c.ultramarino))
                if (i == 0) Box(Modifier.fillMaxWidth(ProfileMedidas.FRACAO_CURTA).height(ProfileMedidas.miniaturaBarra).clip(DivvupTheme.shapes.sm).background(c.lima))
            }
        }
    }
}

/** E6 Sobre (docs/design/fonte/Sobre.dc.html): versão, links legais e licenças. */
@Composable
fun SobreScreen(
    versao: String,
    build: Int,
    aoVoltar: () -> Unit,
    aoTermos: () -> Unit,
    aoPrivacidade: () -> Unit,
    aoContato: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(modifier.fillMaxSize().background(c.background)) {
        Topo(aoVoltar)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = e.s4, end = e.s4, bottom = e.s5)) {
            Column(Modifier.fillMaxWidth().clip(DivvupTheme.shapes.md).background(c.ultramarino).padding(ProfileMedidas.sobreCartaoPadding)) {
                DivvupLogo(stringResource(R.string.sobre_nome), altura = ProfileMedidas.sobreLogo, corSimbolo = c.lima, corNome = c.onUltramarino)
                Text(stringResource(R.string.sobre_nome), style = t.small, color = c.onUltramarino, modifier = Modifier.padding(top = e.s1))
                Text(
                    stringResource(R.string.sobre_versao, versao, build),
                    style = t.small.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    color = c.lima,
                    modifier = Modifier.padding(top = e.s5),
                )
            }
            Column(Modifier.padding(top = ProfileMedidas.cartaoTopo)) {
                DivvupDivider()
                LinkSobre(null, stringResource(R.string.sobre_termos), stringResource(R.string.sobre_dominio), aoTermos)
                LinkSobre(null, stringResource(R.string.sobre_privacidade), stringResource(R.string.sobre_lgpd), aoPrivacidade)
                LinkSobre(DivvupIcone.Email, stringResource(R.string.sobre_contato), null, aoContato)
            }
            DivvupLabel(stringResource(R.string.sobre_licencas), Modifier.padding(top = ProfileMedidas.secaoTopo, bottom = e.s2))
            listOf(
                R.string.sobre_dicebear to R.string.sobre_dicebear_licenca,
                R.string.sobre_avataaars to R.string.sobre_avataaars_licenca,
                R.string.sobre_bricolage to R.string.sobre_bricolage_licenca,
            ).forEach { (nome, licenca) ->
                Row(Modifier.padding(bottom = e.s3), horizontalArrangement = Arrangement.spacedBy(e.s4)) {
                    Text(stringResource(nome), style = t.small.copy(fontWeight = FontWeight.SemiBold), color = c.text)
                    Text(stringResource(licenca), style = t.small, color = c.textMuted)
                }
            }
            Text(stringResource(R.string.sobre_rodape), style = t.small, color = c.textMuted, modifier = Modifier.padding(top = e.s4))
        }
    }
}

@Composable
private fun LinkSobre(icone: DivvupIcone?, nome: String, meta: String?, aoClicar: () -> Unit) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = ProfileMedidas.linkAltura).clickable(role = Role.Button, onClick = aoClicar),
            horizontalArrangement = Arrangement.spacedBy(e.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icone?.let { DivvupIcon(it, tamanho = TamanhoIcone.Pequeno.dp, tint = c.text) }
            Text(nome, style = t.ttl, color = c.text, modifier = Modifier.weight(1f))
            meta?.let { Text(it, style = t.small, color = c.textMuted) }
            DivvupIcon(DivvupIcone.ChevR, tamanho = TamanhoIcone.Pequeno.dp, tint = c.textMuted)
        }
        DivvupDivider()
    }
}

/** Estado de E2. */
data class EditarPerfilEstado(
    val nome: String = "",
    val avatarId: String? = null,
    val email: String? = null,
    val temSenha: Boolean = true,
    val salvando: Boolean = false,
    val erro: Boolean = false,
) {
    val podeSalvar: Boolean get() = nome.trim().length in 1..NOME_MAXIMO && !salvando
}

const val NOME_MAXIMO = 24

/** E2 Editar perfil (docs/design/fonte/EditarPerfil.dc.html). "Adicionar foto" fica fora da v1.0 (ADR 0003). */
@Composable
fun EditarPerfilScreen(
    estado: EditarPerfilEstado,
    aoVoltar: () -> Unit,
    aoTrocarAvatar: () -> Unit,
    aoMudarNome: (String) -> Unit,
    aoAlterarSenha: () -> Unit,
    aoSalvar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DivvupTheme.colors
    val t = DivvupTheme.typography
    val e = DivvupTheme.spacing
    Column(modifier.fillMaxSize().background(c.background)) {
        Topo(aoVoltar)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = e.s4, end = e.s4, bottom = e.s4)) {
            DivvupText(stringResource(R.string.editar_titulo), t.h1.copy(fontSize = ProfileMedidas.tituloGrande))
            Row(Modifier.padding(top = e.s5, bottom = ProfileMedidas.cartaoTopo), horizontalArrangement = Arrangement.spacedBy(e.s4), verticalAlignment = Alignment.CenterVertically) {
                DivvupAvatarDaGaleria(estado.avatarId, ProfileMedidas.avatarEditar)
                DivvupButton(stringResource(R.string.editar_trocar_avatar), aoTrocarAvatar, variante = VarianteBotao.Texto, icone = DivvupIcone.Perfil, alinharInicio = true)
            }
            DivvupDivider()
            Column(Modifier.padding(top = ProfileMedidas.cartaoTopo), verticalArrangement = Arrangement.spacedBy(e.s2)) {
                DivvupLabel(stringResource(R.string.editar_nome))
                DivvupTextField(estado.nome, { aoMudarNome(it.take(NOME_MAXIMO)) }, teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
            }
            estado.email?.let {
                Column(Modifier.padding(top = e.s4), verticalArrangement = Arrangement.spacedBy(e.s2)) {
                    DivvupLabel(stringResource(R.string.editar_email))
                    Row(
                        Modifier.fillMaxWidth().height(e.campo).clip(DivvupTheme.shapes.sm).background(c.surfaceAlt).border(e.borda, c.border, DivvupTheme.shapes.sm).padding(horizontal = e.s4),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(it, style = t.body, color = c.textMuted, modifier = Modifier.weight(1f), maxLines = 1)
                        DivvupIcon(DivvupIcone.Cadeado, tamanho = TamanhoIcone.Pequeno.dp, tint = c.textMuted)
                    }
                    Text(stringResource(R.string.editar_email_nota), style = t.small, color = c.textMuted)
                }
            }
            if (estado.temSenha) {
                Column(Modifier.padding(top = ProfileMedidas.cartaoTopo)) {
                    DivvupDivider()
                    LinkSobre(DivvupIcone.Cadeado, stringResource(R.string.editar_senha), null, aoAlterarSenha)
                }
            }
            if (estado.erro) MensagemErro(stringResource(R.string.editar_erro))
        }
        DivvupDivider()
        DivvupButton(
            stringResource(R.string.editar_salvar),
            aoSalvar,
            larguraTotal = true,
            habilitado = estado.podeSalvar,
            carregando = estado.salvando,
            modifier = Modifier.padding(start = e.s4, end = e.s4, top = e.s3, bottom = ProfileMedidas.cartaoTopo),
        )
    }
}
