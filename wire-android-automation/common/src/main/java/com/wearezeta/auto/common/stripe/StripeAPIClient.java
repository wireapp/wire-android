package com.wearezeta.auto.common.stripe;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.RequestOptions;
import com.wearezeta.auto.common.backend.HttpRequestException;
import com.wearezeta.auto.common.credentials.Credentials;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.wearezeta.auto.common.misc.URLTransformer;
import org.json.JSONObject;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Optional.ofNullable;

public class StripeAPIClient {

    private static final Logger log = ZetaLogger.getLog(StripeAPIClient.class.getSimpleName());

    private static final int PAGE$_LIMIT = 10;
    private final RequestOptions requestOptions;
    private static final String BASEURI = "https://api.stripe.com";
    private static final String stripeAPIKey = Credentials.get("STRIPE_API_KEY");

    public StripeAPIClient() {
        Stripe.apiKey = stripeAPIKey;
        this.requestOptions = new RequestOptions.RequestOptionsBuilder().setApiKey(stripeAPIKey).build();
    }

    // Helpers

    private String truncate(String text) {
        return truncate(text, 100);
    }

    private String truncateOnlyOnBig(String text) {
        return truncate(text, 5000);
    }

    private String truncate(String text, int maxLength) {
        if (text.length() > maxLength) {
            return text.substring(0, maxLength) + "...";
        }
        return text;
    }

