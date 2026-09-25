from typing import Optional, List, Dict, Any, Union
from uuid import UUID
from datetime import datetime, date
from pydantic import BaseModel, Field

class UserCreate(BaseModel):
    preferred_language: str = "en"

class UserResponse(BaseModel):
    id: UUID
    created_at: datetime
    preferred_language: str
    class Config:
        from_attributes = True

class MatterCreate(BaseModel):
    user_id: UUID
    title: str
    domain: str
    jurisdiction_state: Optional[str] = None
    procedural_stage: str = "intake"
    status: str = "active"
    scratch: Optional[Dict[str, Any]] = None
    case_state_json: Optional[str] = None

class MatterResponse(BaseModel):
    id: UUID
    user_id: UUID
    title: str
    domain: str
    jurisdiction_state: Optional[str]
    procedural_stage: str
    status: str
    created_at: datetime
    updated_at: datetime
    scratch: Dict[str, Any] = Field(default_factory=dict)
    case_state_json: Optional[str] = None
    class Config:
        from_attributes = True

class MatterList(BaseModel):
    items: List[MatterResponse]

class FactCreate(BaseModel):
    matter_id: UUID
    slot_name: Optional[str] = None
    statement: str
    source_type: str
    confidence: str = "stated"
    supported_by: Optional[List[UUID]] = None

class FactResponse(BaseModel):
    id: UUID
    matter_id: UUID
    slot_name: Optional[str]
    statement: str
    source_type: str
    confidence: str
    supported_by: Optional[List[UUID]]
    created_at: datetime
    class Config:
        from_attributes = True

class IntakeMessage(BaseModel):
    user_message: str
    matter_id: Optional[UUID] = None
    user_id: Optional[UUID] = None  # required for new matters; from auth in prod

class IntakeResponse(BaseModel):
    matter_id: UUID
    question: Optional[str] = None
    intake_complete: bool
    facts_collected: List[FactResponse] = []
    missing_information: List[str] = []
    domain: Optional[str] = None
    filled_slots: Optional[Dict[str, Any]] = None

class EvidenceItemResponse(BaseModel):
    id: UUID
    matter_id: UUID
    object_key: str
    doc_type: Optional[str]
    classification_confidence: Optional[float]
    extracted_text: Optional[str]
    uploaded_at: datetime
    class Config:
        from_attributes = True

class CitationResponse(BaseModel):
    id: UUID
    matter_id: UUID
    legal_source_id: UUID
    retrieved_passage: str
    claim_text: str
    verified: bool
    created_at: datetime
    class Config:
        from_attributes = True

class ActionPlanResponse(BaseModel):
    id: UUID
    matter_id: UUID
    situation_summary: Optional[str]
    options: Optional[Dict[str, Any]]
    evidence_required: Optional[Dict[str, Any]]
    immediate_actions: Optional[List[str]]
    future_actions: Optional[List[str]]
    open_questions: Optional[List[str]]
    generated_at: datetime
    class Config:
        from_attributes = True

class EscalationResponse(BaseModel):
    id: UUID
    matter_id: UUID
    trigger_reason: str
    recommended_path: str
    created_at: datetime
    class Config:
        from_attributes = True

class CaseEventResponse(BaseModel):
    id: UUID
    matter_id: UUID
    event_date: Optional[date]
    description: str
    source_fact_id: Optional[UUID]
    class Config:
        from_attributes = True

class CaseStateResponse(BaseModel):
    matter: MatterResponse
    facts: List[FactResponse]
    events: List[CaseEventResponse]
    evidence: List[EvidenceItemResponse]
    citations: List[CitationResponse]
    escalations: List[EscalationResponse]
    action_plans: List[ActionPlanResponse]

# ---------------------------------------------------------------------------
# Generation Schemas
# ---------------------------------------------------------------------------

class GenerateRequest(BaseModel):
    prompt: str
    system_prompt: Optional[str] = ""
    temperature: float = 0.3
    json_mode: bool = False

class GenerateResponse(BaseModel):
    text: str
    provider: str
    model: str

