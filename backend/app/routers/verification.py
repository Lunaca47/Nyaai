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

def is_case_citation(citation: str) -> bool:
    """Detect if a citation refers to a case law precedent rather than a statutory section."""
    upper = citation.upper()
    return bool(
        " V. " in upper or " VS. " in upper or " V/S " in upper or
        " SCC " in upper or upper.endswith(" SCC") or "(SCC)" in upper or
        " AIR " in upper or upper.endswith(" AIR") or
        " SCR " in upper or upper.endswith(" SCR") or
        "SCALE" in upper
    )

def extract_citations(text: str) -> List[str]:
    """Extract formal legal citations (statutory provisions and case precedents) from text."""
    citations = []
    # Statutory provisions
    stat_matches = re.finditer(
        rf'\b(?:(?:Section|Sec\.?|§|Article|Art\.?)\s*\d+[A-Z]?(?:\(\w+\))*(?:\s+(?:of\s+(?:the\s+)?)?(?:{ACT_PATTERNS}))?|(?:{ACT_PATTERNS})\s+(?:Section|Sec\.?|§)?\s*\d+[A-Z]?(?:\(\w+\))*)\b',
        text,
        re.IGNORECASE
    )
    for m in stat_matches:
        cit = m.group(0).strip()
        if cit and cit not in citations:
            citations.append(cit)

    # Case law precedents: e.g. "Lalita Kumari v. Govt. of U.P., (2014) 2 SCC 1"
    case_matches = re.finditer(
        r'\b([A-Z][A-Za-z0-9\.\'\s]{1,45}?\s+(?:v\.|vs\.)\s+[A-Z][A-Za-z0-9\.\'\s]{1,45}?(?:,\s*(?:\(\d{4}\)|\d{4})\s+[A-Za-z0-9\s\(\)]+)?)(?=[,\.\n;]|$)',
        text
    )
    for m in case_matches:
        cit = m.group(1).strip()
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

def _format_source_content(s: Any) -> str:
    """Extract searchable content from either statutory chunk or case law precedent dict/obj."""
    if isinstance(s, dict):
        parts = [
            str(s.get("act", "")),
            str(s.get("section", "")),
            str(s.get("title", "")),
            str(s.get("content", "")),
            str(s.get("case_name", "")),
            str(s.get("citation", "")),
            str(s.get("case_id", "")),
            str(s.get("legal_domain", "")),
            str(s.get("ratio_decidendi", "")),
            str(s.get("precedent_status", "")),
            str(s.get("currentness_check", "")),
        ]
        if "statutory_provisions" in s and isinstance(s["statutory_provisions"], list):
            parts.extend([str(p) for p in s["statutory_provisions"]])
        if "key_principles" in s and isinstance(s["key_principles"], list):
            parts.extend([str(p) for p in s["key_principles"]])
        return " ".join(p for p in parts if p)
    return str(s)

def check_grounding(citation: str, retrieved_sources: List[Any]) -> Tuple[bool, Optional[str], Optional[str]]:
    """
    Check if citation appears in retrieved context.
    Returns (is_grounded, precedent_status_warning, replacement_or_check).
    """
    if not retrieved_sources:
        return False, None, None

    if is_case_citation(citation):
        # Case Law Precedent Grounding & Precedent Status verification
        cit_lower = citation.lower()
        # Extract party names from citation
        parts = re.split(r'\s+(?:v\.|vs\.)\s+', citation, flags=re.IGNORECASE)
        p1 = parts[0].strip() if len(parts) > 0 else ""
        p1 = re.sub(r'^(?:according\s+to|as\s+held\s+in|in|per|see|vide|under)\s+', '', p1, flags=re.IGNORECASE).strip()
        p2 = parts[1].strip() if len(parts) > 1 else ""

        # Key distinctive tokens (ignoring generic legal titles)
        ignore_words = {"state", "union", "india", "govt", "u.p.", "delhi", "of", "the", "and", "v.", "vs.", "dr.", "according", "to"}
        p1_tokens = [t.lower() for t in re.findall(r'[A-Za-z]+', p1) if t.lower() not in ignore_words and len(t) > 2]

        matched_source = None
        for s in retrieved_sources:
            s_text = _format_source_content(s).lower()
            if isinstance(s, dict):
                c_name = s.get("case_name", "").lower()
                c_cite = s.get("citation", "").lower()
                if c_name and (c_name in cit_lower or cit_lower in c_name):
                    matched_source = s
                    break
                if c_cite and c_cite in cit_lower:
                    matched_source = s
                    break
            # Token match
            if p1_tokens and all(tok in s_text for tok in p1_tokens):
                matched_source = s
                break

        if matched_source is not None:
            # Grounded! Now check precedent status
            if isinstance(matched_source, dict):
                status = matched_source.get("precedent_status", "GOOD_LAW").upper()
                check_note = matched_source.get("currentness_check", "")
            else:
                s_str = str(matched_source).upper()
                if "SUPERSEDED_BY_STATUTE" in s_str or "SUPERSEDED" in s_str:
                    status = "SUPERSEDED_BY_STATUTE"
                elif "OVERRULED" in s_str:
                    status = "OVERRULED"
                elif "MODIFIED" in s_str:
                    status = "MODIFIED"
                else:
                    status = "GOOD_LAW"
                check_note = str(matched_source)

            if status in ("SUPERSEDED_BY_STATUTE", "MODIFIED", "OVERRULED"):
                warn = f"Precedent '{citation}' is {status}. {check_note}".strip()
                return True, warn, check_note
            return True, None, None
        else:
            return False, None, None

    # Statutory section grounding
    str_sources = [_format_source_content(s) for s in retrieved_sources]
    combined_sources = " ".join(str_sources).lower()
    numbers = re.findall(r'\d+[A-Z]?', citation, re.IGNORECASE)
    if not numbers:
        return (citation.lower() in combined_sources), None, None
    for num in numbers:
        if num.lower() in combined_sources:
            return True, None, None
    return False, None, None

