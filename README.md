# Erthiscan Android App

The official Android client for the **Erthiscan** ethical company scoring platform. This application allows users to scan barcodes (EAN-13) to identify parent companies, view crowdsourced ethical reports, and participate in the community-driven scoring system.

## Overview

The Erthiscan Android app is built with a focus on modern security, real-time performance, and a seamless user experience. It leverages Jetpack Compose for its UI and implements a robust API-driven architecture with secure, encrypted session management.

## Key Features

- **Live Barcode Scanning**: High-speed EAN-13 detection using CameraX and MLKit.
- **Ethical Intelligence**: Instant access to company scores and detailed reports directly from the scanner.
- **Community Participation**: Authenticated users can submit new reports, challenge existing ones, and vote on community claims.
- **Secure Authentication**: Seamless login via Google OAuth2 with stateful token rotation.
- **Deep Linking**: Integrated support for `pjdth.xyz/company/{id}` links to view company profiles directly.

## ⚠️ Important: Domain Dependencies

The project currently targets `pjdth.xyz`. If you point the app to a different backend, you **MUST** update the hardcoded domain references in the following locations, or the app will fail to connect or handle links:

1.  **SSL Pinning**: Update the `CERTIFICATE_PINNER` in `io.erthiscan.di.NetworkModule.kt` with the new domain and its valid SPKI hashes.
2.  **Deep Linking**: Update the host check in `io.erthiscan.MainActivity.kt` (`parseDeepLinkCompanyId` function).
3.  **App Links**: Update the `<data android:host="..." />` tags in `AndroidManifest.xml`.

## Technical Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) with Material 3.
- **Architecture**: MVVM (Model-View-ViewModel) with `StateFlow` and structured navigation.
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) (Dagger-based).
- **Networking**: [Retrofit](https://square.github.io/retrofit/) with [OkHttp](https://square.github.io/okhttp/).
- **Serialization**: [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization).
- **Async & Threading**: Kotlin Coroutines and Flow.
- **Camera & ML**: [CameraX](https://developer.android.com/training/camerax) and [MLKit Barcode Scanning](https://developers.google.com/ml-kit/vision/barcode-scanning).
- **Security**: [Google Tink](https://github.com/tink-crypto/tink-android) for AEAD encrypted storage and [OkHttp Certificate Pinning](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-certificate-pinner/).

## Security Architecture

Erthiscan implements multiple layers of security to protect user data and ensure communication integrity:

1.  **SSL Pinning**: The app uses a hard-coded `CertificatePinner` targeting the Let's Encrypt CA hierarchy (intermediates and roots) to prevent Man-in-the-Middle (MITM) attacks.
2.  **Encrypted DataStore**: Sensitive session tokens and user data are encrypted using **AES-256 GCM** via Google's Tink library before being persisted to `PreferencesDataStore`. The encryption keys are protected by the Android Keystore system.
3.  **Token Rotation**: Implements a secure `Authenticator` pattern in OkHttp. When an access token expires, the app performs a synchronous refresh call using a stateful refresh token, maintaining a secure session without user intervention.

## Project Structure

- `io.erthiscan`: Root package.
    - `api/`: Retrofit service definitions and Pydantic-style data models.
    - `auth/`: Session management and encrypted token orchestration.
    - `company/`: UI and ViewModels for the company directory and profiles.
    - `data/`: Repository implementations (Auth, Companies, Reports, Scan).
    - `di/`: Hilt modules for Network, Coroutines, and local dependencies.
    - `nav/`: Type-safe navigation routes and host configuration.
    - `scan/`: Real-time camera preview, MLKit analyzer, and scanner logic.
    - `ui/`: Design system, themes, and shared UI components.

## Getting Started

### Prerequisites

- Android Studio Ladybug (or newer)
- Android SDK 31+
- A `local.properties` file with the following keys:
    ```properties
    API_BASE_URL=https://api.pjdth.xyz/
    GOOGLE_WEB_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
    # Release signing (optional for dev)
    RELEASE_STORE_FILE=path/to/keystore
    RELEASE_STORE_PASSWORD=password
    RELEASE_KEY_ALIAS=alias
    RELEASE_KEY_PASSWORD=password
    ```

### Building

The project uses the **Built-in Kotlin support** (AGP 9.1+) and **Version Catalogs** (`libs.versions.toml`) for dependency management.

```bash
# Clean and build the debug APK
./gradlew clean assembleDebug
```

## Quality Control

- **Static Analysis**: `ruff` style checks (managed by the team) and `Android Lint`.
- **Testing**: JUnit 4 for unit tests and Compose Test for UI verification.
