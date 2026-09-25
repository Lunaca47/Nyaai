import yaml
from pathlib import Path
from typing import Optional
from pydantic import BaseModel

class SlotDefinition(BaseModel):
    name: str
    question_template: str
    impact_weight: int  # 1-5
    extractable_from_document: bool
    value_type: str  # text, number, date, boolean, enum
    enum_values: Optional[list[str]] = None

class DomainSchema(BaseModel):
    domain: str
    label: str
    slots: list[SlotDefinition]

def load_domain_schema(domain: str) -> Optional[DomainSchema]:
    '''Load a domain's slot schema from YAML file.'''
    schema_dir = Path(__file__).parent / 'slot_schemas'
    schema_file = schema_dir / f'{domain}.yaml'
    if not schema_file.exists():
        return None
    with open(schema_file) as f:
        data = yaml.safe_load(f)
    return DomainSchema(**data)

def list_available_domains() -> list[str]:
    '''List all available domain schemas.'''
    schema_dir = Path(__file__).parent / 'slot_schemas'
    return [f.stem for f in schema_dir.glob('*.yaml')]

def load_all_schemas() -> dict[str, DomainSchema]:
    '''Load all domain schemas.'''
    return {domain: load_domain_schema(domain) for domain in list_available_domains() if load_domain_schema(domain) is not None}
