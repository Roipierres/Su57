package com.example.security

import android.content.Context
import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * High-performance AES-256 local file encryption and decryption manager.
 * Ensures media files inside the secret vault remain completely unreadable
 * outside this application.
 */
object VaultEncryption {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val KEY_SIZE = 256
    private const val IV_SIZE = 16

    /**
     * Hashes string using SHA-256 for secure PIN comparison.
     */
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Derives a SecretKey from user PIN or default salt.
     */
    private fun getSecretKey(context: Context, pin: String? = null): SecretKey {
        val masterSeed = if (!pin.isNullOrEmpty()) {
            pin + "_SKY_PILOT_VAULT_SALT_2026"
        } else {
            "SKY_PILOT_DEFAULT_VAULT_KEY_SAFE_88"
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(masterSeed.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    /**
     * Encrypts input stream data into a destination file.
     */
    fun encryptStreamToFile(
        context: Context,
        inputStream: InputStream,
        outputFile: File,
        userPin: String? = null
    ): Boolean {
        return try {
            val key = getSecretKey(context, userPin)
            val iv = ByteArray(IV_SIZE)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

            FileOutputStream(outputFile).use { fos ->
                // Write IV as first 16 bytes
                fos.write(iv)
                CipherOutputStream(fos, cipher).use { cos ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        cos.write(buffer, 0, bytesRead)
                    }
                    cos.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Decrypts encrypted file into a temporary decrypted file for previewing/playing.
     */
    fun decryptToTempFile(
        context: Context,
        encryptedFile: File,
        tempFile: File,
        userPin: String? = null
    ): Boolean {
        if (!encryptedFile.exists() || encryptedFile.length() <= IV_SIZE) return false
        return try {
            val key = getSecretKey(context, userPin)
            FileInputStream(encryptedFile).use { fis ->
                val iv = ByteArray(IV_SIZE)
                val ivRead = fis.read(iv)
                if (ivRead != IV_SIZE) return false

                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

                CipherInputStream(fis, cipher).use { cis ->
                    FileOutputStream(tempFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (cis.read(buffer).also { bytesRead = it } != -1) {
                            fos.write(buffer, 0, bytesRead)
                        }
                        fos.flush()
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Decrypts encrypted file into an OutputStream (for exporting media).
     */
    fun decryptToOutputStream(
        context: Context,
        encryptedFile: File,
        outputStream: OutputStream,
        userPin: String? = null
    ): Boolean {
        if (!encryptedFile.exists() || encryptedFile.length() <= IV_SIZE) return false
        return try {
            val key = getSecretKey(context, userPin)
            FileInputStream(encryptedFile).use { fis ->
                val iv = ByteArray(IV_SIZE)
                val ivRead = fis.read(iv)
                if (ivRead != IV_SIZE) return false

                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

                CipherInputStream(fis, cipher).use { cis ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (cis.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
