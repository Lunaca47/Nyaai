from unittest.mock import AsyncMock, patch
import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app

@pytest.mark.asyncio
async def test_generate_endpoint_success():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        with patch("app.routers.generation.llm_client.generate", new_callable=AsyncMock) as mock_gen:
            mock_gen.return_value = "Under Section 138 of NI Act, a 15-day notice is required."
            resp = await ac.post("/api/v1/generate", json={
                "prompt": "What is the procedure for cheque bounce?",
                "system_prompt": "You are a legal assistant.",
                "temperature": 0.2,
                "json_mode": False
            })
            assert resp.status_code == 200
            data = resp.json()
            assert "15-day notice" in data["text"]
            assert data["provider"] == "gemini"
            mock_gen.assert_called_once_with(
                prompt="What is the procedure for cheque bounce?",
                system="You are a legal assistant.",
                temperature=0.2,
                json_mode=False
            )

@pytest.mark.asyncio
async def test_generate_endpoint_error_handling():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        with patch("app.routers.generation.llm_client.generate", new_callable=AsyncMock) as mock_gen:
            mock_gen.side_effect = RuntimeError("API key invalid or quota exceeded")
            resp = await ac.post("/api/v1/generate", json={
                "prompt": "Test error prompt",
            })
            assert resp.status_code == 503
            assert "Model generation failed" in resp.json()["detail"]
