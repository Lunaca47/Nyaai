package com.nyaai

import com.nyaai.data.procedure.ProcedureEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProcedureEngineDepthTest {

    private lateinit var procedureEngine: ProcedureEngine

    @Before
    fun setUp() {
        procedureEngine = ProcedureEngine()
    }

    @Test
    fun testConsumerProtectionProcedureRoadmap() {
        val query = "Bought defective laptop on Flipkart and they are refusing refund for damaged screen"
        assertTrue(procedureEngine.isScenarioQuery(query))

        val procedure = procedureEngine.matchProcedure(query)
        assertEquals("consumer_complaint", procedure.id)
        assertTrue(procedure.title.contains("Defective Goods"))
        assertTrue(procedure.primaryStatutes.contains("Consumer Protection Act 2019"))

        val (answer, confidence) = procedureEngine.buildProcedureAnswer(query)
        assertTrue(confidence >= 0.98)
        assertTrue(answer.contains("e-Daakhil", ignoreCase = true))
        assertTrue(answer.contains("National Consumer Helpline", ignoreCase = true))
        assertTrue(answer.contains("Section 35", ignoreCase = true))
        assertTrue(answer.contains("1915"))
    }

    @Test
    fun testConsumerProtectionDetailedBrief() {
        val query = "Explain in detail the step by step legal procedure to file consumer case for defective product"
        assertTrue(procedureEngine.isDetailedQuery(query))

        val (answer, confidence) = procedureEngine.buildProcedureAnswer(query)
        assertEquals(0.99, confidence, 0.001)
        assertTrue(answer.contains("SENIOR ADVOCATE COMPREHENSIVE LEGAL BRIEF"))
        assertTrue(answer.contains("Section 35"))
        assertTrue(answer.contains("Section 47"))
        assertTrue(answer.contains("Product Liability", ignoreCase = true))
        assertTrue(answer.contains("2 YEARS"))
    }

    @Test
    fun testEmploymentLaborProcedureRoadmap() {
        val query = "My employer has withheld unpaid salary for 3 months after resignation"
        assertTrue(procedureEngine.isScenarioQuery(query))

        val procedure = procedureEngine.matchProcedure(query)
        assertEquals("employment_dispute", procedure.id)
        assertTrue(procedure.title.contains("Unpaid Salary"))
        assertTrue(procedure.primaryStatutes.contains("Code on Wages 2019"))
        assertTrue(procedure.primaryStatutes.contains("Payment of Gratuity Act 1972"))

        val (answer, confidence) = procedureEngine.buildProcedureAnswer(query)
        assertTrue(confidence >= 0.98)
        assertTrue(answer.contains("15-Day Advocate Legal Notice", ignoreCase = true))
        assertTrue(answer.contains("Labor Commissioner", ignoreCase = true))
        assertTrue(answer.contains("Section 33C(2)", ignoreCase = true))
        assertTrue(answer.contains("Full & Final", ignoreCase = true))
    }

    @Test
    fun testEmploymentLaborDetailedBrief() {
        val query = "Comprehensive step-by-step legal brief on recovering unpaid salary and gratuity from company"
        assertTrue(procedureEngine.isDetailedQuery(query))

        val (answer, confidence) = procedureEngine.buildProcedureAnswer(query)
        assertEquals(0.99, confidence, 0.001)
        assertTrue(answer.contains("Code on Wages 2019"))
        assertTrue(answer.contains("Controlling Authority"))
        assertTrue(answer.contains("10% compound interest"))
        assertTrue(answer.contains("Revenue Recovery Certificate"))
    }

    @Test
    fun testDomesticViolenceProcedureRoadmap() {
        val query = "Wife facing domestic violence and beaten by husband and in-laws need protection order"
        assertTrue(procedureEngine.isScenarioQuery(query))

        val procedure = procedureEngine.matchProcedure(query)
        assertEquals("domestic_violence", procedure.id)
        assertTrue(procedure.title.contains("Domestic Violence"))
        assertTrue(procedure.primaryStatutes.contains("Protection of Women from Domestic Violence Act 2005"))

        val (answer, confidence) = procedureEngine.buildProcedureAnswer(query)
        assertTrue(confidence >= 0.98)
        assertTrue(answer.contains("Domestic Incident Report", ignoreCase = true))
        assertTrue(answer.contains("Section 12", ignoreCase = true))
        assertTrue(answer.contains("Section 18", ignoreCase = true))
        assertTrue(answer.contains("Section 19", ignoreCase = true))
        assertTrue(answer.contains("Section 144 BNSS", ignoreCase = true))
    }

    @Test
    fun testDomesticViolenceDetailedBrief() {
        val query = "Exhaustive legal advisory on domestic violence protection orders, residence rights, and maintenance in detail"
        assertTrue(procedureEngine.isDetailedQuery(query))

        val (answer, confidence) = procedureEngine.buildProcedureAnswer(query)
        assertEquals(0.99, confidence, 0.001)
        assertTrue(answer.contains("SENIOR ADVOCATE COMPREHENSIVE LEGAL BRIEF"))
        assertTrue(answer.contains("Section 23"))
        assertTrue(answer.contains("Satish Chander Ahuja"))
        assertTrue(answer.contains("Stridhan"))
        assertTrue(answer.contains("Section 31"))
    }
}
