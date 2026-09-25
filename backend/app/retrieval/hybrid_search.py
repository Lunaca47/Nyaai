import math
import os
import json
import logging
import re
from collections import Counter
from dataclasses import dataclass, field
from typing import List, Dict, Optional, Tuple, Set
import numpy as np

logger = logging.getLogger(__name__)


@dataclass
class DocumentChunk:
    id: str
    act: str
    section: str
    title: str
    content: str
    jurisdiction: str = "central"
    status: str = "in_force"  # "in_force", "amended", "repealed", "model_law_not_uniformly_adopted"
    effective_date: str = "2024-07-01"
    source_url: str = ""


@dataclass
class CaseLawChunk:
    case_id: str
    case_name: str
    citation: str
    court: str = "Supreme Court of India"
    judgment_date: str = ""
    bench_strength: int = 2
    bench_judges: List[str] = field(default_factory=list)
    statutory_provisions: List[str] = field(default_factory=list)
    legal_domain: str = ""
    ratio_decidendi: str = ""
    key_principles: List[str] = field(default_factory=list)
    precedent_status: str = "GOOD_LAW"
    currentness_check: str = ""
    source_url: str = ""
    verification_status: str = "VERIFIED"

    def get_full_text(self) -> str:
        principles = " ".join(self.key_principles)
        provisions = " ".join(self.statutory_provisions)
        judges = ", ".join(self.bench_judges)
        return (
            f"{self.case_name}. Citation: {self.citation}. Court: {self.court} ({self.judgment_date}). "
            f"Bench: {self.bench_strength} Judges ({judges}). Domain: {self.legal_domain}. "
            f"Statutory Provisions: {provisions}. "
            f"Ratio Decidendi: {self.ratio_decidendi} "
            f"Key Principles: {principles} "
            f"Precedent Status: {self.precedent_status}. {self.currentness_check}"
        )


