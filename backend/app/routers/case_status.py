"""
Standalone Live Case-Status Lookup Tool & Intent.
Handles 16-character Case National Record (CNR) numbers and provides verified,
honest blocker notifications with official pre-filled eCourts and High Court portals,
adhering to Supreme Court of India e-Committee cybersecurity and CAPTCHA policies.
"""

import re
import logging
from typing import Dict, Any, Optional
from fastapi import APIRouter, HTTPException
from app.schemas.schemas import CaseStatusRequest, CaseStatusResponse

logger = logging.getLogger(__name__)

router = APIRouter()

# Official High Court and District Court Code Mappings
COURT_CODE_MAP = {
    "DLHC": ("High Court of Delhi", "https://delhihighcourt.nic.in/"),
    "MUBM": ("High Court of Judicature at Bombay", "https://bombayhighcourt.nic.in/"),
    "KAKA": ("High Court of Karnataka", "https://karnatakajudiciary.kar.nic.in/"),
    "UPHC": ("High Court of Judicature at Allahabad", "https://www.allahabadhighcourt.in/"),
    "PBCH": ("High Court of Punjab and Haryana", "https://highcourtchd.gov.in/"),
    "WBCH": ("High Court at Calcutta", "https://calcuttahighcourt.gov.in/"),
    "TNCH": ("High Court of Judicature at Madras", "https://www.hcmadras.tn.gov.in/"),
    "GJHC": ("High Court of Gujarat", "https://gujarathighcourt.nic.in/"),
    "MPHC": ("High Court of Madhya Pradesh", "https://mphc.gov.in/"),
    "TSHC": ("High Court for the State of Telangana", "https://tshc.gov.in/"),
    "KLHC": ("High Court of Kerala", "https://hckerala.gov.in/"),
    "RJHC": ("High Court of Rajasthan", "https://hcraj.nic.in/"),
}

CNR_REGEX = re.compile(r'^([A-Z]{4})([0-9A-Z]{2})(\d{6})(\d{4})$', re.IGNORECASE)


def parse_cnr(raw_cnr: str) -> Dict[str, Any]:
    """
    Parse a 16-character Case National Record (CNR) number into its statutory components.
    Format:
      - 4 chars: State / High Court code (e.g. 'DLHC')
      - 2 chars: District / Establishment code (e.g. '01')
      - 6 digits: Case number (e.g. '000001')
      - 4 digits: Filing year (e.g. '2024')
    """
    clean = re.sub(r'[^A-Za-z0-9]', '', raw_cnr).upper()
    if len(clean) != 16:
        return {
            "is_valid": False,
            "error": f"CNR must be exactly 16 alphanumeric characters, got {len(clean)} characters ({raw_cnr})."
        }

    match = CNR_REGEX.match(clean)
    if not match:
        return {
            "is_valid": False,
            "error": "CNR format is invalid. Expected 4 letters (Court), 2 alphanumeric (Establishment), 6 digits (Case), 4 digits (Year)."
        }

    court_code, est_code, case_num_str, year_str = match.groups()
    court_info = COURT_CODE_MAP.get(court_code)

    if court_info:
        court_name, court_url = court_info
    else:
        court_name = f"District Court / Tribunal ({court_code}-{est_code})"
        court_url = "https://services.ecourts.gov.in/ecourtindia_v6/"

    return {
        "is_valid": True,
        "clean_cnr": clean,
        "court_code": court_code,
        "establishment_code": est_code,
        "court_name": court_name,
        "court_url": court_url,
        "case_number": str(int(case_num_str)),
        "case_num_raw": case_num_str,
        "filing_year": int(year_str),
    }


@router.post("/lookup", response_model=CaseStatusResponse)
async def lookup_case_status(request: CaseStatusRequest):
    """
    Lookup case status for a 16-character CNR number.
    Empirical reality check: eCourts National Portal enforces mandatory Securimage CAPTCHAs
    per Supreme Court e-Committee policy. NYAAI delivers an honest blocker notification
    with parsed case metadata and direct pre-filled portal links, never fabricating case status.
    """
    cnr_val = request.cnr
    if not cnr_val and request.court and request.case_number and request.year:
        # Construct synthetic/attempted CNR if court and numbers provided
        court_prefix = (request.court[:4].upper()).ljust(4, 'X')
        cnr_val = f"{court_prefix}01{str(request.case_number).zfill(6)}{request.year}"

    if not cnr_val:
        return CaseStatusResponse(
            cnr="",
            is_valid_format=False,
            status="MISSING_INPUT",
            official_portal_url="https://services.ecourts.gov.in/ecourtindia_v6/",
            blocker_reason="No CNR number or court details provided. Please provide a 16-character CNR number.",
            instructions="Provide a valid CNR (e.g., DLHC010000012024) to locate case details.",
            retrieval_mode="clean_blocker_notification"
        )

    parsed = parse_cnr(cnr_val)

    if not parsed["is_valid"]:
        return CaseStatusResponse(
            cnr=cnr_val,
            is_valid_format=False,
            status="INVALID_CNR_FORMAT",
            official_portal_url="https://services.ecourts.gov.in/ecourtindia_v6/",
            blocker_reason=parsed["error"],
            instructions=(
                "A Case National Record (CNR) must be exactly 16 characters:\n"
                "- 4 letters: State/Court Code (e.g. DLHC for Delhi High Court)\n"
                "- 2 alphanumeric: District/Establishment (e.g. 01)\n"
                "- 6 digits: Case Number (e.g. 000001)\n"
                "- 4 digits: Filing Year (e.g. 2024)\n"
                "Example: DLHC010000012024"
            ),
            retrieval_mode="clean_blocker_notification"
        )

    clean_cnr = parsed["clean_cnr"]
    court_name = parsed["court_name"]
    court_url = parsed["court_url"]
    case_no = parsed["case_number"]
    year = parsed["filing_year"]

    logger.info(f"[CaseStatus] Successfully validated CNR {clean_cnr} for {court_name} (Year {year}, Case #{case_no})")

    return CaseStatusResponse(
        cnr=clean_cnr,
        is_valid_format=True,
        status="CAPTCHA_GATED_OFFICIAL_PORTAL",
        court_name=court_name,
        case_number=case_no,
        filing_year=year,
        official_portal_url="https://services.ecourts.gov.in/ecourtindia_v6/",
        deep_link_url=f"https://services.ecourts.gov.in/ecourtindia_v6/?cino={clean_cnr}",
        court_specific_url=court_url,
        blocker_reason=(
            "The official eCourts National Portal enforces a mandatory Securimage visual/audio CAPTCHA "
            "per Supreme Court of India e-Committee cybersecurity regulations. Automated scraping without human "
            "verification is blocked to safeguard judicial infrastructure and litigant data privacy."
        ),
        instructions=(
            f"1. Open official eCourts portal via the deep link: https://services.ecourts.gov.in/ecourtindia_v6/?cino={clean_cnr}\n"
            f"2. Your CNR number {clean_cnr} will be queried for {court_name} (Filing Year {year}, Case No. {case_no}).\n"
            "3. Complete the interactive security CAPTCHA on the official portal to view real-time cause lists, case status, and signed orders."
        ),
        retrieval_mode="clean_blocker_notification"
    )
