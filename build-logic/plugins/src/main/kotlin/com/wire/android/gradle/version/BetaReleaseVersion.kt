package com.wire.android.gradle.version

object BetaReleaseVersion {
    fun resolve(baseVersion: String, releaseVersion: String?): String? {
        val version = releaseVersion?.takeIf { it.isNotBlank() }?.removePrefix("v") ?: return null
        require(Regex("${Regex.escape(baseVersion)}-beta\\.[1-9][0-9]*").matches(version)) {
            "WIRE_RELEASE_VERSION must match $baseVersion-beta.N without leading zeros"
        }
        return version
    }
}
