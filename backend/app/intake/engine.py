"""
Intake Engine — LangGraph-backed state machine for adaptive legal case intake.

Replaces the old single-shot Q&A flow.  The unit of interaction is a *Matter*:
a persistent, structured case file in Postgres that accumulates facts over
multiple conversation turns.

Nodes
-----
1. issue_classifier   — classify domain from free-text
2. load_slots         — load the domain's YAML slot schema
3. select_question    — pick the highest-impact unfilled slot, phrase a question
4. extract_answer     — parse the user's reply into a slot value, write a Fact
5. check_stopping_rule — decide whether to keep asking or hand off to retrieval
"""

from __future__ import annotations

import json
import logging
import uuid
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, TypedDict

from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.models import Matter, Fact

from .prompts import (
    ANSWER_EXTRACTOR_SYSTEM_PROMPT,
    ANSWER_EXTRACTOR_USER_PROMPT_TEMPLATE,
    ISSUE_CLASSIFIER_SYSTEM_PROMPT,
    ISSUE_CLASSIFIER_USER_PROMPT_TEMPLATE,
    QUESTION_PERSONALIZER_SYSTEM_PROMPT,
    QUESTION_PERSONALIZER_USER_PROMPT_TEMPLATE,
)
from .schema_loader import DomainSchema, load_domain_schema

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# LLM response models
# ---------------------------------------------------------------------------

class ClassificationResult(BaseModel):
    domain: str
    confidence: float


class ExtractionResult(BaseModel):
    extracted_value: Any = None
    could_not_extract: bool = False
    domain_change_hint: Optional[str] = None


# ---------------------------------------------------------------------------
# Public result type
# ---------------------------------------------------------------------------

@dataclass
class IntakeResult:
    matter_id: uuid.UUID
    question: Optional[str]
    intake_complete: bool
    facts_collected: list[dict[str, Any]]
    missing_information: list[str]
    domain: Optional[str]
    filled_slots: dict[str, Any]


# ---------------------------------------------------------------------------
# Internal state (persisted in matters.scratch between turns)
# ---------------------------------------------------------------------------

class IntakeState(TypedDict):
    matter_id: str  # UUID serialised as str for JSON
    user_message: str
    domain: Optional[str]
    domain_confidence: float
    filled_slots: Dict[str, Any]
    unfilled_slots: List[str]
    current_question: Optional[str]
    current_slot: Optional[str]
    turn_count: int
    intake_complete: bool
    missing_information: List[str]
    conversation_history: List[Dict[str, str]]


# ---------------------------------------------------------------------------
# Engine
# ---------------------------------------------------------------------------

