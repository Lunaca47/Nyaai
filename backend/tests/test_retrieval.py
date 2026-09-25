import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app
from app.retrieval.hybrid_search import HybridLegalSearchEngine, DocumentChunk

def test_sparse_bm25_exact_section_match():
    engine = HybridLegalSearchEngine()
    results = engine.search("Section 138 cheque bounce notice", limit=3)
    assert len(results) > 0
    top_doc, score, mode = results[0]
    assert top_doc.section == "Section 138"
    assert "Negotiable Instruments Act" in top_doc.act
    assert mode == "sparse-heavy"

def test_dense_semantic_fact_pattern_search():
    engine = HybridLegalSearchEngine()
    results = engine.search("landlord locked me out and refused to return my security deposit", limit=3)
    assert len(results) > 0
    sections = [doc.section for doc, _, _ in results]
    # Should find criminal breach of trust / trespass, rent control, or Model Tenancy deposit (Sec 11) / lockout (Sec 20)
    assert any(s in ["Section 11", "Section 316", "Section 329", "Section 14", "Section 15", "Section 20"] for s in sections)

def test_repealed_statutes_filtered_by_default():
    engine = HybridLegalSearchEngine()
    # Query mentions cheating
    results = engine.search("cheating and fraud property delivery", limit=10, include_repealed=False)
    for doc, _, _ in results:
        assert doc.status != "repealed"
        assert "Indian Penal Code" not in doc.act

def test_jurisdiction_boosting():
    engine = HybridLegalSearchEngine()
    # Query rent control with Delhi jurisdiction
    delhi_results = engine.search("eviction of tenant and non payment of rent", jurisdiction="delhi", limit=5)
    # The top result or high ranking result should be Delhi Rent Control
    delhi_acts = [doc.act for doc, _, _ in delhi_results]
    assert "Delhi Rent Control Act, 1958" in delhi_acts

    # Compare score with Maharashtra
    mh_results = engine.search("eviction of tenant and non payment of rent", jurisdiction="maharashtra", limit=5)
    mh_acts = [doc.act for doc, _, _ in mh_results]
    assert "Maharashtra Rent Control Act, 1999" in mh_acts

@pytest.mark.asyncio
async def test_retrieval_router_endpoint():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/api/v1/retrieval/search", json={
            "query": "Rash driving accident hit and run",
            "jurisdiction": "central",
            "limit": 3
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["total"] > 0
        sections = [r["section"] for r in data["results"]]
        # Rash driving / hit and run BNS sections
        assert any(s in ["Section 281", "Section 106", "Section 166"] for s in sections)
