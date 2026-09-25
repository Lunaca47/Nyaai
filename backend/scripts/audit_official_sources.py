"""
Audit of Official Indian Government Legal and Judicial Domains.
Tests live reachability, latency, SSL validity, and access barriers (CAPTCHA, WAF, JS rendering).
"""
import sys
import time
import json
import ssl
from typing import Dict, Any, List
import urllib.request
import urllib.error

# Ensure UTF-8 console output on Windows
sys.stdout.reconfigure(encoding='utf-8')

TARGETS = [
    {
        "domain": "indiacode.gov.in",
        "name": "India Code (National DSpace 9.1 Portal)",
        "url": "https://indiacode.gov.in/",
        "sample_content_url": "https://indiacode.gov.in/handle/123456789/1362"
    },
    {
        "domain": "indiacode.gov.in/server/api",
        "name": "India Code (Official REST Discovery API)",
        "url": "https://indiacode.gov.in/server/api",
        "sample_content_url": "https://indiacode.gov.in/server/api/discover/search/objects?query=cheque&size=2"
    },
    {
        "domain": "indiacode.nic.in",
        "name": "India Code (Legacy NIC Domain - Redirects to .gov.in)",
        "url": "https://www.indiacode.nic.in/",
        "sample_content_url": "https://www.indiacode.nic.in/"
    },
    {
        "domain": "legislative.gov.in",
        "name": "Legislative Department (Min. of Law & Justice)",
        "url": "https://legislative.gov.in/",
        "sample_content_url": "https://legislative.gov.in/constitution-of-india/"
    },
    {
        "domain": "egazette.gov.in",
        "name": "The Gazette of India (NIC)",
        "url": "https://egazette.gov.in/",
        "sample_content_url": "https://egazette.gov.in/"
    },
    {
        "domain": "mohua.gov.in",
        "name": "MoHUA (Model Tenancy Act)",
        "url": "https://mohua.gov.in/",
        "sample_content_url": "https://mohua.gov.in/upload/uploadfiles/files/Model_Tenancy_Act_English.pdf"
    },
    {
        "domain": "rbi.org.in",
        "name": "Reserve Bank of India (Regulatory Acts & Notifications)",
        "url": "https://www.rbi.org.in/",
        "sample_content_url": "https://www.rbi.org.in/scripts/BS_ViewMasCirculardetails.aspx"
    },
    {
        "domain": "sci.gov.in",
        "name": "Supreme Court of India (Official Portal)",
        "url": "https://www.sci.gov.in/",
        "sample_content_url": "https://www.sci.gov.in/judgments/"
    },
    {
        "domain": "digiscr.sci.gov.in",
        "name": "Digital Supreme Court Reports (e-SCR)",
        "url": "https://digiscr.sci.gov.in/",
        "sample_content_url": "https://digiscr.sci.gov.in/"
    },
    {
        "domain": "services.ecourts.gov.in",
        "name": "eCourts Services (Case Status Portal)",
        "url": "https://services.ecourts.gov.in/ecourtindia_v6/",
        "sample_content_url": "https://services.ecourts.gov.in/ecourtindia_v6/"
    },
    {
        "domain": "delhihighcourt.nic.in",
        "name": "Delhi High Court (Orders & Case Status)",
        "url": "https://delhihighcourt.nic.in/",
        "sample_content_url": "https://delhihighcourt.nic.in/"
    },
    {
        "domain": "bombayhighcourt.nic.in",
        "name": "Bombay High Court (Official Portal)",
        "url": "https://bombayhighcourt.nic.in/",
        "sample_content_url": "https://bombayhighcourt.nic.in/"
    },
    {
        "domain": "karnatakajudiciary.kar.nic.in",
        "name": "Karnataka High Court",
        "url": "https://karnatakajudiciary.kar.nic.in/",
        "sample_content_url": "https://karnatakajudiciary.kar.nic.in/"
    }
]

