package com.wearezeta.auto.common.testiny;

import java.util.NoSuchElementException;

/**
 * https://www.testiny.io/docs/rest-api/add-remove-update-mappings-between-test-run-entities-and-other-entities/ -> result_status
 */
public enum TestinyExecutionStatus {
    Untested(1, "NOTRUN"),
    Passed(2, "PASSED"),
    Failed(3, "FAILED"),
    Blocked(4, "BLOCKED"),
    Skipped(5, "SKIPPED");

    private final int id;
    private final String status;

    public int getId() {
        return this.id;
    }

    public String getStatus() {
        return this.status;
    }

    TestinyExecutionStatus(Integer id, String status) {
        this.id = id;
        this.status = status;
    }

    @Override
    public String toString() {
        return this.name();
    }

    public static TestinyExecutionStatus getById(int id) {
        for (TestinyExecutionStatus exStatus : TestinyExecutionStatus.values()) {
            if (exStatus.getId() == id) {
                return exStatus;
            }
        }
        throw new NoSuchElementException(String.format(
                "Execution result id '%d' is unknown", id));
    }

    public static TestinyExecutionStatus getByStatus(String status) {
        for (TestinyExecutionStatus exStatus : TestinyExecutionStatus.values()) {
            if (exStatus.getStatus().equalsIgnoreCase(status)) {
                return exStatus;
            }
        }
        throw new NoSuchElementException(String.format(
                "Execution Result '%s' is unknown", status));
    }
}
