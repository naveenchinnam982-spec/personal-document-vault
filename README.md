# SecureVault

SecureVault is an offline-first, military-grade Android digital locker and privacy vault built with Jetpack Compose and Kotlin. It provides secure storage for sensitive personal documents, official certificates, government IDs, and private multimedia with hardware-backed AES-256-GCM encryption, granular multi-vault isolation, and discreet stealth access.

---

## Overview

Modern mobile users carry sensitive personal information—passports, tax filings, driver's licenses, insurance policies, and private photographs. SecureVault guarantees absolute privacy by enforcing zero-knowledge architecture: all files are encrypted locally on the device using AES-256-GCM with keys secured inside the Android KeyStore, ensuring that no unencrypted data ever touches external cloud providers without explicit user consent.

---

## Features

### 1. Multi-Tier Vault Isolation
- **Main Vault**: Dedicated storage for identity cards, contracts, financial records, medical documents, and certificates with categorization, tags, and expiry tracking.
- **Private Photo & Media Vault**: Encrypted gallery supporting photo and video import, custom private albums, interactive pinch-to-zoom viewing, and secure metadata scrubbing.
- **Hidden Vault (Dual-Layer Stealth)**: An ultra-secure secondary partition isolated by a separate PIN, discreet gesture unlock, and calculator disguised access.

### 2. High-Grade Security Architecture
- **Hardware-Backed Cryptography**: AES-256-GCM encryption using keys stored in the Android KeyStore with fallback support for virtual environments.
- **Biometric Authentication**: Fingerprint and face recognition integration via AndroidX BiometricPrompt with graceful fallback for devices lacking biometric hardware.
- **Auto-Lock & Threat Protection**: Configurable session timeouts (immediate, 30s, 1m, 5m), brute-force attempt lockout penalties, and optional screenshot prevention (`FLAG_SECURE`).
- **Encrypted Thumbnail Pipeline**: On-the-fly encrypted thumbnail generation with heap-proportional LRU caching to eliminate plain-text disk leaks.

### 3. Integrated Utilities
- **Document Scanner**: Built-in document camera scanner with edge detection, contrast enhancement, black-and-white mode, and multi-page capture.
- **Interactive PDF Viewer**: Native high-fidelity PDF rendering via Android `PdfRenderer` with multi-page navigation, zooming, and caching.
- **Optical Character Recognition (OCR)**: On-device text extraction for instant full-text search across scanned documents.
- **Storage Analytics**: Real-time breakdown of encrypted storage consumption across document types, media, and vault partitions.
- **Backup & Encrypted Export**: Export password-protected encrypted archives for device transfers.

---

## Technology Stack

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern + Clean Architecture
- **Local Persistence**: Room Database (SQLite with KSP code generation)
- **Concurrency**: Kotlin Coroutines & StateFlow / SharedFlow
- **Cryptography**: Android KeyStore, AES-256-GCM, SHA-256 Salted Hashes
- **Biometrics**: AndroidX Biometric 1.2.0
- **Image & PDF Processing**: Coil 2.7.0, Android Native `PdfRenderer`
- **Testing**: Robolectric, JUnit 4, AndroidX Test Core

---

## Architecture

```
app/
 ├── data/
 │    ├── export/          # Encrypted file export, download, and URI share handlers
 │    ├── local/           # Room entities, DAOs, and database configuration
 │    ├── model/           # Domain models, enum definitions, and UI state models
 │    ├── pdf/             # Native PdfRenderer wrapper and page bitmap cache
 │    ├── repository/      # VaultRepository coordinating crypto, Room, and I/O
 │    └── security/        # CryptoManager (AES-256-GCM), AuthManager, ThumbnailManager
 ├── ui/
 │    ├── components/      # Reusable Compose components (PinPad, Dialogs, PDF Viewer)
 │    ├── screens/
 │    │    ├── documents/  # Main document library, detail viewer, and creation screens
 │    │    ├── hiddenvault/# Secondary stealth vault, PIN validation, and media viewer
 │    │    ├── lock/       # Main and private vault PIN/Biometric lock screens
 │    │    ├── privatevault/# Encrypted photo/video vault and album manager
 │    │    ├── scanner/    # Document scanner camera and image enhancement
 │    │    └── settings/   # Security logs, auto-lock timeouts, storage analytics
 │    ├── theme/           # Color palettes, M3 Typography, and Theme definitions
 │    └── viewmodel/       # VaultViewModel orchestrating reactive state & navigation
 └── MainActivity.kt       # Single-Activity host managing state-driven Compose routing
```

