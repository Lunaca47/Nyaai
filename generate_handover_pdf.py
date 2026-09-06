"""
Generate a beautifully styled HTML version of the handover document.
Open in browser and use Ctrl+P → Save as PDF.
"""
import markdown2
import os

MD_FILE = "NYAAI_Project_Handover.md"
HTML_FILE = "NYAAI_Project_Handover.html"

with open(MD_FILE, "r", encoding="utf-8") as f:
    md_content = f.read()

html_body = markdown2.markdown(
    md_content,
    extras=["tables", "fenced-code-blocks", "code-friendly", "cuddled-lists", "break-on-newline"]
)

html_full = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>NYAAI — Project Handover Document</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap');

  :root {{
    --primary: #1E3A8A;
    --primary-light: #3B82F6;
    --bg: #FFFFFF;
    --text: #1a1a2e;
    --text-muted: #64748b;
    --border: #e2e8f0;
    --code-bg: #f1f5f9;
    --table-header: #1E3A8A;
    --table-stripe: #f8fafc;
    --blockquote-bg: #eff6ff;
    --blockquote-border: #3B82F6;
    --warning-bg: #fef3c7;
    --warning-border: #f59e0b;
    --tip-bg: #ecfdf5;
    --tip-border: #10b981;
    --note-bg: #eff6ff;
    --note-border: #3B82F6;
    --important-bg: #f3e8ff;
    --important-border: #8b5cf6;
  }}

  * {{ margin: 0; padding: 0; box-sizing: border-box; }}

  body {{
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    color: var(--text);
    background: var(--bg);
    line-height: 1.7;
    font-size: 14px;
    max-width: 900px;
    margin: 0 auto;
    padding: 40px 50px;
  }}

  h1 {{
    font-size: 28px;
    font-weight: 700;
    color: var(--primary);
    margin: 30px 0 10px;
    padding-bottom: 8px;
    border-bottom: 3px solid var(--primary);
  }}

  h2 {{
    font-size: 20px;
    font-weight: 600;
    color: var(--primary);
    margin: 28px 0 12px;
    padding-bottom: 6px;
    border-bottom: 2px solid var(--border);
  }}

  h3 {{
    font-size: 16px;
    font-weight: 600;
    color: var(--text);
    margin: 20px 0 8px;
  }}

  p {{ margin: 8px 0; }}

  blockquote {{
    margin: 12px 0;
    padding: 12px 16px;
    border-left: 4px solid var(--blockquote-border);
    background: var(--blockquote-bg);
    border-radius: 0 6px 6px 0;
    font-size: 13px;
  }}

  /* GitHub-style alerts */
  blockquote p:first-child strong:first-child {{
    display: inline;
  }}

  /* WARNING alerts */
  blockquote:has(p:first-child > strong:first-child) {{
    background: var(--note-bg);
    border-left-color: var(--note-border);
  }}

  table {{
    width: 100%;
    border-collapse: collapse;
    margin: 12px 0;
    font-size: 13px;
  }}

  thead th {{
    background: var(--table-header);
    color: white;
    font-weight: 600;
    text-align: left;
    padding: 10px 14px;
  }}

  thead th:first-child {{ border-radius: 6px 0 0 0; }}
  thead th:last-child {{ border-radius: 0 6px 0 0; }}

  tbody td {{
    padding: 9px 14px;
    border-bottom: 1px solid var(--border);
  }}

  tbody tr:nth-child(even) {{ background: var(--table-stripe); }}

  code {{
    font-family: 'JetBrains Mono', 'Consolas', monospace;
    background: var(--code-bg);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 12.5px;
    color: #be185d;
  }}

  pre {{
    background: #0f172a;
    color: #e2e8f0;
    padding: 16px 20px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 12px 0;
    font-size: 12.5px;
    line-height: 1.6;
  }}

  pre code {{
    background: none;
    color: inherit;
    padding: 0;
    font-size: inherit;
  }}

  ul, ol {{
    margin: 8px 0;
    padding-left: 24px;
  }}

  li {{ margin: 4px 0; }}

  hr {{
    border: none;
    height: 1px;
    background: var(--border);
    margin: 24px 0;
  }}

  a {{
    color: var(--primary-light);
    text-decoration: none;
  }}

  strong {{ font-weight: 600; }}

  /* Print styles */
  @media print {{
    body {{
      padding: 20px 30px;
      font-size: 12px;
      max-width: none;
    }}
    h1 {{ font-size: 24px; }}
    h2 {{ font-size: 17px; page-break-after: avoid; }}
    h3 {{ font-size: 14px; page-break-after: avoid; }}
    pre {{ page-break-inside: avoid; font-size: 11px; }}
    table {{ page-break-inside: avoid; font-size: 11.5px; }}
    blockquote {{ page-break-inside: avoid; }}
    .no-print {{ display: none; }}
  }}
</style>
</head>
<body>
{html_body}
</body>
</html>
"""

with open(HTML_FILE, "w", encoding="utf-8") as f:
    f.write(html_full)

print(f"Generated: {os.path.abspath(HTML_FILE)}")
print(f"   Open in browser -> Ctrl+P -> Save as PDF")