# Core statutory codex seeded in memory
DEFAULT_STATUTES: List[DocumentChunk] = [
    DocumentChunk(
        id="coi_art_14",
        act="Constitution of India",
        section="Article 14",
        title="Equality before law",
        content="The State shall not deny to any person equality before the law or the equal protection of the laws within the territory of India.",
        jurisdiction="central",
        status="in_force",
        effective_date="1950-01-26",
    ),
    DocumentChunk(
        id="coi_art_19",
        act="Constitution of India",
        section="Article 19",
        title="Protection of certain rights regarding freedom of speech, etc.",
        content="All citizens shall have the right to freedom of speech and expression, to assemble peaceably and without arms, to form associations or unions, and to move freely throughout the territory of India.",
        jurisdiction="central",
        status="in_force",
        effective_date="1950-01-26",
    ),
    DocumentChunk(
        id="coi_art_21",
        act="Constitution of India",
        section="Article 21",
        title="Protection of life and personal liberty",
        content="No person shall be deprived of his life or personal liberty except according to procedure established by law. This guarantees right to live with human dignity, speedy trial, and legal aid.",
        jurisdiction="central",
        status="in_force",
        effective_date="1950-01-26",
    ),
    DocumentChunk(
        id="coi_art_226",
        act="Constitution of India",
        section="Article 226",
        title="Power of High Courts to issue certain writs",
        content="Every High Court shall have powers to issue to any person or authority directions, orders or writs, including writs in the nature of habeas corpus, mandamus, prohibition, quo warranto and certiorari for the enforcement of fundamental rights and for any other purpose.",
        jurisdiction="central",
        status="in_force",
        effective_date="1950-01-26",
    ),
    DocumentChunk(
        id="bns_sec_281",
        act="Bharatiya Nyaya Sanhita, 2023",
        section="Section 281",
        title="Rash driving or riding on a public way",
        content="Whoever drives any vehicle, or rides, on any public way in a manner so rash or negligent as to endanger human life, or to be likely to cause hurt or injury to any other person, shall be punished with imprisonment of either description for a term which may extend to six months, or with fine which may extend to one thousand rupees, or with both. (Replaces IPC Section 279)",
        jurisdiction="central",
        status="in_force",
        effective_date="2024-07-01",
    ),
    DocumentChunk(
        id="bns_sec_106",
        act="Bharatiya Nyaya Sanhita, 2023",
        section="Section 106",
        title="Causing death by negligence (Hit-and-run)",
        content="Whoever causes the death of any person by doing any rash or negligent act not amounting to culpable homicide, shall be punished with imprisonment of either description for a term which may extend to five years, and shall also be liable to fine. Sub-section (2): Whoever causes death of any person by rash and negligent driving and escapes without reporting to a police officer or Magistrate soon after the incident shall be punished with imprisonment up to ten years. (Replaces IPC Section 304A)",
        jurisdiction="central",
        status="in_force",
        effective_date="2024-07-01",
    ),
    DocumentChunk(
        id="bns_sec_316",
        act="Bharatiya Nyaya Sanhita, 2023",
        section="Section 316",
        title="Criminal breach of trust",
        content="Whoever, being in any manner entrusted with property, or with any dominion over property, dishonestly misappropriates or converts to his own use that property, commits criminal breach of trust. Punishable with imprisonment up to five years, or with fine, or with both. (Applicable to unlawful retention of tenant security deposits and seizure of tenant belongings).",
        jurisdiction="central",
        status="in_force",
        effective_date="2024-07-01",
    ),
    DocumentChunk(
        id="bns_sec_329",
        act="Bharatiya Nyaya Sanhita, 2023",
        section="Section 329",
        title="Criminal trespass and house-trespass",
        content="Whoever enters into or upon property in the possession of another with intent to commit an offence or to intimidate, insult or annoy any person in possession, or having lawfully entered, unlawfully remains there with intent to intimidate, commits criminal trespass. Sub-section (3): House-trespass punishable with imprisonment. (Covers illegal lockout and unlawful dispossession).",
        jurisdiction="central",
        status="in_force",
        effective_date="2024-07-01",
    ),
    DocumentChunk(
        id="bns_sec_318",
        act="Bharatiya Nyaya Sanhita, 2023",
        section="Section 318",
        title="Cheating",
        content="Whoever, by deceiving any person, fraudulently or dishonestly induces the person so deceived to deliver any property, commits cheating. Sub-section (4): Cheating and dishonestly inducing delivery of property shall be punished with imprisonment for a term which may extend to seven years, and fine. (Replaces IPC Section 420).",
        jurisdiction="central",
        status="in_force",
        effective_date="2024-07-01",
    ),
    DocumentChunk(
        id="bnss_sec_173",
        act="Bharatiya Nagarik Suraksha Sanhita, 2023",
        section="Section 173",
        title="Information in cognizable cases (FIR & Zero FIR)",
        content="Every information relating to the commission of a cognizable offence, if given orally to an officer in charge of a police station, shall be reduced to writing. Information may be given electronically and recorded. Irrespective of the territorial jurisdiction of the police station, the information shall be recorded (Zero FIR) and transferred to the police station having jurisdiction. (Replaces CrPC Section 154; reinforces Lalita Kumari mandate).",
        jurisdiction="central",
        status="in_force",
        effective_date="2024-07-01",
    ),
    DocumentChunk(
        id="bnss_sec_175_3",
        act="Bharatiya Nagarik Suraksha Sanhita, 2023",
        section="Section 175(3)",
        title="Remedy on refusal to register FIR — Representation to Superintendent of Police",
        content="Any person aggrieved by a refusal on the part of an officer in charge of a police station to record the information referred to in section 173 may send the substance of such information, in writing and by post, to the Superintendent of Police concerned who, if satisfied that such information discloses the commission of a cognizable offence, shall either investigate the case or direct an investigation. (Replaces CrPC Section 154(3)).",
        jurisdiction="central",
        status="in_force",
        effective_date="2024-07-01",
    ),
    DocumentChunk(
        id="bnss_sec_175_4",
        act="Bharatiya Nagarik Suraksha Sanhita, 2023",
        section="Section 175(4)",
        title="Remedy on refusal to register FIR — Application to Magistrate",
        content="Any person aggrieved by the refusal to register an FIR and failure of action by Superintendent of Police under sub-section (3) may make an application to the Magistrate having jurisdiction under section 175(4) / 176 for directing the police to register an FIR and investigate the matter. (Successor to CrPC Section 156(3)).",
        jurisdiction="central",
        status="in_force",
        effective_date="2024-07-01",
    ),
    DocumentChunk(
        id="bnss_sec_503",
        act="Bharatiya Nagarik Suraksha Sanhita, 2023",
        section="Section 503",
        title="Order for custody and disposal of property / De-freezing bank accounts",
        content="When any property has been seized by the police or an account frozen under investigation, the Magistrate may make an order for proper custody or release of such property/funds during the pendency of the inquiry or trial upon furnishing security or indemnity bond.",
        jurisdiction="central",
        status="in_force",
        effective_date="2024-07-01",
    ),
    DocumentChunk(
        id="bsa_sec_63",
        act="Bharatiya Sakshya Adhiniyam, 2023",
        section="Section 63",
        title="Admissibility of electronic records and digital certificates",
        content="Any information contained in an electronic record which is printed on a paper, stored, recorded or copied in optical or magnetic media shall be deemed to be also a document. Accompanied by a certificate signed by a person occupying a responsible official position in relation to the operation of the device. (Replaces Indian Evidence Act Section 65B).",
        jurisdiction="central",
        status="in_force",
        effective_date="2024-07-01",
    ),
    DocumentChunk(
        id="ni_act_sec_138",
        act="Negotiable Instruments Act, 1881",
        section="Section 138",
        title="Dishonour of cheque for insufficiency, etc., of funds in the account",
        content="Where any cheque drawn by a person on an account maintained by him with a banker for payment of any amount of money is returned by the bank unpaid, either because of insufficiency of funds or exceeds arrangement, such person shall be deemed to have committed an offence and shall be punished with imprisonment up to two years, or with fine up to twice the amount of the cheque, or with both. Requires legal demand notice within 30 days of memo, 15 days to pay, complaint within 30 days thereafter.",
        jurisdiction="central",
        status="in_force",
        effective_date="1881-12-09",
    ),
    DocumentChunk(
        id="ni_act_sec_141",
        act="Negotiable Instruments Act, 1881",
        section="Section 141",
        title="Offences by companies and directors' vicarious liability",
        content="If the person committing an offence under section 138 is a company, every person who; at the time the offence was committed, was in charge of, and was responsible to the company for the conduct of the business of the company, as well as the company, shall be deemed to be guilty of the offence.",
        jurisdiction="central",
        status="in_force",
        effective_date="1881-12-09",
    ),
    DocumentChunk(
        id="ni_act_sec_143a",
        act="Negotiable Instruments Act, 1881",
        section="Section 143A",
        title="Power to direct interim compensation up to 20%",
        content="Notwithstanding anything contained in the Code of Criminal Procedure, the Court trying an offence under section 138 may order the drawer of the cheque to pay interim compensation to the complainant not exceeding twenty per cent of the amount of the cheque.",
        jurisdiction="central",
        status="in_force",
        effective_date="2018-09-01",
    ),
    DocumentChunk(
        id="it_act_sec_66d",
        act="Information Technology Act, 2000",
        section="Section 66D",
        title="Punishment for cheating by personation by using computer resource",
        content="Whoever, by means of any communication device or computer resource cheats by personation (including OTP scams, impersonation fraud, phishing, fake UPI links), shall be punished with imprisonment of either description for a term which may extend to three years and shall also be liable to fine which may extend to one lakh rupees.",
        jurisdiction="central",
        status="in_force",
        effective_date="2000-10-17",
    ),
    DocumentChunk(
        id="it_act_sec_43",
        act="Information Technology Act, 2000",
        section="Section 43",
        title="Penalty and compensation for damage to computer system",
        content="If any person without permission of the owner damages, downloads, copies, introduces virus, or disrupts access to computer system, he shall be liable to pay damages by way of compensation to the person so affected.",
        jurisdiction="central",
        status="in_force",
        effective_date="2000-10-17",
    ),
    DocumentChunk(
        id="tp_act_sec_106",
        act="Transfer of Property Act, 1882",
        section="Section 106",
        title="Duration of certain leases in absence of written contract and notice to quit",
        content="In the absence of a contract or local law to the contrary, a lease of immovable property for agricultural or manufacturing purposes is deemed to be from year to year (six months notice), and for other purposes from month to month (fifteen days notice). Notice must be in writing, signed and tendered or sent by post.",
        jurisdiction="central",
        status="in_force",
        effective_date="1882-07-01",
    ),
    DocumentChunk(
        id="cpa_sec_35",
        act="Consumer Protection Act, 2019",
        section="Section 35",
        title="Manner in which complaint shall be made to District Commission",
        content="A complaint, in relation to any goods sold or delivered or agreed to be sold or delivered or any service provided or agreed to be provided, may be filed with a District Commission by the consumer to whom such goods are sold or delivered, or by any recognized consumer association. Allows filing through e-Daakhil portal.",
        jurisdiction="central",
        status="in_force",
        effective_date="2020-07-20",
    ),
    DocumentChunk(
        id="cpa_sec_39",
        act="Consumer Protection Act, 2019",
        section="Section 39",
        title="Findings of District Commission — Orders for Removal of Defects, Refund, and Compensation",
        content="Where the District Commission is satisfied that the goods or services suffer from any defect or deficiency, it shall issue an order directing the opposite party to: remove the defect; replace goods; return price or refund amount paid; pay compensation for loss or injury suffered; pay punitive damages.",
        jurisdiction="central",
        status="in_force",
        effective_date="2020-07-20",
    ),
    DocumentChunk(
        id="cpa_sec_83",
        act="Consumer Protection Act, 2019",
        section="Section 83",
        title="Product liability action against manufacturer, service provider, or seller",
        content="A product liability action may be brought by a complainant against a product manufacturer or a product service provider or a product seller for any harm caused to him on account of a defective product.",
        jurisdiction="central",
        status="in_force",
        effective_date="2020-07-20",
    ),
    DocumentChunk(
        id="pga_sec_4",
        act="Payment of Gratuity Act, 1972",
        section="Section 4",
        title="Payment of gratuity — Five years continuous service and calculation formula",
        content="Gratuity shall be payable to an employee on termination of employment after rendering continuous service for not less than five years: on superannuation, retirement, resignation, or death/disablement. Calculated at fifteen days wages per completed year of service based on last drawn salary.",
        jurisdiction="central",
        status="in_force",
        effective_date="1972-09-16",
    ),
    DocumentChunk(
        id="pga_sec_7",
        act="Payment of Gratuity Act, 1972",
        section="Section 7",
        title="Determination of gratuity — 30-day employer payment deadline and 10% interest",
        content="Employer must determine and pay gratuity within thirty days of it becoming payable. Sub-section (3A): Failure to pay within 30 days incurs statutory simple interest at ten percent per annum from due date until actual payment.",
        jurisdiction="central",
        status="in_force",
        effective_date="1972-09-16",
    ),
    DocumentChunk(
        id="pga_sec_8",
        act="Payment of Gratuity Act, 1972",
        section="Section 8",
        title="Recovery of gratuity — Revenue Recovery Certificate (RRC) via Collector",
        content="If gratuity is unpaid, the Controlling Authority on application issues a certificate to the District Collector to recover the amount along with compound interest as arrears of land revenue.",
        jurisdiction="central",
        status="in_force",
        effective_date="1972-09-16",
    ),
    DocumentChunk(
        id="cow_sec_17",
        act="Code on Wages, 2019",
        section="Section 17",
        title="Time limit for payment of wages and settlement within two working days upon resignation",
        content="Wages must be paid monthly before the 7th day after the wage period. Upon dismissal, retrenchment or resignation, wages earned shall be paid within two working days of departure.",
        jurisdiction="central",
        status="in_force",
        effective_date="2020-12-18",
    ),
    DocumentChunk(
        id="cow_sec_45",
        act="Code on Wages, 2019",
        section="Section 45",
        title="Claims under Code and procedure for unpaid wages with compensation up to 10x",
        content="Application for unpaid wages, unauthorized deductions, or delayed payment may be filed within three years before the appointed Authority. Authority may award unpaid wages plus compensation up to ten times the claim amount.",
        jurisdiction="central",
        status="in_force",
        effective_date="2020-12-18",
    ),
    DocumentChunk(
        id="ida_sec_33c_2",
        act="Industrial Disputes Act, 1947",
        section="Section 33C(2)",
        title="Recovery of money or computation of monetary benefit by Labour Court",
        content="Where a workman is entitled to receive from the employer any money or monetary benefit, the Labour Court can determine the exact quantum due and recover it as an arrear of land revenue through the District Collector.",
        jurisdiction="central",
        status="in_force",
        effective_date="1947-04-01",
    ),
    DocumentChunk(
        id="dva_sec_12",
        act="Protection of Women from Domestic Violence Act, 2005",
        section="Section 12",
        title="Application to Magistrate for seeking reliefs with Domestic Incident Report (DIR)",
        content="An aggrieved woman or Protection Officer may present an application to the Magistrate seeking protection, residence, monetary relief, custody, or compensation. The Magistrate considers any Domestic Incident Report (DIR) and shall fix hearing within three days.",
        jurisdiction="central",
        status="in_force",
        effective_date="2006-10-26",
    ),
    DocumentChunk(
        id="dva_sec_18",
        act="Protection of Women from Domestic Violence Act, 2005",
        section="Section 18",
        title="Protection orders prohibiting domestic violence and communications",
        content="Magistrate may pass a protection order prohibiting respondent from committing acts of domestic violence, entering victim's workplace or school, communicating in any form, or alienating shared assets.",
        jurisdiction="central",
        status="in_force",
        effective_date="2006-10-26",
    ),
    DocumentChunk(
        id="dva_sec_19",
        act="Protection of Women from Domestic Violence Act, 2005",
        section="Section 19",
        title="Residence orders — Restraining dispossession and shared household protection",
        content="Magistrate may pass a residence order restraining respondent from dispossessing the aggrieved woman from the shared household (as affirmed in Satish Chander Ahuja v. Sneha Ahuja), directing removal of respondent, or ordering provision of alternate accommodation.",
        jurisdiction="central",
        status="in_force",
        effective_date="2006-10-26",
    ),
    DocumentChunk(
        id="dva_sec_20",
        act="Protection of Women from Domestic Violence Act, 2005",
        section="Section 20",
        title="Monetary reliefs and maintenance for aggrieved woman and children",
        content="Magistrate may direct payment of monetary relief to meet expenses, medical costs, and fair, reasonable maintenance for the aggrieved woman and her children consistent with standard of living.",
        jurisdiction="central",
        status="in_force",
        effective_date="2006-10-26",
    ),
    DocumentChunk(
        id="dva_sec_21",
        act="Protection of Women from Domestic Violence Act, 2005",
        section="Section 21",
        title="Custody orders for children",
        content="Magistrate may grant temporary custody of any child or children to the aggrieved woman at any stage of proceedings, and may restrict or refuse visits by the respondent if harmful to the child.",
        jurisdiction="central",
        status="in_force",
        effective_date="2006-10-26",
    ),
    DocumentChunk(
        id="dva_sec_23",
        act="Protection of Women from Domestic Violence Act, 2005",
        section="Section 23",
        title="Power to grant emergency ex parte interim orders",
        content="Magistrate has power to pass interim orders and, on basis of prima facie affidavit, grant emergency ex parte orders under sections 18, 19, 20, 21 or 22 to prevent immediate harm.",
        jurisdiction="central",
        status="in_force",
        effective_date="2006-10-26",
    ),
    DocumentChunk(
        id="dva_sec_31",
        act="Protection of Women from Domestic Violence Act, 2005",
        section="Section 31",
        title="Penalty for breach of protection order — One year jail, cognizable and non-bailable",
        content="A breach of protection order or interim protection order by respondent is a cognizable and non-bailable offence punishable with imprisonment up to one year, or fine up to twenty thousand rupees, or both.",
        jurisdiction="central",
        status="in_force",
        effective_date="2006-10-26",
    ),
    DocumentChunk(
        id="mva_sec_166",
        act="Motor Vehicles Act, 1988",
        section="Section 166",
        title="Application for compensation before Claims Tribunal",
        content="An application for compensation arising out of an accident of the nature specified in sub-section (1) of section 165 may be made by the person who has sustained the injury, or by the owner of the property, or where death has resulted from the accident, by all or any of the legal representatives of the deceased.",
        jurisdiction="central",
        status="in_force",
        effective_date="1989-07-01",
    ),
    DocumentChunk(
        id="delhi_rent_control_sec_14",
        act="Delhi Rent Control Act, 1958",
        section="Section 14",
        title="Protection of tenant against eviction",
        content="Notwithstanding anything to the contrary in any other law or contract, no order or decree for the recovery of possession of any premises shall be made by any court or Controller in favour of the landlord against a tenant, except on specific grounds specified in the Act (e.g. non-payment of rent, subletting, bona fide requirement).",
        jurisdiction="delhi",
        status="in_force",
        effective_date="1958-12-31",
    ),
    DocumentChunk(
        id="maharashtra_rent_control_sec_15",
        act="Maharashtra Rent Control Act, 1999",
        section="Section 15",
        title="No ejectment ordinarily if tenant pays or is ready and willing to pay standard rent and permitted increases",
        content="A landlord shall not be entitled to the recovery of possession of any premises so long as the tenant pays, or is ready and willing to pay, the amount of the standard rent and permitted increases, and observes other conditions of tenancy.",
        jurisdiction="maharashtra",
        status="in_force",
        effective_date="2000-03-31",
    ),
    # Repealed provisions preserved for historical and negative-verification testing
    DocumentChunk(
        id="ipc_sec_420",
        act="Indian Penal Code, 1860",
        section="Section 420",
        title="Cheating and dishonestly inducing delivery of property",
        content="Whoever cheats and thereby dishonestly induces the person deceived to deliver any property. (Repealed w.e.f. July 1, 2024. Replaced by Bharatiya Nyaya Sanhita, 2023, Section 318(4)).",
        jurisdiction="central",
        status="repealed",
        effective_date="1860-10-06",
    ),
    DocumentChunk(
        id="crpc_sec_154",
        act="Code of Criminal Procedure, 1973",
        section="Section 154",
        title="Information in cognizable cases",
        content="Every information relating to the commission of a cognizable offence, if given orally to an officer in charge of a police station. (Repealed w.e.f. July 1, 2024. Replaced by Bharatiya Nagarik Suraksha Sanhita, 2023, Section 173).",
        jurisdiction="central",
        status="repealed",
        effective_date="1974-04-01",
    ),
]

