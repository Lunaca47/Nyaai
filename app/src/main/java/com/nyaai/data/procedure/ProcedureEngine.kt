package com.nyaai.data.procedure

import com.nyaai.ui.state.AppLanguage

class ProcedureEngine {

    companion object {
        private val SCENARIO_PHRASES = listOf(
            "what should i do", "what can i do", "how to proceed", "how do i proceed", "what is the process",
            "what is the procedure", "how do i file", "how can i file", "next steps",
            "further proceeding", "further proceedings", "legal action", "where to complain",
            "what are my options", "legal remedies", "how to handle this", "how to handle", "i need advice",
            "please help", "what happens if", "is it legal for", "is it illegal for", "what to do",
            "my landlord", "my tenant", "my employer", "my boss", "my company",
            "my husband", "my wife", "my neighbour", "my neighbor", "my brother",
            "someone hit", "hit my car", "hit and run", "cheque bounce", "cheque bounced",
            "bounced cheque", "cheque", "dishonored cheque", "dishonour", "insufficient funds",
            "salary not paid", "unpaid salary", "withholding salary",
            "refused to pay", "refused my fir", "refused to register fir", "police refused",
            "police not taking", "morphed photo", "blackmail", "blackmailing", "leaked photo",
            "cyber fraud", "upi scam", "upi fraud", "account hacked", "illegal detention",
            "arrested without", "without warrant", "domestic violence", "dowry harassment",
            "encroach", "encroachment", "land grabbing", "boundary wall", "threat to life",
            "threatening me", "medical negligence", "doctor negligence", "servant stole",
            "domestic help", "maid stole", "defamation", "defaming", "defamatory", "false fir", "fake fir", "fake case",
            "locked my flat", "locked out", "deposit not returning", "stole my", "stolen my", "stole gold",
            "fraud", "cheated", "scammed", "delayed salary", "custodial", "harassing me",
            "how to recover", "fake loan", "loud music", "frame me", "divorce", "custody",
            "refuse refund", "refused refund", "refusing refund", "not paying", "invoices",
            "online store", "bought a", "hospital doctor", "landlord", "tenant"
        )

        private val DETAILED_PHRASES = listOf(
            "detail", "detailed", "in detail", "explain in detail", "elaborate", "comprehensive",
            "full explanation", "deep dive", "step by step", "step-by-step", "complete guide",
            "thorough", "everything", "explain fully", "long answer", "explain each and everything",
            "in depth", "in-depth", "clearly explain all points", "all details", "exhaustive",
            "full procedure", "complete roadmap", "7 points", "detailed version", "expand",
            "break down completely", "all points", "full steps", "detailed explanation",
            "explain properly", "more details", "each and everything", "entire process",
            "complete legal brief", "full advisory", "long version"
        )
    }

    private val procedures: Map<String, LegalProcedure> = initProcedures()

    fun isScenarioQuery(query: String): Boolean {
        val q = query.lowercase().trim()
        if (SCENARIO_PHRASES.any { q.contains(it) }) return true
        if (q.contains("cheque") && (q.contains("bounce") || q.contains("bounced") || q.contains("dishonor") || q.contains("insufficient"))) return true
        if (q.contains("police") && (q.contains("fir") || q.contains("complaint") || q.contains("refuse") || q.contains("arrest"))) return true
        if (q.contains("salary") && (q.contains("unpaid") || q.contains("not paid") || q.contains("withheld") || q.contains("due") || q.contains("delay"))) return true
        if ((q.contains("consumer") || q.contains("defective") || q.contains("product")) && (q.contains("refund") || q.contains("damaged") || q.contains("broken") || q.contains("complaint") || q.contains("deficiency") || q.contains("flipkart") || q.contains("amazon"))) return true
        if (q.contains("domestic violence") || (q.contains("protection order") && (q.contains("husband") || q.contains("wife") || q.contains("dv")))) return true
        return false
    }

    fun isDetailedQuery(query: String): Boolean {
        val q = query.lowercase().trim()
        return DETAILED_PHRASES.any { q.contains(it) }
    }

    fun matchProcedure(query: String): LegalProcedure {
        val q = query.lowercase().trim()
        return when {
            (q.contains("landlord") || q.contains("tenant") || q.contains("flat") || q.contains("rent")) &&
            (q.contains("locked") || q.contains("belonging") || q.contains("evict") || q.contains("deposit") || q.contains("vacate") || q.contains("advance")) ->
                procedures.getValue("landlord_tenant")

            q.contains("cheque") && (q.contains("bounce") || q.contains("bounced") || q.contains("dishonor") || q.contains("returned") || q.contains("insufficient")) ->
                procedures.getValue("cheque_bounce")

            q.contains("hit and run") || q.contains("hit my car") || q.contains("hit my bike") || (q.contains("accident") && (q.contains("car") || q.contains("vehicle") || q.contains("speeding") || q.contains("rash") || q.contains("ran away"))) ->
                procedures.getValue("hit_and_run")

            q.contains("police") && (q.contains("refused") || q.contains("not taking") || q.contains("not registering") || q.contains("rejected")) && (q.contains("fir") || q.contains("complaint")) ->
                procedures.getValue("police_fir_refusal")

            (q.contains("cyber") || q.contains("online") || q.contains("upi") || q.contains("phishing") || q.contains("otp") || q.contains("bank account")) &&
            (q.contains("fraud") || q.contains("scam") || q.contains("debited") || q.contains("money") || q.contains("hacked") || q.contains("stolen") || q.contains("cheated")) ->
                procedures.getValue("cyber_fraud")

            (q.contains("consumer") || q.contains("defective") || q.contains("warranty") || q.contains("e-commerce") || q.contains("amazon") || q.contains("flipkart") || q.contains("goods")) &&
            (q.contains("refund") || q.contains("deficiency") || q.contains("damaged") || q.contains("complaint") || q.contains("service") || q.contains("bought") || q.contains("order") || q.contains("product") || q.contains("case") || q.contains("forum") || q.contains("e-daakhil") || q.contains("cpa")) ->
                procedures.getValue("consumer_complaint")

            (q.contains("salary") || q.contains("wages") || q.contains("employer") || q.contains("company") || q.contains("boss") || q.contains("terminated") || q.contains("gratuity")) &&
            (q.contains("not paid") || q.contains("unpaid") || q.contains("withheld") || q.contains("delay") || q.contains("fire") || q.contains("fired") || q.contains("wrongful") || q.contains("clearance") || q.contains("fnf")) ->
                procedures.getValue("employment_dispute")

            (q.contains("domestic violence") || q.contains("wife") || q.contains("in-laws") || q.contains("dowry") || q.contains("protection order") || (q.contains("husband") && (q.contains("abuse") || q.contains("assault") || q.contains("harass") || q.contains("beaten")))) ->
                procedures.getValue("domestic_violence")

            else ->
                procedures.getValue("general_dispute")
        }
    }

    fun buildProcedureAnswer(query: String, @Suppress("UNUSED_PARAMETER") language: AppLanguage = AppLanguage.ENGLISH): Pair<String, Double> {
        val isDetailed = isDetailedQuery(query)
        val proc = matchProcedure(query)

        val text = if (isDetailed) {
            formatDetailedBrief(proc)
        } else {
            formatStandardRoadmap(proc)
        }

        val confidence = if (isDetailed) 0.99 else 0.98
        return text to confidence
    }

    fun formatDetailedBrief(proc: LegalProcedure): String {
        val sb = StringBuilder()
        sb.appendLine("⚖️ **SENIOR ADVOCATE COMPREHENSIVE LEGAL BRIEF (8+ POINTS)**")
        sb.appendLine("**Matter:** Comprehensive Legal Evaluation & Multi-Stage Proceeding Brief")
        sb.appendLine("**Statutory Codex:** BNS 2023 • BNSS 2023 • BSA 2023 • CPC 1908 • Special Enactments")
        sb.appendLine()

        proc.detailedPoints.forEachIndexed { index, point ->
            sb.appendLine("${index + 1}. $point")
            if (index < proc.detailedPoints.size - 1) {
                sb.appendLine()
            }
        }

        sb.appendLine()
        sb.append("⚡ Note: Exhaustive 9-point Senior Advocate brief grounded in BNS, BNSS, BSA, and Special Acts.")
        return sb.toString().trim()
    }

