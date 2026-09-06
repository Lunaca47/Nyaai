# 📋 NYAAI — Master Project Handover & Production Release Document

> **Project:** NYAAI (न्यायAI) — AI-Powered Indian Legal Assistant  
> **Platform:** Android Native (Kotlin + Jetpack Compose)  
> **Release Version:** v1.0 Production  
> **Date:** September 2026

---

## 📌 1. What is NYAAI?

NYAAI is an **AI-powered legal technology platform** that makes Indian criminal and constitutional law accessible to advocates, law students, citizens, and law enforcement in plain, everyday language.

Key architectural capabilities:
- **Instant Pre-Packaged Legal Knowledge Base**: 1,838 sections across BNS 2023, BNSS 2023, BSA 2023, and Constitution of India + 75 curated legal Q&As pre-indexed in an SQLite/Room FTS4 database (`nyaai_preloaded.db`, 4.27 MB, Schema v8).
- **Sub-100ms Cold Start**: Pre-packaged database replaces runtime PDF indexing, guaranteeing instant zero-latency startup.
- **Dual AI Mode**: Sub-second offline statutory retrieval + Google Gemini 2.5 Flash for deep statutory synthesis and drafting.
- **On-Device Legal OCR & Document Scanner**: Zero cloud upload; private on-device image text recognition via Google ML Kit Vision & PDF text extraction via PDFBox Android.
- **Multilingual Voice Search**: Real-time speech-to-text supporting Indian accents across English, Hindi, Bengali, Telugu, and Tamil.
- **Legal Bookmarks & Case Notes Export**: Save sections with persistent Room storage and export styled PDF research reports.
- **Production Release Signing & R8 Minification**: Code/resource shrinking reducing APK size by ~66% (11.47 MB APK / 14.70 MB AAB).

---

## 📦 2. Release Deliverables & Turnkey Package

A self-contained production handoff bundle is compiled in [`release_package/`](release_package/):

```
release_package/
├── HANDOFF_README.md              # Executive summary & deployment guide
├── checksums.txt                  # Cryptographic SHA-256 and MD5 hashes
├── binaries/
│   ├── app-release.apk            # Production Signed APK (11.47 MB)
│   └── app-release.aab            # Production Signed Google Play App Bundle (14.70 MB)
├── store_listing/
│   ├── en-US/                     # English titles, descriptions, and changelogs
│   └── hi-IN/                     # Hindi titles, descriptions, and changelogs
├── marketing_graphics/
│   ├── icon_512x512.png           # Google Play high-res icon
│   ├── feature_graphic_1024x500.png# Play Store panoramic feature graphic
│   └── screenshot_*.png           # 4x 1080x1920 (9:16) marketing screenshots
└── compliance/
    ├── PRIVACY_POLICY.md          # Google Play & DPDP Act 2023 privacy policy
    ├── DATA_SAFETY_GUIDE.md       # Exact answers for Play Console Data Safety form
    └── PLAY_STORE_CHECKLIST.md    # Step-by-step publishing & IARC rating guide
```

### Verified Release Hashes
- **APK (`app-release.apk`)**:
  - Size: 12,032,926 bytes (~11.47 MB)
  - SHA-256: `818514398A81773BB8F466B33221D19332027E180741D139F0011B8AF1FF9926`
  - APK Signature Scheme v2: **Verified** (`CN=Nyaai, OU=LegalTech, O=Nyaai, L=Hyderabad, ST=Telangana, C=IN`)
- **AAB (`app-release.aab`)**:
  - Size: 15,409,169 bytes (~14.70 MB)
  - SHA-256: `0E7C122377D3353B35747B831A1A06F7E0ED8A20492144DBFB3C7F251BB34E46`

---

## 🏗️ 3. Source Tree Architecture

```
app/src/main/java/com/nyaai/
├── MainActivity.kt                # App entry point; binds preloaded DB v8, AI service, Firebase, State
├── data/local/
│   ├── RagDatabase.kt             # Room DB: documents (FTS4), bookmarks, chat_sessions, training_examples
│   ├── DocumentScannerService.kt  # On-device ML Kit OCR & PDFBox extractor
│   ├── PdfExtractorService.kt      # Safe fallback indexer
│   └── AiService.kt               # Hybrid RAG pipeline: retrieval → confidence → Gemini API → fallback
├── theme/
│   ├── Theme.kt                   # Material 3 dark/light themes (Navy & Gold palette)
│   └── Type.kt                    # Typography styles
└── ui/
    ├── navigation/
    │   └── AppNavigation.kt        # Jetpack Compose navigation graph
    ├── screens/
    │   ├── SplashScreen.kt         # Animated splash
    │   ├── WelcomeScreen.kt        # Onboarding
    │   ├── LoginScreen.kt          # Firebase Google Sign-In with safe Context unwrapping
    │   ├── MainScreen.kt           # Main hub with adaptive IME insets & Bookmarks drawer
    │   ├── ChatScreen.kt           # AI chat, voice input (STT), OCR document scanner, PDF export
    │   ├── SettingsScreen.kt       # Persistent settings (Theme, Language, Clear DB, Logout)
    │   └── AboutScreen.kt          # Mission, Legal SOS helplines, reactive training counter
    ├── state/
    │   └── AppState.kt             # 13 CompositionLocal providers (Theme, Lang, Auth, AI, etc.)
    └── strings/
        └── AppStrings.kt           # Multilingual strings for EN, HI, BN, TE, TA
```

---

## 🔑 4. Credentials & Keystore Setup

Credentials reside exclusively in `local.properties` (strictly ignored by `.gitignore`):

```properties
sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
gemini.api.key=YOUR_GEMINI_API_KEY
release.keystore.file=release.keystore
release.keystore.password=NyaaiSecure2026!
release.key.alias=nyaai_release
release.key.password=NyaaiSecure2026!
```

---

## 🚀 5. Build, Test & Deployment Commands

```bash
# 1. Run Automated Unit Tests (6/6 tests passing)
.\gradlew.bat test --no-daemon

# 2. Build Production Signed Release APK
.\gradlew.bat assembleRelease --no-daemon

# 3. Build Production Signed Release AAB for Google Play
.\gradlew.bat bundleRelease --no-daemon

# 4. Fastlane: Run All Checks & Upload to Google Play Internal Track
bundle exec fastlane android internal
```

---

## 🔒 6. Security, Privacy & Data Compliance

1. **Zero Cloud File Storage**: Neither legal document uploads, camera photos, nor microphone recordings are retained or transmitted to external servers. All text recognition is performed strictly on-device using Google ML Kit.
2. **Encrypted Communication**: All outbound Gemini AI prompts and Firebase Auth tokens are protected via TLS 1.3 / HTTPS.
3. **Hardened ProGuard / R8**: ProGuard keep rules protect Room database classes, Gemini SDK schemas, and ML Kit vision libraries while stripping unused code and symbols.
4. **Google Play Compliance**: Pre-filled guides for Privacy Policy, Data Safety declarations, and IARC content ratings are provided in `compliance/`.
