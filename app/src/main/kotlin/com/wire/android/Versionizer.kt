package com.wire.android

import java.time.*

class Versionizer(private val localDateTime: LocalDateTime = LocalDateTime.now()) {

    val versionCode = generateVersionCode()

    private fun generateVersionCode(): Int {
        val timeInSeconds = localDateTime.atZone(ZoneOffset.UTC).toEpochSecond()
        return (timeInSeconds.toInt()..MAX_VERSION_CODE_ALLOWED).first()
    }

    companion object {
        //This is Google Play Max Version Code allowed
        //https://developer.android.com/studio/publish/versioning
        const val MAX_VERSION_CODE_ALLOWED = 2100000000
    }
}
