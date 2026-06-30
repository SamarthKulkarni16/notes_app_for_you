import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

// Load local.properties (gitignored) so SUPABASE_URL etc. are available via
// project.findProperty() during local builds. CI builds pass these in
// directly as -P gradle properties instead, so this file is optional there.
val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        load(FileInputStream(localFile))
    }
}
fun localOrProjectProperty(key: String): String =
    (localProperties.getProperty(key) ?: project.findProperty(key) as String?) ?: ""

android {
    namespace = "com.samarth.notesapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.samarth.notesapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // Supabase config is injected at build time via -P properties (see CI workflow)
        // and gradle.properties for local builds, so no secrets are hardcoded in source.
        buildConfigField(
            "String", "SUPABASE_URL",
            "\"${localOrProjectProperty("SUPABASE_URL")}\""
        )
        buildConfigField(
            "String", "SUPABASE_ANON_KEY",
            "\"${localOrProjectProperty("SUPABASE_ANON_KEY")}\""
        )
        buildConfigField(
            "String", "GOOGLE_WEB_CLIENT_ID",
            "\"${localOrProjectProperty("GOOGLE_WEB_CLIENT_ID")}\""
        )
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Core
    implementation("androidx.core:core-ktx:1.13.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Supabase-Kt (BOM keeps Auth/Postgrest/Ktor engine versions aligned)
    implementation(platform("io.github.jan-tennert.supabase:bom:3.6.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.ktor:ktor-client-okhttp:3.3.0")

    // Google Sign-In via Credential Manager (modern, no deprecated GoogleSignInClient)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // DataStore for local session/draft persistence
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Room — local sync ledger (NOT a content cache; tracks which .md files
    // have been pushed to Supabase yet). Pinned to stable 2.x; Room 3.0 is
    // KMP-focused and still alpha as of writing, so deliberately not used.
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Unit testing
    testImplementation("junit:junit:4.13.2")
}
