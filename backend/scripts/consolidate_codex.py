"""
Consolidates all core statutory provisions, existing statutory codex, and domain-specific
statutes into backend/data/statutory_codex.json for comprehensive statutory grounding.
"""
import json
import os
import sys

# Ensure backend directory is in sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
from app.retrieval.hybrid_search import DEFAULT_STATUTES

ADDITIONAL_CORE_PROVISIONS = [
    # ── Tenancy: Essential Services & Rent Control ─────────────────────────────
    {
        "id": "mta_sec_20",
        "act": "Model Tenancy Act, 2021",
        "section": "Section 20",
        "title": "Restriction on withholding essential supply or service (Electricity & Water)",
        "content": "No landlord or property manager shall, either by himself or through any person, withhold or cut off any essential supply or service including electricity, water supply, passages, stairways, light, or conservancy to the premises occupied by the tenant. If the landlord violates this provision, the Rent Authority may pass an interim order directing immediate restoration of the essential service and may impose compensation or penalty on the landlord.",
        "jurisdiction": "model",
        "status": "model_law_not_uniformly_adopted",
        "effective_date": "2021-06-02",
        "source_url": "https://mohua.gov.in/upload/uploadfiles/files/Model_Tenancy_Act_English.pdf"
    },
    {
        "id": "mta_sec_11",
        "act": "Model Tenancy Act, 2021",
        "section": "Section 11",
        "title": "Security deposit — Statutory cap and refund upon taking vacant possession",
        "content": "The security deposit to be paid by the tenant in advance shall not exceed two months' rent for residential premises, and shall not exceed six months' rent for non-residential premises. The security deposit shall be refunded to the tenant on the date of taking over vacant possession of the premises from the tenant, after making due deductions of any liability or unpaid rent.",
        "jurisdiction": "model",
        "status": "model_law_not_uniformly_adopted",
        "effective_date": "2021-06-02",
        "source_url": "https://mohua.gov.in/upload/uploadfiles/files/Model_Tenancy_Act_English.pdf"
    },
    {
        "id": "mta_sec_9",
        "act": "Model Tenancy Act, 2021",
        "section": "Section 9",
        "title": "Revision of rent — Mutual agreement and three months prior written notice",
        "content": "Revision of rent between the landlord and the tenant shall be in accordance with the terms of the tenancy agreement. The landlord shall give a notice in writing to the tenant three months before the revised rent becomes due. Arbitrary mid-tenancy rent hikes without an escalation clause in the registered tenancy agreement are unlawful.",
        "jurisdiction": "model",
        "status": "model_law_not_uniformly_adopted",
        "effective_date": "2021-06-02",
        "source_url": "https://mohua.gov.in/upload/uploadfiles/files/Model_Tenancy_Act_English.pdf"
    },
    {
        "id": "mta_sec_10",
        "act": "Model Tenancy Act, 2021",
        "section": "Section 10",
        "title": "Rent Authority to determine revised rent in case of dispute",
        "content": "In case of any dispute between a landlord and a tenant regarding the revision of rent, the Rent Authority may, on an application made by either the landlord or the tenant, determine the revised rent and other related issues.",
        "jurisdiction": "model",
        "status": "model_law_not_uniformly_adopted",
        "effective_date": "2021-06-02",
        "source_url": "https://mohua.gov.in/upload/uploadfiles/files/Model_Tenancy_Act_English.pdf"
    },
    {
        "id": "mta_sec_8_9",
        "act": "Model Tenancy Act, 2021",
        "section": "Section 8 & 9",
        "title": "Revision of rent and notice requirement for rent escalation",
        "content": "Revision of rent between the landlord and the tenant shall be in accordance with the terms of the tenancy agreement. The landlord shall give a notice in writing three months before the revised rent becomes due. Arbitrary mid-tenancy rent hikes without an escalation clause in the registered tenancy agreement are unlawful, and the tenant cannot be evicted for refusing an unauthorized hike.",
        "jurisdiction": "model",
        "status": "model_law_not_uniformly_adopted",
        "effective_date": "2021-06-02",
        "source_url": "https://mohua.gov.in/upload/uploadfiles/files/Model_Tenancy_Act_English.pdf"
    },
    {
        "id": "mta_sec_22",
        "act": "Model Tenancy Act, 2021",
        "section": "Section 22",
        "title": "Eviction and recovery of possession of premises in case of death of the landlord",
        "content": "Where the landlord dies, his legal heirs may apply to the Rent Court for recovery of possession of the premises on ground of bona fide requirement for occupation.",
        "jurisdiction": "model",
        "status": "model_law_not_uniformly_adopted",
        "effective_date": "2021-06-02",
        "source_url": "https://mohua.gov.in/upload/uploadfiles/files/Model_Tenancy_Act_English.pdf"
    },
    {
        "id": "tpa_sec_108_b_c",
        "act": "Transfer of Property Act, 1882",
        "section": "Section 108(B)(c)",
        "title": "Rights and liabilities of lessor — Covenant for quiet and uninterrupted enjoyment",
        "content": "The lessor shall be deemed to contract with the lessee that, if the latter pays the rent reserved by the lease and performs the contracts binding on the lessee, he may hold the property during the time limited by the lease without interruption. The lessor cannot arbitrarily disconnect electricity, water, or access amenities.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "1882-07-01",
    },

    # ── Cheque Bounce: Section 142 Cognizance ──────────────────────────────────
    {
        "id": "ni_act_sec_142",
        "act": "Negotiable Instruments Act, 1881",
        "section": "Section 142",
        "title": "Cognizance of offences — Complaint within one month after expiry of 15 days notice period",
        "content": "Notwithstanding anything contained in the Code of Criminal Procedure, no court shall take cognizance of any offence punishable under section 138 except upon a complaint in writing made by the payee or the holder in due course of the cheque. Such complaint must be made within one month of the date on which the cause of action arises under clause (c) of the proviso to section 138 (i.e. when the drawer fails to make the payment within fifteen days of receipt of the statutory demand notice).",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "1881-12-09",
    },

    # ── Hit & Run / Motor Accident Claims ─────────────────────────────────────
    {
        "id": "mva_sec_161",
        "act": "Motor Vehicles Act, 1988",
        "section": "Section 161",
        "title": "Special provisions as to compensation in cases of hit and run motor accident",
        "content": "Provides statutory compensation in respect of death of any person resulting from a hit and run motor accident (fixed sum of two lakh rupees or higher prescribed amount) and in respect of grievous hurt (fifty thousand rupees or higher). Paid through the Motor Vehicles Accident Compensation Fund via the District Magistrate/Sub-Divisional Officer as Claims Enquiry Officer.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2019-09-01",
    },
    {
        "id": "mva_sec_166",
        "act": "Motor Vehicles Act, 1988",
        "section": "Section 166",
        "title": "Application for compensation before Motor Accidents Claims Tribunal (MACT)",
        "content": "An application for compensation arising out of an accident involving death or bodily injury may be made by the person sustaining injury, the owner of damaged property, or legal representatives of deceased before the Motor Accidents Claims Tribunal (MACT) within the local limits of whose jurisdiction the accident occurred or where claimant resides.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2019-09-01",
    },

    # ── Cyber Financial Fraud & Customer Protection ───────────────────────────
    {
        "id": "rbi_unauthorized_electronic_banking_2017",
        "act": "Reserve Bank of India Directives / Banking Regulation Act, 1949",
        "section": "RBI Circular DBR.No.Leg.BC.78/09.07.005/2017-18",
        "title": "Limiting Liability of Customers in Unauthorised Electronic Banking Transactions",
        "content": "Mandates Zero Liability of a customer in unauthorized electronic transactions: (1) Contributory fraud/negligence/deficiency on the part of the bank (whether reported or not). (2) Third party breach where deficiency lies neither with the bank nor customer but lies elsewhere in the system, and customer notifies bank within three working days of receiving communication. Limited liability of customer if reported within four to seven working days (maximum liability Rs. 10,000 to 25,000 depending on account type).",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2017-07-06",
        "source_url": "https://www.rbi.org.in/Scripts/NotificationUser.aspx?Id=11040"
    },
    {
        "id": "rbi_master_direction_credit_cards_2022",
        "act": "Reserve Bank of India Directives / Banking Regulation Act, 1949",
        "section": "RBI Master Direction DOR.AUT.REC.No.27/24.01.041/2022-23 Section 21 & 22",
        "title": "Credit and Debit Card Issuance and Conduct — Mandatory OTP/AFA for Card Not Present (CNP) Transactions and International Usage Controls",
        "content": "Card issuers shall put in place mechanisms for mandatory Additional Factor of Authentication (AFA/OTP) for all Card Not Present (CNP) transactions, including domestic and international transactions where applicable. Facilities for international transactions, online transactions (CNP), and contactless transactions shall be disabled by default on all cards and activated only upon explicit customer consent. In case of any unauthorized transaction occurring due to lack of required two-factor authentication or without explicit cardholder enablement of international transactions, the cardholder shall have zero liability, and the card issuer shall reverse the unauthorized charges within thirty days of reporting without penalty or adverse credit bureau reporting.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2022-07-01",
        "source_url": "https://www.rbi.org.in/Scripts/BS_ViewMasDirections.aspx?id=12300"
    },
    {
        "id": "it_act_sec_66c",
        "act": "Information Technology Act, 2000",
        "section": "Section 66C",
        "title": "Punishment for identity theft — Fraudulent use of password, OTP, or digital signature",
        "content": "Whoever, fraudulently or dishonestly make use of the electronic signature, password or any other unique identification feature of any other person, shall be punished with imprisonment of either description for a term which may extend to three years and shall also be liable to fine which may extend to one lakh rupees.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2008-10-27",
    },
    {
        "id": "it_act_sec_66e",
        "act": "Information Technology Act, 2000",
        "section": "Section 66E",
        "title": "Punishment for violation of privacy — Capturing, publishing or transmitting images",
        "content": "Whoever, intentionally or knowingly captures, publishes or transmits the image of a private area of any person without consent, under circumstances violating privacy, shall be punished with imprisonment up to three years or with fine up to two lakh rupees, or with both.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2008-10-27",
    },
    {
        "id": "it_act_sec_67",
        "act": "Information Technology Act, 2000",
        "section": "Section 67",
        "title": "Punishment for publishing or transmitting obscene or defamatory material in electronic form",
        "content": "Whoever publishes or transmits or causes to be published or transmitted in the electronic form, any material which is lascivious or appeals to the prurient interest, or which tends to deprave and corrupt persons, shall be punished on first conviction with imprisonment up to three years and fine up to five lakh rupees. Covers cyber extortion, morphing images, and blackmail.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2000-10-17",
    },

    # ── Consumer Protection: Product Liability ────────────────────────────────
    {
        "id": "cpa_2019_sec_84",
        "act": "Consumer Protection Act, 2019",
        "section": "Section 84",
        "title": "Liability of product manufacturer in product liability action",
        "content": "A product manufacturer shall be liable in a product liability action if: (a) the product contains a manufacturing defect; (b) the product is defective in design; (c) there is a deviation from manufacturing specifications; (d) the product does not conform to the express warranty; (e) the product fails to contain adequate instructions for correct usage to prevent harm or any warning regarding improper or hazardous use. Strict liability applies even if manufacturer was not negligent.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-07-20",
    },

    # ── Industrial Disputes: Retrenchment & Individual Grievance ─────────────
    {
        "id": "ida_sec_25f",
        "act": "Industrial Disputes Act, 1947",
        "section": "Section 25F",
        "title": "Conditions precedent to retrenchment of workmen — Notice and compensation",
        "content": "No workman employed in any industry who has been in continuous service for not less than one year under an employer shall be retrenched by that employer until: (a) the workman has been given one month notice in writing indicating the reasons for retrenchment or wages in lieu thereof; (b) the workman has been paid retrenchment compensation equivalent to fifteen days average pay for every completed year of continuous service; and (c) notice in prescribed manner is served on appropriate Government.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "1947-04-01",
    },
    {
        "id": "ida_sec_2a",
        "act": "Industrial Disputes Act, 1947",
        "section": "Section 2A",
        "title": "Dismissal, etc., of an individual workman to be deemed to be an industrial dispute",
        "content": "Where any employer discharges, dismisses, retrenches, or terminates the services of an individual workman, any dispute or difference between that workman and his employer connected with such discharge or dismissal shall be deemed to be an industrial dispute notwithstanding that no other workman or union is a party to the dispute. Workman may apply directly to Labour Court after three months.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "1965-12-01",
    },
]

