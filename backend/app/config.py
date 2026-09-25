from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import model_validator
from typing import Optional

class Settings(BaseSettings):
    DATABASE_URL: str = "postgresql+asyncpg://nyaai:nyaai@localhost:5432/nyaai"
    QDRANT_URL: str = "http://localhost:6333"
    MINIO_ENDPOINT: str = "localhost:9000"
    MINIO_ACCESS_KEY: str = "minioadmin"
    MINIO_SECRET_KEY: str = "minioadmin"
    
    LLM_PROVIDER: str = "gemini"
    GEMINI_API_KEY: Optional[str] = None
    ANTHROPIC_API_KEY: Optional[str] = None
    OLLAMA_BASE_URL: str = "http://localhost:11434"
    OLLAMA_MODEL: str = "gemma2:2b"
    
    EMBEDDING_MODEL: str = "sentence-transformers/all-MiniLM-L6-v2"
    RERANKER_MODEL: str = "cross-encoder/ms-marco-MiniLM-L-6-v2"

    ENVIRONMENT: str = ""  # Must be explicitly set: "development", "test", "production"
    ALLOW_IN_MEMORY_FALLBACK: bool = True
    AUTH_SECRET_KEY: Optional[str] = None
    CORS_ORIGINS: Optional[str] = None

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    @model_validator(mode="after")
    def validate_environment_and_security(self):
        valid_envs = {"development", "test", "production"}
        if not self.ENVIRONMENT or self.ENVIRONMENT not in valid_envs:
            raise ValueError(
                f"ENVIRONMENT must be explicitly set to 'development', 'test', or 'production'. Found: '{self.ENVIRONMENT}'"
            )

        if self.ENVIRONMENT != "test":
            if not self.AUTH_SECRET_KEY or self.AUTH_SECRET_KEY == "nyaai_production_hmac_secret_key_2026":
                raise ValueError(
                    "AUTH_SECRET_KEY environment variable is required and cannot be empty or the deprecated default string in non-test environments."
                )
        else:
            if not self.AUTH_SECRET_KEY:
                self.AUTH_SECRET_KEY = "test_environment_ephemeral_secret_key_2026"

        return self

settings = Settings()
