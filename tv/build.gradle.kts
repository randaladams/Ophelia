plugins {
    alias(libs.plugins.android.application)
}

// The Fire TV build. It plays the same game page as the phone app (read from the phone
// module's assets, never copied), adds remote-control play (src/main/assets/tv.js), and
// is a separate app, so the phone build is untouched.

val runNumber = providers.environmentVariable("GITHUB_RUN_NUMBER").orNull?.toIntOrNull()
fun env(name: String) = providers.environmentVariable(name).orNull

android {
    namespace = "com.adamselite.ophelia.tv"
    compileSdk = 37

    // Java only, like the phone app (developer.android.com/build/migrate-to-built-in-kotlin).
    enableKotlin = false

    defaultConfig {
        applicationId = "com.adamselite.ophelia.tv"
        minSdk = 25          // Fire OS 6 (Android 7.1), e.g. Fire TV Stick 4K 1st Gen
        targetSdk = 36
        versionCode = runNumber ?: 1
        versionName = "1.0.${runNumber ?: 0}"
    }

    sourceSets {
        getByName("main") {
            assets.directories.add("../app/src/main/assets")
            res.directories.add("../app/src/main/res")
        }
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
                // The same test-only sideload key as the phone app's sideload builds.
                storeFile = file("../app/sideload-test.p12")
                storePassword = "ophelia-test"
                keyAlias = "ophelia-test"
                keyPassword = "ophelia-test"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    buildFeatures {
        buildConfig = true   // the version number shown in settings
    }

    compileOptions {
        // minSdk is below 26, so newer Java library APIs (java.time and others) are desugared.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // A call too new for minSdk fails the build instead of crashing on an older device.
        fatal += "NewApi"
        abortOnError = true
    }
}

// Deprecated Android or Java APIs fail the build.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Werror"))
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
