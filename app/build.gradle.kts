import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.studybuddy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.studybuddy"
        minSdk = 23
        targetSdk = 36
        versionCode = 8
        versionName = "8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Firma de release: nunca versionada. Se lee de variables de entorno (CI) o de local.properties.
    //   STUDYBUDDY_KEYSTORE_FILE, STUDYBUDDY_KEYSTORE_PASSWORD, STUDYBUDDY_KEY_ALIAS, STUDYBUDDY_KEY_PASSWORD
    // Si faltan, la release se firma con la clave de debug estándar (solo para pruebas locales).
    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    fun signingValue(key: String): String? =
        System.getenv(key)?.takeIf { it.isNotBlank() } ?: localProps.getProperty(key)?.takeIf { it.isNotBlank() }

    val releaseKeystorePath = signingValue("STUDYBUDDY_KEYSTORE_FILE")
    val hasReleaseSigning = releaseKeystorePath != null && rootProject.file(releaseKeystorePath).exists()

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseKeystorePath!!)
                storePassword = signingValue("STUDYBUDDY_KEYSTORE_PASSWORD")
                keyAlias = signingValue("STUDYBUDDY_KEY_ALIAS")
                keyPassword = signingValue("STUDYBUDDY_KEY_PASSWORD")
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
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)
    implementation(libs.splashscreen)
    implementation(libs.coroutines.android)
    implementation(libs.work.runtime)
    implementation(libs.play.services.ads)

    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.process)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.lifecycle.runtime.testing)
}