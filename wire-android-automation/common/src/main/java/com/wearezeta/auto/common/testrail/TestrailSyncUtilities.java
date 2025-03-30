package com.wearezeta.auto.common.testrail;

import com.wearezeta.auto.common.Config;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wire.qa.picklejar.engine.gherkin.model.Tag;

import java.util.logging.Logger;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestrailSyncUtilities {
    private static final Logger log = ZetaLogger.getLog(TestrailSyncUtilities.class.getSimpleName());

    private static final Optional<String> testrailProjectName = Config.common()
            .getTestrailProjectName(TestrailSyncUtilities.class);
    private static final Optional<String> testrailPlanName = Config.common()
            .getTestrailPlanName(TestrailSyncUtilities.class);
    private static final Optional<String> testrailRunName = Config.common()
            .getTestrailRunName(TestrailSyncUtilities.class);
    private static final Optional<String> testrailRunConfigName = Config.common()
            .getTestrailRunConfigName(TestrailSyncUtilities.class);
    private static final Optional<String> jiraUrl = Config.common()
            .getJiraUrl(TestrailSyncUtilities.class);

    private static Optional<String> rcTestsComment = Optional.empty();

    // TODO: Remove this
    static {
        try {
            final Optional<String> rcTestsCommentPath = Config.common()
                    .getRcTestsCommentPath(TestrailSyncUtilities.class);
            if (rcTestsCommentPath.isPresent() && !rcTestsCommentPath.get().trim().isEmpty()) {
                if (new File(rcTestsCommentPath.get()).exists()) {
                    rcTestsComment = Optional.of(new String(Files.readAllBytes(Paths.get(rcTestsCommentPath.get())),
                            Charset.forName("UTF-8")));
                } else {
                    log.severe(String.format("Please make sure the file %s exists and is accessible",
                            rcTestsCommentPath.get()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final boolean isTestrailAutomatedSyncEnabled = Config.common()
            .getSyncIsAutomated(TestrailSyncUtilities.class);

    private static Optional<Long> testrailRunId = Optional.empty();

    private static final String MAGIC_TAG_PREFIX = "@";
    private static final String RC_TAG = "@rc";
    private static final String KNOWN_TAG = "@knownbug";
    private static final String UNSTABLE_TAG = "@unstable";
    private static final String SF_TAG = "@SF.";
    private static final String TITLE_PROPERTY = "title";
    private static final String IS_AUTOMATED_PROPERTY = "custom_is_automated";
    private static final String IS_RC_PROPERTY = "custom_is_rc";
    private static final String TESTS_SECURITY_FUNCTION_PROPERTY = "custom_tests_sf";

    private static List<String> getActualIds(Set<String> normalizedTags) {
        return normalizedTags
                .stream()
                .filter(id -> id.matches("@C[0-9]+"))
                .map(id -> id.substring("@C".length()))
                .collect(Collectors.toList());
    }

    // Update test properties
    private static void syncTestrailIsAutomatedState(String scenarioName, Set<String> normalizedTags) {
        final List<String> actualIds = getActualIds(normalizedTags);

        if (actualIds.isEmpty()) {
            final String warningMessage = String.format(
                    "Cannot change IsAutomated state for the test case '%s' (tags: '%s'). " +
                            "No Testrail ids can be parsed.",
                    scenarioName, normalizedTags);
            log.warning(warningMessage);
            return;
        }

        for (String caseId : actualIds) {
            final Map<String, Object> props = new HashMap<>();
            if (actualIds.size() == 1) {
                // Only update test case title if the case contains only one C tag
                props.put(TITLE_PROPERTY, scenarioName); // remove this
            }
            props.put(IS_AUTOMATED_PROPERTY, true);
            props.put(IS_RC_PROPERTY, normalizedTags.stream().anyMatch(t -> t.equalsIgnoreCase(RC_TAG)));
            props.put(TESTS_SECURITY_FUNCTION_PROPERTY, normalizedTags.stream().anyMatch(t -> t.startsWith(SF_TAG)));
            log.info(String.format("Sync test case @C%s with properties %s to Testrail", caseId, props.keySet().stream()
                    .map(key -> key + "=" + props.get(key))
                    .collect(Collectors.joining(", ", "{", "}"))));
            try {
                TestrailRESTWrapper.updateCustomCaseProperties(Long.parseLong(caseId), props);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void syncCurrentTestResultWithTestrail(TestrailExecutionStatus actualTestResult, Set<String> normalizedTags) {
        final List<String> actualIds = getActualIds(normalizedTags);
        if (actualIds.isEmpty()) {
            final String warningMessage = String.format("Cannot change execution status for a test case (tags: '%s'). " +
                    "No Testrail ids can be parsed.", normalizedTags);
            log.warning(warningMessage);
            final String notificationHeader = String
                    .format("ACHTUNG! An unknown RC test case has been executed in "
                                    + "project '%s', test plan '%s', run '%s (%s)'",
                            testrailProjectName.get(),
                            testrailPlanName.get(), testrailRunName.get(),
                            testrailRunConfigName.orElse("<No Config>"));
            return;
        }

        final boolean isMonitoringRun = Pattern.compile(
                ".*monitor.*", Pattern.CASE_INSENSITIVE
        ).matcher(testrailPlanName.get()).matches();

        for (String tcId : actualIds) {
            TestrailExecutionStatus previousTestResult = TestrailExecutionStatus.Untested;
            log.info(" tcId--> " + tcId + "\n\n");
            try {
                previousTestResult =
                        TestrailRESTWrapper.getCurrentTestResult(testrailRunId.get(), Long.parseLong(tcId));
            } catch (TestrailRequestException e) {
                if (e.getReturnCode() == 400) {
                    if (!isMonitoringRun) {
                        // No such test case error
                        final String warningMessage = String
                                .format("It seems like there is no test case(s) # %s in "
                                                + "Testrail project '%s', plan '%s', run '%s (%s)'. "
                                                + "This could slow down the whole RC run. "
                                                + "Please double check .feature files whether the %s tag is properly set!",
                                        actualIds, testrailProjectName.get(),
                                        testrailPlanName.get(), testrailRunName.get(),
                                        testrailRunConfigName.orElse("<No Config>"),
                                        RC_TAG);
                        log.warning(" --> " + warningMessage + "\n\n");
                    }
                } else {
                    log.warning(" --> " + e.getMessage() + "\n\n");
                    e.printStackTrace();
                }
                continue;
            }
            log.info(String
                    .format(" --> Adding execution result '%s' to RC test case #%s (previous result was '%s'). "
                                    + "Project Name: '%s', Plan Name: '%s', Run Name: '%s (%s)')\n\n",
                            actualTestResult.toString(), tcId, previousTestResult.toString(),
                            testrailProjectName.get(),
                            testrailPlanName.get(), testrailRunName.get(),
                            testrailRunConfigName.orElse("<No Config>")));

            if (normalizedTags.contains(KNOWN_TAG)) {
                // TODO: expand filter for other ticket prefixes and do it if there isn't known tag as well
                List<String> issueTags = normalizedTags.stream()
                        .filter(t -> t.contains(MAGIC_TAG_PREFIX + "WEBAPP"))
                        .collect(Collectors.toList());
                String comment = "Known issue";
                if (!issueTags.isEmpty()) {
                    comment += ": " + issueTags.stream()
                            .map(t -> jiraUrl.orElse("") + t.replace(MAGIC_TAG_PREFIX, ""))
                            .collect(Collectors.joining(" "));
                }
                TestrailRESTWrapper.updateTestResult(testrailRunId.get(), Long.parseLong(tcId),
                        TestrailExecutionStatus.Known, Optional.of(comment));
            } else if (normalizedTags.contains(UNSTABLE_TAG)) {
                String comment = "Unstable test (failed because of automation issues)";
                TestrailRESTWrapper.updateTestResult(testrailRunId.get(), Long.parseLong(tcId),
                        TestrailExecutionStatus.Blocked, Optional.of(comment));
            } else {
                TestrailRESTWrapper.updateTestResult(testrailRunId.get(), Long.parseLong(tcId),
                        actualTestResult, rcTestsComment);
            }
        }
    }

    public static void syncExecutedScenarioWithTestrail(String scenarioName, TestrailExecutionStatus actualTestResult,
                                                        List<Tag> tags) {
        System.out.println("SYNC WITH TESTRAIL");
        Set<String> normalizedTags = tags.stream().map(Tag::getName).collect(Collectors.toSet());
        // Check if the test case is a part of a Testrail run
        final boolean isTestrailRCRun = testrailProjectName.isPresent() && testrailProjectName.get().length() > 0
                && testrailPlanName.isPresent() && testrailPlanName.get().length() > 0
                && testrailRunName.isPresent() && testrailRunName.get().length() > 0;

        // If the test case is a part of a Testrail run, but the run id is not known, try to find it
        System.out.println("RUN ID PRESENT: " + testrailRunId.isPresent());
        if (isTestrailRCRun && !testrailRunId.isPresent()) {
            System.out.println("GETTING TESTRAIL RUN ID");
            try {
                // Get project id could be hardcoded
                final long projectId = TestrailRESTWrapper.getProjectId(testrailProjectName.get());
                final long planId = TestrailRESTWrapper.getTestPlanId(projectId, testrailPlanName.get());
                testrailRunId = Optional.of(TestrailRESTWrapper.getTestRunId(planId, testrailRunName.get(),
                        testrailRunConfigName));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("RUN ID: " + testrailRunId.get());

        // If the test case is a part of a Testrail run, try to sync the test result with Testrail
        if (isTestrailRCRun && testrailRunId.isPresent()) {
            System.out.println("SEND RESULTS TO TESTRAIL");
            try {
                syncCurrentTestResultWithTestrail(actualTestResult, normalizedTags);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Mark test as automated in Testrail
        if (isTestrailAutomatedSyncEnabled) {
            syncTestrailIsAutomatedState(scenarioName, normalizedTags);
        }
    }
}
