# NYAAI v1.0 — Production Release & Handover Guide

Welcome to the **NYAAI (न्यायAI)** v1.0 production release package. NYAAI is an Indian legal tech mobile application built with native Android (Jetpack Compose, Kotlin, Room FTS4, and Google ML Kit) providing offline statutory reasoning, on-device legal document OCR, voice search, and hybrid Google Gemini generative intelligence.

---

## 1. Release Deliverables Inventory

This directory (`release_package/`) contains all binaries, metadata, and compliance documents needed for direct distribution and Google Play Console publishing:

```
release_package/
├── HANDOFF_README.md              # This master handover document
├── checksums.txt                  # Cryptographic SHA-256 and MD5 hashes
├── binaries/
│   ├── app-release.apk            # Production Signed APK (11.47 MB) for direct sideloading / testing
│   └── app-release.aab            # Production Signed Android App Bundle (14.70 MB) for Google Play
├── store_listing/
│   ├── en-US/                     # English store listing (Title, Short/Full Descriptions, Changelogs)
│   └── hi-IN/                     # Hindi store listing (शीर्षक, संक्षिप्त विवरण, पूर्ण विवरण, रिलीज़ नोट्स)
├── marketing_graphics/
│   ├── icon_512x512.png           # 32-bit PNG high-resolution Play Store icon (512x512 px)
│   ├── feature_graphic_1024x500.png# 24-bit PNG panoramic marketing banner (1024x500 px)
│   ├── screenshot_1_chat_1080x1920.png       # Screenshot 1: AI Legal Chat with statutory citations
│   ├── screenshot_2_ocr_1080x1920.png        # Screenshot 2: On-device legal notice OCR scanner
│   ├── screenshot_3_acts_1080x1920.png       # Screenshot 3: 1,800+ indexed sections (BNS, BNSS, BSA, COI)
│   └── screenshot_4_bookmarks_1080x1920.png  # Screenshot 4: Saved bookmarks & PDF case notes export
└── compliance/
    ├── PRIVACY_POLICY.md          # Google Play & India DPDP Act 2023 compliant privacy policy
    ├── DATA_SAFETY_GUIDE.md       # Exact field-by-field responses for Play Console Data Safety form
    └── PLAY_STORE_CHECKLIST.md    # Complete Play Store submission walkthrough & IARC rating guide
```

### Cryptographic Signatures & Checksums
- **APK (`app-release.apk`)**:
  - Size: `12,032,926` bytes (~11.47 MB)
  - SHA-256: `818514398A81773BB8F466B33221D19332027E180741D139F0011B8AF1FF9926`
  - MD5: `98E805534065BF0822637547D94CCFF3`
  - Signature: APK Signature Scheme v2 (Signed by `CN=Nyaai, OU=LegalTech, O=Nyaai, L=Hyderabad, ST=Telangana, C=IN`)
- **AAB (`app-release.aab`)**:
  - Size: `15,409,169` bytes (~14.70 MB)
  - SHA-256: `0E7C122377D3353B35747B831A1A06F7E0ED8A20492144DBFB3C7F251BB34E46`
  - MD5: `DE09ED491F9C1FEB9BB14F9E90DBE077`

---

## 2. Technical Architecture Overview

| Component | Technology | Implementation Details |
|---|---|---|
| **UI Framework** | Jetpack Compose & Material 3 | Modern reactive UI, adaptive keyboard window insets, bottom navigation drawer. |
| **Local Legal DB** | Room Database (SQLite + FTS4) | Pre-packaged `nyaai_preloaded.db` (4.27 MB, v8 schema) containing 1,838 legal chunks and 75 Q&As across BNS, BNSS, BSA, and COI. |
| **Document OCR** | Google ML Kit Text Recognition | 100% on-device text recognition for paper notices, FIRs, and agreements with zero cloud transmission. |
| **Cloud AI Reasoning** | Google Generative AI (Gemini) | Deep legal drafting and cross-statute synthesis via encrypted HTTPS calls. |
| **Authentication** | Google Firebase Auth | Google Sign-In with safe activity context resolution and session persistence. |
| **Build & Minification** | Android Gradle Plugin & R8 | `isMinifyEnabled = true` with hardened ProGuard rules reducing APK payload by ~66%. |
| **Deployment** | Fastlane & Supply | Multi-lane pipeline for automated testing, release builds, and Google Play track deployment. |

---

## 3. Environment & Security Protocol

### Keystore Security
- The 2048-bit RSA keystore is saved at `app/release.keystore`.
- Passwords and alias are managed strictly via `local.properties`:
  ```properties
  gemini.api.key=<YOUR_GEMINI_API_KEY>
  release.keystore.file=release.keystore
  release.keystore.password=NyaaiSecure2026!
  release.key.alias=nyaai_release
  release.key.password=NyaaiSecure2026!
  ```
- **Security Guarantee:** `local.properties`, `*.keystore`, and `*.jks` are registered in `.gitignore` and are never committed to version control.

---

## 4. Development & Build Commands

```bash
# Run Unit Tests
.\gradlew.bat test --no-daemon

# Build Signed Release APK
.\gradlew.bat assembleRelease --no-daemon

# Build Signed Release AAB (Google Play)
.\gradlew.bat bundleRelease --no-daemon

# Fastlane: Run Tests
bundle exec fastlane android test

# Fastlane: Build and Upload to Play Store Internal Track
bundle exec fastlane android internal
```

---

## 5. Google Play Submission Next Steps
1. **Google Play Console:** Create a new app entry (`com.nyaai`).
2. **App Content:** Fill out Privacy Policy URL (host `compliance/PRIVACY_POLICY.md`) and Data Safety (use `compliance/DATA_SAFETY_GUIDE.md`).
3. **Store Listing:** Copy metadata from `store_listing/en-US/` and `store_listing/hi-IN/`, and upload images from `marketing_graphics/`.
4. **Release Track:** Upload `binaries/app-release.aab` to Internal Testing or Production.
