package com.wearezeta.auto.common.testiny;

import com.wearezeta.auto.common.Config;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wire.qa.picklejar.engine.gherkin.model.Tag;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

public class TestinySync {
    private static final Logger log = ZetaLogger.getLog(TestinySync.class.getSimpleName());

    private static final Optional<String> testinyProjectName = Config.common()
            .getTestinyProjectName(TestinySync.class);
    private static final Optional<String> testinyRunName = Config.common()
            .getTestinyRunName(TestinySync.class);
    private static final Optional<String> jiraUrl = Config.common()
            .getJiraUrl(TestinySync.class);
    private static final Optional<String> cucumberReportUrl = Config.common()
            .getCucumberReportUrl(TestinySync.class);

    private static final boolean isTestinyAutomatedSyncEnabled = Config.common()
            .getSyncIsAutomated(TestinySync.class);

    private static Optional<Long> testinyRunId = Optional.empty();
    private static long projectId;
    private static AtomicBoolean runDescriptionSet = new AtomicBoolean(false);

    private static final String MAGIC_TAG_PREFIX = "@";
    private static final String JIRA_TAG = "@WPB";
    private static final String RC_TAG = "@rc";
    private static final String KNOWN_TAG = "@knownbug";
    private static final String UNSTABLE_TAG = "@unstable";
    private static final String SF_TAG = "@SF.";
    private static final String SMOKE_TAG = "@smoke";
    private static final String CALLING_SMOKE_TAG = "@callingsmoke";
    private static final String BACKEND_SMOKE_TAG = "@backendsmoke";
    private static final String TESTSERVICE_SMOKE_TAG = "@testservicesmoke";
    private static final String CRITICAL_TAG = "@flow";

    private static final String AUTOMATED_PROPERTY = "automation";
    private static final String IS_RC_PROPERTY = "cf__isrc";
    private static final String TEST_TYPE_PROPERTY = "cf__type";
    private static final String REFERENCES_PROPERTY = "cf__references";
    private static final String TAGS_PROPERTY = "cf__tags";

    private static final Function<List<String>, String> missingTestWarningMessage = (actualIds) -> String
            .format("It seems like one of the test case(s) # %s does not exist in "
                            + "Testiny project '%s' and/or run '%s'. "
                            + "This could slow down the whole RC run. "
                            + "Please double check .feature files and testiny run whether the tags are properly set!",
                    actualIds, testinyProjectName.get(),
                    testinyRunName.get());

    private static long getProjectId(String projectName) {
       switch (projectName) {
           case "Wire WebApp":
               return 3;
           case "Wire Desktop":
               return 9;
           case "Wire Android Reloaded":
               return 8;
           case "Wire iOS":
               return 7;
           case "Wire Account Pages":
               return 10;
           case "Wire Team Management":
               return 11;
           default:
               throw new IllegalArgumentException(String.format("Project '%s' is not supported to sync", projectName));
       }
    }

    private static List<String> getActualIds(Set<String> normalizedTags) {
        List<String> oldIds = normalizedTags.stream()
                .filter(id -> id.matches("@C[0-9]+"))
                .map(id -> id.substring("@".length()))
                .collect(Collectors.toList());

        oldIds = TestinyRESTWrapper.getNewIds(oldIds, projectId);

        List<String> newIds = normalizedTags.stream()
                .filter(id -> id.matches("@TC-[0-9]+"))
                .map(id -> id.substring("@TC-".length()))
                .collect(Collectors.toList());

        return Stream.concat(newIds.stream(), oldIds.stream()).collect(Collectors.toList());
    }

