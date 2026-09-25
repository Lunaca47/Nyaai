import hmac
import hashlib
import logging
from datetime import datetime
from typing import List, Optional, Dict, Any, Tuple
from uuid import UUID, uuid4, uuid5, NAMESPACE_URL
from fastapi import APIRouter, Depends, HTTPException, Header, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.config import settings
from app.database import get_db
from app.models.models import Matter, Fact, CaseEvent, EvidenceItem, Citation, Escalation, ActionPlan
from app.schemas.schemas import MatterCreate, MatterResponse, CaseStateResponse

logger = logging.getLogger(__name__)

router = APIRouter()

# In-memory store for fallback when PostgreSQL is unavailable
_memory_matters: Dict[UUID, Dict[str, Any]] = {}


def create_guest_token(user_id: Optional[UUID] = None) -> Tuple[UUID, str]:
    """Generates a cryptographic HMAC-SHA256 signed guest token."""
    uid = user_id or uuid4()
    msg = f"guest_v1:{uid}".encode("utf-8")
    sig = hmac.new(settings.AUTH_SECRET_KEY.encode("utf-8"), msg, hashlib.sha256).hexdigest()
    token = f"guest_v1:{uid}:{sig}"
    return uid, token


def verify_guest_token(token: str) -> Optional[UUID]:
    """Verifies HMAC-SHA256 signature on guest_v1:<uuid>:<sig> tokens."""
    parts = token.strip().split(":")
    if len(parts) == 3 and parts[0] == "guest_v1":
        uid_str, sig = parts[1], parts[2]
        msg = f"guest_v1:{uid_str}".encode("utf-8")
        expected_sig = hmac.new(settings.AUTH_SECRET_KEY.encode("utf-8"), msg, hashlib.sha256).hexdigest()
        if hmac.compare_digest(sig, expected_sig):
            try:
                return UUID(uid_str)
            except ValueError:
                return None
    return None


def verify_token(token: str) -> Optional[UUID]:
    """Verifies Bearer token: signed guest token or Firebase/JWT ID token."""
    token = token.strip()
    if token.startswith("guest_v1:"):
        return verify_guest_token(token)

    # Firebase ID token validation
    try:
        import firebase_admin
        from firebase_admin import auth as fb_auth
        decoded = fb_auth.verify_id_token(token)
        uid = decoded.get("uid")
        if uid:
            return uuid5(NAMESPACE_URL, f"firebase:{uid}")
    except Exception:
        pass

    # Cryptographic JWT signature verification
    try:
        import jwt
        # Cryptographically verify signature using AUTH_SECRET_KEY with HS256
        # Strictly rejects unsigned (alg: none), forged, or wrong-key tokens
        decoded = jwt.decode(
            token,
            settings.AUTH_SECRET_KEY,
            algorithms=["HS256"],
            options={"verify_signature": True, "require": ["sub"]}
        )
        uid = decoded.get("sub") or decoded.get("user_id")
        if uid:
            try:
                return UUID(str(uid))
            except ValueError:
                return uuid5(NAMESPACE_URL, f"jwt:{uid}")
    except Exception:
        pass

    return None


async def get_current_user_id(
    authorization: Optional[str] = Header(None),
    x_guest_token: Optional[str] = Header(None),
    x_user_id: Optional[str] = Header(None),
    user_id: Optional[UUID] = Query(None)
) -> UUID:
    """
    Enforces cryptographic authentication.
    Requires Authorization: Bearer <token> or X-Guest-Token: guest_v1:<uuid>:<sig>.
    Client-declared identity (X-User-Id / user_id) without a valid signed token is rejected.
    """
    token = None
    if authorization and authorization.lower().startswith("bearer "):
        token = authorization[7:].strip()
    elif x_guest_token:
        token = x_guest_token.strip()

    if token:
        resolved_uid = verify_token(token)
        if resolved_uid:
            return resolved_uid
        raise HTTPException(status_code=401, detail="Unauthorized: Invalid, expired, or forged authentication token")

    # Anti-spoofing check: Client attempting to declare identity without token is rejected
    if x_user_id or user_id:
        raise HTTPException(
            status_code=401,
            detail="Unauthorized: Unauthenticated user identity. Client-declared X-User-Id or user_id is rejected without a cryptographic token."
        )

    raise HTTPException(
        status_code=401,
        detail="Unauthorized: Authentication required. Provide 'Authorization: Bearer <token>' or 'X-Guest-Token'."
    )


def _handle_db_failure(operation: str, exc: Exception):
    """
    Guards Matter durability. In production, fails loudly with 503 Service Unavailable
    unless in-memory fallback is explicitly permitted.
    """
    if settings.ENVIRONMENT == "production" and not settings.ALLOW_IN_MEMORY_FALLBACK:
        logger.error(f"PostgreSQL unreachable during {operation} in production: {exc}")
        raise HTTPException(
            status_code=503,
            detail=f"Service Unavailable: Database storage is unreachable during {operation}. In-memory fallback is disabled in production to prevent data loss."
        )
    logger.warning(f"Database unavailable during {operation} ({exc}), using memory store (allowed in {settings.ENVIRONMENT} mode)")


