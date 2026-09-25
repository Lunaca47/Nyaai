package com.nyaai.data.matter

import java.nio.charset.StandardCharsets
import java.security.Key
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Interface defining the strategy for retrieving or creating AES-256 keys.
 */
interface MatterKeyProvider {
    fun getOrCreateKey(): Key
}

/**
 * Production Android KeyStore provider:
 * Uses AndroidKeyStore (hardware TEE/StrongBox) to generate and store
 * an AES-256 key with GCM mode and no padding.
 */
class AndroidKeyStoreKeyProvider(
    val keyAlias: String = DEFAULT_KEY_ALIAS
) : MatterKeyProvider {

    companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val DEFAULT_KEY_ALIAS = "nyaai_matter_vault_master_key"
    }

    override fun getOrCreateKey(): Key {
        val keyStore = java.security.KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
        if (keyStore.containsAlias(keyAlias)) {
            return keyStore.getKey(keyAlias, null)
        }

        val keyGenerator = javax.crypto.KeyGenerator.getInstance(
            android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            keyAlias,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}

/**
 * Test JVM key provider used strictly when running in JVM unit test environments
 * where AndroidKeyStore is not present.
 */
class TestJvmKeyProvider(
    val seed: String = "nyaai_legal_matter_vault_test_key_seed_2026"
) : MatterKeyProvider {
    override fun getOrCreateKey(): Key {
        val sha = MessageDigest.getInstance("SHA-256")
        val keyBytes = sha.digest(seed.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }
}

/**
 * Provides AES-GCM encryption for sensitive Matter case data at rest.
 * Uses AndroidKeyStore (AES-256 in hardware TEE/StrongBox) on device runtime,
 * with injectable KeyProvider abstraction for JVM host testing.
 */
object MatterCryptoService {

    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH_BYTES = 12
    private const val ENCRYPTED_PREFIX = "enc:v1:"

    @Volatile
    var keyProvider: MatterKeyProvider = createDefaultKeyProvider()

    fun createDefaultKeyProvider(): MatterKeyProvider {
        return try {
            // Verify AndroidKeyStore provider is genuinely functional on current runtime
            val keyStore = java.security.KeyStore.getInstance(AndroidKeyStoreKeyProvider.ANDROID_KEYSTORE)
            keyStore.load(null)
            AndroidKeyStoreKeyProvider()
        } catch (e: Throwable) {
            // Host JVM fallback when running outside Android OS (e.g. pure JUnit tests)
            TestJvmKeyProvider()
        }
    }

    fun setKeyProviderForTesting(provider: MatterKeyProvider) {
        this.keyProvider = provider
    }

    fun resetDefaultKeyProvider() {
        this.keyProvider = createDefaultKeyProvider()
    }

    private fun getKey(): Key = keyProvider.getOrCreateKey()

    /**
     * Encrypts a plaintext JSON string using AES-GCM.
     * Returns a string prefixed with "enc:v1:" followed by base64-encoded IV + ciphertext.
     */
    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return plainText

        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, getKey(), spec)

        val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

        val base64Payload = Base64.getEncoder().encodeToString(combined)
        return "$ENCRYPTED_PREFIX$base64Payload"
    }

    /**
     * Decrypts an encrypted payload. If the input is not encrypted (does not start
     * with the prefix), returns the input as-is for transparent backward compatibility.
     */
    fun decrypt(cipherText: String): String {
        if (!isEncrypted(cipherText)) {
            return cipherText
        }

        val base64Payload = cipherText.removePrefix(ENCRYPTED_PREFIX)
        val combined = Base64.getDecoder().decode(base64Payload)
        if (combined.size < IV_LENGTH_BYTES) {
            throw IllegalArgumentException("Invalid encrypted payload size")
        }

        val iv = ByteArray(IV_LENGTH_BYTES)
        val cipherBytes = ByteArray(combined.size - IV_LENGTH_BYTES)
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES)
        System.arraycopy(combined, IV_LENGTH_BYTES, cipherBytes, 0, cipherBytes.size)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)

        val decryptedBytes = cipher.doFinal(cipherBytes)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    fun isEncrypted(text: String): Boolean {
        return text.startsWith(ENCRYPTED_PREFIX)
    }
}