    private static void syncTestinyCustomFields(List<String> actualIds, String scenarioName, Set<String> normalizedTags) {
        if (actualIds.isEmpty()) {
            final String warningMessage = String.format(
                    "Cannot change custom fields for the test case '%s' (tags: '%s'). " +
                            "No Testiny ids can be parsed.",
                    scenarioName, normalizedTags);
            log.warning(warningMessage);
            return;
        }

        ArrayList<String> types = new ArrayList<>();

        // Prepare test case properties
        JSONObject testCase = new JSONObject();

        // Filter for references
        String references = normalizedTags.stream().filter(t -> t.startsWith(JIRA_TAG))
                .map(t -> t.replace(MAGIC_TAG_PREFIX, ""))
                .collect(Collectors.joining(", "));
        
        // Filter for feature tags
        String featureTags = normalizedTags.stream()
                .filter(t -> !t.startsWith(JIRA_TAG) && !containsIgnoreCase(t, RC_TAG) && !t.startsWith(KNOWN_TAG) &&
                        !t.startsWith(UNSTABLE_TAG) && !t.startsWith(SF_TAG) && !t.startsWith(SMOKE_TAG) &&
                        !t.startsWith(CALLING_SMOKE_TAG) && !t.startsWith(BACKEND_SMOKE_TAG) && !t.startsWith(TESTSERVICE_SMOKE_TAG) &&
                        !t.startsWith(CRITICAL_TAG) && !t.startsWith("@SQ") && !t.startsWith("@QA") && !t.startsWith("@WEBAPP") &&
                        !t.startsWith("@websocket") && !t.startsWith("@useSpecialEmail") && !t.startsWith("@C") && !t.startsWith("@TC") &&
                        !t.startsWith("@mute") && !t.startsWith("@ignoreSendingClientIdentityError") &&
                        !t.contains("regression") && !t.matches("@col\\d+") && !t.startsWith("@TSFI") &&
                        !t.matches("@S\\d+.*") && !t.startsWith("@ressource="))
                .map(t -> t.replace(MAGIC_TAG_PREFIX, ""))
                .collect(Collectors.joining(", "));

        testCase.put("project_id", projectId);
        testCase.put(REFERENCES_PROPERTY, references);
        testCase.put(TAGS_PROPERTY, featureTags);
        testCase.put(AUTOMATED_PROPERTY, "AUTOMATED");
        testCase.put(IS_RC_PROPERTY, normalizedTags.stream().anyMatch(t -> equalsIgnoreCase(t, RC_TAG)));

        // Check for type tags
        if (normalizedTags.stream().anyMatch(t -> t.startsWith(SMOKE_TAG))) {
            types.add("SMOKE");
        }
        if (normalizedTags.stream().anyMatch(t -> t.startsWith(CALLING_SMOKE_TAG))) {
            types.add("CALLING_SMOKE");
        }
        if (normalizedTags.stream().anyMatch(t -> t.startsWith(BACKEND_SMOKE_TAG))) {
            types.add("BACKEND_SMOKE");
        }
        if (normalizedTags.stream().anyMatch(t -> t.startsWith(TESTSERVICE_SMOKE_TAG))) {
            types.add("TESTSERVICE_SMOKE");
        }
        if (normalizedTags.stream().anyMatch(t -> t.startsWith(CRITICAL_TAG))) {
            types.add("CRITICAL");
        }
        if (normalizedTags.stream().anyMatch(t -> t.startsWith(SF_TAG))) {
            types.add("SECURITY");
        }
        testCase.put(TEST_TYPE_PROPERTY, types.isEmpty() ? new String[0] : types.toArray(new String[0]));

        log.info(String.format("Sync test case %s with properties %s to Testiny", actualIds,
                testCase.keySet().stream()
                    .map(key -> key + "=" + testCase.get((String) key))
                    .collect(Collectors.joining(", ", "{", "}"))));

        try {
            TestinyRESTWrapper.updateTestsFields(actualIds, testCase);
        } catch (TestinyRequestException e) {
            handleCustomFieldSyncException(actualIds, e, testCase);
        }
    }

    private static void handleCustomFieldSyncException(List<String> actualIds, TestinyRequestException e, JSONObject testCase) {
        if (e.getReturnCode() >= 400 || e.getReturnCode() < 500) {
            // No such test case error
            log.warning(" --> Error while syncing Fields \n " + missingTestWarningMessage.apply(actualIds) + "\n");
            log.warning(" --> Retrying updating individual tests \n\n");
            for (String id : actualIds) {
                try {
                    TestinyRESTWrapper.updateSingleTestFields(id, testCase);
                } catch (TestinyRequestException e1) {
                    log.warning(" --> Exception during field sync retry " + e1.getMessage() + "\n\n");
                    e1.printStackTrace();
                }
            }
        } else {
            log.warning(" --> Unexpected response code during field sync:\n " + e.getMessage() + "\n\n");
            e.printStackTrace();
        }
    }

    private static void syncCurrentTestResultWithTestiny(List<String> actualIds, TestinyExecutionStatus actualTestResult, Set<String> normalizedTags) {
        if (actualIds.isEmpty()) {
            final String warningMessage = String.format("Cannot change execution status for a test case (tags: '%s'). " +
                    "No Testiny ids can be parsed.", normalizedTags);
            log.warning(warningMessage);
            return;
        }
        String comment = null;
        try {
            log.info(String
                    .format(" --> Adding execution result '%s' to RC test case #%s. "
                                    + "Project Name: '%s', Run Name: '%s')\n\n",
                            actualTestResult.toString(), actualIds,
                            testinyProjectName.get(),
                            testinyRunName.get()));

            // Check if the test case has an assigned bug ticket
            List<String> issueTags = normalizedTags.stream()
                    .filter(t -> t.contains(JIRA_TAG))
                    .collect(Collectors.toList());

            // Handle known tag
            if (normalizedTags.contains(KNOWN_TAG)) {
                comment = "Known issue";
                updateTestResults(TestinyExecutionStatus.Blocked, comment, issueTags, actualIds);

            // Handle unstable tag
            } else if (normalizedTags.contains(UNSTABLE_TAG)) {
                comment = "Unstable test (failed because of automation issues)";
                updateTestResults(TestinyExecutionStatus.Blocked, comment,
                        issueTags, actualIds);

            // Handle skipped test case due to 'SkipException'
            } else if (actualTestResult.getStatus().equals("SKIPPED")) {
                comment = "Skipped because of a 'SkipException'";
                updateTestResults(actualTestResult, comment, issueTags, actualIds);

            // Handle normal test case
            } else {
                updateTestResults(actualTestResult, null, issueTags, actualIds);
            }
        } catch (TestinyRequestException e) {
            handleResultSyncException(actualIds, actualTestResult, e, comment);
        }
    }

