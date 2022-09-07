package com.nice.cxonechat.sample

import java.lang.Exception
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

object PKCEConstants {
    const val BITS_IN_CHAR = 6
    const val BITS_IN_OCTET = 8
    const val MIN_CODE_VERIFIER_LENGTH = 43
    const val MAX_CODE_VERIFIER_LENGTH = 128
}

enum class PKCEError {
    INVALID_CODE_VERIFIER_LENGTH,
    FAILED_TO_GENERATE_RANDOM_OCTETS,
    FAILED_TO_CREATE_CODE_CHALLENGE
}

fun generateCodeVerifier(length: Int = PKCEConstants.MAX_CODE_VERIFIER_LENGTH): String {
    if (length < PKCEConstants.MIN_CODE_VERIFIER_LENGTH || length > PKCEConstants.MAX_CODE_VERIFIER_LENGTH) {
        throw Exception("Invalid length for code verifier. Must be greater than 43 and less than 128.")
    }
    val octetCount = length * PKCEConstants.BITS_IN_CHAR / PKCEConstants.BITS_IN_OCTET
    val octets = generateRandomOctets(octetCount)
    return encodeBase64URLString(octets)
}

fun generateCodeChallenge(codeVerifier: String): String {
    val challengeBytes = codeVerifier.toByteArray()
    val messageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(challengeBytes, 0, challengeBytes.size)
    return encodeBase64URLString(messageDigest.digest())
}

private fun generateRandomOctets(octetCount: Int): ByteArray {
    val octets = ByteArray(octetCount)
    val secureRandom = SecureRandom()
    secureRandom.nextBytes(octets)
    return octets
}

private fun encodeBase64URLString(octets: ByteArray): String {
    return Base64.getUrlEncoder().encodeToString(octets)
        .replace("=", "")
        .replace("+", "-")
        .replace("/", "_")
}
