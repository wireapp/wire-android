package com.wearezeta.auto.common.calling2.v1.model;

import com.wearezeta.auto.common.log.AsciiTable;
import com.wearezeta.auto.common.Config;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class UiCallTrackingEventGroup {
    private UUID id;
    private List<UiCallTrackingEvent> events;
    private final String CALL_DASHBOARD_DATA_FILE = "dashboard_data.txt";

    public UiCallTrackingEventGroup(Map.Entry<UUID, List<UiCallTrackingEvent>> eventsEntry) {
        id = eventsEntry.getKey();
        events = eventsEntry.getValue();
    }

    public Optional<Duration> getIncomingEventDelta() {
        return getDeltaBetweenEvents(NativeCallingEvent.start, NativeCallingEvent.incomingh);
    }

    public Optional<Duration> getEstablishedDelta() {
        return getDeltaBetweenEvents(NativeCallingEvent.answer, NativeCallingEvent.dcestabh);
    }

    private Optional<Duration> getDeltaBetweenEvents(NativeCallingEvent first, NativeCallingEvent second) {
        Optional<UiCallTrackingEvent> startOptional =
                events.stream().filter(event -> event.getEvent().equals(first))
                        .findFirst();
        if (!startOptional.isPresent()) {
            return Optional.empty();
        }
        UiCallTrackingEvent start = startOptional.get();
        Optional<UiCallTrackingEvent> incomingOptional =
                events.stream().filter(event -> event.getEvent().equals(second))
                        .findFirst();
        if (!incomingOptional.isPresent()) {
            return Optional.empty();
        }
        UiCallTrackingEvent incoming = incomingOptional.get();
        long millisDelta = start.getTimestamp().until(incoming.getTimestamp(), ChronoUnit.MILLIS);
        return Optional.of(Duration.ofMillis(millisDelta));
    }

    public String render() {
        if (events.isEmpty()) {
            return String.format("No data recorded in group [%s]", id);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        AsciiTable table = new AsciiTable();
        table.setMaxColumnWidth(45);

        List<AsciiTable.Column> columns = table.getColumns();
        columns.add(new AsciiTable.Column("event"));
        columns.add(new AsciiTable.Column("time"));
        columns.add(new AsciiTable.Column("incoming delta"));
        columns.add(new AsciiTable.Column("established delta"));

        for (UiCallTrackingEvent event : events) {
            AsciiTable.Row row = new AsciiTable.Row();
            table.getData().add(row);
            row.getValues().add(event.getEvent().name());
            row.getValues().add(event.getTimestamp().format(formatter));
            printEventDelta(row, event, NativeCallingEvent.incomingh, this::getIncomingEventDelta);
            printEventDelta(row, event, NativeCallingEvent.dcestabh, this::getEstablishedDelta);
        }

        try {
            String dashBoardData = new String(String.format("%d %d\n", getIncomingEventDelta().get().toMillis(), getEstablishedDelta().get().toMillis()));

            File callDashboardDataFile = new File(Paths.get(Config.current().getBuildPath(UiCallTrackingEventGroup.class)).toString() + "/" +  CALL_DASHBOARD_DATA_FILE);
            if( callDashboardDataFile.exists() ) {
                Files.write(Paths.get(Config.current().getBuildPath(UiCallTrackingEventGroup.class), CALL_DASHBOARD_DATA_FILE), dashBoardData.getBytes(), StandardOpenOption.APPEND);
            } else {
                Files.write(Paths.get(Config.current().getBuildPath(UiCallTrackingEventGroup.class), CALL_DASHBOARD_DATA_FILE), dashBoardData.getBytes());
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to write dashboard_data.txt", e);
        }

        table.calculateColumnWidth();
        String render = table.render();

        return String.format("Call [%s]:\n%s", events.get(0).getCallId(), render);
    }

    private void printEventDelta(AsciiTable.Row row,
                                 UiCallTrackingEvent event,
                                 NativeCallingEvent expectedEvent,
                                 Supplier<Optional<Duration>> successAction) {
        if (event.getEvent().equals(expectedEvent)) {
            Optional<Duration> delta = successAction.get();
            row.getValues().add(delta.map(UiCallTrackingEventGroup::humanReadableFormat).orElse(""));
        } else {
            row.getValues().add("");
        }
    }

    public static String humanReadableFormat(Duration duration) {
        return duration.toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }
}
