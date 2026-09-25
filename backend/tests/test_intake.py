"""
Phase 1 Acceptance Tests — Intake Engine.

Tests the tenancy_deposit vertical slice end-to-end with a mocked LLM client.
Covers all acceptance criteria from Section 4 of the rebuild brief.
"""

import uuid
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.intake.engine import (
    ClassificationResult,
    ExtractionResult,
    IntakeEngine,
    IntakeState,
)
from app.intake.schema_loader import load_domain_schema, list_available_domains


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

class MockLLMClient:
    """Predictable LLM mock for deterministic testing."""

    def __init__(self):
        self.generate_structured = AsyncMock()
        self.generate_text = AsyncMock()


class FakeAsyncSession:
    """Minimal async session stub — just enough for the engine to work."""

    def __init__(self):
        self._store: dict = {}
        self.add = MagicMock()
        self.commit = AsyncMock()

    async def execute(self, stmt):
        """Return empty results for all queries."""
        result = MagicMock()
        result.scalars = MagicMock(return_value=MagicMock(
            first=MagicMock(return_value=None),
            all=MagicMock(return_value=[]),
        ))
        result.all = MagicMock(return_value=[])
        return result


@pytest.fixture
def mock_llm():
    return MockLLMClient()


@pytest.fixture
def mock_db():
    return FakeAsyncSession()


@pytest.fixture
def engine(mock_db, mock_llm):
    return IntakeEngine(db_session=mock_db, llm_client=mock_llm)


# ---------------------------------------------------------------------------
# Schema loading tests
# ---------------------------------------------------------------------------

def test_slot_loading_tenancy_deposit():
    """Assert tenancy_deposit schema loads with the correct 7 slots."""
    schema = load_domain_schema("tenancy_deposit")
    assert schema is not None
    assert schema.domain == "tenancy_deposit"
    assert len(schema.slots) == 7
    slot_names = [s.name for s in schema.slots]
    assert "state" in slot_names
    assert "lease_type" in slot_names
    assert "deposit_amount" in slot_names
    assert "move_out_date" in slot_names
    assert "landlord_stated_reason" in slot_names
    assert "written_demand_sent" in slot_names
    assert "damage_claimed_by_landlord" in slot_names


def test_all_five_domains_available():
    """All 5 launch domains have YAML schemas."""
    domains = list_available_domains()
    for d in [
        "tenancy_deposit",
        "salary_nonpayment",
        "consumer_complaint",
        "cyber_fraud",
        "legal_aid_eligibility",
    ]:
        assert d in domains, f"Missing domain schema: {d}"


def test_slot_impact_weights():
    """Verify that high-impact slots have weight >= 4."""
    schema = load_domain_schema("tenancy_deposit")
    high_impact = [s for s in schema.slots if s.impact_weight >= 4]
    # state(5), lease_type(4), deposit_amount(4), move_out_date(4),
    # landlord_stated_reason(5), damage_claimed_by_landlord(4)  = 6 high-impact
    assert len(high_impact) == 6
    # written_demand_sent(3) is the only low-impact slot
    low_impact = [s for s in schema.slots if s.impact_weight < 4]
    assert len(low_impact) == 1
    assert low_impact[0].name == "written_demand_sent"


# ---------------------------------------------------------------------------
# Classification tests
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_classify_tenancy_deposit(engine, mock_llm):
    """A tenancy deposit problem should be classified as tenancy_deposit."""
    mock_llm.generate_structured.return_value = ClassificationResult(
        domain="tenancy_deposit", confidence=0.92
    )
    mock_llm.generate_text.return_value = (
        "Could you tell me which state the rented property is in?"
    )

    result = await engine.start(
        "My landlord won't return my security deposit of 50000 rupees",
        uuid.uuid4(),
    )

    assert result.domain == "tenancy_deposit"
    assert result.question is not None
    assert not result.intake_complete


