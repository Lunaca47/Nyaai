"""
Unit and Integration Tests for Phase 2: Live Official-Source Retrieval Layer.
Validates:
1. Strict Domain Allowlist Enforcement (rejects mirrors like indiankanoon, scconline, and arbitrary URLs).
2. Cache-Hit vs Cache-Miss behavior and TTL expiration.
3. Graceful degradation: Deliberate failure on dead/unreachable URLs falls back to local pre-verified corpus.
4. Live official API endpoint /api/v1/retrieval/search/live.
"""
import time
import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.retrieval.hybrid_search import DocumentChunk, hybrid_search_engine
from app.retrieval.live_source_retriever import (
    LiveOfficialSourceRetriever,
    LiveSourceCache,
    OFFICIAL_DOMAIN_ALLOWLIST
)

client = TestClient(app)


def test_domain_allowlist_enforcement():
    """Verify strict allowlist: only approved official government domains are permitted."""
    retriever = LiveOfficialSourceRetriever()

    # Allowed official government domains (HTTPS only)
    assert retriever.is_allowlisted_url("https://indiacode.gov.in/handle/123456789/1362") is True
    assert retriever.is_allowlisted_url("https://www.indiacode.nic.in/handle/123") is True
    assert retriever.is_allowlisted_url("https://sci.gov.in/judgments/") is True
    assert retriever.is_allowlisted_url("https://main.sci.gov.in/case-status") is True
    assert retriever.is_allowlisted_url("https://delhihighcourt.nic.in/orders") is True
    assert retriever.is_allowlisted_url("https://rbi.org.in/scripts/BS_ViewMasCirculardetails.aspx") is True
    assert retriever.is_allowlisted_url("https://legislative.gov.in/constitution-of-india") is True
    assert retriever.is_allowlisted_url("https://egazette.gov.in/default.aspx") is True

    # Non-government mirrors MUST be rejected as primary sources
    assert retriever.is_allowlisted_url("https://indiankanoon.org/doc/123456/") is False
    assert retriever.is_allowlisted_url("https://www.scconline.com/post/999") is False
    assert retriever.is_allowlisted_url("https://livelaw.in/news-updates") is False
    assert retriever.is_allowlisted_url("https://barandbench.com/news") is False
    assert retriever.is_allowlisted_url("https://en.wikipedia.org/wiki/Section_138") is False
    assert retriever.is_allowlisted_url("https://arbitrary-site.com/act") is False

    # Insecure HTTP scheme MUST be rejected
    assert retriever.is_allowlisted_url("http://indiacode.gov.in/handle/123") is False


def test_live_source_cache_hit_miss_and_expiration(tmp_path):
    """Verify persistent caching, cache-hit retrieval, and TTL expiration."""
    test_cache_file = str(tmp_path / "test_cache.json")
    cache = LiveSourceCache(cache_file=test_cache_file)

    sample_doc = DocumentChunk(
        id="test-doc-1",
        act="Negotiable Instruments Act, 1881",
        section="138",
        title="Dishonour of cheque",
        content="Where any cheque drawn by a person on an account maintained by him with a banker...",
        source_url="https://indiacode.gov.in/handle/123456789/1362",
        source_type="live_fetch"
    )

    # 1. Cache Miss initially
    assert cache.get("test_key") is None

    # 2. Put in cache with 2-second TTL
    cache.put("test_key", sample_doc, ttl_seconds=2)

    # 3. Cache Hit immediately
    cached_doc = cache.get("test_key")
    assert cached_doc is not None
    assert cached_doc.act == "Negotiable Instruments Act, 1881"
    assert cached_doc.section == "138"
    assert cached_doc.source_type == "live_fetch"
    assert cached_doc.source_url == "https://indiacode.gov.in/handle/123456789/1362"
    assert cached_doc.fetched_at != ""

    # 4. Persistence Check: Reload from disk
    new_cache_instance = LiveSourceCache(cache_file=test_cache_file)
    reloaded_doc = new_cache_instance.get("test_key")
    assert reloaded_doc is not None
    assert reloaded_doc.title == "Dishonour of cheque"

    # 5. TTL Expiration: Sleep past TTL
    time.sleep(2.1)
    assert new_cache_instance.get("test_key") is None


def test_dead_url_deliberate_failure_triggers_graceful_local_fallback(tmp_path):
    """
    CRITICAL NEGATIVE-PROOF TEST:
    Deliberately point the live fetcher at a dead / unreachable URL.
    Proves that the system catches the failure and gracefully falls back to the
    matching pre-verified local corpus provision without failing or fabricating.
    """
    test_cache_file = str(tmp_path / "failure_test_cache.json")
    retriever = LiveOfficialSourceRetriever(cache=LiveSourceCache(test_cache_file))

    dead_url = "https://delhihighcourt.nic.in/non_existent_dead_url_404_test"
    query = "Dishonour of cheque for insufficiency of funds under Section 138"

    doc, mode = retriever.retrieve_with_fallback(
        query=query,
        local_corpus=hybrid_search_engine.documents,
        target_act="Negotiable Instruments Act, 1881",
        target_section="138",
        dead_url_for_test=dead_url
    )

    # Must NOT fail, must NOT be ungrounded
    assert doc is not None, "Fallback must return local pre-verified document chunk"
    assert mode == "fallback_local_preverified", f"Expected fallback_local_preverified, got {mode}"
    assert "Negotiable Instruments Act" in doc.act
    assert "138" in doc.section or "138" in doc.content or "138" in doc.title
    assert "cheque" in doc.content.lower()


def test_unreachable_mohua_gracefully_falls_back_to_local_mta(tmp_path):
    """
    Phase 1 empirical finding: mohua.gov.in has SSL handshake failures.
    Verify that querying Model Tenancy Act provisions automatically falls back
    to the verified local corpus Model Tenancy Act chunk.
    """
    test_cache_file = str(tmp_path / "mta_test_cache.json")
    retriever = LiveOfficialSourceRetriever(cache=LiveSourceCache(test_cache_file))

    query = "Security deposit cap under Section 11 of the Model Tenancy Act"

    doc, mode = retriever.retrieve_with_fallback(
        query=query,
        local_corpus=hybrid_search_engine.documents,
        target_act="Model Tenancy Act, 2021",
        target_section="11",
        dead_url_for_test="https://mohua.gov.in/upload/uploadfiles/files/Model_Tenancy_Act_English.pdf"
    )

    assert doc is not None
    assert mode == "fallback_local_preverified"
    assert "Model Tenancy Act" in doc.act
    assert "11" in doc.section or "11" in doc.title or "security deposit" in doc.content.lower()


def test_search_live_api_endpoint():
    """Verify the /api/v1/retrieval/search/live endpoint with fallback."""
    response = client.post(
        "/api/v1/retrieval/search/live",
        json={
            "query": "Bail under Section 480 of Bharatiya Nagarik Suraksha Sanhita",
            "act": "Bharatiya Nagarik Suraksha Sanhita, 2023",
            "dead_url_for_test": "https://delhihighcourt.nic.in/dead_endpoint_test"
        }
    )
    assert response.status_code == 200
    data = response.json()
    assert data["total"] >= 1
    top_result = data["results"][0]
    assert top_result["retrieval_mode"] == "fallback_local_preverified"
    assert "480" in top_result["section"] or "480" in top_result["title"] or "480" in top_result["content"]
    assert top_result["source_type"] == "local_corpus"
