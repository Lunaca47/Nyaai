"""
Statutory Codex & Knowledge Base Ingestion Script.

Ingests statutory provisions from nyaai_preloaded.db and curated statutory sources
into the backend hybrid retrieval knowledge base with structured metadata tags:
- act
- section
- jurisdiction
- status (in_force / repealed)
- effective_date
"""

import os
import re
import json
import sqlite3
import logging
from typing import List, Dict, Any

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

ACT_MAPPINGS = {
    "bns.pdf": "Bharatiya Nyaya Sanhita, 2023",
    "bnss.pdf": "Bharatiya Nagarik Suraksha Sanhita, 2023",
    "bsa.pdf": "Bharatiya Sakshya Adhiniyam, 2023",
    "coi.pdf": "Constitution of India",
}

SECTION_REGEX = re.compile(
    r'(?:(?:Section|Sec\.?|§|Article|Art\.?)\s*(\d+[A-Z]?(?:\(\d+\))?))',
    re.IGNORECASE
)

def ingest_from_preloaded_db(db_path: str) -> List[Dict[str, Any]]:
    if not os.path.exists(db_path):
        logger.warning(f"Database file not found at {db_path}")
        return []

    conn = sqlite3.connect(db_path)
    cur = conn.cursor()
    cur.execute("SELECT sourcePath, content FROM documents")
    rows = cur.fetchall()
    conn.close()

    chunks = []
    for idx, (source_path, content) in enumerate(rows):
        act_name = ACT_MAPPINGS.get(source_path, source_path)
        # Extract section match if present
        sec_match = SECTION_REGEX.search(content)
        sec_str = sec_match.group(0) if sec_match else f"Page {idx+1}"
        
        # Derive title from first line
        lines = [line.strip() for line in content.split("\n") if line.strip()]
        title = lines[0][:80] if lines else sec_str

        chunks.append({
            "id": f"doc_{source_path}_{idx}",
            "act": act_name,
            "section": sec_str,
            "title": title,
            "content": content[:1500],
            "jurisdiction": "central",
            "status": "in_force",
            "effective_date": "2024-07-01",
        })

    logger.info(f"Ingested {len(chunks)} document chunks from {db_path}")
    return chunks

def main():
    default_db = os.path.join("app", "src", "main", "assets", "database", "nyaai_preloaded.db")
    chunks = ingest_from_preloaded_db(default_db)
    
    out_dir = os.path.join("backend", "data")
    os.makedirs(out_dir, exist_ok=True)
    out_file = os.path.join(out_dir, "statutory_codex.json")
    
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(chunks, f, indent=2, ensure_ascii=False)
        
    logger.info(f"Successfully saved {len(chunks)} indexed statutory items to {out_file}")

if __name__ == "__main__":
    main()