STOP_WORDS = {
    "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
    "any", "are", "as", "at", "be", "because", "been", "before", "being", "below",
    "between", "both", "but", "by", "can", "did", "do", "does", "doing", "don",
    "down", "during", "each", "few", "for", "from", "further", "had", "has", "have",
    "having", "he", "her", "here", "hers", "herself", "him", "himself", "his", "how",
    "i", "if", "in", "into", "is", "it", "its", "itself", "just", "me", "more", "most",
    "my", "myself", "no", "nor", "not", "now", "of", "off", "on", "once", "only", "or",
    "other", "our", "ours", "ourselves", "out", "over", "own", "s", "same", "she",
    "should", "so", "some", "such", "t", "than", "that", "the", "their", "theirs",
    "them", "themselves", "then", "there", "these", "they", "this", "those", "through",
    "to", "too", "under", "until", "up", "very", "was", "we", "were", "what", "when",
    "where", "which", "while", "who", "whom", "why", "will", "with", "you", "your",
}


def tokenize(text: str) -> List[str]:
    """Tokenize text into lowercase alphanumeric tokens without stop words."""
    raw_tokens = re.findall(r'[A-Za-z0-9]+', text.lower())
    return [t for t in raw_tokens if t not in STOP_WORDS]


class BM25Okapi:
    """
    Standard BM25 Okapi implementation for sparse statutory retrieval.
    Parameters: k1=1.5, b=0.75.
    """
    def __init__(self, corpus: List[DocumentChunk], k1: float = 1.5, b: float = 0.75):
        self.k1 = k1
        self.b = b
        self.corpus = corpus
        self.corpus_size = len(corpus)
        self.doc_lengths = []
        self.doc_term_freqs: List[Counter] = []
        self.doc_freqs: Counter = Counter()

        total_length = 0
        for doc in corpus:
            full_text = f"{doc.act} {doc.section} {doc.title} {doc.content}"
            tokens = tokenize(full_text)
            length = len(tokens)
            self.doc_lengths.append(length)
            total_length += length
            tf = Counter(tokens)
            self.doc_term_freqs.append(tf)
            for token in tf:
                self.doc_freqs[token] += 1

        self.avg_doc_len = total_length / self.corpus_size if self.corpus_size > 0 else 1.0

    def score(self, query_tokens: List[str]) -> List[float]:
        scores = [0.0] * self.corpus_size
        for token in query_tokens:
            df = self.doc_freqs.get(token, 0)
            if df == 0:
                continue
            # Standard Robertson-Spärck Jones IDF
            idf = math.log(((self.corpus_size - df + 0.5) / (df + 0.5)) + 1.0)
            for i, tf_dict in enumerate(self.doc_term_freqs):
                tf = tf_dict.get(token, 0)
                if tf > 0:
                    denom = tf + self.k1 * (1.0 - self.b + self.b * (self.doc_lengths[i] / self.avg_doc_len))
                    term_score = idf * (tf * (self.k1 + 1.0)) / denom
                    scores[i] += term_score

        # Exact section boosting: check if query contains exact section numbers matching document
        for i, doc in enumerate(self.corpus):
            sec_num = re.findall(r'\d+[A-Za-z]?', doc.section)
            for num in sec_num:
                if num.lower() in query_tokens:
                    scores[i] += 5.0  # Strong boost for exact section match
        return scores


