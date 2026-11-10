plugins {
    alias(libs.plugins.divvup.android.application)
    alias(libs.plugins.divvup.android.compose)
    alias(libs.plugins.divvup.hilt)
    alias(libs.plugins.kotlin.serialization)
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
val urlBackendProd = providers.gradleProperty("divvup.backend.url").getOrElse("")

fun texto(valor: String) = "\"" + valor.replace("\"", "\\\"") + "\""

android {
    // Invariante: nunca alterar (CLAUDE.md).
    namespace = "br.app.divvup"

    buildFeatures {
        buildConfig = true
    }

    productFlavors {
        getByName("dev") {
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
        versionCode = 1
        versionName = "0.1.0"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
