package com.example.taskmanagement.domain.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordManager {
    companion object {
        private const val SALT_LENGTH = 32
        private const val HASH_ITERATIONS = 10000
        private const val HASH_KEY_LENGTH = 256
    }

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hashPassword(password: String, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val spec = PBEKeySpec(
            password.toCharArray(),
            saltBytes,
            HASH_ITERATIONS,
            HASH_KEY_LENGTH
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun verifyPassword(password: String, salt: String, storedHash: String): Boolean {
        val calculatedHash = hashPassword(password, salt)
        return calculatedHash == storedHash
    }

    fun isPasswordStrong(password: String): PasswordStrength {
        return when {
            password.length < 8 -> PasswordStrength.WEAK
            !password.any { it.isDigit() } -> PasswordStrength.WEAK
            !password.any { it.isUpperCase() } -> PasswordStrength.MEDIUM
            !password.any { it.isLowerCase() } -> PasswordStrength.MEDIUM
            !password.any { !it.isLetterOrDigit() } -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }
}

enum class PasswordStrength {
    WEAK, MEDIUM, STRONG
}