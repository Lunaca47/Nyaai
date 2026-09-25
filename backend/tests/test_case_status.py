"""
Phase 5 Test Suite — Standalone Live Case-Status Lookup Tool.
Tests CNR parsing, format validation, honest blocker reporting (Supreme Court e-Committee
CAPTCHA policy disclosure), official portal deep linking, and verification that no case data
is fabricated under any circumstances.
"""

import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.routers.case_status import parse_cnr
from app.schemas.schemas import CaseStatusRequest

client = TestClient(app)


def test_valid_delhi_hc_cnr_parsed_correctly():
    """Valid Delhi High Court CNR must be accurately parsed into court, year, and case components."""
    cnr = "DLHC010000012024"
    parsed = parse_cnr(cnr)
    assert parsed["is_valid"] is True
    assert parsed["court_code"] == "DLHC"
    assert parsed["establishment_code"] == "01"
    assert parsed["case_number"] == "1"
    assert parsed["filing_year"] == 2024
    assert "Delhi" in parsed["court_name"]
    assert parsed["court_url"] == "https://delhihighcourt.nic.in/"


def test_valid_bombay_hc_cnr_parsed_correctly():
    """Valid Bombay High Court CNR must be accurately parsed."""
    cnr = "MUBM010045672023"
    parsed = parse_cnr(cnr)
    assert parsed["is_valid"] is True
    assert parsed["court_code"] == "MUBM"
    assert parsed["establishment_code"] == "01"
    assert parsed["case_number"] == "4567"
    assert parsed["filing_year"] == 2023
    assert "Bombay" in parsed["court_name"]
    assert parsed["court_url"] == "https://bombayhighcourt.nic.in/"


def test_invalid_cnr_rejected_with_format_guidance():
    """Invalid CNR numbers must be rejected with informative format error messages."""
    # Test short CNR
    short_res = parse_cnr("DLHC01")
    assert short_res["is_valid"] is False
    assert "16 alphanumeric characters" in short_res["error"]

    # Test non-alphanumeric / malformed CNR
    bad_res = parse_cnr("1234567890123456")
    assert bad_res["is_valid"] is False
    assert "format is invalid" in bad_res["error"].lower()


def test_honest_blocker_response_and_no_fabrication():
    """
    CRITICAL INTEGRITY TEST:
    Verify that the system delivers an honest blocker notification detailing the
    Supreme Court e-Committee CAPTCHA policy, providing direct official deep links,
    and NEVER fabricating fake case outcomes, next hearing dates, or orders.
    """
    response = client.post(
        "/api/v1/case-status/lookup",
        json={"cnr": "DLHC010000012024"}
    )
    assert response.status_code == 200
    data = response.json()

    assert data["is_valid_format"] is True
    assert data["status"] == "CAPTCHA_GATED_OFFICIAL_PORTAL"
    assert data["retrieval_mode"] == "clean_blocker_notification"
    assert "services.ecourts.gov.in" in data["official_portal_url"]
    assert "DLHC010000012024" in data["deep_link_url"]
    assert "delhihighcourt.nic.in" in data["court_specific_url"]
    assert data["court_name"] == "High Court of Delhi"
    assert data["case_number"] == "1"
    assert data["filing_year"] == 2024

    # Blocker disclosure must cite real cybersecurity policy
    assert "CAPTCHA" in data["blocker_reason"]
    assert "Supreme Court" in data["blocker_reason"] or "e-Committee" in data["blocker_reason"]

    # Instructions must give user step-by-step guidance
    assert "1." in data["instructions"]
    assert "CAPTCHA" in data["instructions"]

    # Strict negative test: Must NOT contain fabricated case attributes
    assert "dismissed" not in str(data).lower()
    assert "allowed" not in str(data).lower()
    assert "pending disposal" not in str(data).lower()


def test_case_status_api_endpoint_handles_missing_input():
    """Verify endpoint handles missing input gracefully."""
    response = client.post("/api/v1/case-status/lookup", json={})
    assert response.status_code == 200
    data = response.json()
    assert data["is_valid_format"] is False
    assert data["status"] == "MISSING_INPUT"
