"""initial schema

Revision ID: 001_initial_schema
Revises: 
Create Date: 2024-01-01 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision: str = '001_initial_schema'
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table('users',
    sa.Column('id', postgresql.UUID(as_uuid=True), server_default=sa.text('gen_random_uuid()'), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.Column('preferred_language', sa.String(), server_default='en', nullable=True),
    sa.PrimaryKeyConstraint('id')
    )
    
    op.create_table('matters',
    sa.Column('id', postgresql.UUID(as_uuid=True), server_default=sa.text('gen_random_uuid()'), nullable=False),
    sa.Column('user_id', postgresql.UUID(as_uuid=True), nullable=False),
    sa.Column('title', sa.String(), nullable=False),
    sa.Column('domain', sa.String(), nullable=False),
    sa.Column('jurisdiction_state', sa.String(), nullable=True),
    sa.Column('procedural_stage', sa.String(), server_default='intake', nullable=False),
    sa.Column('status', sa.String(), server_default='active', nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.Column('scratch', postgresql.JSONB(astext_type=sa.Text()), server_default=sa.text("'{}'::jsonb"), nullable=True),
    sa.ForeignKeyConstraint(['user_id'], ['users.id'], ),
    sa.PrimaryKeyConstraint('id')
    )
    
    op.create_table('facts',
    sa.Column('id', postgresql.UUID(as_uuid=True), server_default=sa.text('gen_random_uuid()'), nullable=False),
    sa.Column('matter_id', postgresql.UUID(as_uuid=True), nullable=False),
    sa.Column('slot_name', sa.String(), nullable=True),
    sa.Column('statement', sa.String(), nullable=False),
    sa.Column('source_type', sa.String(), nullable=False),
    sa.Column('confidence', sa.String(), server_default='stated', nullable=False),
    sa.Column('supported_by', postgresql.ARRAY(postgresql.UUID(as_uuid=True)), nullable=True),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.ForeignKeyConstraint(['matter_id'], ['matters.id'], ),
    sa.PrimaryKeyConstraint('id')
    )
    
    op.create_table('case_events',
    sa.Column('id', postgresql.UUID(as_uuid=True), server_default=sa.text('gen_random_uuid()'), nullable=False),
    sa.Column('matter_id', postgresql.UUID(as_uuid=True), nullable=False),
    sa.Column('event_date', sa.Date(), nullable=True),
    sa.Column('description', sa.String(), nullable=False),
    sa.Column('source_fact_id', postgresql.UUID(as_uuid=True), nullable=True),
    sa.ForeignKeyConstraint(['matter_id'], ['matters.id'], ),
    sa.ForeignKeyConstraint(['source_fact_id'], ['facts.id'], ),
    sa.PrimaryKeyConstraint('id')
    )
    
    op.create_table('evidence_items',
    sa.Column('id', postgresql.UUID(as_uuid=True), server_default=sa.text('gen_random_uuid()'), nullable=False),
    sa.Column('matter_id', postgresql.UUID(as_uuid=True), nullable=False),
    sa.Column('object_key', sa.String(), nullable=False),
    sa.Column('doc_type', sa.String(), nullable=True),
    sa.Column('classification_confidence', sa.Float(), nullable=True),
    sa.Column('extracted_text', sa.String(), nullable=True),
    sa.Column('uploaded_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.ForeignKeyConstraint(['matter_id'], ['matters.id'], ),
    sa.PrimaryKeyConstraint('id')
    )
    
    op.create_table('legal_sources',
    sa.Column('id', postgresql.UUID(as_uuid=True), server_default=sa.text('gen_random_uuid()'), nullable=False),
    sa.Column('source_type', sa.String(), nullable=False),
    sa.Column('title', sa.String(), nullable=False),
    sa.Column('act_or_court', sa.String(), nullable=True),
    sa.Column('section_or_para', sa.String(), nullable=True),
    sa.Column('jurisdiction_level', sa.String(), nullable=False),
    sa.Column('applicable_states', postgresql.ARRAY(sa.String()), nullable=True),
    sa.Column('effective_from', sa.Date(), nullable=True),
    sa.Column('effective_to', sa.Date(), nullable=True),
    sa.Column('status', sa.String(), server_default='in_force', nullable=False),
    sa.Column('supersedes', postgresql.UUID(as_uuid=True), nullable=True),
    sa.Column('superseded_by', postgresql.UUID(as_uuid=True), nullable=True),
    sa.Column('still_good_law', sa.Boolean(), server_default=sa.text('true'), nullable=True),
    sa.Column('source_url', sa.String(), nullable=False),
    sa.Column('full_text', sa.String(), nullable=False),
    sa.Column('last_verified_date', sa.Date(), nullable=False),
    sa.Column('qdrant_point_id', sa.String(), nullable=True),
    sa.ForeignKeyConstraint(['superseded_by'], ['legal_sources.id'], ),
    sa.ForeignKeyConstraint(['supersedes'], ['legal_sources.id'], ),
    sa.PrimaryKeyConstraint('id')
    )
    
    op.create_table('citations',
    sa.Column('id', postgresql.UUID(as_uuid=True), server_default=sa.text('gen_random_uuid()'), nullable=False),
    sa.Column('matter_id', postgresql.UUID(as_uuid=True), nullable=False),
    sa.Column('legal_source_id', postgresql.UUID(as_uuid=True), nullable=False),
    sa.Column('retrieved_passage', sa.String(), nullable=False),
    sa.Column('claim_text', sa.String(), nullable=False),
    sa.Column('verified', sa.Boolean(), server_default=sa.text('false'), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.ForeignKeyConstraint(['legal_source_id'], ['legal_sources.id'], ),
    sa.ForeignKeyConstraint(['matter_id'], ['matters.id'], ),
    sa.PrimaryKeyConstraint('id')
    )
    
    op.create_table('escalations',
    sa.Column('id', postgresql.UUID(as_uuid=True), server_default=sa.text('gen_random_uuid()'), nullable=False),
    sa.Column('matter_id', postgresql.UUID(as_uuid=True), nullable=False),
    sa.Column('trigger_reason', sa.String(), nullable=False),
    sa.Column('recommended_path', sa.String(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.ForeignKeyConstraint(['matter_id'], ['matters.id'], ),
    sa.PrimaryKeyConstraint('id')
    )
    
    op.create_table('action_plans',
    sa.Column('id', postgresql.UUID(as_uuid=True), server_default=sa.text('gen_random_uuid()'), nullable=False),
    sa.Column('matter_id', postgresql.UUID(as_uuid=True), nullable=False),
    sa.Column('situation_summary', sa.String(), nullable=True),
    sa.Column('options', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
    sa.Column('evidence_required', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
    sa.Column('immediate_actions', postgresql.ARRAY(sa.String()), nullable=True),
    sa.Column('future_actions', postgresql.ARRAY(sa.String()), nullable=True),
    sa.Column('open_questions', postgresql.ARRAY(sa.String()), nullable=True),
    sa.Column('generated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.ForeignKeyConstraint(['matter_id'], ['matters.id'], ),
    sa.PrimaryKeyConstraint('id')
    )


def downgrade() -> None:
    op.drop_table('action_plans')
    op.drop_table('escalations')
    op.drop_table('citations')
    op.drop_table('legal_sources')
    op.drop_table('evidence_items')
    op.drop_table('case_events')
    op.drop_table('facts')
    op.drop_table('matters')
    op.drop_table('users')
