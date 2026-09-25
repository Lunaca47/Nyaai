import pytest
from uuid import uuid4
from httpx import AsyncClient, ASGITransport
from app.main import app
from app.config import settings
from app.routers.matters import create_guest_token

@pytest.mark.asyncio
async def test_matter_create_and_sync_case_state():
    user_a, token_a = create_guest_token()
    transport = ASGITransport(app=app)
    headers_a = {"Authorization": f"Bearer {token_a}"}

    case_state_payload = '{"matterId":"m123","title":"Illegal Lockout","facts":[{"statement":"Lock changed","confidence":"HIGH"}],"evidence":[]}'

    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        # Create matter with case_state_json using authenticated token
        create_resp = await ac.post("/api/v1/matters", headers=headers_a, json={
            "user_id": str(user_a),
            "title": "Tenancy Lockout Case",
            "domain": "tenancy",
            "jurisdiction_state": "delhi",
            "procedural_stage": "intake",
            "status": "active",
            "case_state_json": case_state_payload
        })
        assert create_resp.status_code == 200
        created = create_resp.json()
        matter_id = created["id"]
        assert created["title"] == "Tenancy Lockout Case"
        assert created["case_state_json"] == case_state_payload
        assert created["user_id"] == str(user_a)

        # Retrieve matter as user_a (owner)
        get_resp = await ac.get(f"/api/v1/matters/{matter_id}", headers=headers_a)
        assert get_resp.status_code == 200
        fetched = get_resp.json()
        assert fetched["id"] == matter_id
        assert fetched["case_state_json"] == case_state_payload

@pytest.mark.asyncio
async def test_matter_access_control_cross_user_forbidden():
    user_a, token_a = create_guest_token()
    user_b, token_b = create_guest_token()
    transport = ASGITransport(app=app)
    headers_a = {"Authorization": f"Bearer {token_a}"}
    headers_b = {"Authorization": f"Bearer {token_b}"}

    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        # User A creates a confidential matter
        create_resp = await ac.post("/api/v1/matters", headers=headers_a, json={
            "user_id": str(user_a),
            "title": "Confidential Domestic Dispute",
            "domain": "domestic_violence",
            "jurisdiction_state": "maharashtra",
            "procedural_stage": "intake",
            "status": "active",
            "case_state_json": '{"sensitive":"dv_allegations"}'
        })
        assert create_resp.status_code == 200
        matter_id = create_resp.json()["id"]

        # User B attempts to access User A's matter -> MUST BE 403 FORBIDDEN
        unauthorized_resp = await ac.get(f"/api/v1/matters/{matter_id}", headers=headers_b)
        assert unauthorized_resp.status_code == 403
        assert "Access forbidden" in unauthorized_resp.json()["detail"]

        # User B attempts to update User A's matter -> MUST BE 403 FORBIDDEN
        patch_resp = await ac.patch(
            f"/api/v1/matters/{matter_id}",
            headers=headers_b,
            json={"title": "Hacked Title"}
        )
        assert patch_resp.status_code == 403

        # User B attempts to delete User A's matter -> MUST BE 403 FORBIDDEN
        del_resp = await ac.delete(f"/api/v1/matters/{matter_id}", headers=headers_b)
        assert del_resp.status_code == 403

        # User A can legitimately delete their own matter
        legit_del_resp = await ac.delete(f"/api/v1/matters/{matter_id}", headers=headers_a)
        assert legit_del_resp.status_code == 200

@pytest.mark.asyncio
async def test_anti_spoofing_unauthenticated_request_rejected():
    """
    Test that an attacker cannot spoof identity by simply setting X-User-Id
    or query param user_id to User A's ID without a cryptographic token.
    Must be rejected with 401 Unauthorized.
    """
    user_a, token_a = create_guest_token()
    transport = ASGITransport(app=app)
    headers_a = {"Authorization": f"Bearer {token_a}"}

    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        # Create a matter owned by User A
        create_resp = await ac.post("/api/v1/matters", headers=headers_a, json={
            "user_id": str(user_a),
            "title": "Private Corporate Dispute",
            "domain": "industrial",
            "jurisdiction_state": "delhi",
            "procedural_stage": "intake",
            "status": "active"
        })
        assert create_resp.status_code == 200
        matter_id = create_resp.json()["id"]

        # Attacker attempts to spoof X-User-Id without valid token -> MUST BE 401
        spoof_resp1 = await ac.get(
            f"/api/v1/matters/{matter_id}",
            headers={"X-User-Id": str(user_a)}
        )
        assert spoof_resp1.status_code == 401
        assert "Unauthorized" in spoof_resp1.json()["detail"]

        # Attacker attempts to spoof user_id query param without valid token -> MUST BE 401
        spoof_resp2 = await ac.get(f"/api/v1/matters/{matter_id}?user_id={user_a}")
        assert spoof_resp2.status_code == 401
        assert "Unauthorized" in spoof_resp2.json()["detail"]

        # Attacker attempts to forge guest token with invalid signature -> MUST BE 401
        forged_resp = await ac.get(
            f"/api/v1/matters/{matter_id}",
            headers={"Authorization": f"Bearer guest_v1:{user_a}:deadbeef1234567890badsignature"}
        )
        assert forged_resp.status_code == 401
        assert "Unauthorized" in forged_resp.json()["detail"]