_MODEL_INSTANCE = None


def get_sentence_transformer():
    global _MODEL_INSTANCE
    if _MODEL_INSTANCE is None:
        try:
            from sentence_transformers import SentenceTransformer
            _MODEL_INSTANCE = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
        except Exception as e:
            logger.warning(f"Could not load SentenceTransformer: {e}")
            _MODEL_INSTANCE = None
    return _MODEL_INSTANCE


def load_default_documents() -> List[DocumentChunk]:
    """Load documents from statutory_codex.json if available, otherwise DEFAULT_STATUTES."""
    candidate_paths = [
        os.path.join("backend", "data", "statutory_codex.json"),
        os.path.join(os.path.dirname(__file__), "..", "..", "data", "statutory_codex.json"),
        os.path.join("data", "statutory_codex.json"),
    ]
    for p in candidate_paths:
        if os.path.exists(p):
            try:
                with open(p, "r", encoding="utf-8") as f:
                    data = json.load(f)
                return [
                    DocumentChunk(
                        id=d["id"],
                        act=d["act"],
                        section=d["section"],
                        title=d["title"],
                        content=d["content"],
                        jurisdiction=d.get("jurisdiction", "central"),
                        status=d.get("status", "in_force"),
                        effective_date=d.get("effective_date", "2024-07-01"),
                        source_url=d.get("source_url", ""),
                    )
                    for d in data
                ]
            except Exception as e:
                logger.warning(f"Failed to load statutory_codex from {p}: {e}")
    return list(DEFAULT_STATUTES)