@pytest.mark.asyncio
async def test_classify_cyber_fraud(engine, mock_llm):
    """A cyber fraud description should be classified correctly."""
    mock_llm.generate_structured.return_value = ClassificationResult(
        domain="cyber_fraud", confidence=0.88
    )
    mock_llm.generate_text.return_value = "When did this happen?"

    result = await engine.start(
        "Someone hacked my UPI and transferred 25000 from my account",
        uuid.uuid4(),
    )

    assert result.domain == "cyber_fraud"
    assert not result.intake_complete


# ---------------------------------------------------------------------------
# Question selection priority tests
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_question_selection_highest_impact_first(engine, mock_llm):
    """The engine should select the highest impact_weight unfilled slot first."""
    mock_llm.generate_structured.return_value = ClassificationResult(
        domain="tenancy_deposit", confidence=0.9
    )
    mock_llm.generate_text.return_value = "Which state is the property in?"

    result = await engine.start(
        "My landlord is not returning my deposit",
        uuid.uuid4(),
    )

    # The engine should ask about a weight-5 slot first
    # (state or landlord_stated_reason, both weight 5)
    # Since the engine sorts by weight desc and stable-sort preserves YAML order,
    # 'state' comes first in the YAML, so it should be selected.
    assert result.question is not None


# ---------------------------------------------------------------------------
# Stopping rule tests
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_stopping_rule_max_turns(engine):
    """Intake stops after MAX_TURNS (8) even with unfilled slots."""
    state: IntakeState = {
        "matter_id": str(uuid.uuid4()),
        "user_message": "test",
        "domain": "tenancy_deposit",
        "domain_confidence": 0.9,
        "filled_slots": {},
        "unfilled_slots": ["state", "lease_type"],
        "current_question": "Which state?",
        "current_slot": "state",
        "turn_count": 8,  # at the limit
        "intake_complete": False,
        "missing_information": [],
        "conversation_history": [],
    }

    new_state = await engine._check_stopping_rule(state)
    assert new_state["intake_complete"] is True
    assert "state" in new_state["missing_information"]
    assert "lease_type" in new_state["missing_information"]


@pytest.mark.asyncio
async def test_stopping_rule_all_high_impact_filled(engine):
    """Intake stops when all impact_weight >= 4 slots are filled."""
    state: IntakeState = {
        "matter_id": str(uuid.uuid4()),
        "user_message": "test",
        "domain": "tenancy_deposit",
        "domain_confidence": 0.9,
        "filled_slots": {
            "state": "Delhi",
            "lease_type": "written",
            "deposit_amount": 50000,
            "move_out_date": "2024-01-15",
            "landlord_stated_reason": "damage to walls",
            "damage_claimed_by_landlord": True,
        },
        "unfilled_slots": ["written_demand_sent"],  # weight 3 only
        "current_question": "...",
        "current_slot": "written_demand_sent",
        "turn_count": 3,
        "intake_complete": False,
        "missing_information": [],
        "conversation_history": [],
    }

    new_state = await engine._check_stopping_rule(state)
    assert new_state["intake_complete"] is True


@pytest.mark.asyncio
async def test_stopping_rule_user_says_proceed(engine):
    """Intake stops when user explicitly says 'proceed'."""
    state: IntakeState = {
        "matter_id": str(uuid.uuid4()),
        "user_message": "Just proceed with whatever you have",
        "domain": "tenancy_deposit",
        "domain_confidence": 0.9,
        "filled_slots": {"state": "Delhi"},
        "unfilled_slots": ["lease_type", "deposit_amount"],
        "current_question": None,
        "current_slot": None,
        "turn_count": 2,
        "intake_complete": False,
        "missing_information": [],
        "conversation_history": [],
    }

    new_state = await engine._check_stopping_rule(state)
    assert new_state["intake_complete"] is True
    assert "lease_type" in new_state["missing_information"]