    fun formatStandardRoadmap(proc: LegalProcedure): String {
        val sb = StringBuilder()
        sb.appendLine("⚖️ **SENIOR ADVOCATE LEGAL ADVISORY & PROCEEDING ROADMAP**")
        sb.appendLine("**Matter:** ${proc.title}")
        sb.appendLine("**Primary Statutes:** ${proc.primaryStatutes}")
        sb.appendLine()
        sb.appendLine("⚖️ **NATURE OF OFFENSE & CLASSIFICATION:**")
        sb.appendLine("• **Classification:** ${proc.offenseClassification}")
        sb.appendLine("• **Cognizable Status:** ${proc.cognizableStatus}")
        sb.appendLine()

        proc.phases.forEach { phase ->
            val phaseHeader = when (phase.phaseNumber) {
                1 -> "🚨 **PHASE 1: ${phase.title.uppercase()}${if (phase.timeframe.isNotBlank()) " (${phase.timeframe})" else ""}:**"
                2 -> "📜 **PHASE 2: ${phase.title.uppercase()}${if (phase.timeframe.isNotBlank()) " (${phase.timeframe})" else ""}:**"
                3 -> "🏛️ **PHASE 3: ${phase.title.uppercase()}${if (phase.timeframe.isNotBlank()) " (${phase.timeframe})" else ""}:**"
                else -> "📋 **PHASE ${phase.phaseNumber}: ${phase.title.uppercase()}:**"
            }
            sb.appendLine(phaseHeader)
            phase.actions.forEach { action ->
                sb.appendLine("• $action")
            }
            sb.appendLine()
        }

        if (proc.strategicAdvice.isNotEmpty()) {
            sb.appendLine("🛡️ **ADVOCATE'S STRATEGIC ADVICE & IMPORTANT CAUTIONS:**")
            proc.strategicAdvice.forEach { advice ->
                sb.appendLine("• $advice")
            }
        }

        sb.appendLine()
        sb.append("⚡ Note: Senior Advocate procedural roadmap grounded in BNS, BNSS, BSA, and Special Acts.")
        return sb.toString().trim()
    }

