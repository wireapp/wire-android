package com.wearezeta.auto.common.email.handlers;

import java.util.Arrays;

import javax.ws.rs.core.MediaType;

import com.wearezeta.auto.common.Config;
import com.wearezeta.auto.common.misc.Timedelta;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpStatus;
import java.util.logging.Logger;
import org.json.JSONArray;

import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.rest.CommonRESTHandlers;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;

import org.glassfish.jersey.client.ClientProperties;

final class EmailListenerAPI {

    private static final Logger log = ZetaLogger.getLog(EmailListenerAPI.class.getSimpleName());

    public static String getApiRoot() {
        return Config.common().getDefaultEmailListenerUrl(EmailListenerAPI.class);

    }

    private static final CommonRESTHandlers restHandlers = new CommonRESTHandlers(EmailListenerAPI::verifyRequestResult);

    private static void verifyRequestResult(int currentResponseCode,
                                            int[] acceptableResponseCodes, String message) throws EmailListenerException {
        if (!ArrayUtils.contains(acceptableResponseCodes, currentResponseCode)) {
            throw new EmailListenerException(String.format(
                    "Mailbox service API request failed. Request return code is: %d. Expected codes are: %s. " +
                            "Message from service is: %s",
                    currentResponseCode, Arrays.toString(acceptableResponseCodes), message), currentResponseCode);
        }
    }

    private static Builder buildDefaultRequest(String restAction, Timedelta timeout) {
        final String dstUrl = String.format("%s/%s", getApiRoot(), restAction);
        log.info(String.format("Request to %s...", dstUrl));
        final Client client = ClientBuilder.newClient();
        client.property(ClientProperties.READ_TIMEOUT, (int) timeout.asMillis());
        return client.target(dstUrl).request().accept(MediaType.APPLICATION_JSON);
    }

    public static JSONArray getRecentEmailsForUser(String email, int minCount, int maxCount, Timedelta timeout) {
        Builder webResource = buildDefaultRequest(String.format("recent_emails/%s/%s/%s", email, maxCount, minCount),
                timeout);
        try {
            return new JSONArray(restHandlers.httpGet(webResource, new int[]{HttpStatus.SC_OK}));
        } catch (EmailListenerException e) {
            throw e;
        } catch (Exception e) {
            log.info(String.format("Got zero messages after %s because of: %s", timeout, e.getMessage()));
            return new JSONArray();
        }
    }
}
