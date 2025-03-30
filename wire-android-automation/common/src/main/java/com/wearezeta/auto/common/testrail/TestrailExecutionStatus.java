package com.wearezeta.auto.common.testrail;

import java.util.NoSuchElementException;

/**
 * http://docs.gurock.com/testrail-api2/reference-results#add_result
 */
public enum TestrailExecutionStatus {
    Untested(3), Passed(1), Blocked(2), Retest(4), Failed(5), Known(6);

    private final int id;

    public int getId() {
        return this.id;
    }

    TestrailExecutionStatus(Integer id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.name();
    }

    public static TestrailExecutionStatus getById(int id) {
        for (TestrailExecutionStatus status : TestrailExecutionStatus.values()) {
            if (status.getId() == id) {
                return status;
            }
        }
        throw new NoSuchElementException(String.format(
                "Execution result id '%d' is unknown", id));
    }

    public static TestrailExecutionStatus getByName(String resultName) {
        for (TestrailExecutionStatus result : TestrailExecutionStatus.values()) {
            if (result.name().equalsIgnoreCase(resultName)) {
                return result;
            }
        }
        throw new NoSuchElementException(String.format(
                "Execution Result '%s' is unknown", resultName));
    }
}
