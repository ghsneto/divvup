plugins {
    alias(libs.plugins.divvup.android.application)
    alias(libs.plugins.divvup.android.compose)
    alias(libs.plugins.divvup.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.firebase.crashlytics)
}

// Configuração do Firebase por flavor, sem o plugin google-services (ADR 0002).
// prod: lida de app/google-services.json (fora do git; na CI vem do segredo GOOGLE_SERVICES_JSON_PROD).
// dev: Firebase Emulator com o projeto fictício demo-divvup; host ajustável por
// -Pdivvup.emulador.host=<ip do computador> para aparelho físico.
val googleServices: Map<String, String> = run {
    val arquivo = file("google-services.json")
    if (!arquivo.exists()) return@run emptyMap()
    @Suppress("UNCHECKED_CAST")
    val raiz = groovy.json.JsonSlurper().parse(arquivo) as Map<String, Any?>
    val projeto = raiz["project_info"] as Map<String, Any?>
    val cliente = (raiz["client"] as List<Map<String, Any?>>).first { c ->
        ((c["client_info"] as Map<String, Any?>)["android_client_info"] as Map<String, Any?>)["package_name"] == "br.app.divvup"
    }
    // Cliente OAuth "web" (tipo 3): o serverClientId do login com Google (Credential Manager).
    val clienteWeb = (cliente["oauth_client"] as? List<Map<String, Any?>>).orEmpty()
        .firstOrNull { it["client_type"].toString().startsWith("3") }?.get("client_id")?.toString()
    mapOf(
        "googleWebClientId" to (clienteWeb ?: ""),
        "projectId" to projeto["project_id"].toString(),
        "gcmSenderId" to projeto["project_number"].toString(),
        "applicationId" to (cliente["client_info"] as Map<String, Any?>)["mobilesdk_app_id"].toString(),
        "apiKey" to ((cliente["api_key"] as List<Map<String, Any?>>).first()["current_key"]).toString(),
    )
}
val hostEmulador = providers.gradleProperty("divvup.emulador.host").getOrElse("10.0.2.2")
// Backend publicado (issue #41). Pode ser trocado com -Pdivvup.backend.url=...
val urlBackendProd = providers.gradleProperty("divvup.backend.url").getOrElse("https://api.divvup.app.br")

fun texto(valor: String) = "\"" + valor.replace("\"", "\\\"") + "\""

android {
    // Invariante: nunca alterar (CLAUDE.md).
    namespace = "br.app.divvup"

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    productFlavors {
        getByName("dev") {
            // Crashlytics e Analytics existem no dev, mas não coletam: o projeto é fictício (ADR 0002).
            manifestPlaceholders["coletar"] = "false"
            buildConfigField("String", "FIREBASE_PROJECT_ID", texto("demo-divvup"))
            buildConfigField("String", "FIREBASE_API_KEY", texto("chave-falsa-do-emulador"))
            buildConfigField("String", "FIREBASE_APP_ID", texto("1:000000000000:android:0000000000000000"))
            buildConfigField("String", "FIREBASE_SENDER_ID", texto("000000000000"))
            buildConfigField("String", "FIREBASE_EMULADOR_HOST", texto(hostEmulador))
            buildConfigField("String", "URL_BACKEND", texto("http://$hostEmulador:8787"))
            // O Auth Emulator aceita qualquer conta Google; o ID web vem do google-services.json, se houver.
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", texto(googleServices["googleWebClientId"] ?: ""))
        }
        getByName("prod") {
            manifestPlaceholders["coletar"] = "true"
            // Crashlytics lê o App ID deste recurso para enviar o mapeamento do R8.
            resValue("string", "google_app_id", googleServices["applicationId"] ?: "")
            buildConfigField("String", "FIREBASE_PROJECT_ID", texto(googleServices["projectId"] ?: "divvup-prod"))
            buildConfigField("String", "FIREBASE_API_KEY", texto(googleServices["apiKey"] ?: ""))
            buildConfigField("String", "FIREBASE_APP_ID", texto(googleServices["applicationId"] ?: ""))
            buildConfigField("String", "FIREBASE_SENDER_ID", texto(googleServices["gcmSenderId"] ?: ""))
            buildConfigField("String", "FIREBASE_EMULADOR_HOST", texto(""))
            buildConfigField("String", "URL_BACKEND", texto(urlBackendProd))
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", texto(googleServices["googleWebClientId"] ?: ""))
        }
    }

    defaultConfig {
        applicationId = "br.app.divvup"
        // A CI pode subir o código de versão a cada envio (-Pdivvup.versionCode=N).
        versionCode = providers.gradleProperty("divvup.versionCode").map(String::toInt).getOrElse(1)
        versionName = "1.0.0"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // Assinatura de release só na CI: keystore e senhas vêm de segredos (nada no repositório).
    val keystore = providers.environmentVariable("DIVVUP_KEYSTORE_PATH").orNull
    signingConfigs {
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = providers.environmentVariable("DIVVUP_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("DIVVUP_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("DIVVUP_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystore != null) signingConfig = signingConfigs.getByName("release")
            // Sem google-services.json (build local) não há para onde enviar o mapeamento.
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = googleServices.isNotEmpty()
            }
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))

    implementation(project(":feature:auth"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:house"))
    implementation(project(":feature:deck"))
    implementation(project(":feature:tasks"))
    implementation(project(":feature:ranking"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:discover"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.serialization.core)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.hilt.android.testing)
    testImplementation(project(":core:testing"))
    kspTest(libs.hilt.compiler)
}
