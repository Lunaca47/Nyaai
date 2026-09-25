package com.nyaai.data.intake

import com.nyaai.data.matter.*

data class DomainSlot(
    val slotName: String,
    val description: String,
    val isMaterial: Boolean,
    val promptQuestion: String,
    val options: List<String> = emptyList()
)

data class DomainChecklist(
    val domain: String,
    val requiresJurisdiction: Boolean,
    val slots: List<DomainSlot>
)

data class IntakeStepResult(
    val nextQuestion: String?,
    val isComplete: Boolean,
    val updatedMatter: Matter
)

class IntakeEngine {

    private val checklists: Map<String, DomainChecklist> = initChecklists()

    fun classifyDomain(query: String): String {
        val q = query.lowercase().trim()
        return when {
            (q.contains("landlord") || q.contains("tenant") || q.contains("flat") || q.contains("rent") || q.contains("deposit")) ->
                "tenancy_deposit"

            (q.contains("cheque") || q.contains("bounced") || q.contains("dishonor")) ->
                "cheque_bounce"

            (q.contains("salary") || q.contains("employer") || q.contains("wages") || q.contains("resignation") || q.contains("boss")) ->
                "salary_nonpayment"

            (q.contains("cyber") || q.contains("upi") || q.contains("hacked") || q.contains("phishing") || q.contains("otp") || q.contains("scam")) ->
                "cyber_fraud"

            (q.contains("police") && (q.contains("refused") || q.contains("fir") || q.contains("complaint"))) ->
                "police_fir_refusal"

            (q.contains("bought") || q.contains("consumer") || q.contains("refund") || q.contains("product") || q.contains("service")) ->
                "consumer_complaint"

            (q.contains("domestic violence") || q.contains("dowry") || q.contains("beaten") || (q.contains("husband") && (q.contains("assault") || q.contains("cruelty") || q.contains("harass")))) ->
                "domestic_violence"

            else ->
                "general_dispute"
        }
    }

    fun getChecklist(domain: String): DomainChecklist? = checklists[domain]

