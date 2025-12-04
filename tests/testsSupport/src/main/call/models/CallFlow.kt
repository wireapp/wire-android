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

import com.google.gson.annotations.SerializedName

data class CallFlow(
    @SerializedName("audioPacketsReceived")
    val audioPacketsReceived: Long,

    @SerializedName("audioPacketsSent")
    val audioPacketsSent: Long,

    @SerializedName("videoPacketsReceived")
    val videoPacketsReceived: Long,

    @SerializedName("videoPacketsSent")
    val videoPacketsSent: Long,

    @SerializedName("remoteUserId")
    val remoteUserId: String
) {

    // Secondary constructor for parsing from raw pcStats string
    constructor(pcStats: String) : this(
        audioPacketsReceived = getStat(pcStats, "ar"),
        audioPacketsSent = getStat(pcStats, "as"),
        videoPacketsReceived = getStat(pcStats, "vr"),
        videoPacketsSent = getStat(pcStats, "vs"),
        remoteUserId = ""
    )

    companion object {
        private fun getStat(pcStats: String, stat: String): Long {
            // Example: "pc_set_stats: level: 0 ar: 82 vr: 80 as: 164 vs: 248 rtt=0 dloss=0"
            val regex = Regex("$stat:\\s(\\d+)")
            val match = regex.find(pcStats)
            return match?.groupValues?.getOrNull(1)?.toLongOrNull() ?: -1L
        }
    }

    fun toPrettyString(): String = buildString {
        appendLine(remoteUserId)
        appendLine(" - audio sent: $audioPacketsSent")
        appendLine(" - video sent: $videoPacketsSent")
        appendLine(" - audio recv: $audioPacketsReceived")
        appendLine(" - video recv: $videoPacketsReceived")
    }

    fun equalTo(other: CallFlow): Boolean {
        return audioPacketsReceived == other.audioPacketsReceived &&
                audioPacketsSent == other.audioPacketsSent &&
                videoPacketsReceived == other.videoPacketsReceived &&
                videoPacketsSent == other.videoPacketsSent
    }

    override fun toString(): String =
        "Flow(audioPacketsReceived=$audioPacketsReceived, audioPacketsSent=$audioPacketsSent, " +
                "videoPacketsReceived=$videoPacketsReceived, videoPacketsSent=$videoPacketsSent, " +
                "remoteUserId='$remoteUserId')"
}
