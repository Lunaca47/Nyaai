import re
import logging
from typing import List, Tuple, Dict, Any
from fastapi import APIRouter
from app.schemas.schemas import VerifyCitationRequest, CitationVerificationResult, VerifyResponse

logger = logging.getLogger(__name__)

router = APIRouter()

# Map of repealed acts and their replacement acts
REPEALED_ACTS = {
    "IPC": ("Indian Penal Code, 1860", "Bharatiya Nyaya Sanhita, 2023 (BNS)"),
    "INDIAN PENAL CODE": ("Indian Penal Code, 1860", "Bharatiya Nyaya Sanhita, 2023 (BNS)"),
    "CRPC": ("Code of Criminal Procedure, 1973", "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS)"),
    "CODE OF CRIMINAL PROCEDURE": ("Code of Criminal Procedure, 1973", "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS)"),
    "IEA": ("Indian Evidence Act, 1872", "Bharatiya Sakshya Adhiniyam, 2023 (BSA)"),
    "INDIAN EVIDENCE ACT": ("Indian Evidence Act, 1872", "Bharatiya Sakshya Adhiniyam, 2023 (BSA)"),
}

# Known statutory section transitions
SECTION_MIGRATIONS = {
    "IPC 420": "BNS Section 318(4) (Cheating)",
    "SECTION 420 IPC": "BNS Section 318(4) (Cheating)",
    "IPC 302": "BNS Section 103(1) (Murder)",
    "SECTION 302 IPC": "BNS Section 103(1) (Murder)",
    "IPC 376": "BNS Section 64 (Rape)",
    "SECTION 376 IPC": "BNS Section 64 (Rape)",
    "IPC 498A": "BNS Section 85 / 86 (Cruelty to woman)",
    "SECTION 498A IPC": "BNS Section 85 / 86 (Cruelty to woman)",
    "IPC 304A": "BNS Section 106(1) (Causing death by negligence)",
    "SECTION 304A IPC": "BNS Section 106(1) (Causing death by negligence)",
    "IPC 279": "BNS Section 281 (Rash driving)",
    "SECTION 279 IPC": "BNS Section 281 (Rash driving)",
    "IPC 323": "BNS Section 115(2) (Voluntarily causing hurt)",
    "SECTION 323 IPC": "BNS Section 115(2) (Voluntarily causing hurt)",
    "CRPC 154": "BNSS Section 173 (FIR / Zero FIR)",
    "SECTION 154 CRPC": "BNSS Section 173 (FIR / Zero FIR)",
    "CRPC 156(3)": "BNSS Section 175(3) (Magistrate order for investigation)",
    "SECTION 156(3) CRPC": "BNSS Section 175(3) (Magistrate order for investigation)",
    "CRPC 161": "BNSS Section 180 (Witness statements to police)",
    "SECTION 161 CRPC": "BNSS Section 180 (Witness statements to police)",
    "CRPC 173": "BNSS Section 193 (Police investigation chargesheet)",
    "SECTION 173 CRPC": "BNSS Section 193 (Police investigation chargesheet)",
    "CRPC 437": "BNSS Section 480 (Bail in non-bailable offences)",
    "CRPC 439": "BNSS Section 482 (Special powers of High Court/Sessions on bail)",
    "CRPC 70": "BNSS Section 72 (Form of warrant of arrest and duration)",
    "SECTION 70 CRPC": "BNSS Section 72 (Form of warrant of arrest and duration)",
    "CRPC 71": "BNSS Section 73 (Power to direct security to be taken upon warrant)",
    "SECTION 71 CRPC": "BNSS Section 73 (Power to direct security to be taken upon warrant)",
    "IEA 65B": "BSA Section 63 (Admissibility of electronic records)",
    "SECTION 65B IEA": "BSA Section 63 (Admissibility of electronic records)",
}

