package com.wearezeta.auto.common.rest;

import org.apache.commons.lang3.StringUtils;
import java.util.logging.Logger;

import com.wearezeta.auto.common.log.ZetaLogger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Supplier;

public final class CommonRESTHandlers {

    private RESTResponseHandler responseHandler;
    private int maxRetries = 1;

    public CommonRESTHandlers(RESTResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    public CommonRESTHandlers(RESTResponseHandler responseHandler, int maxRetries) {
        this.responseHandler = responseHandler;
        this.maxRetries = maxRetries;
    }

    private static final Logger log = ZetaLogger.getLog(CommonRESTHandlers.class.getSimpleName());
    private static final String EMPTY_LOG_RECORD = "EMPTY";
    private static final int IS_ALIVE_VERIFICATION_TIMEOUT_MS = 5000;

    public static boolean isAlive(URL siteURL) {
        try {
            final HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(IS_ALIVE_VERIFICATION_TIMEOUT_MS);
            connection.setReadTimeout(IS_ALIVE_VERIFICATION_TIMEOUT_MS);
            connection.connect();
            final int responseCode = connection.getResponseCode();
            log.info(String.format("Response code from %s: %s", siteURL.toString(), responseCode));
            return (responseCode == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            // Just ignore
            // e.printStackTrace();
        }
        return false;
    }

    private static final int MAX_SINGLE_ENTITY_LENGTH_IN_LOG = 400;

    private static String formatLogRecord(Object entity) {
        String result = EMPTY_LOG_RECORD;
        if (entity != null) {
            if (entity instanceof String) {
                result = ((String) entity);
            } else {
                result = entity.toString();
            }
            result = result.isEmpty() ? EMPTY_LOG_RECORD :
                    StringUtils.abbreviate(result, MAX_SINGLE_ENTITY_LENGTH_IN_LOG);
        }
        return result;
    }

    private Response retryRequest(Supplier<Response> requestFunc) {
        int tryNum = 0;
        ProcessingException savedException;
        do {
            try {
                return requestFunc.get();
            } catch (ProcessingException e) {
                savedException = e;
            }
        } while (++tryNum < this.maxRetries);
        throw savedException;
    }

    public <T> T httpPost(Builder webResource, Object entity,
                          Class<T> responseEntityType, int[] acceptableResponseCodes) {
        return httpPost(webResource, MediaType.APPLICATION_JSON, entity,
                responseEntityType, acceptableResponseCodes);
    }

    public <T> T httpPost(Builder webResource, String contentType,
                          Object entity, Class<T> responseEntityType, int[] acceptableResponseCodes) {
        log.info("POST REQUEST...");
        log.info(String.format(" >>> Input data: %s", formatLogRecord(entity)));
        final Response response = retryRequest(
                () -> webResource.post(Entity.entity(entity, contentType), Response.class)
        );
        return getEntityFromResponse(response, responseEntityType, acceptableResponseCodes);
    }

    public Response httpPostFullResponse(Builder webResource, String contentType,
                                         Object entity) throws RESTError {
        log.info("POST REQUEST...");
        log.info(String.format(" >>> Input data: %s", formatLogRecord(entity)));
        final Response response = retryRequest(
                () -> webResource.post(Entity.entity(entity, contentType), Response.class)
        );
        return response;
    }

    public <T> T getEntityFromResponse(Response response, Class<T> responseEntityType, int[] acceptableResponseCodes)
            throws RESTError {
        T responseEntity;
        try {
            response.bufferEntity();
            responseEntity = response.readEntity(responseEntityType);
            log.info(String.format(" >>> Response: %s", formatLogRecord(responseEntity)));
            this.responseHandler.verifyRequestResult(response.getStatus(),
                    acceptableResponseCodes, formatLogRecord(responseEntity));
        } catch (ProcessingException | IllegalStateException | NullPointerException e) {
            responseEntity = null;
            log.warning(e.getMessage());

            if (!"java.lang.String".equals(responseEntityType.getName())) {
                try {
                    String responseString = response.readEntity(String.class);
                    log.info(String.format(" >>> Response: %s", formatLogRecord(responseString)));
                    this.responseHandler.verifyRequestResult(
                            response.getStatus(), acceptableResponseCodes, formatLogRecord(responseString));
                } catch (ProcessingException | IllegalStateException | NullPointerException ex) {
                    log.warning(ex.getMessage());
                }
            }
        }
        return responseEntity;
    }

    public String httpPost(Builder webResource, Object entity, int[] acceptableResponseCodes) {
        String returnString = httpPost(webResource, entity, String.class, acceptableResponseCodes);
        returnString = returnString == null ? "" : returnString;
        return returnString;
    }

    public String httpPost(Builder webResource, Object entity, String contentType, int[] acceptableResponseCodes) {
        String returnString = httpPost(webResource, contentType, entity, String.class, acceptableResponseCodes);
        returnString = returnString == null ? "" : returnString;
        return returnString;
    }

    public <T> T httpPut(Builder webResource, Object entity,
                         Class<T> responseEntityType, int[] acceptableResponseCodes) {
        log.info("PUT REQUEST...");
        log.info(String.format(" >>> Input data: %s", formatLogRecord(entity)));
        final Response response = retryRequest(
                () -> webResource.put(
                        Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE),
                        Response.class)
        );
        return getEntityFromResponse(response, responseEntityType, acceptableResponseCodes);
    }

    public String httpPut(Builder webResource, Object entity, int[] acceptableResponseCodes) {
        String returnString = httpPut(webResource, entity, String.class, acceptableResponseCodes);
        returnString = returnString == null ? "" : returnString;
        return returnString;
    }

    public <T> T httpDelete(Builder webResource, Object entity, Class<T> responseEntityType,
                            int[] acceptableResponseCodes) {
        log.info("DELETE REQUEST...");
        final Response response = retryRequest(
                () -> webResource.method("DELETE", Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE),
                        Response.class)
        );
        return getEntityFromResponse(response, responseEntityType, acceptableResponseCodes);
    }

