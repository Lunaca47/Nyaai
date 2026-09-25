import json
import numpy as np
from sentence_transformers import SentenceTransformer

def generate_embeddings():
    corpus_path = "backend/data/case_law_corpus.json"
    with open(corpus_path, "r", encoding="utf-8") as f:
        cases = json.load(f)

    model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
    ids = []
    texts = []
    for c in cases:
        ids.append(c["case_id"])
        principles = " ".join(c.get("key_principles", []))
        provisions = " ".join(c.get("statutory_provisions", []))
        judges = ", ".join(c.get("bench_judges", []))
        text = (
            f"{c['case_name']}. Citation: {c['citation']}. Court: {c['court']} ({c['judgment_date']}). "
            f"Bench: {c['bench_strength']} Judges ({judges}). Domain: {c['legal_domain']}. "
            f"Statutory Provisions: {provisions}. "
            f"Ratio Decidendi: {c['ratio_decidendi']} "
            f"Key Principles: {principles} "
            f"Precedent Status: {c['precedent_status']}. {c['currentness_check']}"
        )
        texts.append(text)

    embeddings = model.encode(texts, normalize_embeddings=True, convert_to_numpy=True)
    np.save("backend/data/case_law_embeddings.npy", embeddings)
    with open("backend/data/case_law_embeddings_ids.json", "w", encoding="utf-8") as f:
        json.dump(ids, f, indent=2)
    print(f"Generated case_law_embeddings.npy shape: {embeddings.shape} and ids count: {len(ids)}")

if __name__ == "__main__":
    generate_embeddings()
