"""
Precomputes dense vector embeddings for all provisions in backend/data/statutory_codex.json
using sentence-transformers/all-MiniLM-L6-v2 and saves them to a .npy matrix sidecar.
"""
import os
import sys
import json
import time
import numpy as np
from sentence_transformers import SentenceTransformer

def precompute():
    base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    codex_path = os.path.join(base_dir, "data", "statutory_codex.json")
    embeddings_path = os.path.join(base_dir, "data", "statutory_embeddings.npy")
    ids_path = os.path.join(base_dir, "data", "statutory_embeddings_ids.json")

    print(f"Loading codex from {codex_path}...")
    with open(codex_path, "r", encoding="utf-8") as f:
        codex = json.load(f)

    print(f"Total provisions to encode: {len(codex)}")
    doc_ids = []
    texts_to_encode = []

    for d in codex:
        doc_ids.append(d["id"])
        # Format text to emphasize Act, Section, Title, and substantive content
        text = f"{d.get('act', '')} {d.get('section', '')} {d.get('title', '')}: {d.get('content', '')}"
        texts_to_encode.append(text)

    print("Loading SentenceTransformer model ('sentence-transformers/all-MiniLM-L6-v2')...")
    t0 = time.time()
    model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
    t_load = time.time() - t0
    print(f"Model loaded in {t_load:.2f}s")

    print("Encoding provisions with normalized embeddings (L2 norm = 1.0)...")
    t1 = time.time()
    embeddings = model.encode(
        texts_to_encode,
        batch_size=64,
        show_progress_bar=True,
        normalize_embeddings=True,
        convert_to_numpy=True
    ).astype(np.float32)
    t_encode = time.time() - t1
    print(f"Encoded {len(texts_to_encode)} provisions in {t_encode:.2f}s. Shape: {embeddings.shape}")

    print(f"Saving embeddings matrix to {embeddings_path}...")
    np.save(embeddings_path, embeddings)

    print(f"Saving ID mapping to {ids_path}...")
    with open(ids_path, "w", encoding="utf-8") as f:
        json.dump(doc_ids, f, indent=2)

    print("Precomputation successfully completed!")

if __name__ == "__main__":
    precompute()
