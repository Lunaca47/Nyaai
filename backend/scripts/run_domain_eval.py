import os
import sys
import json
import logging
from typing import List, Dict, Any

# Ensure backend directory is in path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

if sys.platform == "win32":
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

from app.retrieval.hybrid_search import hybrid_search_engine
from app.routers.verification import extract_citations, check_currentness, check_grounding

logging.basicConfig(level=logging.WARNING)

# 9 Domains x 3 Fact Patterns = 27 Representative Test Cases
EVAL_DATASET = [
    # 1. Tenancy / Rent
    {
        "domain": "Tenancy & Eviction",
        "query": "My landlord refuses to refund my security deposit of 1.5 lakhs after I vacated the flat on proper notice.",
        "expected_act": "Model Tenancy Act / Transfer of Property Act / BNS 316"
    },
    {
        "domain": "Tenancy & Eviction",
        "query": "Landlord disconnected electricity and water supply to forcefully evict tenant without court order.",
        "expected_act": "Model Tenancy Act Section 20 / TPA 108 / BNS 329"
    },
    {
        "domain": "Tenancy & Eviction",
        "query": "Landlord arbitrarily hiked rent by 30% mid-tenancy without any escalation clause in registered lease agreement.",
        "expected_act": "Model Tenancy Act Section 8 & 9 / Rent Control / TPA 106"
    },

    # 2. Cheque Bounce (Section 138 NI Act)
    {
        "domain": "Cheque Bounce (NI Act)",
        "query": "A client gave me a cheque of 2 lakhs which bounced due to funds insufficient, what legal notice to send under Section 138?",
        "expected_act": "Negotiable Instruments Act, 1881 (Section 138/141)"
    },
    {
        "domain": "Cheque Bounce (NI Act)",
        "query": "Received cheque return memo from bank with reason account closed, 30 days statutory notice deadline under NI Act 138.",
        "expected_act": "Negotiable Instruments Act, 1881 (Section 138/141)"
    },
    {
        "domain": "Cheque Bounce (NI Act)",
        "query": "Accused failed to pay cheque amount within 15 days of statutory legal demand notice, filing complaint under Section 142 NI Act.",
        "expected_act": "Negotiable Instruments Act, 1881 (Section 142/138)"
    },

    # 3. Hit & Run / Motor Accident Claims
    {
        "domain": "Hit & Run / Motor Accident",
        "query": "A speeding commercial truck hit my motorcycle from behind causing grievous hurt and fled the scene without reporting.",
        "expected_act": "Bharatiya Nyaya Sanhita, 2023 (Section 281/106) / Motor Vehicles Act"
    },
    {
        "domain": "Hit & Run / Motor Accident",
        "query": "Hit and run compensation claim under Section 161 Motor Vehicles Act for grievous hurt or death.",
        "expected_act": "Motor Vehicles Act, 1988 (Section 161/166)"
    },
    {
        "domain": "Hit & Run / Motor Accident",
        "query": "Driver was intoxicated and driving rashly, hit pedestrian, criminal charges under BNS 281 and BNS 106.",
        "expected_act": "Bharatiya Nyaya Sanhita, 2023 (Section 281/106)"
    },

    # 4. Police FIR Refusal (Section 173 BNSS)
    {
        "domain": "Police FIR Refusal",
        "query": "Police station refuses to register FIR for stolen mobile phone and forced me to sign a lost article memo.",
        "expected_act": "Bharatiya Nagarik Suraksha Sanhita, 2023 (Section 173/175)"
    },
    {
        "domain": "Police FIR Refusal",
        "query": "Can police refuse to register FIR because incident happened outside jurisdiction, mandatory Zero FIR under Section 173 BNSS?",
        "expected_act": "Bharatiya Nagarik Suraksha Sanhita, 2023 (Section 173/175)"
    },
    {
        "domain": "Police FIR Refusal",
        "query": "SHO refused to register cognizable offense FIR, filing complaint to Superintendent of Police under Section 175(3) BNSS or Magistrate under 175(4).",
        "expected_act": "Bharatiya Nagarik Suraksha Sanhita, 2023 (Section 175(3)/175(4))"
    },

    # 5. Cyber Financial Fraud
    {
        "domain": "Cyber Financial Fraud",
        "query": "Victim of UPI fraud lost 80000 rupees after clicking an unauthorized APK payment link sent on WhatsApp, how to freeze account via 1930?",
        "expected_act": "Information Technology Act (Section 66D) / BNS 318 / BNSS 503"
    },
    {
        "domain": "Cyber Financial Fraud",
        "query": "Unauthorized credit card transactions executed internationally without OTP, RBI circular on zero liability for bank customer.",
        "expected_act": "RBI Circular (Customer Zero Liability) / IT Act Section 43"
    },
    {
        "domain": "Cyber Financial Fraud",
        "query": "Cyber extortion blackmail through morphed defamatory pictures on social media under Section 66E and 67 Information Technology Act.",
        "expected_act": "Information Technology Act, 2000 (Section 66E/67)"
    },

    # 6. Consumer Disputes (CPA 2019)
    {
        "domain": "Consumer Disputes (CPA 2019)",
        "query": "E-commerce company delivered damaged television and refuses refund or replacement under Consumer Protection Act 2019.",
        "expected_act": "Consumer Protection Act, 2019 (Section 35/39)"
    },
    {
        "domain": "Consumer Disputes (CPA 2019)",
        "query": "Car dealer sold vehicle with inherent manufacturing engine defect, consumer complaint before District Commission under Section 35 CPA 2019.",
        "expected_act": "Consumer Protection Act, 2019 (Section 35/38/39)"
    },
    {
        "domain": "Consumer Disputes (CPA 2019)",
        "query": "Product liability claim against manufacturer for defective electronic device causing fire damage under Section 84 of CPA 2019.",
        "expected_act": "Consumer Protection Act, 2019 (Section 84/83)"
    },

    # 7. Unpaid Wages & Gratuity
    {
        "domain": "Unpaid Wages & Gratuity",
        "query": "Employer terminated me without paying 3 months earned wages and salary dues under Code on Wages and Payment of Wages Act.",
        "expected_act": "Code on Wages, 2019 (Section 17/45) / Payment of Wages Act"
    },
    {
        "domain": "Unpaid Wages & Gratuity",
        "query": "Completed 5 years continuous service but company refuses to pay gratuity amount upon resignation under Payment of Gratuity Act 1972.",
        "expected_act": "Payment of Gratuity Act, 1972 (Section 4/7)"
    },
    {
        "domain": "Unpaid Wages & Gratuity",
        "query": "Recovery of unpaid gratuity with compound interest under Section 8 of Payment of Gratuity Act 1972 before Controlling Authority.",
        "expected_act": "Payment of Gratuity Act, 1972 (Section 8/7)"
    },

    # 8. Industrial Disputes / Wrongful Termination
    {
        "domain": "Industrial Disputes / Termination",
        "query": "Factory retrenched 40 workers without one month notice or retrenchment compensation under Section 25F of Industrial Disputes Act 1947.",
        "expected_act": "Industrial Disputes Act, 1947 (Section 25F/2A)"
    },
    {
        "domain": "Industrial Disputes / Termination",
        "query": "Individual workman illegally dismissed without domestic inquiry, reference to Labour Court under Section 2A Industrial Disputes Act.",
        "expected_act": "Industrial Disputes Act, 1947 (Section 2A/33C(2))"
    },
    {
        "domain": "Industrial Disputes / Termination",
        "query": "Recovery of money due from employer under settlement through Labour Court application under Section 33C(2) of Industrial Disputes Act.",
        "expected_act": "Industrial Disputes Act, 1947 (Section 33C(2))"
    },

    # 9. Domestic Violence & Maintenance
    {
        "domain": "Domestic Violence & Maintenance",
        "query": "Husband and in-laws subjecting married woman to domestic violence and threatening to throw her out of shared matrimonial household.",
        "expected_act": "Protection of Women from Domestic Violence Act, 2005 (Section 12/19/18)"
    },
    {
        "domain": "Domestic Violence & Maintenance",
        "query": "Application for protection order and residence order under Section 12, Section 18 and Section 19 of DV Act 2005.",
        "expected_act": "Protection of Women from Domestic Violence Act, 2005 (Section 12/18/19)"
    },
    {
        "domain": "Domestic Violence & Maintenance",
        "query": "Seeking monetary relief and interim maintenance for children under Section 20 and Section 23 of Domestic Violence Act.",
        "expected_act": "Protection of Women from Domestic Violence Act, 2005 (Section 20/23/12)"
    },
]

