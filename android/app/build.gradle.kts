import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Schematy Room trafiają do repozytorium — z nich powstają przyszłe migracje.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.kropi.hydration"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kropi.hydration"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "1.10.0"
    }

    // Szpila czyta dane Kropi przez uprawnienie na poziomie podpisu, więc Kropi
    // MUSI być podpisane tym samym kluczem co Szpila. Klucz podaje
    // android/keystore.properties (poza gitem: storeFile, storePassword,
    // keyAlias, keyPassword). Bez niego build wraca do klucza debug —
    // instaluje się, ale Szpila nie dostanie wtedy żadnych danych.
    val keystoreProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    val sharedSigning = if (keystoreProps.isEmpty) null else signingConfigs.create("shared") {
        storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
        storePassword = keystoreProps.getProperty("storePassword")
        keyAlias = keystoreProps.getProperty("keyAlias")
        keyPassword = keystoreProps.getProperty("keyPassword")
    }

    buildTypes {
        debug {
            sharedSigning?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = false
            signingConfig = sharedSigning ?: signingConfigs.getByName("debug")
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
    }

    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src/main/kotlin")
        }
        getByName("test") {
            kotlin.srcDirs("src/test/kotlin")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.health.connect:connect-client:1.1.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    // Na JVM org.json z Androida to atrapa — testy formatu kopii i parsera wydań potrzebują prawdziwego.
    testImplementation("org.json:json:20240303")
}
