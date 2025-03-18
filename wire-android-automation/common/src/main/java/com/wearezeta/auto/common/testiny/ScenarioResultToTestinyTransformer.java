package com.wearezeta.auto.common.testiny;

import com.wire.qa.picklejar.engine.gherkin.model.Scenario;
import com.wire.qa.picklejar.engine.gherkin.model.Step;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class ScenarioResultToTestinyTransformer {

    private static final Logger LOG = Logger.getLogger(ScenarioResultToTestinyTransformer.class.getName());

    private final List<Step> steps;
    public static final String SKIPPED = "skipped";
    public static final String PASSED = "passed";
    public static final String FAILED = "failed";

    public ScenarioResultToTestinyTransformer(Scenario scenario) {
        Objects.requireNonNull(scenario);
        this.steps = scenario.getSteps();
    }

    // Transform the scenario result to Testiny execution status
    public TestinyExecutionStatus transform() {
        printStepResults();
        if (isPassed()) {
            return TestinyExecutionStatus.Passed;
        } else if (isFailed()) {
            return TestinyExecutionStatus.Failed;
        } else if (isSkipped()) {
            return TestinyExecutionStatus.Skipped;
        }
        return TestinyExecutionStatus.Blocked;
    }

    private boolean isPassed() {
        return steps.stream().allMatch(
                step -> step.getResult().getStatus().equalsIgnoreCase(PASSED));
    }

    private boolean isFailed() {
        return steps.stream().anyMatch(
                step -> step.getResult().getStatus().equalsIgnoreCase(FAILED));
    }

    // Not all steps passed
    // No failed steps
    // -> Test was skipped because of a 'SkipException'
    // => Some requirement for the test to be executed was not met
    private boolean isSkipped() {
        return steps.stream().anyMatch(
                step -> step.getResult().getStatus().equalsIgnoreCase(SKIPPED));
    }

    private void printStepResults(){
        for (Step step : steps) {
            LOG.finest(String.format("%s -\t %s", step.getName(), step.getResult().getStatus()));
        }
    }
    
}
