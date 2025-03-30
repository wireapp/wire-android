package com.wearezeta.auto.common.log;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogListener {
    private final List<String> logs = new CopyOnWriteArrayList<>();

    public LogListener() {}

    public void addLogMessage(String logLine) {
        logs.add(logLine);
    }

    public List<String> getLogs() {
        return Collections.unmodifiableList(logs);
    }
}
