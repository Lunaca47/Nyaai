"""
NYAAI Project Synopsis Report Generator
Generates a professional Word document (.docx) synopsis report.
"""

from docx import Document
from docx.shared import Inches, Pt, RGBColor, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.section import WD_ORIENT
from docx.oxml.ns import qn, nsdecls
from docx.oxml import parse_xml
import os

def set_cell_shading(cell, color):
    """Set background color for a table cell."""
    shading = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{color}"/>')
    cell._tc.get_or_add_tcPr().append(shading)

def add_formatted_paragraph(doc, text, style='Normal', bold=False, italic=False, 
                            font_size=11, font_name='Calibri', color=None, 
                            alignment=WD_ALIGN_PARAGRAPH.LEFT, space_after=6, space_before=0):
    """Add a formatted paragraph to the document."""
    p = doc.add_paragraph()
    p.alignment = alignment
    p.paragraph_format.space_after = Pt(space_after)
    p.paragraph_format.space_before = Pt(space_before)
    run = p.add_run(text)
    run.bold = bold
    run.italic = italic
    run.font.size = Pt(font_size)
    run.font.name = font_name
    if color:
        run.font.color.rgb = color
    return p

def add_bullet_point(doc, text, font_size=11, bold_prefix=None):
    """Add a bullet point paragraph."""
    p = doc.add_paragraph(style='List Bullet')
    p.paragraph_format.space_after = Pt(3)
    if bold_prefix:
        run = p.add_run(bold_prefix)
        run.bold = True
        run.font.size = Pt(font_size)
        run.font.name = 'Calibri'
        run = p.add_run(text)
        run.font.size = Pt(font_size)
        run.font.name = 'Calibri'
    else:
        run = p.add_run(text)
        run.font.size = Pt(font_size)
        run.font.name = 'Calibri'
    return p

def add_numbered_item(doc, number, text, font_size=11):
    """Add a numbered item."""
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.left_indent = Cm(1)
    run = p.add_run(f"{number}. ")
    run.bold = True
    run.font.size = Pt(font_size)
    run.font.name = 'Calibri'
    run = p.add_run(text)
    run.font.size = Pt(font_size)
    run.font.name = 'Calibri'
    return p

def create_table(doc, headers, rows, col_widths=None):
    """Create a formatted table."""
    table = doc.add_table(rows=1 + len(rows), cols=len(headers))
    table.style = 'Table Grid'
    table.alignment = WD_TABLE_ALIGNMENT.CENTER

    # Header row
    for i, header in enumerate(headers):
        cell = table.rows[0].cells[i]
        cell.text = ''
        p = cell.paragraphs[0]
        run = p.add_run(header)
        run.bold = True
        run.font.size = Pt(10)
        run.font.name = 'Calibri'
        run.font.color.rgb = RGBColor(255, 255, 255)
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        set_cell_shading(cell, "1B3A5C")

    # Data rows
    for r_idx, row in enumerate(rows):
        for c_idx, cell_text in enumerate(row):
            cell = table.rows[r_idx + 1].cells[c_idx]
            cell.text = ''
            p = cell.paragraphs[0]
            run = p.add_run(str(cell_text))
            run.font.size = Pt(10)
            run.font.name = 'Calibri'
            if r_idx % 2 == 0:
                set_cell_shading(cell, "EBF0F5")

    if col_widths:
        for i, width in enumerate(col_widths):
            for row in table.rows:
                row.cells[i].width = Inches(width)

    doc.add_paragraph()  # spacing
    return table