    fun processInput(currentMatter: Matter, userInput: String): IntakeStepResult {
        val qLower = userInput.lowercase().trim()

        // 1. Check if user wants to bypass intake and proceed immediately
        val proceedPhrases = listOf("proceed", "give advice", "what should i do", "skip", "continue", "give steps", "tell me what to do")
        val forceProceed = proceedPhrases.any { qLower.contains(it) }

        // 2. Extract facts and update slot values
        val existingSlots = currentMatter.facts.mapNotNull { it.slotName }.toSet()
        val extractedFacts = mutableListOf<MatterFact>()
        extractedFacts.addAll(currentMatter.facts)

        var detectedJurisdiction = currentMatter.jurisdiction

        // Extract State / City
        val indianStates = listOf(
            "delhi", "maharashtra", "karnataka", "tamil nadu", "telangana", "uttar pradesh",
            "west bengal", "gujarat", "rajasthan", "punjab", "haryana", "kerala", "bihar",
            "madhya pradesh", "odisha", "andhra pradesh", "assam", "goa", "mumbai", "bangalore",
            "bengaluru", "hyderabad", "chennai", "kolkata", "pune", "ahmedabad", "jaipur", "lucknow"
        )
        for (state in indianStates) {
            if (qLower.contains(state)) {
                val stateName = state.replaceFirstChar { it.uppercase() }
                detectedJurisdiction = JurisdictionInfo(
                    state = stateName,
                    confidence = JurisdictionConfidence.CONFIRMED
                )
                if ("jurisdiction_state" !in existingSlots) {
                    extractedFacts.add(
                        MatterFact(
                            slotName = "jurisdiction_state",
                            statement = "Jurisdiction confirmed as $stateName",
                            source = FactSource.USER_STATED,
                            confidence = "HIGH"
                        )
                    )
                }
                break
            }
        }

        // Add user statement as general fact if new
        if (userInput.isNotBlank()) {
            extractedFacts.add(
                MatterFact(
                    statement = userInput.trim(),
                    source = FactSource.USER_STATED,
                    confidence = "STATED"
                )
            )
        }

        val domain = currentMatter.domain.ifBlank { classifyDomain(userInput) }
        val checklist = checklists[domain]

        val updatedMatter = currentMatter.copy(
            domain = domain,
            jurisdiction = detectedJurisdiction,
            facts = extractedFacts,
            updatedAt = System.currentTimeMillis()
        )

        if (forceProceed || checklist == null) {
            return IntakeStepResult(
                nextQuestion = null,
                isComplete = true,
                updatedMatter = updatedMatter.copy(proceduralStage = "assessment")
            )
        }

        // 3. Stopping rule check:
        // Jurisdiction check for domain requiring it
        val jurisdictionNeeded = checklist.requiresJurisdiction && updatedMatter.jurisdiction.confidence == JurisdictionConfidence.UNKNOWN

        if (jurisdictionNeeded) {
            return IntakeStepResult(
                nextQuestion = "Which Indian state or city did this occur in? (State laws govern tenancy, rent control, and local police jurisdiction)",
                isComplete = false,
                updatedMatter = updatedMatter
            )
        }

        // Material slots check
        val filledSlotNames = updatedMatter.facts.mapNotNull { it.slotName }.toSet()
        val missingMaterialSlot = checklist.slots.firstOrNull { it.isMaterial && it.slotName !in filledSlotNames }

        if (missingMaterialSlot != null) {
            return IntakeStepResult(
                nextQuestion = missingMaterialSlot.promptQuestion,
                isComplete = false,
                updatedMatter = updatedMatter.copy(
                    missingInformation = checklist.slots.filter { it.slotName !in filledSlotNames }.map { it.description }
                )
            )
        }

        // All material facts collected
        return IntakeStepResult(
            nextQuestion = null,
            isComplete = true,
            updatedMatter = updatedMatter.copy(proceduralStage = "assessment")
        )
    }

