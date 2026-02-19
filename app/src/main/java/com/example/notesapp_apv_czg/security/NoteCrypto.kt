package com.example.notesapp_apv_czg.security

import android.util.Base64
import javax.crypto.Cipher

class NoteCrypto(
    private val keyManager: SecureKeyManager
) {
    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return plaintext
        val cipher = keyManager.createEncryptCipher()
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)
        return PREFIX + encoded
    }

    fun decrypt(ciphertext: String): String {
        if (!ciphertext.startsWith(PREFIX)) return ciphertext
        val b64 = ciphertext.removePrefix(PREFIX)
        val combined = Base64.decode(b64, Base64.NO_WRAP)
        if (combined.size <= IV_SIZE) return ""
        val iv = combined.copyOfRange(0, IV_SIZE)
        val cipherBytes = combined.copyOfRange(IV_SIZE, combined.size)
        val cipher = keyManager.createDecryptCipher(iv)
        val plain = cipher.doFinal(cipherBytes)
        return String(plain, Charsets.UTF_8)
    }

    companion object {
        private const val IV_SIZE = 12
        private const val PREFIX = "enc:"
    }
}
