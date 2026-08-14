/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.debug.securityproviders

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.os.Build
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec

class KeyAttestationDiagnosticsProvider {

    fun collect(): KeyAttestationDiagnostics = try {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        if (keyStore.containsAlias(DIAGNOSTIC_KEY_ALIAS)) {
            keyStore.deleteEntry(DIAGNOSTIC_KEY_ALIAS)
        }

        val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM_EC, ANDROID_KEY_STORE)
        keyPairGenerator.initialize(
            KeyGenParameterSpec.Builder(
                DIAGNOSTIC_KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec(CURVE_SECP256R1))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAttestationChallenge(ByteArray(ATTESTATION_CHALLENGE_LENGTH).also(SecureRandom()::nextBytes))
                .build()
        )
        keyPairGenerator.generateKeyPair()

        val certificateChain = keyStore.getCertificateChain(DIAGNOSTIC_KEY_ALIAS).orEmpty()
        val x509Certificates = certificateChain.filterIsInstance<X509Certificate>()
        val certificates = x509Certificates.mapIndexed { index, certificate -> certificate.toDiagnostic(index) }

        KeyAttestationDiagnostics.Success(
            rawCertificateChainLength = certificateChain.size,
            certificates = certificates,
            attestation = x509Certificates.firstOrNull()?.attestationExtension(),
            key = inspectDiagnosticKey(keyStore),
        )
    } catch (error: Exception) {
        KeyAttestationDiagnostics.Failure(
            error.javaClass.simpleName,
            error.message.orEmpty()
        )
    }

    private fun X509Certificate.toDiagnostic(index: Int): AttestationCertificate = AttestationCertificate(
        index = index,
        subject = subjectX500Principal.name,
        issuer = issuerX500Principal.name,
        signatureAlgorithm = sigAlgName,
        sha256Fingerprint = MessageDigest.getInstance(SHA_256)
            .digest(encoded)
            .toFingerprint(),
    )

    private fun ByteArray.toFingerprint(): String = joinToString(":") { byte ->
        "%02X".format(byte.toInt() and 0xff)
    }

    private fun inspectDiagnosticKey(keyStore: KeyStore): KeyInspection {
        return try {
            val entry = keyStore.getEntry(DIAGNOSTIC_KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
                ?: return KeyInspection.Unavailable("Diagnostic attestation key", "Key entry unavailable")
            val algorithm = entry.privateKey.algorithm
            val keyInfo = KeyFactory
                .getInstance(algorithm, ANDROID_KEY_STORE)
                .getKeySpec(entry.privateKey, KeyInfo::class.java) as KeyInfo
            KeyInspection.Available(
                label = "Diagnostic attestation key",
                algorithm = algorithm,
                securityLevel = keyInfo.securityLevelName(),
            )
        } catch (error: Exception) {
            KeyInspection.Unavailable("Diagnostic attestation key", "${error.javaClass.simpleName}: ${error.message.orEmpty()}")
        }
    }

    private fun KeyInfo.securityLevelName(): String = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> when (securityLevel) {
            KeyProperties.SECURITY_LEVEL_SOFTWARE -> "Software"
            KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> "Trusted Environment"
            KeyProperties.SECURITY_LEVEL_STRONGBOX -> "StrongBox"
            else -> "Unknown ($securityLevel)"
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && optionalBooleanProperty("isStrongBoxBacked") == true -> "StrongBox"
        isInsideSecureHardware -> "Secure hardware (TEE level unavailable)"
        else -> "Software"
    }

    private fun KeyInfo.optionalBooleanProperty(methodName: String): Boolean? = runCatching {
        javaClass.getMethod(methodName).invoke(this) as Boolean
    }.getOrNull()

    private fun X509Certificate.attestationExtension(): AttestationExtension? = runCatching {
        AndroidKeyAttestationParser.parse(getExtensionValue(ANDROID_KEY_ATTESTATION_OID) ?: return null)
    }.getOrNull()

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val DIAGNOSTIC_KEY_ALIAS = "wire_security_attestation_diagnostic_key"
        const val ALGORITHM_EC = "EC"
        const val CURVE_SECP256R1 = "secp256r1"
        const val SHA_256 = "SHA-256"
        const val ATTESTATION_CHALLENGE_LENGTH = 32
        const val ANDROID_KEY_ATTESTATION_OID = "1.3.6.1.4.1.11129.2.1.17"
    }
}

sealed interface KeyAttestationDiagnostics {
    data class Success(
        val rawCertificateChainLength: Int,
        val certificates: List<AttestationCertificate>,
        val attestation: AttestationExtension?,
        val key: KeyInspection,
    ) : KeyAttestationDiagnostics

    data class Failure(
        val exceptionType: String,
        val message: String,
    ) : KeyAttestationDiagnostics
}

data class AttestationCertificate(
    val index: Int,
    val subject: String,
    val issuer: String,
    val signatureAlgorithm: String,
    val sha256Fingerprint: String,
)

sealed interface KeyInspection {
    val label: String

    data class Available(
        override val label: String,
        val algorithm: String,
        val securityLevel: String,
    ) : KeyInspection

    data class Unavailable(override val label: String, val reason: String) : KeyInspection
}

data class AttestationExtension(
    val attestationSecurityLevel: String,
    val keymasterSecurityLevel: String,
    val verifiedBootState: String?,
)

