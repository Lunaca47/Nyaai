# NYAAI RAG Backend — Setup Guide

## Overview

This is the Python RAG backend for the NYAAI Indian Legal AI Assistant. It provides:

- **Legal Scraper**: Fetches Constitution, BNS/BNSS/BSA, and 100 landmark cases
- **Indexer**: Chunks, embeds, and indexes documents into ChromaDB + BM25
- **RAG Server**: FastAPI server with hybrid retrieval, cross-encoder re-ranking, and Ollama LLM generation

## Prerequisites

- **Python 3.10+**
- **Ollama** (for local LLM) — [Install Ollama](https://ollama.ai/download)

## Quick Start

### 1. Setup Python Environment

```bash
cd backend

# Create virtual environment
python -m venv venv

# Activate (Windows)
.\venv\Scripts\activate

# Activate (macOS/Linux)
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

### 2. Configure Environment

```bash
cp .env.example .env
# Edit .env if needed (defaults work fine for local development)
```

### 3. Install Ollama & Pull Model

```bash
# Install Ollama from https://ollama.ai/download
# Then pull the model:
ollama pull gemma2:2b

# Optional fallback model:
ollama pull phi3:mini
```

### 4. Scrape Legal Data

```bash
# Scrape all sources (Constitution, BNS/BNSS/BSA, 100 cases)
python legal_scraper.py --all

# Or scrape selectively:
python legal_scraper.py --constitution
python legal_scraper.py --acts
python legal_scraper.py --cases
```

### 5. Build Indexes

```bash
# Index scraped data
python complete_indexer.py

# Also include the Flutter app's existing dataset:
python complete_indexer.py --include-app-data
```

### 6. Start the RAG Server

```bash
# Start with auto-reload (development)
python rag_server.py

# Or with uvicorn directly:
uvicorn rag_server:app --host 0.0.0.0 --port 8000 --reload
```

### 7. Test the Server

```bash
# Health check
curl http://localhost:8000/api/health

# Query (retrieval only)
curl -X POST http://localhost:8000/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What are my rights if arrested?"}'

# Full chat (streaming)
curl -X POST http://localhost:8000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"query": "What is Article 21?", "stream": true}'
```

## Flutter App Configuration

The Flutter app connects to the RAG server at `http://localhost:8000` by default.

For testing on a physical device, use your machine's LAN IP:
```
http://192.168.x.x:8000
```

Update the server URL in `lib/services/rag_service_v2.dart`.

## API Reference

| Endpoint | Method | Description |
|-----|--------|-------------|
| `/api/chat` | POST | Full pipeline: retrieval → gate → LLM |
| `/api/query` | POST | Retrieval only (debugging) |
| `/api/health` | GET | Server status check |
| `/api/models` | GET | Available Ollama models |
| `/docs` | GET | Interactive API docs (Swagger) |

## Architecture

```
User Query → Embed (MiniLM) → ChromaDB Top-10 + BM25 Top-10
    → Merge & Dedup → Cross-Encoder Re-rank → Top-3
    → Confidence Gate:
        > 0.75: Direct answer from chunks
        0.4-0.75: Pass to Ollama LLM with grounding
        < 0.4: "Insufficient information"
    → Stream response to Flutter
```

## File Structure

```
backend/
├── config.py              # Configuration (env vars)
├── legal_scraper.py       # Scrapes legal sources
├── complete_indexer.py    # Builds ChromaDB + BM25 indexes
├── rag_server.py          # FastAPI RAG server
├── requirements.txt       # Python dependencies
├── .env.example           # Environment template
├── data/                  # Raw legal documents
│   └── raw/               # Scraped output
└── nyaai_index/           # Generated indexes
    ├── chroma/            # ChromaDB persistence
    ├── bm25_index.pkl     # BM25 index
    └── chunk_metadata.pkl # Chunk metadata
```
