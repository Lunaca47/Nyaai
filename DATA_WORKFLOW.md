# Nyaai (न्यायAI) — Data Architecture & Workflow

This document details the end-to-end data pipeline of Nyaai, explaining how legal documents are indexed, how user queries are processed across online and offline states, and how on-device retrieval and self-training work.

---

## 1. Complete Workflow Diagram

```mermaid
flowchart TD
    %% 1. Data Ingestion & Indexing Pipeline
    subgraph Data_Prep ["1. Data Ingestion and Build Pipeline (Offline Indexing)"]
        A1["Official Statutory PDFs<br>(BNS, BNSS, BSA, Constitution)"]
        A2["Curated Q&A Corpus<br>(10,240 Verified Legal Pairs)"]
        A3["Python Bundler and Parser<br>(scripts/build_preloaded_db.py)"]
        A4[("SQLite FTS4 Database<br>nyaai_preloaded.db")]
        
        A1 --> A3
        A2 --> A3
        A3 -->|"Tokenization and FTS4 Indexing"| A4
    end

    %% 2. User Input Channels
    subgraph Input_Channels ["2. Client Ingestion Layer"]
        U1["Voice Query<br>(Native Dialect STT in 5 Languages)"]
        U2["Text Query<br>(Mobile App or Web Terminal)"]
        U3["Document Image / PDF<br>(Legal Notice, FIR, Contract)"]
        U4["On-Device ML Kit OCR<br>(Extracts clauses, dates, violations)"]
        
        U3 --> U4
        U4 --> U2
        U1 --> U2
    end

    %% 3. Retrieval Engine
    subgraph RAG_Engine ["3. On-Device Retrieval Engine (FTS4)"]
        R1["Query Normalizer and Keyword Extractor"]
        R2["Fast Multi-Token and Section Matcher"]
        R3["Ranked Statutory Chunks + Confidence Score"]
        
        U2 --> R1
        A4 -.->|"Preloaded and Embedded"| R2
        R1 --> R2
        R2 --> R3
    end

    %% 4. Decision & Synthesis Layer
    subgraph Decision_Branch ["4. Synthesis and Grounding Layer"]
        D1{"Internet Available and<br>Gemini API Configured?"}
        
        S_ONLINE["Online AI Synthesis<br>(Google Gemini 2.5 Flash)"]
        S_OFFLINE["Offline Local Retrieval<br>(Direct Codified Excerpts)"]
        
        R3 --> D1
        D1 -->|"Yes"| S_ONLINE
        D1 -->|"No / Offline"| S_OFFLINE
    end

    %% 5. Response & Feedback
    subgraph Output_Loop ["5. Presentation and Autonomous Feedback Loop"]
        OUT["Plain-Language Legal Assessment<br>+ Exact Section Citation<br>+ Confidence Metric<br>+ Speech Read-Aloud"]
        FB["User Feedback Click<br>(Helpful / Train)"]
        LOCAL_STORE[("Local Storage<br>Custom Q&A Cache")]
        
        S_ONLINE --> OUT
        S_OFFLINE --> OUT
        OUT --> FB
        FB -->|"Auto-Learned Entry"| LOCAL_STORE
        LOCAL_STORE -.->|"Unshifted into KB"| R2
    end

    %% Styling
    classDef prep fill:#EEF2FF,stroke:#4F46E5,stroke-width:1.5px,color:#1E1B4B;
    classDef input fill:#F0FDF4,stroke:#16A34A,stroke-width:1.5px,color:#14532D;
    classDef rag fill:#FEF3C7,stroke:#D97706,stroke-width:1.5px,color:#78350F;
    classDef decision fill:#F1F5F9,stroke:#475569,stroke-width:1.5px,color:#0F172A;
    classDef output fill:#FDF2F8,stroke:#DB2777,stroke-width:1.5px,color:#831843;

    class A1,A2,A3,A4 prep;
    class U1,U2,U3,U4 input;
    class R1,R2,R3 rag;
    class D1,S_ONLINE,S_OFFLINE decision;
    class OUT,FB,LOCAL_STORE output;
```

---

## 2. Pipeline Breakdown

### Stage 1: Data Ingestion and Offline Indexing
- Source documents: Constitution of India, Bharatiya Nyaya Sanhita (BNS 2023), Bharatiya Nagarik Suraksha Sanhita (BNSS 2023), and Bharatiya Sakshya Adhiniyam (BSA 2023).
- Data is parsed, stripped of stopwords, tokenized, and indexed into SQLite with FTS4 full-text search capability.
- Bundled directly inside the Android APK (under `app/src/main/assets/database/nyaai_preloaded.db`) and web client for instant offline cold starts.

### Stage 2: Client Ingestion Layer
- Text Queries: Typed directly in the mobile app or web terminal.
- Voice Search: Speech-to-Text with Indian dialect acoustic routing (`en-IN`, `hi-IN`, `bn-IN`, `te-IN`, `ta-IN`).
- Document Vision: Google ML Kit OCR and Android PDFBox parse legal notices, contracts, and FIR copies on-device with zero cloud leakage.

### Stage 3: On-Device Retrieval Engine (FTS4)
- Multi-token scoring and section regex detection (e.g., Section 482, Article 21, Section 103).
- BM25/FTS4 scoring across 1,838 indexed sections and 10,240 verified Q&As.
- Calculates confidence score based on keyword relevance and exact section matches.

### Stage 4: Synthesis and Grounding Layer
- Online Mode: Pipes the retrieved statutory citations as strict context to Google Gemini 2.5 Flash, generating a plain-language summary and actionable next steps.
- Offline Mode: If network is unavailable or API key is absent, immediately displays the exact verified statutory provisions directly from local storage.

### Stage 5: Autonomous Feedback Loop
- User feedback (Helpful / Train) triggers on-device caching in local storage (`nyaai_user_learned_qa`).
- The learned Q&A entry is unshifted into the active knowledge base for instantaneous recall on subsequent queries.