    private void writeStream(String data, OutputStream os) throws IOException {
        DataOutputStream wr = new DataOutputStream(os);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(wr, StandardCharsets.UTF_8));
        try {
            writer.write(data);
        } finally {
            writer.close();
            wr.close();
        }
    }

    private void writeStream(byte[] data, OutputStream os) throws IOException {
        DataOutputStream wr = new DataOutputStream(os);
        try {
            wr.write(data);
        } finally {
            wr.close();
        }
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
        } else {
            return "";
        }
    }

    private void logResponseAndStatusCode(String response, int responseCode) {
        if (response.isEmpty()) {
            log.info(String.format(" >>> Response (%s) with no response body", responseCode));
        } else {
            if (log.isLoggable(Level.FINE)) {
                log.info(String.format(" >>> Response (%s): %s", responseCode, truncateOnlyOnBig(response)));
            } else {
                log.info(String.format(" >>> Response (%s): %s", responseCode, truncate(response)));
            }
        }
    }

    private <T> T retryOnBackendFailure(Supplier<T> r) {
        int ntry = 1;
        HttpRequestException savedException = null;
        while (ntry <= 2) {
            try {
                return r.get();
            } catch (HttpRequestException e) {
                savedException = e;
                Timedelta.ofMillis(2000 * ntry).sleep();
            }
            ntry++;
        }
        throw savedException;
    }

    private void assertResponseCode(int responseCode, int[] acceptableResponseCodes) {
        if (Arrays.stream(acceptableResponseCodes).noneMatch(a -> a == responseCode)) {
            throw new HttpRequestException(
                    String.format("Backend request failed. Request return code is: %d. Expected codes are: %s.",
                            responseCode,
                            Arrays.toString(acceptableResponseCodes)),
                    responseCode);
        }
    }

    private void logHttpRequestProperties(HttpURLConnection c) {
        if (log.isLoggable(Level.FINE)) {
            for (String property : c.getRequestProperties().keySet()) {
                List<String> values = Collections.singletonList(c.getRequestProperty(property));
                log.fine(String.format("%s: %s", property, String.join(", ", values)));
            }
        }
    }

    private void logRequest(String request) {
        if (request.isEmpty()) {
            log.info(" >>> Request with no request body");
        } else {
            if (log.isLoggable(Level.FINE)) {
                log.info(String.format(" >>> Request: %s", request));
            } else {
                log.info(String.format(" >>> Request: %s", truncate(request)));
            }
        }
    }

    //

    // Backend

    private HttpURLConnection buildDefaultRequestWithAuth(String path, String header) {
        return buildDefaultRequestWithAuth(path, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, header);
    }

    private HttpURLConnection buildDefaultRequestWithAuth(String path, String contentType, String acceptType,
                                                          String header) {
        try {
            String parameters = null;
            if (path.contains("?")) {
                parameters = URLTransformer.getQuery(path);
                path = URLTransformer.getPath(path);
            }
            URL url = new URL(String.format("%s/%s", BASEURI, path));
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            c.setRequestProperty("Content-Type", contentType);
            c.setRequestProperty("Accept", acceptType);
            c.setRequestProperty("Authorization", header);
            if (parameters != null) {
                c.setRequestProperty("Content-Length",
                        Integer.toString(parameters.getBytes(StandardCharsets.UTF_8).length));
            }
            return c;
        }
        catch (MalformedURLException ex) {
            return null;
        }
    }

    private String httpPost(HttpURLConnection c, byte[] requestBody, int[] acceptableResponseCodes) {
        String response = "";
        int status = -1;
        try {
            log.info("POST " + c.getURL());
            c.setRequestMethod("POST");
            logHttpRequestProperties(c);
            //logRequest(requestBody);
            c.setDoOutput(true);
            writeStream(requestBody, c.getOutputStream());
            status = c.getResponseCode();
            response = readStream(c.getInputStream());
            logResponseAndStatusCode(response, status);
            assertResponseCode(status, acceptableResponseCodes);
            return response;
        } catch (IOException e) {
            try {
                response = readStream(c.getErrorStream());
            } catch (IOException ex) {
                log.fine("Could not read error stream: " + e.getMessage());
            }
            if (Arrays.stream(acceptableResponseCodes).anyMatch(acceptable -> acceptable > 400)) {
                assertResponseCode(status, acceptableResponseCodes);
                log.info(String.format(">>> Response (%s): %s", status, response));
                return response;
            } else {
                String error = String.format("%s (%s): %s", e.getMessage(), status, response);
                log.severe(error);
                throw new HttpRequestException(error, status);
            }
        } finally {
            c.disconnect();
        }
    }

    //

    public Timedelta getTrialPeriodDuration(String customerId) throws StripeException {
        final Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        final SubscriptionCollection subscriptions = Subscription.list(params);
        if(subscriptions.getData().size() == 1) {
            return Timedelta.ofSeconds(subscriptions.getData().get(0).getTrialEnd() - subscriptions.getData().get(0).getTrialStart());
        } else {
            throw new IllegalStateException("Customer doesn't have 1 subscription as expected");
        }
    }

    public void changeTrialPeriodDuration(String customerId, Timedelta newDuration) throws StripeException {
        final Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        params.put("status", "trialing");
        final SubscriptionCollection subscriptions = Subscription.list(params);
        if(subscriptions.getData().size() == 1) {
            final Map<String, Object> updateParams = new HashMap<>();
            if(newDuration.asMillis() > 0) {
                updateParams.put("trial_end", Timedelta
                        .now()
                        .sum(newDuration)
                        .asSeconds());
            } else {
                updateParams.put("trial_end", "now");
            }
            subscriptions.getData().get(0).update(updateParams, requestOptions);
        } else {
            throw new IllegalStateException("Customer doesn't have 1 trialing subscription as expected");
        }
    }

    public void changeGracePeriodDuration(String customerId, Timedelta newDuration) throws StripeException {
        final Customer dstCustomer = Customer.retrieve(customerId, requestOptions);
        final Map<String, String> metadata = dstCustomer.getMetadata();
        JSONObject suspend = new JSONObject(metadata.get("suspend"));
        suspend.put("graceEnding", Timedelta.now().sum(newDuration).asSeconds());
        final Map<String, String> meta = new HashMap<>();
        meta.put("suspend", suspend.toString());
        final Map<String, Object> params = new HashMap<>();
        params.put("metadata", meta);
        dstCustomer.update(params);
    }

    public void createDefaultValidVisaCard(String customerId, String teamId) throws StripeException {
        Map<String, Object> retrieveParams = new HashMap<>();
        List<String> expandList = new ArrayList<>();
        expandList.add("sources");
        retrieveParams.put("expand", expandList);
        final Customer customer = Customer.retrieve(customerId, retrieveParams,null);

        final Map<String, String> meta = new HashMap<>();
        meta.put("teamId", teamId);
        final Map<String, Object> params = new HashMap<>();
        params.put("source", "tok_visa");
        params.put("metadata", meta);

        customer.getSources().create(params, requestOptions);
    }

    public void createUnchargeableCreditCard(String customerId, String teamId) throws StripeException {
        Map<String, Object> retrieveParams = new HashMap<>();
        List<String> expandList = new ArrayList<>();
        expandList.add("sources");
        retrieveParams.put("expand", expandList);
        final Customer customer = Customer.retrieve(customerId, retrieveParams,null);

        final Map<String, String> meta = new HashMap<>();
        meta.put("teamId", teamId);
        final Map<String, Object> params = new HashMap<>();
        params.put("source", "tok_chargeCustomerFail");
        params.put("metadata", meta);

        customer.getSources().create(params, requestOptions);
    }

    public void updateCreditCard(String customerId, JSONObject creditCardMetadata) throws StripeException {
        Map<String, Object> retrieveParams = new HashMap<>();
        List<String> expandList = new ArrayList<>();
        expandList.add("sources");
        retrieveParams.put("expand", expandList);
        final Customer customer = Customer.retrieve(customerId, retrieveParams,null);

        final String cardID = customer.getSources().getData().get(0).getId();
        final Card card = (Card) customer.getSources().retrieve(cardID, requestOptions);
        final Map<String, Object> cardParams = new HashMap<>();

        creditCardMetadata.keySet()
                .forEach(key -> cardParams.put(key, creditCardMetadata.getString(key)));
        card.update(cardParams, requestOptions);
    }

    public void removeCreditCard(String customerId) throws StripeException {
        Map<String, Object> retrieveParams = new HashMap<>();
        List<String> expandList = new ArrayList<>();
        expandList.add("sources");
        retrieveParams.put("expand", expandList);
        final Customer customer = Customer.retrieve(customerId, retrieveParams,null);

        final String cardID = customer.getSources().getData().get(0).getId();
        final Card card = (Card) customer.getSources().retrieve(cardID, requestOptions);
        card.delete(requestOptions);
    }

    public boolean isCouponExist(String identifier) {
        try {
            return Coupon.retrieve(identifier, requestOptions) != null;
        } catch (StripeException | NullPointerException e) {
            return false;
        }
    }

    /**
     * https://stripe.com/docs/api/java#create_coupon
     *
     * @param identifier unique coupon identifier
     * @param duration Specifies how long the discount will be in effect. Can be forever, once, or repeating
     * @param percentOff A positive integer between 1 and 100 that represents the discount
     *                   the coupon will apply (required if amount_off is not passed).
     * @param amountOff A positive integer representing the amount in cents to subtract from an invoice total
     *                 (required if percent_off is not passed).
     * @return
     * @throws StripeException
     */
    public Coupon addCoupon(String identifier, String duration,
                            @Nullable Integer percentOff, @Nullable Integer amountOff) throws StripeException {
        final Map<String, Object> couponParams = new HashMap<>();
        ofNullable(percentOff).map(x -> couponParams.put("percent_off", x));
        ofNullable(amountOff).map(x -> {
            couponParams.put("amount_off", x);
            couponParams.put("currency", "EUR");
            return x;
        });
        couponParams.put("duration", duration);
        couponParams.put("id", identifier);
        return Coupon.create(couponParams, requestOptions);
    }

    public String getPlanId(String planName) throws StripeException {
        String firstPlanId = null;
        PlanCollection planCollection;
        do {
            final Map<String, Object> params = new HashMap<>();
            params.put("limit", String.valueOf(PAGE$_LIMIT));
            ofNullable(firstPlanId).map(x -> params.put("starting_after", x));
            planCollection = Plan.list(params, requestOptions);
            List<Plan> plans = planCollection.getData();
            Optional<String> result = Optional.empty();
            for (Plan x : plans) {
                if (x.getNickname() != null && x.getNickname().equalsIgnoreCase(planName)) {
                    String id = x.getId();
                    result = Optional.of(id);
                    break;
                }
            }
            if (result.isPresent()) {
                return result.get();
            }
            firstPlanId = plans.get(plans.size() - 1).getId();
        } while (planCollection.getHasMore());
        throw new IllegalArgumentException(String.format("There is no plan with '%s' name", planName));
    }

    public List<String> getCustomerSubscriptionIds(String customerId) throws StripeException {
        String firstSubscriptionId = null;
        SubscriptionCollection subscriptionCollection;
        final List<String> result = new ArrayList<>();
        do {
            final Map<String, Object> params = new HashMap<>();
            params.put("limit", String.valueOf(PAGE$_LIMIT));
            params.put("customer", customerId);
            ofNullable(firstSubscriptionId).map(x -> params.put("starting_after", x));
            subscriptionCollection = Subscription.list(params, requestOptions);
            List<Subscription> subscriptions = subscriptionCollection.getData();
            result.addAll(subscriptions.stream()
                    .map(Subscription::getId)
                    .collect(Collectors.toList()));
            if (!subscriptions.isEmpty()) {
                firstSubscriptionId = subscriptions.get(subscriptions.size() - 1).getId();
            }
        } while (subscriptionCollection.getHasMore());
        return result;
    }

    public void cancelSubscriptions(List<String> subscriptionIds) throws StripeException {
        for (String subscriptionId : subscriptionIds) {
            Subscription.retrieve(subscriptionId, requestOptions).cancel(new HashMap<>(), requestOptions);
        }
    }

    public void addSubscription(String customerId, String planId) throws StripeException {
        final Map<String, Object> item = new HashMap<>();
        item.put("plan", planId);
        final Map<String, Object> items = new HashMap<>();
        items.put("0", item);
        final Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        params.put("items", items);
        Subscription.create(params, requestOptions);
    }

    public void setSubscriptionMetadata(String customerId, String metadata) throws StripeException {
        final String subId = getCustomerSubscriptionIds(customerId).get(0);
        JSONObject mappedJSON = new JSONObject(metadata);
        final Map<String, String> mappedData = new HashMap<>();
        for(String key: mappedJSON.keySet()) {
            String value = mappedJSON.getString(key);
            mappedData.put(key, value);
        }
        final Map<String, Object> subData = new HashMap<>();
        subData.put("metadata", mappedData);
        Subscription
                .retrieve(subId, requestOptions)
                .update(subData, requestOptions);
    }

    public void createInvoiceItem(String customerId, int amount, String currency, String description) throws StripeException {
        final Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        params.put("amount", amount);
        params.put("currency", currency);
        params.put("description", description);
        InvoiceItem.create(params, requestOptions);
    }

    public void createInvoice(String customerId) throws StripeException {
        final Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        Invoice.create(params, requestOptions);
    }

    public void payAllOutstandingInvoices(String customerId, boolean ignoreDeclinedPayment) throws StripeException {
        boolean hasMoreInvoices = false;
        String startingAfter = "";
        final Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        params.put("limit", 100);
        do {
            if(!startingAfter.isEmpty()) {
                params.put("starting_after", startingAfter);
            }
            InvoiceCollection invoices = Invoice.list(params);
            for(Invoice invoice : invoices.getData()) {
                if(!invoice.getPaid()) {
                    try {
                        invoice.pay();
                    } catch(StripeException ex) {
                        if(!((ex.getMessage().contains("Your card was declined.") || ex.getMessage().contains("no active card")) && ignoreDeclinedPayment)) {
                            throw ex;
                        }
                    }
                }
                startingAfter = invoice.getId();
            }
            hasMoreInvoices = invoices.getHasMore();
        } while(hasMoreInvoices);
    }

    public void tryToPayAllOutstandingInvoices(String customerId) throws StripeException {
        boolean hasMoreInvoices = false;
        String startingAfter = "";
        final Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        params.put("limit", 100);
        do {
            if(!startingAfter.isEmpty()) {
                params.put("starting_after", startingAfter);
            }
            InvoiceCollection invoices = Invoice.list(params);
            for(Invoice invoice : invoices.getData()) {
                if(!invoice.getPaid()) {
                    try {
                        invoice.pay();
                    } catch(StripeException ex) {
                        log.fine("StripeException: " + ex.getMessage());
                    }
                }
                startingAfter = invoice.getId();
            }
            hasMoreInvoices = invoices.getHasMore();
        } while(hasMoreInvoices);
    }

    public void forcePaymentOnLastOutstandingInvoice(String customerId) throws StripeException {
        boolean hasMoreInvoices = false;
        String startingAfter = "";
        final Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        params.put("limit", 100);
        do {
            if(!startingAfter.isEmpty()) {
                params.put("starting_after", startingAfter);
            }
            InvoiceCollection invoices = Invoice.list(params);
            List<Invoice> invoicesByDate = invoices.getData();
            invoicesByDate.sort((Invoice in1, Invoice in2) -> (int)(in1.getDueDate() - in2.getDueDate()));

            for(Invoice invoice : invoicesByDate) {
                if(!invoice.getPaid()) {
                    try {
                        invoice.pay();
                    } catch(StripeException ex) {
                        invoice.setPaid(true);
                        }
                    break;
                }
                startingAfter = invoice.getId();
            }
            hasMoreInvoices = invoices.getHasMore();
        } while(hasMoreInvoices);
    }

    public String createInvoiceForAllPendingInvoiceItems(String customerId) throws StripeException {
        String path = String.format("v1/invoices?customer=%s&pending_invoice_items_behavior=include", customerId);
        return retryOnBackendFailure(() -> {
            HttpURLConnection c = buildDefaultRequestWithAuth(
                    path,
                    MediaType.APPLICATION_FORM_URLENCODED,
                    MediaType.APPLICATION_JSON, "Bearer " + stripeAPIKey);
            String response = httpPost(c, path.split("\\?")[1].getBytes(StandardCharsets.UTF_8), new int[]{HTTP_OK, HTTP_ACCEPTED});
            log.fine("Response: " + response);
            return new JSONObject(response).getString("id");
        });
    }

    public void finalizeAllScheduledInvoices(String customerId) throws StripeException {
        boolean hasMoreInvoices;
        String startingAfter = "";
        final Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        params.put("limit", 100);
        do {
            if(!startingAfter.isEmpty()) {
                params.put("starting_after", startingAfter);
            }
            InvoiceCollection invoices = Invoice.list(params);
            for(Invoice invoice : invoices.getData()) {
                log.fine("Processing invoice: " + invoice.getId() + " (amount: " + invoice.getTotal() + ")");
                log.fine("Attempted: " + invoice.getAttempted());
                if(!invoice.getAttempted()) {
                    retryOnBackendFailure(() -> {
                        HttpURLConnection c = buildDefaultRequestWithAuth(
                                String.format("v1/invoices/%s/finalize", invoice.getId()), "Bearer " + stripeAPIKey);
                        String response = httpPost(c, "".getBytes(StandardCharsets.UTF_8), new int[]{HTTP_OK, HTTP_ACCEPTED});
                        log.fine("Finalize response: " + response);
                        return null;
                    });
                }
                startingAfter = invoice.getId();
            }
            hasMoreInvoices = invoices.getHasMore();
        } while(hasMoreInvoices);
    }

    public int getNumberOfInvoices(String customerId) throws StripeException {
        final Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        InvoiceCollection invoiceCollection = Invoice.list(params);
        return (int)invoiceCollection.getData().stream()
                .filter(i -> i.getTotal() != 0)
                .count();
    }

    public Customer getCustomer(String customerId) throws StripeException {
        return Customer.retrieve(customerId, requestOptions);
    }
}
