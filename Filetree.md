MercyShieldPlus/
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/mercy/shieldplus/
│   │   │   ├── ui/ (Compose screens: MainScreen, Presplash, Ledger)
│   │   │   ├── viewmodel/ (ShieldViewModel: LiveData/Flow from Rust)
│   │   │   ├── MainActivity.kt (uniFFI init, Compose setContent)
│   │   │   └── di/ (Hilt/Koin if scaled)
│   │   ├── res/ (drawables, themes Material You)
│   │   └── AndroidManifest.xml (permissions: INTERNET minimal, foreground service)
│   └── build.gradle.kts (Rust plugin, cargo-ndk integration)
├── rust/
│   ├── Cargo.toml
│   ├── build.rs (uniFFI scaffold)
│   └── src/
│       ├── lib.rs (uniFFI exports: PQ ops, integrity chain)
│       ├── pq_shield.rs (ML-KEM/ML-DSA core)
│       └── integrity.rs (Play Integrity + custom checks)
├── assets/ (presplash.jpg, quantum icons)
├── docs/ (Structure.md, Animations.md, BestPractices.md — migrated notes)
├── build.gradle.kts (top-level)
├── settings.gradle.kts
├── gradlew/...
└── README.md (updated vision)
