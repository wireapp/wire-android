package com.wearezeta.auto.common.calling2.v1.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Flow {

    private final long audioPacketsReceived;
    private final long audioPacketsSent;
    private final long videoPacketsReceived;
    private final long videoPacketsSent;
    private final String remoteUserId;

    @JsonCreator
    public Flow(@JsonProperty("audioPacketsReceived") long audioPacketsReceived,
                @JsonProperty("audioPacketsSent") long audioPacketsSent,
                @JsonProperty("videoPacketsReceived") long videoPacketsReceived,
                @JsonProperty("videoPacketsSent") long videoPacketsSent,
                @JsonProperty("remoteUserId") String remoteUserId) {
        this.audioPacketsReceived = audioPacketsReceived;
        this.audioPacketsSent = audioPacketsSent;
        this.videoPacketsReceived = videoPacketsReceived;
        this.videoPacketsSent = videoPacketsSent;
        this.remoteUserId = remoteUserId;
    }

    public Flow(String pcStats) {
        this.audioPacketsReceived = getStat(pcStats, "ar");
        this.videoPacketsReceived = getStat(pcStats, "vr");
        this.audioPacketsSent = getStat(pcStats, "as");
        this.videoPacketsSent = getStat(pcStats, "vs");
        this.remoteUserId = "";
    }

    private Long getStat(String pcStats, String stat) {
        // pc_set_stats: level: 0 ar: 82 vr: 80 as: 164 vs: 248 rtt=0 dloss=0" 0
        Pattern pattern = Pattern.compile(String.format("%s:\\s([\\d]+)", stat));
        Matcher matcher = pattern.matcher(pcStats);
        return (matcher.find())
                ? Long.parseLong(matcher.group(1))
                : -1;
    }

    public long getAudioPacketsReceived() {
        return audioPacketsReceived;
    }

    public long getAudioPacketsSent() {
        return audioPacketsSent;
    }

    public long getVideoPacketsReceived() {
        return videoPacketsReceived;
    }

    public long getVideoPacketsSent() {
        return videoPacketsSent;
    }

    public String getRemoteUserId() {
        return remoteUserId;
    }

    @Override
    public String toString() {
        return String.format("Flow {audioPacketsReceived=%d, audioPacketsSent=%d, videoPacketsReceived=%d, videoPacketsSent=%d"
                + ", remoteUserId=%s}", audioPacketsReceived, audioPacketsSent, videoPacketsReceived, videoPacketsSent,
                remoteUserId);
    }

    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getRemoteUserId());
        sb.append("\n");
        sb.append(" - audio sent: ");
        sb.append(this.getAudioPacketsSent());
        sb.append("\n");
        sb.append(" - video sent: ");
        sb.append(this.getVideoPacketsSent());
        sb.append("\n");
        sb.append(" - audio recv: ");
        sb.append(this.getAudioPacketsReceived());
        sb.append("\n");
        sb.append(" - video recv: ");
        sb.append(this.getVideoPacketsReceived());
        sb.append("\n");
        return sb.toString();
    }

    public boolean equalTo(Flow f) {
        return this.getAudioPacketsReceived() == f.getAudioPacketsReceived()
                && this.getAudioPacketsSent() == f.getAudioPacketsSent()
                && this.getVideoPacketsReceived() == f.getVideoPacketsReceived()
                && this.getVideoPacketsSent() == f.getVideoPacketsSent();
    }
}