def load_default_case_law() -> List[CaseLawChunk]:
    """Load case law precedents from case_law_corpus.json."""
    candidate_paths = [
        os.path.join("backend", "data", "case_law_corpus.json"),
        os.path.join(os.path.dirname(__file__), "..", "..", "data", "case_law_corpus.json"),
        os.path.join("data", "case_law_corpus.json"),
    ]
    for p in candidate_paths:
        if os.path.exists(p):
            try:
                with open(p, "r", encoding="utf-8") as f:
                    data = json.load(f)
                return [
                    CaseLawChunk(
                        case_id=d["case_id"],
                        case_name=d["case_name"],
                        citation=d["citation"],
                        court=d.get("court", "Supreme Court of India"),
                        judgment_date=d.get("judgment_date", ""),
                        bench_strength=d.get("bench_strength", 2),
                        bench_judges=d.get("bench_judges", []),
                        statutory_provisions=d.get("statutory_provisions", []),
                        legal_domain=d.get("legal_domain", ""),
                        ratio_decidendi=d.get("ratio_decidendi", ""),
                        key_principles=d.get("key_principles", []),
                        precedent_status=d.get("precedent_status", "GOOD_LAW"),
                        currentness_check=d.get("currentness_check", ""),
                        source_url=d.get("source_url", ""),
                        verification_status=d.get("verification_status", "VERIFIED"),
                    )
                    for d in data
                ]
            except Exception as e:
                logger.warning(f"Failed to load case_law_corpus from {p}: {e}")
    return []