@pytest.mark.asyncio
async def test_guest_token_issuance_endpoint():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/api/v1/matters/auth/guest-token")
        assert resp.status_code == 200
        data = resp.json()
        assert "guest_token" in data
        assert "user_id" in data
        assert data["guest_token"].startswith("guest_v1:")

        # The issued token must be usable to create a matter
        token = data["guest_token"]
        create_resp = await ac.post("/api/v1/matters", headers={"Authorization": f"Bearer {token}"}, json={
            "user_id": data["user_id"],
            "title": "Guest Consultation",
            "domain": "consumer",
            "jurisdiction_state": "karnataka",
            "procedural_stage": "intake",
            "status": "active"
        })
        assert create_resp.status_code == 200

@pytest.mark.asyncio
async def test_production_environment_fails_loudly_on_db_down(monkeypatch):
    """
    Test that when ENVIRONMENT == 'production' and ALLOW_IN_MEMORY_FALLBACK == False,
    database errors raise 503 Service Unavailable instead of silently degrading to in-memory store.
    """
    monkeypatch.setattr(settings, "ENVIRONMENT", "production")
    monkeypatch.setattr(settings, "ALLOW_IN_MEMORY_FALLBACK", False)

    user_a, token_a = create_guest_token()
    transport = ASGITransport(app=app)
    headers_a = {"Authorization": f"Bearer {token_a}"}

    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        # Since PostgreSQL is not running on localhost:5432, db operations fail
        resp = await ac.post("/api/v1/matters", headers=headers_a, json={
            "user_id": str(user_a),
            "title": "Production Test Case",
            "domain": "tenancy",
            "jurisdiction_state": "delhi",
            "procedural_stage": "intake",
            "status": "active"
        })
        assert resp.status_code == 503
        assert "Service Unavailable" in resp.json()["detail"]


@pytest.mark.asyncio
async def test_cryptographic_jwt_signature_verification():
    """
    Test that valid signed JWTs authenticate successfully, while forged, unsigned,
    or wrong-key JWTs are strictly rejected with 401 Unauthorized.
    """
    import jwt
    from uuid import uuid4

    user_id = uuid4()
    transport = ASGITransport(app=app)

    # 1. Valid signed JWT with settings.AUTH_SECRET_KEY
    valid_jwt = jwt.encode(
        {"sub": str(user_id), "user_id": str(user_id)},
        settings.AUTH_SECRET_KEY,
        algorithm="HS256"
    )
    headers_valid = {"Authorization": f"Bearer {valid_jwt}"}

    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        # Should succeed
        create_resp = await ac.post("/api/v1/matters", headers=headers_valid, json={
            "user_id": str(user_id),
            "title": "Valid JWT Matter",
            "domain": "tenancy",
            "jurisdiction_state": "delhi",
            "procedural_stage": "intake",
            "status": "active"
        })
        assert create_resp.status_code == 200
        matter_id = create_resp.json()["id"]

        # 2. Forged JWT signed with wrong secret key -> MUST BE 401
        forged_jwt = jwt.encode(
            {"sub": str(user_id), "user_id": str(user_id)},
            "attacker_wrong_secret_key_9999",
            algorithm="HS256"
        )
        forged_resp = await ac.get(f"/api/v1/matters/{matter_id}", headers={"Authorization": f"Bearer {forged_jwt}"})
        assert forged_resp.status_code == 401
        assert "Unauthorized" in forged_resp.json()["detail"]

        # 3. Unsigned / Algorithm 'none' JWT -> MUST BE 401
        # Construct header.payload without signature or with fake signature
        unsigned_jwt = jwt.encode(
            {"sub": str(user_id), "user_id": str(user_id)},
            key="",
            algorithm="none"
        )
        unsigned_resp = await ac.get(f"/api/v1/matters/{matter_id}", headers={"Authorization": f"Bearer {unsigned_jwt}"})
        assert unsigned_resp.status_code == 401
        assert "Unauthorized" in unsigned_resp.json()["detail"]


def test_environment_and_secret_key_validation():
    """
    Test that Settings validates ENVIRONMENT and AUTH_SECRET_KEY strictly:
    - Fails if ENVIRONMENT is missing or invalid.
    - Fails if ENVIRONMENT is 'production' and AUTH_SECRET_KEY is missing or the hardcoded default.
    """
    from app.config import Settings
    import pytest

    # Missing or invalid ENVIRONMENT
    with pytest.raises(Exception) as excinfo:
        Settings(ENVIRONMENT="invalid_stage")
    assert "ENVIRONMENT must be explicitly set" in str(excinfo.value)

    # Missing AUTH_SECRET_KEY in production
    with pytest.raises(Exception) as excinfo:
        Settings(ENVIRONMENT="production", AUTH_SECRET_KEY=None)
    assert "AUTH_SECRET_KEY environment variable is required" in str(excinfo.value)

    # Hardcoded default AUTH_SECRET_KEY in production
    with pytest.raises(Exception) as excinfo:
        Settings(ENVIRONMENT="production", AUTH_SECRET_KEY="nyaai_production_hmac_secret_key_2026")
    assert "cannot be empty or the deprecated default string" in str(excinfo.value)

    # Valid non-test settings
    valid_settings = Settings(ENVIRONMENT="production", AUTH_SECRET_KEY="super_secret_random_production_key_321")
    assert valid_settings.ENVIRONMENT == "production"
    assert valid_settings.AUTH_SECRET_KEY == "super_secret_random_production_key_321"