CITATION_PATTERN = re.compile(
    r'(?:(?:Section|Sec\.?|§|Article|Art\.?)\s*(\d+[A-Z]?(?:\(\d+\))?)\s*(?:of\s+the\s+)?([A-Za-z\s]+)?)'
    r'|(?:([A-Za-z]{2,10})\s*(?:Section|Sec\.?|§)?\s*(\d+[A-Z]?(?:\(\d+\))?))',
    re.IGNORECASE
)

ACT_PATTERNS = r"BNS|BNSS|BSA|IPC|CrPC|IEA|NI Act|IT Act|CPC|Constitution|Model Tenancy Act|MTA|Motor Vehicles Act|MVA|Payment of Gratuity Act|Consumer Protection Act|CPA|Industrial Disputes Act|Protection of Women from Domestic Violence Act|PWDVA"

def extract_citations(text: str) -> List[str]:
    """Extract formal legal citations from text."""
    citations = []
    matches = re.finditer(
        rf'\b(?:(?:Section|Sec\.?|§|Article|Art\.?)\s*\d+[A-Z]?(?:\(\w+\))*(?:\s+(?:of\s+(?:the\s+)?)?(?:{ACT_PATTERNS}))?|(?:{ACT_PATTERNS})\s+(?:Section|Sec\.?|§)?\s*\d+[A-Z]?(?:\(\w+\))*)\b',
        text,
        re.IGNORECASE
    )
    for m in matches:
        cit = m.group(0).strip()
        if cit and cit not in citations:
            citations.append(cit)
    return citations

def check_currentness(citation: str) -> Tuple[bool, str | None, str | None]:
    """Check if citation is for a repealed statute."""
    upper = citation.upper()
    for repealed_key, (old_name, new_name) in REPEALED_ACTS.items():
        if repealed_key in upper:
            # Check for specific section replacement
            replacement = None
            for sec_key, repl in SECTION_MIGRATIONS.items():
                if sec_key in upper:
                    replacement = repl
                    break
            warning = f"'{citation}' references {old_name} which was repealed on July 1, 2024. Use {new_name} instead."
            return False, warning, replacement
    return True, None, None

def check_applicability(citation: str) -> Tuple[bool, str | None]:
    """Check if citation is for a model law requiring State legislative enactment."""
    upper = citation.upper()
    if "MODEL TENANCY" in upper or "MTA" in upper:
        warning = (
            f"'{citation}' references the Model Tenancy Act, 2021 which is a non-binding model framework circulated to States under Entry 18 of the State List. "
            "It requires State legislative adoption. Existing State rent control acts or Transfer of Property Act, 1882 govern unless adopted."
        )
        return False, warning
    return True, None

def check_grounding(citation: str, retrieved_sources: List[Any]) -> bool:
    """Check if citation appears in retrieved context."""
    if not retrieved_sources:
        return False
    str_sources = []
    for s in retrieved_sources:
        if isinstance(s, dict):
            str_sources.append(f"{s.get('act', '')} {s.get('section', '')} {s.get('title', '')} {s.get('content', '')}")
        else:
            str_sources.append(str(s))
    combined_sources = " ".join(str_sources).lower()
    # Normalize citation to core terms
    numbers = re.findall(r'\d+[A-Z]?', citation, re.IGNORECASE)
    if not numbers:
        return citation.lower() in combined_sources
    # Check if number appears in source text
    for num in numbers:
        if num.lower() in combined_sources:
            return True
    return False

