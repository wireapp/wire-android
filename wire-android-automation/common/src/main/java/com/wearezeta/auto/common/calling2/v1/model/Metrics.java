package com.wearezeta.auto.common.calling2.v1.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Metrics {
    
    private final boolean success;
    private final long estabTime;
    private final long setupTime;
    private final long avgRateU;
    private final long avgRateD;

    @JsonCreator
    public Metrics(@JsonProperty("success") boolean success, @JsonProperty("estab_time(ms)") long estabTime,
                   @JsonProperty("setup_time(ms)") long setupTime, @JsonProperty("avg_rate_u") long avgRateU,
                   @JsonProperty("avg_rate_d") long avgRateD) {
        this.success = success;
        this.estabTime = estabTime;
        this.setupTime = setupTime;
        this.avgRateU = avgRateU;
        this.avgRateD = avgRateD;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getEstabTime() {
        return estabTime;
    }

    public long getSetupTime() {
        return setupTime;
    }

    public long getAvgRateU() {
        return avgRateU;
    }

    public long getAvgRateD() {
        return avgRateD;
    }

    @Override
    public String toString() {
        return "Metrics{" +
                "success=" + success +
                ", estabTime=" + estabTime +
                ", setupTime=" + setupTime +
                ", avgRateU=" + avgRateU +
                ", avgRateD=" + avgRateD +
                '}';
    }
}
