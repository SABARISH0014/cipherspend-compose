# 🛡️ CipherSpend

![Flutter](https://img.shields.io/badge/Flutter-02569B?style=for-the-badge&logo=flutter&logoColor=white)
![Dart](https://img.shields.io/badge/Dart-0175C2?style=for-the-badge&logo=dart&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Security](https://img.shields.io/badge/Security-Bank_Grade-green?style=for-the-badge)

**CipherSpend** is a 100% offline, privacy-first personal finance tracker and intelligent SMS parser. Designed with a clean, modern, and user-friendly interface, it acts as a "Hybrid Vault," ensuring your financial data never leaves your device while providing automated expense tracking through advanced NLP parsing.

Built exclusively for Android using **Flutter**, secured by **SQLCipher**, and guarded by a robust **TOTP + MPIN** authentication flow.

---

## 📱 App Previews

| Setup & Security | Main Dashboard | Interactive Training |
|:---:|:---:|:---:|
| ![Setup 1](screenshots/setup_screenshot_1.jpg)<br>![Setup 2](screenshots/setup_screenshot_2.jpg) | ![Dashboard](screenshots/dashboard_screenshot.jpg) | ![Training](screenshots/training_screenshot.jpg) |
| *Bank-grade setup with TOTP* | *Clean, user-friendly Light Theme* | *Custom NER regex extraction* |

---

## ✨ Features

### 🔒 Core Security & Privacy
- **100% Offline Architecture** — No cloud databases, no APIs, and absolutely zero telemetry. Your data is strictly device-bound.
- **Hybrid Vault Authentication** — Employs a secure 4-digit MPIN for daily access, guarded by a mandatory TOTP (Google Authenticator) requirement for high-risk actions.
- **SQLCipher Database Encryption** — All transactions and rules are stored in a fully encrypted, military-grade local SQLite database.
- **Background Protection** — UI blurring and background state management to prevent shoulder-surfing and unauthorized access.

### 🧠 Intelligent Expense Tracking
- **Automated SMS Parsing** — Runs locally to intercept and parse bank transaction SMS messages in real-time using native Android background receivers.
- **Interactive Regex Training** — A custom Named Entity Recognition (NER) labeling interface. Users can select words in an SMS to dynamically generate custom regex rules for identifying 'Merchants' and 'Amounts'.
- **Smart Categorization** — Built-in local prediction service to categorize expenses without cloud reliance.

### 🎨 UI/UX Excellence
- **Modern Light Theme** — A calming, trustworthy aesthetic designed to feel safe and professional, akin to modern banking or wellness apps.
- **Glassmorphism Elements** — Beautiful blurred backdrop filters (`BackdropFilter`) to draw focus to critical actions like editing merchants or adjusting settings.
- **Frictionless Navigation** — Cleanly organized dashboard with intuitive visual reports, search capabilities, and minimal clutter.

---

## 🛠 Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Framework** | Flutter (Dart) | Cross-platform UI framework |
| **Database** | `sqflite_sqlcipher` | Encrypted local data persistence |
| **Security** | `totp_authenticator` | Time-based One-Time Password generation |
| **Storage** | `flutter_secure_storage` | Secure KeyStore/Keychain management for keys |
| **Native Integration**| Kotlin (Android) | Background SMS receivers and services |
| **Testing** | `integration_test` | End-to-end user flow testing |

---

## 🏗 Architecture

CipherSpend utilizes a strict, offline-first service architecture to isolate UI from data processing and security layers:

```text
Device Hardware (SMS Inbox / KeyStore)
    │
    ▼
┌──────────────────────────────────────────┐
│  Native Layer (Android/Kotlin)           │
│  - SmsBackgroundReceiver.kt              │
│  - NotificationService.kt                │
└──────────────────────────────────────────┘
    │
    ▼ (Method Channels / Bridge)
┌──────────────────────────────────────────┐
│  Flutter Services Layer                  │
│  ├─ SmsBridge / SmsService               │
│  ├─ ParserService (Regex extraction)     │
│  ├─ TrainingService (Rule generation)    │
│  ├─ VaultService (MPIN/TOTP handling)    │
│  └─ DbService (SQLCipher Interface)      │
└──────────────────────────────────────────┘
    │
    ▼ (State Management)
┌──────────────────────────────────────────┐
│  Flutter UI Layer                        │
│  - Screens (Dashboard, Settings, Setup)  │
│  - Widgets (HighRiskGuard, Shields)      │
└──────────────────────────────────────────┘
```

---

## 📁 Project Structure

```text
cipherspend-v1/
│
├── android/                         # Native Android Code
│   └── app/src/main/
│       ├── kotlin/com/example/cipherspend/
│       │   ├── MainActivity.kt
│       │   ├── NotificationService.kt
│       │   └── SmsBackgroundReceiver.kt
│       └── AndroidManifest.xml      # SMS and Background Permissions
│
├── assets/                          # Local Assets
│   └── labels.json                  # Fallback offline ML/categorization labels
│
├── integration_test/                # E2E Tests
│   └── app_flow_test.dart
│
└── lib/                             # Core Flutter Codebase
    ├── main.dart                    # Entry point & global themes
    │
    ├── models/                      # Data Models
    │   └── transaction_model.dart
    │
    ├── screens/                     # UI Views
    │   ├── dashboard_screen.dart    # Main financial overview
    │   ├── interactive_training_screen.dart # Regex NLP builder
    │   ├── mpin_setup_screen.dart   # Security initialization
    │   ├── settings_screen.dart     # App configurations
    │   └── transaction_detail_screen.dart 
    │
    ├── services/                    # Business Logic
    │   ├── ai_service.dart          # Local data categorization
    │   ├── db_service.dart          # SQLCipher operations
    │   ├── parser_service.dart      # SMS text extraction
    │   ├── sms_bridge.dart          # Native communication
    │   └── vault_service.dart       # TOTP & KeyStore security
    │
    ├── utils/                       # Shared Utilities
    │   └── constants.dart
    │
    └── widgets/                     # Reusable UI Components
        ├── high_risk_guard.dart     # TOTP interceptor widget
        ├── offline_shield.dart      # UI state protector
        └── restricted_settings_dialog.dart
```

---

## 🚀 Getting Started

### Prerequisites

- **Flutter SDK** (`>=3.0.0`) — [Installation Guide](https://docs.flutter.dev/get-started/install)
- **Android Studio** — Required for Android SDK and Emulators.
- **Authenticator App** — You MUST have Google Authenticator, Authy, or a similar TOTP app installed on your testing device/emulator to complete the initial setup flow.

### Installation

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/yourusername/cipherspend.git](https://github.com/yourusername/cipherspend.git)
   cd cipherspend
   ```

2. **Fetch Flutter packages:**
   ```bash
   flutter pub get
   ```

3. **Configure Android SDK (If necessary):**
   Ensure your `local.properties` file points to your Android SDK path.

4. **Run the App:**
   Connect a physical Android device or launch an emulator, then run:
   ```bash
   flutter run
   ```

---

## 🔐 Security Workflows

### The Hybrid Setup Flow
1. **Initial Boot:** The app detects no existing vault and forces the user into `setup_screen.dart`.
2. **TOTP Pairing:** The user is presented with a QR Code / Secret Key to pair with their Authenticator app.
3. **Verification:** The user inputs the 6-digit TOTP code to prove ownership.
4. **MPIN Generation:** The user creates a 4-digit MPIN for daily use.
5. **Key Generation:** A secure encryption key is generated, stored in Android KeyStore (`flutter_secure_storage`), and used to initialize the `sqflite_sqlcipher` database.

### High-Risk Actions
Actions like exporting data, deleting the database, or changing settings are wrapped in the `HighRiskGuard` widget. This intercepts the user intent and forces them to input a fresh 6-digit TOTP code before allowing the action to proceed.

---

## 🧪 Testing

The project includes an end-to-end integration test that verifies the critical path of the application.

To run the integration suite:
```bash
flutter test integration_test/app_flow_test.dart
```

---

## 🤝 Contributing
Because this project deals with sensitive local encryption and financial data logic, please ensure all PRs maintain the strict "100% Offline" rule. No external HTTP packages or cloud analytics SDKs will be merged.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request