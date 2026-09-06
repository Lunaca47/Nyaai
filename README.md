# 🏛️ NYAAI — AI-Powered Indian Legal Assistant

> **Nyaai** makes Indian law simple and accessible for everyone — farmers, students, workers, homemakers — by translating complex legal jargon into plain, everyday language using AI.

---

## 📌 Quick Overview

| Aspect | Detail |
|--------|--------|
| **Platform** | Android (Native) |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose + Material 3 |
| **AI Model** | Google Gemini 2.5 Flash (via REST API) |
| **Database** | Room (SQLite) with FTS4 Full-Text Search |
| **Auth** | Firebase Google Sign-In |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 34 (Android 14) |

---

## 🚀 How to Run

### Prerequisites
- **Android Studio** (latest stable version)
- **Android SDK** API 34
- **Internet connection** (for Gemini AI & Firebase Auth)

### Steps

1. **Open** the project in Android Studio (`File → Open → select this folder`)
2. **Wait** for Gradle sync to complete (2-5 min on first load)
3. **Fix paths** if needed:
   - `local.properties` → Update `sdk.dir` to your Android SDK path
   - `gradle.properties` → Update `org.gradle.java.home` to your Android Studio JBR path (or remove the line)
4. **Connect** a physical device (USB debugging) or create an emulator (API 26+)
5. **Click** the green ▶ Run button

> ⚡ **Instant Launch**: Database comes pre-packaged with 1,838 indexed legal sections and 75 training Q&As (`nyaai_preloaded.db`). First launch and database preparation take **< 100ms**.

---

## 🔑 API Key

The Gemini API key is configured in `local.properties` in your project root:

```properties
gemini.api.key=YOUR_API_KEY_HERE
```

Get a key from [Google AI Studio](https://aistudio.google.com/). Gradle automatically injects this into `BuildConfig.GEMINI_API_KEY` at build time.

> The app automatically falls back to **offline mode** (direct document excerpts) if the API is unavailable or key is not provided.

---

## 🏗️ Architecture

```
app/src/main/java/com/nyaai/
├── MainActivity.kt           ← Entry point: sets up DB (v8), AI, Firebase, Compose
├── data/local/
│   ├── RagDatabase.kt        ← Room DB: documents (FTS4), bookmarks, chat history, training
│   ├── DocumentScannerService.kt ← Dual-pipeline OCR (ML Kit) & PDF Parser (PDFBox)
│   ├── PdfExtractorService.kt ← Fallback indexer for local documents
│   └── AiService.kt          ← RAG pipeline: retrieval → confidence → Gemini API → fallback
├── theme/
│   ├── Theme.kt              ← Material 3 color schemes (dark/light)
│   └── Type.kt               ← Typography definitions
└── ui/
    ├── navigation/
    │   └── AppNavigation.kt   ← Route definitions & navigation graph
    ├── screens/
    │   ├── SplashScreen.kt    ← Loading screen
    │   ├── WelcomeScreen.kt   ← Onboarding
    │   ├── LoginScreen.kt     ← Google Sign-In
    │   ├── MainScreen.kt      ← Main hub with bottom nav & Bookmarks drawer
    │   ├── ChatScreen.kt      ← AI chat, voice input (STT), OCR document scanner
    │   ├── SettingsScreen.kt  ← Theme, language, privacy settings
    │   └── AboutScreen.kt    ← Vision & Legal SOS helplines
    ├── state/
    │   └── AppState.kt        ← CompositionLocal providers (theme, language, auth)
    └── strings/
        └── AppStrings.kt      ← Multi-language UI text (EN, HI, BN, TE, TA)
```

---

## 📱 How It Works

```
User Question → Keyword Extraction → FTS4 Search (Room DB)
    → Retrieve top 5 legal document chunks
    → Calculate confidence score (0.40 – 0.99)
    → Send to Gemini 2.5 Flash with grounded prompt
    → Return friendly, plain-language legal answer
    → If API fails → Offline fallback with direct excerpts
```

### Pre-packaged Legal Knowledge Base (`app/src/main/assets/database/nyaai_preloaded.db`)
| Source Act | Full Name | Indexed Sections |
|------------|-----------|------------------|
| `coi.pdf`  | Constitution of India | 448 Articles & Preamble |
| `bns.pdf`  | Bharatiya Nyaya Sanhita (Criminal Law) | 358 Sections |
| `bnss.pdf` | Bharatiya Nagarik Suraksha Sanhita (Criminal Procedure) | 531 Sections |
| `bsa.pdf`  | Bharatiya Sakshya Adhiniyam (Evidence Act) | 170 Sections |
| Curated Q&A | Pre-trained legal question & answer pairs | 75 Curated pairs |

---

## ✨ Key Features

- 🤖 **AI Legal Chat** — Ask any legal question in natural language
- 🎙️ **Voice Input (STT)** — Multi-language speech-to-text with Indian dialect routing (`en-IN`, `hi-IN`, `bn-IN`, `te-IN`, `ta-IN`)
- 📷 **Legal Document Scanner & OCR** — On-device image text recognition (ML Kit) and PDF parsing (PDFBox)
- 🔖 **Legal Bookmarks** — Save and organize key sections and AI responses with persistent Room storage
- 📚 **Instant Startup & Preloaded RAG** — Over 1,800 legal sections pre-packaged for instant < 100ms cold start
- 🌐 **5 Indian Languages** — English, Hindi, Bengali, Telugu, Tamil
- 🎨 **Dark/Light Theme** — Material 3 adaptive theming
- 🔒 **Firebase Auth** — Secure Google Sign-In with dynamic profile integration
- 💬 **Chat History** — Persistent conversations with feedback system
- 🚨 **Legal SOS** — Emergency helpline numbers
- 📴 **Offline Fallback** — Works without internet (raw excerpts)

---

## 🧪 Testing Suggestions

1. Sign in → Ask "What is Article 21?" → Verify AI response
2. Ask "What are my rights if arrested?" → Check confidence + sources
3. Turn off internet → Ask any question → Verify offline fallback
4. Switch to Hindi in Settings → Verify UI language change
5. Toggle Dark Mode → Verify theme changes

---

## 📝 Note

This project does **NOT** require any Python backend server. The `backend/` folder contains an optional advanced RAG server that is not needed for the Android app to function. Everything works standalone.