    private fun initChecklists(): Map<String, DomainChecklist> {
        val tenancy = DomainChecklist(
            domain = "tenancy_deposit",
            requiresJurisdiction = true,
            slots = listOf(
                DomainSlot("jurisdiction_state", "State/City where property is located", true, "Which State or City is the flat/property located in?"),
                DomainSlot("rent_agreement_exists", "Whether a formal written rent agreement exists", true, "Do you have an active, signed rent agreement (registered or notarized)?", listOf("Yes, registered", "Yes, notarized", "Unregistered / Verbal")),
                DomainSlot("deposit_amount", "Security deposit amount withheld", true, "What is the security deposit amount withheld or rent in dispute?"),
                DomainSlot("lockout_or_refusal", "Whether physically locked out or belongings seized", false, "Has the landlord locked you out or seized personal belongings?")
            )
        )

        val cheque = DomainChecklist(
            domain = "cheque_bounce",
            requiresJurisdiction = false,
            slots = listOf(
                DomainSlot("cheque_amount", "Cheque amount in dispute", true, "What is the face value amount written on the bounced cheque?"),
                DomainSlot("bank_memo_date", "Date of bank return memo", true, "When did you receive the bank return memo? (The strict 30-day statutory notice clock starts from this date)"),
                DomainSlot("statutory_notice_issued", "Whether 15-day statutory demand notice has been sent", true, "Have you issued a 15-day statutory legal notice under Section 138(b) NI Act via Speed Post?", listOf("Not yet", "Notice dispatched within 30 days", "30-day window expired")),
                DomainSlot("enforceable_debt_proof", "Invoices or agreements proving debt", false, "Do you have invoices, purchase orders, or agreements proving this was for a legally enforceable debt?")
            )
        )

        val cyber = DomainChecklist(
            domain = "cyber_fraud",
            requiresJurisdiction = false,
            slots = listOf(
                DomainSlot("amount_lost", "Total money debited fraudulently", true, "What was the total amount debited or defrauded?"),
                DomainSlot("incident_hours", "Hours elapsed since fraud occurred", true, "How many hours have passed since the unauthorized debit occurred? (Golden Hours: Dial 1930 within 2-4 hours to freeze funds)", listOf("Less than 2 hours", "2–24 hours", "24–72 hours", "More than 3 days")),
                DomainSlot("bank_notified", "Whether bank has been formally informed", true, "Have you formally notified your bank branch in writing or via official customer care?", listOf("Yes, within 72 hours (Zero Liability window)", "Yes, after 72 hours", "Not yet notified"))
            )
        )

        val policeRefusal = DomainChecklist(
            domain = "police_fir_refusal",
            requiresJurisdiction = true,
            slots = listOf(
                DomainSlot("jurisdiction_state", "State/City where incident occurred", true, "Which City/State did this incident take place in?"),
                DomainSlot("offense_type", "Nature of crime reported", true, "What was the offense reported? (Theft, assault, cheating, cyber crime, harassment)"),
                DomainSlot("receiving_copy_stamped", "Whether complaint has a receiving stamp or GD entry", true, "Did you obtain a stamped receiving copy or General Diary (GD) entry number from the police station?", listOf("Yes, stamped receiving copy", "No, verbal refusal / turned away", "Gave online complaint"))
            )
        )

        val salary = DomainChecklist(
            domain = "salary_nonpayment",
            requiresJurisdiction = true,
            slots = listOf(
                DomainSlot("jurisdiction_state", "State/City of workplace", true, "Which State is the company / workplace located in?"),
                DomainSlot("unpaid_months", "Duration and amount of salary pending", true, "How many months of salary are unpaid, and what is the total amount due?"),
                DomainSlot("employment_proof", "Appointment letter or pay slips", false, "Do you have an appointment letter, official email ID, or previous salary slips?")
            )
        )

        val consumer = DomainChecklist(
            domain = "consumer_complaint",
            requiresJurisdiction = false,
            slots = listOf(
                DomainSlot("dispute_amount", "Value of defective product or service", true, "What was the purchase value of the product or service in dispute?"),
                DomainSlot("defect_nature", "Nature of deficiency or defect", true, "What is the deficiency in service or defect in goods?"),
                DomainSlot("invoice_available", "Whether invoice or warranty card exists", false, "Do you have the tax invoice, transaction receipt, or warranty card?")
            )
        )

        val dv = DomainChecklist(
            domain = "domestic_violence",
            requiresJurisdiction = true,
            slots = listOf(
                DomainSlot("jurisdiction_state", "State/City where victim currently resides", true, "Which State/City are you currently residing in? (Under DV Act, you can file where you reside temporarily or permanently)"),
                DomainSlot("shared_household_status", "Whether residing in shared household or evicted", true, "Are you currently residing in the matrimonial/shared home, or have you been forced out?", listOf("Currently in shared home", "Locked out / Dispossessed", "At parents / shelter home")),
                DomainSlot("dir_prepared", "Whether Domestic Incident Report (DIR) is prepared", true, "Have you contacted a Protection Officer or Mahila Thana to record a Domestic Incident Report (DIR)?", listOf("Not yet", "Yes, DIR filed", "General police complaint only")),
                DomainSlot("children_custody", "Whether minor children are involved", false, "Are there minor children requiring immediate interim custody or school fee maintenance?")
            )
        )

        return mapOf(
            "tenancy_deposit" to tenancy,
            "cheque_bounce" to cheque,
            "cyber_fraud" to cyber,
            "police_fir_refusal" to policeRefusal,
            "salary_nonpayment" to salary,
            "consumer_complaint" to consumer,
            "domestic_violence" to dv
        )
    }
}