    public String httpDelete(Builder webResource, Object entity, int[] acceptableResponseCodes) {
        String returnString = httpDelete(webResource, entity, String.class, acceptableResponseCodes);
        returnString = returnString == null ? "" : returnString;
        return returnString;
    }

    public String httpDelete(Builder webResource, int[] acceptableResponseCodes) {
        String returnString = httpDelete(webResource, null, String.class, acceptableResponseCodes);
        returnString = returnString == null ? "" : returnString;
        return returnString;
    }

    public <T> T httpGet(Builder webResource,
                         GenericType<T> responseEntityType, int[] acceptableResponseCodes) {
        log.info("GET REQUEST...");
        final Response response = retryRequest(
                () -> webResource.get(Response.class)
        );
        T responseEntity;
        try {
            response.bufferEntity();
            responseEntity = response.readEntity(responseEntityType);
            log.info(String.format(" >>> Response: %s", formatLogRecord(responseEntity)));
            this.responseHandler.verifyRequestResult(response.getStatus(),
                    acceptableResponseCodes, formatLogRecord(responseEntity));
        } catch (ProcessingException | IllegalStateException | NullPointerException e) {
            responseEntity = null;
            log.warning(e.getMessage());
            if (!"java.lang.String".equals(responseEntityType.getRawType().getName())) {
                try {
                    String responseString = response.readEntity(String.class);
                    log.info(String.format(" >>> Response: %s", formatLogRecord(responseString)));
                    this.responseHandler.verifyRequestResult(
                            response.getStatus(), acceptableResponseCodes, formatLogRecord(responseString));
                } catch (ProcessingException | IllegalStateException | NullPointerException ex) {
                    log.warning(ex.getMessage());
                }
            }
        }
        return responseEntity;
    }

    public String httpGet(Builder webResource, int[] acceptableResponseCodes) {
        String returnString = httpGet(webResource, new GenericType<String>() {}, acceptableResponseCodes);
        returnString = returnString == null ? "" : returnString;
        return returnString;
    }

    public int getResponseCode(Builder webResource, String contentType, Object entity) {
        log.info("POST REQUEST...");
        log.info(String.format(" >>> Input data: %s", formatLogRecord(entity)));
        return retryRequest(
                () -> webResource.post(Entity.entity(entity, contentType), Response.class)
        ).getStatus();
    }

    public int getResponseCode(Builder webResource) {
        log.info("GET REQUEST...");
        return retryRequest(
                () -> webResource.get(Response.class)
        ).getStatus();
    }
}
