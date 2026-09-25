import sqlite3

conn = sqlite3.connect("app/src/main/assets/database/nyaai_preloaded.db")
cur = conn.cursor()
cur.execute("SELECT DISTINCT sourcePath, count(*) FROM documents GROUP BY sourcePath")
print("Sources:", cur.fetchall())
cur.execute("SELECT DISTINCT legalDomain, count(*) FROM training_examples GROUP BY legalDomain")
print("Training Domains:", cur.fetchall())
conn.close()
