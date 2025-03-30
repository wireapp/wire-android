package com.wearezeta.auto.common.misc;

public class EphemeralTimeConverter {

    public static long asMillis(String timeAsString) {
        switch (timeAsString) {
            case "Off":
                return 0;
            case "10 seconds":
                return 10 * 1000;
            case "23 seconds":
                // custom timer for tests of custom timers only
                return 23 * 1000;
            case "5 minutes":
                return 5 * 60 * 1000;
            case "1 hour":
                return 60 * 60 * 1000;
            case "1 day":
                return 24 * 60 * 60 * 1000;
            case "1 week":
                return 7 * 24 * 60 * 60 * 1000;
            case "4 weeks":
                return (long) 28 * 24 * 60 * 60 * 1000;
            default:
                throw new IllegalArgumentException("Not a valid timer.");
        }
    }

    public static long convertCustomEphemeralTimerToMillis(String msgTimer) {
        long customTimerInMillies = 0;
        String[] splitTimer = msgTimer.split(" ");
        int time = Integer.parseInt(splitTimer[0]);
        String unit = splitTimer[1];
        if (unit.contains("sec")) {
            customTimerInMillies = time * 1000;
        } else if (unit.contains("min")) {
            customTimerInMillies = time * 1000 * 60;
        } else if (unit.contains("hour")) {
            customTimerInMillies = time * 1000 * 60 * 60;
        } else if (unit.contains("day")) {
            customTimerInMillies = time * 1000 * 60 * 60 * 24;
        } else if (unit.contains("week")) {
            customTimerInMillies = time * 1000 * 60 * 60 * 24 * 7;
        }
        return customTimerInMillies;
    }
}
