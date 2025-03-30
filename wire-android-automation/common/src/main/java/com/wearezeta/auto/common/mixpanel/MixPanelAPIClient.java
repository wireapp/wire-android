package com.wearezeta.auto.common.mixpanel;

import org.json.JSONObject;

import java.io.IOException;

public interface MixPanelAPIClient {
    JSONObject getTrackingPropertiesFromLastEvent(String distinctId, String event) throws IOException;
}
