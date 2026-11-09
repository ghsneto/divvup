// Kotlin puro: toda regra de cálculo mora aqui (CLAUDE.md). Sem Android nem Firebase.
plugins {
    alias(libs.plugins.divvup.jvm.library)
}

dependencies {
    api(project(":core:model"))
}

// Guarda do invariante: falha o `check` se Android ou Firebase entrarem no classpath do domínio.
val verificarDominioPuro = tasks.register("verificarDominioPuro") {
    description = "Garante que :core:domain não depende de Android nem de Firebase."
    group = "verification"
    val proibidos = listOf("androidx.", "com.android.", "com.google.firebase", "com.google.android")
    val artefatos = configurations.named("runtimeClasspath").map { cfg ->
        cfg.incoming.resolutionResult.allComponents.map { it.id.displayName }
    }
    inputs.property("artefatos", artefatos)
    doLast {
        val violacoes = artefatos.get().filter { id -> proibidos.any { id.startsWith(it) } }
        check(violacoes.isEmpty()) {
            ":core:domain precisa ser Kotlin puro, mas depende de: ${violacoes.joinToString()}"
        }
    }
}

tasks.named("check") { dependsOn(verificarDominioPuro) }
