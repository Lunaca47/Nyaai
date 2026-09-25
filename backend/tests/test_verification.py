import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app

@pytest.mark.asyncio
async def test_verification_grounded_and_current():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/api/v1/verify", json={
            "response_text": "The police must register an FIR under Section 173 BNSS.",
            "cited_citations": ["Section 173 BNSS"],
            "retrieved_sources": [
                "Section 173 BNSS: Information in cognizable cases and Zero FIR mandate."
            ],
            "jurisdiction": "central"
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["is_grounded"] is True
        assert data["is_current"] is True
        assert len(data["warnings"]) == 0
        assert len(data["citations"]) == 1
        assert data["citations"][0]["grounded"] is True
        assert data["citations"][0]["current"] is True

@pytest.mark.asyncio
async def test_verification_detects_repealed_statute():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/api/v1/verify", json={
            "response_text": "You can file a complaint under Section 420 IPC and Section 154 CrPC.",
            "cited_citations": ["Section 420 IPC", "Section 154 CrPC"],
            "retrieved_sources": ["Context mentions 420 and 154"],
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["is_current"] is False
        assert len(data["warnings"]) >= 2
        
        # Verify specific replacement suggestions
        ipc_cit = next(c for c in data["citations"] if "420" in c["citation"])
        assert ipc_cit["current"] is False
        assert "BNS Section 318(4)" in ipc_cit["replacement"]

        crpc_cit = next(c for c in data["citations"] if "154" in c["citation"])
        assert crpc_cit["current"] is False
        assert "BNSS Section 173" in crpc_cit["replacement"]

@pytest.mark.asyncio
async def test_verification_detects_ungrounded_citation():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/api/v1/verify", json={
            "response_text": "Apply for relief under Section 999 BNS.",
            "cited_citations": ["Section 999 BNS"],
            "retrieved_sources": ["Retrieved context mentions only Section 106 and Section 281."],
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["is_grounded"] is False
        cit = data["citations"][0]
        assert cit["grounded"] is False
        assert any("ungrounded" in w for w in data["warnings"])