/** Minimal DER reader for the Android Key Attestation extension. */
private object AndroidKeyAttestationParser {
    fun parse(extension: ByteArray): AttestationExtension {
        val fields = extension.readKeyDescriptionFields()
        require(fields.size >= KEY_DESCRIPTION_FIELD_COUNT) { "Key attestation extension is incomplete" }

        val rootOfTrust = fields[TEE_ENFORCED_INDEX].children().rootOfTrust()

        return AttestationExtension(
            attestationSecurityLevel = fields[ATTESTATION_SECURITY_LEVEL_INDEX].securityLevelName(),
            keymasterSecurityLevel = fields[KEYMASTER_SECURITY_LEVEL_INDEX].securityLevelName(),
            verifiedBootState = rootOfTrust?.getOrNull(VERIFIED_BOOT_STATE_INDEX)?.verifiedBootStateName(),
        )
    }

    private fun ByteArray.readKeyDescriptionFields(): List<DerValue> {
        val keyDescription = DerReader(this)
            .readValue()
            .requireUniversalTag(DER_TAG_OCTET_STRING)
            .content
        return DerReader(keyDescription)
            .readValue()
            .requireUniversalTag(DER_TAG_SEQUENCE)
            .children()
    }

    private fun List<DerValue>.rootOfTrust(): List<DerValue>? = firstOrNull {
        it.isContextSpecificTag(ROOT_OF_TRUST_TAG)
    }?.content?.let { encodedRootOfTrust ->
        DerReader(encodedRootOfTrust)
            .readValue()
            .requireUniversalTag(DER_TAG_SEQUENCE)
            .children()
    }

    private fun DerValue.securityLevelName(): String {
        val securityLevel = integerValue()
        return when (securityLevel) {
            0 -> "Software"
            1 -> "Trusted Environment"
            2 -> "StrongBox"
            else -> "Unknown ($securityLevel)"
        }
    }

    private fun DerValue.verifiedBootStateName(): String {
        val verifiedBootState = integerValue()
        return when (verifiedBootState) {
            0 -> "Verified"
            1 -> "Self-signed"
            2 -> "Unverified"
            3 -> "Failed"
            else -> "Unknown ($verifiedBootState)"
        }
    }

    private fun DerValue.integerValue(): Int = content.fold(0) { value, byte ->
        (value shl BITS_PER_BYTE) or (byte.toInt() and BYTE_MASK)
    }

    private fun DerValue.requireUniversalTag(expectedTagNumber: Int): DerValue {
        require(tagClass == DER_TAG_CLASS_UNIVERSAL && tagNumber == expectedTagNumber) {
            "Unexpected DER tag"
        }
        return this
    }

    private fun DerValue.isContextSpecificTag(expectedTagNumber: Int): Boolean =
        tagClass == DER_TAG_CLASS_CONTEXT_SPECIFIC && tagNumber == expectedTagNumber

    private const val DER_TAG_CLASS_UNIVERSAL = 0x00
    private const val DER_TAG_CLASS_CONTEXT_SPECIFIC = 0x80
    private const val DER_TAG_OCTET_STRING = 4
    private const val DER_TAG_SEQUENCE = 16
    private const val ROOT_OF_TRUST_TAG = 704
    private const val ATTESTATION_SECURITY_LEVEL_INDEX = 1
    private const val KEYMASTER_SECURITY_LEVEL_INDEX = 3
    private const val TEE_ENFORCED_INDEX = 7
    private const val VERIFIED_BOOT_STATE_INDEX = 2
    private const val KEY_DESCRIPTION_FIELD_COUNT = 8
    private const val BITS_PER_BYTE = 8
    private const val BYTE_MASK = 0xff
}

private class DerReader(private val bytes: ByteArray) {
    private var position = 0

    fun readValue(): DerValue {
        require(position < bytes.size) { "Unexpected end of DER data" }
        val firstTagByte = readUnsignedByte()
        val tagClass = firstTagByte and 0xc0
        val tagNumber = readTagNumber(firstTagByte and 0x1f)
        val length = readLength()
        require(length <= bytes.size - position) { "Invalid DER length" }
        val content = bytes.copyOfRange(position, position + length)
        position += length
        return DerValue(tagClass, tagNumber, content)
    }

    fun readAllValues(): List<DerValue> = buildList {
        while (position < bytes.size) add(readValue())
    }

    private fun readTagNumber(initialValue: Int): Int {
        if (initialValue != 0x1f) return initialValue
        var value = 0
        while (true) {
            val byte = readUnsignedByte()
            value = (value shl 7) or (byte and 0x7f)
            if (byte and 0x80 == 0) return value
        }
    }

    private fun readLength(): Int {
        val firstLengthByte = readUnsignedByte()
        if (firstLengthByte and 0x80 == 0) return firstLengthByte
        val lengthByteCount = firstLengthByte and 0x7f
        require(lengthByteCount in 1..4) { "Unsupported DER length" }
        var length = 0
        repeat(lengthByteCount) { length = (length shl 8) or readUnsignedByte() }
        return length
    }

    private fun readUnsignedByte(): Int {
        require(position < bytes.size) { "Unexpected end of DER data" }
        return bytes[position++].toInt() and 0xff
    }
}

private data class DerValue(
    val tagClass: Int,
    val tagNumber: Int,
    val content: ByteArray,
) {
    fun children(): List<DerValue> = DerReader(content).readAllValues()
}
