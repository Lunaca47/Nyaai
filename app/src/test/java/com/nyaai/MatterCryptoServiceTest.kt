package com.nyaai

import com.nyaai.data.matter.AndroidKeyStoreKeyProvider
import com.nyaai.data.matter.MatterCryptoService
import com.nyaai.data.matter.TestJvmKeyProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MatterCryptoServiceTest {

    @Before
    fun setUp() {
        MatterCryptoService.setKeyProviderForTesting(TestJvmKeyProvider("test_master_seed_123"))
    }

    @After
    fun tearDown() {
        MatterCryptoService.resetDefaultKeyProvider()
    }

    @Test
    fun testEncryptionAndDecryptionRoundTrip() {
        val sensitiveData = """{"allegations":"Domestic harassment","financial_loss":150000}"""
        val encrypted = MatterCryptoService.encrypt(sensitiveData)

        assertNotNull(encrypted)
        assertTrue("Ciphertext must start with enc:v1: prefix", encrypted.startsWith("enc:v1:"))
        assertNotEquals(sensitiveData, encrypted)

        val decrypted = MatterCryptoService.decrypt(encrypted)
        assertEquals(sensitiveData, decrypted)
    }

    @Test
    fun testUnencryptedTextPassedThroughTransparently() {
        val plain = """{"legacy":true,"unencrypted":"ok"}"""
        val result = MatterCryptoService.decrypt(plain)
        assertEquals(plain, result)
    }

    @Test
    fun testInvalidPayloadThrowsException() {
        val corrupted = "enc:v1:QUJD" // "ABC" in base64, too short for 12-byte IV
        try {
            MatterCryptoService.decrypt(corrupted)
            fail("Expected IllegalArgumentException for undersized payload")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Invalid encrypted payload size"))
        }
    }

    @Test
    fun testKeyIsolationDifferentKeysCannotDecrypt() {
        val originalText = "Confidential matter details for client"
        MatterCryptoService.setKeyProviderForTesting(TestJvmKeyProvider("seed_key_A"))
        val cipherA = MatterCryptoService.encrypt(originalText)

        // Switch to a different key provider
        MatterCryptoService.setKeyProviderForTesting(TestJvmKeyProvider("seed_key_B"))
        try {
            MatterCryptoService.decrypt(cipherA)
            fail("Decrypting with wrong key must fail with AEADBadTagException / general security error")
        } catch (e: Exception) {
            // Expected AEAD tag verification failure
            assertTrue(e is javax.crypto.AEADBadTagException || e is java.security.GeneralSecurityException)
        }
    }

    @Test
    fun testAndroidKeyStoreKeyProviderConfiguration() {
        val provider = AndroidKeyStoreKeyProvider()
        assertEquals("AndroidKeyStore", AndroidKeyStoreKeyProvider.ANDROID_KEYSTORE)
        assertEquals("nyaai_matter_vault_master_key", provider.keyAlias)
        assertEquals("nyaai_matter_vault_master_key", AndroidKeyStoreKeyProvider.DEFAULT_KEY_ALIAS)
    }
}