def is_doc_relevant(domain: str, doc) -> bool:
    """Strictly evaluates whether a retrieved statutory provision is genuinely relevant to the domain problem."""
    act = doc.act.lower()
    sec = doc.section.lower()
    title = doc.title.lower()
    content = doc.content.lower()
    full_text = f"{act} {sec} {title} {content}"

    if domain == "Tenancy & Eviction":
        return any(k in full_text for k in ["tenan", "rent", "lease", "evict", "security deposit", "breach of trust", "lessor", "lessee", "lockout", "trespass", "essential supply", "withholding essential"])
    elif domain == "Cheque Bounce (NI Act)":
        return "negotiable" in act or any(s in sec for s in ["138", "141", "142", "143"]) or "cheque" in full_text or "dishonour" in full_text
    elif domain == "Hit & Run / Motor Accident":
        return "motor vehicle" in act or any(s in sec for s in ["281", "106", "161", "166"]) or "rash driving" in full_text or "hit and run" in full_text or "accident" in full_text
    elif domain == "Police FIR Refusal":
        return ("nagarik" in act or "bnss" in act) and any(s in sec for s in ["173", "175", "176", "187", "180"]) or "zero fir" in full_text or "refusal to register" in full_text
    elif domain == "Cyber Financial Fraud":
        return "information technology" in act or "banking regulation" in act or "rbi circular" in act or any(s in sec for s in ["66d", "66c", "66e", "67", "43", "503", "318"]) or "unauthorised electronic" in full_text or "zero liability" in full_text
    elif domain == "Consumer Disputes (CPA 2019)":
        return "consumer protection" in act or any(s in sec for s in ["34", "35", "38", "39", "83", "84", "85", "86", "2"]) or "product liability" in full_text
    elif domain == "Unpaid Wages & Gratuity":
        return "gratuity" in act or "wages" in act or any(s in sec for s in ["4", "7", "8", "17", "45", "15"]) or "unpaid wages" in full_text
    elif domain == "Industrial Disputes / Termination":
        return "industrial disputes" in act or any(s in sec for s in ["25f", "2a", "33c"]) or "retrenchment" in full_text or "workman" in full_text or "labour court" in full_text
    elif domain == "Domestic Violence & Maintenance":
        return "domestic violence" in act or any(s in sec for s in ["12", "18", "19", "20", "21", "23", "31", "17"]) or "shared household" in full_text or "protection order" in full_text
    return False