def consolidate():
    codex_path = os.path.join(os.path.dirname(__file__), "..", "data", "statutory_codex.json")
    with open(codex_path, "r", encoding="utf-8") as f:
        existing_codex = json.load(f)

    existing_ids = {d["id"]: idx for idx, d in enumerate(existing_codex)}

    # 1. Merge DEFAULT_STATUTES
    merged_count = 0
    for chunk in DEFAULT_STATUTES:
        chunk_dict = {
            "id": chunk.id,
            "act": chunk.act,
            "section": chunk.section,
            "title": chunk.title,
            "content": chunk.content,
            "jurisdiction": chunk.jurisdiction,
            "status": chunk.status,
            "effective_date": chunk.effective_date,
        }
        if chunk.id in existing_ids:
            existing_codex[existing_ids[chunk.id]] = chunk_dict
        else:
            existing_codex.append(chunk_dict)
            existing_ids[chunk.id] = len(existing_codex) - 1
            merged_count += 1

    # 2. Merge ADDITIONAL_CORE_PROVISIONS
    added_provisions_count = 0
    for item in ADDITIONAL_CORE_PROVISIONS:
        if item["id"] in existing_ids:
            existing_codex[existing_ids[item["id"]]] = item
        else:
            existing_codex.append(item)
            existing_ids[item["id"]] = len(existing_codex) - 1
            added_provisions_count += 1

    print(f"Total provisions in consolidated codex: {len(existing_codex)}")
    print(f"Merged from DEFAULT_STATUTES: {merged_count}")
    print(f"Added new core provisions: {added_provisions_count}")

    with open(codex_path, "w", encoding="utf-8") as f:
        json.dump(existing_codex, f, indent=2, ensure_ascii=False)
    print(f"Successfully wrote {len(existing_codex)} provisions to {codex_path}")

if __name__ == "__main__":
    consolidate()
