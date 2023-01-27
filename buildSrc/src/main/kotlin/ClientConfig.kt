/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

import scripts.Variants_gradle.BuildTypes

/**
 * Config fields with DEFAULT values per Build Type.
 */
enum class ConfigFields(val defaultValue: String) {
    SUPPORT_URL(""""https://support.wire.com""""),
    SENDER_ID(""""782078216207"""")
}

/**
 * Defines a map for fields per Build Type.
 */
object ClientConfig {
    val properties = mapOf(

        // Config field values for DEBUG Build Type
        BuildTypes.DEBUG to ConfigFields.values().associate { Pair(it, it.defaultValue) },
        BuildTypes.COMPAT to ConfigFields.values().associate { Pair(it, it.defaultValue) },

        // Config field values for RELEASE Build Type
        // TODO: Certificate pinning, change backend based on flavour
        BuildTypes.RELEASE to mapOf(
            ConfigFields.SUPPORT_URL to """"https://support.wire.com"""",
            ConfigFields.SENDER_ID to """"782078216207""""
        )
    )
}
