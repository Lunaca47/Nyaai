# Nyaai (न्यायAI) — Indian Law, Explained in Plain English

Ever tried reading an official legal notice or statutory section and felt like you needed a law degree just to understand what was going on?

When India's reformed criminal codes (Bharatiya Nyaya Sanhita, BNSS, and BSA) rolled out to replace the 160-year-old IPC and CrPC, navigating personal rights didn't get any simpler for everyday citizens.

Nyaai is an open-source, on-device legal assistant designed to take complex legal jargon and translate it into clear, everyday language. Whether you are a law student, an advocate looking for quick citations, or someone trying to understand a police notice, consumer refund rights, or bail procedures, Nyaai helps you find grounded answers quickly.

---

## Try It

You can test Nyaai without setting up Android Studio:

- Web Version: [Launch Web Terminal and Scanner](https://lunaca47.github.io/Nyaai/)
- Android App: Download the signed APK directly from [web/app-release.apk](web/app-release.apk) (11.4 MB) and install it on your device.

---

## Highlights

- Plain-Language Answers: Ask practical questions like "What happens if police arrest someone without a warrant?" or "How do I deal with a cheque bounce notice?", and receive clear explanations with specific statutory citations.
- Works Offline: You do not need an active internet connection for basic statutory research. Nyaai comes with a preloaded local database containing 1,838 sections across reformed penal laws and the Constitution of India.
- Voice Search in 5 Languages: Supports voice input in English, Hindi, Bengali, Telugu, and Tamil.
- Document Scanner: Snap a photo of a legal notice, agreement, or FIR copy. On-device text recognition extracts key deadlines, relevant sections, and recommended next steps without uploading your documents to third-party servers.
- Emergency Legal Helplines: Direct access to verified national numbers including NALSA (free legal aid), NCW women helpline, and National Cybercrime reporting (1930).
- Privacy-Focused: No tracking, no forced logins. A guest mode is available directly from the welcome screen.

---

## Tech Stack

| Component | Technology | Purpose |
|:---|:---|:---|
| Mobile App | Kotlin + Jetpack Compose | Material 3 interface with adaptive light/dark theme |
| Online AI | Google Gemini 2.5 Flash | Conversational synthesis grounded in retrieved statutes |
| Offline Retrieval | SQLite FTS4 | Instant keyword and section search (< 50ms) |
| OCR and Vision | Google ML Kit + Android PDFBox | On-device private document parsing |
| Web Client | Vanilla JavaScript + CSS | Lightweight static client hosted on GitHub Pages |
| Authentication | Firebase Auth + Guest Mode | Optional Google Sign-In or one-tap guest access |

---

## Running Locally

### Prerequisites
- Android Studio (recent version)
- JDK 17 (included with Android Studio)
- Android device or emulator running Android 7.0 (API 24) or newer

### Setup Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/Lunaca47/Nyaai.git
   ```

2. Open the project in Android Studio and let Gradle complete its initial sync.

3. Optional: Configure Gemini API Key
   The app works offline out of the box using local database retrieval. If you want full online AI responses:
   - Get an API key from Google AI Studio.
   - Add the following line to `local.properties` in your project root:
     ```properties
     gemini.api.key=YOUR_GEMINI_API_KEY
     ```

4. Connect your device and click Run.

---

## How It Works

```text
Your Question
   │
   ├── 1. Keyword extraction and section number detection
   │
   ├── 2. Local full-text search (FTS4) across 1,838 sections
   │
   ├── 3. Retrieval of relevant legal chunks with confidence score
   │
   ├── 4. Online: Grounded synthesis via Gemini 2.5 Flash
   │
   └── 5. Offline fallback: Direct excerpts from statutory database
```

### Covered Acts

- Constitution of India (1950): Fundamental rights, writ remedies, constitutional structure.
- Bharatiya Nyaya Sanhita, 2023 (BNS): Substantive penal code (replaces IPC 1860).
- Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS): Criminal procedure, arrest rights, bail (replaces CrPC 1973).
- Bharatiya Sakshya Adhiniyam, 2023 (BSA): Rules of evidence and electronic records (replaces IEA 1872).

---

## Project Structure

```text
nyaai/
├── app/                  # Android Kotlin source code and assets
├── web/                  # Web application source files
├── docs/                 # GitHub Pages deployment bundle
├── scripts/              # Dataset generation and indexing scripts
└── release_package/      # Signed release binaries and store assets
```

---

## Build Commands

From the project root:

```bash
# Run unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

---

## Disclaimer

Nyaai is an educational research and legal literacy tool. It provides information based on codified statutes and AI synthesis, but does not constitute formal legal counsel or create an attorney-client relationship. For specific litigation or legal disputes, consult a licensed advocate.

---

## Contributing

Contributions, bug reports, and suggestions are welcome. Feel free to open an issue or submit a pull request on GitHub.
