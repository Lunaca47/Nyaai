import pytest
import time
from fastapi import FastAPI, Request, Depends
from fastapi.testclient import TestClient
from app.rate_limiter import SlidingWindowRateLimiter, limit_search, limit_generate, generate_rate_limiter, search_rate_limiter
from app.main import app

client = TestClient(app)

def test_sliding_window_rate_limiter_unit():
    limiter = SlidingWindowRateLimiter(requests_per_minute=3, scope="test_unit")
    
    # Mock request
    class MockRequest:
        def __init__(self, ip="192.168.1.1", auth=None):
            self.headers = {}
            if auth:
                self.headers["Authorization"] = auth
            self.client = type("Client", (), {"host": ip})()

    req = MockRequest()

    # First 3 requests should pass
    limiter.check(req)
    limiter.check(req)
    limiter.check(req)

    # 4th request should raise HTTPException 429
    with pytest.raises(Exception) as exc_info:
        limiter.check(req)

    assert "429" in str(exc_info.value)
    assert hasattr(exc_info.value, "headers")
    assert "Retry-After" in exc_info.value.headers
    retry_after = int(exc_info.value.headers["Retry-After"])
    assert 1 <= retry_after <= 60

    # Different IP should not be blocked
    req2 = MockRequest(ip="192.168.1.2")
    limiter.check(req2)  # Should pass without error

    # Reset should clear counters
    limiter.reset()
    limiter.check(req)  # Should pass again

def test_health_endpoints():
    r1 = client.get("/health")
    assert r1.status_code == 200
    assert r1.json()["status"] == "ok"

    r2 = client.get("/api/v1/health")
    assert r2.status_code == 200
    assert r2.json()["status"] == "ok"
    assert r2.json()["service"] == "nyaai-backend"
    assert "environment" in r2.json()

def test_cors_headers_github_pages():
    response = client.options(
        "/api/v1/retrieval/search",
        headers={
            "Origin": "https://lunaca47.github.io",
            "Access-Control-Request-Method": "POST",
            "Access-Control-Request-Headers": "Content-Type,Authorization",
        }
    )
    assert response.status_code == 200
    assert response.headers.get("access-control-allow-origin") == "https://lunaca47.github.io"
    assert response.headers.get("access-control-allow-credentials") == "true"

def test_search_rate_limiter_integration():
    search_rate_limiter.reset()
    # Test that standard search requests go through
    payload = {"query": "cheque bounce", "limit": 1}
    res = client.post("/api/v1/retrieval/search", json=payload)
    assert res.status_code == 200
    search_rate_limiter.reset()
