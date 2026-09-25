# Nyaai (न्यायAI) — Indian Statutory Intelligence & Case Navigation Platform

Nyaai is an open-source legal intelligence platform designed to navigate India's reformed criminal codes (**Bharatiya Nyaya Sanhita, 2023**, **BNSS, 2023**, and **BSA, 2023**) alongside constitutional provisions and consumer protections.

Nyaai bridges the gap between codified statutory law and everyday legal situations. It combines structured legal intake, hybrid statutory retrieval (BM25 + dense semantic embeddings), strict citation verification gates, and encrypted matter workspaces for citizens, law students, and legal professionals.

---

## Live Access & Downloads

- **Web Client:** [Launch Live Web Terminal & Document Scanner](https://lunaca47.github.io/Nyaai/)
- **Android App:** Download the signed release APK directly from [docs/app-release.apk](docs/app-release.apk) (11.97 MB)
- **GitHub Repository:** [https://github.com/Lunaca47/Nyaai](https://github.com/Lunaca47/Nyaai)

---

## Core Capabilities (V2 Architecture)

- **FastAPI Statutory Backend:** High-performance REST service providing hybrid search, citation verification, case intake, and matter management.
- **Hybrid Retrieval Engine (BM25 + Dense RRF):** Combines lexical search (BM25 Okapi) and semantic vector search (`all-MiniLM-L6-v2`) fused via Reciprocal Rank Fusion ($k=60$) with jurisdiction boosting and metadata filtering across India's criminal and constitutional codes.
- **CitationVerifier Grounding & Currentness Gate:** A strict 5-stage verification gate that validates every cited statute against retrieved context, flags repealed pre-2024 laws (IPC, CrPC, IEA), and annotates model frameworks.
- **Matter Workspace:** End-to-end legal matter management with client facts, chronological timeline events, structured evidence items, action plans, and client-side encryption.
- **Codified Statutory Codex:** Curated corpus of **1,705 substantive legal provisions** plus **220 reference stubs** (schedule forms, definitions, and transitional references).
- **On-Device OCR & Document Intelligence:** Client-side document scanning and text extraction for FIRs, legal notices, and agreements using Google ML Kit.
- **Emergency Legal Helplines:** Instant access to 17 verified national emergency helplines including NALSA (15100 free legal aid), NCW women helpline (1091), and National Cybercrime (1930).

---

## Architecture Overview

Nyaai operates as a modular, privacy-conscious platform with an asynchronous FastAPI backend, a native Android client with encrypted local storage, and a responsive web client hosted on GitHub Pages:

```text
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                 Client Interfaces                                      │
│   • Android Native App (Jetpack Compose, Room, Encrypted Matter Workspace, AES-GCM)   │
│   • Web Client (Static SPA deployed to GitHub Pages at https://lunaca47.github.io/Nyaai/)│
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ HTTP / REST (Bearer Token / HMAC Guest)
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              FastAPI Statutory Backend                                 │
│                                                                                        │
│  ┌──────────────────────┐   ┌──────────────────────────┐   ┌────────────────────────┐  │
│  │   Intake & Matters   │   │     Hybrid Retrieval     │   │    CitationVerifier    │  │
│  │  • Slot filling      │   │  • BM25 Okapi (lexical)  │   │  • Grounding check     │  │
│  │  • Fact extraction   │──▶│  • Dense all-MiniLM-L6-v2│──▶│  • Repealed law gate   │  │
│  │  • Action plans      │   │  • Reciprocal Rank Fusion│   │  • Model law caveats   │  │
│  │  • AES-GCM encryption│   │  • Jurisdiction boosting │   │  • Hallucination block │  │
│  └──────────────────────┘   └──────────────────────────┘   └────────────────────────┘  │
│                                           │                                            │
│                                           ▼                                            │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │                          Codified Statutory Knowledge Base                       │  │
│  │   • 1,705 Substantive Provisions (BNS 2023, BNSS 2023, BSA 2023, Constitution)   │  │
│  │   • 220 Reference Stubs (Schedules, procedural forms, cross-statute stubs)       │  │
│  └──────────────────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## How It Works: The Five-Stage Pipeline

```text
User Legal Query / Scenario
         │
         ▼
[ Stage 1: Intake & Entity Extraction ]
  • Extracts core facts, legal domain (criminal, procedural, consumer, tenancy)
  • Identifies statutory mentions, section numbers, and required reliefs
         │
         ▼
[ Stage 2: Hybrid Retrieval (BM25 + Dense RRF) ]
  • Lexical BM25 Okapi search over 1,705 substantive provisions
  • Dense vector search using 384-dimensional embeddings (all-MiniLM-L6-v2)
  • Reciprocal Rank Fusion (RRF, k=60) with central/state jurisdiction boosting
         │
         ▼
[ Stage 3: Statutory Synthesis ]
  • Formulates plain-language explanation strictly grounded in retrieved provisions
  • Constructs multi-phase procedural roadmaps (immediate, investigative, judicial)
         │
         ▼
[ Stage 4: CitationVerifier Hard Gate ]
  • Analyzes all candidate legal citations in the output against retrieved text
  • Validates currentness (rejects repealed IPC/CrPC; routes to BNS/BNSS/BSA)
  • Checks model law status (annotates non-binding Model Tenancy Act with Entry 18 notice)
  • Emits one of 5 gate verdicts:
    - PASSED: Clean, grounded, binding statutory answer
    - ANNOTATED_REPEALED: Grounded, but annotated with post-July 1, 2024 repeal mappings
    - ANNOTATED_MODEL_LAW: Grounded, but annotated with State adoption requirement
    - ANNOTATED_UNGROUNDED: Output contains unverified citations alongside valid ones
    - REJECTED_UNGROUNDED: Output rejected entirely due to hallucinated citations
         │
         ▼
[ Stage 5: Matter Workspace / Client Response ]
  • Delivers structured guidance with statutory citations and confidence scores
  • Syncs facts, timeline, evidence items, and action plans to encrypted Matter
```

---

## CitationVerifier Gate Hierarchy

Legal AI must never cite repealed statutes as current law, nor hallucinate non-existent sections. Nyaai enforces a strict deterministic validation gate:

| Gate Action | Trigger Condition | System Response |
|:---|:---|:---|
| `PASSED` | All cited sections are grounded in retrieved statutory context and currently in force. | Verified response presented with full statutory citations and high confidence. |
| `ANNOTATED_REPEALED` | Citations reference repealed pre-2024 statutes (e.g. IPC Section 420, CrPC Section 439). | Citations are flagged as repealed on July 1, 2024, with direct mapped replacements (e.g. BNS Section 318(4), BNSS Section 482). |
| `ANNOTATED_MODEL_LAW` | Citations reference model frameworks (e.g. Model Tenancy Act, 2021). | Response includes mandatory caveat that model law requires State legislative enactment under Entry 18 of the State List. |
| `ANNOTATED_UNGROUNDED` | Mixed response where some citations are grounded but others lack context. | Unverified citations are flagged with an ungrounded warning badge. |
| `REJECTED_UNGROUNDED` | Citations exist in output but none appear in retrieved statutory context. | Response is blocked from presentation to prevent hallucination hazards. |

---

## Statutory Codex Coverage

The codified knowledge base contains **1,705 substantive provisions** and **220 non-searched reference stubs**:

| Act / Framework | Year | Provisions Indexed | Scope & Focus |
|:---|:---|:---|:---|
| **Bharatiya Nyaya Sanhita (BNS)** | 2023 | 358 Sections | Substantive penal offenses, offenses against body and property, cyber fraud, cruelty (replaces IPC 1860). |
| **Bharatiya Nagarik Suraksha Sanhita (BNSS)** | 2023 | 531 Sections | Criminal procedure, Zero FIR (Sec 173), arrest rights (Sec 35/47), regular & anticipatory bail (Sec 480/482). |
| **Bharatiya Sakshya Adhiniyam (BSA)** | 2023 | 170 Sections | Rules of evidence, electronic evidence admissibility (Sec 61), digital records compliance (Sec 63). |
| **Constitution of India (COI)** | 1950 | 395 Articles | Fundamental Rights (Arts 14, 19, 21), privacy jurisprudence, writ remedies (Arts 32 & 226). |
| **Special & Consumer Acts** | Various | 251 Sections | Consumer Protection Act 2019, Negotiable Instruments Act (Sec 138 cheque bounce), IT Act, Motor Vehicles Act. |
| **Reference Stubs (Excluded from active search)** | — | 220 Stubs | Procedural schedule forms, definitions, repeals stubs (`statutory_reference_stubs.json`). |

---

## Matter Workspace

The Matter Workspace provides structured, client-side encrypted case file tracking:
- **Intake Slot-Filling:** Gathers critical incident data (parties, timeline, jurisdiction, financial impact, relief sought).
- **Cryptographic Security:** Matter metadata and narrative facts are encrypted client-side using **AES-GCM-256**.
- **Action Plans & Timelines:** Automated generation of chronological incident sequences, procedural deadlines, and required legal actions.
- **Evidence Management:** Checklists for preserving critical digital or documentary evidence (WhatsApp exports, bank receipts, CCTV footage).

---

## Project Structure

```text
nyaai/
├── app/                  # Android Kotlin application (Jetpack Compose, Room, MVVM)
│   ├── src/main/java/com/nyaai/
│   │   ├── data/intake/        # Case intake & entity orchestrator
│   │   ├── data/matter/        # Encrypted matter workspace & models
│   │   ├── data/retrieval/     # Client-side legal retrieval service
│   │   ├── data/verification/  # Client-side CitationVerifier gate
│   │   └── ui/screens/         # Compose screens (Chat, Matters, Detail, Codex, SOS)
│   └── src/test/java/com/nyaai/# 25 Android unit & integration tests
├── backend/              # FastAPI statutory RAG & verification service
│   ├── app/
│   │   ├── intake/             # Slot-filling engine & YAML schemas
│   │   ├── retrieval/          # Hybrid BM25 + dense search (all-MiniLM-L6-v2)
│   │   ├── routers/            # matters, intake, retrieval, verification, generation
│   │   └── models/             # SQLAlchemy ORM models (PostgreSQL)
│   ├── data/                   # statutory_codex.json (1,705 items), embeddings.npy, stubs
│   ├── tests/                  # 43 pytest unit & integration tests
│   └── Dockerfile              # Containerized production deployment
├── docs/                 # GitHub Pages client (HTML5, CSS3, Vanilla JS)
│   ├── index.html              # Landing page & feature showcase
│   ├── terminal.html           # Live interactive AI research terminal
│   ├── scanner.html            # Private client-side legal document scanner
│   ├── codex.html              # Searchable statutory codex
│   ├── sos.html                # Emergency helplines directory
│   └── config.js               # Client API endpoint configuration
└── scripts/              # Dataset consolidation and evaluation scripts
```

---

## Current Status & What Is NOT Yet Done

Nyaai adheres to a strict standard of engineering transparency. The following items represent work in progress and are **not** yet complete:

1. **No Human Manual Test Pass:** The system has passed automated unit test suites (43 backend tests, 25 Android tests) and automated retrieval benchmarks, but has not yet undergone a comprehensive manual user-testing pass by licensed legal practitioners across all jurisdictions.
2. **No Live Production Cloud Deployment:** The backend service is containerized (`backend/Dockerfile`, `backend/docker-compose.yml`) and verified locally, but is not currently hosted on a live cloud cluster. The GitHub Pages web client operates against local or user-configured API endpoints via `docs/config.js`.
3. **Statutory Law Only (No Case Law / Precedents):** The codex indexes codified Indian statutes and constitutional provisions only. It does not yet index Supreme Court or High Court case judgments, judicial precedents, or law reports.
4. **No Knowledge Graph:** Cross-statutory relationships are managed through BM25+dense vector search and migration dictionaries, rather than a formal graph database.
5. **No Backend Audio Synthesis:** Voice input and speech synthesis are implemented purely client-side via the browser Web Speech API and Android SpeechRecognizer; there is no server-side voice processing pipeline.
6. **English-Only Semantic Embeddings:** Dense vector search (`all-MiniLM-L6-v2`) operates in English. While client interfaces provide UI localization in 5 Indian languages (Hindi, Bengali, Telugu, Tamil, English), multilingual cross-lingual embedding retrieval is not yet implemented.

---

## Running Locally

### 1. Backend Service (FastAPI)

```bash
# Navigate to backend directory
cd backend

# Create and activate virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: .\venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run test suite (43 tests)
pytest tests

# Launch API server
uvicorn app.main:app --reload --port 8000
```

The Swagger API documentation will be available at `http://localhost:8000/docs`.

### 2. Android Application

```bash
# From project root
# Run unit test suite (25 tests)
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug

# Assemble signed release APK
./gradlew assembleRelease
```

### 3. Web Client (GitHub Pages)

The web client in `docs/` is pure static HTML/CSS/JavaScript with zero build dependencies:
- Serve locally using any HTTP server: `python -m http.server 5500 --directory docs`
- Open `http://localhost:5500` in your browser.
- Point to your backend by opening the browser devtools console and setting:
  `localStorage.setItem("nyaai_api_url", "http://localhost:8000")`

---

## Disclaimer

Nyaai is an educational, research, and legal literacy platform. It provides information based on codified statutes and computational analysis, but **does not constitute formal legal counsel** or establish an attorney-client relationship. For specific litigation, trial proceedings, or legal advice, always consult an advocate registered with the Bar Council of India.

---

## License

This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.
