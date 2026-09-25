package com.nyaai

import com.nyaai.data.intake.IntakeEngine
import com.nyaai.data.matter.JurisdictionConfidence
import com.nyaai.data.matter.Matter
import org.junit.Assert.*
import org.junit.Test

class IntakeEngineTest {

    private val intake = IntakeEngine()

    @Test
    fun testDomainClassification() {
        assertEquals("tenancy_deposit", intake.classifyDomain("My landlord refused to return my security deposit"))
        assertEquals("cheque_bounce", intake.classifyDomain("Cheque bounced due to insufficient funds"))
        assertEquals("salary_nonpayment", intake.classifyDomain("My employer has not paid my salary for 3 months"))
        assertEquals("cyber_fraud", intake.classifyDomain("Someone hacked my account and made unauthorized UPI transfer"))
        assertEquals("police_fir_refusal", intake.classifyDomain("Police refused to register my FIR for bike theft"))
        assertEquals("consumer_complaint", intake.classifyDomain("Bought a defective refrigerator, seller refusing refund"))
    }

    @Test
    fun testMandatoryJurisdictionCheckForTenancy() {
        val initialMatter = Matter(title = "Rent Dispute", domain = "")
        val result = intake.processInput(initialMatter, "My landlord locked my flat")

        assertEquals("tenancy_deposit", result.updatedMatter.domain)
        assertEquals(JurisdictionConfidence.UNKNOWN, result.updatedMatter.jurisdiction.confidence)
        assertFalse("Intake should not be complete without jurisdiction", result.isComplete)
        assertNotNull("Intake should ask for state or city", result.nextQuestion)
        assertTrue("Question should mention state or city", result.nextQuestion!!.contains("state or city", ignoreCase = true))
    }

    @Test
    fun testJurisdictionDetectionFromInput() {
        val initialMatter = Matter(title = "Rent Dispute", domain = "tenancy_deposit")
        val result = intake.processInput(initialMatter, "The flat is in Bangalore, Karnataka")

        assertEquals(JurisdictionConfidence.CONFIRMED, result.updatedMatter.jurisdiction.confidence)
        assertTrue("Jurisdiction should be Karnataka", result.updatedMatter.jurisdiction.state == "Karnataka" || result.updatedMatter.jurisdiction.state == "Bangalore")
    }

    @Test
    fun testStoppingRuleWhenUserRequestsProceed() {
        val initialMatter = Matter(title = "Cheque Dispute", domain = "cheque_bounce")
        val result = intake.processInput(initialMatter, "Please give advice on what should I do now")

        assertTrue("Should complete immediately when user requests advice/proceed", result.isComplete)
        assertNull("No further questions should be asked", result.nextQuestion)
        assertEquals("assessment", result.updatedMatter.proceduralStage)
    }
}
