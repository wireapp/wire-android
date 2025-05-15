package com.wire.android.tests.core.messages

import com.wire.android.tests.core.utils.Timedelta
import java.util.concurrent.Future
import java.util.regex.Pattern

class ActivationMessage(msg: String) : WireMessage(msg) {

    companion object {
        val ACTIVATION_TIMEOUT: Timedelta = Timedelta.ofSeconds(120.0) // seconds
        const val MESSAGE_PURPOSE = "Activation"
        private const val ACTIVATION_LINK_REGEX = "https://[a-zA-Z_0-9.=-]+/verify/\\?key=([a-zA-Z_0-9=-]+)&code=([0-9]+)"
    }

    val xZetaKey: String?
        get() = getHeaderValue(ZETA_KEY_HEADER_NAME)

    val xZetaCode: String?
        get() = getHeaderValue(ZETA_CODE_HEADER_NAME)

    @Throws(Exception::class)
    fun getActivationLink(): String {
        val pattern = Pattern.compile(ACTIVATION_LINK_REGEX, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(content)
        return if (matcher.find()) {
            matcher.group(0)
        } else {
            throw Exception("Unable to extract URL from mail: $content")
        }
    }

   // companion object {
        @Throws(Exception::class)
        fun getMessageContent(activationMessage: Future<String>): String {
            val sentence = ActivationMessage(activationMessage.get())
            return sentence.content
        }
   // }

    override val expectedPurposeValue: String
        get() = MESSAGE_PURPOSE
}