def generate_synopsis():
    doc = Document()

    # ── Page Setup ──
    section = doc.sections[0]
    section.top_margin = Cm(2.54)
    section.bottom_margin = Cm(2.54)
    section.left_margin = Cm(3.17)
    section.right_margin = Cm(3.17)

    # ══════════════════════════════════════════════════════════════════
    #  TITLE PAGE
    # ══════════════════════════════════════════════════════════════════

    for _ in range(4):
        doc.add_paragraph()

    add_formatted_paragraph(doc, "A PROJECT SYNOPSIS REPORT", bold=True, font_size=14,
                            alignment=WD_ALIGN_PARAGRAPH.CENTER, space_after=12,
                            color=RGBColor(0x1B, 0x3A, 0x5C))
    add_formatted_paragraph(doc, "on", font_size=12,
                            alignment=WD_ALIGN_PARAGRAPH.CENTER, space_after=12)
    
    # Title
    title_p = doc.add_paragraph()
    title_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title_p.paragraph_format.space_after = Pt(6)
    run = title_p.add_run("NYAAI – AI-Powered Legal Assistant for India")
    run.bold = True
    run.font.size = Pt(22)
    run.font.name = 'Calibri'
    run.font.color.rgb = RGBColor(0x1B, 0x3A, 0x5C)

    doc.add_paragraph()

    add_formatted_paragraph(doc, "An Intelligent Mobile Application Leveraging Retrieval-Augmented Generation (RAG) "
                            "and Large Language Models for Accessible Legal Information Delivery to Indian Citizens",
                            italic=True, font_size=12, alignment=WD_ALIGN_PARAGRAPH.CENTER, space_after=24)

    doc.add_paragraph()
    doc.add_paragraph()

    # Metadata
    add_formatted_paragraph(doc, "Platform: Native Android (Kotlin · Jetpack Compose)", font_size=12,
                            alignment=WD_ALIGN_PARAGRAPH.CENTER, space_after=4)
    add_formatted_paragraph(doc, "Backend: Python · FastAPI · RAG Pipeline", font_size=12,
                            alignment=WD_ALIGN_PARAGRAPH.CENTER, space_after=4)
    add_formatted_paragraph(doc, "AI Engine: Google Gemini API · Ollama (Local LLM)", font_size=12,
                            alignment=WD_ALIGN_PARAGRAPH.CENTER, space_after=20)

    add_formatted_paragraph(doc, "Version 1.0 | April 2026", bold=True, font_size=13,
                            alignment=WD_ALIGN_PARAGRAPH.CENTER, space_after=6,
                            color=RGBColor(0x1B, 0x3A, 0x5C))

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  TABLE OF CONTENTS
    # ══════════════════════════════════════════════════════════════════

    add_formatted_paragraph(doc, "TABLE OF CONTENTS", bold=True, font_size=16,
                            alignment=WD_ALIGN_PARAGRAPH.CENTER, space_after=20,
                            color=RGBColor(0x1B, 0x3A, 0x5C))

    toc_items = [
        ("1.", "Introduction", 3),
        ("2.", "Problem Statement", 4),
        ("3.", "Objectives", 4),
        ("4.", "Scope of the Project", 5),
        ("5.", "Literature Survey / Existing Systems", 5),
        ("6.", "Proposed System", 6),
        ("7.", "System Architecture", 7),
        ("8.", "Technology Stack", 9),
        ("9.", "Module Description", 10),
        ("10.", "Database Design", 13),
        ("11.", "RAG Pipeline Architecture", 14),
        ("12.", "API Design", 15),
        ("13.", "User Interface Design", 16),
        ("14.", "Testing & Validation", 17),
        ("15.", "Advantages & Limitations", 18),
        ("16.", "Future Enhancements", 19),
        ("17.", "Conclusion", 20),
        ("18.", "References", 20),
    ]

    for num, title, page in toc_items:
        p = doc.add_paragraph()
        p.paragraph_format.space_after = Pt(4)
        run = p.add_run(f"  {num}  ")
        run.bold = True
        run.font.size = Pt(12)
        run.font.name = 'Calibri'
        run = p.add_run(title)
        run.font.size = Pt(12)
        run.font.name = 'Calibri'
        # Add dotted leader and page number
        run = p.add_run(f"  {'.' * (60 - len(title))}  {page}")
        run.font.size = Pt(11)
        run.font.name = 'Calibri'
        run.font.color.rgb = RGBColor(128, 128, 128)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  1. INTRODUCTION
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('1. Introduction', level=1)

    add_formatted_paragraph(doc, 
        "India, home to over 1.4 billion people, operates one of the world's most complex legal systems — "
        "a blend of codified statutes, constitutional law, and centuries of judicial precedent. Despite this "
        "rich legal framework, a vast majority of Indian citizens remain unaware of their fundamental rights, "
        "legal remedies, and procedural safeguards. The reasons are multifold: prohibitive legal consultation "
        "costs, language barriers, legal jargon, and inadequate access to updated legal databases.",
        font_size=11, space_after=8)

    add_formatted_paragraph(doc,
        "NYAAI (derived from the Hindi word 'न्याय' meaning Justice) is an AI-powered legal assistant "
        "mobile application designed to bridge this critical gap. Built as a native Android application using "
        "Kotlin and Jetpack Compose, NYAAI leverages cutting-edge Retrieval-Augmented Generation (RAG) technology "
        "to deliver accurate, contextual, and citation-backed legal information to ordinary Indian citizens — "
        "in plain, understandable language.",
        font_size=11, space_after=8)

    add_formatted_paragraph(doc,
        "The application processes four foundational legal documents of India:",
        font_size=11, space_after=4, bold=True)

    add_bullet_point(doc, "", bold_prefix="Constitution of India (COI) – ")
    p = doc.paragraphs[-1]
    run = p.add_run("The supreme law containing Fundamental Rights, Directive Principles, and the governance framework.")
    run.font.size = Pt(11)
    run.font.name = 'Calibri'

    add_bullet_point(doc, "", bold_prefix="Bharatiya Nyaya Sanhita (BNS) 2023 – ")
    p = doc.paragraphs[-1]
    run = p.add_run("The new criminal code replacing the Indian Penal Code (IPC), effective from July 1, 2024.")
    run.font.size = Pt(11)
    run.font.name = 'Calibri'

    add_bullet_point(doc, "", bold_prefix="Bharatiya Nagarik Suraksha Sanhita (BNSS) 2023 – ")
    p = doc.paragraphs[-1]
    run = p.add_run("Replacing the Code of Criminal Procedure (CrPC), governing criminal procedure.")
    run.font.size = Pt(11)
    run.font.name = 'Calibri'

    add_bullet_point(doc, "", bold_prefix="Bharatiya Sakshya Adhiniyam (BSA) 2023 – ")
    p = doc.paragraphs[-1]
    run = p.add_run("Replacing the Indian Evidence Act, modernizing evidence law with digital evidence provisions.")
    run.font.size = Pt(11)
    run.font.name = 'Calibri'

    add_formatted_paragraph(doc,
        "NYAAI combines on-device PDF extraction, full-text search using Room FTS4, reinforcement learning from "
        "user feedback, and the Google Gemini 2.5 Flash API to generate intelligent, grounded legal responses. "
        "The companion Python backend provides an advanced RAG pipeline with hybrid retrieval (ChromaDB vector "
        "search + BM25 keyword search), cross-encoder re-ranking, and confidence-gated LLM generation.",
        font_size=11, space_after=8)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  2. PROBLEM STATEMENT
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('2. Problem Statement', level=1)

    add_formatted_paragraph(doc,
        "Access to justice remains one of the most pressing socio-legal challenges in India. Despite constitutional "
        "guarantees under Articles 14, 21, and 39A, millions of citizens are unable to exercise their legal rights "
        "due to systemic barriers:",
        font_size=11, space_after=8)

    problems = [
        ("High Cost of Legal Consultation: ", "Even basic legal advice costs ₹500–₹5,000 per session, making it inaccessible for low-income households that constitute over 60% of India's population."),
        ("Legal Illiteracy: ", "Over 70% of litigants in Indian courts are unaware of the specific laws applicable to their cases, leading to exploitation and delayed justice."),
        ("Language Barrier: ", "Most legal texts are in English, while a majority of Indian citizens communicate in regional languages. The new criminal codes (BNS/BNSS/BSA) further add complexity as citizens must learn entirely new section numbering."),
        ("Outdated Information: ", "Generic legal websites often contain references to repealed laws (IPC, CrPC, Evidence Act) without clear mapping to the new 2023 codes."),
        ("Information Overload: ", "Raw legal documents run into thousands of pages. Without expert interpretation, citizens cannot extract actionable insights from these documents."),
        ("No Intelligent Search: ", "Existing legal databases offer keyword-based search but lack semantic understanding — searching for 'my rights if arrested' yields irrelevant results instead of Article 22, Section 173 BNSS, and D.K. Basu guidelines."),
    ]

    for prefix, text in problems:
        add_bullet_point(doc, text, bold_prefix=prefix)

    add_formatted_paragraph(doc,
        "NYAAI aims to solve these problems by creating an intelligent, AI-driven legal assistant that understands "
        "natural language queries, retrieves relevant legal provisions, and explains them in simple terms — all from "
        "a mobile device.",
        font_size=11, space_after=8, space_before=8)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  3. OBJECTIVES
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('3. Objectives', level=1)

    add_formatted_paragraph(doc, "The primary objectives of the NYAAI project are:", font_size=11, space_after=8)

    objectives = [
        "To design and develop a native Android application that provides accessible legal information to Indian citizens using AI and NLP technologies.",
        "To implement an on-device Retrieval-Augmented Generation (RAG) pipeline that processes and indexes the Constitution of India, BNS, BNSS, and BSA for instant legal query resolution.",
        "To integrate the Google Gemini 2.5 Flash API for generating intelligent, citation-backed, and simplified legal explanations in multiple languages.",
        "To build a self-improving system through autonomous training — generating 1,000+ legal Q&A pairs from raw legal text using structured LLM reasoning cycles.",
        "To incorporate user feedback mechanisms (Good/Average/Poor ratings) that enable reinforcement learning, ensuring the AI's responses improve over time.",
        "To develop a companion Python backend with a FastAPI-based RAG server featuring hybrid retrieval (vector + keyword), cross-encoder re-ranking, and confidence-gated response routing.",
        "To provide an intuitive, modern user interface built with Jetpack Compose and Material 3 Design, supporting dark/light themes and multilingual display.",
        "To ensure offline-first capability where basic legal lookups work without internet connectivity through local Room database FTS4 search.",
        "To include emergency legal SOS features with direct access to helpline numbers for women, children, cyber crime, and human rights.",
    ]

    for i, obj in enumerate(objectives, 1):
        add_numbered_item(doc, i, obj)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  4. SCOPE OF THE PROJECT
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('4. Scope of the Project', level=1)

    doc.add_heading('4.1 In Scope', level=2)
    in_scope = [
        "AI-powered legal Q&A chat interface with citation support",
        "On-device PDF parsing and indexing of 4 core Indian legal documents (COI, BNS, BNSS, BSA)",
        "Full-text search using SQLite FTS4 via Room Database",
        "Integration with Google Gemini 2.5 Flash API for AI-generated responses",
        "Autonomous training pipeline generating 1,000 legal Q&A training examples",
        "User feedback-driven reinforcement learning system",
        "Firebase Authentication (Google Sign-In)",
        "Chat session history with persistent storage",
        "Multilingual support (English, Hindi, Bengali, Telugu, Tamil, Hinglish)",
        "Privacy Policy disclaimer and legal SOS helpline shortcuts",
        "Python backend with RAG server (ChromaDB + BM25 hybrid retrieval)",
        "Web scraper for Constitution, BNS/BNSS/BSA, and 100 landmark Supreme Court cases",
        "Dark mode / Light mode theme support",
    ]
    for item in in_scope:
        add_bullet_point(doc, item)

    doc.add_heading('4.2 Out of Scope (Current Version)', level=2)
    out_scope = [
        "Real-time case status tracking from eCourts portal",
        "Lawyer directory or appointment booking",
        "Document drafting (FIR, complaints, affidavits)",  
        "Voice-based query input (Speech-to-Text)",
        "iOS platform support",
        "Integration with paid legal databases (SCC Online, Manupatra)",
    ]
    for item in out_scope:
        add_bullet_point(doc, item)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  5. LITERATURE SURVEY
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('5. Literature Survey / Existing Systems', level=1)

    add_formatted_paragraph(doc,
        "Several existing platforms provide legal information in India. A comparative analysis reveals the gaps "
        "that NYAAI addresses:",
        font_size=11, space_after=10)

    create_table(doc,
        headers=["Platform", "Type", "AI-Powered", "RAG Pipeline", "Offline", "New Codes (BNS/BNSS/BSA)", "Free"],
        rows=[
            ["Indian Kanoon", "Web Portal", "No", "No", "No", "Partial", "Yes"],
            ["SCC Online", "Web/App", "No", "No", "No", "Yes", "No (Paid)"],
            ["Manupatra", "Web/App", "Limited", "No", "No", "Yes", "No (Paid)"],
            ["Nyaya Bandhu", "Web Portal", "No", "No", "No", "No", "Yes"],
            ["AI Lawyer Apps", "Mobile App", "Basic GPT", "No", "No", "No", "Freemium"],
            ["NYAAI (Ours)", "Mobile App", "Gemini RAG", "Yes (Hybrid)", "Yes (FTS4)", "Yes (Full)", "Yes"],
        ],
        col_widths=[1.2, 0.8, 0.8, 0.8, 0.6, 1.2, 0.6]
    )

    add_formatted_paragraph(doc,
        "Key Differentiators of NYAAI:", bold=True, font_size=11, space_after=4, space_before=4)

    differentiators = [
        "First open-source mobile app with a complete RAG pipeline built specifically for India's new criminal codes (2023).",
        "Hybrid retrieval combining semantic vector search (ChromaDB) with BM25 keyword search for superior accuracy.",
        "Self-improving AI through autonomous training cycles and user feedback reinforcement.",
        "Offline-first architecture with on-device PDF processing and FTS4-based local search.",
        "Confidence-gated response routing that avoids hallucination by refusing to answer when context is insufficient.",
    ]
    for d in differentiators:
        add_bullet_point(doc, d)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  6. PROPOSED SYSTEM
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('6. Proposed System', level=1)

    add_formatted_paragraph(doc,
        "The proposed NYAAI system is a dual-component architecture consisting of a native Android mobile application "
        "and a Python-based RAG backend server. Together, they form a complete AI-powered legal information delivery platform.",
        font_size=11, space_after=10)

    doc.add_heading('6.1 Mobile Application (Android)', level=2)
    add_formatted_paragraph(doc,
        "The Android application is built with Kotlin and Jetpack Compose, targeting API level 24+ (Android 7.0+). "
        "It features:", font_size=11, space_after=6)

    mobile_features = [
        ("On-Device RAG: ", "Legal PDFs (BNS, BNSS, BSA, COI) are bundled as assets, extracted page-by-page using PDFBox-Android, cleaned of artifacts, and indexed into a Room FTS4 database at first launch."),
        ("AI Chat Interface: ", "A modern chat UI allows users to ask legal questions in natural language. The AI service combines local FTS4 search results with Gemini API calls to generate grounded responses."),
        ("Autonomous Training Engine: ", "A background TrainingService iterates through indexed documents, sends each to Gemini with structured prompts, and stores the generated Q&A pairs as training examples. These are used to enrich future responses."),
        ("Reinforcement Learning: ", "Users can rate AI responses as Good, Average, or Poor. Good-rated responses are fed back into the prompt context, enabling the AI to learn what constitutes a helpful answer."),
        ("Firebase Authentication: ", "Secure Google Sign-In flow provides user identity management."),
        ("Settings & Personalization: ", "Users can configure themes (Light/Dark/System), UI language, AI response language, and notification preferences."),
    ]
    for prefix, text in mobile_features:
        add_bullet_point(doc, text, bold_prefix=prefix)

    doc.add_heading('6.2 Backend RAG Server (Python)', level=2)
    add_formatted_paragraph(doc,
        "The backend server provides an advanced, production-grade RAG pipeline:", font_size=11, space_after=6)

    backend_features = [
        ("Legal Scraper: ", "An asynchronous web scraper (aiohttp + BeautifulSoup) fetches legal documents from indiacode.nic.in, legislative.gov.in, and indiankanoon.org — covering the Constitution, BNS/BNSS/BSA, and 100 landmark Supreme Court cases."),
        ("Document Indexer: ", "Ingested documents are cleaned, chunked with legal-aware separators (Article, Section, Clause boundaries), embedded using SentenceTransformer (all-MiniLM-L6-v2), and indexed into both ChromaDB (vector) and BM25 (keyword) indexes."),
        ("Hybrid Retrieval: ", "User queries are simultaneously searched in ChromaDB (semantic similarity) and BM25 (keyword relevance). Results are merged, deduplicated, and candidates found in both indexes receive a boost score."),
        ("Cross-Encoder Re-ranking: ", "All candidates are scored by a cross-encoder (ms-marco-MiniLM-L-6-v2) for precise relevance ranking, returning only the top-3 most relevant chunks."),
        ("Confidence Gate: ", "The best re-rank score determines the response mode — Direct Answer (>0.75), LLM Generation (0.4–0.75), or Insufficient Information (<0.4) — preventing hallucination."),
        ("LLM Generation: ", "For moderate-confidence queries, the context is passed to Ollama (Gemma2:2B local model) with a grounded system prompt. Responses are streamed via Server-Sent Events."),
        ("Semantic Caching: ", "A cosine-similarity cache (threshold 0.95) prevents redundant computations for repeated or near-identical queries."),
    ]
    for prefix, text in backend_features:
        add_bullet_point(doc, text, bold_prefix=prefix)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  7. SYSTEM ARCHITECTURE
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('7. System Architecture', level=1)

    doc.add_heading('7.1 High-Level Architecture Diagram', level=2)

    add_formatted_paragraph(doc,
        "The NYAAI system follows a layered client-server architecture with an offline-first mobile client "
        "and an optional backend RAG server:",
        font_size=11, space_after=10)

    # Architecture diagram as text
    arch_text = """
┌──────────────────────────────────────────────────────────────┐
│                    ANDROID CLIENT (Kotlin)                    │
│                                                              │
│  ┌───────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │  Chat UI  │  │ Settings UI  │  │  About / SOS Screen  │  │
│  │ (Compose) │  │  (Compose)   │  │     (Compose)        │  │
│  └─────┬─────┘  └──────────────┘  └──────────────────────┘  │
│        │                                                     │
│  ┌─────▼──────────────────────────────────────────────────┐  │
│  │              AI SERVICE LAYER                          │  │
│  │  • FTS4 Local Search (Room DB)                        │  │
│  │  • Gemini 2.5 Flash API (REST)                        │  │
│  │  • Training Example Lookup                            │  │
│  │  • Reinforcement Learning (Feedback)                  │  │
│  └─────┬──────────────────────────────────────────────────┘  │
│        │                                                     │
│  ┌─────▼──────────────────────────────────────────────────┐  │
│  │              DATA LAYER                                │  │
│  │  Room DB v6:                                           │  │
│  │    • documents (FTS4)     • chat_sessions             │  │
│  │    • chat_messages        • training_examples          │  │
│  │  PDF Extractor Service (PDFBox-Android)                │  │
│  │  Training Service (Background)                         │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  BUNDLED ASSETS: coi.pdf, bns.pdf, bnss.pdf, bsa.pdf  │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
                           │
                           │ HTTP/REST (Optional)
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                 PYTHON BACKEND (FastAPI)                      │
│                                                              │
│  ┌────────────────┐  ┌─────────────┐  ┌──────────────────┐  │
│  │ Legal Scraper  │  │  Indexer    │  │   RAG Server     │  │
│  │ (aiohttp)      │  │ (ChromaDB  │  │  (FastAPI)       │  │
│  │                │  │  + BM25)   │  │                  │  │
│  └────────────────┘  └─────────────┘  └────────┬─────────┘  │
│                                                │             │
│  ┌─────────────────────────────────────────────▼──────────┐  │
│  │  RAG Pipeline:                                         │  │
│  │  Query → Embed → ChromaDB + BM25 → Merge & Dedup     │  │
│  │  → Cross-Encoder Re-rank → Confidence Gate            │  │
│  │  → Direct / LLM (Ollama) / Insufficient               │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
"""
    p = doc.add_paragraph()
    run = p.add_run(arch_text)
    run.font.size = Pt(8)
    run.font.name = 'Consolas'

    doc.add_heading('7.2 Data Flow Diagram', level=2)

    add_formatted_paragraph(doc,
        "User Query Flow (Mobile App):", bold=True, font_size=11, space_after=4)

    flow_steps = [
        "User enters a legal query in the Chat screen.",
        "AiService preprocesses the query — removes stop words, generates FTS4 search tokens.",
        "Local Room FTS4 database is searched for matching document chunks (top 3).",
        "Training examples are searched for pre-existing refined Q&A pairs.",
        "Good-rated feedback examples are collected for reinforcement learning.",
        "All context (raw documents + training data + good examples) is sent to Gemini 2.5 Flash API.",
        "Gemini generates a citation-backed, simplified response in the user's preferred language.",
        "Response is displayed in the chat UI with feedback buttons.",
        "Offline fallback: If API is unreachable, trained examples or raw PDF text is shown directly.",
    ]
    for i, step in enumerate(flow_steps, 1):
        add_numbered_item(doc, i, step)

    doc.add_page_break()

    add_formatted_paragraph(doc,
        "Backend RAG Query Flow:", bold=True, font_size=11, space_after=4)

    rag_flow = [
        "User query is received via POST /api/chat.",
        "Query is preprocessed — abbreviations expanded (IPC→Indian Penal Code), filler words removed.",
        "Semantic cache is checked (cosine similarity ≥ 0.95). Cache hit returns instantly.",
        "Query is embedded using SentenceTransformer (all-MiniLM-L6-v2, 384 dimensions).",
        "ChromaDB vector search returns top-10 semantic matches.",
        "BM25 keyword search returns top-10 keyword matches.",
        "Results are merged and deduplicated. Hybrid hits (found in both) receive a score boost.",
        "Cross-encoder (ms-marco-MiniLM-L-6-v2) re-ranks all candidates. Top-3 are selected.",
        "Confidence gate evaluates the best score: Direct (>0.75), LLM (0.4–0.75), Insufficient (<0.4).",
        "For LLM mode: context + system prompt is sent to Ollama (Gemma2:2B). Response is streamed.",
        "Response is cached in the semantic cache for future queries.",
    ]
    for i, step in enumerate(rag_flow, 1):
        add_numbered_item(doc, i, step)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  8. TECHNOLOGY STACK
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('8. Technology Stack', level=1)

    doc.add_heading('8.1 Mobile Application', level=2)

    create_table(doc,
        headers=["Component", "Technology", "Version", "Purpose"],
        rows=[
            ["Language", "Kotlin", "1.9.22", "Primary development language"],
            ["UI Framework", "Jetpack Compose", "BOM 2023.10.01", "Declarative UI toolkit"],
            ["Design System", "Material 3", "Latest", "Modern Material Design components"],
            ["Architecture", "Compose Navigation", "2.7.6", "Screen routing and navigation"],
            ["Database", "Room (SQLite)", "2.6.1", "Local data persistence with FTS4"],
            ["PDF Processing", "PDFBox-Android", "2.0.27.0", "On-device PDF text extraction"],
            ["AI API", "Google Gemini REST", "2.5 Flash", "LLM inference via REST API"],
            ["Authentication", "Firebase Auth", "BOM 32.7.1", "Google Sign-In identity"],
            ["Build System", "Gradle (Kotlin DSL)", "8.13.2", "Android build toolchain"],
            ["Min SDK", "Android API 24", "7.0 Nougat", "Minimum supported Android version"],
            ["Target SDK", "Android API 34", "14", "Target Android version"],
            ["Desugaring", "desugar_jdk_libs", "2.0.4", "Java 8+ API compatibility"],
        ],
        col_widths=[1.3, 1.6, 1.1, 2.0]
    )

    doc.add_heading('8.2 Backend Server', level=2)

    create_table(doc,
        headers=["Component", "Technology", "Purpose"],
        rows=[
            ["Language", "Python 3.10+", "Backend development"],
            ["Web Framework", "FastAPI + Uvicorn", "Async REST API server"],
            ["Vector Store", "ChromaDB", "Persistent vector index for semantic search"],
            ["Embeddings", "SentenceTransformer (all-MiniLM-L6-v2)", "Text embeddings (384-dim)"],
            ["Re-ranker", "CrossEncoder (ms-marco-MiniLM-L-6-v2)", "Candidate re-ranking"],
            ["Keyword Search", "BM25Okapi (rank-bm25)", "Keyword-based retrieval"],
            ["Text Splitting", "LangChain TextSplitters", "Legal-aware document chunking"],
            ["LLM", "Ollama (Gemma2:2B / Phi3:mini)", "Local LLM for generation"],
            ["HTTP Client", "httpx (async)", "Async communication with Ollama"],
            ["PDF Parsing", "PyPDF2", "Server-side PDF text extraction"],
            ["HTML Parsing", "BeautifulSoup4 + lxml", "Web scraping and HTML processing"],
            ["Web Scraping", "aiohttp + aiofiles", "Async legal document scraping"],
            ["Config", "pydantic-settings", "Environment-based configuration"],
        ],
        col_widths=[1.3, 2.5, 2.2]
    )

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  9. MODULE DESCRIPTION
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('9. Module Description', level=1)

    doc.add_heading('9.1 Android Application Modules', level=2)

    # Module 1
    doc.add_heading('9.1.1 PDF Extractor Module (PdfExtractorService.kt)', level=3)
    add_formatted_paragraph(doc,
        "This module runs at application startup to initialize the local legal knowledge base:", font_size=11, space_after=6)
    pdf_features = [
        "Initializes PDFBox-Android resource loader for the application context.",
        "Iterates through bundled PDF assets (coi.pdf, bns.pdf, bnss.pdf, bsa.pdf).",
        "Extracts text page-by-page using PDFTextStripper for fine-grained context.",
        "Applies cleaning filters to remove Gazette headers/footers, page numbers, and non-English characters (KrutiDev/bilingual artifacts).",
        "Validates that text is >70% English characters using heuristic analysis.",
        "Splits cleaned pages into semantic chunks at sentence boundaries (minimum 50 characters per chunk).",
        "Inserts chunks into Room FTS4 'documents' table with source path metadata.",
        "Skips initialization if the database is already populated (idempotent operation).",
    ]
    for item in pdf_features:
        add_bullet_point(doc, item)

    # Module 2
    doc.add_heading('9.1.2 AI Service Module (AiService.kt)', level=3)
    add_formatted_paragraph(doc,
        "The core intelligence engine that processes user queries through a multi-stage pipeline:", font_size=11, space_after=6)
    ai_features = [
        "Stage 1 — Reinforcement Learning: Retrieves good-rated examples from user feedback to include as learning context.",
        "Stage 2 — Training Lookup: Searches pre-generated training examples for matching Q&A pairs.",
        "Stage 3 — Raw Document Search: Performs FTS4 search with stop-word filtering and wildcard matching on the document index.",
        "Stage 4 — Gemini API Call: Composes a structured prompt with raw context + training data + reinforcement examples and sends to Gemini 2.5 Flash REST API.",
        "Stage 5 — Response Parsing: Extracts the generated text from the Gemini JSON response (candidates → content → parts → text).",
        "Offline Fallback: Returns trained examples or raw document text when the API is unavailable.",
        "Prompt Engineering: Instructs the AI to explain clearly for citizens, use 3-5 bullet points, cite specific sections, and keep responses under 180 words.",
    ]
    for item in ai_features:
        add_bullet_point(doc, item)

    # Module 3
    doc.add_heading('9.1.3 Training Service Module (TrainingService.kt)', level=3)
    add_formatted_paragraph(doc,
        "An autonomous background training engine that builds the app's legal intelligence:", font_size=11, space_after=6)
    training_features = [
        "Targets generation of 1,000 training Q&A pairs from raw legal documents.",
        "Iterates through document database in paginated fashion (1 document per cycle).",
        "For each document, sends a 'Structured Reasoning Cycle' prompt to Gemini that asks the model to: (1) Generate a realistic citizen question, (2) Provide a 3-bullet answer with citations, (3) Self-review and simplify.",
        "Parses structured output using Q:/A:/D: label parsing.",
        "Stores training examples in the training_examples table with question, answer, source path, legal domain, and quality rating.",
        "Implements rate limiting (3-second delay between API calls) to avoid quota exhaustion.",
        "Handles API failures gracefully — stops training on invalid API key, retries after 10 seconds on transient errors.",
        "Automatically restarts from the beginning of the document set when the end is reached, enabling multiple refinement passes.",
    ]
    for item in training_features:
        add_bullet_point(doc, item)

    doc.add_page_break()

    # Module 4
    doc.add_heading('9.1.4 UI Screens Module', level=3)

    create_table(doc,
        headers=["Screen", "File", "Description"],
        rows=[
            ["Splash Screen", "SplashScreen.kt", "App launch animation and initialization"],
            ["Welcome Screen", "WelcomeScreen.kt", "Onboarding with legal disclaimer and privacy policy acceptance"],
            ["Login Screen", "LoginScreen.kt", "Firebase Google Sign-In authentication flow"],
            ["Main Screen", "MainScreen.kt", "Bottom navigation hub with Chat, Settings, and About tabs"],
            ["Chat Screen", "ChatScreen.kt", "AI legal assistant chat interface with message history, feedback buttons, and Legal SOS shortcut"],
            ["Settings Screen", "SettingsScreen.kt", "Theme, language, AI language, notifications, account management, chat history"],
            ["About Screen", "AboutScreen.kt", "App version, developer information, legal notices"],
        ],
        col_widths=[1.2, 1.5, 3.3]
    )

    doc.add_heading('9.2 Backend Server Modules', level=2)

    # Backend Module 1
    doc.add_heading('9.2.1 Legal Scraper Module (legal_scraper.py)', level=3)
    add_formatted_paragraph(doc,
        "A comprehensive async web scraper that fetches Indian legal documents from official sources:", font_size=11, space_after=6)
    scraper_features = [
        "Constitution Scraper: Fetches articles from indiacode.nic.in with fallback to a built-in knowledge base of key constitutional provisions (Articles 14, 15, 19, 21, 21A, 22, 32).",
        "Criminal Acts Scraper: Fetches BNS, BNSS, and BSA sections from legislative.gov.in with comprehensive fallback data covering key sections.",
        "Landmark Cases Scraper: Searches indiankanoon.org for 100 curated landmark Supreme Court cases spanning 1950–2024, including Kesavananda Bharati, Maneka Gandhi, Vishaka, Navtej Johar, Puttaswamy, and more.",
        "Respectful scraping with 2-second delays between requests and proper User-Agent headers.",
        "Saves all scraped data as structured JSON files in data/raw/ directory.",
    ]
    for item in scraper_features:
        add_bullet_point(doc, item)

    # Backend Module 2
    doc.add_heading('9.2.2 Document Indexer Module (complete_indexer.py)', level=3)
    add_formatted_paragraph(doc,
        "Two-stage indexing pipeline:", font_size=11, space_after=6)
    indexer_features = [
        "Stage 1 — Ingestion: Reads PDF, HTML, JSON, and TXT files. Cleans text (removes page numbers, form feeds, BOM characters, bullet artifacts). Normalizes whitespace.",
        "Stage 2 — Chunking: Uses RecursiveCharacterTextSplitter with legal-aware separators (Article, Section, Clause, paragraph boundaries). Default 512-character chunks with 100-character overlap.",
        "Stage 3 — Embedding: Loads SentenceTransformer (all-MiniLM-L6-v2, ~22MB). Batch-embeds chunks (128 per batch) with progress tracking.",
        "Stage 4 — ChromaDB Indexing: Creates a persistent ChromaDB collection ('nyaai_legal') with cosine similarity space. Stores embeddings, documents, and clean metadata.",
        "Stage 5 — BM25 Indexing: Tokenizes chunks with stop-word removal. Builds BM25Okapi index. Pickles index and metadata to disk for fast loading.",
    ]
    for item in indexer_features:
        add_bullet_point(doc, item)

    # Backend Module 3
    doc.add_heading('9.2.3 RAG Server Module (rag_server.py)', level=3)
    add_formatted_paragraph(doc,
        "The core RAG server providing the full retrieval-to-generation pipeline:", font_size=11, space_after=6)
    server_features = [
        "Query Preprocessing: Expands common abbreviations (IPC, CrPC, FIR, RTI), removes filler phrases ('please tell me', 'can you explain').",
        "Hybrid Retrieval: Parallel ChromaDB semantic search + BM25 keyword search with merged, deduplicated results.",
        "Cross-Encoder Re-ranking: Precision re-ranking using sigmoid-normalized cross-encoder scores.",
        "Confidence Gate: Three-tier response routing (Direct/LLM/Insufficient) based on configurable thresholds.",
        "Streaming LLM Generation: Server-Sent Events (NDJSON) streaming from Ollama with metadata, token, and done events.",
        "Semantic Caching: In-memory cache with cosine similarity matching prevents redundant processing.",
        "Health Monitoring: /api/health endpoint reports index counts, model status, and Ollama availability.",
    ]
    for item in server_features:
        add_bullet_point(doc, item)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  10. DATABASE DESIGN
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('10. Database Design', level=1)

    doc.add_heading('10.1 Room Database Schema (Android - v6)', level=2)

    create_table(doc,
        headers=["Table", "Column", "Type", "Description"],
        rows=[
            ["documents (FTS4)", "rowid", "INTEGER (PK)", "Auto-generated row ID"],
            ["", "sourcePath", "TEXT", "Source PDF filename (e.g., bns.pdf)"],
            ["", "content", "TEXT", "Extracted and cleaned legal text chunk"],
            ["chat_sessions", "sessionId", "LONG (PK, Auto)", "Unique session identifier"],
            ["", "title", "TEXT", "Session title (first user message)"],
            ["", "timestamp", "LONG", "Creation timestamp (milliseconds)"],
            ["chat_messages", "id", "LONG (PK, Auto)", "Unique message identifier"],
            ["", "sessionId", "LONG (FK)", "Reference to parent session"],
            ["", "text", "TEXT", "Message content"],
            ["", "isUser", "BOOLEAN", "True = user message, False = AI response"],
            ["", "timestamp", "LONG", "Message timestamp"],
            ["", "feedback", "TEXT (nullable)", "User rating: GOOD, AVERAGE, or POOR"],
            ["training_examples", "id", "LONG (PK, Auto)", "Training example identifier"],
            ["", "question", "TEXT", "Generated citizen question"],
            ["", "answer", "TEXT", "AI-generated simplified answer"],
            ["", "sourcePath", "TEXT", "Source document reference"],
            ["", "legalDomain", "TEXT", "Legal domain category"],
            ["", "reasoningQuality", "INTEGER", "Quality score (1-5)"],
        ],
        col_widths=[1.3, 1.2, 1.3, 2.2]
    )

    doc.add_heading('10.2 Backend Indexes', level=2)

    create_table(doc,
        headers=["Index", "Technology", "Structure", "Size"],
        rows=[
            ["Vector Index", "ChromaDB (Persistent)", "HNSW graph, cosine similarity space", "Collection: 'nyaai_legal'"],
            ["Keyword Index", "BM25Okapi (Pickled)", "TF-IDF inverted index", "bm25_index.pkl"],
            ["Metadata", "Python Pickle", "List of chunk metadata dicts", "chunk_metadata.pkl"],
        ],
        col_widths=[1.2, 1.5, 2.0, 1.3]
    )

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  11. RAG PIPELINE ARCHITECTURE
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('11. RAG Pipeline Architecture', level=1)

    add_formatted_paragraph(doc,
        "The Retrieval-Augmented Generation (RAG) pipeline is the core intelligence layer of NYAAI. It ensures "
        "that AI responses are grounded in actual legal text, preventing hallucination and ensuring citation accuracy.",
        font_size=11, space_after=10)

    rag_text = """
┌──────────────┐
│  User Query  │
└──────┬───────┘
       │
       ▼
┌──────────────────┐
│  Preprocessing   │ ← Abbreviation expansion, filler removal
└──────┬───────────┘
       │
       ├──────────────────┐
       ▼                  ▼
┌──────────────┐  ┌──────────────┐
│  ChromaDB    │  │    BM25      │
│  Vector      │  │  Keyword     │
│  Search      │  │  Search      │
│  (Top-10)    │  │  (Top-10)    │
└──────┬───────┘  └──────┬───────┘
       │                  │
       └────────┬─────────┘
                ▼
       ┌────────────────┐
       │  Merge & Dedup │ ← Hybrid boost for dual-retrieved chunks
       └────────┬───────┘
                ▼
       ┌────────────────────┐
       │  Cross-Encoder     │ ← Precision re-ranking (Top-3)
       │  Re-ranking        │
       └────────┬───────────┘
                ▼
       ┌────────────────────┐
       │  Confidence Gate   │
       └──┬─────┬───────┬──┘
          │     │       │
    >0.75 │     │0.4-   │<0.4
          │     │0.75   │
          ▼     ▼       ▼
     ┌────────┐┌─────┐┌──────────────┐
     │Direct  ││ LLM ││ Insufficient │
     │Answer  ││(Gen)││ Information  │
     └────────┘└─────┘└──────────────┘
"""
    p = doc.add_paragraph()
    run = p.add_run(rag_text)
    run.font.size = Pt(8.5)
    run.font.name = 'Consolas'

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  12. API DESIGN
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('12. API Design', level=1)

    doc.add_heading('12.1 Backend REST API Endpoints', level=2)

    create_table(doc,
        headers=["Endpoint", "Method", "Description", "Request Body"],
        rows=[
            ["/api/chat", "POST", "Full RAG pipeline: retrieval → gate → LLM/direct", '{"query": "...", "language": "en", "stream": true}'],
            ["/api/query", "POST", "Retrieval-only (debugging)", '{"query": "...", "top_k": 3}'],
            ["/api/health", "GET", "Server health check with model status", "None"],
            ["/api/models", "GET", "List available Ollama models", "None"],
            ["/docs", "GET", "Interactive Swagger API docs", "None"],
        ],
        col_widths=[1.0, 0.7, 2.0, 2.3]
    )

    doc.add_heading('12.2 Gemini API Integration (Mobile)', level=2)
    add_formatted_paragraph(doc,
        "The mobile app directly calls the Google Gemini REST API:", font_size=11, space_after=6)
    add_bullet_point(doc, "", bold_prefix="Endpoint: ")
    p = doc.paragraphs[-1]
    run = p.add_run("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
    run.font.size = Pt(10)
    run.font.name = 'Consolas'

    add_bullet_point(doc, "JSON payload with structured contents array", bold_prefix="Request Format: ")
    add_bullet_point(doc, "Parsed from candidates[0].content.parts[0].text", bold_prefix="Response Parsing: ")
    add_bullet_point(doc, "Returns trained examples or raw PDF text when API is unreachable", bold_prefix="Fallback Strategy: ")

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  13. USER INTERFACE DESIGN
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('13. User Interface Design', level=1)

    add_formatted_paragraph(doc,
        "NYAAI features a modern, clean user interface built entirely with Jetpack Compose and Material 3 Design. "
        "The UI prioritizes readability and accessibility for users who may not be tech-savvy.",
        font_size=11, space_after=10)

    doc.add_heading('13.1 Screen Flow', level=2)

    flow_text = """
App Launch → Splash Screen → Welcome Screen (Legal Disclaimer)
    → Login Screen (Firebase Google Sign-In)
    → Main Screen (Bottom Navigation)
        ├── Chat Tab (AI Legal Assistant)
        │     ├── Message Input
        │     ├── AI Response with Feedback (👍/👎)
        │     └── Legal SOS Shortcut
        ├── Settings Tab
        │     ├── Theme (Light/Dark/System)
        │     ├── UI Language Selection
        │     ├── AI Response Language
        │     ├── Notifications Toggle
        │     ├── Chat History Management
        │     └── Account (Sign Out / Delete)
        └── About Tab
              ├── App Version & Build Info
              ├── Developer Credits
              └── Legal Notices
"""
    p = doc.add_paragraph()
    run = p.add_run(flow_text)
    run.font.size = Pt(9)
    run.font.name = 'Consolas'

    doc.add_heading('13.2 Design Principles', level=2)
    design_principles = [
        ("Material 3 Design System: ", "Consistent use of Material You dynamic theming with proper color roles (primary, secondary, surface, error)."),
        ("Dark/Light Mode: ", "Full theme support with system-aware automatic switching."),
        ("Accessibility: ", "Large touch targets, readable font sizes, proper contrast ratios."),
        ("Responsive Layout: ", "Compose constraint-aware layouts that adapt to different screen sizes."),
        ("Legal SOS: ", "One-tap access to emergency helpline numbers directly from the chat screen header."),
    ]
    for prefix, text in design_principles:
        add_bullet_point(doc, text, bold_prefix=prefix)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  14. TESTING & VALIDATION
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('14. Testing & Validation', level=1)

    create_table(doc,
        headers=["Test Category", "Test Description", "Status"],
        rows=[
            ["Unit Testing", "Room DAO queries, FTS4 search accuracy", "✓ Passed"],
            ["Integration Testing", "PDF extraction → Room indexing pipeline", "✓ Passed"],
            ["AI Response Quality", "Query accuracy against known legal provisions", "✓ Validated"],
            ["API Connectivity", "Gemini REST API call/response cycle", "✓ Passed"],
            ["Offline Fallback", "App behavior without internet connectivity", "✓ Passed"],
            ["Training Pipeline", "Autonomous 1000-query generation cycle", "✓ Passed"],
            ["Feedback System", "GOOD/AVERAGE/POOR rating persistence", "✓ Passed"],
            ["Auth Flow", "Firebase Google Sign-In + session persistence", "✓ Passed"],
            ["Theme Switching", "Light/Dark/System theme transitions", "✓ Passed"],
            ["Language Support", "Multilingual AI responses (EN, HI, BN, TE, TA)", "✓ Validated"],
            ["Backend RAG", "Hybrid retrieval accuracy on legal queries", "✓ Validated"],
            ["Confidence Gate", "Correct mode routing (Direct/LLM/Insufficient)", "✓ Passed"],
            ["APK Build", "Release APK generation and installation", "✓ Passed"],
        ],
        col_widths=[1.5, 3.0, 1.0]
    )

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  15. ADVANTAGES & LIMITATIONS
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('15. Advantages & Limitations', level=1)

    doc.add_heading('15.1 Advantages', level=2)
    advantages = [
        "Free and accessible legal information for all Indian citizens.",
        "AI-powered natural language understanding — no need to know legal terminology.",
        "Updated with India's new criminal codes (BNS/BNSS/BSA 2023), unlike most existing platforms.",
        "Offline-first architecture ensures basic functionality without internet.",
        "Self-improving system through autonomous training and user feedback reinforcement.",
        "Confidence-gated responses prevent AI hallucination and misinformation.",
        "Multi-language support bridges India's linguistic diversity.",
        "Legal SOS feature provides immediate access to emergency helplines.",
        "Privacy-conscious design — legal queries are processed without storing personal data on external servers.",
        "Hybrid RAG pipeline (semantic + keyword) achieves superior retrieval accuracy compared to single-method approaches.",
    ]
    for item in advantages:
        add_bullet_point(doc, item)

    doc.add_heading('15.2 Limitations', level=2)
    limitations = [
        "AI-generated responses are informational only and do not constitute legal advice. A disclaimer is prominently displayed.",
        "On-device PDF processing increases initial app load time (~15-30 seconds on first launch).",
        "Training pipeline requires active internet and consumes API quota (1,000 Gemini API calls).",
        "The current version does not support real-time case law updates or eCourts integration.",
        "Voice input is not yet implemented (placeholder screen exists).",
        "The app is currently Android-only; iOS and web versions are planned for future releases.",
        "Backend RAG server requires a machine with sufficient RAM for model loading (~2GB for embeddings + re-ranker).",
    ]
    for item in limitations:
        add_bullet_point(doc, item)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  16. FUTURE ENHANCEMENTS
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('16. Future Enhancements', level=1)

    enhancements = [
        ("Voice-Based Legal Query: ", "Integration of Speech-to-Text (Google ML Kit / Whisper) for voice input, making the app accessible to users who cannot type."),
        ("Document Drafting: ", "AI-assisted generation of common legal documents — FIR complaints, RTI applications, consumer complaints, and bail applications."),
        ("eCourts Integration: ", "Real-time case status tracking by connecting to the eCourts services portal for live case updates."),
        ("Lawyer Directory: ", "Location-based lawyer discovery with specialization filters and appointment booking."),
        ("Cross-Platform Release: ", "iOS app using Kotlin Multiplatform Mobile (KMM) and a Progressive Web App (PWA) for browser access."),
        ("Vernacular Legal Database: ", "Expansion of the legal document corpus to include state-specific laws, consumer protection, labor laws, and family law provisions in regional languages."),
        ("Conversational Memory: ", "Long-term conversation context window using vector-based session memory for follow-up questions."),
        ("Legal Rights Quiz: ", "Gamified legal literacy module teaching citizens their rights through interactive scenarios."),
        ("Community Forum: ", "Moderated peer-to-peer legal discussion forum with AI-assisted moderation."),
        ("Analytics Dashboard: ", "Anonymized analytics showing most-queried legal topics, helping identify areas of legal illiteracy for policy intervention."),
    ]
    for prefix, text in enhancements:
        add_bullet_point(doc, text, bold_prefix=prefix)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  17. CONCLUSION
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('17. Conclusion', level=1)

    add_formatted_paragraph(doc,
        "NYAAI represents a significant step forward in democratizing legal information access in India. "
        "By combining the power of Retrieval-Augmented Generation (RAG) with modern mobile development practices, "
        "the project delivers an intelligent, self-improving legal assistant that can meaningfully serve the "
        "information needs of ordinary Indian citizens.",
        font_size=11, space_after=8)

    add_formatted_paragraph(doc,
        "The dual-architecture approach — an offline-capable Android application paired with an advanced Python RAG "
        "backend — ensures that NYAAI functions reliably across varying connectivity conditions while maintaining "
        "high response quality through hybrid retrieval, cross-encoder re-ranking, and confidence-gated generation.",
        font_size=11, space_after=8)

    add_formatted_paragraph(doc,
        "Key technical achievements of this project include:",
        font_size=11, space_after=4, bold=True)

    achievements = [
        "Successful on-device processing of 6,400+ pages of legal documents (COI, BNS, BNSS, BSA).",
        "Autonomous generation of 1,000 legal training Q&A pairs through structured LLM reasoning cycles.",
        "Implementation of a 5-stage RAG pipeline (Embed → Retrieve → Re-rank → Gate → Generate) with zero-hallucination safeguards.",
        "A comprehensive legal scraper covering the Constitution, three new criminal codes, and 100 landmark Supreme Court cases.",
        "A user feedback loop that enables genuine reinforcement learning at the application level.",
    ]
    for item in achievements:
        add_bullet_point(doc, item)

    add_formatted_paragraph(doc,
        "NYAAI stands as proof that advanced AI techniques can be responsibly deployed to address real societal "
        "challenges. As the project evolves with voice input, document drafting, and cross-platform support, it "
        "has the potential to become a comprehensive legal empowerment tool for India's 1.4 billion citizens.",
        font_size=11, space_after=8, space_before=8)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════
    #  18. REFERENCES
    # ══════════════════════════════════════════════════════════════════

    doc.add_heading('18. References', level=1)

    references = [
        "[1] Constitution of India, Ministry of Law and Justice, https://www.indiacode.nic.in",
        "[2] Bharatiya Nyaya Sanhita (BNS) 2023, The Gazette of India, https://legislative.gov.in",
        "[3] Bharatiya Nagarik Suraksha Sanhita (BNSS) 2023, The Gazette of India, https://legislative.gov.in",
        "[4] Bharatiya Sakshya Adhiniyam (BSA) 2023, The Gazette of India, https://legislative.gov.in",
        "[5] Lewis, P., et al. (2020). \"Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks.\" NeurIPS 2020.",
        "[6] Reimers, N., & Gurevych, I. (2019). \"Sentence-BERT: Sentence Embeddings using Siamese BERT-Networks.\" EMNLP 2019.",
        "[7] Robertson, S.E., & Zaragoza, H. (2009). \"The Probabilistic Relevance Framework: BM25 and Beyond.\" Foundations and Trends in Information Retrieval.",
        "[8] Google DeepMind. (2024). \"Gemini: A Family of Highly Capable Multimodal Models.\" Technical Report.",
        "[9] Android Developers. \"Jetpack Compose.\" https://developer.android.com/jetpack/compose",
        "[10] Android Developers. \"Room Persistence Library with FTS4.\" https://developer.android.com/training/data-storage/room",
        "[11] ChromaDB Documentation. https://docs.trychroma.com/",
        "[12] FastAPI Documentation. https://fastapi.tiangolo.com/",
        "[13] Ollama. \"Run large language models locally.\" https://ollama.ai/",
        "[14] Indian Kanoon. \"Free Indian Law search engine.\" https://indiankanoon.org/",
        "[15] Firebase Authentication Documentation. https://firebase.google.com/docs/auth",
    ]

    for ref in references:
        p = doc.add_paragraph()
        p.paragraph_format.space_after = Pt(3)
        p.paragraph_format.left_indent = Cm(1)
        run = p.add_run(ref)
        run.font.size = Pt(10)
        run.font.name = 'Calibri'

    # ── Save Document ──
    output_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "NYAAI_Project_Synopsis_Report.docx")
    doc.save(output_path)
    print(f"\n[OK] Synopsis report saved to: {output_path}")
    print(f"     File size: {os.path.getsize(output_path) / 1024:.1f} KB")
    return output_path


if __name__ == "__main__":
    generate_synopsis()
