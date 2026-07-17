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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.feature.privacy.model

/**
 * The heart of the feature: a pure function mapping (base level, panic active) to an
 * [EffectivePrivacyLevel]. Every behaviour decision funnels through here.
 *
 * NORMAL is never escalated by Panic Mode (per product spec); Panic Mode upgrades the *behaviour*
 * of the two private tiers without changing the user's stored base level.
 */
object PrivacyResolver {

    fun resolve(
        base: ConversationPrivacyLevel,
        panicActive: Boolean,
        panicProtected: Boolean = false,
    ): EffectivePrivacyLevel {
        if (!panicActive) {
            // Panic Mode off: behave at the base level; panic-list membership has no effect.
            return when (base) {
                ConversationPrivacyLevel.NORMAL -> EffectivePrivacyLevel.NORMAL
                ConversationPrivacyLevel.SENSITIVE -> EffectivePrivacyLevel.SENSITIVE
                ConversationPrivacyLevel.HIGHLY_SENSITIVE -> EffectivePrivacyLevel.HIGHLY_SENSITIVE
            }
        }
        // Panic Mode on: panic-listed conversations are escalated to at least Sensitive.
        val escalated = if (panicProtected && base == ConversationPrivacyLevel.NORMAL) {
            ConversationPrivacyLevel.SENSITIVE
        } else {
            base
        }
        return when (escalated) {
            ConversationPrivacyLevel.NORMAL -> EffectivePrivacyLevel.NORMAL
            ConversationPrivacyLevel.SENSITIVE -> EffectivePrivacyLevel.SENSITIVE_PANIC
            ConversationPrivacyLevel.HIGHLY_SENSITIVE -> EffectivePrivacyLevel.HIGHLY_SENSITIVE_PANIC
        }
    }
}
