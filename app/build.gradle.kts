plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.johirxofficial.x"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.johirxofficial.x"
        minSdk = 21
        targetSdk = 35
        versionCode = 3
        versionName = "3.0.0"

        buildConfigField("String", "DEVELOPER", "\"johirxofficial\"")
        buildConfigField("String", "PROJECT_URL", "\"https://github.com/johirxofficial/x\"")

        // Primary API (changeable in-app). The public catalog is only used by the
        // "Public" preset in Settings, never as an automatic fallback.
        buildConfigField(
            "String",
            "DEFAULT_API_URL",
            "\"http://10.31.253.82/live/api/data.json\"",
        )
        buildConfigField(
            "String",
            "FALLBACK_API_URL",
            "\"https://raw.githubusercontent.com/johirxofficial/x/main/docs/api/categories.json\"",
        )

        resourceConfigurations += listOf("en", "bn")

        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // Unminified so sideloaded builds stay debuggable; see README for R8.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.ui)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