# ---------------------------------------------------------------------------
# Verification Schemas
# ---------------------------------------------------------------------------

class VerifyCitationRequest(BaseModel):
    response_text: str
    cited_citations: List[str] = []
    retrieved_sources: List[Union[str, Dict[str, Any]]] = []
    jurisdiction: Optional[str] = "central"

class CitationVerificationResult(BaseModel):
    citation: str
    grounded: bool
    current: bool
    warning: Optional[str] = None
    replacement: Optional[str] = None
    source_type: str = "local_corpus"  # "local_corpus" | "live_fetch"
    source_url: Optional[str] = None
    fetched_at: Optional[str] = None

class VerifyResponse(BaseModel):
    is_grounded: bool
    is_current: bool
    action: str = "PASSED"
    citations: List[CitationVerificationResult]
    warnings: List[str] = []

# ---------------------------------------------------------------------------
# Retrieval Schemas
# ---------------------------------------------------------------------------

class RetrievalQuery(BaseModel):
    query: str
    jurisdiction: Optional[str] = None
    act: Optional[str] = None
    limit: int = 5
    enable_live_fetch: bool = False
    dead_url_for_test: Optional[str] = None

class LegalDocumentResult(BaseModel):
    id: str
    act: str
    section: str
    title: str
    content: str
    jurisdiction: str
    status: str  # "in_force", "amended", "repealed"
    relevance_score: float
    retrieval_mode: str  # "sparse", "dense", "hybrid", "live_fetch", "live_fetch_cached", "fallback_local_preverified"
    source_type: str = "local_corpus"  # "local_corpus" or "live_fetch"
    source_url: Optional[str] = None
    fetched_at: Optional[str] = None

class RetrievalResponse(BaseModel):
    query: str
    results: List[LegalDocumentResult]
    total: int

# ---------------------------------------------------------------------------
# Case Law Precedent Schemas (Phase 1)
# ---------------------------------------------------------------------------

class CaseLawEntry(BaseModel):
    case_id: str
    case_name: str
    citation: str
    court: str = "Supreme Court of India"
    judgment_date: str
    bench_strength: int
    bench_judges: List[str]
    statutory_provisions: List[str]
    legal_domain: str
    ratio_decidendi: str
    key_principles: List[str]
    precedent_status: str = Field(..., description="GOOD_LAW | MODIFIED | SUPERSEDED_BY_STATUTE | OVERRULED")
    currentness_check: str
    source_url: str
    verification_status: str = "VERIFIED"

class CaseLawQuery(BaseModel):
    query: Optional[str] = None
    statute: Optional[str] = None
    domain: Optional[str] = None
    precedent_status: Optional[str] = None
    limit: int = 10

class CaseLawListResponse(BaseModel):
    cases: List[CaseLawEntry]
    total: int

class CaseLawQueryResult(BaseModel):
    case_id: str
    case_name: str
    citation: str
    court: str
    judgment_date: str
    bench_strength: int
    bench_judges: List[str]
    statutory_provisions: List[str]
    legal_domain: str
    ratio_decidendi: str
    key_principles: List[str]
    precedent_status: str
    currentness_check: str
    source_url: str
    relevance_score: float
    retrieval_mode: str

class CaseLawRetrievalResponse(BaseModel):
    query: str
    results: List[CaseLawQueryResult]
    total: int

# ---------------------------------------------------------------------------
# Case Status Schemas
# ---------------------------------------------------------------------------

class CaseStatusRequest(BaseModel):
    cnr: Optional[str] = None
    court: Optional[str] = None
    case_type: Optional[str] = None
    case_number: Optional[str] = None
    year: Optional[int] = None

class CaseStatusResponse(BaseModel):
    cnr: Optional[str] = None
    is_valid_format: bool
    status: str
    court_name: Optional[str] = None
    case_number: Optional[str] = None
    filing_year: Optional[int] = None
    official_portal_url: str
    deep_link_url: Optional[str] = None
    court_specific_url: Optional[str] = None
    blocker_reason: Optional[str] = None
    instructions: Optional[str] = None
    retrieval_mode: str = "clean_blocker_notification"