class DenseSemanticRetriever:
    """
    Genuine dense vector semantic retriever using sentence-transformers/all-MiniLM-L6-v2.
    Loads pre-encoded embeddings from statutory_embeddings.npy sidecar and computes
    cosine similarity in normalized vector space against runtime query embeddings.
    """
    def __init__(self, corpus: List[DocumentChunk]):
        self.corpus = corpus
        self.corpus_size = len(corpus)
        self.doc_embeddings: Optional[np.ndarray] = None
        self.doc_vectors = None
        self._init_embeddings()

    def _init_embeddings(self):
        paths = [
            (
                os.path.join("backend", "data", "statutory_embeddings.npy"),
                os.path.join("backend", "data", "statutory_embeddings_ids.json"),
            ),
            (
                os.path.join(os.path.dirname(__file__), "..", "..", "data", "statutory_embeddings.npy"),
                os.path.join(os.path.dirname(__file__), "..", "..", "data", "statutory_embeddings_ids.json"),
            ),
            (
                os.path.join("data", "statutory_embeddings.npy"),
                os.path.join("data", "statutory_embeddings_ids.json"),
            ),
        ]
        precomputed_mat = None
        id_to_idx = {}
        for npy_p, ids_p in paths:
            if os.path.exists(npy_p) and os.path.exists(ids_p):
                try:
                    precomputed_mat = np.load(npy_p)
                    with open(ids_p, "r", encoding="utf-8") as f:
                        ids_list = json.load(f)
                    id_to_idx = {doc_id: i for i, doc_id in enumerate(ids_list)}
                    break
                except Exception as e:
                    logger.warning(f"Failed to load precomputed embeddings: {e}")

        model = get_sentence_transformer()
        if precomputed_mat is not None and model is not None:
            aligned = []
            missing_texts = []
            missing_indices = []

            for i, doc in enumerate(self.corpus):
                if doc.id in id_to_idx:
                    aligned.append(precomputed_mat[id_to_idx[doc.id]])
                else:
                    aligned.append(None)
                    missing_texts.append(f"{doc.act} {doc.section} {doc.title}: {doc.content}")
                    missing_indices.append(i)

            if missing_texts and model is not None:
                new_embs = model.encode(missing_texts, normalize_embeddings=True, convert_to_numpy=True)
                for idx, emb in zip(missing_indices, new_embs):
                    aligned[idx] = emb

            if len(aligned) > 0 and all(v is not None for v in aligned):
                self.doc_embeddings = np.array(aligned, dtype=np.float32)
                return

        # Fallback if sentence-transformer model is unavailable
        self.doc_vectors = [self._embed_text_fallback(f"{d.act} {d.section} {d.title} {d.content}") for d in self.corpus]

    def _embed_text_fallback(self, text: str) -> Dict[str, float]:
        tokens = tokenize(text)
        vec: Dict[str, float] = Counter(tokens)
        for t in tokens:
            if len(t) >= 4:
                for k in range(len(t) - 2):
                    ngram = f"##{t[k:k+3]}"
                    vec[ngram] = vec.get(ngram, 0.0) + 0.5
        norm = math.sqrt(sum(v * v for v in vec.values()))
        if norm > 0:
            for k in vec:
                vec[k] /= norm
        return vec

    def score(self, query: str) -> List[float]:
        if self.doc_embeddings is not None:
            model = get_sentence_transformer()
            if model is not None:
                q_vec = model.encode(query, normalize_embeddings=True, convert_to_numpy=True)
                scores = np.dot(self.doc_embeddings, q_vec)
                return [float(s) for s in scores]

        q_vec = self._embed_text_fallback(query)
        scores = []
        for d_vec in (self.doc_vectors or []):
            dot = sum(q_vec[k] * d_vec.get(k, 0.0) for k in q_vec)
            scores.append(max(0.0, dot))
        return scores


class CaseLawBM25:
    """
    BM25 Okapi retriever specifically calibrated for Case Law Precedents.
    Maintains completely distinct vocabulary, document lengths, and IDF calculations
    from statutory provisions to eliminate length-normalization collision.
    """
    def __init__(self, corpus: List[CaseLawChunk], k1: float = 1.5, b: float = 0.75):
        self.k1 = k1
        self.b = b
        self.corpus = corpus
        self.corpus_size = len(corpus)
        self.doc_lengths = []
        self.doc_term_freqs: List[Counter] = []
        self.doc_freqs: Counter = Counter()

        total_length = 0
        for chunk in corpus:
            tokens = tokenize(chunk.get_full_text())
            doc_len = len(tokens)
            self.doc_lengths.append(doc_len)
            total_length += doc_len
            tf = Counter(tokens)
            self.doc_term_freqs.append(tf)
            for token in set(tokens):
                self.doc_freqs[token] += 1

        self.avg_doc_len = total_length / self.corpus_size if self.corpus_size > 0 else 1.0
        self.idf: Dict[str, float] = {}
        for token, df in self.doc_freqs.items():
            self.idf[token] = math.log(1.0 + (self.corpus_size - df + 0.5) / (df + 0.5))

    def score(self, query_tokens: List[str]) -> List[float]:
        scores = [0.0] * self.corpus_size
        for token in query_tokens:
            if token not in self.idf:
                continue
            idf = self.idf[token]
            for i, tf_dict in enumerate(self.doc_term_freqs):
                tf = tf_dict.get(token, 0)
                if tf > 0:
                    denom = tf + self.k1 * (1.0 - self.b + self.b * (self.doc_lengths[i] / self.avg_doc_len))
                    term_score = idf * (tf * (self.k1 + 1.0)) / denom
                    scores[i] += term_score

        # Case citation and party name exact boost
        for i, chunk in enumerate(self.corpus):
            name_tokens = [t for t in tokenize(chunk.case_name) if len(t) > 3 and t not in ("state", "union", "india")]
            if name_tokens and any(t in query_tokens for t in name_tokens):
                scores[i] += 4.0
        return scores


