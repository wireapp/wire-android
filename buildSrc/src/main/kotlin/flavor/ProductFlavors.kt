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
package flavor

object FlavorDimensions {
    const val DEFAULT = "default"
}

sealed class ProductFlavors(
    val buildName: String,
    val appName: String,
    val versionNameSuffix: String = "-${buildName}",
    val dimensions: String = FlavorDimensions.DEFAULT,
    val shareduserId: String = ""
) {
    override fun toString(): String = this.buildName

    object Dev : ProductFlavors("dev", "Wire Dev")
    object Staging : ProductFlavors("staging", "Wire Staging")

    object Beta : ProductFlavors("beta", "Wire Beta")
    object Internal : ProductFlavors("internal", "Wire Internal")
    object Production : ProductFlavors("prod", "Wire", shareduserId = "com.waz.userid")
    object Bkp : ProductFlavors("bkp", "Wire Bkp")
    object Fdroid : ProductFlavors(
        buildName = "fdroid",
        appName = "Wire",
        shareduserId = "com.waz.userid",
        versionNameSuffix = ""
    )

    companion object {
        val all: Collection<ProductFlavors> = setOf(
            Dev,
            Staging,
            Beta,
            Internal,
            Production,
            Bkp,
            Fdroid,
        )
    }
}