@router.post("", response_model=VerifyResponse)
async def verify_citations(request: VerifyCitationRequest):
    """
    Verify citations in a response for grounding, statutory currentness, model law applicability,
    and judicial precedent status (5-tier + case law gate).
    """
    candidates = list(request.cited_citations)
    if not candidates:
        candidates = extract_citations(request.response_text)

    results: List[CitationVerificationResult] = []
    warnings: List[str] = []
    all_grounded = True
    all_current = True
    superseded_precedents: List[Tuple[str, str]] = []

    for cit in candidates:
        is_current, curr_warn, replacement = check_currentness(cit)
        is_applicable, app_warn = check_applicability(cit)
        is_grounded, prec_warn, prec_check = check_grounding(cit, request.retrieved_sources)

        if prec_warn:
            superseded_precedents.append((cit, prec_warn))
            warnings.append(prec_warn)
            replacement = prec_check

        if not is_current:
            all_current = False
            if curr_warn:
                warnings.append(curr_warn)
        if not is_applicable and app_warn:
            warnings.append(app_warn)
        if not is_grounded:
            all_grounded = False
            if is_case_citation(cit):
                warnings.append(f"Case precedent '{cit}' is ungrounded in retrieved context.")
            else:
                warnings.append(f"Citation '{cit}' is ungrounded in retrieved statutory context.")

        results.append(CitationVerificationResult(
            citation=cit,
            grounded=is_grounded,
            current=is_current and not bool(prec_warn),
            warning=prec_warn or curr_warn,
            replacement=replacement,
        ))

    # Check Model Law applicability
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

    def is_mta_citation(cit_result: CitationVerificationResult) -> bool:
        cit_upper = cit_result.citation.upper()
        if "MODEL TENANCY" in cit_upper or "MTA" in cit_upper:
            return True
        if mentions_mta and request.retrieved_sources:
            mta_sources = [
                s for s in request.retrieved_sources
                if "model tenancy" in _format_source_content(s).lower() or "mta" in _format_source_content(s).lower()
            ]
            if mta_sources and check_grounding(cit_result.citation, mta_sources)[0]:
                return True
        return False

    grounded_mta_citations = [c for c in results if c.grounded and is_mta_citation(c)]
    has_grounded_mta = len(grounded_mta_citations) > 0

    # Gate logic precedence hierarchy:
    # 1. Severe Ungrounded Hallucination: cited provisions exist but none are grounded in context
    # 2. Mixed Ungrounded Hallucination: some grounded citations mixed with hallucinated citations
    # 3. Grounded Superseded Precedent: judicial precedent verified in context but superseded by statute or overruled
    # 4. Grounded Repealed Statute: statutory provisions verified in context but repealed post July 1, 2024
    # 5. Grounded Model Law: provisions verified in context but require State legislative adoption
    # 6. Clean Grounded Pass: fully grounded, current, binding law
    if ungrounded_count > 0 and grounded_count == 0:
        action = "REJECTED_UNGROUNDED"
    elif ungrounded_count > 0:
        action = "ANNOTATED_UNGROUNDED"
    elif superseded_precedents:
        action = "ANNOTATED_SUPERSEDED_PRECEDENT"
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
