"""
Provider-agnostic LLM client.

Every call site in NYAAI uses this module — never a provider SDK directly.
Swap the active provider via the LLM_PROVIDER env var (gemini | claude | ollama).
"""

import json
import logging
from typing import Any, Optional, Type

from pydantic import BaseModel

from app.config import settings

logger = logging.getLogger(__name__)


class LLMClient:
    """Thin wrapper that routes generation calls to the configured provider."""

    # ------------------------------------------------------------------
    # Public interface
    # ------------------------------------------------------------------

    async def generate(
        self,
        prompt: str,
        system: str = "",
        temperature: float = 0.3,
        json_mode: bool = False,
    ) -> str:
        """Free-text (or raw-JSON) generation."""
        provider = settings.LLM_PROVIDER.lower()
        if provider == "gemini":
            return await self._gemini_generate(prompt, system, temperature, json_mode)
        elif provider == "claude":
            return await self._claude_generate(prompt, system, temperature, json_mode)
        elif provider == "ollama":
            return await self._ollama_generate(prompt, system, temperature, json_mode)
        else:
            raise ValueError(f"Unknown LLM provider: {provider}")

    async def generate_structured(
        self,
        system_prompt: str,
        user_prompt: str,
        response_model: Type[BaseModel],
        temperature: float = 0.1,
    ) -> BaseModel:
        """Generate and parse into a Pydantic model.

        Instructs the LLM to reply with JSON matching *response_model*'s
        schema, then validates the response.
        """
        schema_json = json.dumps(response_model.model_json_schema(), indent=2)
        augmented_system = (
            f"{system_prompt}\n\n"
            f"You MUST respond with valid JSON matching this schema:\n"
            f"{schema_json}\n"
            f"Do not include any text outside the JSON object."
        )
        raw = await self.generate(
            prompt=user_prompt,
            system=augmented_system,
            temperature=temperature,
            json_mode=True,
        )
        # Strip markdown fences if the model wraps its reply
        raw = raw.strip()
        if raw.startswith("```"):
            raw = raw.split("\n", 1)[1] if "\n" in raw else raw[3:]
            if raw.endswith("```"):
                raw = raw[:-3]
            raw = raw.strip()
        parsed = json.loads(raw)
        return response_model.model_validate(parsed)

    async def generate_text(
        self,
        system_prompt: str,
        user_prompt: str,
        temperature: float = 0.3,
    ) -> str:
        """Convenience alias matching the intake engine's call pattern."""
        return await self.generate(
            prompt=user_prompt,
            system=system_prompt,
            temperature=temperature,
            json_mode=False,
        )

    # ------------------------------------------------------------------
    # Provider implementations
    # ------------------------------------------------------------------

    async def _gemini_generate(
        self, prompt: str, system: str, temperature: float, json_mode: bool
    ) -> str:
        import google.generativeai as genai

        genai.configure(api_key=settings.GEMINI_API_KEY)
        model = genai.GenerativeModel(
            "gemini-2.5-flash",
            system_instruction=system or None,
            generation_config=genai.GenerationConfig(
                temperature=temperature,
                response_mime_type="application/json" if json_mode else "text/plain",
            ),
        )
        response = await model.generate_content_async(prompt)
        return response.text

    async def _claude_generate(
        self, prompt: str, system: str, temperature: float, json_mode: bool
    ) -> str:
        import anthropic

        client = anthropic.AsyncAnthropic(api_key=settings.ANTHROPIC_API_KEY)
        messages = [{"role": "user", "content": prompt}]
        if json_mode:
            messages[0]["content"] = (
                f"{prompt}\n\nRespond ONLY with valid JSON, no other text."
            )
        response = await client.messages.create(
            model="claude-sonnet-4-20250514",
            max_tokens=4096,
            system=system or "",
            messages=messages,
            temperature=temperature,
        )
        return response.content[0].text

    async def _ollama_generate(
        self, prompt: str, system: str, temperature: float, json_mode: bool
    ) -> str:
        import httpx

        payload: dict[str, Any] = {
            "model": settings.OLLAMA_MODEL,
            "prompt": prompt,
            "system": system or "",
            "stream": False,
            "options": {"temperature": temperature},
        }
        if json_mode:
            payload["format"] = "json"

        async with httpx.AsyncClient(timeout=120.0) as client:
            resp = await client.post(
                f"{settings.OLLAMA_BASE_URL}/api/generate", json=payload
            )
            resp.raise_for_status()
            return resp.json()["response"]


# Module-level singleton — import this everywhere.
llm_client = LLMClient()
