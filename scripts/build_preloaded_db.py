import os
import re
import json
import sqlite3
from pypdf import PdfReader

DB_PATH = os.path.join("app", "src", "main", "assets", "database", "nyaai_preloaded.db")
PDF_DIR = os.path.join("app", "src", "main", "assets")
SCRIPTS_DIR = "scripts"

def clean_legal_text(text):
    lines = text.splitlines()
    cleaned = []
    for line in lines:
        l = line.strip()
        if "GAZETTE" in l.upper() or "PUBLISHED BY AUTHORITY" in l.upper():
            continue
        if len(l) > 3:
            cleaned.append(l)
    full = " ".join(cleaned)
    return re.sub(r'\s+', ' ', full).strip()

def build_database():
    existing_docs = []
    if os.path.exists(DB_PATH):
        try:
            old_conn = sqlite3.connect(DB_PATH)
            existing_docs = old_conn.execute("SELECT sourcePath, content FROM documents").fetchall()
            old_conn.close()
            print(f"Found {len(existing_docs)} existing document records in current DB.")
        except Exception:
            existing_docs = []
        os.remove(DB_PATH)
        print(f"Removed existing DB at {DB_PATH}")

    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    # Exact Room DDL
    cursor.execute("""CREATE VIRTUAL TABLE IF NOT EXISTS `documents` USING FTS4(`sourcePath` TEXT NOT NULL, `content` TEXT NOT NULL);""")
    cursor.execute("""CREATE TABLE IF NOT EXISTS `chat_sessions` (`sessionId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `timestamp` INTEGER NOT NULL);""")
    cursor.execute("""CREATE TABLE IF NOT EXISTS `chat_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `text` TEXT NOT NULL, `isUser` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `feedback` TEXT, `confidence` REAL, FOREIGN KEY(`sessionId`) REFERENCES `chat_sessions`(`sessionId`) ON UPDATE NO ACTION ON DELETE CASCADE );""")
    cursor.execute("""CREATE INDEX IF NOT EXISTS `index_chat_messages_sessionId` ON `chat_messages` (`sessionId`);""")
    cursor.execute("""CREATE TABLE IF NOT EXISTS `training_examples` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `question` TEXT NOT NULL, `answer` TEXT NOT NULL, `sourcePath` TEXT NOT NULL, `legalDomain` TEXT NOT NULL, `reasoningQuality` INTEGER NOT NULL);""")
    cursor.execute("""CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT);""")
    cursor.execute("""INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '969b174290f4e5a39b0a611d265ce172');""")
    conn.commit()

    if existing_docs:
        cursor.executemany("INSERT INTO documents(sourcePath, content) VALUES(?, ?)", existing_docs)
        conn.commit()
        print(f"Restored {len(existing_docs)} existing document chunks.")

    # 1. Process PDFs
    pdf_files = ["coi.pdf", "bns.pdf", "bnss.pdf", "bsa.pdf"]
    chunk_count = len(existing_docs)

    chunk_pattern = re.compile(r'(?=(?:ARTICLE|SECTION|Part)\s+\d+)', re.IGNORECASE)

    for pdf_name in pdf_files:
        pdf_path = os.path.join(PDF_DIR, pdf_name)
        if not os.path.exists(pdf_path):
            print(f"Warning: {pdf_path} not found, skipping.")
            continue

        print(f"Extracting {pdf_name}...")
        reader = PdfReader(pdf_path)
        num_pages = len(reader.pages)
        print(f"  {pdf_name}: {num_pages} pages")

        batch = []
        for page_num in range(1, num_pages + 1):
            try:
                page = reader.pages[page_num - 1]
                page_text = page.extract_text() or ""
                cleaned_page = clean_legal_text(page_text)

                if len(cleaned_page) > 100:
                    chunks = chunk_pattern.split(cleaned_page)
                    for chunk in chunks:
                        c = chunk.strip()
                        if len(c) > 50:
                            content = f"Page {page_num}: {c}"
                            batch.append((pdf_name, content))
                            chunk_count += 1
                            if len(batch) >= 200:
                                cursor.executemany("INSERT INTO documents(sourcePath, content) VALUES(?, ?)", batch)
                                conn.commit()
                                batch.clear()
            except Exception as e:
                print(f"  Error on page {page_num} of {pdf_name}: {e}")

        if batch:
            cursor.executemany("INSERT INTO documents(sourcePath, content) VALUES(?, ?)", batch)
            conn.commit()
            batch.clear()

    print(f"Total legal document chunks inserted: {chunk_count}")

    # 2. Process QA Batches into training_examples
    training_count = 0
    training_batch = []
    all_queries_export = []
    
    # We load the 10,000+ mega batch
    batch_files = [os.path.join(SCRIPTS_DIR, "batch_mega_10k.json")]
    
    for batch_file in batch_files:
        if os.path.exists(batch_file):
            print(f"Loading {batch_file}...")
            try:
                with open(batch_file, "r", encoding="utf-8") as f:
                    items = json.load(f)
                    for item in items:
                        q = item.get("user_query", "").strip()
                        a = item.get("layman_explanation", "").strip()
                        src = item.get("source_act", "").strip()
                        domain = item.get("category", "Indian Law").strip()
                        if q and a:
                            training_batch.append((q, a, src, domain, 1))
                            all_queries_export.append({
                                "question": q,
                                "answer": a,
                                "sourcePath": src,
                                "legalDomain": domain,
                                "reasoningQuality": 1
                            })
                            training_count += 1
            except Exception as e:
                print(f"  Error reading {batch_file}: {e}")

    if training_batch:
        cursor.executemany("INSERT INTO training_examples(question, answer, sourcePath, legalDomain, reasoningQuality) VALUES(?, ?, ?, ?, ?)", training_batch)
        conn.commit()

    # Also export all training queries to assets for runtime seeding fallback
    export_json_path = os.path.join("app", "src", "main", "assets", "preloaded_training_queries.json")
    try:
        with open(export_json_path, "w", encoding="utf-8") as f:
            json.dump(all_queries_export, f, indent=2, ensure_ascii=False)
        print(f"Exported {len(all_queries_export)} training queries to {export_json_path}")
    except Exception as e:
        print(f"Warning: Failed to export {export_json_path}: {e}")

    print(f"Total training examples inserted: {training_count}")

    # Optimize and compact
    print("Running SQLite VACUUM...")
    cursor.execute("VACUUM;")
    conn.commit()
    conn.close()

    db_size_mb = os.path.getsize(DB_PATH) / (1024 * 1024)
    print(f"SUCCESS: Preloaded database created at {DB_PATH} ({db_size_mb:.2f} MB)")

if __name__ == "__main__":
    build_database()
