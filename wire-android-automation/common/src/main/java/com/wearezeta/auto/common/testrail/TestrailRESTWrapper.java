package com.wearezeta.auto.common.testrail;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class TestrailRESTWrapper {
    public static long getProjectId(String projectName) {
        final JSONObject response = TestrailREST.getProjects();
        final JSONArray projects = response.getJSONArray("projects");
        for (int i = 0; i < projects.length(); i++) {
            if (projects.getJSONObject(i).getString("name").equals(projectName)) {
                return projects.getJSONObject(i).getLong("id");
            }
        }
        throw new IllegalArgumentException(String.format("Project '%s' cannot be found in Testrail",
                projectName));
    }

    public static long getTestPlanId(long projectId, String testPlanName) {
        final JSONObject response = TestrailREST.getTestPlans(projectId);
        final JSONArray plans = response.getJSONArray("plans");
        for (int i = 0; i < plans.length(); i++) {
            if (plans.getJSONObject(i).getString("name").equals(testPlanName)) {
                return plans.getJSONObject(i).getLong("id");
            }
        }
        throw new IllegalArgumentException(String.format("Test plan '%s' cannot be found in Testrail",
                testPlanName));
    }

    private static boolean isConfigurationEqual(String expectedConfiguration, String actualConfiguration) {
        // Configuration name is comma-separated string
        final Set<String> normalizedExpectedConfig = Arrays.stream(expectedConfiguration.split(",")).map(
                String::trim).collect(Collectors.toSet());
        final Set<String> normalizedActualConfig = Arrays.stream(actualConfiguration.split(",")).map(
                String::trim).collect(Collectors.toSet());
        return normalizedExpectedConfig.equals(normalizedActualConfig);
    }

    public static long getTestRunId(long testPlanId, String testRunName, Optional<String> configurationName) {
        final JSONObject response = TestrailREST.getTestPlan(testPlanId);
        if (!response.has("entries")) {
            throw new IllegalArgumentException(String.format("Test run '%s' cannot be found", testRunName));
        }
        final JSONArray entries = response.getJSONArray("entries");
        for (int entryIdx = 0; entryIdx < entries.length(); entryIdx++) {
            if (entries.getJSONObject(entryIdx).has("runs")) {
                final JSONArray runs = entries.getJSONObject(entryIdx).getJSONArray("runs");
                for (int runIdx = 0; runIdx < runs.length(); runIdx++) {
                    if (runs.getJSONObject(runIdx).getString("name").equals(testRunName)) {
                        if (configurationName.isPresent()) {
                            if (runs.getJSONObject(runIdx).has("config") && isConfigurationEqual(
                                    configurationName.get(), runs.getJSONObject(runIdx).getString("config"))) {
                                return runs.getJSONObject(runIdx).getLong("id");
                            } else {
                                continue;
                            }
                        }
                        return runs.getJSONObject(runIdx).getLong("id");
                    }
                }
            }
        }
        throw new IllegalArgumentException(String.format("Test run '%s (%s)' cannot be found",
                testRunName, configurationName.orElse("<No Config>")));
    }

    /**
     * @param testRunId
     * @param caseId
     * @param newStatus this has to be never set to TestrailExecutionStatus.Untested
     *                  Otherwise API call will fail for sure
     * @param comment
     * @throws Exception
     */

    public static void updateTestResult(long testRunId, long caseId,
                                        TestrailExecutionStatus newStatus, Optional<String> comment) {
        TestrailREST.addTestCaseResult(testRunId, caseId, newStatus.getId(), comment);
    }

    public static TestrailExecutionStatus getCurrentTestResult(long testRunId, long caseId) {
        final JSONObject response = TestrailREST.getTestCaseResults(testRunId, caseId);
        final JSONArray results = response.getJSONArray("results");
        if (results.length() == 0) {
            return TestrailExecutionStatus.Untested;
        } else {
            return TestrailExecutionStatus.getById(results.getJSONObject(0).getInt("status_id"));
        }
    }

    public static void updateCustomCaseProperty(long caseId, String valueName, Object newValue) {
        final JSONObject requestBody = new JSONObject();
        requestBody.put(valueName, newValue);
        TestrailREST.updateCase(caseId, requestBody);
    }

    public static void updateCustomCaseProperties(long caseId, Map<String, Object> newValues) {
        final JSONObject requestBody = new JSONObject();
        for (Map.Entry<String, Object> entry : newValues.entrySet()) {
            requestBody.put(entry.getKey(), entry.getValue());
        }
        TestrailREST.updateCase(caseId, requestBody);
    }
}
