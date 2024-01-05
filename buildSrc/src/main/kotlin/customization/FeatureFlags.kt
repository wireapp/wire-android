/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package customization

import flavor.ProductFlavors

/**
 * By convention use the prefix FEATURE_ for every
 * defined functionality that will be under a feature flag.
 */
enum class Features {
    FEATURE_SEARCH,
    FEATURE_CONVERSATIONS
}

/**
 * Defines a map for activated flags per product flavor.
 */
object FeatureFlags {
    val activated = mapOf(

        //Enabled Features for DEV Product Flavor
        ProductFlavors.Dev to setOf(
            Features.FEATURE_SEARCH,
            Features.FEATURE_CONVERSATIONS
        ),

        //Enabled Features for INTERNAL Product Flavor
        ProductFlavors.Internal to setOf(
            Features.FEATURE_CONVERSATIONS
        )
    )
}
