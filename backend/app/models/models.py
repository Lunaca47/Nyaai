import uuid
from datetime import datetime, date
from typing import List, Optional, Any
from sqlalchemy import Column, String, DateTime, Date, ForeignKey, Boolean, Float
from sqlalchemy.dialects.postgresql import UUID, JSONB, ARRAY
from sqlalchemy.orm import relationship
from app.database import Base

class User(Base):
    __tablename__ = 'users'
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)
    preferred_language = Column(String, default='en')
    matters = relationship("Matter", back_populates="user")

class Matter(Base):
    __tablename__ = 'matters'
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey('users.id'), nullable=False)
    title = Column(String, nullable=False)
    domain = Column(String, nullable=False)
    jurisdiction_state = Column(String)
    procedural_stage = Column(String, nullable=False, default='intake')
    status = Column(String, nullable=False, default='active')
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)
    updated_at = Column(DateTime(timezone=True), default=datetime.utcnow, onupdate=datetime.utcnow)
    scratch = Column(JSONB, default={})

    user = relationship("User", back_populates="matters")
    facts = relationship("Fact", back_populates="matter")
    events = relationship("CaseEvent", back_populates="matter")
    evidence_items = relationship("EvidenceItem", back_populates="matter")
    citations = relationship("Citation", back_populates="matter")
    escalations = relationship("Escalation", back_populates="matter")
    action_plans = relationship("ActionPlan", back_populates="matter")

class Fact(Base):
    __tablename__ = 'facts'
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    matter_id = Column(UUID(as_uuid=True), ForeignKey('matters.id'), nullable=False)
    slot_name = Column(String)
    statement = Column(String, nullable=False)
    source_type = Column(String, nullable=False)
    confidence = Column(String, nullable=False, default='stated')
    supported_by = Column(ARRAY(UUID(as_uuid=True)))
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)
    
    matter = relationship("Matter", back_populates="facts")
    events = relationship("CaseEvent", back_populates="source_fact")

class CaseEvent(Base):
    __tablename__ = 'case_events'
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    matter_id = Column(UUID(as_uuid=True), ForeignKey('matters.id'), nullable=False)
    event_date = Column(Date)
    description = Column(String, nullable=False)
    source_fact_id = Column(UUID(as_uuid=True), ForeignKey('facts.id'))
    
    matter = relationship("Matter", back_populates="events")
    source_fact = relationship("Fact", back_populates="events")

class EvidenceItem(Base):
    __tablename__ = 'evidence_items'
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    matter_id = Column(UUID(as_uuid=True), ForeignKey('matters.id'), nullable=False)
    object_key = Column(String, nullable=False)
    doc_type = Column(String)
    classification_confidence = Column(Float)
    extracted_text = Column(String)
    uploaded_at = Column(DateTime(timezone=True), default=datetime.utcnow)
    
    matter = relationship("Matter", back_populates="evidence_items")

class LegalSource(Base):
    __tablename__ = 'legal_sources'
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    source_type = Column(String, nullable=False)
    title = Column(String, nullable=False)
    act_or_court = Column(String)
    section_or_para = Column(String)
    jurisdiction_level = Column(String, nullable=False)
    applicable_states = Column(ARRAY(String))
    effective_from = Column(Date)
    effective_to = Column(Date)
    status = Column(String, nullable=False, default='in_force')
    supersedes = Column(UUID(as_uuid=True), ForeignKey('legal_sources.id'))
    superseded_by = Column(UUID(as_uuid=True), ForeignKey('legal_sources.id'))
    still_good_law = Column(Boolean, default=True)
    source_url = Column(String, nullable=False)
    full_text = Column(String, nullable=False)
    last_verified_date = Column(Date, nullable=False)
    qdrant_point_id = Column(String)

    supersedes_source = relationship("LegalSource", remote_side=[id], foreign_keys=[supersedes])
    superseded_by_source = relationship("LegalSource", remote_side=[id], foreign_keys=[superseded_by])
    citations = relationship("Citation", back_populates="legal_source")

class Citation(Base):
    __tablename__ = 'citations'
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    matter_id = Column(UUID(as_uuid=True), ForeignKey('matters.id'), nullable=False)
    legal_source_id = Column(UUID(as_uuid=True), ForeignKey('legal_sources.id'), nullable=False)
    retrieved_passage = Column(String, nullable=False)
    claim_text = Column(String, nullable=False)
    verified = Column(Boolean, nullable=False, default=False)
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)
    
    matter = relationship("Matter", back_populates="citations")
    legal_source = relationship("LegalSource", back_populates="citations")

class Escalation(Base):
    __tablename__ = 'escalations'
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    matter_id = Column(UUID(as_uuid=True), ForeignKey('matters.id'), nullable=False)
    trigger_reason = Column(String, nullable=False)
    recommended_path = Column(String, nullable=False)
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)
    
    matter = relationship("Matter", back_populates="escalations")

class ActionPlan(Base):
    __tablename__ = 'action_plans'
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    matter_id = Column(UUID(as_uuid=True), ForeignKey('matters.id'), nullable=False)
    situation_summary = Column(String)
    options = Column(JSONB)
    evidence_required = Column(JSONB)
    immediate_actions = Column(ARRAY(String))
    future_actions = Column(ARRAY(String))
    open_questions = Column(ARRAY(String))
    generated_at = Column(DateTime(timezone=True), default=datetime.utcnow)
    
    matter = relationship("Matter", back_populates="action_plans")