class IntakeEngine:
    """Drives the intake dialogue for one Matter.

    Usage::

        engine = IntakeEngine(db_session=session, llm_client=llm)
        result = await engine.start("My landlord won't return my deposit", user_id)
        # … user replies …
        result = await engine.continue_intake(result.matter_id, "Delhi")
    """

    MAX_TURNS: int = 8

    def __init__(self, db_session: AsyncSession, llm_client: Any) -> None:
        self.db = db_session
        self.llm = llm_client

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    async def start(
        self, user_message: str, user_id: uuid.UUID
    ) -> IntakeResult:
        """Begin a brand-new Matter from the user's opening statement."""
        matter_id = uuid.uuid4()

        state: IntakeState = {
            "matter_id": str(matter_id),
            "user_message": user_message,
            "domain": None,
            "domain_confidence": 0.0,
            "filled_slots": {},
            "unfilled_slots": [],
            "current_question": None,
            "current_slot": None,
            "turn_count": 0,
            "intake_complete": False,
            "missing_information": [],
            "conversation_history": [{"role": "user", "content": user_message}],
        }

        # Run: classify → load_slots → select_question
        state = await self._issue_classifier(state)
        state = await self._load_slots(state)
        state = await self._select_question(state)

        # Persist the new Matter + state
        matter = Matter(
            id=matter_id,
            user_id=user_id,
            title=self._derive_title(user_message),
            domain=state["domain"] or "other",
            procedural_stage="intake",
            status="active",
            scratch={"intake_state": state},
        )
        self.db.add(matter)
        await self.db.commit()

        return self._to_result(state)

    async def continue_intake(
        self, matter_id: uuid.UUID, user_message: str
    ) -> IntakeResult:
        """Process the next user message for an existing Matter."""
        # Load persisted state from DB
        result = await self.db.execute(
            select(Matter).where(Matter.id == matter_id)
        )
        matter = result.scalars().first()
        if not matter:
            raise ValueError(f"Matter {matter_id} not found")

        state: IntakeState = matter.scratch.get("intake_state")
        if not state:
            raise ValueError(f"Matter {matter_id} has no intake state")

        state["user_message"] = user_message
        state["conversation_history"].append(
            {"role": "user", "content": user_message}
        )

        # Run: extract_answer → check_stopping
        state = await self._extract_answer(state)

        # If the answer triggered a domain reclassification, re-run
        # classification + slot loading
        if state["domain"] is None:
            state = await self._issue_classifier(state)
            state = await self._load_slots(state)

        state = await self._check_stopping_rule(state)

        if not state["intake_complete"]:
            state = await self._select_question(state)

        # Write facts to the facts table
        await self._persist_facts(matter_id, state)

        # Persist updated state back to the Matter
        matter.scratch = {**matter.scratch, "intake_state": state}
        if state["domain"]:
            matter.domain = state["domain"]
        await self.db.commit()

        return self._to_result(state)

    # ------------------------------------------------------------------
    # Node implementations
    # ------------------------------------------------------------------

    async def _issue_classifier(self, state: IntakeState) -> IntakeState:
        """Node 1 — classify the user's issue into a domain."""
        if state.get("domain") is not None:
            return state

        result = await self.llm.generate_structured(
            system_prompt=ISSUE_CLASSIFIER_SYSTEM_PROMPT,
            user_prompt=ISSUE_CLASSIFIER_USER_PROMPT_TEMPLATE.format(
                user_message=state["user_message"]
            ),
            response_model=ClassificationResult,
            temperature=0.1,
        )
        state["domain"] = result.domain
        state["domain_confidence"] = result.confidence
        return state

    async def _load_slots(self, state: IntakeState) -> IntakeState:
        """Node 2 — load the domain's slot schema."""
        domain = state["domain"]
        if not domain:
            return state

        schema = load_domain_schema(domain)
        if schema:
            already_filled = set(state["filled_slots"].keys())
            state["unfilled_slots"] = [
                s.name for s in schema.slots if s.name not in already_filled
            ]
        return state

    async def _select_question(self, state: IntakeState) -> IntakeState:
        """Node 3 — pick the highest-impact unfilled slot and phrase a question."""
        domain = state["domain"]
        schema = load_domain_schema(domain) if domain else None

        if not schema or not state["unfilled_slots"]:
            state["intake_complete"] = True
            return state

        # Filter to unfilled slots only
        unfilled_defs = [
            s for s in schema.slots if s.name in state["unfilled_slots"]
        ]
        if not unfilled_defs:
            state["intake_complete"] = True
            return state

        # Sort by impact_weight descending (stable sort preserves YAML order
        # for ties, which is intentional)
        unfilled_defs.sort(key=lambda s: s.impact_weight, reverse=True)
        selected = unfilled_defs[0]

        state["current_slot"] = selected.name

        # Personalise the question via LLM
        history_str = json.dumps(state["conversation_history"][-6:])  # last 3 turns
        question = await self.llm.generate_text(
            system_prompt=QUESTION_PERSONALIZER_SYSTEM_PROMPT,
            user_prompt=QUESTION_PERSONALIZER_USER_PROMPT_TEMPLATE.format(
                history=history_str,
                question_template=selected.question_template,
            ),
            temperature=0.7,
        )
        question = question.strip()

        state["current_question"] = question
        state["conversation_history"].append(
            {"role": "assistant", "content": question}
        )
        return state

    async def _extract_answer(self, state: IntakeState) -> IntakeState:
        """Node 4 — extract a slot value from the user's reply."""
        current_slot = state.get("current_slot")
        if not current_slot:
            return state

        domain = state["domain"]
        schema = load_domain_schema(domain) if domain else None
        if not schema:
            return state

        slot_def = next(
            (s for s in schema.slots if s.name == current_slot), None
        )
        if not slot_def:
            return state

        result = await self.llm.generate_structured(
            system_prompt=ANSWER_EXTRACTOR_SYSTEM_PROMPT.format(
                value_type=slot_def.value_type,
                slot_name=current_slot,
            ),
            user_prompt=ANSWER_EXTRACTOR_USER_PROMPT_TEMPLATE.format(
                question=state["current_question"] or "",
                user_message=state["user_message"],
            ),
            response_model=ExtractionResult,
            temperature=0.1,
        )

        if result.extracted_value is not None and not result.could_not_extract:
            state["filled_slots"][current_slot] = result.extracted_value
            if current_slot in state["unfilled_slots"]:
                state["unfilled_slots"].remove(current_slot)

        # If the user's answer hints at a different legal domain
        if result.domain_change_hint and result.domain_change_hint != domain:
            logger.info(
                "Domain reclassification triggered: %s → %s",
                domain,
                result.domain_change_hint,
            )
            state["domain"] = None  # force re-classification

        state["current_slot"] = None
        state["current_question"] = None
        state["turn_count"] += 1
        return state

    async def _check_stopping_rule(self, state: IntakeState) -> IntakeState:
        """Node 5 — decide whether intake is complete."""
        msg_lower = state["user_message"].lower().strip()

        # (c) User explicitly asks to proceed
        proceed_phrases = [
            "proceed", "just tell me", "skip", "go ahead",
            "that's all", "that is all", "enough questions",
        ]
        if any(phrase in msg_lower for phrase in proceed_phrases):
            state["intake_complete"] = True

        # (b) Max turn count
        if state["turn_count"] >= self.MAX_TURNS:
            state["intake_complete"] = True

        # (a) All high-impact slots filled
        domain = state["domain"]
        schema = load_domain_schema(domain) if domain else None
        if schema:
            high_impact = [s.name for s in schema.slots if s.impact_weight >= 4]
            unfilled_high = [s for s in high_impact if s in state["unfilled_slots"]]
            if not unfilled_high:
                state["intake_complete"] = True

        # On stop: populate missing_information
        if state["intake_complete"]:
            state["missing_information"] = list(state["unfilled_slots"])

        return state

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------

    async def _persist_facts(
        self, matter_id: uuid.UUID, state: IntakeState
    ) -> None:
        """Write filled slots as Fact rows (idempotent — skips existing)."""
        existing = await self.db.execute(
            select(Fact.slot_name).where(Fact.matter_id == matter_id)
        )
        existing_slots = {row[0] for row in existing.all()}

        for slot_name, value in state["filled_slots"].items():
            if slot_name in existing_slots:
                continue
            fact = Fact(
                matter_id=matter_id,
                slot_name=slot_name,
                statement=str(value),
                source_type="user_claim",
                confidence="stated",
            )
            self.db.add(fact)

    @staticmethod
    def _derive_title(message: str) -> str:
        """Generate a short title from the user's opening message."""
        words = message.strip().split()
        if len(words) <= 6:
            return message.strip()
        return " ".join(words[:6]) + "…"

    @staticmethod
    def _to_result(state: IntakeState) -> IntakeResult:
        return IntakeResult(
            matter_id=uuid.UUID(state["matter_id"]),
            question=state.get("current_question"),
            intake_complete=state["intake_complete"],
            facts_collected=[
                {"slot": k, "value": v} for k, v in state["filled_slots"].items()
            ],
            missing_information=state["missing_information"],
            domain=state["domain"],
            filled_slots=state["filled_slots"],
        )