class DenseCaseLawRetriever:
    """
    Dense semantic retriever for Case Law chunks using all-MiniLM-L6-v2 embeddings.
    Loads pre-encoded embeddings from case_law_embeddings.npy sidecar.
    """
    def __init__(self, corpus: List[CaseLawChunk]):
        self.corpus = corpus
        self.corpus_size = len(corpus)
        self.doc_embeddings: Optional[np.ndarray] = None
        self.doc_vectors = None
        self._init_embeddings()

    def _init_embeddings(self):
        paths = [
            (
                os.path.join("backend", "data", "case_law_embeddings.npy"),
                os.path.join("backend", "data", "case_law_embeddings_ids.json"),
            ),
            (
                os.path.join(os.path.dirname(__file__), "..", "..", "data", "case_law_embeddings.npy"),
                os.path.join(os.path.dirname(__file__), "..", "..", "data", "case_law_embeddings_ids.json"),
            ),
            (
                os.path.join("data", "case_law_embeddings.npy"),
                os.path.join("data", "case_law_embeddings_ids.json"),
            ),
        ]
        precomputed_mat = None
        id_to_idx = {}
        for npy_p, ids_p in paths:
            if os.path.exists(npy_p) and os.path.exists(ids_p):
                try:
                    precomputed_mat = np.load(npy_p)
                    with open(ids_p, "r", encoding="utf-8") as f:
                        ids_list = json.load(f)
                    id_to_idx = {cid: i for i, cid in enumerate(ids_list)}
                    break
                except Exception as e:
                    logger.warning(f"Failed to load precomputed case law embeddings: {e}")

        model = get_sentence_transformer()
        if precomputed_mat is not None and model is not None:
            aligned = []
            missing_texts = []
            missing_indices = []

            for i, chunk in enumerate(self.corpus):
                if chunk.case_id in id_to_idx:
                    aligned.append(precomputed_mat[id_to_idx[chunk.case_id]])
                else:
                    aligned.append(None)
                    missing_texts.append(chunk.get_full_text())
                    missing_indices.append(i)

            if missing_texts and model is not None:
                new_embs = model.encode(missing_texts, normalize_embeddings=True, convert_to_numpy=True)
                for idx, emb in zip(missing_indices, new_embs):
                    aligned[idx] = emb

            if len(aligned) > 0 and all(v is not None for v in aligned):
                self.doc_embeddings = np.array(aligned, dtype=np.float32)
                return

        # Fallback if sentence-transformer model is unavailable
        self.doc_vectors = [self._embed_text_fallback(c.get_full_text()) for c in self.corpus]

    def _embed_text_fallback(self, text: str) -> Dict[str, float]:
        tokens = tokenize(text)
        vec: Dict[str, float] = Counter(tokens)
        for t in tokens:
            if len(t) >= 4:
                for k in range(len(t) - 2):
                    ngram = f"##{t[k:k+3]}"
                    vec[ngram] = vec.get(ngram, 0.0) + 0.5
        norm = math.sqrt(sum(v * v for v in vec.values()))
        if norm > 0:
            for k in vec:
                vec[k] /= norm
        return vec

    def score(self, query: str) -> List[float]:
        if self.doc_embeddings is not None:
            model = get_sentence_transformer()
            if model is not None:
                q_vec = model.encode(query, normalize_embeddings=True, convert_to_numpy=True)
                scores = np.dot(self.doc_embeddings, q_vec)
                return [float(s) for s in scores]

        q_vec = self._embed_text_fallback(query)
        scores = []
        for d_vec in (self.doc_vectors or []):
            dot = sum(q_vec[k] * d_vec.get(k, 0.0) for k in q_vec)
            scores.append(max(0.0, dot))
        return scores


