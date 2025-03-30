package com.wearezeta.auto.common.testiny;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class TestinyRESTWrapper {

    public static long getTestRunId(long projectId, String testRunName) {
        final JSONObject response = TestinyREST.getOpenTestRun(projectId, testRunName);
        if (!response.has("data") || response.getJSONArray("data").isEmpty()) {
            throw new IllegalArgumentException(String.format("Test run '%s' cannot be found", testRunName));
        }
        JSONArray data = response.getJSONArray("data");
        return data.getJSONObject(0).getLong("id");
    }

    public static long getOrCreateTestRun(long projectID, String testRunName) {
        long testRunId;
        try {
            testRunId = getTestRunId(projectID, testRunName);
        } catch (IllegalArgumentException e) {
            final JSONObject response = TestinyREST.createTestRun(testRunName, projectID);
            testRunId = response.getLong("id");
        }
        return testRunId;
    }

    public static List<String> getNewIds(List<String> oldIds, long projectId){
        final JSONObject response = TestinyREST.getNewIds(oldIds, projectId);
        if (!response.has("data")) {
            throw new IllegalArgumentException("No new ids found");
        }

        JSONArray data = response.getJSONArray("data");
        List<String> newIds = new ArrayList<>();

        for (int i = 0; i < data.length(); i++) {
            JSONObject obj = data.getJSONObject(i);
            newIds.add(String.valueOf(obj.getInt("id")));
        }

        return newIds;
    }

    public static void updateRunDescription(long testRunId, String newDescriptionLine) {
        // Split the description line into parts: Build name - url
        String[] parts = newDescriptionLine.replace("\n", "").split(" - ");
        // Split the url into parts: label - link
        String[] urlParts = parts[1].split(": ");
        List<JSONObject> entries  = new ArrayList<>();

        // Get current description
        String olddescription = TestinyREST.getTestrunDescription(testRunId);
        JSONObject description;

        if(!olddescription.isEmpty()) {
            description = new JSONObject(olddescription);

            JSONArray descriptionArray = description.getJSONArray("c");
            for (int i = 0; i < descriptionArray.length(); i++) {
                entries.add(descriptionArray.getJSONObject(i));
            }

            // Remove the old description
            description.remove("c");
        } else {
            description = new JSONObject("{}");
        }

        // Create a new description entry
        //  {                               // newDescriptionEntry
        //      "t": "p",
        //      "children":[                // descriptionChildren
        //          {"text": "%s - "},      // parts[0]
        //          {                       // urlObject
        //              "t": "a",
        //              "children":[        // urlChildren
        //                  {"text": "%s"}  // urlParts[0]
        //               ],
        //              "url": "%s"         // urlParts[1]
        //          },
        //          {"text":""}
        //      ]
        //  }";
        JSONObject newDescriptionEntry = new JSONObject();
        JSONArray descriptionChildren = new JSONArray();

        JSONObject urlObject = new JSONObject();
        JSONArray urlChildren = new JSONArray();

        urlChildren.put(new JSONObject().put("text", urlParts[0]));
        urlObject.put("t", "a");
        urlObject.put("children", urlChildren);
        urlObject.put("url", urlParts[1]);

        descriptionChildren.put(new JSONObject().put("text", parts[0] + " - "));
        descriptionChildren.put(urlObject);
        descriptionChildren.put(new JSONObject().put("text", ""));

        newDescriptionEntry.put("t", "p");
        newDescriptionEntry.put("children", descriptionChildren);

        // Add the new description entry to the beginning of the list
        entries.add(0, newDescriptionEntry);
        description.put("c", new JSONArray(entries));
        description.put("v", 1);
        description.put("t", "slate");

        TestinyREST.updateTestRunDescription(testRunId, description.toString());
    }

    public static void addSingleTestResult(long testRunId, long caseId, long projectId,
                                           TestinyExecutionStatus newStatus, Optional<String> comment) {
        TestinyREST.addTestCaseResult(testRunId, caseId, newStatus.getStatus());

        // Add a comment to the test case if it is provided
        comment.ifPresent(x -> {
            JSONObject commentObj = TestinyREST.createCommentEntity(x, projectId);
            TestinyREST.addCommentToTestCaseInTestRun(caseId, testRunId, commentObj.getInt("id"));
        });
    }

    public static void addTestResults(long testRunId, List<String> caseIds, long projectId,
                                      TestinyExecutionStatus newStatus, Optional<String> comment) {
        List<Long> ids = caseIds.stream().map(Long::parseLong).collect(Collectors.toList());
        TestinyREST.bulkAddTestCaseResults(testRunId, ids, newStatus.getStatus());

        // Add a comment to the test case if it is provided
        comment.ifPresent(x -> {
            JSONObject commentObj = TestinyREST.createCommentEntity(x, projectId);
            TestinyREST.bulkAddCommentToTestCaseInTestRun(ids, testRunId, commentObj.getInt("id"));
        });
    }

    public static void updateSingleTestFields(String id, JSONObject newProperties) {
        TestinyREST.updateCase(Long.parseLong(id), newProperties);
    }

    public static void updateTestsFields(List<String> caseIds, JSONObject testCaseProperties) {
        JSONArray newProperties = new JSONArray();

        // Add test ids to the test case properties
        for (String caseId : caseIds) {
            JSONObject tc = new JSONObject(testCaseProperties.toString());
            tc.put("id", Long.parseLong(caseId));
            newProperties.put(tc);
        }

        TestinyREST.bulkUpdateCases(newProperties);
    }
}
