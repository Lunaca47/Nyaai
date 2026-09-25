"""
Intake API endpoint.

POST /api/v1/intake — accepts a user message (with optional matter_id for
continuation) and returns the next question or a completion signal.
"""

import uuid

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.llm_client import llm_client
from app.schemas.schemas import IntakeMessage, IntakeResponse, FactResponse
from app.intake.engine import IntakeEngine

router = APIRouter()


@router.post("", response_model=IntakeResponse)
async def intake(
    message: IntakeMessage,
    db: AsyncSession = Depends(get_db),
) -> IntakeResponse:
    """Process one turn of the intake dialogue.

    - If ``matter_id`` is *null*, a new Matter is created and intake begins.
    - If ``matter_id`` is provided, the existing Matter's intake continues.
    """
    engine = IntakeEngine(db_session=db, llm_client=llm_client)

    try:
        if message.matter_id is None:
            # New matter — need a user_id.  For now, create a throwaway user
            # until auth is wired up.  In production this comes from the
            # auth middleware.
            user_id = message.user_id or uuid.uuid4()
            result = await engine.start(message.user_message, user_id)
        else:
            result = await engine.continue_intake(
                matter_id=message.matter_id,
                user_message=message.user_message,
            )
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc

    return IntakeResponse(
        matter_id=result.matter_id,
        question=result.question,
        intake_complete=result.intake_complete,
        facts_collected=[],  # facts are persisted in DB, not echoed in bulk
        missing_information=result.missing_information,
        domain=result.domain,
        filled_slots=result.filled_slots,
    )
