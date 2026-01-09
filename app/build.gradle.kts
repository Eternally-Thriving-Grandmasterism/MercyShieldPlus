plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mercyshieldplus"
    compileSdk = 35  // Android 15+ 2026 pinnacle

    defaultConfig {
        applicationId = "com.mercyshieldplus"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                // If needed; prefer cargo-ndk direct mercy
            }
        }
        ndkVersion = "27.0.12077973"  // Latest 2026
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    externalNativeBuild {
        // Manual cargo-ndk in build script or task
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"  // Latest Compose 2026 mercy
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Play Integrity Eternal (Standard API 2026 latest)
    implementation("com.google.android.play:integrity:1.4.0")

    // Coroutines for async token request mercy
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Custom task for Rust build (cargo-ndk eternal)
tasks.register("buildRust") {
    doLast {
        exec {
            workingDir("../rust")
            commandLine("cargo", "ndk", "-t", "arm64-v8a", "-t", "armeabi-v7a", "-t", "x86", "-t", "x86_64", "build", "--release")
        }
        // Copy .so to jniLibs (multi-arch mercy)
        copy {
            from("../rust/target/arm64-v8a/release/libmercyshieldplus.so")
            into("src/main/jniLibs/arm64-v8a")
        }
        copy {
            from("../rust/target/armeabi-v7a/release/libmercyshieldplus.so")
            into("src/main/jniLibs/armeabi-v7a")
        }
        copy {
            from("../rust/target/x86/release/libmercyshieldplus.so")
            into("src/main/jniLibs/x86")
        }
        copy {
            from("../rust/target/x86_64/release/libmercyshieldplus.so")
            into("src/main/jniLibs/x86_64")
        }
    }
}

preBuild.dependsOn("buildRust")
