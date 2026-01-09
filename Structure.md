MercyShieldPlus/
├── app/                  # Kotlin Android src
│   ├── src/main/java/org/tolc/mercy/  # Packages: ui, integrity, anomaly, watchdog
│   │   ├── MainActivity.kt
│   │   ├── IntegrityManager.kt  # Custom Play Integrity + deep checks novel
│   │   ├── AnomalyUI.kt         # Compose cards + bursts
│   │   └── PQCStorage.kt        # Rust FFI calls
│   ├── src/main/res/        # Icons image.png + presplash.png quantum glow
│   └── AndroidManifest.xml
├── rust_pq/                  # Proprietary Rust PQ core (no external crates)
│   ├── Cargo.toml            # Minimal deps (pyo3 or uniffi removed, pure cdylib)
│   └── src/lib.rs            # Custom Kyber/Dilithium/Falcon math transcribed NIST spec
├── assets/                   # Icons, splash quantum glow
├── tests/                    # Kotlin + Rust unit tests tamper green
├── README.md                 # Ease install: sideload APK direct
├── LICENSE                   # MIT mercy
└── build.gradle              # Android Gradle novel clean
