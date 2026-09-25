import os
import json
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.database import engine
from app.routers import matters, intake, generation, verification, retrieval
from app.retrieval.hybrid_search import hybrid_search_engine, DocumentChunk

logger = logging.getLogger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Test DB connection gracefully (skip during test to avoid network timeout)
    if settings.ENVIRONMENT != "test":
        try:
            async with engine.begin() as conn:
                pass
            logger.info("Database connection established.")
        except Exception as e:
            logger.warning(f"Database connection could not be established: {e}. Running with memory stores.")
    else:
        logger.info("Test environment active: skipping DB probe, running with in-memory stores.")

    # Load ingested statutory codex if available
    codex_path = os.path.join("backend", "data", "statutory_codex.json")
    if os.path.exists(codex_path):
        try:
            with open(codex_path, "r", encoding="utf-8") as f:
                data = json.load(f)
                loaded_chunks = [
                    DocumentChunk(
                        id=d["id"],
                        act=d["act"],
                        section=d["section"],
                        title=d["title"],
                        content=d["content"],
                        jurisdiction=d.get("jurisdiction", "central"),
                        status=d.get("status", "in_force"),
                        effective_date=d.get("effective_date", "2024-07-01"),
                    )
                    for d in data
                ]
                hybrid_search_engine.add_documents(loaded_chunks)
                logger.info(f"Loaded {len(loaded_chunks)} documents from statutory codex into retrieval engine.")
        except Exception as e:
            logger.warning(f"Could not load statutory codex: {e}")

    yield
    try:
        await engine.dispose()
    except Exception:
        pass

def get_cors_origins() -> list[str]:
    origins = [
        "https://lunaca47.github.io",
        "http://localhost:8000",
        "http://127.0.0.1:8000",
        "http://localhost:8089",
        "http://127.0.0.1:8089",
        "http://localhost:3000",
        "http://localhost:8080",
        "http://localhost:5500",
        "http://127.0.0.1:5500",
    ]
    env_origins = getattr(settings, "CORS_ORIGINS", None) or os.getenv("CORS_ORIGINS", "")
    if env_origins:
        for origin in env_origins.split(","):
            origin = origin.strip()
            if origin and origin not in origins:
                origins.append(origin)
    return origins

app = FastAPI(title="NYAAI API", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=get_cors_origins(),
    allow_origin_regex=r"^https?://(localhost|127\.0\.0\.1)(:\d+)?$",
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(matters.router, prefix="/api/v1/matters", tags=["matters"])
app.include_router(intake.router, prefix="/api/v1/intake", tags=["intake"])
app.include_router(generation.router, prefix="/api/v1/generate", tags=["generation"])
app.include_router(verification.router, prefix="/api/v1/verify", tags=["verification"])
app.include_router(retrieval.router, prefix="/api/v1/retrieval", tags=["retrieval"])

@app.get("/health")
@app.get("/api/v1/health")
async def health_check():
    return {
        "status": "ok",
        "version": "2.0.0",
        "service": "nyaai-backend",
        "environment": getattr(settings, "ENVIRONMENT", "unknown"),
    }

