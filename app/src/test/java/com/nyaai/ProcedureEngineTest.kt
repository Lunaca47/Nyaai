package com.nyaai

import com.nyaai.data.procedure.ProcedureEngine
import com.nyaai.ui.state.AppLanguage
import org.junit.Assert.*
import org.junit.Test

class ProcedureEngineTest {

    private val engine = ProcedureEngine()

    @Test
    fun testScenarioQueryDetection() {
        assertTrue(engine.isScenarioQuery("My landlord locked my flat, what should I do?"))
        assertTrue(engine.isScenarioQuery("Client gave me a bounced cheque due to insufficient funds"))
        assertTrue(engine.isScenarioQuery("Hit and run accident happened with my car"))
        assertTrue(engine.isScenarioQuery("Police refused to register my FIR"))
        assertTrue(engine.isScenarioQuery("Someone did UPI fraud from my bank account"))
        assertTrue(engine.isScenarioQuery("I have a legal dispute, how to proceed?"))
        assertFalse(engine.isScenarioQuery("what is article 21"))
    }

    @Test
    fun testDetailedQueryDetection() {
        assertTrue(engine.isDetailedQuery("explain in detail step by step each and everything"))
        assertTrue(engine.isDetailedQuery("provide full detailed explanation with all points"))
        assertFalse(engine.isDetailedQuery("what should I do"))
    }

    @Test
    fun testLandlordProcedureMatchingAndRoadmap() {
        val proc = engine.matchProcedure("My landlord locked my flat and threw my belongings")
        assertEquals("landlord_tenant", proc.id)
        assertEquals("Property & Tenancy Law", proc.domain)

        val (roadmap, conf) = engine.buildProcedureAnswer("My landlord locked my flat", AppLanguage.ENGLISH)
        assertTrue(roadmap.contains("SENIOR ADVOCATE LEGAL ADVISORY"))
        assertTrue(roadmap.contains("Transfer of Property Act"))
        assertTrue(roadmap.contains("Section 175(3) BNSS"))
        assertTrue(roadmap.contains("Order 39 CPC"))
        assertEquals(0.98, conf, 0.001)
    }

    @Test
    fun testLandlordDetailedBriefContainsMoreThanSevenPoints() {
        val (brief, conf) = engine.buildProcedureAnswer(
            "My landlord locked my flat, explain in detail step by step each and everything",
            AppLanguage.ENGLISH
        )
        assertTrue(brief.contains("COMPREHENSIVE LEGAL BRIEF"))
        val pointMatches = Regex("(?m)^\\d+\\.").findAll(brief).count()
        assertTrue("Must contain at least 8 numbered points, found $pointMatches", pointMatches >= 8)
        assertEquals(0.99, conf, 0.001)
    }

    @Test
    fun testChequeBounceProcedureRoadmap() {
        val proc = engine.matchProcedure("Cheque bounced due to insufficient funds")
        assertEquals("cheque_bounce", proc.id)

        val (roadmap, conf) = engine.buildProcedureAnswer("Cheque bounced due to insufficient funds", AppLanguage.ENGLISH)
        assertTrue(roadmap.contains("Negotiable Instruments Act"))
        assertTrue(roadmap.contains("138"))
        assertTrue(roadmap.contains("15-Day Statutory Legal Demand Notice"))
        assertTrue(roadmap.contains("143A"))
        assertEquals(0.98, conf, 0.001)
    }

    @Test
    fun testHitAndRunProcedureRoadmap() {
        val proc = engine.matchProcedure("Hit and run accident happened with my car")
        assertEquals("hit_and_run", proc.id)

        val (roadmap, _) = engine.buildProcedureAnswer("Hit and run accident happened with my car", AppLanguage.ENGLISH)
        assertTrue(roadmap.contains("Hit-and-Run Motor Vehicle Accident"))
        assertTrue(roadmap.contains("Medico-Legal Certificate"))
        assertTrue(roadmap.contains("MACT Claim"))
    }

    @Test
    fun testPoliceRefusalProcedureRoadmap() {
        val proc = engine.matchProcedure("Police refused to register my FIR")
        assertEquals("police_fir_refusal", proc.id)

        val (roadmap, _) = engine.buildProcedureAnswer("Police refused to register my FIR", AppLanguage.ENGLISH)
        assertTrue(roadmap.contains("175(3) BNSS"))
        assertTrue(roadmap.contains("Lalita Kumari"))
        assertTrue(roadmap.contains("175(4)"))
    }

    @Test
    fun testCyberFraudProcedureRoadmap() {
        val proc = engine.matchProcedure("UPI fraud happened and money debited")
        assertEquals("cyber_fraud", proc.id)

        val (roadmap, _) = engine.buildProcedureAnswer("UPI fraud happened and money debited", AppLanguage.ENGLISH)
        assertTrue(roadmap.contains("1930 Cyber Fraud Helpline"))
        assertTrue(roadmap.contains("66D IT Act"))
        assertTrue(roadmap.contains("503 BNSS"))
    }
}