    private fun initProcedures(): Map<String, LegalProcedure> {
        val landlord = LegalProcedure(
            id = "landlord_tenant",
            domain = "Property & Tenancy Law",
            title = "Unlawful Eviction, Flat Lockout & Belongings Seizure",
            primaryStatutes = "Transfer of Property Act 1882 (Sec 106) • BNS 2023 (Sec 329, 316) • Order 39 CPC",
            offenseClassification = "Civil Dispossession + Cognizable Criminal Trespass (Sec 329 BNS) & Criminal Breach of Trust (Sec 316 BNS).",
            cognizableStatus = "Cognizable • Bailable • Non-Compoundable without Magistrate permission.",
            phases = listOf(
                ProcedurePhase(
                    phaseNumber = 1,
                    title = "Immediate Steps & Evidence Preservation",
                    timeframe = "First 24-48 Hours",
                    actions = listOf(
                        "**Do NOT break locks yourself:** Forcible entry allows the landlord to counter-allege housebreaking.",
                        "**Photograph & Video Record:** Capture high-resolution timestamped photos/video of padlocks and posted notices.",
                        "**Preserve Tenancy Communications:** Archive WhatsApp chats, rent bank statements, and agreement copy under Section 63 BSA 2023.",
                        "**Dial 112 from the Spot:** Generates an official Police Control Room (PCR) dispatch log verifying physical dispossession."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 2,
                    title = "Formal Legal Notice & Police / Statutory Recourse",
                    timeframe = "",
                    actions = listOf(
                        "**Lodge Police Complaint / Zero FIR:** Visit jurisdictional police under Section 173 BNSS 2023 for Criminal Trespass (Sec 329 BNS) & Breach of Trust (Sec 316 BNS).",
                        "**If Police Refuse (Crucial Advocate Step):** Send signed complaint via Registered Speed Post to Superintendent of Police (SP) / DCP under Section 175(3) BNSS 2023.",
                        "**Advocate Legal Demand Notice:** Dispatch a formal 7-Day Demand Notice demanding keys, return of belongings, and damages."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 3,
                    title = "Judicial Proceedings, Petitions & Reliefs in Court",
                    timeframe = "",
                    actions = listOf(
                        "**Section 175(4) BNSS Application to Magistrate:** Move Judicial Magistrate to order FIR and search/recovery of personal belongings.",
                        "**Summary Suit u/s 6 Specific Relief Act 1963:** File civil suit for restoration of possession without title contest.",
                        "**Order 39 Rules 1 & 2 CPC Temporary Mandatory Injunction:** Move for ex-parte order directing landlord to unlock premises under Court Commissioner supervision within 24 hours.",
                        "**Claim Damages:** Pray for compensation for hotel stay, replacement of essential items, and mental agony."
                    )
                )
            ),
            strategicAdvice = listOf(
                "*Precedent:* Supreme Court in 'Bishandas v. State of Punjab' ruled landlords cannot forcibly dispossess without court eviction decrees.",
                "*Limitation:* Suit u/s 6 Specific Relief Act must be filed within 6 months of dispossession.",
                "Preserve Speed Post tracking receipts as indisputable proof in court."
            ),
            detailedPoints = listOf(
                "⚖️ **NATURE OF OFFENSE & SUBSTANTIVE JURISDICTION:** Civil dispossession coupled with cognizable criminal offenses under Section 329 BNS (Criminal Trespass), Section 316 BNS (Criminal Breach of Trust for seizing belongings), and Section 303 BNS (Theft). Under Section 106 of the Transfer of Property Act 1882, tenancy can only be determined through valid statutory notice; physical lockout without a court eviction decree is strictly illegal.",
                "🚨 **FIRST 24–48 HOUR EMERGENCY PROTOCOL (GOLDEN HOURS):** Do NOT take the law into your own hands or forcibly break padlocks, as the landlord could file counter-allegations of housebreaking under Section 331 BNS. Immediately dial 112 while standing outside the locked premises to generate an official Police Control Room (PCR) computer-aided dispatch record establishing timestamp and location.",
                "📱 **EVIDENTIARY AUDIT & DIGITAL FORENSIC PRESERVATION (SEC 63 BSA 2023):** Capture high-resolution timestamped photographs and video of padlocks, chains, and notices pasted on the door. Archive all rent bank transfers, security deposit receipts, and WhatsApp communications with the landlord, supported by electronic certificate under Section 63 of Bharatiya Sakshya Adhiniyam 2023.",
                "🚓 **POLICE STATION PROTOCOL, GENERAL DIARY & ZERO FIR (SEC 173 BNSS):** Submit two typed, signed copies of your criminal complaint to the jurisdictional Station House Officer (SHO). Insist on obtaining a receiving stamp and General Diary (GD) entry number. Under Section 173 BNSS 2023, police are statutorily mandated to record cognizable information; geographical jurisdiction cannot be cited to refuse an FIR.",
                "📜 **MANDATORY STATUTORY ESCALATION AGAINST INACTION (SEC 175(3) BNSS TO SP):** If the local police station dismisses the matter as a 'civil dispute', immediately invoke Section 175(3) BNSS. Send a signed copy of the complaint via Registered Speed Post directly to the Superintendent of Police (SP) or Deputy Commissioner of Police (DCP). Preserve the India Post postal receipt and tracking delivery slip.",
                "✉️ **FORMAL ADVOCATE LEGAL DEMAND NOTICE:** Have an advocate issue a formal 7-Day Legal Notice via Speed Post and Email, putting the landlord on notice to hand over duplicate keys, restore peaceful possession, and return all seized goods, failing which civil and criminal proceedings will follow at their sole risk and costs.",
                "🏛️ **JUDICIAL RECOURSE & MAGISTERIAL PETITIONS (SEC 175(4) / 176 BNSS):** File an application under Section 175(4) BNSS before the Judicial Magistrate First Class (JMFC) praying for judicial directions ordering the police to register an FIR and seeking a search warrant for the immediate recovery and restitution of personal belongings.",
                "🛡️ **URGENT CIVIL INJUNCTION (ORDER 39 CPC) & SECTION 6 SPECIFIC RELIEF SUIT:** File a summary suit under Section 6 of the Specific Relief Act 1963 for restoration of possession without having to prove title. Simultaneously move an interlocutory application under Order 39 Rules 1 & 2 CPC for an ex-parte temporary mandatory injunction directing the landlord to open the flat under Court Commissioner supervision within 24 hours.",
                "⏳ **STRATEGIC ADVOCATE SAFEGUARDS & LIMITATION CLOCK:** In 'Bishandas v. State of Punjab', the Supreme Court established that even an unauthorized occupant cannot be forcibly dispossessed without due process of law. Note: A suit under Section 6 Specific Relief Act carries a strict 6-MONTH limitation period from the date of dispossession."
            )
        )

        val cheque = LegalProcedure(
            id = "cheque_bounce",
            domain = "Commercial & Banking Law",
            title = "Dishonour of Cheque & Criminal Prosecution",
            primaryStatutes = "Negotiable Instruments Act 1881 (Sec 138, 142) • Section 143A • Section 223 BNSS",
            offenseClassification = "Quasi-Criminal Offense punishable with up to 2 years imprisonment or fine up to twice cheque amount.",
            cognizableStatus = "Non-Cognizable • Bailable • Compoundable at any stage.",
            phases = listOf(
                ProcedurePhase(
                    phaseNumber = 1,
                    title = "Immediate Steps & Evidence Preservation",
                    timeframe = "",
                    actions = listOf(
                        "**Bank Return Memo:** Collect original cheque with memo stating 'Funds Insufficient' or 'Account Closed'.",
                        "**30-Day Notice Clock:** Notice MUST be dispatched within 30 DAYS of receiving bank memo.",
                        "**Enforceable Debt Proof:** Collect contracts, invoices, ledger statements, or delivery receipts showing valid debt."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 2,
                    title = "Formal Legal Notice & Statutory Demand",
                    timeframe = "",
                    actions = listOf(
                        "**15-Day Statutory Legal Demand Notice:** Serve formal notice u/s 138(b) NI Act demanding payment within 15 DAYS of receipt.",
                        "**Speed Post with Tracking:** Dispatch via Registered Speed Post (RPAD) and email; preserve delivery tracking report.",
                        "**Cause of Action:** Legally arises on the 16th day if drawer fails to pay."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 3,
                    title = "Judicial Proceedings in Court",
                    timeframe = "",
                    actions = listOf(
                        "**File Criminal Complaint within 30 Days:** File complaint before Judicial Magistrate u/s 142(1)(b) NI Act within 30 days from expiry of notice.",
                        "**Pre-Summoning Affidavit u/s 145 NI Act:** Complainant tenders evidence on affidavit for issuance of summons/warrant.",
                        "**20% Interim Compensation u/s 143A:** Move court for interim deposit of up to 20% of cheque amount.",
                        "**Company Offense u/s 141:** Implead all Directors/Partners in charge of day-to-day business."
                    )
                )
            ),
            strategicAdvice = listOf(
                "*Limitation Warning:* Missing the 30-day notice or 30-day filing window is fatal to prosecution.",
                "*Presumption u/s 139:* Law presumes cheque was issued for debt; burden of proof is entirely on the accused.",
                "Keep original cheque in protective plastic cover; do not staple or overwrite."
            ),
            detailedPoints = listOf(
                "⚖️ **NATURE OF OFFENSE & QUASI-CRIMINAL JURISDICTION:** Dishonour of cheque is a quasi-criminal statutory offense under Section 138 of the Negotiable Instruments Act 1881, punishable with imprisonment up to 2 years, or fine extending up to twice the cheque amount, or both.",
                "🚨 **BANK RETURN MEMO VERIFICATION & 30-DAY STATUTORY CLOCK:** Obtain the original return memo from your bank stating reasons such as 'Funds Insufficient' or 'Account Closed'. The strict 30-DAY limitation period for issuing the statutory demand notice starts ticking from the exact date on which you received the memo.",
                "📱 **EVIDENTIARY AUDIT OF ENFORCEABLE DEBT:** Assemble all underlying commercial invoices, agreements, purchase orders, delivery challans, and ledger balance sheets proving that the cheque was issued in discharge of a legally enforceable debt or liability, invoking the statutory presumption under Section 139 NI Act.",
                "✉️ **DRAFTING & SERVING 15-DAY STATUTORY LEGAL DEMAND NOTICE:** Issue a formal notice under Section 138(b) NI Act demanding payment of the exact cheque amount within 15 DAYS of receipt. The notice must be dispatched via Registered Speed Post (RPAD) and email to ensure traceable service.",
                "⏳ **CAUSE OF ACTION ACCRUAL (DAY 16):** The cause of action to prosecute the drawer does not arise until the 15-day notice period expires without payment. If the drawer fails to pay within 15 days, the cause of action legally accrues on the 16th day.",
                "🏛️ **FILING CRIMINAL COMPLAINT U/S 142 NI ACT (30-DAY WINDOW):** File a formal complaint under Section 142(1)(b) NI Act before the competent Judicial Magistrate within 30 DAYS from the date the cause of action accrued. Missing this window requires filing a Section 142(1)(b) proviso condonation of delay application.",
                "📜 **PRE-SUMMONING EVIDENCE ON AFFIDAVIT (SEC 145 NI ACT):** Complainant tenders their pre-summoning evidence on affidavit under Section 145 NI Act, expediting the issuance of court summons or bailable warrants against the accused without prolonged oral examination.",
                "💰 **20% INTERIM COMPENSATION APPLICATION (SEC 143A NI ACT):** File an application under Section 143A NI Act praying for an order directing the accused to deposit up to 20% of the cheque amount as interim compensation within 60 days.",
                "👥 **CORPORATE VICARIOUS LIABILITY (SEC 141 NI ACT):** If the cheque was issued by a company or LLP, implead all Directors and managing partners in charge of day-to-day business under Section 141 NI Act, demonstrating joint and several criminal liability."
            )
        )

        val hitAndRun = LegalProcedure(
            id = "hit_and_run",
            domain = "Motor Accidents & Criminal Law",
            title = "Hit-and-Run Motor Vehicle Accident & Compensation Claim",
            primaryStatutes = "Bharatiya Nyaya Sanhita (BNS 2023) Sec 281, 125, 106 • Motor Vehicles Act 1988 Sec 161, 166",
            offenseClassification = "Cognizable Criminal Offense (Rash/Negligent Driving) + Statutory MACT Claim.",
            cognizableStatus = "Cognizable • Bailable (Sec 281/125 BNS) • Sec 106(2) hit-and-run carries up to 10 years imprisonment.",
            phases = listOf(
                ProcedurePhase(
                    phaseNumber = 1,
                    title = "Immediate Steps & Evidence Preservation",
                    timeframe = "",
                    actions = listOf(
                        "**Hospital Medico-Legal Certificate (MLC):** Ensure treating doctor records the accident history in hospital casualty register.",
                        "**Vehicle Details & Scene Photos:** Note vehicle registration number, make, color; photograph vehicle damage and skid marks.",
                        "**Retrieve CCTV:** Request nearby shops, fuel stations, and traffic signals to preserve footage under Section 63 BSA 2023.",
                        "**Witness Contacts:** Note phone numbers of bystanders who witnessed the collision."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 2,
                    title = "Formal Legal Notice & Police / Statutory Recourse",
                    timeframe = "",
                    actions = listOf(
                        "**Mandatory FIR Registration:** File written complaint under Section 281 & 106/125 BNS at jurisdictional police station.",
                        "**Section 175(3) BNSS Escalation:** If police delay or try to compromise, send written complaint to SP/DCP.",
                        "**Certified Police Documents:** Obtain certified copies of FIR, Spot Panchnama, and Motor Vehicle Inspector (MVI) inspection report."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 3,
                    title = "Judicial Proceedings & Reliefs in Court",
                    timeframe = "",
                    actions = listOf(
                        "**File MACT Claim under Section 166 MV Act:** File before Motor Accident Claims Tribunal for medical costs, vehicle repair, and loss of earning capacity.",
                        "**Solatium Scheme for Unidentified Hit-and-Run:** If vehicle remains untraceable, claim statutory compensation u/s 161 MV Act via SDM office."
                    )
                )
            ),
            strategicAdvice = listOf(
                "Do NOT sign compromise letters from the driver or insurance surveyor without consulting an advocate.",
                "*Limitation:* File MACT petition within 6 months from accident date.",
                "Preserve all medical bills, pharmacy receipts, and employer salary loss statements."
            ),
            detailedPoints = listOf(
                "⚖️ **NATURE OF OFFENSE & STATUTORY CLASSIFICATION:** Offenses under Section 281 BNS (Rash and Negligent Driving), Section 125 BNS (Endangering Life/Personal Safety), and Section 106(2) BNS (Hit-and-run failing to report to police/Magistrate, carrying up to 10 years imprisonment), alongside civil claim remedies under the Motor Vehicles Act 1988.",
                "🚨 **HOSPITALIZATION & MEDICO-LEGAL CERTIFICATE (MLC):** When admitted to hospital, ensure the casualty medical officer enters the case as a Medico-Legal Case (MLC) citing 'RTA' (Road Traffic Accident). The MLC number is the foundational document for both police FIR and tribunal compensation.",
                "📱 **FORENSIC EVIDENCE & SCENE PRESERVATION (SEC 63 BSA):** Photograph vehicle damage, tyre skid marks, paint transfers, and road signage. Immediately issue written requests to surrounding commercial establishments and traffic police to preserve CCTV footage under Section 63 BSA 2023 before the 7-day DVR overwriting cycle.",
                "🚓 **MANDATORY FIR REGISTRATION U/S 173 BNSS:** Submit a written complaint specifying vehicle registration number, make, color, direction of escape, and witness statements. Secure a certified stamped copy of the FIR, Spot Panchnama, and Motor Vehicle Inspector (MVI) mechanical inspection report.",
                "📜 **POLICE ESCALATION & NODAL INVESTIGATION U/S 175(3) BNSS:** If the local police delay tracing the vehicle or registering the FIR, dispatch written representation to the DCP/SP Traffic and SP Police under Section 175(3) BNSS, requesting automated ANPR (Automatic Number Plate Recognition) toll camera tracing.",
                "🛡️ **SOLATIUM SCHEME APPLICATION FOR UNIDENTIFIED VEHICLES (SEC 161 MV ACT):** If the offending vehicle remains untraceable, submit a claim under the Central Government Solatium Scheme via the Sub-Divisional Magistrate (SDM) / Claims Enquiry Officer for statutory compensation (₹2 Lakhs for death, ₹50,000 for grievous hurt).",
                "🏛️ **MACT CLAIM PETITION U/S 166 MOTOR VEHICLES ACT:** File a comprehensive claim petition before the Motor Accident Claims Tribunal (MACT) having territorial jurisdiction over the accident site or your residence, impleading the driver, owner, and insurer within the 6-month statutory limitation period.",
                "💰 **COMPUTATION OF MULTI-HEAD DAMAGES:** Pray for compensation across medical expenses, future treatment costs, vehicle repair depreciation, permanent disability loss of earnings using the structured multiplier system, pain and suffering, and loss of amenities.",
                "⏳ **ADVOCATE PRECAUTIONS & INSURANCE SAFEGUARDS:** Never sign blanket discharge vouchers from insurance surveyors without advocate review. Preserve every pharmacy invoice, hospital discharge summary, employer leave certificate, and salary payslip."
            )
        )

        val policeRefusal = LegalProcedure(
            id = "police_fir_refusal",
            domain = "Criminal Procedure & Constitutional Rights",
            title = "Statutory Remedies against Police Refusal to Lodge FIR",
            primaryStatutes = "Bharatiya Nagarik Suraksha Sanhita (BNSS 2023) Sec 173, 175(3), 175(4), 176 • BNS Sec 199",
            offenseClassification = "Dereliction of Public Duty by Police & Infringement of Statutory Rights.",
            cognizableStatus = "Mandatory duty to register FIR for cognizable offenses under Lalita Kumari v. Govt of UP.",
            phases = listOf(
                ProcedurePhase(
                    phaseNumber = 1,
                    title = "Immediate Steps at the Police Station",
                    timeframe = "",
                    actions = listOf(
                        "**Written Complaint with Receiving Stamp:** Carry two copies; insist on station seal and General Diary (GD) entry number.",
                        "**Call 112 from the Station:** Creates an official computer-aided dispatch log verifying you attended the station.",
                        "**Zero FIR u/s 173 BNSS:** If out-of-jurisdiction is cited, demand registration of a Zero FIR for transfer."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 2,
                    title = "Statutory Escalation (Mandatory Advocate Step)",
                    timeframe = "",
                    actions = listOf(
                        "**Section 175(3) BNSS Representation to SP:** Send your signed complaint via Registered Speed Post directly to Superintendent of Police (SP) / DCP.",
                        "**Preserve Speed Post Consignment Receipt:** Mandatory prerequisite for approaching Magistrate.",
                        "**Section 199 BNS Action:** Section 199 BNS punishes with up to 2 years jail any public servant who willfully disobeys direction to record cognizable information."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 3,
                    title = "Judicial Proceedings before Magistrate & High Court",
                    timeframe = "",
                    actions = listOf(
                        "**Section 175(4) / 176 BNSS Application to Judicial Magistrate:** File application before JMFC praying for judicial orders directing police to lodge FIR.",
                        "**Article 226 Writ of Mandamus:** Approach High Court if offense is severe and local police machinery is compromised."
                    )
                )
            ),
            strategicAdvice = listOf(
                "*Precedent:* Supreme Court in 'Lalita Kumari' held FIR registration is mandatory if cognizable crime is disclosed.",
                "Keep dated chronological binder of all complaint copies and India Post consignment slips."
            ),
            detailedPoints = listOf(
                "⚖️ **NATURE OF POLICE DERELICTION & STATUTORY MANDATE:** Under Section 173 BNSS 2023 and the Constitution of India, police officers have a mandatory statutory duty to record information disclosing a cognizable offense. Deliberate refusal to register an FIR constitutes dereliction of public duty punishable under Section 199 BNS with up to 2 years imprisonment.",
                "🚨 **POLICE STATION PROTOCOL & GENERAL DIARY (GD) DEMAND:** Always carry two typed, signed copies of your complaint. Insist that the Station Duty Officer place the official police station seal and date-time stamp on your receiving copy and note the General Diary (GD) entry number.",
                "📞 **DIALING 112 FROM POLICE STATION PREMISES:** If the duty officer verbally refuses or turns you away, dial 112 while standing inside or directly outside the police station. State that you are at the station and the officer is refusing to record your cognizable complaint; this creates a computer-aided dispatch (CAD) audio log that cannot be altered.",
                "🚓 **RIGHT TO ZERO FIR U/S 173 BNSS:** If the officer claims the incident occurred outside their police station limits, demand the registration of a Zero FIR under Section 173 BNSS 2023. By law, a Zero FIR must be registered and subsequently transferred to the jurisdictional police station.",
                "📜 **MANDATORY STATUTORY ESCALATION TO SP U/S 175(3) BNSS:** Send your complete signed complaint via Registered Speed Post directly to the Superintendent of Police (SP) or Deputy Commissioner of Police (DCP) under Section 175(3) BNSS. Speed Post provides indisputable legal proof of service.",
                "✉️ **PRESERVING POSTAL CONSIGNMENT EVIDENCE:** Retain the India Post consignment receipt and download the online delivery tracking report showing the date and time the SP's office received the complaint. This is a mandatory condition precedent for approaching the judiciary.",
                "🏛️ **APPLICATION TO JUDICIAL MAGISTRATE U/S 175(4) / 176 BNSS:** When the SP fails to direct an investigation within a reasonable time, file an application before the Judicial Magistrate First Class (JMFC) under Section 175(4) BNSS praying for judicial orders directing the police to lodge an FIR and submit a compliance report.",
                "⚖️ **ARTICLE 226 WRIT OF MANDAMUS IN HIGH COURT:** In severe cases involving grave human rights violations, police complicity, or political interference, file a Writ Petition (Criminal) under Article 226 of the Constitution before the High Court seeking a Writ of Mandamus commanding registration of the FIR and an independent SIT probe.",
                "⏳ **LANDMARK PRECEDENTS & ADVOCATE CAUTIONS:** The Supreme Court 5-judge Constitution Bench in 'Lalita Kumari v. Govt of UP' held that FIR registration is mandatory if information discloses a cognizable offense. Maintain a chronological binder of all stamped papers and speed post slips."
            )
        )

        val cyber = LegalProcedure(
            id = "cyber_fraud",
            domain = "Cyber Crime & Financial Protection",
            title = "Cyber Financial Fraud, UPI Scam & Fund Freezing Recourse",
            primaryStatutes = "Information Technology Act 2000 (Sec 43, 66D) • BNS 2023 Sec 318(4) • RBI Master Direction 2017",
            offenseClassification = "Cognizable Cyber Crime + Statutory Bank Zero-Liability Protection.",
            cognizableStatus = "Cognizable • Non-Bailable depending on quantum.",
            phases = listOf(
                ProcedurePhase(
                    phaseNumber = 1,
                    title = "Immediate Steps & Golden Hours Actions",
                    timeframe = "First 2-4 Hours",
                    actions = listOf(
                        "**Dial 1930 Cyber Fraud Helpline:** Call 1930 immediately or log on to cybercrime.gov.in to trigger an automated inter-bank lien to freeze funds.",
                        "**Notify Bank within 72 Hours:** Under RBI circular on Customer Protection, reporting within 3 days grants ZERO customer liability.",
                        "**Preserve Digital Evidence:** Screenshot UPI transaction IDs, reference numbers, SMS alerts, and bank statements under Section 63 BSA 2023.",
                        "**Block Cards & Reset Net Banking:** Freeze compromised debit/credit cards and UPI credentials immediately."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 2,
                    title = "Formal Notice & Police Recourse",
                    timeframe = "",
                    actions = listOf(
                        "**Cyber Crime FIR:** Register FIR under Section 66D IT Act & Section 318(4) BNS.",
                        "**Section 175(3) BNSS Escalation:** If local police refuse, escalate directly to Cyber Crime Nodal Officer / SP.",
                        "**RBI Banking Ombudsman:** File complaint on cms.rbi.org.in if bank fails to reverse fraudulent debits within 30 days."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 3,
                    title = "Judicial Proceedings & Fund Recovery",
                    timeframe = "",
                    actions = listOf(
                        "**Section 503 BNSS De-freezing Application:** Move Magistrate for release of frozen funds directly back into your account.",
                        "**Adjudicating Officer u/s 46 IT Act:** Claim compensation from State IT Secretary for financial losses.",
                        "**Consumer Court:** File for deficiency in banking service if bank failed to enforce security standards."
                    )
                )
            ),
            strategicAdvice = listOf(
                "Do NOT delete WhatsApp chats or SMS with fraudsters; export and backup chat history.",
                "The first 2 hours are the most critical for account freezing before money is laundered."
            ),
            detailedPoints = listOf(
                "⚖️ **NATURE OF CYBER OFFENSE & STATUTORY FRAMEWORK:** Financial cyber fraud constitutes offenses under Section 66D of the Information Technology Act 2000 (Cheating by personation using computer resource) and Section 318(4) BNS (Cheating and dishonestly inducing delivery of property), along with RBI Master Directions on Customer Protection.",
                "🚨 **GOLDEN HOURS EMERGENCY ACTION (DIAL 1930):** Immediately call the National Cyber Crime Helpline at 1930 or submit an incident report at cybercrime.gov.in. Within the first 2-4 hours, this triggers the Citizen Financial Cyber Fraud Reporting and Management System (CFCFRMS) to automatically freeze funds across intermediate payment gateways.",
                "🏦 **FORMAL BANK NOTICE WITHIN 72 HOURS (ZERO CUSTOMER LIABILITY):** Submit a written complaint to your home bank branch within 72 hours. Under RBI Circular DBR.No.Leg.BC.78/09.07.005/2017-18, where fraud is reported within 3 days without customer negligence, the customer's liability is ZERO, and the bank must shadow-credit the amount within 10 working days.",
                "📱 **DIGITAL FORENSIC EVIDENCE PRESERVATION (SEC 63 BSA):** Capture and preserve unedited screenshots of UPI transaction IDs, UTR numbers, debit SMS alerts, fraudulent payment links, APK files, and WhatsApp conversations, accompanied by Section 63 BSA 2023 metadata.",
                "🚓 **CYBER CRIME POLICE STATION FIR (SEC 173 BNSS):** Register a formal Cyber FIR under Section 173 BNSS 2023. Specify the beneficiary bank IFSC, account number, or UPI handle to facilitate immediate Section 102/106 BNSS seizure notices by the investigating officer.",
                "📜 **ESCALATION TO DISTRICT CYBER CELL & SP U/S 175(3) BNSS:** If the local police delay issuing freeze notices to the intermediary banks, escalate in writing to the District Cyber Crime Nodal Officer and SP under Section 175(3) BNSS.",
                "🏛️ **MAGISTRATE DE-FREEZING APPLICATION (SEC 503 BNSS):** Once the beneficiary bank confirms that the fraudulent money has been placed on hold, move an application under Section 503 BNSS before the Judicial Magistrate praying for release and transfer of the seized money back into your account on indemnity bond.",
                "⚖️ **BANKING OMBUDSMAN & IT ADJUDICATING OFFICER (SEC 46 IT ACT):** If your bank fails to adhere to RBI zero-liability norms within 30 days, file an online complaint with the RBI Banking Ombudsman (cms.rbi.org.in). Additionally, claim compensation before the State IT Secretary (Adjudicating Officer u/s 46 IT Act).",
                "⏳ **ADVOCATE CAUTIONS & CYBER HYGIENE:** Never communicate further with the fraudsters or click 'refund links'. Do not format your phone until digital evidence has been extracted and certified."
            )
        )

        val general = LegalProcedure(
            id = "general_dispute",
            domain = "Indian Civil & Criminal Jurisprudence",
            title = "Legal Evaluation & Procedural Roadmap for Client Inquiry",
            primaryStatutes = "Bharatiya Nyaya Sanhita (BNS 2023) • Bharatiya Nagarik Suraksha Sanhita (BNSS 2023) • Special Enactments",
            offenseClassification = "Civil/Criminal Infringement of Codified Rights under Indian Jurisprudence.",
            cognizableStatus = "Subject to Station House Officer General Diary assessment.",
            phases = listOf(
                ProcedurePhase(
                    phaseNumber = 1,
                    title = "Immediate Steps & Evidence Preservation",
                    timeframe = "First 24-48 Hours",
                    actions = listOf(
                        "**Avoid Self-Help:** Do not take the law into your own hands or engage in verbal/physical retaliation.",
                        "**Preserve Evidence:** Document all contemporaneous evidence (WhatsApp, audio recordings, invoices, photographs) under Section 63 BSA 2023.",
                        "**Official Helpline Logging:** Call 112 or relevant government helpline to generate an official police event log."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 2,
                    title = "Formal Legal Notice & Statutory Police Recourse",
                    timeframe = "",
                    actions = listOf(
                        "**Advocate Legal Demand Notice:** Serve formal 15-Day Legal Demand Notice via Registered Speed Post detailing statutory violations.",
                        "**Police Complaint / Zero FIR:** Submit written complaint under Section 173 BNSS 2023; obtain stamped receiving copy.",
                        "**Section 175(3) BNSS Escalation to SP:** If local police refuse, escalate in writing to the Superintendent of Police."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 3,
                    title = "Judicial Proceedings, Petitions & Court Reliefs",
                    timeframe = "",
                    actions = listOf(
                        "**Section 175(4) / 176 BNSS Application to Magistrate:** Move Judicial Magistrate for orders directing investigation if police fail to act.",
                        "**Civil Injunction / Specific Relief / Consumer Forum:** File in competent forum for injunction, recovery, or compensation.",
                        "**Claim Liquidated Damages:** Pray for financial restitution and compensation for mental agony and litigation costs."
                    )
                )
            ),
            strategicAdvice = listOf(
                "Always verify statutory limitation periods before instituting proceedings.",
                "Maintain India Post Speed Post receipts and delivery tracking consignment notes as primary evidence.",
                "Consult licensed Bar Council advocate for customized litigation strategy."
            ),
            detailedPoints = listOf(
                "⚖️ **SUBSTANTIVE LEGAL NATURE & CODIFIED CLASSIFICATION:** Legal evaluation of your situation under Bharatiya Nyaya Sanhita (BNS 2023), Bharatiya Nagarik Suraksha Sanhita (BNSS 2023), and relevant Special Enactments. The matter is classified into civil or criminal remedies, assessing cognizable status and on whom the statutory burden of proof lies.",
                "🚨 **FIRST 24–48 HOUR EMERGENCY PROTOCOL & GOLDEN HOURS:** Do not take the law into your own hands or resort to extrajudicial self-help. Immediately dial 112 or relevant statutory helplines to generate an official computer-aided dispatch log verifying the date, time, and nature of the incident.",
                "📱 **EVIDENTIARY AUDIT & FORENSIC PRESERVATION (BSA 2023 SEC 63):** Compile all primary documentary evidence, contemporaneous audio/video recordings, invoices, and digital chats. Ensure all digital evidence is preserved with complete metadata and certificates under Section 63 of Bharatiya Sakshya Adhiniyam 2023.",
                "🚓 **POLICE STATION PROTOCOL, GENERAL DIARY & ZERO FIR (SEC 173 BNSS):** Submit two typed, signed copies of your complaint to the Station House Officer. Insist on obtaining a receiving seal with a General Diary (GD) entry number, or demand a Zero FIR under Section 173 BNSS if out-of-jurisdiction is cited.",
                "📜 **MANDATORY STATUTORY ESCALATION AGAINST INACTION (SEC 175(3) BNSS TO SP):** If the local police station refuses to register an FIR or act, send your signed complaint via Registered Speed Post directly to the Superintendent of Police (SP) / DCP under Section 175(3) BNSS. Note that willful refusal violates Section 199 BNS.",
                "✉️ **FORMAL ADVOCATE LEGAL DEMAND NOTICE & STATUTORY TIMELINES:** Have a licensed advocate issue a formal 15-Day Legal Demand Notice via Speed Post and Email, formally putting the opposing party on notice of statutory violations, articulating the cause of action, and proposing resolution before instituting litigation.",
                "🏛️ **JUDICIAL RECOURSE & MAGISTERIAL PETITIONS (SEC 175(4) / 176 BNSS):** Approach the Judicial Magistrate First Class under Section 175(4) BNSS praying for judicial orders directing investigation, summons, or search warrants if police machinery fails to act.",
                "🛡️ **CIVIL REDRESSAL, URGENT INJUNCTIONS (CPC ORDER 39) & DAMAGES:** File for appropriate civil relief before the competent District Court or High Court, seeking temporary injunctions under Order 39 Rules 1 & 2 CPC, specific relief, restitution, and claiming compensation for financial losses and mental agony.",
                "⏳ **STRATEGIC ADVOCATE SAFEGUARDS, LIMITATION CLOCK & CROSS-FIR PRECAUTIONS:** Always calculate limitation periods under the Limitation Act 1963 before filing. If threatened with false cross-cases, prepare Section 482 BNSS anticipatory bail applications in advance. Preserve all India Post Speed Post consignment receipts."
            )
        )

        val consumer = LegalProcedure(
            id = "consumer_complaint",
            domain = "Consumer Protection & Commercial Liability",
            title = "Defective Goods, Deficiency in Service & Consumer Commission Recourse",
            primaryStatutes = "Consumer Protection Act 2019 (Sec 35, 47, 58, 83–87) • Consumer Protection (E-Commerce) Rules 2020 • Section 63 BSA 2023",
            offenseClassification = "Statutory Consumer Dispute + Product Liability Claim.",
            cognizableStatus = "Regulatory / Quasi-Judicial • Summary adjudication with pecuniary tiers.",
            phases = listOf(
                ProcedurePhase(
                    phaseNumber = 1,
                    title = "Immediate Steps & Evidence Preservation",
                    timeframe = "First 1-7 Days",
                    actions = listOf(
                        "**Preserve Invoices & Packaging:** Retain original tax invoice, delivery receipts, packaging box with shipping label, and warranty card.",
                        "**Forensic Photo/Video Evidence:** Photograph and video-record the defect, non-functionality, or deficiency under Section 63 BSA 2023.",
                        "**Lodge National Consumer Helpline (NCH) Grievance:** Call 1915 or register ticket on consumerhelpline.gov.in with company ticket reference.",
                        "**Escalate to E-Commerce Grievance Officer:** Send formal grievance email citing Consumer Protection (E-Commerce) Rules 2020 (mandatory 48-hour acknowledgment)."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 2,
                    title = "Formal 15-Day Advocate Legal Demand Notice",
                    timeframe = "",
                    actions = listOf(
                        "**15-Day Statutory Legal Demand Notice:** Dispatch formal legal notice via Speed Post and email to seller, e-commerce platform, and manufacturer.",
                        "**Demand Full Refund & Damages:** Demand replacement, full refund with 18% interest, and damages for harassment within 15 days.",
                        "**Preserve Postal Delivery Proof:** Download India Post tracking delivery confirmation."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 3,
                    title = "Judicial Proceedings before Consumer Commission",
                    timeframe = "",
                    actions = listOf(
                        "**File via e-Daakhil Portal:** Lodge complaint online on edaakhil.nic.in before the competent Consumer Commission.",
                        "**Pecuniary Tier Selection (Section 35 CPA 2019):** District Commission under Section 35 (claims up to ₹50 Lakhs); State Commission under Section 47 (₹50 Lakhs to ₹2 Crores); National Commission under Section 58 (above ₹2 Crores).",
                        "**Product Liability Action u/s 83-87 CPA:** Implead manufacturer and seller for harm caused by defective product.",
                        "**Pray for Multiple Reliefs:** Full refund + replacement + statutory punitive damages + compensation for mental agony + litigation costs."
                    )
                )
            ),
            strategicAdvice = listOf(
                "*Limitation Period:* Complaint must be filed within 2 YEARS from the date the cause of action arose (Sec 69 CPA 2019).",
                "*Pecuniary Basis:* Jurisdiction is determined by the consideration paid (value of goods/services), not the inflated compensation claimed.",
                "Under CPA 2019, consumers can file complaints where they reside or work, not where the seller is located."
            ),
            detailedPoints = listOf(
                "⚖️ **NATURE OF CONSUMER DISPUTE & STATUTORY JURISDICTION:** Statutory consumer remedy under the Consumer Protection Act 2019 (CPA 2019) for 'deficiency in service' (Sec 2(11)), 'defect in goods' (Sec 2(10)), and 'unfair trade practice' (Sec 2(47)), along with product liability claims under Sections 83–87 CPA 2019.",
                "🚨 **EVIDENTIARY AUDIT & FORENSIC PROOF (SEC 63 BSA 2023):** Compile all primary proof: tax invoices, payment gateway UTR receipts, product warranty documents, unboxing videos, and photos of defect. Preserve digital records with electronic certification under Section 63 of Bharatiya Sakshya Adhiniyam 2023.",
                "📞 **NATIONAL CONSUMER HELPLINE (1915) & GRIEVANCE ESCALATION:** File a formal pre-litigation grievance on the National Consumer Helpline (NCH) portal (consumerhelpline.gov.in) or call 1915. Under the Consumer Protection (E-Commerce) Rules 2020, e-commerce entities must appoint a Resident Grievance Officer who must acknowledge complaints within 48 hours and resolve them within 1 month.",
                "✉️ **DRAFTING & SERVING 15-DAY ADVOCATE LEGAL DEMAND NOTICE:** Issue a formal 15-Day Legal Demand Notice via Registered Speed Post and email to both the retailer/platform and the manufacturer, specifying defect particulars, giving 15 days to refund/replace, and putting them on notice of Consumer Commission litigation.",
                "🏛️ **DETERMINATION OF PECUNIARY & TERRITORIAL JURISDICTION (SECTION 35 CPA):** Under Section 35 and Section 47 of CPA 2019, pecuniary limits are based strictly on consideration paid: District Commission under Section 35 handles matters up to ₹50 Lakhs; State Commission under Section 47 from ₹50 Lakhs to ₹2 Crores; National Commission under Section 58 above ₹2 Crores. CPA 2019 specifically allows consumers to file in their local territorial jurisdiction where they reside or work.",
                "💻 **ONLINE FILING VIA E-DAAKHIL PORTAL (EDAAKHIL.NIC.IN):** File the verified consumer complaint online via the e-Daakhil system without physical court attendance. Attach scanned copies of invoice, demand notice, proof of delivery, and verification affidavit.",
                "🛡️ **PRODUCT LIABILITY CLAIMS (SECTIONS 83–87 CPA 2019):** If the defective product caused personal injury, property damage, or economic harm, invoke product liability under Sections 84 (Manufacturer liability), 85 (Service provider liability), and 86 (Product seller liability) for comprehensive compensation.",
                "💰 **PUNITIVE DAMAGES & CONSUMER WELFARE RELIEFS:** Pray for multiple statutory reliefs: return of price paid with interest, replacement of goods, removal of defects, compensation for mental agony, litigation costs, and punitive damages u/s 39(1)(d) CPA 2019.",
                "⏳ **STRATEGIC ADVOCATE SAFEGUARDS & 2-YEAR LIMITATION CLOCK:** Under Section 69 of CPA 2019, a consumer complaint must be filed within 2 YEARS from the date the cause of action accrued. Keep a meticulous binder of all communications, courier receipts, and customer support tickets."
            )
        )

        val employment = LegalProcedure(
            id = "employment_dispute",
            domain = "Labor & Employment Law",
            title = "Unpaid Salary, Wrongful Termination & Statutory Gratuity Recovery",
            primaryStatutes = "Code on Wages 2019 (Sec 17, 45) • Payment of Gratuity Act 1972 (Sec 7, 8) • Industrial Disputes Act 1947 (Sec 2A, 33C(2)) • Shops and Establishments Act",
            offenseClassification = "Labor Law Statutory Violation + Civil Debt Claim + Breach of Contract.",
            cognizableStatus = "Civil/Labor Dispute • Non-Cognizable • Quasi-Judicial recovery before Labor Court.",
            phases = listOf(
                ProcedurePhase(
                    phaseNumber = 1,
                    title = "Evidentiary Audit & Document Compilation",
                    timeframe = "First 1-14 Days",
                    actions = listOf(
                        "**Secure Employment Documents:** Retain appointment letter, employment contract, monthly payslips, Form 16, and appraisal letters.",
                        "**Bank Salary Credit Trail:** Obtain certified bank statements showing regular monthly salary credits and subsequent cessation.",
                        "**Termination/Resignation Email:** Preserve written record of resignation, termination notice, or Full & Final (FnF) settlement communications.",
                        "**Calculate Dues Checklist:** Itemize unpaid salary, notice pay, accumulated leave encashment, incentive bonus, and statutory gratuity."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 2,
                    title = "Formal 15-Day Advocate Demand Notice & Labor Conciliation",
                    timeframe = "",
                    actions = listOf(
                        "**15-Day Advocate Legal Notice:** Issue formal statutory legal demand notice to employer company and managing directors via Speed Post.",
                        "**Labor Officer / ALC Conciliation:** Submit formal petition before Assistant Labor Commissioner (ALC) / Conciliation Officer under Code on Wages / Industrial Disputes Act.",
                        "**Claim 18% Statutory Interest:** Put employer on notice of statutory interest and penal damages for willful wage withholding."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 3,
                    title = "Judicial Proceedings in Labor Court & Gratuity Recovery",
                    timeframe = "",
                    actions = listOf(
                        "**Section 33C(2) Industrial Disputes Act Petition:** File claim before Labor Court for computation and recovery of all admitted and contractual dues.",
                        "**Form I & Form N Gratuity Application:** If completed 5 years of service, file Form N before Controlling Authority under Payment of Gratuity Act 1972 for dues + 10% compound interest u/s 7(3A).",
                        "**Revenue Recovery Certificate (RRC):** Obtain RRC from Labor Court/Controlling Authority directed to District Collector for recovery as arrears of land revenue.",
                        "**Summary Civil Suit (Order 37 CPC):** For managerial/executive employees not covered under definition of workman, file summary recovery suit in District Court."
                    )
                )
            ),
            strategicAdvice = listOf(
                "*Limitation Window:* Wage recovery claims under Code on Wages carry a 3-year limitation period from date due.",
                "*Gratuity Eligibility:* Mandatory upon completing 5 years of continuous service; employer must pay within 30 days of leaving.",
                "Export and secure all official emails, performance appraisals, and timesheets to personal storage before corporate email access is revoked."
            ),
            detailedPoints = listOf(
                "⚖️ **SUBSTANTIVE EMPLOYMENT RIGHTS & LEGAL CLASSIFICATION:** Unpaid wages constitute a violation of Section 17 of Code on Wages 2019 (mandating payment within 7–10 days of wage period), breach of employment contract, and illegal deprivation of livelihood under Article 21 of the Constitution.",
                "📱 **EVIDENTIARY AUDIT & DOCUMENT PRESERVATION (SEC 63 BSA):** Compile full employment dossier: signed offer letter, service rules/handbook, monthly payslips, bank statements, Timesheet logs, official Slack/email communications, and Form 16, certified under Section 63 BSA 2023.",
                "💰 **COMPREHENSIVE FULL & FINAL (FNF) DUE COMPUTATION:** Prepare an itemized financial schedule: unpaid basic salary, HRA, statutory bonus under Payment of Bonus Act, earned leave encashment, notice period pay if wrongfully terminated without notice, and statutory gratuity.",
                "✉️ **SERVING 15-DAY ADVOCATE LEGAL DEMAND NOTICE:** Have an advocate issue a formal 15-Day Demand Notice via Speed Post and registered email addressed to the company, HR Head, and Managing Directors jointly, demanding immediate disbursement of Full & Final settlement with 18% per annum commercial interest.",
                "🤝 **CONCILIATION PROCEEDINGS BEFORE ASSISTANT LABOR COMMISSIONER (ALC):** File a conciliation petition before the jurisdictional Labor Officer / ALC under Section 12 Industrial Disputes Act 1947 or Code on Wages. The ALC issues summons to management to produce wage registers and explore conciliation settlement.",
                "🏛️ **LABOR COURT RECOVERY PETITION U/S 33C(2) INDUSTRIAL DISPUTES ACT:** If conciliation fails (Failure of Conciliation Report - FCR), institute a Section 33C(2) recovery petition before the Labor Court to compute money due from the employer, which operates as an executable judicial decree.",
                "📜 **STATUTORY GRATUITY RECOVERY & 10% COMPOUND INTEREST (SEC 7/8):** If you completed 5 years of continuous service, submit Form I to employer; upon failure to pay within 30 days, file Form N before the Controlling Authority under Payment of Gratuity Act 1972. Under Section 7(3A), the authority statutorily awards 10% compound interest on delayed gratuity.",
                "🛡️ **REVENUE RECOVERY CERTIFICATE (RRC) & DISTRICT COLLECTOR EXECUTION:** When the employer fails to satisfy the Labor Court or Gratuity Authority order, the authority issues a Revenue Recovery Certificate (RRC) to the District Collector, who attaches company bank accounts and assets under land revenue arrears procedure.",
                "⏳ **STRATEGIC ADVOCATE SAFEGUARDS & NON-COMPETE ADVISORY:** Indian courts (Delhi HC in 'Pepsi Foods' and SC in 'Percept D'Mark') consistently hold post-termination non-compete clauses void under Section 27 of the Indian Contract Act 1872. Note: 3-year limitation clock applies for wage recovery."
            )
        )

        val domesticViolence = LegalProcedure(
            id = "domestic_violence",
            domain = "Family & Domestic Protection Law",
            title = "Domestic Violence, Residence Orders & Statutory Maintenance Recourse",
            primaryStatutes = "Protection of Women from Domestic Violence Act 2005 (Sec 12, 18, 19, 20, 22, 23) • BNSS 2023 Sec 144 • BNS 2023 Sec 85, 86",
            offenseClassification = "Quasi-Criminal Statutory Protection + Criminal Cruelty (Sec 85 BNS) + Summary Maintenance.",
            cognizableStatus = "Cognizable for BNS Sec 85 • Special Magisterial summary procedure under DV Act.",
            phases = listOf(
                ProcedurePhase(
                    phaseNumber = 1,
                    title = "Immediate Safety, Medical Exam & Domestic Incident Report (DIR)",
                    timeframe = "First 24-48 Hours",
                    actions = listOf(
                        "**Immediate Safety & Helplines:** Dial 112 (Police) or 181 (Women Helpline) in emergency; reach a safe place or shelter home.",
                        "**Government Hospital Medico-Legal Exam:** Undergo medical examination at government hospital to document physical injuries on official MLC register.",
                        "**Domestic Incident Report (DIR):** Approach the local Protection Officer (PO) or registered Service Provider to prepare a Domestic Incident Report in Form I.",
                        "**Preserve Cruelty Evidence:** Archive WhatsApp threats, audio recordings, medical slips, and stridhan jewelry receipts under Section 63 BSA 2023."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 2,
                    title = "Section 12 DV Application & Ex-Parte Interim Relief",
                    timeframe = "",
                    actions = listOf(
                        "**File Section 12 DV Application:** Move petition before Judicial Magistrate First Class (JMFC) or Metropolitan Magistrate (MM).",
                        "**Section 23 Ex-Parte Interim Orders:** File urgent application u/s 23 supported by affidavit in Form III for immediate interim protection and interim maintenance.",
                        "**Notice to Respondents:** Magistrate issues summons to husband and in-laws returnable within 30 days."
                    )
                ),
                ProcedurePhase(
                    phaseNumber = 3,
                    title = "Judicial Reliefs: Residence, Protection & Maintenance Orders",
                    timeframe = "",
                    actions = listOf(
                        "**Residence Orders under Section 19 (Shared Household):** Prohibit respondent from dispossessing aggrieved person from shared household, or direct respondent to pay rent for alternative accommodation.",
                        "**Protection Orders under Section 18:** Restrain respondent from entering place of employment, contacting, or committing any act of violence.",
                        "**Monetary Relief under Section 20 & Maintenance under Section 144 BNSS:** Order monthly maintenance, medical expenses, and loss of earnings.",
                        "**Compensation u/s 22 & Custody u/s 21:** Award compensation for emotional distress/mental agony, and grant temporary custody of children.",
                        "**Breach of Protection Order u/s 31:** Breach of protection order is a cognizable, non-bailable criminal offense punishable with 1 year imprisonment."
                    )
                )
            ),
            strategicAdvice = listOf(
                "*Shared Household Right:* In 'Satish Chander Ahuja v. Sneha Ahuja', the Supreme Court held women have an absolute right to reside in the shared household even if owned by in-laws.",
                "*Stridhan Recovery:* Wife is absolute owner of Stridhan (jewelry, gifts); withholding Stridhan constitutes criminal breach of trust (Sec 316 BNS).",
                "Filing under DV Act does not require court fees and provides multi-pronged relief in a single proceeding."
            ),
            detailedPoints = listOf(
                "⚖️ **SUBSTANTIVE SCOPE OF DOMESTIC VIOLENCE ACT 2005 & BNS SECTION 85:** The Protection of Women from Domestic Violence Act 2005 (PWDVA) covers physical, sexual, verbal, emotional, and economic abuse. In parallel, Section 85 BNS (formerly 498A IPC) criminalizes cruelty by husband or relatives with up to 3 years imprisonment.",
                "🚨 **EMERGENCY SAFETY PROTOCOL, 181 HELPLINE & MEDICAL PROOF:** If facing immediate physical harm, dial 112 or 181. Immediately undergo a medical examination at a government hospital casualty, ensuring doctor notes history of domestic assault on the MLC. Photographs of bruises and injuries should be taken immediately.",
                "📋 **ROLE OF PROTECTION OFFICER & DOMESTIC INCIDENT REPORT (DIR):** Under Section 9 and Rule 5, contact the District Protection Officer (PO) or designated Mahila Thana to record a Domestic Incident Report (DIR) in Form I. Under Section 12 proviso, the Magistrate must take into consideration any DIR submitted by the Protection Officer.",
                "🏛️ **MAGISTERIAL APPLICATION UNDER SECTION 12 DV ACT:** File a comprehensive petition under Section 12 PWDVA before the Judicial Magistrate First Class (JMFC) or Metropolitan Magistrate (MM). The petition consolidates all forms of abuse, itemizes financial requirements, and seeks multiple statutory reliefs.",
                "⚡ **EX-PARTE EMERGENCY INTERIM RELIEF UNDER SECTION 23 DV ACT:** Move an interim application under Section 23 PWDVA supported by an affidavit in Form III. If satisfied that an application discloses an act of domestic violence, the Magistrate has express statutory power to grant ex-parte interim protection and interim maintenance on the very first date of hearing.",
                "🛡️ **INVIOLABLE RESIDENCE ORDERS UNDER SECTION 19 (SHARED HOUSEHOLD):** Secure an order under Section 19 restraining the respondent from dispossessing or throwing out the aggrieved woman from the shared household. The Supreme Court in 'Satish Chander Ahuja v. Sneha Ahuja (2020)' affirmed that 'shared household' includes premises owned by husband or in-laws. Alternatively, the court will direct the husband to pay rent for suitable accommodation.",
                "🚫 **RESTRAINING INJUNCTIONS & PROTECTION ORDERS UNDER SECTION 18:** Obtain Section 18 protection orders prohibiting the respondent from committing domestic violence, communicating by phone or email, entering the woman's workplace, or alienating shared household assets.",
                "💰 **MONETARY RELIEF (SEC 20), COMPENSATION (SEC 22) & SECTION 144 BNSS:** Pray for monetary relief under Section 20 covering medical expenses, household maintenance, and children's school fees. File a parallel or consolidated petition under Section 144 BNSS (formerly Section 125 CrPC) for permanent monthly maintenance, as well as Section 22 compensation for mental torture.",
                "⏳ **STRATEGIC ADVOCATE SAFEGUARDS, STRIDHAN RECOVERY & PENAL BREACH (SEC 31):** Stridhan belongs exclusively to the woman; retain bills and photos of wedding jewelry. Crucially, under Section 31 PWDVA, any breach of a protection order by the respondent is a COGNIZABLE, NON-BAILABLE criminal offense punishable with 1 year jail."
            )
        )

        return mapOf(
            "landlord_tenant" to landlord,
            "cheque_bounce" to cheque,
            "hit_and_run" to hitAndRun,
            "police_fir_refusal" to policeRefusal,
            "cyber_fraud" to cyber,
            "consumer_complaint" to consumer,
            "employment_dispute" to employment,
            "domestic_violence" to domesticViolence,
            "general_dispute" to general
        )
    }
}

