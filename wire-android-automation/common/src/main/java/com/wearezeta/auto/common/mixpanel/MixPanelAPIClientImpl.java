package com.wearezeta.auto.common.mixpanel;

import com.wearezeta.auto.common.log.ZetaLogger;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Scanner;

@Deprecated
public class MixPanelAPIClientImpl implements MixPanelAPIClient {
    private static final Logger log = ZetaLogger.getLog(MixPanelAPIClient.class.getSimpleName());

    private static final String MIXPANEL_JQL_API_URL = "https://mixpanel.com/api/2.0/jql";
    private static final String MIXPANEL_JQL_SCRIPT = "function main() {" +
            "return Events({from_date: \"%s\", to_date: \"%s\", event_selectors: [{event: \"%s\"}]})" +
            ".filter(event => event.distinct_id == \"%s\")}";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private final String apiSecret;

    public MixPanelAPIClientImpl(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    /**
     * The data from mixpanel.com comes as a big JSONArray containing JSON object of the event. The last object in
     * the array is
     * the lastest event.
     * <p>
     * This method gets all events that match the given event type and distinct id from the current day via
     * JavaScript Query
     * Language (JQL) and returns the properties of the last event. HTTP basic authentication is used to
     * authenticated to the
     * API. The POST data contains a field called "script" that contains JQL code. Both need to be URL encoded.
     * <p>
     * See: https://mixpanel.com/help/reference/jql/api-reference#api/access
     * <p>
     * You can use the JQL console from mixpanel to check your JQL script.
     * <p>
     * Example: https://mixpanel.com/report/1174750/jql-console/#edit/2814484
     *
     * @param distinctId distinct id of the user (for example:
     *                   15ebe8d2986501-0bfb0d373cb799-73783329-13c680-15ebe8d2987297)
     * @param event      type of event action (for example: contributed)
     * @return A JSONObject with all properties
     * @throws IOException
     */
    public JSONObject getTrackingPropertiesFromLastEvent(String distinctId, String event) throws IOException {
        log.info(String.format("Looking for action '%s' by user with distinct_id '%s'", event, distinctId));
        final String today = sdf.format(new Date());

        final HttpURLConnection connection = (HttpURLConnection) new URL(MIXPANEL_JQL_API_URL)
                .openConnection();
        final String encoded = Base64
                .getEncoder()
                .encodeToString((apiSecret + ":").getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + encoded);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        final StringBuilder result = new StringBuilder();
        result.append(URLEncoder.encode("script", "UTF-8"));
        result.append("=");
        result.append(URLEncoder.encode(String.format(MIXPANEL_JQL_SCRIPT, today, today, event, distinctId),
                "UTF-8"));
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(),
                StandardCharsets.UTF_8))) {
            writer.write(result.toString());
        }
        connection.connect();
        connection.getResponseCode();
        final String content = new Scanner(connection.getInputStream()).useDelimiter("\\Z").next();
        log.info("Returns: " + content);
        JSONArray array = new JSONArray(content);
        if (array.length() > 0) {
            JSONObject json = array.getJSONObject(array.length() - 1);
            log.info("Last object from the JQL response: " + json.toString());
            return json.getJSONObject("properties");
        }
        return new JSONObject();
    }
}
