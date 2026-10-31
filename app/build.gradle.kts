plugins {
    alias(libs.plugins.android.application)
}

// CI sets versionCode from the GitHub run number, so every new build installs over the last.
val runNumber = providers.environmentVariable("GITHUB_RUN_NUMBER").orNull?.toIntOrNull()

// Release signing comes from environment variables that CI fills in:
//  - Play build (-Psigning=play): the Play upload key, decoded from GitHub secrets. Never committed.
//  - Sideload test build (default): the test key in this folder, unless SIDELOAD_* secrets override it.
val signingTarget = providers.gradleProperty("signing").orElse("sideload").get()
fun env(name: String) = providers.environmentVariable(name).orNull

android {
    namespace = "com.adamselite.ophelia"
    compileSdk = 37

    // The app is written in Java. Turning off AGP's built-in Kotlin skips Kotlin compilation,
    // as recommended for modules with no Kotlin code (developer.android.com/build/migrate-to-built-in-kotlin).
    enableKotlin = false

    defaultConfig {
        applicationId = "com.adamselite.ophelia"
        minSdk = 26          // Android 8.0: phones and tablets
        targetSdk = 36       // Google Play requirement since 2026-08-31
        versionCode = runNumber ?: 1
        versionName = "1.0.${runNumber ?: 0}"
    }

    signingConfigs {
        create("sideload") {
            val file = env("SIDELOAD_KEYSTORE_FILE")
            if (file != null) {
                storeFile = file(file)
                storePassword = env("SIDELOAD_KEYSTORE_PASSWORD")
                keyAlias = env("SIDELOAD_KEY_ALIAS")
                keyPassword = env("SIDELOAD_KEY_PASSWORD")
            } else {
                // Test-only key for sideloaded builds. It is not the Play upload key.
                storeFile = file("sideload-test.p12")
                storePassword = "ophelia-test"
                keyAlias = "ophelia-test"
                keyPassword = "ophelia-test"
            }
        }
        create("play") {
            val file = env("PLAY_UPLOAD_KEYSTORE_FILE")
            if (file != null) {
                storeFile = file(file)
                storePassword = env("PLAY_UPLOAD_KEYSTORE_PASSWORD")
                keyAlias = env("PLAY_UPLOAD_KEY_ALIAS")
                keyPassword = env("PLAY_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (signingTarget == "play") "play" else "sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // A call too new for minSdk fails the build instead of crashing on an older device.
        fatal += "NewApi"
        abortOnError = true
    }
}

// Deprecated Android or Java APIs fail the build, so warnings can't pile up unnoticed.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Werror"))
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core)
}
