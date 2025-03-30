package com.wearezeta.auto.common.misc;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class Timedelta implements Comparable<Timedelta> {
    private static final int MILLISECONDS_IN_SECOND = 1000;
    private static final int SECONDS_IN_MINUTE = 60;
    private static final int MINUTES_IN_HOUR = 60;
    private static final int HOURS_IN_DAY = 24;

    private static final long CACHE_RANGE =
            HOURS_IN_DAY * MINUTES_IN_HOUR * SECONDS_IN_MINUTE * MILLISECONDS_IN_SECOND;

    private long milliSeconds;

    private static final Map<Long, Timedelta> cache = new HashMap<>();

    private Timedelta(double milliSeconds) {
        this.milliSeconds = (long) milliSeconds;
    }

    private static synchronized Timedelta getInstance(double milliSeconds) {
        if (-CACHE_RANGE <= milliSeconds && milliSeconds <= CACHE_RANGE) {
            final long milliSecondsLong = (long) milliSeconds;
            if (!cache.containsKey(milliSecondsLong)) {
                cache.put(milliSecondsLong, new Timedelta(milliSeconds));
            }
            return cache.get(milliSecondsLong);
        }
        return new Timedelta(milliSeconds);
    }

    public static Timedelta ofSeconds(double seconds) {
        return getInstance(seconds * MILLISECONDS_IN_SECOND);
    }

    public int asSeconds() {
        return (int) (this.milliSeconds / MILLISECONDS_IN_SECOND);
    }

    public double asFloatSeconds() {
        return this.milliSeconds * 1.0 / MILLISECONDS_IN_SECOND;
    }

    public static Timedelta ofMillis(double milliSeconds) {
        return getInstance(milliSeconds);
    }

    public long asMillis() {
        return this.milliSeconds;
    }

    public static Timedelta ofMinutes(double minutes) {
        return getInstance(minutes * SECONDS_IN_MINUTE * MILLISECONDS_IN_SECOND);
    }

    public static Timedelta ofDays(double days) {
        return getInstance(days * HOURS_IN_DAY * MINUTES_IN_HOUR * SECONDS_IN_MINUTE * MILLISECONDS_IN_SECOND);
    }

    public int asMinutes() {
        return (int) this.asFloatMinutes();
    }

    public double asFloatMinutes() {
        return this.milliSeconds * 1.0 / SECONDS_IN_MINUTE / MILLISECONDS_IN_SECOND;
    }

    public Duration asDuration() {
        return Duration.ofMillis(this.milliSeconds);
    }

    public static Timedelta ofDuration(Duration d) {
        return getInstance(d.toMillis());
    }

//    public static Timedelta fromHours(double hours) {
//        return getInstance(hours * MINUTES_IN_HOUR * SECONDS_IN_MINUTE * MILLISECONDS_IN_SECOND);
//    }

    private int asHours() {
        return (int) this.asFloatHours();
    }

    private double asFloatHours() {
        return this.milliSeconds * 1.0 / MINUTES_IN_HOUR / SECONDS_IN_MINUTE / MILLISECONDS_IN_SECOND;
    }

    public Timedelta sum(Timedelta other) {
        return new Timedelta(this.milliSeconds + other.milliSeconds);
    }

    public Timedelta diff(Timedelta other) {
        return new Timedelta(this.milliSeconds - other.milliSeconds);
    }

    public boolean isInRange(Timedelta minIncluding, Timedelta maxExcluding) {
        return this.compareTo(minIncluding) >= 0 && this.compareTo(maxExcluding) < 0;
    }

    public static Timedelta now() {
        return new Timedelta(System.currentTimeMillis());
    }

    public boolean isDiffGreaterOrEqual(Timedelta other, Timedelta maxDelta) {
        return this.diff(other).compareTo(maxDelta) >= 0;
    }

    public boolean isDiffGreater(Timedelta other, Timedelta maxDelta) {
        return this.diff(other).compareTo(maxDelta) > 0;
    }

    public boolean isDiffLessOrEqual(Timedelta other, Timedelta maxDelta) {
        return this.diff(other).compareTo(maxDelta) <= 0;
    }

    public boolean isDiffLess(Timedelta other, Timedelta maxDelta) {
        return this.diff(other).compareTo(maxDelta) < 0;
    }

    public Timedelta sleep() {
        try {
            Thread.sleep(milliSeconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof Timedelta) &&
                (Math.abs(((Timedelta) other).asMillis()) == Math.abs(this.asMillis()));
    }

    @Override
    public String toString() {
        final int hours = this.asHours();
        final int minutes = this.asMinutes() % MINUTES_IN_HOUR;
        final int seconds = this.asSeconds() % SECONDS_IN_MINUTE;
        final long milliSeconds = this.asMillis() % MILLISECONDS_IN_SECOND;
        if (hours != 0) {
            return String.format((this.milliSeconds < 0 ? "- " : "") + "%02d h:%02d m:%02d s::%d ms",
                    Math.abs(hours), Math.abs(minutes), Math.abs(seconds), Math.abs(milliSeconds));
        } else if (minutes != 0) {
            return String.format((this.milliSeconds < 0 ? "- " : "") + "%02d m:%02d s::%d ms",
                    Math.abs(minutes), Math.abs(seconds), Math.abs(milliSeconds));
        } else if (seconds != 0) {
            return String.format((this.milliSeconds < 0 ? "- " : "") + "%02d s::%d ms",
                    Math.abs(seconds), Math.abs(milliSeconds));
        }
        return String.format("%d ms", milliSeconds);
    }

    @Override
    public int compareTo(Timedelta o) {
        if (o == null) {
            throw new NullPointerException("null value is not comparable");
        }
        if (this.equals(o)) {
            return 0;
        }
        if (Math.abs(this.milliSeconds) > Math.abs(o.milliSeconds)) {
            return 1;
        }
        return -1;
    }
}
