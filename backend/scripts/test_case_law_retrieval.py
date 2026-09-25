import sys
import os

# Ensure backend directory is in path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.retrieval.hybrid_search import hybrid_search_engine

def run_retrieval_tests():
    queries = [
        "can police refuse to register an FIR",
        "anticipatory bail conditions",
        "is electronic WhatsApp evidence admissible without certificate",
        "unlawful arrest without notice under section 41A",
        "fundamental right to privacy telephone tapping",
    ]

    print("================================================================================")
    print("NYAAI V2 — CASE LAW RETRIEVAL LEG EVALUATION")
    print(f"Total Precedents Loaded in Engine: {len(hybrid_search_engine.case_law_entries)}")
    print(f"Total Statutes Loaded in Statutory Codex: {len(hybrid_search_engine.documents)}")
    print("================================================================================\n")

    for q_idx, query in enumerate(queries, 1):
        print(f"--- QUERY {q_idx}: \"{query}\" ---")
        results = hybrid_search_engine.search_case_law(query, limit=3)
        if not results:
            print("  NO RESULTS RETURNED!")
        for rank, (chunk, score, mode) in enumerate(results, 1):
            print(f"  Rank #{rank}: [{mode.upper()}] Score: {score:.5f}")
            print(f"    Case: {chunk.case_name} | {chunk.citation}")
            print(f"    Court/Date: {chunk.court} ({chunk.judgment_date}) | Bench ({chunk.bench_strength}): {', '.join(chunk.bench_judges)}")
            print(f"    Domain: {chunk.legal_domain} | Status: {chunk.precedent_status}")
            print(f"    Provisions: {', '.join(chunk.statutory_provisions)}")
            print(f"    Ratio: {chunk.ratio_decidendi[:140]}...")
            print()
        print("--------------------------------------------------------------------------------\n")

if __name__ == "__main__":
    run_retrieval_tests()