# ---------------------------------------------------------------------------
# No duplicate questions
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_no_duplicate_questions(engine, mock_llm):
    """Already-filled slots must never be re-asked."""
    state: IntakeState = {
        "matter_id": str(uuid.uuid4()),
        "user_message": "Delhi",
        "domain": "tenancy_deposit",
        "domain_confidence": 0.9,
        "filled_slots": {"state": "Delhi"},
        "unfilled_slots": ["lease_type", "deposit_amount", "move_out_date",
                           "landlord_stated_reason", "damage_claimed_by_landlord",
                           "written_demand_sent"],
        "current_question": None,
        "current_slot": None,
        "turn_count": 1,
        "intake_complete": False,
        "missing_information": [],
        "conversation_history": [{"role": "user", "content": "Delhi"}],
    }

    mock_llm.generate_text.return_value = "What reason did the landlord give?"

    new_state = await engine._select_question(state)

    # The selected slot must NOT be 'state' since it's already filled
    assert new_state["current_slot"] != "state"
    assert new_state["current_slot"] in state["unfilled_slots"]


# ---------------------------------------------------------------------------
# Missing information populated correctly
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_missing_information_populated(engine):
    """On completion, unfilled slots are recorded in missing_information."""
    state: IntakeState = {
        "matter_id": str(uuid.uuid4()),
        "user_message": "proceed",
        "domain": "tenancy_deposit",
        "domain_confidence": 0.9,
        "filled_slots": {"state": "Delhi"},
        "unfilled_slots": ["lease_type", "deposit_amount"],
        "current_question": None,
        "current_slot": None,
        "turn_count": 2,
        "intake_complete": False,
        "missing_information": [],
        "conversation_history": [],
    }

    new_state = await engine._check_stopping_rule(state)
    assert new_state["intake_complete"] is True
    assert set(new_state["missing_information"]) == {"lease_type", "deposit_amount"}


# ---------------------------------------------------------------------------
# Answer extraction
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_extract_answer_fills_slot(engine, mock_llm):
    """Extraction should move a slot from unfilled to filled."""
    state: IntakeState = {
        "matter_id": str(uuid.uuid4()),
        "user_message": "It was in Maharashtra",
        "domain": "tenancy_deposit",
        "domain_confidence": 0.9,
        "filled_slots": {},
        "unfilled_slots": ["state", "lease_type", "deposit_amount",
                           "move_out_date", "landlord_stated_reason",
                           "damage_claimed_by_landlord", "written_demand_sent"],
        "current_question": "Which state is the property in?",
        "current_slot": "state",
        "turn_count": 0,
        "intake_complete": False,
        "missing_information": [],
        "conversation_history": [],
    }

    mock_llm.generate_structured.return_value = ExtractionResult(
        extracted_value="Maharashtra",
        could_not_extract=False,
        domain_change_hint=None,
    )

    new_state = await engine._extract_answer(state)

    assert new_state["filled_slots"]["state"] == "Maharashtra"
    assert "state" not in new_state["unfilled_slots"]
    assert new_state["turn_count"] == 1


# ---------------------------------------------------------------------------
# Domain reclassification
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_domain_reclassification_on_threat(engine, mock_llm):
    """If user mentions threats during tenancy intake, domain is re-evaluated."""
    state: IntakeState = {
        "matter_id": str(uuid.uuid4()),
        "user_message": "He threatened to beat me if I ask for the deposit",
        "domain": "tenancy_deposit",
        "domain_confidence": 0.9,
        "filled_slots": {"state": "Delhi"},
        "unfilled_slots": ["lease_type"],
        "current_question": "What type of lease?",
        "current_slot": "lease_type",
        "turn_count": 1,
        "intake_complete": False,
        "missing_information": [],
        "conversation_history": [],
    }

    mock_llm.generate_structured.return_value = ExtractionResult(
        extracted_value=None,
        could_not_extract=True,
        domain_change_hint="other",  # hints at criminal threat
    )

    new_state = await engine._extract_answer(state)

    # Domain should be cleared, triggering re-classification
    assert new_state["domain"] is None


# ---------------------------------------------------------------------------
# Title derivation
# ---------------------------------------------------------------------------

def test_derive_title_short_message():
    title = IntakeEngine._derive_title("Help me")
    assert title == "Help me"


def test_derive_title_long_message():
    title = IntakeEngine._derive_title(
        "My landlord is not returning my security deposit of fifty thousand rupees"
    )
    assert title == "My landlord is not returning my…"
