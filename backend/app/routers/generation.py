import logging
from fastapi import APIRouter, HTTPException, Depends
from app.config import settings
from app.llm_client import llm_client
from app.schemas.schemas import GenerateRequest, GenerateResponse
from app.rate_limiter import limit_generate

logger = logging.getLogger(__name__)

router = APIRouter()

@router.post("", response_model=GenerateResponse, dependencies=[Depends(limit_generate)])
async def generate(request: GenerateRequest):
    """
    Server-side LLM generation endpoint.
    Keeps provider API keys securely on the server without shipping them in the client APK.
    """
    try:
        text = await llm_client.generate(
            prompt=request.prompt,
            system=request.system_prompt or "",
            temperature=request.temperature,
            json_mode=request.json_mode,
        )
        model_name = "gemini-2.5-flash" if settings.LLM_PROVIDER == "gemini" else settings.OLLAMA_MODEL
        return GenerateResponse(
            text=text,
            provider=settings.LLM_PROVIDER,
            model=model_name,
        )
    except Exception as e:
        logger.error(f"Generation error: {e}", exc_info=True)
        raise HTTPException(
            status_code=503,
            detail=f"Model generation failed: {str(e)}"
        )
