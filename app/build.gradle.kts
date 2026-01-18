plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android) // Plugin de Hilt
    alias(libs.plugins.google.services) // Plugin de Firebase
    id("kotlin-kapt") // Necesario para que Hilt genere codigo
}

android {
    namespace = "edu.ucam.reservashack"
    compileSdk = 34 // Usamos una version estable

    defaultConfig {
        applicationId = "edu.ucam.reservashack"
        minSdk = 26 // Recomendado subir a 26 para evitar problemas de seguridad con cripto
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Habilitar minificación para reducir tamaño
            isShrinkResources = true // Eliminar recursos no usados
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            versionNameSuffix = "-DEBUG" // Identifica versión debug fácilmente
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8" // Compatible con Kotlin 1.9.22
    }
}

dependencies {
    // 1. Core Android & Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended") // Iconos extra
    implementation("androidx.compose.foundation:foundation") // Para HorizontalPager

    // 2. Navegación
    implementation(libs.androidx.navigation.compose)

    // 3. Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation("org.jsoup:jsoup:1.17.2")

    // 4. Inyección de Dependencias (Hilt)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler) // IMPORTANTE: Usamos kapt aqui
    implementation(libs.hilt.navigation.compose)

    // 5. Seguridad
    implementation(libs.security.crypto)

    // 6. WebView
    implementation(libs.accompanist.webview)
    implementation("com.google.accompanist:accompanist-swiperefresh:0.34.0")

    // 7. Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation("com.google.firebase:firebase-auth-ktx") // Auth
}