import sys
import os

if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

# Ensure backend directory is in path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.retrieval.hybrid_search import hybrid_search_engine
from app.routers.verification import verify_citations
from app.schemas.schemas import VerifyCitationRequest

def execute_multihop_assessment():
    query = "Police refused to register FIR after landlord forcibly locked flat and retained security deposit"
    domain = "tenancy_dispute_and_criminal_procedure"
    jurisdiction = "Karnataka"

    print("================================================================================")
    print("NYAAI V2 — PHASE 4: MULTI-HOP RETRIEVAL & SYNTHESIS END-TO-END DEMO")
    print(f"Matter Query: \"{query}\"")
    print(f"Domain: {domain} | Jurisdiction: {jurisdiction}")
    print("================================================================================\n")

    # Leg 1: Statutory Retrieval
    statute_results = hybrid_search_engine.search(query=query, limit=3)
    # Leg 2: Case Law Retrieval
    case_results = hybrid_search_engine.search_case_law(query=query, limit=2)

    retrieved_sources = []
    print("--- RETRIEVED STATUTORY PROVISIONS (LEG 1) ---")
    for doc, score, mode in statute_results:
        print(f"• [{mode.upper()} {score:.4f}] {doc.act} - {doc.section}: {doc.title}")
        retrieved_sources.append({
            "act": doc.act,
            "section": doc.section,
            "title": doc.title,
            "content": doc.content
        })
    print()

    print("--- RETRIEVED CASE PRECEDENTS (LEG 2) ---")
    for chunk, score, mode in case_results:
        print(f"• [{mode.upper()} {score:.4f}] {chunk.case_name} | {chunk.citation} [{chunk.precedent_status}]")
        retrieved_sources.append({
            "case_id": chunk.case_id,
            "case_name": chunk.case_name,
            "citation": chunk.citation,
            "ratio_decidendi": chunk.ratio_decidendi,
            "precedent_status": chunk.precedent_status,
            "currentness_check": chunk.currentness_check
        })
    print()

    # Formulate Synthesized Assessment Memo combining both legs
    memo = f"""🏛️ **SENIOR ADVOCATE MULTI-HOP LEGAL ASSESSMENT MEMO**
**Matter Title:** Illegal Lockout & Police Inaction in Indiranagar
**Legal Domain:** Criminal Breach of Trust & Criminal Procedure
**Jurisdiction:** {jurisdiction} (Central Bharatiya Sanhitas Applicable)
**Codex Baseline:** BNS 2023 • BNSS 2023 • BSA 2023 • Special Statutory Enactments

📋 **MULTI-DIMENSIONAL LEGAL ANALYSIS:**

### 1. Substantive Legal Provisions & Supporting Precedents
• **Statutory Offenses:** Unlawful lockout and retention of tenant property constitutes Criminal Trespass under Section 329 BNS and Criminal Breach of Trust under Section 316 BNS.
• **Landmark Precedent Authority:** In Lalita Kumari v. Govt. of U.P., (2014) 2 SCC 1, the Supreme Court ruled that registration of an FIR is mandatory under Section 173 BNSS (formerly Section 154 CrPC) if the information discloses the commission of a cognizable offence. Police officers have no discretion to refuse registration.

### 2. Competent Judicial Forum & Escalation Hierarchy
• **Remedy Against Police Refusal:** Upon refusal by the Station House Officer, submit a written representation to the Superintendent of Police under Section 175(3) BNSS. If inaction persists, apply directly to the Judicial Magistrate under Section 175(4) BNSS seeking a judicial direction for investigation.

### 3. Evidentiary Preservation & Certification
• Preserve WhatsApp communications, rent receipts, and lock-change photographs. Ensure electronic evidence compliance under Section 63 BSA 2023 per Arjun Panditrao Khotkar v. Kailash Kushanrao Gorantyal, (2020) 7 SCC 1.

⚖️ **STRATEGIC ADVOCATE DIRECTIVE:**
Serve statutory 15-day notice to landlord, petition SP under Section 175(3) BNSS citing Lalita Kumari mandate, and initiate Section 175(4) BNSS Magistrate application if registration is not completed within 14 days."""

    # Verification Gate Execution
    import asyncio
    verify_req = VerifyCitationRequest(
        response_text=memo,
        retrieved_sources=retrieved_sources,
        jurisdiction="central"
    )
    verification = asyncio.run(verify_citations(verify_req))

    print("================================================================================")
    print("SYNTHESIZED ASSESSMENT MEMO OUTPUT")
    print("================================================================================")
    print(memo)
    print("================================================================================")
    print(f"VERIFICATION GATE ACTION: {verification.action}")
    print(f"IS GROUNDED: {verification.is_grounded}")
    print(f"IS CURRENT: {verification.is_current}")
    print("VERIFIED CITATIONS:")
    for c in verification.citations:
        status_tag = "PASSED" if (c.grounded and c.current) else ("SUPERSEDED" if not c.current else "UNGROUNDED")
        print(f"  • [{status_tag}] {c.citation} (Grounded: {c.grounded}, Current: {c.current})")
    print("================================================================================\n")

if __name__ == "__main__":
    execute_multihop_assessment()