@router.post("/auth/guest-token")
async def issue_guest_token():
    """
    Issues a cryptographically signed HMAC guest token for guest mode clients.
    Format: guest_v1:<uuid>:<sig>
    """
    uid, token = create_guest_token()
    return {
        "guest_token": token,
        "user_id": str(uid),
        "token_type": "Bearer"
    }


@router.post("", response_model=MatterResponse)
async def create_matter(
    matter: MatterCreate,
    caller_id: UUID = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Creates a new legal Matter tied to the authenticated user.
    """
    matter_id = uuid4()
    now = datetime.utcnow()

    # Try DB
    try:
        db_matter = Matter(
            id=matter_id,
            user_id=caller_id,
            title=matter.title,
            domain=matter.domain,
            jurisdiction_state=matter.jurisdiction_state,
            procedural_stage=matter.procedural_stage,
            status=matter.status,
            scratch=matter.scratch or ({"case_state_json": matter.case_state_json} if matter.case_state_json else {})
        )
        db.add(db_matter)
        await db.commit()
        await db.refresh(db_matter)

        # Mirror in memory store
        _memory_matters[matter_id] = {
            "id": matter_id,
            "user_id": caller_id,
            "title": matter.title,
            "domain": matter.domain,
            "jurisdiction_state": matter.jurisdiction_state,
            "procedural_stage": matter.procedural_stage,
            "status": matter.status,
            "created_at": now,
            "updated_at": now,
            "scratch": db_matter.scratch or {},
            "case_state_json": matter.case_state_json
        }

        return MatterResponse(
            id=db_matter.id,
            user_id=db_matter.user_id,
            title=db_matter.title,
            domain=db_matter.domain,
            jurisdiction_state=db_matter.jurisdiction_state,
            procedural_stage=db_matter.procedural_stage,
            status=db_matter.status,
            created_at=db_matter.created_at or now,
            updated_at=db_matter.updated_at or now,
            scratch=db_matter.scratch or {},
            case_state_json=matter.case_state_json or (db_matter.scratch.get("case_state_json") if db_matter.scratch else None)
        )
    except HTTPException:
        raise
    except Exception as e:
        _handle_db_failure("create_matter", e)
        mem_data = {
            "id": matter_id,
            "user_id": caller_id,
            "title": matter.title,
            "domain": matter.domain,
            "jurisdiction_state": matter.jurisdiction_state,
            "procedural_stage": matter.procedural_stage,
            "status": matter.status,
            "created_at": now,
            "updated_at": now,
            "scratch": matter.scratch or {},
            "case_state_json": matter.case_state_json
        }
        _memory_matters[matter_id] = mem_data
        return MatterResponse(**mem_data)


@router.get("", response_model=List[MatterResponse])
async def list_matters(
    caller_id: UUID = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Lists all Matters belonging to the authenticated user.
    """
    try:
        result = await db.execute(select(Matter).where(Matter.user_id == caller_id))
        items = result.scalars().all()
        return [
            MatterResponse(
                id=m.id,
                user_id=m.user_id,
                title=m.title,
                domain=m.domain,
                jurisdiction_state=m.jurisdiction_state,
                procedural_stage=m.procedural_stage,
                status=m.status,
                created_at=m.created_at or datetime.utcnow(),
                updated_at=m.updated_at or datetime.utcnow(),
                scratch=m.scratch or {},
                case_state_json=m.scratch.get("case_state_json") if m.scratch else None
            )
            for m in items
        ]
    except HTTPException:
        raise
    except Exception as e:
        _handle_db_failure("list_matters", e)
        return [
            MatterResponse(**data)
            for data in _memory_matters.values()
            if data["user_id"] == caller_id and data["status"] != "deleted"
        ]


@router.get("/{id}", response_model=MatterResponse)
async def get_matter(
    id: UUID,
    caller_id: UUID = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Retrieves a single Matter by ID, strictly enforcing ownership access control.
    """
    try:
        result = await db.execute(select(Matter).where(Matter.id == id))
        matter = result.scalars().first()
        if matter:
            if matter.user_id != caller_id:
                raise HTTPException(status_code=403, detail="Access forbidden: You do not own this matter")
            return MatterResponse(
                id=matter.id,
                user_id=matter.user_id,
                title=matter.title,
                domain=matter.domain,
                jurisdiction_state=matter.jurisdiction_state,
                procedural_stage=matter.procedural_stage,
                status=matter.status,
                created_at=matter.created_at or datetime.utcnow(),
                updated_at=matter.updated_at or datetime.utcnow(),
                scratch=matter.scratch or {},
                case_state_json=matter.scratch.get("case_state_json") if matter.scratch else None
            )
    except HTTPException:
        raise
    except Exception as e:
        _handle_db_failure("get_matter", e)

    if id in _memory_matters:
        data = _memory_matters[id]
        if data["user_id"] != caller_id:
            raise HTTPException(status_code=403, detail="Access forbidden: You do not own this matter")
        return MatterResponse(**data)

    raise HTTPException(status_code=404, detail="Matter not found")


@router.patch("/{id}", response_model=MatterResponse)
async def update_matter(
    id: UUID,
    matter_update: dict,
    caller_id: UUID = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Updates Matter properties or state, strictly enforcing user ownership.
    """
    try:
        result = await db.execute(select(Matter).where(Matter.id == id))
        matter = result.scalars().first()
        if matter:
            if matter.user_id != caller_id:
                raise HTTPException(status_code=403, detail="Access forbidden: You do not own this matter")
            for k, v in matter_update.items():
                if k == "case_state_json":
                    if not matter.scratch:
                        matter.scratch = {}
                    matter.scratch["case_state_json"] = v
                elif hasattr(matter, k):
                    setattr(matter, k, v)
            matter.updated_at = datetime.utcnow()
            await db.commit()
            await db.refresh(matter)
            return MatterResponse(
                id=matter.id,
                user_id=matter.user_id,
                title=matter.title,
                domain=matter.domain,
                jurisdiction_state=matter.jurisdiction_state,
                procedural_stage=matter.procedural_stage,
                status=matter.status,
                created_at=matter.created_at,
                updated_at=matter.updated_at,
                scratch=matter.scratch or {},
                case_state_json=matter.scratch.get("case_state_json") if matter.scratch else None
            )
    except HTTPException:
        raise
    except Exception as e:
        _handle_db_failure("update_matter", e)

    if id in _memory_matters:
        data = _memory_matters[id]
        if data["user_id"] != caller_id:
            raise HTTPException(status_code=403, detail="Access forbidden: You do not own this matter")
        for k, v in matter_update.items():
            data[k] = v
        data["updated_at"] = datetime.utcnow()
        return MatterResponse(**data)

    raise HTTPException(status_code=404, detail="Matter not found")


@router.delete("/{id}")
async def delete_matter(
    id: UUID,
    caller_id: UUID = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Deletes a Matter, strictly enforcing user ownership.
    """
    try:
        result = await db.execute(select(Matter).where(Matter.id == id))
        matter = result.scalars().first()
        if matter:
            if matter.user_id != caller_id:
                raise HTTPException(status_code=403, detail="Access forbidden: You do not own this matter")
            await db.delete(matter)
            await db.commit()
            _memory_matters.pop(id, None)
            return {"status": "deleted", "id": str(id)}
    except HTTPException:
        raise
    except Exception as e:
        _handle_db_failure("delete_matter", e)

    if id in _memory_matters:
        data = _memory_matters[id]
        if data["user_id"] != caller_id:
            raise HTTPException(status_code=403, detail="Access forbidden: You do not own this matter")
        _memory_matters.pop(id, None)
        return {"status": "deleted", "id": str(id)}

    raise HTTPException(status_code=404, detail="Matter not found")


@router.get("/{id}/state", response_model=CaseStateResponse)
async def get_case_state(
    id: UUID,
    caller_id: UUID = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Retrieves full relational case state, strictly enforcing user ownership.
    """
    try:
        matter_result = await db.execute(select(Matter).where(Matter.id == id))
        matter = matter_result.scalars().first()
        if not matter:
            raise HTTPException(status_code=404, detail="Matter not found")

        if matter.user_id != caller_id:
            raise HTTPException(status_code=403, detail="Access forbidden: You do not own this matter")

        facts = (await db.execute(select(Fact).where(Fact.matter_id == id))).scalars().all()
        events = (await db.execute(select(CaseEvent).where(CaseEvent.matter_id == id))).scalars().all()
        evidence = (await db.execute(select(EvidenceItem).where(EvidenceItem.matter_id == id))).scalars().all()
        citations = (await db.execute(select(Citation).where(Citation.matter_id == id))).scalars().all()
        escalations = (await db.execute(select(Escalation).where(Escalation.matter_id == id))).scalars().all()
        action_plans = (await db.execute(select(ActionPlan).where(ActionPlan.matter_id == id))).scalars().all()

        return CaseStateResponse(
            matter=matter,
            facts=list(facts),
            events=list(events),
            evidence=list(evidence),
            citations=list(citations),
            escalations=list(escalations),
            action_plans=list(action_plans)
        )
    except HTTPException:
        raise
    except Exception as e:
        _handle_db_failure("get_case_state", e)
        if id in _memory_matters:
            data = _memory_matters[id]
            if data["user_id"] != caller_id:
                raise HTTPException(status_code=403, detail="Access forbidden: You do not own this matter")
            mock_matter = Matter(
                id=data["id"],
                user_id=data["user_id"],
                title=data["title"],
                domain=data["domain"],
                jurisdiction_state=data["jurisdiction_state"],
                procedural_stage=data["procedural_stage"],
                status=data["status"],
                scratch=data["scratch"]
            )
            return CaseStateResponse(
                matter=mock_matter,
                facts=[],
                events=[],
                evidence=[],
                citations=[],
                escalations=[],
                action_plans=[]
            )
        raise HTTPException(status_code=404, detail="Matter not found")
