package com.wearezeta.auto.common.calling2.v1;

import com.wearezeta.auto.common.calling2.v1.model.NativeCallingEvent;
import com.wearezeta.auto.common.calling2.v1.model.UiCallTrackingEvent;
import com.wearezeta.auto.common.calling2.v1.model.UiCallTrackingEventGroup;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.wearezeta.auto.common.calling2.v1.model.UiCallTrackingEventGroup.*;

public class UiCallingStatTracker {

    private List<UiCallTrackingEvent> events = new ArrayList<>();

    public UiCallingStatTracker() {
    }

    public void trackEvent(UUID callId, NativeCallingEvent event) {
        events.add(new UiCallTrackingEvent(callId, event));
    }

    public List<String> getSummary() {
        List<String> summaryLines = new ArrayList<>();
        summaryLines.add("------ CALLING SUMMARY ------\n");
        List<UiCallTrackingEventGroup> groups = events.stream()
                .collect(Collectors.groupingBy(UiCallTrackingEvent::getCallId, Collectors.toList()))
                .entrySet().stream()
                .map(UiCallTrackingEventGroup::new)
                .collect(Collectors.toList());

        groups.stream()
                .map(UiCallTrackingEventGroup::render)
                .forEach(summaryLines::add);

        if (!groups.isEmpty()) {
            long incomingDeltaLong = groups.stream()
                    .map(UiCallTrackingEventGroup::getIncomingEventDelta)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(Duration::toMillis)
                    .reduce(0L, (a, b) -> a + b) / groups.size();
            long establishedDeltaLong = groups.stream()
                    .map(UiCallTrackingEventGroup::getEstablishedDelta)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(Duration::toMillis)
                    .reduce(0L, (a, b) -> a + b) / groups.size();

            summaryLines.add(String.format("Average for %s call(s):\n" +
                            "Incoming event delta: %s\n" +
                            "Establish event delta: %s\n", groups.size(), humanReadableFormat(Duration.ofMillis(incomingDeltaLong)),
                    humanReadableFormat(Duration.ofMillis(establishedDeltaLong))));
        }
        return summaryLines;
    }
}