@router.post("", response_model=VerifyResponse)
async def verify_citations(request: VerifyCitationRequest):
    """
    Verify citations in a response for grounding and currentness.
    - Grounding: Cited legal sections must be substantiated by retrieved statutory sources.
    - Currentness: Flag repealed statutes (IPC/CrPC/IEA) and suggest BNS/BNSS/BSA replacements.
    - Applicability: Flag model laws (Model Tenancy Act) that require state-level adoption.
    """
    candidates = list(request.cited_citations)
    if not candidates:
        candidates = extract_citations(request.response_text)

    results: List[CitationVerificationResult] = []
    warnings: List[str] = []
    all_grounded = True
    all_current = True

    for cit in candidates:
        is_current, curr_warn, replacement = check_currentness(cit)
        is_applicable, app_warn = check_applicability(cit)
        is_grounded = check_grounding(cit, request.retrieved_sources)

        if not is_current:
            all_current = False
            if curr_warn:
                warnings.append(curr_warn)
        if not is_applicable and app_warn:
            warnings.append(app_warn)
        if not is_grounded:
            all_grounded = False
            warnings.append(f"Citation '{cit}' is ungrounded in retrieved statutory context.")

        results.append(CitationVerificationResult(
            citation=cit,
            grounded=is_grounded,
            current=is_current,
            warning=curr_warn,
            replacement=replacement,
        ))

    # Check Model Law applicability in response_text and warnings (matches Android CitationVerifier.kt)
    response_lower = request.response_text.lower()
    mentions_mta = "model tenancy" in response_lower or "mta 2021" in response_lower or any("Model Tenancy Act" in w for w in warnings)
    has_disclaimer = "state adoption" in response_lower or "entry 18" in response_lower or "model law" in response_lower

    if mentions_mta and not has_disclaimer:
        mta_warning = (
            "References Model Tenancy Act, 2021 which is a non-binding model framework circulated to States under Entry 18 of the State List. "
            "It requires State legislative adoption. Existing State rent control acts or Transfer of Property Act, 1882 govern unless adopted."
        )
        if mta_warning not in warnings:
            warnings.append(mta_warning)

    ungrounded_count = sum(1 for c in results if not c.grounded)
    grounded_count = sum(1 for c in results if c.grounded)

    # Tightened MTA Grounding: has_grounded_mta is True ONLY when the MTA citation itself is in the grounded set
    def is_mta_citation(cit_result: CitationVerificationResult) -> bool:
        cit_upper = cit_result.citation.upper()
        if "MODEL TENANCY" in cit_upper or "MTA" in cit_upper:
            return True
        if mentions_mta and request.retrieved_sources:
            mta_sources = [
                s for s in request.retrieved_sources
                if "model tenancy" in (f"{s.get('act', '')} {s.get('title', '')} {s.get('content', '')}".lower() if isinstance(s, dict) else str(s).lower())
                or "mta" in (f"{s.get('act', '')} {s.get('title', '')} {s.get('content', '')}".lower() if isinstance(s, dict) else str(s).lower())
            ]
            if mta_sources and check_grounding(cit_result.citation, mta_sources):
                return True
        return False

    grounded_mta_citations = [c for c in results if c.grounded and is_mta_citation(c)]
    has_grounded_mta = len(grounded_mta_citations) > 0

    # Gate logic precedence hierarchy:
    # 1. Severe Ungrounded Hallucination: cited provisions exist but none are grounded in context
    # 2. Mixed Ungrounded Hallucination: some grounded citations mixed with hallucinated citations
    # 3. Grounded Repealed Statute: provisions verified in context but repealed post July 1, 2024
    # 4. Grounded Model Law: provisions verified in context but require State legislative adoption
    # 5. Clean Grounded Pass: fully grounded, current, binding law
    if ungrounded_count > 0 and grounded_count == 0:
        action = "REJECTED_UNGROUNDED"
    elif ungrounded_count > 0:
        action = "ANNOTATED_UNGROUNDED"
    elif not all_current:
        action = "ANNOTATED_REPEALED"
    elif has_grounded_mta:
        action = "ANNOTATED_MODEL_LAW"
    else:
        action = "PASSED"

    return VerifyResponse(
        is_grounded=all_grounded if candidates else True,
        is_current=all_current if candidates else True,
        action=action,
        citations=results,
        warnings=warnings,
    )
