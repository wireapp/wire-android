/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package call.models

data class VersionedInstanceType(
    val name: String,
    val version: String
) {

    fun getPath(): String {
        return if (isOSX()) {
            when {
                name.contains("firefox", ignoreCase = true) ->
                    "/$name/Contents/MacOS/firefox"

                name.contains("chrome", ignoreCase = true) ->
                    "/$name/Contents/MacOS/Google Chrome"

                else -> "/${name}-${version}/zcall"
            }
        } else {
            when (name.lowercase()) {
                "chrome" -> "/${name}-${version}/google-chrome"
                else -> "/${name}-${version}/$name"
            }
        }
    }

    fun isFirefox(): Boolean = name.contains("firefox", ignoreCase = true)

    fun isChrome(): Boolean = name.contains("chrome", ignoreCase = true)

    fun isZCall(): Boolean = name.contains("zcall", ignoreCase = true)

    private fun isOSX(): Boolean = System.getProperty("os.name").contains("OS X")

    companion object {
        fun parse(text: String): VersionedInstanceType {
            val parts = text.split("-", limit = 2)
            return if (parts.size == 2) {
                VersionedInstanceType(parts[0], parts[1])
            } else {
                VersionedInstanceType("", "")
            }
        }
    }

    override fun toString(): String {
        return "VersionedInstanceType(name=$name, version=$version)"
    }
}