@pytest.mark.asyncio
async def test_verification_detects_model_tenancy_act_caveat():
    """Permanent regression test: Model Tenancy Act citation must trigger ANNOTATED_MODEL_LAW action."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/api/v1/verify", json={
            "response_text": "Under the Model Tenancy Act, 2021 Section 11, the landlord cannot demand more than two months rent as security deposit.",
            "cited_citations": ["Section 11 Model Tenancy Act"],
            "retrieved_sources": [
                "Section 11 Model Tenancy Act: Security deposit cap and refund upon taking vacant possession."
            ],
            "jurisdiction": "central"
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["action"] == "ANNOTATED_MODEL_LAW", f"Expected ANNOTATED_MODEL_LAW, got {data['action']}"
        assert data["is_grounded"] is True
        assert data["is_current"] is True
        assert len(data["warnings"]) >= 1
        assert any("Model Tenancy Act" in w for w in data["warnings"])
        assert any("Entry 18" in w for w in data["warnings"])
        assert any("State" in w for w in data["warnings"])

@pytest.mark.asyncio
async def test_verification_extracts_and_annotates_mta_from_text_without_explicit_citations():
    """Permanent regression test: Implicit regex extraction must recognize MTA citations and trigger ANNOTATED_MODEL_LAW."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/api/v1/verify", json={
            "response_text": "Under Section 11 of the Model Tenancy Act, the security deposit is strictly limited to 2 months rent.",
            "retrieved_sources": [
                "Section 11 of the Model Tenancy Act provides that security deposit cannot exceed two months."
            ],
            "jurisdiction": "central"
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["action"] == "ANNOTATED_MODEL_LAW", f"Expected ANNOTATED_MODEL_LAW, got {data['action']}"
        assert len(data["citations"]) > 0
        assert any("11" in c["citation"] for c in data["citations"])
        assert data["is_grounded"] is True
        assert data["is_current"] is True

@pytest.mark.asyncio
async def test_fabricated_mta_citation_rejected_not_annotated_model_law():
    """Negative regression test: Invented MTA citation with zero grounded sources must be REJECTED_UNGROUNDED, not ANNOTATED_MODEL_LAW."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/api/v1/verify", json={
            "response_text": "Under Section 99 of the Model Tenancy Act, 2021, the tenant can withhold rent indefinitely.",
            "cited_citations": ["Section 99 of the Model Tenancy Act"],
            "retrieved_sources": [],
            "jurisdiction": "central"
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["action"] == "REJECTED_UNGROUNDED", f"Expected REJECTED_UNGROUNDED, got {data['action']}"
        assert data["is_grounded"] is False

@pytest.mark.asyncio
async def test_fabricated_repealed_citation_rejected_not_annotated_repealed():
    """Negative regression test: Invented repealed citation with zero grounded sources must be REJECTED_UNGROUNDED, not ANNOTATED_REPEALED."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/api/v1/verify", json={
            "response_text": "File a petition under Section 9999 of the Indian Penal Code.",
            "cited_citations": ["Section 9999 IPC"],
            "retrieved_sources": [],
            "jurisdiction": "central"
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["action"] == "REJECTED_UNGROUNDED", f"Expected REJECTED_UNGROUNDED, got {data['action']}"
        assert data["is_grounded"] is False

@pytest.mark.asyncio
async def test_positive_proof_grounded_repealed_statute_annotated_repealed():
    """Positive proof: Real, grounded colonial-law citation (IPC 302) must gate to ANNOTATED_REPEALED."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/api/v1/verify", json={
            "response_text": "The accused should be prosecuted under Section 302 IPC for the offense of murder.",
            "cited_citations": ["Section 302 IPC"],
            "retrieved_sources": [
                "Section 302 IPC / BNS Section 103(1): Punishment for murder with death or imprisonment for life."
            ],
            "jurisdiction": "central"
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["action"] == "ANNOTATED_REPEALED", f"Expected ANNOTATED_REPEALED, got {data['action']}"
        assert data["is_grounded"] is True
        assert data["is_current"] is False
        assert any("BNS Section 103(1)" in c["replacement"] for c in data["citations"] if "302" in c["citation"])

@pytest.mark.asyncio
async def test_grounded_non_mta_with_ungrounded_mta_annotated_ungrounded_not_model_law():
    """Negative regression test: Response with grounded non-MTA citation and ungrounded MTA citation must produce ANNOTATED_UNGROUNDED, NOT ANNOTATED_MODEL_LAW."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/api/v1/verify", json={
            "response_text": "Under Section 318 BNS cheating is penalized, while Section 99 of the Model Tenancy Act governs eviction.",
            "cited_citations": ["Section 318 BNS", "Section 99 of the Model Tenancy Act"],
            "retrieved_sources": [
                "Section 318 BNS: Cheating and dishonestly inducing delivery of property."
            ],
            "jurisdiction": "central"
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["action"] == "ANNOTATED_UNGROUNDED", f"Expected ANNOTATED_UNGROUNDED, got {data['action']}"
        assert data["action"] != "ANNOTATED_MODEL_LAW"

@pytest.mark.asyncio
async def test_incidental_mta_mention_with_grounded_non_mta_citation_passed_not_model_law():
    """Tightened MTA grounding: Response mentioning MTA incidentally without an MTA citation must NOT receive ANNOTATED_MODEL_LAW."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/api/v1/verify", json={
            "response_text": "Unlike the Model Tenancy Act which applies to leases, under Section 318 BNS cheating is penalized.",
            "cited_citations": ["Section 318 BNS"],
            "retrieved_sources": [
                "Section 318 BNS: Cheating and dishonestly inducing delivery of property."
            ],
            "jurisdiction": "central"
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["action"] == "PASSED", f"Expected PASSED, got {data['action']}"
        assert data["action"] != "ANNOTATED_MODEL_LAW"

@pytest.mark.asyncio
async def test_case_law_fabricated_ungrounded_rejected():
    """Negative-proof test: Fabricated case precedent with zero grounded sources must be REJECTED_UNGROUNDED."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/api/v1/verify", json={
            "response_text": "As held in Ramesh Sharma v. State of Narnia, (2024) 99 SCC 999, all financial debts are cancelled.",
            "cited_citations": ["Ramesh Sharma v. State of Narnia, (2024) 99 SCC 999"],
            "retrieved_sources": [
                "Negotiable Instruments Act, 1881 Section 138: Dishonour of cheque for insufficiency of funds."
            ],
            "jurisdiction": "central"
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["action"] == "REJECTED_UNGROUNDED", f"Expected REJECTED_UNGROUNDED, got {data['action']}"
        assert data["is_grounded"] is False
        assert any("ungrounded" in w.lower() for w in data["warnings"])

@pytest.mark.asyncio
async def test_case_law_superseded_precedent_subhash_mahajan():
    """Negative-proof test: Subhash Kashinath Mahajan citation, grounded, must produce ANNOTATED_SUPERSEDED_PRECEDENT."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/api/v1/verify", json={
            "response_text": "According to Dr. Subhash Kashinath Mahajan v. State of Maharashtra, (2018) 6 SCC 454, anticipatory bail is maintainable under the SC/ST Act.",
            "cited_citations": ["Dr. Subhash Kashinath Mahajan v. State of Maharashtra, (2018) 6 SCC 454"],
            "retrieved_sources": [
                {
                    "case_id": "CASE_SC_2018_SUBHASH_MAHAJAN",
                    "case_name": "Dr. Subhash Kashinath Mahajan v. State of Maharashtra",
                    "citation": "(2018) 6 SCC 454",
                    "precedent_status": "SUPERSEDED_BY_STATUTE",
                    "currentness_check": "Superseded by Parliament through Scheduled Castes and the Scheduled Tribes (Prevention of Atrocities) Amendment Act, 2018 inserting Section 18A. Constitutionality upheld by 3-Judge Bench in Prathvi Raj Chauhan v. Union of India (2020) 4 SCC 727; Review Bench recalled Mahajan directions in Union of India v. State of Maharashtra (2020) 4 SCC 761.",
                    "ratio_decidendi": "Introduced procedural safeguards including preliminary inquiry and pre-arrest approval under SC/ST Act."
                }
            ],
            "jurisdiction": "central"
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["action"] == "ANNOTATED_SUPERSEDED_PRECEDENT", f"Expected ANNOTATED_SUPERSEDED_PRECEDENT, got {data['action']}"
        assert any("SUPERSEDED" in w.upper() or "18A" in w for w in data["warnings"])

@pytest.mark.asyncio
async def test_case_law_good_law_passed():
    """Grounded good law precedent must produce PASSED."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/api/v1/verify", json={
            "response_text": "In Lalita Kumari v. Govt. of U.P., (2014) 2 SCC 1, registration of FIR was held mandatory.",
            "cited_citations": ["Lalita Kumari v. Govt. of U.P., (2014) 2 SCC 1"],
            "retrieved_sources": [
                {
                    "case_id": "CASE_SC_2014_LALITA_KUMARI",
                    "case_name": "Lalita Kumari v. Govt. of U.P.",
                    "citation": "(2014) 2 SCC 1",
                    "precedent_status": "GOOD_LAW",
                    "ratio_decidendi": "Registration of FIR is mandatory under Section 154 if cognizable offence is disclosed."
                }
            ],
            "jurisdiction": "central"
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["action"] == "PASSED", f"Expected PASSED, got {data['action']}"

