plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.qrscanner12"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.qrscanner12"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // Standard CameraX
    val cameraxVersion = "1.6.0" // Use the stable 2026 release
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // The NEW Compose-specific artifact (Note the group change)
    implementation("androidx.camera:camera-compose:1.0.0-alpha01")

    // If you need the standalone viewfinder specifically:
    implementation("androidx.camera.viewfinder:viewfinder-compose:1.7.0-alpha01")

    // ML Kit & Google Safety
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.android.gms:play-services-safetynet:18.1.0")
    implementation("com.google.accompanist:accompanist-permissions:0.35.0-alpha")

    implementation("androidx.compose.material:material-icons-extended")



}