// Top-level build file
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.0")  // 2026 latest stable
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
        // Rust integration if plugin needed; cargo-ndk manual
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
