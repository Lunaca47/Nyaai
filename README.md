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
- 10,240 Pre-Trained Legal Queries: Pre-packaged with thousands of verified real-world legal question-and-answer pairs across reformed criminal acts and citizen rights.
- Works Offline: You do not need an active internet connection for statutory research. Nyaai comes with a preloaded local database containing 1,838 sections across reformed penal laws and the Constitution of India.
- Autonomous Self-Training: User feedback dynamically teaches the on-device retrieval engine, caching verified answers for instant future recall.
- Voice Search in 5 Languages: Supports voice input in English, Hindi, Bengali, Telugu, and Tamil.
- Document Scanner: Snap a photo of a legal notice, agreement, or FIR copy. On-device text recognition extracts key deadlines, relevant sections, and recommended next steps without uploading your documents to third-party servers.
- Emergency Legal Helplines: Direct access to verified national numbers including NALSA (free legal aid), NCW women helpline, and National Cybercrime reporting (1930).
- Privacy-Focused: No tracking, no forced logins. A guest mode is available directly from the welcome screen.

---

## Trained Legal Knowledge Base (10,240 Queries)

Nyaai includes a curated corpus of 10,240 verified legal inquiry pairs covering common Indian legal situations, statutory interpretations, and procedural workflows.

### Coverage Breakdown

| Legal Domain | Source Act / Framework | Key Topics Trained |
|:---|:---|:---|
| Criminal Offenses | Bharatiya Nyaya Sanhita (BNS 2023) | Theft (Sec 303/305), cheating (Sec 318), criminal breach of trust (Sec 316), homicide (Sec 103), cyber offences |
| Police & Criminal Procedure | Bharatiya Nagarik Suraksha Sanhita (BNSS 2023) | Zero FIR (Sec 173), arrest rights (Sec 35/47/53), regular & anticipatory bail (Sec 480/482), search warrants |
| Evidence & Digital Records | Bharatiya Sakshya Adhiniyam (BSA 2023) | Electronic evidence admissibility (Sec 61), digital certificate compliance (Sec 63), WhatsApp/email records |
| Constitutional Rights | Constitution of India (1950) | Fundamental rights (Art 14, 19, 21), privacy protections, writ jurisdiction (Art 32 and 226) |
| Civil & Consumer Law | Consumer Protection Act, NI Act, Labor Codes | Defective products & e-commerce refunds, Section 138 cheque bounce notices, maximum working hours & overtime |

### Autonomous Self-Training Loop

Nyaai features a client-side learning mechanism:
1. When a user queries a legal scenario and marks an answer as helpful, the system records the validated query-answer mapping into local storage (`nyaai_user_learned_qa`).
2. The learned entry is indexed at the top of the local retrieval structure.
3. Subsequent identical or similar questions match with high confidence without needing cloud re-evaluation.

---

## Cloud and Edge Architecture

Nyaai uses a hybrid architecture designed for speed, privacy, and full offline resilience:

```text
               ┌────────────────────────────────────────────────────────┐
               │                     Cloud Services                     │
               │  ┌───────────────────────┐  ┌───────────────────────┐  │
               │  │  Google Gemini Flash  │  │  Global Dataset CDN   │  │
               │  │   (Online Synthesis)  │  │ (SHA-256 Manifest v2) │  │
               │  └───────────▲───────────┘  └───────────┬───────────┘  │
               └──────────────┼──────────────────────────┼──────────────┘
                              │                          │
                   REST Grounded Synthesis      HTTP Manifest Sync
                              │                          │
┌─────────────────────────────┼──────────────────────────┼─────────────────────────────┐
│                             │   Client Device (Edge)   ▼                             │
│  ┌──────────────────────────┴───────────────┐   ┌─────────────────────────────────┐  │
│  │       Hybrid Query Processing            │   │   Background Dataset Updater    │  │
│  │  • Online: Gemini 2.5 Flash synthesis    │   │   • Checks manifest.json hash   │  │
│  │  • Offline: Sub-50ms local DB fallback   │   │   • Over-the-air dataset sync   │  │
│  └──────────────────────────▲───────────────┘   └─────────────────────────────────┘  │
│                             │                                                        │
│  ┌──────────────────────────┴─────────────────────────────────────────────────────┐  │
│  │                         On-Device Knowledge Engine                             │  │
│  │   SQLite FTS4 (1,838 Sections)  +  Preloaded Training Corpus (10,240 Q&As)     │  │
│  └────────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

### Key Architectural Pillars

- Edge-First Execution: All core queries, searches, and OCR operations run on the device using SQLite FTS4 and ML Kit. The app does not require a cloud connection to function.
- Continuous Cloud Dataset Sync: The client periodically checks `manifest.json` hosted on the global CDN. When a new legal batch or revised statutory commentary is published, the client downloads the updated master dataset without requiring an app store release.
- Grounded Cloud Synthesis: When connected to the internet, user queries are augmented with retrieved statutory sections and processed through Google Gemini 2.5 Flash to ensure factual, hallucination-free legal explanations.
- Zero-Knowledge Document Privacy: Legal documents scanned via camera or PDF are parsed entirely within device memory using on-device ML Kit OCR. Document images and extracted text are never transmitted to external servers.

---

## Tech Stack

| Component | Technology | Purpose |
|:---|:---|:---|
| Mobile App | Kotlin + Jetpack Compose | Material 3 interface with adaptive light/dark theme |
| Online AI | Google Gemini 2.5 Flash | Conversational synthesis grounded in retrieved statutes |
| Offline Retrieval | SQLite FTS4 | Instant keyword and section search (< 50ms) |
| Knowledge Corpus | 10,240 Verified Q&As | Pre-trained corpus covering criminal, civil, and constitutional law |
| Cloud Sync Engine | CDN Manifest Engine | SHA-256 verified over-the-air dataset synchronization |
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
   ├── 2. Local full-text search (FTS4) across 1,838 sections & 10,240 Q&As
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
