import json
import numpy as np
from sentence_transformers import SentenceTransformer

def main():
    model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
    emb = np.load("backend/data/statutory_embeddings.npy")
    with open("backend/data/statutory_embeddings_ids.json", "r", encoding="utf-8") as f:
        ids = json.load(f)
    with open("backend/data/statutory_codex.json", "r", encoding="utf-8") as f:
        codex = json.load(f)
    codex_by_id = {d["id"]: d for d in codex}

    query = "my landlord won't give my money back"
    q_vec = model.encode(query, normalize_embeddings=True)
    scores = np.dot(emb, q_vec)

    top_indices = np.argsort(scores)[::-1][:10]
    print(f"Query: \"{query}\"\n")
    print("Top 10 Dense Semantic Results (DENSE LEG ALONE):")
    for rank, idx in enumerate(top_indices, 1):
        doc_id = ids[idx]
        doc = codex_by_id.get(doc_id, {})
        print(f"{rank}. [{doc_id}] {doc.get('act')} {doc.get('section')}: {doc.get('title')} | Cosine Similarity: {scores[idx]:.4f}")

if __name__ == "__main__":
    main()
