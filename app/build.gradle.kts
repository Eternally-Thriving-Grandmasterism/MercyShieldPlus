plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
}

android {
    namespace = "com.mercyshieldplus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mercyshieldplus"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
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
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    implementation("com.google.android.play:integrity:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Room + SQLCipher + Security Crypto + Biometric
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.security:security-crypto-ktx:1.1.0")
    implementation("androidx.biometric:biometric:1.2.0")

    implementation("com.google.code.gson:gson:2.10.1")
}

// Full Rust build with uniFFI bindings copy mercy
tasks.register("buildRustRelease") {
    dependsOn("generateUniffiBindings")  // Ensure scaffolding first
    doLast {
        exec {
            workingDir("../rust")
            commandLine("cargo", "ndk", "-t", "armeabi-v7a", "-t", "arm64-v8a", "-t", "x86", "-t", "x86_64", "build", "--release")
        }
        // Copy .so multi-arch
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

// Generate uniFFI Kotlin bindings (run cargo build first to create)
tasks.register("generateUniffiBindings") {
    doLast {
        exec {
            workingDir("../rust")
            commandLine("cargo", "build", "--release")  // Triggers build.rs scaffolding
        }
        // Copy generated Kotlin to src/main/kotlin/com/mercyshieldplus
        copy {
            from("../rust/target/uniffi-bindings")
            into("src/main/kotlin/com/mercyshieldplus")
            include("*.kt")
        }
    }
}

preBuild.dependsOn("buildRustRelease")
