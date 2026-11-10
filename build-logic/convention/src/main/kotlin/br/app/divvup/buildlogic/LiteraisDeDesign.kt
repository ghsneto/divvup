package br.app.divvup.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Guarda do CLAUDE.md "nada de valores literais nas telas": fora de `:core:designsystem`,
 * `Color(0x…)` é proibido e `.dp`/`.sp` só podem aparecer em arquivos `*Medidas.kt`
 * (medidas nomeadas tiradas do desenho).
 */
abstract class VerificarLiteraisDeDesign : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val fontes: ConfigurableFileCollection

    @TaskAction
    fun verificar() {
        val violacoes = mutableListOf<String>()
        fontes.asFileTree.matching { include("**/*.kt") }.forEach { arquivo ->
            val medidasPermitidas = arquivo.name.endsWith("Medidas.kt")
            arquivo.readLines().forEachIndexed { i, linha ->
                val codigo = linha.substringBefore("//").trim()
                if (codigo.startsWith("import ") || codigo.startsWith("*") || codigo.startsWith("/*")) return@forEachIndexed
                if (COR.containsMatchIn(codigo)) {
                    violacoes += "${arquivo.path}:${i + 1}: cor literal; use DivvupTheme.colors"
                }
                if (!medidasPermitidas && MEDIDA.containsMatchIn(codigo)) {
                    violacoes += "${arquivo.path}:${i + 1}: .dp/.sp fora de *Medidas.kt; use DivvupTheme ou Medidas"
                }
            }
        }
        if (violacoes.isNotEmpty()) {
            throw GradleException("Valores literais de design encontrados:\n" + violacoes.joinToString("\n"))
        }
    }

    private companion object {
        val COR = Regex("""\bColor\s*\(\s*0[xX]""")
        /** Número ou expressão entre parênteses seguidos de `.dp`/`.sp` (ex.: `16.dp`, `(-4).dp`). */
        val MEDIDA = Regex("""(\d|\))\.(dp|sp)\b""")
    }
}

/** Registra a guarda e liga ao `check` em módulos de UI (exceto o design system e os testes). */
internal fun Project.configurarGuardaDeLiterais() {
    if (path == ":core:designsystem" || path == ":core:testing") return
    val tarefa = tasks.register("verificarLiteraisDeDesign", VerificarLiteraisDeDesign::class.java) {
        description = "Barra cores e medidas literais fora do design system."
        group = "verification"
        fontes.from(layout.projectDirectory.dir("src/main/kotlin"), layout.projectDirectory.dir("src/main/java"))
    }
    tasks.named("check").configure { dependsOn(tarefa) }
}
