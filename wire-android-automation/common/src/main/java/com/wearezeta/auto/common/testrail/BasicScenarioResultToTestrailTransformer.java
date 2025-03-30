package com.wearezeta.auto.common.testrail;

import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class BasicScenarioResultToTestrailTransformer {

    private static final Logger LOG = Logger.getLogger(BasicScenarioResultToTestrailTransformer.class.getName());

    private final Map<String, String> scenario;
    public static final String SKIPPED = "skipped";
    public static final String PASSED = "passed";
    public static final String FAILED = "failed";

    public BasicScenarioResultToTestrailTransformer(Map<String, String> scenario) {
        Objects.requireNonNull(scenario);
        this.scenario = scenario;
    }

    // Transform the scenario result to Testrail execution status
    public TestrailExecutionStatus transform() {
        printStepResults();
        if (isPassed()) {
            return TestrailExecutionStatus.Passed;
        } else if (isFailed()) {
            return TestrailExecutionStatus.Failed;
        } else if (isSkipped()) {
            return TestrailExecutionStatus.Blocked;
        }
        return TestrailExecutionStatus.Retest;
    }

    private boolean isPassed() {
        return scenario.entrySet().stream().allMatch(
                entry -> entry.getValue().equalsIgnoreCase(PASSED));
    }

    private boolean isFailed() {
        return scenario.entrySet().stream().anyMatch(
                entry -> entry.getValue().equalsIgnoreCase(FAILED));
    }

    private boolean isSkipped() {
        return scenario.entrySet().stream().anyMatch(
                entry -> entry.getValue().equalsIgnoreCase(SKIPPED));
    }

    private void printStepResults(){
        for (Map.Entry<String, String> entry : scenario.entrySet()) {
            LOG.finest(String.format("%s -\t %s", entry.getValue(), entry.getKey()));
        }
    }
    
}