---

## Requirements

- **Android Studio**: Ladybug (2024.2.1) or newer
- **Gradle**: 8.7+
- **Android Gradle Plugin**: 8.6.0+
- **JDK**: Java 17 / 21
- **Minimum SDK**: API 26 (Android 8.0 Oreo)
- **Target SDK**: API 35 (Android 15)
- **Compile SDK**: API 35

---

## Setup Instructions

1. Clone or download the project repository to your local development machine:
   ```bash
   git clone <repository-url>
   cd securevault
   ```
2. Open Android Studio and select **Open**, navigating to the project root directory.
3. Allow Gradle to download dependencies and sync the project automatically.

---

## Configuration

The application is fully functional offline by default and does not require external cloud keys for its core vault operations.
Environment-specific variables can be configured via `.env` (refer to `.env.example`).

---

## API/Firebase Setup

- **Local-First Architecture**: The application operates with zero external server dependencies out-of-the-box.
- **Optional Firebase Sync**: If remote encrypted backup via Firebase Cloud Storage is desired:
  1. Register the application in the Firebase Console with package name `com.example`.
  2. Download `google-services.json` and place it in the `/app` directory.
  3. Uncomment the `com.google.gms.google-services` plugin and Firebase dependencies in `app/build.gradle.kts`.

---

## How to Build

To compile the debug APK via the command line:

```bash
gradle assembleDebug
```

The resulting debug APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

To compile a release bundle (requires signing configuration):
```bash
gradle bundleRelease
```

---

## How to Run

1. Connect an Android device with USB Debugging enabled, or launch an Android Virtual Device (AVD).
2. Install and launch the application directly:
   ```bash
   gradle installDebug
   ```
3. On first launch, the app prompts you to establish a Master PIN to initialize encryption keys in the Android KeyStore.

---

## How to Test

Run local JVM and Robolectric unit tests:

```bash
gradle :app:testDebugUnitTest
```

All 32 actionable build tasks and test suites execute without requiring a physical device or emulator.

---

## Permissions

Declared in `AndroidManifest.xml`:

| Permission | Purpose | Runtime Requirement |
| :--- | :--- | :--- |
| `CAMERA` | Document scanning and camera photo capture | Requested dynamically on first scanner use (`required="false"`) |
| `USE_BIOMETRIC` | Fingerprint and face biometric unlock | Checked safely; app works with PIN if unavailable |
| `VIBRATE` | Haptic feedback on PIN keypad interaction | Install-time permission |
| `INTERNET` | Optional secure cloud backup (disabled by default) | Install-time permission |

---

## Troubleshooting

1. **App fails to start or shows a blank screen on emulator**:
   - Verify that `FLAG_SECURE` is not blocking virtual video streams. SecureVault automatically disables `FLAG_SECURE` in development/debug modes for emulator compatibility.
2. **Biometric option not shown**:
   - The device or emulator may not have enrolled fingerprints or biometric hardware. SecureVault falls back to Master PIN authentication automatically.
3. **Database schema modification**:
   - If Room entity models are modified during development, `fallbackToDestructiveMigration(true)` is pre-configured in `AppDatabase` to prevent database schema mismatch crashes.

---

## Future Improvements

- Cloud-synchronized end-to-end encrypted backup (E2EE) with user-managed passphrase.
- Hardware security key (YubiKey / FIDO2) NFC authentication support.
- Multi-language localization (i18n) for international document categories.
- Automatic PDF redaction tool for sensitive ID numbers.
