import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Release signing, from `keystore.properties` at the repo root (local, gitignored) or the
 * NIF_KEYSTORE_* environment variables (CI). Every release must use the same key, or installed
 * copies refuse to update in place. See README › Releases.
 */
val releaseSigning: Map<String, String>? = run {
    val file = rootProject.file("keystore.properties")
    val props = Properties().apply { if (file.exists()) file.inputStream().use(::load) }
    fun value(key: String, env: String) = props.getProperty(key) ?: providers.environmentVariable(env).orNull
    mapOf(
        "storeFile" to value("storeFile", "NIF_KEYSTORE_FILE"),
        "storePassword" to value("storePassword", "NIF_KEYSTORE_PASSWORD"),
        "keyAlias" to value("keyAlias", "NIF_KEY_ALIAS"),
        "keyPassword" to value("keyPassword", "NIF_KEY_PASSWORD"),
    ).takeIf { m -> m.values.all { !it.isNullOrBlank() } }?.mapValues { it.value!! }
}

android {
    namespace = "io.github.hassanm0301.nothingisfree"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.hassanm0301.nothingisfree"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 3
        versionName = "0.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = file(releaseSigning.getValue("storeFile"))
                storePassword = releaseSigning.getValue("storePassword")
                keyAlias = releaseSigning.getValue("keyAlias")
                keyPassword = releaseSigning.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Without a release key the APK is left unsigned rather than signed with a throwaway one.
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    lint {
        abortOnError = true
        // Adaptive icons must live in mipmap-anydpi-v26 regardless of minSdk; targetSdk is
        // raised deliberately, one release behind compileSdk.
        disable += setOf("ObsoleteSdkInt", "OldTargetApi")
    }
}

dependencies {
    // Glyph Matrix SDK (glyph-matrix-sdk-*.aar from Nothing's GlyphMatrix-Developer-Kit). See docs/GLYPH.md.
    implementation(fileTree("libs") { include("*.aar") })

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
