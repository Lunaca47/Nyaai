# ⚖️ Nyaai (न्यायAI) — Indian Law, Explained in Plain Human

> Ever tried reading an official legal notice or statutory section and felt like you needed a law degree just to understand what was going on? Yeah, us too.
>
> When the reformed criminal codes (**Bharatiya Nyaya Sanhita**, **BNSS**, and **BSA**) rolled out to replace the 160-year-old IPC and CrPC, navigating your rights didn't get any simpler for the average person.
>
> **Nyaai** is an open-source, on-device legal AI assistant designed to take all that complex legal jargon and translate it into clear, everyday language. Whether you're a law student studying for exams, an advocate looking up citations on the go, or a citizen trying to understand an FIR or consumer refund rights—Nyaai has your back.

---

## ⚡ Try It Right Now

You don't even need Android Studio to test it out:

- 🌐 **Live Web Playground**: [Try Nyaai Web Terminal & Scanner](https://lunaca47.github.io/Nyaai/)
- 📲 **Get the Android App**: Grab the signed production build directly from [`web/app-release.apk`](web/app-release.apk) (11.4 MB) and sideload it onto any Android phone.

---

## 💡 What Makes Nyaai Cool?

- 💬 **Speaks Plain Language**: Ask a question like *"My landlord won't return my security deposit"* or *"What happens if police arrest someone without a warrant?"*, and get back an easy-to-read explanation with actual section citations.
- 📴 **Works Even Without Internet**: Nobody has high-speed Wi-Fi in a rural courtroom or basement police station. Nyaai bundles a preloaded SQLite database with **1,838 statutory sections** and **10,240 verified Q&As**. If your network drops, it immediately falls back to on-device statutory retrieval.
- 🎙️ **Voice Search in 5 Indian Languages**: Supports English, हिन्दी (Hindi), বাংলা (Bengali), తెలుగు (Telugu), and தமிழ் (Tamil) with native dialect speech-to-text.
- 📄 **Snap & Scan Legal Documents**: Point your phone camera at a paper legal notice, contract, or FIR copy. On-device Google ML Kit OCR extracts the key statutory clauses, legal deadlines, and actionable next steps without your private files ever leaving your phone.
- 🚨 **National Legal SOS**: One-touch access to official emergency toll-free numbers like NALSA (free legal aid), NCW women helpline, National Cybercrime (`1930`), and child protection.
- 🔒 **Privacy-First**: No tracking, zero cloud data leakage for OCR, and you can even bypass login using the in-app **Guest Mode**.

---

## 🛠️ Tech Stack at a Glance

| Layer | What We Used | Why |
|:---|:---|:---|
| **OS / UI** | Android (Kotlin) + Jetpack Compose | Smooth Material 3 design, fast 60fps animations |
| **Brain (Online)** | Google Gemini 2.5 Flash | Lightning-fast conversational legal synthesis |
| **Brain (Offline)** | Pre-packaged SQLite FTS4 (`nyaai_preloaded.db`) | Cold-start queries in under 50ms with zero internet |
| **Document Vision** | Google ML Kit OCR + Android PDFBox | 100% private, on-device document text extraction |
| **Web Client** | Modern Vanilla JS + CSS Grid | Lightweight, zero-dependency browser client |
| **Auth** | Firebase Google Sign-In + Guest Mode | Hassle-free login or instant guest bypass |

---

## 🚀 Running the Project Locally

Getting the Android app running on your machine is straightforward:

### 1. Prerequisites
- [Android Studio](https://developer.android.com/studio) (recent version like Hedgehog / Iguana / Ladybug)
- JDK 17 (comes bundled inside Android Studio as JBR)
- An Android device or emulator (Android 7.0 / API 24 or newer)

### 2. Clone and Open
```bash
git clone https://github.com/Lunaca47/Nyaai.git
```
Open the folder in Android Studio via **File → Open**. Let Gradle do its initial sync.

### 3. Add Your Gemini API Key (Optional)
The app works completely offline right out of the box using local statutory retrieval. But if you want full AI synthesis via Google Gemini:

1. Grab a free API key from [Google AI Studio](https://aistudio.google.com/).
2. Create or edit `local.properties` in your project root and add:
   ```properties
   gemini.api.key=YOUR_GEMINI_KEY_HERE
   ```
3. Hit the green **▶ Run** button in Android Studio. That's it!

---

## 🧠 How the RAG Pipeline Works

When you ask a question, here is what happens under the hood:

```text
Your Question ("Can police search my house without a warrant?")
   │
   ├─► 1. Keyword & Multi-Token Extraction (strips stopwords, finds section numbers)
   │
   ├─► 2. Local FTS4 Search across 1,838 sections (BNS, BNSS, BSA, Constitution)
   │
   ├─► 3. Relevant statutory chunks retrieved + confidence score calculated
   │
   ├─► 4. If Internet is available:
   │       Pipes verified citations into Gemini 2.5 Flash → Returns conversational summary
   │
   └─► 5. If Offline / No API Key:
           Instantly renders the exact verified statutory sections directly from local DB
```

### Indexed Legal Codex

- 📜 **Constitution of India (1950)** — Fundamental rights (Article 14, 19, 21), writ remedies (Article 32 & 226), and governance.
- ⚖️ **Bharatiya Nyaya Sanhita, 2023 (BNS)** — Reformed substantive penal law (replaces IPC 1860).
- 🚔 **Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS)** — Criminal procedure, arrest rights, bail provisions (replaces CrPC 1973).
- 🔍 **Bharatiya Sakshya Adhiniyam, 2023 (BSA)** — Evidence law, digital evidence admissibility & certificates (replaces IEA 1872).

---

## 📁 Repository Structure

```text
nyaai/
├── app/                  # Native Android Kotlin app
│   ├── src/main/assets/  # Preloaded SQLite FTS4 DB (10,240 Q&As, 1,838 sections)
│   └── src/main/java/    # Jetpack Compose UI, Room DB, Gemini RAG, ML Kit OCR
├── web/                  # Complete web client (Terminal, Scanner, Codex, SOS)
├── docs/                 # GitHub Pages static bundle
├── scripts/              # Python tools for database bundling and dataset validation
└── release_package/      # Signed APKs, Play Store metadata, and screenshots
```

---

## 🧪 Building from the Command Line

If you prefer the terminal:

```bash
# Run unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Build optimized, R8-minified release APK
./gradlew assembleRelease
```

Generated APKs will be located under `app/build/outputs/apk/`.

---

## ⚠️ Friendly Legal Disclaimer

*Nyaai is an educational research and legal literacy tool powered by artificial intelligence and codified statutory texts. It is designed to help you understand your rights and procedures, but **it does not constitute formal legal counsel or create an advocate-client relationship**. If you are facing litigation, criminal proceedings, or court deadlines, always consult a licensed advocate.*

---

## 🤝 Contributing & Feedback

Have ideas to make Indian legal information even easier to understand? Found a section that needs better plain-language summaries? 

Feel free to open an issue, submit a pull request, or drop suggestions. Contributions of all kinds—from code to legal prompt refinements—are warmly welcome! 🇮🇳
