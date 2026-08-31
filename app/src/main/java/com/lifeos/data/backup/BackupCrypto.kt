package com.lifeos.data.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Symmetric encryption for LifeOS backup files.
 *
 * Algorithm choices (all from android.jar — zero extra dependencies):
 *   Key derivation : PBKDF2WithHmacSHA256
 *                    310,000 iterations (OWASP 2023 recommendation for SHA-256)
 *                    256-bit output key
 *   Cipher         : AES/GCM/NoPadding — authenticated encryption, detects
 *                    tampering without a separate HMAC pass.
 *   Tag            : 128-bit GCM authentication tag (maximum, appended by JCE)
 *
 * File layout (all big-endian):
 *   [0..3]   magic  = 0x4C 0x4F 0x42 0x4B  ("LOBK" = LifeOS Backup)
 *   [4]      version = 0x01
 *   [5..36]  salt   (32 bytes, random per backup)
 *   [37..48] iv     (12 bytes, random per backup — GCM standard nonce size)
 *   [49..]   AES-GCM ciphertext (includes 16-byte auth tag appended by JCE)
 *
 * The plaintext that gets encrypted is the GZIP-compressed SQLite database.
 *
 * Security properties:
 *   • Passphrase never stored — derived key lives only in memory during operation.
 *   • Salt + IV are random → same passphrase + same file produces different ciphertext.
 *   • AES-GCM authentication tag → any bit flip in the ciphertext or header
 *     causes decryption to throw [javax.crypto.AEADBadTagException] before
 *     any plaintext is returned.
 *   • PBKDF2 iteration count makes brute-force attacks expensive.
 */
object BackupCrypto {

    // ── Constants ──────────────────────────────────────────────────────────────

    private val MAGIC = byteArrayOf(0x4C, 0x4F, 0x42, 0x4B) // "LOBK"
    private const val FORMAT_VERSION: Byte = 0x01
    private const val HEADER_SIZE = 5 + 32 + 12             // magic+version + salt + iv

    private const val SALT_BYTES = 32
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 310_000

    private const val KEY_FACTORY_ALGO = "PBKDF2WithHmacSHA256"
    private const val CIPHER_ALGO = "AES/GCM/NoPadding"

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Encrypts [plaintext] (the GZIP-compressed SQLite bytes) with [passphrase].
     * Returns the full backup file bytes including the file header.
     *
     * This function is CPU-bound (PBKDF2 takes ~2 s on a mid-range device).
     * Always call from [kotlinx.coroutines.Dispatchers.IO].
     */
    fun encrypt(plaintext: ByteArray, passphrase: String): ByteArray {
        val rng = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also { rng.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { rng.nextBytes(it) }

        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance(CIPHER_ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)

        // Assemble: header + ciphertext
        return MAGIC + byteArrayOf(FORMAT_VERSION) + salt + iv + ciphertext
    }

    /**
     * Decrypts a backup file produced by [encrypt].
     *
     * @throws IllegalArgumentException if the file magic or version doesn't match.
     * @throws javax.crypto.AEADBadTagException if the passphrase is wrong or
     *         the file has been tampered with.
     *
     * Always call from [kotlinx.coroutines.Dispatchers.IO].
     */
    fun decrypt(data: ByteArray, passphrase: String): ByteArray {
        require(data.size > HEADER_SIZE) { "File too small to be a valid backup" }

        // Validate magic
        val magic = data.slice(0..3).toByteArray()
        require(magic.contentEquals(MAGIC)) {
            "Not a LifeOS backup file (magic mismatch)"
        }
        require(data[4] == FORMAT_VERSION) {
            "Unsupported backup version: ${data[4]}. Please update LifeOS."
        }

        val salt = data.slice(5 until 37).toByteArray()
        val iv = data.slice(37 until 49).toByteArray()
        val ciphertext = data.slice(49 until data.size).toByteArray()

        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance(CIPHER_ALGO)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ciphertext) // throws AEADBadTagException on wrong passphrase
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGO)
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword() // zero the passphrase copy in the PBEKeySpec
        return SecretKeySpec(keyBytes, "AES")
    }

    // ── Operator helpers (avoids boxing for byte array concatenation) ──────────

    private operator fun ByteArray.plus(other: ByteArray): ByteArray {
        val result = ByteArray(size + other.size)
        System.arraycopy(this, 0, result, 0, size)
        System.arraycopy(other, 0, result, size, other.size)
        return result
    }
}
