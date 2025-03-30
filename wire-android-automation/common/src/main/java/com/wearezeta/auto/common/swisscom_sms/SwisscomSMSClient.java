package com.wearezeta.auto.common.swisscom_sms;

import com.wearezeta.auto.common.CommonSteps;
import com.wearezeta.auto.common.log.ZetaLogger;
import java.util.logging.Logger;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class SwisscomSMSClient {

    private static final Logger log = ZetaLogger.getLog(CommonSteps.class.getSimpleName());

    private final String oneTimeCodeEndpoint = "https://vrwlctuo8b.execute-api.eu-central-1.amazonaws.com/qa/onetimecode";

    public SwisscomSMSClient() {
    }

    public String getLatestOneTimeCode() throws IOException {
        String oneTimeCode = "";

        JSONObject response = null;

        try {
            URL url = new URL(oneTimeCodeEndpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            log.info("Requesting Swisscom SMS one time code: " + oneTimeCodeEndpoint);
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP Error code : " + conn.getResponseCode());
            }
            String result = readStream(conn.getInputStream());
            if(result != null && !result.trim().isEmpty()) {
                log.info("Result: " + result);
                response = new JSONObject(result);
            } else {
                log.info("Result: (empty)");
            }
            conn.disconnect();
        } catch (Exception e) {
            log.info("Error during processing API call for Swisscom SMS one time code: " + e.getMessage());
        }

        if(response != null) {
            oneTimeCode = response.getString("oneTimeCode");
            log.info("One time code: " + oneTimeCode);
        }

        return oneTimeCode;
    }

    private String readStream(InputStream is) throws IOException {
        if (is != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                return content.toString();
            }
        }
        return "";
    }
}