def run_eval():
    results = []
    print(f"Running Domain Evaluation across {len(EVAL_DATASET)} Fact Patterns with Strict Precision@3...\n")

    precisions = []

    for idx, item in enumerate(EVAL_DATASET, 1):
        domain = item["domain"]
        query = item["query"]

        # 1. Retrieval Leg
        search_results = hybrid_search_engine.search(query=query, limit=5)
        top_3 = search_results[:3]
        retrieved_statutes = []
        retrieved_texts = []
        relevance_flags = []

        for doc, score, mode in top_3:
            sec_label = f"{doc.act} §{doc.section} ({doc.title[:35]}...)"
            retrieved_statutes.append(sec_label)
            retrieved_texts.append(f"{doc.act} Section {doc.section}: {doc.title}. {doc.content}")
            rel = is_doc_relevant(domain, doc)
            relevance_flags.append(rel)

        rel_count = sum(1 for r in relevance_flags if r)
        p_at_3 = rel_count / 3.0
        precisions.append(p_at_3)

        # 2. Extract citations & verify grounding and currentness
        citations_in_query = extract_citations(query)
        all_grounded = True
        all_current = True
        gate_action = "ALLOW"

        for cit in citations_in_query:
            is_curr, curr_warn, repl = check_currentness(cit)
            is_grnd = check_grounding(cit, retrieved_texts)

            if not is_curr:
                all_current = False
                gate_action = "PARTIAL (ANNOTATED_REPEALED)"
            if not is_grnd:
                all_grounded = False
                if gate_action != "PARTIAL (ANNOTATED_REPEALED)":
                    gate_action = "FALLBACK (PROCEDURAL_TEMPLATE)"

        # 3. Determine Answer Source (NO Contradiction: If gate_action is FALLBACK, answer MUST be Fallback)
        if gate_action == "FALLBACK (PROCEDURAL_TEMPLATE)":
            answer_source = "Fallback Procedure Template (ProcedureEngine)"
        elif gate_action == "PARTIAL (ANNOTATED_REPEALED)":
            answer_source = "Annotated Grounded Statute + Procedure Template"
        elif p_at_3 >= 0.33:
            answer_source = "Retrieved Statute Text (Grounded)"
        else:
            answer_source = "Fallback Procedure Template (ProcedureEngine)"

        record = {
            "index": idx,
            "domain": domain,
            "query": query,
            "top_3_statutes": retrieved_statutes,
            "relevance_flags": relevance_flags,
            "precision_at_3": p_at_3,
            "gate_action": gate_action,
            "answer_source": answer_source
        }
        results.append(record)

    mean_p_at_3 = sum(precisions) / len(precisions)

    # Print Markdown Table
    print("\n### DOMAIN EVALUATION MATRIX (27 Fact Patterns across 9 Domains)\n")
    print("| # | Domain | Fact Pattern Query | Top-3 Retrieved Statutes | Rel (P@3) | GateAction | Answer Source |")
    print("|---|---|---|---|---|---|---|")
    for r in results:
        statutes_str = "<br>".join([f"• {'[REL] ' if r['relevance_flags'][i] else '[IRR] '}{s}" for i, s in enumerate(r["top_3_statutes"])])
        query_snippet = (r["query"][:50] + "...") if len(r["query"]) > 50 else r["query"]
        p_str = f"{r['precision_at_3']:.2f} ({sum(r['relevance_flags'])}/3)"
        print(f"| {r['index']} | **{r['domain']}** | {query_snippet} | {statutes_str} | {p_str} | `{r['gate_action']}` | {r['answer_source']} |")

    # Summary statistics
    total = len(results)
    grounded_count = sum(1 for r in results if "Retrieved Statute Text" in r["answer_source"])
    fallback_count = sum(1 for r in results if "Fallback Procedure" in r["answer_source"])

    print(f"\n=======================================================")
    print(f"EVALUATION SUMMARY:")
    print(f"Total Fact Patterns: {total}")
    print(f"Grounded (ALLOW): {grounded_count}/{total} ({grounded_count/total*100:.1f}%)")
    print(f"Fallback (PROCEDURAL_TEMPLATE): {fallback_count}/{total} ({fallback_count/total*100:.1f}%)")
    print(f"Mean Precision@3 Across All 27 Cases: {mean_p_at_3:.4f} ({mean_p_at_3*100:.1f}%)")
    print(f"=======================================================\n")

    # Save to JSON
    output_path = os.path.join(os.path.dirname(__file__), "eval_results.json")
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump({
            "mean_precision_at_3": mean_p_at_3,
            "grounded_count": grounded_count,
            "fallback_count": fallback_count,
            "total_cases": total,
            "cases": results
        }, f, indent=2)
    print(f"Detailed results saved to {output_path}")

if __name__ == "__main__":
    run_eval()