def test_endpoint(url: str, timeout: int = 6) -> Dict[str, Any]:
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 NyaaiLegalAuditor/2.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.5",
    }
    req = urllib.request.Request(url, headers=headers)
    
    start_time = time.time()
    try:
        # First try strict SSL
        with urllib.request.urlopen(req, timeout=timeout) as response:
            latency = time.time() - start_time
            code = response.getcode()
            content_type = response.headers.get("Content-Type", "")
            data = response.read(20480) # read first 20KB to check for CAPTCHA/JS challenges
            text_preview = data.decode("utf-8", errors="ignore").lower()
            
            blockers = []
            if "captcha" in text_preview:
                blockers.append("CAPTCHA challenge detected")
            if "cf-browser-verification" in text_preview or "cloudflare" in text_preview:
                blockers.append("Cloudflare challenge")
            if "<script" in text_preview and "document.cookie" in text_preview:
                blockers.append("JavaScript cookie challenge")
            if "access denied" in text_preview or "403 forbidden" in text_preview:
                blockers.append("WAF / Geo-block")
            if "enable javascript" in text_preview:
                blockers.append("Requires JavaScript SPA rendering")
                
            return {
                "reachable": True,
                "status_code": code,
                "latency_sec": round(latency, 2),
                "ssl_valid": True,
                "content_type": content_type,
                "blockers": blockers,
                "note": "OK" if not blockers else f"Protected: {', '.join(blockers)}"
            }
    except urllib.error.HTTPError as e:
        latency = time.time() - start_time
        return {
            "reachable": False,
            "status_code": e.code,
            "latency_sec": round(latency, 2),
            "ssl_valid": True,
            "content_type": "",
            "blockers": [f"HTTP {e.code} ({e.reason})"],
            "note": f"HTTP {e.code}"
        }
    except urllib.error.URLError as e:
        latency = time.time() - start_time
        reason_str = str(e.reason)
        ssl_issue = "CERTIFICATE_VERIFY_FAILED" in reason_str or "ssl" in reason_str.lower()
        
        # Test if unverified SSL works
        unverified_ok = False
        if ssl_issue:
            try:
                ctx = ssl._create_unverified_context()
                with urllib.request.urlopen(req, context=ctx, timeout=timeout) as u_resp:
                    unverified_ok = True
            except Exception:
                pass
                
        blocker = "SSL Handshake / Certificate failure" if ssl_issue else f"Network unreachable: {reason_str}"
        if unverified_ok:
            blocker += " (Reachable only with unverified SSL context)"
            
        return {
            "reachable": unverified_ok,
            "status_code": 0,
            "latency_sec": round(latency, 2),
            "ssl_valid": not ssl_issue,
            "content_type": "",
            "blockers": [blocker],
            "note": blocker
        }
    except Exception as e:
        latency = time.time() - start_time
        return {
            "reachable": False,
            "status_code": 0,
            "latency_sec": round(latency, 2),
            "ssl_valid": False,
            "content_type": "",
            "blockers": [str(e)],
            "note": str(e)
        }

def run_audit():
    print("=" * 80)
    print("NYAAI V2 — OFFICIAL GOVERNMENT SOURCE REACHABILITY AUDIT")
    print("Testing live endpoints for latency, SSL, and server-side automation blockers")
    print("=" * 80)
    
    results = []
    for item in TARGETS:
        print(f"\n[AUDIT] Testing {item['name']} ({item['domain']})...")
        res = test_endpoint(item["url"])
        
        # If reachable, also test sample content URL if different
        content_res = None
        if res["reachable"] and item["sample_content_url"] != item["url"]:
            content_res = test_endpoint(item["sample_content_url"])
            
        final_blockers = list(res["blockers"])
        if content_res and content_res["blockers"]:
            final_blockers.extend(content_res["blockers"])
            
        final_reachable = res["reachable"] and (content_res["reachable"] if content_res else True)
        
        entry = {
            "domain": item["domain"],
            "name": item["name"],
            "endpoint": item["url"],
            "reachable": "YES" if final_reachable else ("PARTIAL (SSL)" if res["reachable"] else "NO"),
            "latency_sec": res["latency_sec"],
            "status_code": res["status_code"],
            "blockers": ", ".join(final_blockers) if final_blockers else "None (Directly Fetchable HTML/PDF)"
        }
        results.append(entry)
        print(f" -> Reachable: {entry['reachable']} | Latency: {entry['latency_sec']}s | Status: {entry['status_code']} | Blockers: {entry['blockers']}")

    print("\n" + "=" * 80)
    print("FINAL AUDIT RESULTS TABLE (MARKDOWN FORMAT)")
    print("=" * 80)
    print("| Domain | Name / Authority | Reachable | Latency | Status | Automation Blockers Found |")
    print("|---|---|---|---|---|---|")
    for r in results:
        print(f"| `{r['domain']}` | {r['name']} | **{r['reachable']}** | {r['latency_sec']}s | {r['status_code']} | {r['blockers']} |")
        
    with open("backend/data/official_source_audit.json", "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2)
    print(f"\nSaved raw audit data to backend/data/official_source_audit.json")

if __name__ == "__main__":
    run_audit()
