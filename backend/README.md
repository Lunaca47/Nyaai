# NYAAI V2 — Statutory & RAG Backend

Production FastAPI backend powering the NYAAI Android Application and the Web Client (`docs/`).

Provides:
- **Statutory Retrieval Service**: Hybrid search combining Sparse BM25 + Dense Semantic Scoring (all-MiniLM-L6-v2) + Reciprocal Rank Fusion ($k=60$) over 1,932 verified statutory provisions.
- **Citation Verification Hard Gate**: Pre-generation and post-generation gate analyzing legal claims against ground truth statutory codex, enforcing colonial law migration (IPC/CrPC/IEA to BNS/BNSS/BSA), and flagging ungrounded claims.
- **Server-Side Generation**: Isolated LLM generation (Gemini, Anthropic, Ollama) preventing API key exposure to clients.
- **Matter Access Control**: Cryptographically signed token authentication (HMAC-SHA256 / PyJWT) with access-controlled matter storage and AES-256-GCM encryption.
- **Rate Limiting**: Sliding window protection against automated abuse on browser-facing endpoints (`/api/v1/generate` and `/api/v1/retrieval/search`).

---

## Deployment & Hosting Guide

## Deployment & Hosting Guide

The web client is hosted as a static site on GitHub Pages (`https://lunaca47.github.io/Nyaai/`). GitHub Pages serves static files only and cannot execute server-side Python. To power the web client or Android remote sync in production, deploy this backend container to a cloud hosting platform.

---

### Operator vs Autonomous System Boundaries

To maintain clear operational responsibilities, cloud deployment tasks are divided as follows:

| Responsibility Area | Handled by Autonomous AI Agent | Operator / Human Administrator Responsibility |
|---------------------|--------------------------------|----------------------------------------------|
| **Containerization** | Multi-stage `Dockerfile`, entrypoints, and healthcheck endpoints | Provisioning cloud infrastructure (GCP Cloud Run, AWS App Runner, Fly.io, or Render) |
| **Configuration Code** | `.env.example`, settings schema, and CORS middleware defaults | Supplying production secrets (`AUTH_SECRET_KEY`, `GEMINI_API_KEY`) in cloud dashboard |
| **Statutory Data** | Codex consolidation, semantic deduplication, and embedding sidecars | Retaining off-site backups of production matter database |
| **Networking & DNS** | Listening on `0.0.0.0:${PORT:-8000}`, `/health` probes | Purchasing domains, configuring DNS A/CNAME records, and pointing public endpoints |
| **SSL/TLS Security** | Enforcing HTTPS redirection headers in production environment | Provisioning TLS/SSL certificates (automatic on Cloud Run/Render/Fly.io) |

---

### Environment Variables Reference

All runtime configuration is managed through environment variables:

| Variable | Requirement | Default | Description |
|----------|-------------|---------|-------------|
| `ENVIRONMENT` | **Required** | `development` | Set to `production` in live deployments (enforces strict HMAC secret checks and secure CORS). |
| `AUTH_SECRET_KEY` | **Required** | None | 32+ character high-entropy secret used for HMAC-SHA256 guest & matter access token signing. Generate with: `openssl rand -hex 32`. |
| `CORS_ORIGINS` | Optional | `https://lunaca47.github.io,http://localhost:8000` | Comma-separated list of allowed origins permitted to call the backend APIs from web browsers. |
| `PORT` | Optional | `8000` | Port on which Uvicorn listens. (Cloud Run / Render automatically inject this). |
| `LLM_PROVIDER` | Optional | `gemini` | Server-side model engine: `gemini`, `anthropic`, or `ollama`. |
| `GEMINI_API_KEY` | Optional | None | Google Gemini API key for server-side grounded generation. Stored exclusively on backend. |
| `ANTHROPIC_API_KEY` | Optional | None | Anthropic Claude API key (if `LLM_PROVIDER=anthropic`). |
| `OLLAMA_BASE_URL` | Optional | `http://localhost:11434` | Endpoint for local/self-hosted Ollama model instances. |
| `EMBEDDING_MODEL` | Optional | `sentence-transformers/all-MiniLM-L6-v2` | SentenceTransformer model identifier for semantic retrieval. |
| `ALLOW_IN_MEMORY_FALLBACK` | Optional | `True` | Permits falling back to precomputed `.npy` statutory embeddings when vector DB is unlinked. |

---

### 1. Cloud Run Deployment (Google Cloud)
```bash
# Build and tag image using Google Cloud Build
gcloud builds submit --tag gcr.io/PROJECT_ID/nyaai-backend:v2 backend/

# Deploy container to Cloud Run
gcloud run deploy nyaai-backend \
  --image gcr.io/PROJECT_ID/nyaai-backend:v2 \
  --platform managed \
  --region asia-south1 \
  --allow-unauthenticated \
  --set-env-vars ENVIRONMENT=production,AUTH_SECRET_KEY=SECURE_HEX_KEY,CORS_ORIGINS=https://lunaca47.github.io \
  --set-secrets GEMINI_API_KEY=nyaai-gemini-key:latest
```

### 2. Render / Fly.io / Railway Deployment
1. **Dockerfile Path**: `backend/Dockerfile` | **Context**: `backend/`.
2. **Environment Variables**: Populate `ENVIRONMENT=production`, `AUTH_SECRET_KEY`, `CORS_ORIGINS`, and LLM keys.
3. **Health Check Path**: `/api/v1/health` (HTTP 200).
4. **Custom Domain / DNS**: Add a CNAME record in your DNS provider pointing your domain (e.g. `api.nyaai.org`) to the assigned service URL. Update `docs/config.js` to point to your new backend URL.

### 3. Local Development Setup
```bash
cd backend
python -m venv venv
# Windows:
.\venv\Scripts\activate
# Linux/macOS:
source venv/bin/activate

pip install -r requirements.txt
cp .env.example .env
# Configure ENVIRONMENT=development and AUTH_SECRET_KEY in .env

uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

---

## API Reference


### Health
- `GET /health` & `GET /api/v1/health`
  Returns service status and active environment.

### Authentication
- `POST /api/v1/matters/auth/guest-token`
  Issues a cryptographically signed HMAC token for anonymous or guest web sessions.

### Statutory Retrieval
- `POST /api/v1/retrieval/search`
  **Rate Limit**: 60 requests/minute per IP/token.
  Payload:
  ```json
  {
    "query": "cheque bounce demand notice 15 days",
    "limit": 3,
    "jurisdiction": "central"
  }
  ```

### Generation
- `POST /api/v1/generate`
  **Rate Limit**: 20 requests/minute per IP/token.
  Payload:
  ```json
  {
    "prompt": "Explain tenant rights regarding security deposit return under Model Tenancy Act.",
    "temperature": 0.2
  }
  ```

### Verification Hard Gate
- `POST /api/v1/verify`
  Payload:
  ```json
  {
    "response_text": "Under Section 138 of Negotiable Instruments Act...",
    "retrieved_sources": [
      {
        "id": "nia_1881_s138",
        "act": "Negotiable Instruments Act, 1881",
        "section": "138",
        "title": "Dishonour of cheque for insufficiency, etc., of funds in the account",
        "content": "..."
      }
    ]
  }
  ```
  Returns:
  ```json
  {
    "action": "PASSED",
    "is_grounded": true,
    "verified_sources": [...],
    "ungrounded_citations": [],
    "model_laws": [],
    "repealed_citations": [],
    "annotated_response": "..."
  }
  ```