    private static void handleResultSyncException(List<String> actualIds, TestinyExecutionStatus actualTestResult, TestinyRequestException e, String comment) {
        System.out.println("Handling exception with error code: " + e.getReturnCode());
        if (e.getReturnCode() >= 400 || e.getReturnCode() < 500) {
            // No such test case error
            log.warning(" --> Error while setting test result \n " + missingTestWarningMessage.apply(actualIds) + "\n");
            log.warning(" --> Retrying setting individual results \n\n");
            for (String tcId : actualIds) {
                try {
                    updateTestResult(actualTestResult, comment, List.of(), tcId);
                } catch (TestinyRequestException e1) {
                    log.warning(" --> Exception during result report retry: " + e1.getMessage() + "\n\n");
                    e1.printStackTrace();
                }
            }
        } else {
            log.warning(" --> Unexpected response code during result report:\n " + e.getMessage() + "\n\n");
            e.printStackTrace();
        }
    }

    private static void updateTestResult(TestinyExecutionStatus status, String comment, List<String> issueTags, String tcId) {
        comment = buildComment(comment, issueTags);

        TestinyRESTWrapper.addSingleTestResult(testinyRunId.get(), Long.parseLong(tcId), projectId, status,
                comment == null ? Optional.empty() : Optional.of(comment));
    }

    private static void updateTestResults(TestinyExecutionStatus status, String comment, List<String> issueTags, List<String> tcIds) {
        // Build comment
        comment = buildComment(comment, issueTags);

        TestinyRESTWrapper.addTestResults(testinyRunId.get(), tcIds, projectId, status,
                comment == null ? Optional.empty() : Optional.of(comment));
    }

    private static String buildComment(String comment, List<String> issueTags) {
        if (!issueTags.isEmpty()) {
            if (comment == null) {
                comment = "Ticket";
            }
            comment = comment + ": " + issueTags.stream()
                    .map(t -> jiraUrl.orElse("") + t.replace(MAGIC_TAG_PREFIX, ""))
                    .collect(Collectors.joining(", "));
        }
        return comment;
    }

    public static void syncExecutedScenarioWithTestiny(String scenarioName, TestinyExecutionStatus actualTestResult,
                                                        List<Tag> tags) {
        Set<String> normalizedTags = tags.stream().map(Tag::getName).collect(Collectors.toSet());
        // Check if the test case is a part of a Testiny run
        final boolean isTestinyRCRun = testinyProjectName.isPresent() && !testinyProjectName.get().isEmpty()
                && testinyRunName.isPresent() && !testinyRunName.get().isEmpty();

        // If the test case is a part of a Testiny run, but the run id is not known, try to find it
        projectId = getProjectId(testinyProjectName.get());
        if (isTestinyRCRun && testinyRunId.isEmpty()) {
            try {
                testinyRunId = Optional.of(TestinyRESTWrapper.getOrCreateTestRun(projectId, testinyRunName.get()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<String> actualIds = getActualIds(normalizedTags);

        // If the test case is a part of a Testiny run, try to sync the test result with Testiny
        if (isTestinyRCRun && testinyRunId.isPresent()) {
            try {
                // Try to update the run description
                if (cucumberReportUrl.isPresent() && runDescriptionSet.compareAndSet(false, true)) {
                    TestinyRESTWrapper.updateRunDescription(testinyRunId.get(), cucumberReportUrl.get());
                }
                // Sync test results
                syncCurrentTestResultWithTestiny(actualIds, actualTestResult, normalizedTags);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Mark test as automated in Testiny
        // This is done for every jenkins run, not only for Testiny runs
        // Check if this affects API rate limits, if so, consider moving this to the Testiny run
        if (isTestinyAutomatedSyncEnabled) {
            syncTestinyCustomFields(actualIds, scenarioName, normalizedTags);
        }
    }
}
