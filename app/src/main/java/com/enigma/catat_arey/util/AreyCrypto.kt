package com.enigma.catat_arey.util

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object AreyCrypto {
    private const val AES_KEY_SIZE_BITS = 256
    private const val GCM_TAG_SIZE_BITS = 128
    const val GCM_IV_SIZE = 12

    fun getDefaultAAD(): String = "meteorite_fall"

    private fun getBiometricKeyAlias(): String = "biohazard"

    private fun getDefaultKeyAlias(): String = "safehazard"

    private fun getAesGCMCipher(): Cipher = Cipher.getInstance("AES/GCM/NoPadding")

    private fun getDefaultAESBiometricKeyGenSpec(keyAlias: String): KeyGenParameterSpec {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(AES_KEY_SIZE_BITS)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(
                    86400,
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                )
                .setInvalidatedByBiometricEnrollment(false)
                .build()
        } else {
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(AES_KEY_SIZE_BITS)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(false)
                .build()
        }
    }

    private fun getDefaultAESKeyGenSpec(keyAlias: String): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(AES_KEY_SIZE_BITS)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

    fun getDefaultEncryptionCipher(): Cipher {
        val cipher = getAesGCMCipher()
        val secretKey = getDefaultKeystoreSecretKey(getDefaultKeyAlias())
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        return cipher
    }

    fun getDefaultDecryptionCipher(iv: ByteArray): Cipher {
        if (iv.size != GCM_IV_SIZE) throw IllegalArgumentException("improper IV size, this shouldn't happen.")

        val cipher = getAesGCMCipher()
        val secretKey = getDefaultKeystoreSecretKey(getDefaultKeyAlias())
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))

        return cipher
    }

    fun getBiometricDecryptionCipher(iv: ByteArray): Cipher {
        if (iv.size != GCM_IV_SIZE) throw IllegalArgumentException("improper IV size, this shouldn't happen.")

        val cipher = getAesGCMCipher()
        val secretKey = getBiometricKeystoreSecretKey(getBiometricKeyAlias())
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))

        return cipher
    }

    fun getBiometricEncryptionCipher(): Cipher {
        val cipher = getAesGCMCipher()
        val secretKey = getBiometricKeystoreSecretKey(getBiometricKeyAlias())
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        return cipher
    }

    fun aesGcmEncrypt(cipher: Cipher, envelope: GCMEnvelope): GCMEnvelope {
        if (cipher.algorithm == null) throw IllegalArgumentException("Not a cipher algorithm, this shouldn't happen.")
        if (cipher.algorithm != getAesGCMCipher().algorithm) throw IllegalArgumentException("Only GCM allowed, this shouldn't happen.")
        if (cipher.iv == null) throw IllegalArgumentException("Cipher not initialized, this shouldn't happen.")
        if (envelope.aad.isEmpty()) throw IllegalArgumentException("empty AAD, this shouldn't happen.")
        if (envelope.data.isEmpty()) throw IllegalArgumentException("empty data, this shouldn't happen.")

        cipher.updateAAD(envelope.aad)

        val ret = GCMEnvelope()

        ret.data = cipher.doFinal(envelope.data)
        ret.iv = cipher.iv

        return ret
    }

    fun aesGcmDecrypt(cipher: Cipher, envelope: GCMEnvelope): ByteArray {
        if (cipher.algorithm == null) throw IllegalArgumentException("Not a cipher algorithm, this shouldn't happen.")
        if (cipher.algorithm != getAesGCMCipher().algorithm) throw IllegalArgumentException("Only GCM allowed, this shouldn't happen.")
        if (envelope.iv.size != GCM_IV_SIZE) throw IllegalArgumentException("improper IV size, this shouldn't happen.")
        if (envelope.aad.isEmpty()) throw IllegalArgumentException("empty AAD, this shouldn't happen.")
        if (envelope.data.isEmpty()) throw IllegalArgumentException("empty data, this shouldn't happen.")

        cipher.updateAAD(envelope.aad)

        return cipher.doFinal(envelope.data)
    }

    private fun getDefaultKeystoreSecretKey(keyAlias: String): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        ks.getKey(keyAlias, null)?.let { return it as SecretKey }

        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(getDefaultAESKeyGenSpec(keyAlias))

        return keyGenerator.generateKey()
    }

    private fun getBiometricKeystoreSecretKey(keyAlias: String): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        ks.getKey(keyAlias, null)?.let { return it as SecretKey }

        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(getDefaultAESBiometricKeyGenSpec(keyAlias))

        return keyGenerator.generateKey()
    }
}

data class GCMEnvelope(
    var iv: ByteArray = ByteArray(0),
    var data: ByteArray = ByteArray(0),
    var aad: ByteArray = ByteArray(0)
)