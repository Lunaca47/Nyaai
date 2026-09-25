"""
Script to expand backend/data/statutory_codex.json with genuine statutory provisions
for Consumer Protection Act 2019, Code on Wages 2019, Payment of Wages Act 1936,
Payment of Gratuity Act 1972, Industrial Disputes Act 1947, and Domestic Violence Act 2005.
"""
import json
import os
import sys

NEW_PROVISIONS = [
    # ── Consumer Protection Act, 2019 ──────────────────────────────────────────
    {
        "id": "cpa_2019_sec_2",
        "act": "Consumer Protection Act, 2019",
        "section": "Section 2",
        "title": "Definitions — Consumer, Defect, Deficiency, and Product Liability",
        "content": "Defines 'consumer' as any person who buys goods or hires services for consideration, excluding commercial purpose. 'Defect' means any fault, imperfection or shortcoming in quality, quantity, potency, purity or standard required by law or contract. 'Deficiency' means any fault, imperfection, shortcoming or inadequacy in the quality, nature and manner of performance of a service. 'Product liability' means the responsibility of a product manufacturer or seller to compensate for harm caused by a defective product.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-07-20",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15256"
    },
    {
        "id": "cpa_2019_sec_34",
        "act": "Consumer Protection Act, 2019",
        "section": "Section 34",
        "title": "Jurisdiction of District Commission",
        "content": "The District Commission shall have jurisdiction to entertain complaints where the value of the goods or services paid as consideration does not exceed fifty lakh rupees. A complaint may be instituted in a District Commission within the local limits of whose jurisdiction the complainant resides or personally works for gain, or where the opposite party resides or carries on business, or where the cause of action wholly or in part arises.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-07-20",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15256"
    },
    {
        "id": "cpa_2019_sec_35",
        "act": "Consumer Protection Act, 2019",
        "section": "Section 35",
        "title": "Manner in which complaint shall be made to District Commission",
        "content": "A complaint in relation to any goods sold or delivered or agreed to be sold or delivered or any service provided or agreed to be provided may be filed with a District Commission by the consumer, any recognized consumer association, one or more consumers having the same interest, or the Central Government. The complaint may also be filed electronically through e-Daakhil with payment of prescribed fees.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-07-20",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15256"
    },
    {
        "id": "cpa_2019_sec_38",
        "act": "Consumer Protection Act, 2019",
        "section": "Section 38",
        "title": "Procedure on admission of complaint",
        "content": "The District Commission shall refer a copy of the admitted complaint within twenty-one days of admission to the opposite party, directing him to give his version of the case within a period of thirty days or such extended period not exceeding fifteen days. Where the complaint alleges a defect in goods which cannot be determined without proper analysis or test, the Commission shall obtain a sample and send it to an appropriate laboratory.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-07-20",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15256"
    },
    {
        "id": "cpa_2019_sec_39",
        "act": "Consumer Protection Act, 2019",
        "section": "Section 39",
        "title": "Findings of District Commission — Orders for Removal of Defects, Refund, and Compensation",
        "content": "Where the District Commission is satisfied that the goods or services suffer from any defect or deficiency, it shall issue an order directing the opposite party to: remove the defect; replace the goods with new goods of similar description; return to the complainant the price or consideration paid; pay compensation to the consumer for any loss or injury suffered due to negligence; pay punitive damages; and award costs of litigation.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-07-20",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15256"
    },
    {
        "id": "cpa_2019_sec_47",
        "act": "Consumer Protection Act, 2019",
        "section": "Section 47",
        "title": "Jurisdiction of State Commission",
        "content": "The State Commission shall have jurisdiction to entertain complaints where the value of the goods or services paid as consideration exceeds fifty lakh rupees but does not exceed two crore rupees, and appeals against the orders of any District Commission within the State.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-07-20",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15256"
    },
    {
        "id": "cpa_2019_sec_58",
        "act": "Consumer Protection Act, 2019",
        "section": "Section 58",
        "title": "Jurisdiction of National Commission (NCDRC)",
        "content": "The National Commission shall have jurisdiction to entertain complaints where the value of the goods or services paid as consideration exceeds two crore rupees, and appeals against the orders of any State Commission.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-07-20",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15256"
    },
    {
        "id": "cpa_2019_sec_69",
        "act": "Consumer Protection Act, 2019",
        "section": "Section 69",
        "title": "Limitation period — Two years from cause of action",
        "content": "The District Commission, the State Commission or the National Commission shall not admit a complaint unless it is filed within two years from the date on which the cause of action has arisen. A complaint may be entertained after the expiry of two years if the complainant satisfies the Commission that he had sufficient cause for not filing within such period, recording reasons for condoning delay.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-07-20",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15256"
    },
    {
        "id": "cpa_2019_sec_83",
        "act": "Consumer Protection Act, 2019",
        "section": "Section 83",
        "title": "Product liability action against manufacturer, service provider, or seller",
        "content": "A product liability action may be brought by a complainant against a product manufacturer or a product service provider or a product seller, as the case may be, for any harm caused to him on account of a defective product.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-07-20",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15256"
    },
    {
        "id": "cpa_2019_sec_84",
        "act": "Consumer Protection Act, 2019",
        "section": "Section 84",
        "title": "Liability of product manufacturer",
        "content": "A product manufacturer shall be liable in a product liability action if the product contains a manufacturing defect, design defect, deviation from manufacturing specifications, does not conform to express warranty, or fails to contain adequate instructions for correct usage or warnings of harm.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-07-20",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15256"
    },
    {
        "id": "cpa_2019_sec_86",
        "act": "Consumer Protection Act, 2019",
        "section": "Section 86",
        "title": "Liability of product seller",
        "content": "A product seller who is not a product manufacturer shall be liable in a product liability action if he exercised substantial control over designing, testing, packaging or labeling; altered or modified the product; failed to exercise reasonable care in assembling or maintaining; or failed to pass on manufacturer warnings.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-07-20",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15256"
    },

    # ── Code on Wages, 2019 ────────────────────────────────────────────────────
    {
        "id": "cow_2019_sec_5",
        "act": "Code on Wages, 2019",
        "section": "Section 5",
        "title": "Payment of minimum rate of wages",
        "content": "No employer shall pay to any employee wages less than the minimum rate of wages notified by the appropriate Government. Wages include all remuneration payable to an employed person in respect of his employment.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-12-18",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15257"
    },
    {
        "id": "cow_2019_sec_17",
        "act": "Code on Wages, 2019",
        "section": "Section 17",
        "title": "Time limit for payment of wages and settlement upon removal or resignation",
        "content": "The employer shall pay or cause to be paid wages: daily before the expiry of the shift; weekly on the last working day; fortnightly before the end of the second day; monthly before the expiry of the seventh day after the last day of the wage period. Where an employee is removed, dismissed, retrenched, resigns or becomes unemployed due to closure, the wages payable to him shall be paid within two working days of his removal, dismissal, retrenchment or resignation.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-12-18",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15257"
    },
    {
        "id": "cow_2019_sec_18",
        "act": "Code on Wages, 2019",
        "section": "Section 18",
        "title": "Deductions which may be made from wages",
        "content": "Wages of an employee shall be paid to him without deductions of any kind except those authorized by this Code (fines, absence from duty, damage to goods expressly entrusted, house accommodation, recovery of advances or loans). Total deductions in any wage period shall not exceed fifty per cent of such wages.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-12-18",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15257"
    },
    {
        "id": "cow_2019_sec_45",
        "act": "Code on Wages, 2019",
        "section": "Section 45",
        "title": "Claims under Code and procedure for unpaid wages and compensation",
        "content": "The appropriate Government may appoint one or more authorities to hear and determine claims arising out of non-payment of minimum wages, deductions from wages, delay in payment of wages or overtime. An application may be filed by an employee, any Trade Union registered under the Trade Unions Act, or an Inspector-cum-Facilitator within three years from the date on which the claim arose. The authority may direct payment of the unpaid wages together with compensation up to ten times the amount of the claim.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-12-18",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15257"
    },
    {
        "id": "cow_2019_sec_54",
        "act": "Code on Wages, 2019",
        "section": "Section 54",
        "title": "Penalties for non-payment of wages and contraventions",
        "content": "Any employer who pays to any employee less than the amount due under this Code shall be punishable with fine which may extend to fifty thousand rupees. Subsequent conviction within five years carries imprisonment up to three months or fine up to one lakh rupees, or both.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2020-12-18",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/15257"
    },

    # ── Payment of Wages Act, 1936 ─────────────────────────────────────────────
    {
        "id": "pwa_1936_sec_3",
        "act": "Payment of Wages Act, 1936",
        "section": "Section 3",
        "title": "Responsibility for payment of wages",
        "content": "Every employer shall be responsible for the payment to persons employed by him of all wages required to be paid under this Act.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "1936-04-23",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/2345"
    },
    {
        "id": "pwa_1936_sec_5",
        "act": "Payment of Wages Act, 1936",
        "section": "Section 5",
        "title": "Time of payment of wages — 7th day / 10th day deadline",
        "content": "The wages of every person employed upon or in any railway, factory or industrial or other establishment shall be paid before the expiry of the seventh day after the last day of the wage-period (or tenth day if establishment employs more than one thousand persons). Where employment is terminated, wages earned shall be paid before the expiry of the second working day from the day on which employment is terminated.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "1936-04-23",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/2345"
    },
    {
        "id": "pwa_1936_sec_15",
        "act": "Payment of Wages Act, 1936",
        "section": "Section 15",
        "title": "Claims arising out of deductions from wages or delay in payment of wages",
        "content": "The State Government may appoint a presiding officer of any Labour Court or Commissioner for Workmen's Compensation to hear and decide claims arising out of deductions from wages or delay in payment of wages. The Authority may direct the refund of the amount deducted or payment of delayed wages together with compensation up to ten times the amount deducted, or not exceeding three thousand rupees for delayed wages.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "1936-04-23",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/2345"
    },

    # ── Payment of Gratuity Act, 1972 ──────────────────────────────────────────
    {
        "id": "pga_1972_sec_4",
        "act": "Payment of Gratuity Act, 1972",
        "section": "Section 4",
        "title": "Payment of gratuity — Five years continuous service eligibility and computation",
        "content": "Gratuity shall be payable to an employee on the termination of his employment after he has rendered continuous service for not less than five years: on his superannuation, on his retirement or resignation, or on his death or disablement due to accident or disease (completion of continuous service of five years shall not be necessary where termination is due to death or disablement). Gratuity is calculated at the rate of fifteen days' wages based on the rate of wages last drawn for every completed year of service or part thereof in excess of six months, subject to statutory maximum ceiling.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "1972-09-16",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/1510"
    },
    {
        "id": "pga_1972_sec_7",
        "act": "Payment of Gratuity Act, 1972",
        "section": "Section 7",
        "title": "Determination of amount of gratuity — Thirty days deadline and 10% compound interest",
        "content": "A person who is eligible for payment of gratuity shall send a written application to the employer within thirty days. As soon as gratuity becomes payable, the employer shall, whether an application has been made or not, determine the amount of gratuity and give notice in writing to the person and to the Controlling Authority. The employer shall arrange to pay the amount of gratuity within thirty days from the date it becomes payable. Sub-section (3A): If the amount of gratuity payable is not paid by the employer within the period specified, the employer shall pay simple interest at such rate (currently 10% per annum) from the date on which gratuity becomes payable to the date on which it is paid.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "1972-09-16",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/1510"
    },
    {
        "id": "pga_1972_sec_8",
        "act": "Payment of Gratuity Act, 1972",
        "section": "Section 8",
        "title": "Recovery of gratuity — Revenue Recovery Certificate (RRC) via District Collector",
        "content": "If the amount of gratuity payable under this Act is not paid by the employer, within the prescribed time, to the person entitled thereto, the Controlling Authority shall, on an application made to it in this behalf by the aggrieved person, issue a certificate for that amount to the Collector, who shall recover the same, together with compound interest thereon at the rate of fifteen per cent per annum from the date of expiry of the prescribed time, as arrears of land revenue and pay the same to the person entitled thereto.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "1972-09-16",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/1510"
    },
    {
        "id": "pga_1972_sec_9",
        "act": "Payment of Gratuity Act, 1972",
        "section": "Section 9",
        "title": "Penalties for failure to comply with payment of gratuity",
        "content": "Whoever, for the purpose of avoiding any payment to be made by himself under this Act, knowingly makes any false statement or false representation shall be punishable with imprisonment for a term which may extend to six months, or with fine. An employer who contravenes, or makes default in complying with, any provision of this Act shall be punishable with imprisonment for a term not less than three months but which may extend to one year, or with fine not less than ten thousand rupees but which may extend to twenty thousand rupees, or with both.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "1972-09-16",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/1510"
    },
    {
        "id": "pga_1972_sec_13",
        "act": "Payment of Gratuity Act, 1972",
        "section": "Section 13",
        "title": "Protection of gratuity against attachment by civil court",
        "content": "No gratuity payable under this Act and no gratuity payable to an employee employed in any establishment, factory, mine, oilfield, plantation, port, railway company or shop exempted under section 5 shall be liable to attachment in execution of any decree or order of any civil, revenue or criminal court.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "1972-09-16",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/1510"
    },

    # ── Industrial Disputes Act, 1947 ──────────────────────────────────────────
    {
        "id": "ida_1947_sec_2a",
        "act": "Industrial Disputes Act, 1947",
        "section": "Section 2A",
        "title": "Dismissal, etc., of an individual workman deemed to be an industrial dispute",
        "content": "Where any employer discharges, dismisses, retrenches, or otherwise terminates the services of an individual workman, any dispute or difference between that workman and his employer connected with, or arising out of, such discharge, dismissal, retrenchment or termination shall be deemed to be an industrial dispute notwithstanding that no other workman nor any union of workmen is a party to the dispute. Workman may make application directly to Labour Court after three months of conciliation application.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "1947-04-01",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/1701"
    },
    {
        "id": "ida_1947_sec_33c",
        "act": "Industrial Disputes Act, 1947",
        "section": "Section 33C",
        "title": "Recovery of money due from an employer",
        "content": "Sub-section (1): Where any money is due to a workman from an employer under a settlement or an award or under the provisions of Chapter VA or Chapter VB, the workman himself or any other person authorized by him in writing may make an application to the appropriate Government for the recovery of the money due to him, and if the appropriate Government is satisfied that any money is so due, it shall issue a certificate for that amount to the Collector who shall proceed to recover the same in the same manner as an arrear of land revenue.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "1947-04-01",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/1701"
    },
    {
        "id": "ida_1947_sec_33c_2",
        "act": "Industrial Disputes Act, 1947",
        "section": "Section 33C(2)",
        "title": "Recovery of money or computation of benefit by Labour Court",
        "content": "Where any workman is entitled to receive from the employer any money or any benefit which is capable of being computed in terms of money, if any question arises as to the amount of money due or as to the amount at which such benefit should be computed, then the question may, subject to any rules that may be made under this Act, be decided by such Labour Court as may be specified in this behalf by the appropriate Government; and the amount so decided may be recovered in the manner provided for in sub-section (1). (Governs recovery of unpaid back wages, withheld increments, and contractual dues).",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "1947-04-01",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/1701"
    },

    # ── Protection of Women from Domestic Violence Act, 2005 ───────────────────
    {
        "id": "dva_2005_sec_3",
        "act": "Protection of Women from Domestic Violence Act, 2005",
        "section": "Section 3",
        "title": "Definition of domestic violence — Physical, Sexual, Verbal, Emotional, and Economic Abuse",
        "content": "For the purposes of this Act, any act, omission or commission or conduct of the respondent shall constitute domestic violence if it harms, injures or endangers the health, safety, life, limb or well-being, whether mental or physical, of the aggrieved person: physical abuse, sexual abuse, verbal and emotional abuse (insults, ridicule, humiliation, name calling), and economic abuse. Economic abuse includes deprivation of all or any economic or financial resources, household necessities, maintenance, or disposal of household assets, stridhan, or alienation of the shared household.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2006-10-26",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/2026"
    },
    {
        "id": "dva_2005_sec_12",
        "act": "Protection of Women from Domestic Violence Act, 2005",
        "section": "Section 12",
        "title": "Application to Magistrate for seeking reliefs",
        "content": "An aggrieved person or a Protection Officer or any other person on behalf of the aggrieved person may present an application to the Magistrate seeking one or more reliefs under this Act (protection orders, residence orders, monetary relief, custody orders, or compensation). The Magistrate shall take into consideration any domestic incident report (DIR) received by him from the Protection Officer or service provider. The Magistrate shall fix the first date of hearing, which shall not ordinarily be beyond three days from the date of receipt of the application, and dispose of every application within a period of sixty days from the date of its first hearing.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2006-10-26",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/2026"
    },
    {
        "id": "dva_2005_sec_17",
        "act": "Protection of Women from Domestic Violence Act, 2005",
        "section": "Section 17",
        "title": "Right to reside in a shared household",
        "content": "Notwithstanding anything contained in any other law for the time being in force, every woman in a domestic relationship shall have the right to reside in the shared household, whether or not she has any right, title or beneficial interest in the same. The aggrieved person shall not be evicted or excluded from the shared household or any part of it by the respondent save in accordance with the procedure established by law. (Reinforced by Supreme Court in Satish Chander Ahuja v. Sneha Ahuja).",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2006-10-26",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/2026"
    },
    {
        "id": "dva_2005_sec_18",
        "act": "Protection of Women from Domestic Violence Act, 2005",
        "section": "Section 18",
        "title": "Protection orders against domestic violence",
        "content": "The Magistrate may, after giving the aggrieved person and the respondent an opportunity of being heard and on being prima facie satisfied that domestic violence has taken place or is likely to take place, pass a protection order prohibiting the respondent from committing any act of domestic violence; aiding or abetting in the commission of acts; entering the place of employment or school of children; attempting to communicate in any form with the aggrieved person; alienating any assets or operating bank lockers; or causing violence to dependents.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2006-10-26",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/2026"
    },
    {
        "id": "dva_2005_sec_19",
        "act": "Protection of Women from Domestic Violence Act, 2005",
        "section": "Section 19",
        "title": "Residence orders — Restraining dispossession and directing alternate accommodation",
        "content": "While disposing of an application under section 12(1), the Magistrate may pass a residence order: restraining the respondent from dispossessing or in any other manner disturbing the possession of the aggrieved person from the shared household; directing the respondent to remove himself from the shared household; restraining the respondent or his relatives from entering any portion of the shared household; restraining the respondent from alienating or disposing of the shared household; or directing the respondent to secure same level of alternate accommodation or pay rent.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2006-10-26",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/2026"
    },
    {
        "id": "dva_2005_sec_20",
        "act": "Protection of Women from Domestic Violence Act, 2005",
        "section": "Section 20",
        "title": "Monetary reliefs — Medical expenses, loss of earnings, and maintenance",
        "content": "While disposing of an application under section 12(1), the Magistrate may direct the respondent to pay monetary relief to meet the expenses incurred and losses suffered by the aggrieved person and any child as a result of domestic violence: loss of earnings, medical expenses, loss caused by destruction or damage of property, and the maintenance for the aggrieved person as well as her children, which shall be fair, reasonable and consistent with the standard of living to which she is accustomed.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2006-10-26",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/2026"
    },
    {
        "id": "dva_2005_sec_21",
        "act": "Protection of Women from Domestic Violence Act, 2005",
        "section": "Section 21",
        "title": "Custody orders for children",
        "content": "Notwithstanding anything contained in any other law for the time being in force, the Magistrate may, at any stage of hearing of the application for grant of relief for a person under this Act, grant temporary custody of any child or children to the aggrieved person or the person making an application on her behalf and specify, if necessary, the arrangements for visit of such child or children by the respondent: Provided that if the Magistrate is of the opinion that any visit of the respondent may be harmful to the child, the Magistrate shall refuse to allow such visit.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2006-10-26",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/2026"
    },
    {
        "id": "dva_2005_sec_22",
        "act": "Protection of Women from Domestic Violence Act, 2005",
        "section": "Section 22",
        "title": "Compensation orders for injuries and emotional distress",
        "content": "In addition to other reliefs, the Magistrate may, on an application made by the aggrieved person, pass an order directing the respondent to pay compensation and damages for the injuries, including mental torture and emotional distress, caused by the acts of domestic violence committed by that respondent.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2006-10-26",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/2026"
    },
    {
        "id": "dva_2005_sec_23",
        "act": "Protection of Women from Domestic Violence Act, 2005",
        "section": "Section 23",
        "title": "Power to grant interim and ex parte orders",
        "content": "In any proceeding before him under this Act, the Magistrate may pass such interim order as he deems just and proper. Sub-section (2): If the Magistrate is satisfied that an application prima facie discloses that the respondent is committing, or has committed an act of domestic violence or that there is a likelihood that the respondent may commit an act of domestic violence, he may grant an ex parte order on the basis of the affidavit in the prescribed form of the aggrieved person under section 18, section 19, section 20, section 21 or, as the case may be, section 22 against the respondent.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2006-10-26",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/2026"
    },
    {
        "id": "dva_2005_sec_31",
        "act": "Protection of Women from Domestic Violence Act, 2005",
        "section": "Section 31",
        "title": "Penalty for breach of protection order by respondent — One year imprisonment",
        "content": "A breach of protection order, or of an interim protection order, by the respondent shall be an offence under this Act and shall be punishable with imprisonment of either description for a term which may extend to one year, or with fine which may extend to twenty thousand rupees, or with both. The offence under sub-section (1) shall be cognizable and non-bailable.",
        "jurisdiction": "central",
        "status": "in_force",
        "effective_date": "2006-10-26",
        "source_url": "https://www.indiacode.nic.in/handle/123456789/2026"
    }
]

def expand_codex():
    codex_path = os.path.join("backend", "data", "statutory_codex.json")
    if not os.path.exists(codex_path):
        print(f"Error: {codex_path} does not exist!")
        sys.exit(1)

    with open(codex_path, "r", encoding="utf-8") as f:
        existing = json.load(f)

    existing_ids = {item["id"] for item in existing}
    added_count = 0
    for prov in NEW_PROVISIONS:
        if prov["id"] not in existing_ids:
            existing.append(prov)
            existing_ids.add(prov["id"])
            added_count += 1

    with open(codex_path, "w", encoding="utf-8") as f:
        json.dump(existing, f, indent=2, ensure_ascii=False)

    print(f"Added {added_count} new provisions to {codex_path}. Total: {len(existing)}")

if __name__ == "__main__":
    expand_codex()
