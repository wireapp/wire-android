/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.feature.aiassistant

import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import com.wire.android.feature.aiassistant.model.AiPromptCapability

internal object AiMessagePromptPolicy {

    fun proofread(descriptor: AiModelDescriptor, inputText: String): PromptRequest =
        when (descriptor.promptCapability) {
            AiPromptCapability.Weak -> PromptRequest(
                userMessage = "Fix grammar, return one result only:\n\"$inputText\"",
                initialExchanges = PROOFREAD_EXAMPLES
            )

            AiPromptCapability.Capable -> PromptRequest(
                userMessage = """
                    Proofread the message below.
                    Fix grammar, spelling, punctuation, and obvious wording issues.
                    Preserve the original meaning, tone, formatting, and language unless correction requires a small change.
                    Return exactly one rewritten message and nothing else.
                    Message:
                    "$inputText"
                """.trimIndent()
            )
        }

    fun adjustTone(descriptor: AiModelDescriptor, inputText: String, toneType: AiMessageToneType): PromptRequest =
        when (descriptor.promptCapability) {
            AiPromptCapability.Weak -> PromptRequest(
                userMessage = "${toneType.toWeakInstruction()}, return one result only:\n\"$inputText\"",
                initialExchanges = toneType.toWeakExamples()
            )

            AiPromptCapability.Capable -> PromptRequest(
                userMessage = """
                    ${toneType.toCapableInstruction()}
                    Rewrite the message below with that tone.
                    Preserve the original meaning, key details, and language unless the instruction implies otherwise.
                    Keep the rewrite natural and concise.
                    Return exactly one rewritten message and nothing else.
                    Message:
                    "$inputText"
                """.trimIndent()
            )
        }

    fun customPrompt(descriptor: AiModelDescriptor, inputText: String, userPrompt: String): PromptRequest =
        when (descriptor.promptCapability) {
            AiPromptCapability.Weak -> PromptRequest(
                userMessage = "$userPrompt, return one result only:\n\"$inputText\""
            )

            AiPromptCapability.Capable -> PromptRequest(
                userMessage = """
                    Apply the following instruction to the message below:
                    "$userPrompt"
                    Rewrite the message to satisfy that instruction.
                    Preserve the original language unless the instruction explicitly asks to change it.
                    Return exactly one final rewritten message and nothing else.
                    Message:
                    "$inputText"
                """.trimIndent()
            )
        }

    data class PromptRequest(
        val userMessage: String,
        val initialExchanges: List<Pair<String, String>> = emptyList()
    )

    private val PROOFREAD_EXAMPLES = listOf(
        "Fix grammar, return one result only:\n\"She dont know nothing about it.\"" to
            "She doesn't know anything about it."
    )

    private fun AiMessageToneType.toWeakInstruction(): String = when (this) {
        AiMessageToneType.Formal -> "Rewrite more formally"
        AiMessageToneType.Informal -> "Rewrite more casually"
    }

    private fun AiMessageToneType.toCapableInstruction(): String = when (this) {
        AiMessageToneType.Formal -> "Rewrite the message in a more formal tone."
        AiMessageToneType.Informal -> "Rewrite the message in a more casual tone."
    }

    private fun AiMessageToneType.toWeakExamples(): List<Pair<String, String>> = when (this) {
        AiMessageToneType.Formal -> listOf(
            "Rewrite more formally, return one result only:\n\"Hey can u help me with this?\"" to
                "Could you please assist me with this?"
        )

        AiMessageToneType.Informal -> listOf(
            "Rewrite more casually, return one result only:\n\"I would like to request your assistance with this matter.\"" to
                "Can you help me out with this?"
        )
    }
}
