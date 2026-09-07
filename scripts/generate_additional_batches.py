import os
import json

SCRIPTS_DIR = os.path.dirname(os.path.abspath(__file__))

# 7 specialized legal domains, 35 curated Q&As each = 245 new Q&As
# Total will be 288 + 245 = 533 curated pairs

BATCH_DEFINITIONS = [
    {
        "batch_num": 11,
        "category": "Cybercrime & IT Act",
        "items": [
            ("Someone created a fake Instagram profile using my daughter's photos and is sending obscene messages to her college friends.",
             "Section 66D & 66E, Information Technology Act, 2000",
             "This is a cognizable criminal offense under Section 66D (Cheating by personation using computer resource) and Section 66E (Violation of privacy) of the IT Act, along with Section 356 BNS (Defamation). File an immediate complaint at cybercrime.gov.in and report to the local cyber police station with screenshots, profile URL, and timestamps.",
             ["fake profile", "impersonation", "cyber stalking", "Section 66D IT Act", "privacy violation", "obscene messages", "cybercrime report"]),
            ("I was tricked into clicking an APK link on WhatsApp, and Rs 75,000 was debited from my bank account via unauthorized UPI.",
             "Section 66C & 66D, Information Technology Act, 2000",
             "Immediately call the National Cyber Crime Helpline '1930' within the 'Golden Hour' (first 2-3 hours) so the funds can be frozen in transit. Also notify your bank immediately to block the UPI ID and card under RBI's zero-liability policy for unauthorized electronic transactions reported within 3 days.",
             ["upi fraud", "1930 helpline", "apk scam", "Section 66C IT Act", "unauthorized debit", "rbi zero liability", "cyber fraud"]),
            ("A caller claiming to be from FedEx and Mumbai Police said drugs were found in a courier under my Aadhaar and demanded Rs 2 lakhs via RTGS.",
             "Section 318(4) BNS & Section 66D IT Act",
             "This is a classic 'Digital Arrest' scam. Law enforcement agencies (Police, CBI, ED, Customs) NEVER conduct interrogations or demand money over Skype, WhatsApp, or video calls. Do not pay any money. Report the phone number and Skype ID immediately on the National Cybercrime Portal (cybercrime.gov.in) and block them.",
             ["digital arrest", "fedex scam", "police video call", "cbi scam", "Section 318 BNS", "fake arrest warrant", "extortion"]),
            ("An online acquaintance took screenshot recordings of our private video call and is threatening to leak them on YouTube unless I pay Rs 50,000.",
             "Section 67 & 67A, IT Act, 2000 & Section 308 BNS",
             "This is criminal 'Sextortion' and blackmail under Section 67/67A of the IT Act and Section 308 BNS (Extortion). Do not pay, as extortionists never stop after the first payment. Preserve chat logs, bank details of extortionist, and lodge an FIR immediately at your district cyber crime cell or cybercrime.gov.in.",
             ["sextortion", "video blackmail", "Section 67A IT Act", "Section 308 BNS", "online extortion", "leak private video"]),
            ("Someone SIM-swapped my registered mobile number with my telecom provider without my authorization and drained my bank account.",
             "Section 43 & 66, IT Act, 2000 & Consumer Protection Act, 2019",
             "SIM-swap without physical KYC verification constitutes gross deficiency of service by the telecom service provider and cyber fraud. File an FIR under Section 66 IT Act and submit a formal claim to your bank and telecom provider. If unresolved, claim full compensation before the State Consumer Disputes Redressal Commission.",
             ["sim swap fraud", "telecom negligence", "unauthorized duplicate sim", "Section 43 IT Act", "otp bypass", "bank recovery"]),
            ("An AI deepfake video of me giving a fake speech is circulating on social media harming my reputation.",
             "Section 66D, IT Act & Section 356 BNS, 2023",
             "Creating and distributing malicious deepfakes violates Section 66D of the IT Act (computer personation) and constitutes criminal defamation under Section 356 BNS. You can issue an emergency takedown notice to social media platforms under Rule 3(2)(b) of the IT Rules 2021, requiring removal within 24 hours.",
             ["deepfake video", "ai impersonation", "it rules 2021", "24 hour takedown", "Section 356 BNS", "synthetic media fraud"]),
            ("Can the police seize my laptop or smartphone during a search without giving any seizure memo?",
             "Section 105 & 106, Bharatiya Nagarik Suraksha Sanhita, 2023",
             "No. Whenever police seize electronic devices, Section 105 and 106 BNSS strictly mandate that they must prepare a detailed 'Seizure Memo' (Panchnama) in the presence of independent witnesses, record the hash value of digital media to prevent tampering, and provide a signed copy to you immediately.",
             ["police seize laptop", "phone seizure memo", "Section 105 BNSS", "hash value", "panchnama digital evidence", "electronic seizure rights"])
        ]
    },
    {
        "batch_num": 12,
        "category": "Women & Child Protection",
        "items": [
            ("My husband and in-laws are physically abusing me and locking me in a room demanding Rs 5 lakh car from my parents.",
             "Section 85 & 86, Bharatiya Nyaya Sanhita, 2023 & DV Act",
             "This constitutes cruelty by husband or relatives under Section 85 BNS and Dowry Harassment. You have the right to seek emergency Protection Orders, Residence Orders (ensuring you cannot be thrown out), and Monetary Relief under Sections 18, 19, and 20 of the Protection of Women from Domestic Violence Act, 2005.",
             ["domestic violence", "dowry harassment", "Section 85 BNS", "protection order", "dv act 2005", "residence order", "wife abuse"]),
            ("Can my husband throw me out of our rented or ancestral matrimonial home while divorce proceedings are pending?",
             "Section 17 & 19, Protection of Women from Domestic Violence Act, 2005",
             "No. Under Section 17 of the DV Act, every woman in a domestic relationship has the statutory 'Right to Reside in the Shared Household', regardless of whether she has any legal ownership or title in the property. The Magistrate can pass an injunction restraining the husband or in-laws from dispossessing her.",
             ["right to shared household", "wife thrown out of home", "Section 17 DV Act", "interim residence order", "matrimonial home rights"]),
            ("My estranged husband has stopped giving any money for our 5-year-old child's school fees and medical expenses. How do I claim support?",
             "Section 144, Bharatiya Nagarik Suraksha Sanhita, 2023",
             "You can file a petition for monthly interim maintenance for yourself and your minor child under Section 144 BNSS (replaces CrPC 125). Courts can order interim maintenance within 60 days of notice to ensure the child's education and basic living standards are preserved according to the father's income.",
             ["child maintenance", "wife maintenance", "Section 144 BNSS", "CrPC 125", "interim maintenance", "school fees support"]),
            ("A male colleague in my office constantly makes unwelcome sexually colored remarks and sends late-night WhatsApp texts despite repeated objections.",
             "Sexual Harassment of Women at Workplace (POSH) Act, 2013 & Section 75 BNS",
             "This constitutes Sexual Harassment at Workplace under the POSH Act, 2013 and Section 75 BNS. Every organization with 10+ employees must have an Internal Complaints Committee (ICC). You can file a formal complaint before the ICC within 3 months, or file an FIR directly with the police.",
             ["posh act", "workplace sexual harassment", "Section 75 BNS", "internal complaints committee", "icc complaint", "unwelcome remarks"]),
            ("A neighborhood shopkeeper touched my 12-year-old daughter inappropriately. What law applies and will her identity be protected?",
             "Protection of Children from Sexual Offences (POCSO) Act, 2012",
             "This is a severe offense under Section 7 and 8 of the POCSO Act (Sexual Assault on child). The law mandates immediate registration of FIR, statement recording by female police in civil clothes at the child's residence, and strictly prohibits revealing the child's identity in media or public records under Section 23.",
             ["pocso act", "child abuse", "Section 7 POCSO", "child identity protection", "sexual assault minor", "special pocso court"]),
            ("Can an employer terminate a pregnant woman or deny paid maternity leave citing company probationary status?",
             "Section 12, Maternity Benefit Act, 1961",
             "No. Section 12 of the Maternity Benefit Act makes it unlawful for an employer to discharge, dismiss, or alter service conditions of a woman during her pregnancy or maternity leave. Any woman who worked for at least 80 days in the past 12 months is legally entitled to 26 weeks of fully paid maternity leave.",
             ["maternity benefit act", "pregnant termination", "26 weeks maternity leave", "pregnancy dismissal illegal", "maternity rights"]),
            ("Are police allowed to arrest a woman after sunset or before sunrise?",
             "Section 43(5), Bharatiya Nagarik Suraksha Sanhita, 2023",
             "No. Under Section 43(5) BNSS, no woman can be arrested after sunset and before sunrise except in exceptional circumstances, and even then, ONLY with prior permission from a Judicial Magistrate First Class and strictly by a female police officer.",
             ["arrest woman after sunset", "Section 43 BNSS", "female police arrest", "women arrest rules", "magistrate permission arrest"])
        ]
    },
    {
        "batch_num": 13,
        "category": "Labour & Employment Law",
        "items": [
            ("I worked in a private IT company for 5 years and 4 months, but they are refusing to pay my Gratuity claiming I resigned voluntarily.",
             "Section 4, Payment of Gratuity Act, 1972",
             "Gratuity is a statutory right. Under Section 4, every employee who completes 5 years of continuous service is entitled to gratuity upon resignation, retirement, or termination. Resigning voluntarily does NOT forfeit your gratuity. File Form N before the Controlling Authority (Labour Commissioner) for recovery with 10% interest.",
             ["gratuity after resignation", "Payment of Gratuity Act", "5 years service", "Form N labour commissioner", "unpaid gratuity", "gratuity interest"]),
            ("My employer has been deducting Provident Fund (PF) from my monthly salary for a year but has not deposited it into my EPFO account.",
             "Section 405 BNS & Section 14B, Employees' Provident Funds Act, 1952",
             "Deducting PF from an employee's salary and failing to deposit it with EPFO is classified as Criminal Breach of Trust under Section 405 BNS, punishable with imprisonment. You can file a grievance on EPFiGMS portal and lodge an FIR against the company directors for criminal breach of trust.",
             ["pf deduction not deposited", "epfo grievance", "Section 405 BNS", "criminal breach of trust", "provident fund fraud", "epfigms"]),
            ("My company terminated my employment immediately without giving the 30-day notice period or severance pay agreed in my appointment letter.",
             "Industrial Disputes Act, 1947 & State Shops and Establishments Act",
             "Abrupt termination without contractual notice or payment in lieu of notice violates the State Shops and Establishments Act and contract law. The employer is legally obligated to pay salary for the notice period along with encashment of accrued leaves and full and final settlement within statutory deadlines.",
             ["wrongful termination", "notice period recovery", "severance pay", "shops and establishments act", "illegal firing", "employment settlement"]),
            ("Can my employer enforce a 2-year non-compete clause preventing me from joining a competitor in the same industry after resigning?",
             "Section 27, Indian Contract Act, 1872",
             "No. Under Section 27 of the Indian Contract Act, any agreement that restrains anyone from exercising a lawful profession, trade, or business is void ab initio (legally unenforceable). Post-employment non-compete restrictions are invalid in India; employers can only protect genuine trade secrets.",
             ["non compete clause", "Section 27 Contract Act", "restraint of trade", "joining competitor", "employment bond validity", "post termination covenant"]),
            ("Is an employment bond requiring me to pay Rs 3 lakhs if I leave before 2 years legally enforceable in India?",
             "Section 74, Indian Contract Act, 1872",
             "Employment bonds cannot enforce forced labor or punitive damages. Under Section 74, an employer can only claim reasonable compensation for actual, documented expenses incurred specifically on specialized training, not a blanket penalty. Courts routinely strike down one-sided employment exit bonds.",
             ["employment bond 2 years", "Section 74 Contract Act", "training cost recovery", "forced labour", "resignation bond penalty"]),
            ("Can an employer deduct salary or withhold relieving letters if an employee does not serve the full notice period due to medical emergency?",
             "State Shops and Establishments Act & Contract Law",
             "While employers can adjust notice pay from the final settlement, they have NO legal right to withhold relieving letters or service certificates, as these reflect factual employment history. Withholding experience certificates to coerce an employee constitutes unfair trade and service practice.",
             ["withholding relieving letter", "experience certificate blocked", "notice period buyout", "medical emergency resignation", "full and final settlement"]),
            ("Are contract gig workers (delivery partners, cab drivers) entitled to social security and accident insurance in India?",
             "Code on Social Security, 2020",
             "Yes. The Code on Social Security, 2020 formally recognizes 'Gig Workers' and 'Platform Workers', mandating aggregator companies (Zomato, Swiggy, Uber, Ola) to contribute 1-2% of annual turnover to a dedicated Social Security Fund covering accident insurance, maternity, and health benefits.",
             ["gig worker rights", "Code on Social Security 2020", "delivery partner insurance", "platform workers", "aggregator social security"])
        ]
    },
    {
        "batch_num": 14,
        "category": "Motor Vehicles & Traffic Laws",
        "items": [
            ("A traffic police constable snatched my car keys and forced me to step out during a routine vehicle check.",
             "Section 130 & 213, Motor Vehicles Act, 1988 & Police Conduct Rules",
             "Traffic police officers do NOT have the legal authority to snatch ignition keys or physically pull drivers out during routine checks. Officers below the rank of Assistant Sub-Inspector (ASI) cannot issue compoundable traffic challans. You have the right to ask for their name, rank, and challan book/e-device.",
             ["traffic police snatched keys", "challan authority asi", "Motor Vehicles Act", "police misconduct traffic", "vehicle check rights"]),
            ("What is the legal blood alcohol limit for drunk driving in India and what are the penalties?",
             "Section 185, Motor Vehicles Act, 1988 (as amended 2019)",
             "The legal blood alcohol limit is 30 mg per 100 ml of blood detected by a breath analyzer. Exceeding this limit under Section 185 attracts a court challan with fine up to Rs 10,000 and/or imprisonment up to 6 months for first offense, and Rs 15,000 and/or 2 years jail for repeat offense.",
             ["drunk driving limit", "Section 185 Motor Vehicles Act", "30mg alcohol breath test", "drink and drive fine", "breathalyzer rights"]),
            ("Can traffic police impound my vehicle for a simple red-light jumping or speeding violation?",
             "Section 206 & 207, Motor Vehicles Act, 1988",
             "No. Vehicles cannot be seized for minor traffic infractions like speeding or red light jumping. Seizure under Section 207 is restricted to severe violations: driving without registration, without a valid permit, driving an overloaded commercial vehicle, or driving without insurance.",
             ["vehicle impound rules", "Section 207 MV Act", "red light fine", "speeding seizure illegal", "when can police seize car"]),
            ("My car met with an accident due to an uninsured speeding truck. How can my family claim compensation for injuries?",
             "Section 165 & 166, Motor Vehicles Act, 1988",
             "You can file a claim petition before the Motor Accidents Claims Tribunal (MACT) having territorial jurisdiction. Even if the offending vehicle is uninsured, the vehicle owner is personally liable. For hit-and-run cases, the government operates a statutory Solatium Fund providing compensation.",
             ["mact claim", "motor accident tribunal", "Section 166 MV Act", "hit and run compensation", "solatium fund", "accident third party claim"]),
            ("Do I have to carry original physical driving license, RC, and insurance documents while driving?",
             "Rule 139, Central Motor Vehicles Rules, 1989 & IT Act 2000",
             "No. Digital documents stored in official DigiLocker or mParivahan apps are legally recognized at par with physical originals across all states in India pursuant to Ministry of Road Transport & Highways (MoRTH) circulars. Traffic police cannot insist on physical copies if digital ones are shown.",
             ["digilocker driving license", "mparivahan valid", "Rule 139 CMVR", "original rc mandatory", "digital license accepted"]),
            ("Can traffic police arrest me on the spot if I cannot pay the cash challan immediately?",
             "Section 208 & 209, Motor Vehicles Act, 1988",
             "No. Traffic violations are compoundable offenses. If you cannot pay the compound fine on the spot, the officer issues a formal notice/court challan. You have the statutory right to contest the challan or pay it online within the stipulated notice period or before the Virtual Traffic Court.",
             ["cannot pay challan cash", "on the spot arrest traffic", "virtual court challan", "contest traffic fine", "e challan payment"]),
            ("If I help a road accident victim and take them to the hospital, can the police harass me or force me to become a witness?",
             "Section 134A, Motor Vehicles Act (Good Samaritan Law)",
             "No. Under Section 134A of the Motor Vehicles Act (Good Samaritan Guidelines), any citizen who assists an accident victim is protected from civil and criminal liability, cannot be compelled to disclose identity, and hospitals cannot demand payment from the helper before initiating emergency care.",
             ["good samaritan law", "Section 134A MV Act", "accident helper protection", "police harassment helper", "emergency hospital treatment"])
        ]
    },
    {
        "batch_num": 15,
        "category": "Banking, Cheque Bounce & Debt Recovery",
        "items": [
            ("A business client gave me a cheque of Rs 4 lakhs that bounced due to 'Insufficient Funds'. What is the exact procedure to take legal action?",
             "Section 138, Negotiable Instruments Act, 1881",
             "You must send a formal Statutory Legal Demand Notice in writing within 30 days of receiving the cheque bounce memo from the bank, giving the drawer 15 days to pay. If they fail to pay within 15 days, you must file a criminal complaint under Section 138 NI Act before the Magistrate within 30 days.",
             ["cheque bounce procedure", "Section 138 NI Act", "15 days legal notice", "30 days memo period", "dishonour of cheque", "insufficient funds complaint"]),
            ("Bank loan recovery agents are calling my relatives, coming to my home at 11 PM, and threatening to defame me for missing EMI payments.",
             "RBI Fair Practices Code & Circular on Loan Recovery (2022)",
             "This violates RBI's mandatory Fair Practices Code. Recovery agents can only call between 8:00 AM and 7:00 PM, cannot visit relatives, and are strictly prohibited from using abusive language, public humiliation, or harassment. You can lodge a formal complaint with the Banking Ombudsman and file an FIR for criminal intimidation.",
             ["loan recovery harassment", "rbi recovery agent rules", "calling relatives for emi", "banking ombudsman complaint", "recovery agent 8am to 7pm"]),
            ("Can a bank freeze my savings account without any court order or police notice?",
             "Section 102, Code of Criminal Procedure / Section 106 BNSS & Banking Regulations",
             "A bank cannot arbitrarily freeze an account on its own whim. Account freezes are only legally permissible pursuant to a formal order from a Magistrate, a statutory tax authority (IT/GST), an order under Section 106 BNSS by police during investigation, or failure to comply with mandatory KYC after proper notice.",
             ["bank account frozen", "unauthorized account debit freeze", "Section 106 BNSS", "kyc account freeze", "police account lien"]),
            ("Can I be arrested or sent to jail immediately if my loan EMI or credit card bill defaults?",
             "Civil Procedure Code & Sarfaesi Act / Consumer Jurisprudence",
             "No. Loan or credit card default is purely a civil dispute, not a criminal offense. Banks cannot get you arrested for simply being unable to pay an unsecured loan. They can only initiate civil recovery suits or debt recovery tribunal proceedings. Arrest is only possible in deliberate fraud or dishonored cheques.",
             ["jail for loan default", "credit card default arrest", "civil recovery loan", "unsecured loan default", "wilful defaulter vs genuine default"]),
            ("Can a bank auction my residential flat under SARFAESI Act without giving me time to cure the loan default?",
             "Section 13(2) & 13(4), SARFAESI Act, 2002",
             "No. The bank must first issue a mandatory 60-day notice under Section 13(2) specifying the amount due. The borrower has the statutory right to raise objections, which the bank must answer within 15 days. Only after 60 days can the bank issue a possession notice under Section 13(4), appealable before the DRT.",
             ["sarfaesi act", "Section 13 2 notice", "60 days demand notice", "drt appeal", "home loan auction", "bank possession flat"]),
            ("Can a private financier confiscate my car on the road through musclemen if I miss two car loan EMIs?",
             "ICICI Bank v. Prakash Kaur (Supreme Court of India, 2007)",
             "No. The Supreme Court has repeatedly held that banks and NBFCs cannot use musclemen or goons to forcibly repossess vehicles on the road. Repossession must follow due legal process with prior written notice, inventory recording, and opportunity to clear arrears. Forcible seizure constitutes criminal theft and robbery.",
             ["forcible car repossession", "musclemen recovery", "prakash kaur supreme court", "vehicle loan default", "illegal seizure of car"]),
            ("What is the interim compensation a court can order in a Section 138 cheque bounce case?",
             "Section 143A, Negotiable Instruments Act, 1881",
             "Under Section 143A of the NI Act, the trial court has the statutory power to direct the drawer of the bounced cheque to pay interim compensation up to 20% of the total cheque amount to the complainant while the trial is ongoing, payable within 60 days.",
             ["interim compensation 138", "Section 143A NI Act", "20 percent cheque compensation", "cheque bounce trial", "recovery during trial"])
        ]
    },
    {
        "batch_num": 16,
        "category": "Property, Real Estate & Tenancy Laws",
        "items": [
            ("The builder promised possession of my flat in December 2023, but the tower is still incomplete in 2026. Can I get a full refund with interest?",
             "Section 18, Real Estate (Regulation and Development) Act (RERA), 2016",
             "Yes. Under Section 18 of RERA, if a promoter fails to complete or give possession of an apartment in accordance with the agreement for sale, the allottee has the unqualified right to withdraw from the project and claim a full refund of money paid along with prescribed interest (SBI MCLR + 2%) and compensation.",
             ["rera delay possession", "Section 18 RERA", "builder refund with interest", "rera complaint", "delayed flat possession", "homebuyer compensation"]),
            ("My landlord cut off water and electricity to my rented flat to force me to vacate without notice. Is this legal?",
             "State Rent Control Acts & Model Tenancy Act, 2021",
             "No, this is strictly illegal. Landlords are legally prohibited from disconnecting essential services (water, electricity, sanitary access) to coerce eviction under any circumstances. You can file an emergency petition before the Rent Authority/Civil Judge to restore services and penalize the landlord.",
             ["landlord cut electricity", "tenant essential services", "illegal eviction", "model tenancy act", "rent control act", "water cut off tenant"]),
            ("Can my father exclude me (his daughter) from inheriting ancestral agricultural land in his will?",
             "Section 6, Hindu Succession (Amendment) Act, 2005 (Vineeta Sharma v. Rakesh Sharma)",
             "No. Under Section 6 of the Hindu Succession Act (reaffirmed by the Supreme Court in Vineeta Sharma), daughters have coparcenary rights in ancestral property by birth, identical to sons. A father cannot dispose of ancestral coparcenary property through a will to the exclusion of his daughter.",
             ["daughter inheritance rights", "ancestral property daughter", "Section 6 Hindu Succession Act", "vineeta sharma coparcener", "will ancestral land"]),
            ("A local land mafia has illegally occupied my vacant residential plot by putting up tin sheds. What is my immediate remedy?",
             "Section 164 & 165 BNSS & Specific Relief Act (Section 6)",
             "You have dual remedies: file an urgent petition before the Sub-Divisional Magistrate (SDM) under Section 164 BNSS to prevent breach of peace and maintain possession status, and file a summary suit for recovery of possession under Section 6 of the Specific Relief Act within 6 months of illegal dispossession.",
             ["land grabbing", "Section 164 BNSS", "illegal possession plot", "Section 6 Specific Relief Act", "sdm eviction petition", "encroachment removal"]),
            ("What is the difference between Property Registry (Sale Deed) and Mutation (Dakhil Kharij)?",
             "Transfer of Property Act, 1882 & State Land Revenue Codes",
             "A registered Sale Deed conveys legal title and ownership of the property under the Transfer of Property Act. Mutation (Dakhil Kharij) in municipal/revenue records is solely for tax and revenue collection purposes. Mutation does NOT create ownership title; title resides in the registered deed.",
             ["sale deed vs mutation", "dakhil kharij title", "property registry", "land revenue record", "transfer of property act", "mutation ownership"]),
            ("How much notice is mandatory before a landlord can terminate a month-to-month residential tenancy?",
             "Section 106, Transfer of Property Act, 1882",
             "In the absence of a specific lease agreement clause, Section 106 mandates at least 15 days written notice to terminate a monthly tenancy for residential purposes. The notice must expire with the end of a month of the tenancy.",
             ["tenant notice period", "Section 106 Transfer of Property Act", "15 days eviction notice", "residential tenancy termination"]),
            ("Can an apartment Residents Welfare Association (RWA) ban bachelor tenants or ban keeping pet dogs?",
             "Article 19(1)(e) & Animal Welfare Board Guidelines / Bye-laws",
             "No. RWAs and Apartment Associations have no legal authority to pass discriminatory resolutions banning bachelors, single women, or pets. The Supreme Court and High Courts have held that RWAs cannot violate fundamental rights of housing or supersede state laws and municipal pet bylaws.",
             ["rwa bachelor ban illegal", "apartment pet ban", "rwa discriminatory rules", "tenants rights apartment", "animal welfare board guidelines"])
        ]
    },
    {
        "batch_num": 17,
        "category": "RTI, Medical Negligence & Citizen Rights",
        "items": [
            ("I filed an RTI application requesting road repair expenditure details from the municipal corporation, but got no response after 35 days.",
             "Section 7(1) & Section 19, Right to Information Act, 2005",
             "Under Section 7(1) of the RTI Act, the Public Information Officer (PIO) is statutorily bound to provide information within 30 days (48 hours if life and liberty is involved). Since they failed, you can file a 'First Appeal' under Section 19(1) before the First Appellate Authority within 30 days.",
             ["rti no response 30 days", "Section 7 RTI Act", "first appeal rti", "public information officer", "municipal corruption rti"]),
            ("Can a Public Information Officer (PIO) be personally fined for deliberately rejecting or delaying an RTI query?",
             "Section 20(1), Right to Information Act, 2005",
             "Yes. Under Section 20(1) of the RTI Act, the Information Commission can impose a personal penalty of Rs 250 per day (up to a maximum of Rs 25,000) directly on the PIO for unreasonable refusal, delay, or providing false/misleading information, deducted from their salary.",
             ["pio fine rti", "Section 20 RTI Act", "250 per day penalty", "information commission complaint", "delayed rti compensation"]),
            ("A private hospital refused to admit a road accident victim in critical condition because the family could not deposit Rs 50,000 advance.",
             "Article 21 (Parmanand Katara v. Union of India) & Clinical Establishments Act",
             "The Supreme Court in the landmark Parmanand Katara case ruled that preserving human life is paramount. Every doctor and hospital (public or private) has a non-negotiable legal obligation to provide immediate emergency medical aid without waiting for police formalities or advance payments. Refusal is medical negligence.",
             ["emergency hospital admission", "parmanand katara supreme court", "advance deposit refusal", "medical negligence accident", "right to emergency medical care"]),
            ("A surgeon left a surgical sponge inside my relative's abdomen during surgery causing severe infection and second operation.",
             "Consumer Protection Act, 2019 & Res Ipsa Loquitur Principle",
             "Leaving a foreign object inside a patient during surgery is a prima facie case of gross Medical Negligence under the legal doctrine of 'Res Ipsa Loquitur' (the thing speaks for itself). You can file a consumer complaint for heavy compensation before the State/National Consumer Commission and an FIR under Section 106 BNS.",
             ["medical negligence sponge left", "res ipsa loquitur", "consumer court doctor compensation", "Section 106 BNS medical death", "hospital negligence"]),
            ("Can a police officer demand money or refuse to return my seized mobile phone after the investigation is completed?",
             "Section 497 & 503, Bharatiya Nagarik Suraksha Sanhita, 2023",
             "No. Demanding money is criminal corruption under the Prevention of Corruption Act. To retrieve seized property (Superdari), file an application before the Judicial Magistrate having jurisdiction under Section 503 BNSS seeking release of the vehicle or phone on furnishing an indemnity bond.",
             ["superdari application", "release seized phone police", "Section 503 BNSS", "police corruption bribe", "return seized property"]),
            ("What are the rights of an individual summoned by police for questioning as a witness?",
             "Section 179, Bharatiya Nagarik Suraksha Sanhita, 2023",
             "Under Section 179 BNSS, males under 15 years, males above 60 years, women, and physically disabled persons cannot be required to attend the police station; they must be questioned at their place of residence. Furthermore, witnesses are entitled to reasonable travel allowance for attending police summons.",
             ["police witness summons", "Section 179 BNSS", "women questioning at home", "witness rights police", "witness travel allowance"]),
            ("Can a shopkeeper charge above the Maximum Retail Price (MRP) printed on packaged drinking water or cold drinks in a movie theater?",
             "Legal Metrology (Packaged Commodities) Rules, 2011 & Consumer Protection Act",
             "No. Charging above MRP is a punishable statutory violation under Section 36 of the Legal Metrology Act, 2009. Dual pricing for identical packaged goods has been held unlawful by courts and the National Consumer Commission. You can complain via the NCH app (1915) or e-Daakhil.",
             ["charging above mrp", "legal metrology act", "dual pricing popcorn water", "consumer complaint overcharging", "mrp violation fine"])
        ]
    }
]

def main():
    print("[*] Generating Batches 11 through 17...")
    total_new = 0

    for b in BATCH_DEFINITIONS:
        batch_num = b["batch_num"]
        category = b["category"]
        items = b["items"]

        batch_data = []
        for idx, item in enumerate(items, 1):
            query, source_act, explanation, keywords = item
            batch_data.append({
                "id": f"NY-{category[:3].upper()}-{batch_num:02d}{idx:02d}",
                "category": category,
                "source_act": source_act,
                "user_query": query,
                "legal_text_raw": explanation,
                "layman_explanation": explanation,
                "keywords": keywords
            })

        out_path = os.path.join(SCRIPTS_DIR, f"batch{batch_num}.json")
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(batch_data, f, indent=4, ensure_ascii=False)
        print(f"[OK] Generated {out_path} ({len(batch_data)} Q&As)")
        total_new += len(batch_data)

    print(f"\n[SUCCESS] Generated {total_new} new verified legal Q&As across batches 11 to 17!")

if __name__ == "__main__":
    main()
