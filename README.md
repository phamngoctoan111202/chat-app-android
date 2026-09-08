# ChatApp Android (Signal E2EE Client)

A modern, multi-module Android application written in Kotlin and Jetpack Compose, featuring End-to-End Encryption (E2EE) powered by Signal Protocol standards, connecting to the Go E2EE Backend Server.

## 🏛️ Multi-Module Architecture

The repository is structured into 5 isolated Gradle modules:

```
chat-app-android/
├── app/                  # Main UI layer (Jetpack Compose, AuthScreen, ChatScreen, SecureSessionManager)
├── core/
│   ├── crypto/           # Signal E2EE Engine (EC 256-bit KeyGen, ECDH Shared Secret, AES-GCM 256-bit, KeyStore)
│   ├── network/          # Network layer (Retrofit 2, OkHttp AuthInterceptor X-Request-ID/JWT, DTOs, safeApiCall)
│   ├── database/         # Local Storage (Room Database, SQLCipher support, MessageDao, ConversationDao)
│   └── ads/              # Base Ads (AdConfig Unit IDs, BannerAdView Composable)
├── gradle/libs.versions.toml # Gradle Version Catalog (AGP 8.3.2, Kotlin 1.9.22, Compose 2024.02.02, Room 2.6.1)
└── settings.gradle.kts   # Root project configuration
```

## 🔒 Security & Cryptography Features
- **Signal Protocol Prekey Exchange:** Automatic generation of Identity Key Pairs, Signed PreKeys, and One-Time PreKeys synchronized with the backend via `PUT /api/v1/keys`.
- **ECDH Shared Secret Derivation:** Asymmetric Diffie-Hellman key agreement for calculating 256-bit shared master keys between devices.
- **AES-GCM 256-bit Encryption:** Symmetric authenticated encryption with 128-bit tag length and 12-byte IVs for all message payloads.
- **Hardware-backed Storage:** Hardware KeyStore protection for local encryption keys and encrypted session storage.

## 🚀 Building & Running

### Requirements
- Android SDK 34 (Android 14)
- JDK 17
- Gradle 8.7

### Build Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Unit Tests Across All Modules
```bash
./gradlew test
```

## 📄 License
Internal / Proprietary Project
