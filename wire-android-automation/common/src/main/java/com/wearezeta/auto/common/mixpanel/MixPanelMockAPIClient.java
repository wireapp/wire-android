package com.wearezeta.auto.common.mixpanel;

import com.wearezeta.auto.common.log.ZetaLogger;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class MixPanelMockAPIClient implements MixPanelAPIClient {

    private static final Logger log = ZetaLogger.getLog(MixPanelAPIClient.class.getSimpleName());

    private static final String MIXPANEL_LAST_REQUEST_FOR_DISTINCT_URL = "http://mixpanel.com/api/distinct/%s/event/%s/last/1";

    @Override
    public JSONObject getTrackingPropertiesFromLastEvent(String distinctId, String event) throws IOException {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        CloseableHttpClient client = httpClientBuilder.build();
        String url = String.format(MIXPANEL_LAST_REQUEST_FOR_DISTINCT_URL, distinctId, event);
        log.info(String.format("Looking for action '%s' by user with distinct_id '%s' at %s", event, distinctId, url));
        HttpGet request = new HttpGet(url);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        client.execute(request).getEntity().writeTo(byteArrayOutputStream);
        String contentString = new String(byteArrayOutputStream.toByteArray());
        JSONArray jsonArray = new JSONArray(contentString);
        if (jsonArray.length() == 0) {
            return new JSONObject();
        }
        return (((JSONObject) jsonArray.get(0)).getJSONObject("properties"));
    }
}