class HybridLegalSearchEngine:
    """
    Unified Hybrid Search Engine:
    - Sparse BM25 retrieval (statutory corpus)
    - Dense semantic retrieval with SentenceTransformer (all-MiniLM-L6-v2)
    - Dedicated Case Law retrieval leg (BM25 + Dense RRF fusion)
    - Reciprocal Rank Fusion (RRF, k=60)
    - Metadata filtering (status != 'repealed', act filter)
    - Jurisdiction boosting (1.25x for user jurisdiction)
    - Query-type routing
    """
    def __init__(
        self,
        documents: Optional[List[DocumentChunk]] = None,
        case_law_entries: Optional[List[CaseLawChunk]] = None,
    ):
        self.documents = list(documents if documents is not None else load_default_documents())
        self.case_law_entries = list(case_law_entries if case_law_entries is not None else load_default_case_law())
        self._reindex()

    def _reindex(self):
        self.sparse_retriever = BM25Okapi(self.documents)
        self.dense_retriever = DenseSemanticRetriever(self.documents)
        self.case_sparse_retriever = CaseLawBM25(self.case_law_entries)
        self.case_dense_retriever = DenseCaseLawRetriever(self.case_law_entries)

    def add_documents(self, new_docs: List[DocumentChunk]):
        existing_ids = {d.id for d in self.documents}
        added = [d for d in new_docs if d.id not in existing_ids]
        if added:
            self.documents.extend(added)
            self._reindex()

    def add_case_law(self, new_cases: List[CaseLawChunk]):
        existing_ids = {c.case_id for c in self.case_law_entries}
        added = [c for c in new_cases if c.case_id not in existing_ids]
        if added:
            self.case_law_entries.extend(added)
            self._reindex()

    def _is_exact_case_query(self, query: str) -> bool:
        """Detect if query references specific case names or legal citations."""
        citation_pattern = r'\b(?:\d{4}\s+(?:SCC|AIR|SCR)|\(\d{4}\)\s+\d+\s+SCC|v\.|vs\.)\b'
        return bool(re.search(citation_pattern, query, re.IGNORECASE))

    def _is_exact_citation_query(self, query: str) -> bool:
        """Detect if query is an exact citation lookup like 'Section 138' or 'Article 21'."""
        pattern = r'\b(?:section|sec\.?|§|article|art\.?)\s*\d+[a-z]?\b'
        return bool(re.search(pattern, query, re.IGNORECASE))

    def search(
        self,
        query: str,
        jurisdiction: Optional[str] = None,
        act_filter: Optional[str] = None,
        limit: int = 5,
        include_repealed: bool = False,
    ) -> List[Tuple[DocumentChunk, float, str]]:
        """
        Executes hybrid retrieval and returns List of (DocumentChunk, score, mode).
        """
        if not self.documents:
            return []

        q_tokens = tokenize(query)
        is_exact = self._is_exact_citation_query(query)

        # 1. Compute Sparse Scores
        sparse_scores = self.sparse_retriever.score(q_tokens)
        sparse_ranked_indices = sorted(range(len(self.documents)), key=lambda i: sparse_scores[i], reverse=True)
        sparse_rank_map = {idx: rank + 1 for rank, idx in enumerate(sparse_ranked_indices)}

        # 2. Compute Dense Scores
        dense_scores = self.dense_retriever.score(query)
        dense_ranked_indices = sorted(range(len(self.documents)), key=lambda i: dense_scores[i], reverse=True)
        dense_rank_map = {idx: rank + 1 for rank, idx in enumerate(dense_ranked_indices)}

        # 3. Reciprocal Rank Fusion (RRF, k=60)
        rrf_k = 60
        fused_results: List[Tuple[DocumentChunk, float, str]] = []

        norm_jurisdiction = jurisdiction.strip().lower() if jurisdiction else None

        for i, doc in enumerate(self.documents):
            # Metadata hard filters
            if not include_repealed and doc.status == "repealed":
                continue
            if act_filter and act_filter.lower() not in doc.act.lower():
                continue

            # Query-type routing weights
            if is_exact:
                w_sparse = 0.85
                w_dense = 0.15
                mode = "sparse-heavy"
            else:
                w_sparse = 0.5
                w_dense = 0.5
                mode = "hybrid"

            r_sparse = sparse_rank_map[i]
            r_dense = dense_rank_map[i]

            rrf_score = (w_sparse / (rrf_k + r_sparse)) + (w_dense / (rrf_k + r_dense))

            # Jurisdiction boost
            if norm_jurisdiction and doc.jurisdiction.lower() == norm_jurisdiction:
                rrf_score *= 1.25

            # Must have non-zero relevance from at least one leg
            if sparse_scores[i] > 0 or dense_scores[i] > 0.15:
                fused_results.append((doc, rrf_score, mode))

        # Sort by fused score descending
        fused_results.sort(key=lambda x: x[1], reverse=True)

        # Deduplicate by (act, normalized section) so top results cover distinct statutory provisions
        deduped: List[Tuple[DocumentChunk, float, str]] = []
        seen_provisions = set()
        for doc, score, mode in fused_results:
            key = (doc.act.strip().lower(), doc.section.strip().lower())
            if key in seen_provisions:
                continue
            seen_provisions.add(key)
            deduped.append((doc, score, mode))
            if len(deduped) >= limit:
                break
        return deduped

    def search_case_law(
        self,
        query: str,
        legal_domain: Optional[str] = None,
        precedent_status: Optional[str] = None,
        limit: int = 5,
    ) -> List[Tuple[CaseLawChunk, float, str]]:
        """
        Executes hybrid retrieval over case law precedents.
        Kept strictly distinct from statutory retrieval to eliminate BM25 document length collisions.
        """
        if not self.case_law_entries:
            return []

        q_tokens = tokenize(query)
        is_exact = self._is_exact_case_query(query)

        # 1. Sparse BM25 scoring
        sparse_scores = self.case_sparse_retriever.score(q_tokens)
        sparse_ranked_indices = sorted(range(len(self.case_law_entries)), key=lambda i: sparse_scores[i], reverse=True)
        sparse_rank_map = {idx: rank + 1 for rank, idx in enumerate(sparse_ranked_indices)}

        # 2. Dense semantic scoring
        dense_scores = self.case_dense_retriever.score(query)
        dense_ranked_indices = sorted(range(len(self.case_law_entries)), key=lambda i: dense_scores[i], reverse=True)
        dense_rank_map = {idx: rank + 1 for rank, idx in enumerate(dense_ranked_indices)}

        # 3. Reciprocal Rank Fusion (RRF, k=60)
        rrf_k = 60
        fused_results: List[Tuple[CaseLawChunk, float, str]] = []

        for i, chunk in enumerate(self.case_law_entries):
            # Metadata filtering
            if legal_domain and legal_domain.lower() not in chunk.legal_domain.lower():
                continue
            if precedent_status and precedent_status.upper() != chunk.precedent_status.upper():
                continue

            if is_exact:
                w_sparse = 0.85
                w_dense = 0.15
                mode = "sparse-heavy"
            else:
                w_sparse = 0.5
                w_dense = 0.5
                mode = "hybrid"

            r_sparse = sparse_rank_map[i]
            r_dense = dense_rank_map[i]
            rrf_score = (w_sparse / (rrf_k + r_sparse)) + (w_dense / (rrf_k + r_dense))

            # Must have non-zero relevance from at least one leg
            if sparse_scores[i] > 0 or dense_scores[i] > 0.15:
                fused_results.append((chunk, rrf_score, mode))

        fused_results.sort(key=lambda x: x[1], reverse=True)

        deduped: List[Tuple[CaseLawChunk, float, str]] = []
        seen_ids = set()
        for chunk, score, mode in fused_results:
            if chunk.case_id in seen_ids:
                continue
            seen_ids.add(chunk.case_id)
            deduped.append((chunk, score, mode))
            if len(deduped) >= limit:
                break
        return deduped


# Global singleton instance
hybrid_search_engine = HybridLegalSearchEngine()
