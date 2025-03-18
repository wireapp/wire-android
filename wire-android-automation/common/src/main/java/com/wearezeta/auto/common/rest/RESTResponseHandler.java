package com.wearezeta.auto.common.rest;

@FunctionalInterface
public interface RESTResponseHandler {
    void verifyRequestResult(int currentResponseCode, int[] acceptableResponseCodes, String message);
}
