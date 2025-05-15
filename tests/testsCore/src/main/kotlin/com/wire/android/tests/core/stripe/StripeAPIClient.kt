/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.tests.core.stripe

import com.stripe.Stripe
import com.stripe.exception.StripeException
import com.stripe.model.*
import com.stripe.net.RequestOptions
import com.wire.android.tests.core.config.Credentials
import com.wire.android.tests.core.exceptions.HttpRequestException
import com.wire.android.tests.core.utils.Timedelta
import com.wire.android.tests.core.utils.URLTransformer
import com.wire.android.tests.core.utils.ZetaLogger
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_ACCEPTED
import java.net.HttpURLConnection.HTTP_OK
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.Optional.ofNullable
import java.util.function.Supplier
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import javax.ws.rs.core.MediaType
import kotlin.collections.HashMap


class StripeAPIClient {

    val log: Logger = ZetaLogger.getLog(StripeAPIClient::class.simpleName)

    val `PAGE$_LIMIT`: Int = 10
    private var requestOptions: RequestOptions? = null
    val BASEURI: String = "https://api.stripe.com"
    val credentials = Credentials()
    val stripeAPIKey: String = credentials.getCredentials("STRIPE_API_KEY")

    fun StripeAPIClient() {
        Stripe.apiKey = stripeAPIKey
        this.requestOptions = RequestOptions.RequestOptionsBuilder().setApiKey(stripeAPIKey).build()
    }


    // Helpers
    private fun truncate(text: String): String {
        return truncate(text, 100)
    }

    private fun truncateOnlyOnBig(text: String): String {
        return truncate(text, 5000)
    }

    private fun truncate(text: String, maxLength: Int): String {
        if (text.length > maxLength) {
            return text.substring(0, maxLength) + "..."
        }
        return text
    }

    @Throws(IOException::class)
    private fun writeStream(data: String, os: OutputStream) {
        val wr = DataOutputStream(os)
        val writer = BufferedWriter(OutputStreamWriter(wr, StandardCharsets.UTF_8))
        try {
            writer.write(data)
        } finally {
            writer.close()
            wr.close()
        }
    }

    @Throws(IOException::class)
    private fun writeStream(data: ByteArray, os: OutputStream) {
        val wr = DataOutputStream(os)
        try {
            wr.write(data)
        } finally {
            wr.close()
        }
    }

    @Throws(IOException::class)
    private fun readStream(`is`: InputStream?): String {
        if (`is` != null) {
            BufferedReader(InputStreamReader(`is`)).use { `in` ->
                var inputLine: String?
                var content = ""
                while ((`in`.readLine().also { inputLine = it }) != null) {
                    content += inputLine
                }
                return content
            }
        } else {
            return ""
        }
    }

    private fun logResponseAndStatusCode(response: String, responseCode: Int) {
        if (response.isEmpty()) {
            log.info(String.format(" >>> Response (%s) with no response body", responseCode))
        } else {
            if (log.isLoggable(Level.FINE)) {
                log.info(String.format(" >>> Response (%s): %s", responseCode, truncateOnlyOnBig(response)))
            } else {
                log.info(String.format(" >>> Response (%s): %s", responseCode, truncate(response)))
            }
        }
    }

    private fun <T> retryOnBackendFailure(r: Supplier<T>): T {
        var ntry = 1
        var savedException: HttpRequestException? = null
        while (ntry <= 2) {
            try {
                return r.get()
            } catch (e: HttpRequestException) {
                savedException = e
                Timedelta.ofMillis((2000 * ntry).toDouble()).sleep()
            }
            ntry++
        }
        throw savedException!!
    }

    private fun assertResponseCode(responseCode: Int, acceptableResponseCodes: IntArray) {
        if (Arrays.stream(acceptableResponseCodes).noneMatch { a: Int -> a == responseCode }) {
            throw HttpRequestException(
                String.format(
                    "Backend request failed. Request return code is: %d. Expected codes are: %s.",
                    responseCode,
                    acceptableResponseCodes.contentToString()
                ),
                responseCode
            )
        }
    }


    private fun logHttpRequestProperties(c: HttpURLConnection) {
        if (log.isLoggable(Level.FINE)) {
            for (property in c.requestProperties.keys) {
                val values = listOf(c.getRequestProperty(property))
                log.fine(String.format("%s: %s", property, java.lang.String.join(", ", values)))
            }
        }
    }

    private fun logRequest(request: String) {
        if (request.isEmpty()) {
            log.info(" >>> Request with no request body")
        } else {
            if (log.isLoggable(Level.FINE)) {
                log.info(String.format(" >>> Request: %s", request))
            } else {
                log.info(String.format(" >>> Request: %s", truncate(request)))
            }
        }
    }


    //
    // Backend
    private fun buildDefaultRequestWithAuth(path: String, header: String): HttpURLConnection? {
        return buildDefaultRequestWithAuth(path, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, header)
    }

    private fun buildDefaultRequestWithAuth(
        path: String, contentType: String, acceptType: String,
        header: String
    ): HttpURLConnection? {
        var path = path
        try {
            var parameters: String? = null
            if (path.contains("?")) {
                parameters = URLTransformer.getQuery(path)
                path = URLTransformer.getPath(path)
            }
            val url = URL(String.format("%s/%s", BASEURI, path))
            var c: HttpURLConnection? = null
            try {
                c = url.openConnection() as HttpURLConnection
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
            c!!.setRequestProperty("Content-Type", contentType)
            c!!.setRequestProperty("Accept", acceptType)
            c!!.setRequestProperty("Authorization", header)
            if (parameters != null) {
                c!!.setRequestProperty(
                    "Content-Length",
                    parameters.toByteArray(StandardCharsets.UTF_8).size.toString()
                )
            }
            return c
        } catch (ex: MalformedURLException) {
            return null
        }
    }

    private fun httpPost(c: HttpURLConnection, requestBody: ByteArray, acceptableResponseCodes: IntArray): String {
        var response = ""
        var status = -1
        try {
            log.info("POST " + c.url)
            c.requestMethod = "POST"
            logHttpRequestProperties(c)
            //logRequest(requestBody);
            c.doOutput = true
            writeStream(requestBody, c.outputStream)
            status = c.responseCode
            response = readStream(c.inputStream)
            logResponseAndStatusCode(response, status)
            assertResponseCode(status, acceptableResponseCodes)
            return response
        } catch (e: IOException) {
            try {
                response = readStream(c.errorStream)
            } catch (ex: IOException) {
                log.fine("Could not read error stream: " + e.message)
            }
            if (Arrays.stream(acceptableResponseCodes).anyMatch { acceptable: Int -> acceptable > 400 }) {
                assertResponseCode(status, acceptableResponseCodes)
                log.info(String.format(">>> Response (%s): %s", status, response))
                return response
            } else {
                val error = String.format("%s (%s): %s", e.message, status, response)
                log.severe(error)
                throw HttpRequestException(error, status)
            }
        } finally {
            c.disconnect()
        }
    }


    //
    @Throws(StripeException::class)
    fun getTrialPeriodDuration(customerId: String): Timedelta {
        val params: MutableMap<String, Any> = HashMap()
        params["customer"] = customerId
        val subscriptions = Subscription.list(params)
        if (subscriptions.data.size == 1) {
            return Timedelta.ofSeconds((subscriptions.data[0].trialEnd - subscriptions.data[0].trialStart).toDouble())
        } else {
            throw IllegalStateException("Customer doesn't have 1 subscription as expected")
        }
    }

    @Throws(StripeException::class)
    fun changeTrialPeriodDuration(customerId: String, newDuration: Timedelta) {
        val params: MutableMap<String, Any> = HashMap()
        params["customer"] = customerId
        params["status"] = "trialing"
        val subscriptions = Subscription.list(params)
        if (subscriptions.data.size == 1) {
            val updateParams: MutableMap<String, Any> = HashMap()
            if (newDuration.asMillis() > 0) {
                updateParams["trial_end"] = Timedelta
                    .now()
                    .sum(newDuration)
                    .asSeconds()
            } else {
                updateParams["trial_end"] = "now"
            }
            subscriptions.data[0].update(updateParams, requestOptions)
        } else {
            throw IllegalStateException("Customer doesn't have 1 trialing subscription as expected")
        }
    }

    @Throws(StripeException::class)
    fun changeGracePeriodDuration(customerId: String?, newDuration: Timedelta?) {
        val dstCustomer = Customer.retrieve(customerId, requestOptions)
        val metadata = dstCustomer.metadata
        val suspend = JSONObject(metadata["suspend"])
        suspend.put("graceEnding", Timedelta.now().sum(newDuration!!).asSeconds())
        val meta: MutableMap<String, String> = HashMap()
        meta["suspend"] = suspend.toString()
        val params: MutableMap<String, Any> = HashMap()
        params["metadata"] = meta
        dstCustomer.update(params)
    }


    @Throws(StripeException::class)
    fun createDefaultValidVisaCard(customerId: String?, teamId: String) {
        val retrieveParams: MutableMap<String, Any> = HashMap()
        val expandList: MutableList<String> = ArrayList()
        expandList.add("sources")
        retrieveParams["expand"] = expandList
        val customer = Customer.retrieve(customerId, retrieveParams, null)

        val meta: MutableMap<String, String> = HashMap()
        meta["teamId"] = teamId
        val params: MutableMap<String, Any> = HashMap()
        params["source"] = "tok_visa"
        params["metadata"] = meta

        customer.sources.create(params, requestOptions)
    }

    @Throws(StripeException::class)
    fun createUnchargeableCreditCard(customerId: String?, teamId: String) {
        val retrieveParams: MutableMap<String, Any> = HashMap()
        val expandList: MutableList<String> = ArrayList()
        expandList.add("sources")
        retrieveParams["expand"] = expandList
        val customer = Customer.retrieve(customerId, retrieveParams, null)

        val meta: MutableMap<String, String> = HashMap()
        meta["teamId"] = teamId
        val params: MutableMap<String, Any> = HashMap()
        params["source"] = "tok_chargeCustomerFail"
        params["metadata"] = meta

        customer.sources.create(params, requestOptions)
    }

    @Throws(StripeException::class)
    fun updateCreditCard(customerId: String?, creditCardMetadata: JSONObject) {
        val retrieveParams: MutableMap<String, Any> = HashMap()
        val expandList: MutableList<String> = ArrayList()
        expandList.add("sources")
        retrieveParams["expand"] = expandList
        val customer = Customer.retrieve(customerId, retrieveParams, null)

        val cardID = customer.sources.data[0].id
        val card = customer.sources.retrieve(cardID, requestOptions) as Card
        val cardParams: MutableMap<String, Any> = HashMap()

        creditCardMetadata.keys()
            .forEach { key -> cardParams.put(key, creditCardMetadata.getString(key)) }
        card.update(cardParams, requestOptions)
    }

    @Throws(StripeException::class)
    fun removeCreditCard(customerId: String?) {
        val retrieveParams: MutableMap<String, Any> = HashMap()
        val expandList: MutableList<String> = ArrayList()
        expandList.add("sources")
        retrieveParams["expand"] = expandList
        val customer = Customer.retrieve(customerId, retrieveParams, null)

        val cardID = customer.sources.data[0].id
        val card = customer.sources.retrieve(cardID, requestOptions) as Card
        card.delete(requestOptions)
    }

    fun isCouponExist(identifier: String?): Boolean {
        return try {
            Coupon.retrieve(identifier, requestOptions) != null
        } catch (e: StripeException) {
            false
        } catch (e: NullPointerException) {
            false
        }
    }

    /**
     * https://stripe.com/docs/api/java#create_coupon
     *
     * @param identifier unique coupon identifier
     * @param duration Specifies how long the discount will be in effect. Can be forever, once, or repeating
     * @param percentOff A positive integer between 1 and 100 that represents the discount
     * the coupon will apply (required if amount_off is not passed).
     * @param amountOff A positive integer representing the amount in cents to subtract from an invoice total
     * (required if percent_off is not passed).
     * @return
     * @throws StripeException
     */
    @Throws(StripeException::class)
    fun addCoupon(
        identifier: String, duration: String,
         percentOff: Int?, amountOff: Int?
    ): Coupon {
        val couponParams: MutableMap<String, Any> = HashMap()
        ofNullable(percentOff).map { x -> couponParams.put("percent_off", x) }
        ofNullable(amountOff).map { x ->
            couponParams["amount_off"] = x
            couponParams["currency"] = "EUR"
            x
        }
        couponParams["duration"] = duration
        couponParams["id"] = identifier
        return Coupon.create(couponParams, requestOptions)
    }

    @Throws(StripeException::class)
    fun getPlanId(planName: String?): String {
        var firstPlanId: String? = null
        var planCollection: PlanCollection
        do {
            val params: MutableMap<String, Any> = HashMap()
            params["limit"] = `PAGE$_LIMIT`.toString()
            ofNullable(firstPlanId).map { x -> params.put("starting_after", x) }
            planCollection = Plan.list(params, requestOptions)
            val plans = planCollection.data
            var result: Optional<String> = Optional.empty()
            for (x in plans) {
                if (planName != null) {
                    if (x.nickname != null && x.nickname.uppercase() == planName.uppercase()) {
                        val id = x.id
                        result = Optional.of(id)
                        break
                    }
                }
            }
            if (result.isPresent) {
                return result.get()
            }
            firstPlanId = plans[plans.size - 1].id
        } while (planCollection.hasMore)
        throw IllegalArgumentException(String.format("There is no plan with '%s' name", planName))
    }

    @Throws(StripeException::class)
    fun getCustomerSubscriptionIds(customerId: String): List<String> {
        var firstSubscriptionId: String? = null
        var subscriptionCollection: SubscriptionCollection
        val result: MutableList<String> = ArrayList()
        do {
            val params: MutableMap<String, Any> = HashMap()
            params["limit"] = `PAGE$_LIMIT`.toString()
            params["customer"] = customerId
            ofNullable(firstSubscriptionId).map { x -> params.put("starting_after", x) }
            subscriptionCollection = Subscription.list(params, requestOptions)
            val subscriptions = subscriptionCollection.data
            result.addAll(
                subscriptions.stream()
                    .map(Subscription::getId)
                    .collect(Collectors.toList())
            )
            if (subscriptions.isNotEmpty()) {
                firstSubscriptionId = subscriptions[subscriptions.size - 1].id
            }
        } while (subscriptionCollection.hasMore)
        return result
    }


    @Throws(StripeException::class)
    fun cancelSubscriptions(subscriptionIds: List<String?>) {
        for (subscriptionId in subscriptionIds) {
            Subscription.retrieve(subscriptionId, requestOptions).cancel(HashMap(), requestOptions)

        }
    }

    @Throws(StripeException::class)
    fun addSubscription(customerId: String, planId: String) {
        val item: MutableMap<String, Any> = HashMap()
        item["plan"] = planId
        val items: MutableMap<String, Any> = HashMap()
        items["0"] = item
        val params: MutableMap<String, Any> = HashMap()
        params["customer"] = customerId
        params["items"] = items
        Subscription.create(params, requestOptions)
    }

    @Throws(StripeException::class)
    fun setSubscriptionMetadata(customerId: String?, metadata: String?) {
        val subId = getCustomerSubscriptionIds(customerId!!)[0]
        val mappedJSON = JSONObject(metadata)
        val mappedData: MutableMap<String, String> = HashMap()
        for (key in mappedJSON.keys()) {
            val value = mappedJSON.getString(key)
            mappedData[key] = value
        }
        val subData: MutableMap<String, Any> = HashMap()
        subData["metadata"] = mappedData
        Subscription
            .retrieve(subId, requestOptions)
            .update(subData, requestOptions)
    }

    @Throws(StripeException::class)
    fun createInvoiceItem(customerId: String, amount: Int, currency: String, description: String) {
        val params: MutableMap<String, Any> = HashMap()
        params["customer"] = customerId
        params["amount"] = amount
        params["currency"] = currency
        params["description"] = description
        InvoiceItem.create(params, requestOptions)
    }

    @Throws(StripeException::class)
    fun createInvoice(customerId: String) {
        val params: MutableMap<String, Any> = HashMap()
        params["customer"] = customerId
        Invoice.create(params, requestOptions)
    }

    @Throws(StripeException::class)
    fun payAllOutstandingInvoices(customerId: String, ignoreDeclinedPayment: Boolean) {
        var hasMoreInvoices = false
        var startingAfter = ""
        val params: MutableMap<String, Any> = HashMap()
        params["customer"] = customerId
        params["limit"] = 100
        do {
            if (!startingAfter.isEmpty()) {
                params["starting_after"] = startingAfter
            }
            val invoices = Invoice.list(params)
            for (invoice in invoices.data) {
                if (!invoice.paid) {
                    try {
                        invoice.pay()
                    } catch (ex: StripeException) {
                        if (!((ex.message!!.contains("Your card was declined.") || ex.message!!.contains("no active card")) && ignoreDeclinedPayment)) {
                            throw ex
                        }
                    }
                }
                startingAfter = invoice.id
            }
            hasMoreInvoices = invoices.hasMore
        } while (hasMoreInvoices)
    }

    @Throws(StripeException::class)
    fun tryToPayAllOutstandingInvoices(customerId: String) {
        var hasMoreInvoices = false
        var startingAfter = ""
        val params: MutableMap<String, Any> = HashMap()
        params["customer"] = customerId
        params["limit"] = 100
        do {
            if (!startingAfter.isEmpty()) {
                params["starting_after"] = startingAfter
            }
            val invoices = Invoice.list(params)
            for (invoice in invoices.data) {
                if (!invoice.paid) {
                    try {
                        invoice.pay()
                    } catch (ex: StripeException) {
                        log.fine("StripeException: " + ex.message)
                    }
                }
                startingAfter = invoice.id
            }
            hasMoreInvoices = invoices.hasMore
        } while (hasMoreInvoices)
    }

    @Throws(StripeException::class)
    fun forcePaymentOnLastOutstandingInvoice(customerId: String) {
        var hasMoreInvoices = false
        var startingAfter = ""
        val params: MutableMap<String, Any> = HashMap()
        params["customer"] = customerId
        params["limit"] = 100
        do {
            if (startingAfter.isNotEmpty()) {
                params["starting_after"] = startingAfter
            }
            val invoices = Invoice.list(params)
            val invoicesByDate = invoices.data
            invoicesByDate.sortWith { in1: Invoice, in2: Invoice -> (in1.dueDate - in2.dueDate).toInt() }

            for (invoice in invoicesByDate) {
                if (!invoice.paid) {
                    try {
                        invoice.pay()
                    } catch (ex: StripeException) {
                        invoice.paid = true
                    }
                    break
                }
                startingAfter = invoice.id
            }
            hasMoreInvoices = invoices.hasMore
        } while (hasMoreInvoices)
    }

    @Throws(StripeException::class)
    fun createInvoiceForAllPendingInvoiceItems(customerId: String?): String {
        val path = String.format("v1/invoices?customer=%s&pending_invoice_items_behavior=include", customerId)
        return retryOnBackendFailure {
            val c = buildDefaultRequestWithAuth(
                path,
                MediaType.APPLICATION_FORM_URLENCODED,
                MediaType.APPLICATION_JSON, "Bearer $stripeAPIKey"
            )
            val response = httpPost(
                c!!,
                path.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                    .toByteArray(StandardCharsets.UTF_8),
                intArrayOf(HTTP_OK, HTTP_ACCEPTED)
            )
            log.fine("Response: $response")
            JSONObject(response).getString("id")
        }
    }

    @Throws(StripeException::class)
    fun finalizeAllScheduledInvoices(customerId: String) {
        var hasMoreInvoices: Boolean
        var startingAfter = ""
        val params: MutableMap<String, Any> = HashMap()
        params["customer"] = customerId
        params["limit"] = 100
        do {
            if (!startingAfter.isEmpty()) {
                params["starting_after"] = startingAfter
            }
            val invoices = Invoice.list(params)
            for (invoice in invoices.data) {
                log.fine("Processing invoice: " + invoice.id + " (amount: " + invoice.total + ")")
                log.fine("Attempted: " + invoice.attempted)
                if (!invoice.attempted) {
                    retryOnBackendFailure {
                        val c = buildDefaultRequestWithAuth(
                            String.format("v1/invoices/%s/finalize", invoice.id), "Bearer $stripeAPIKey"
                        )
                        val response = httpPost(
                            c!!,
                            "".toByteArray(StandardCharsets.UTF_8),
                            intArrayOf(HTTP_OK, HTTP_ACCEPTED)
                        )
                        log.fine("Finalize response: $response")
                        null
                    }
                }
                startingAfter = invoice.id
            }
            hasMoreInvoices = invoices.hasMore
        } while (hasMoreInvoices)
    }

    @Throws(StripeException::class)
    fun getNumberOfInvoices(customerId: String): Int {
        val params: MutableMap<String, Any> = HashMap()
        params["customer"] = customerId
        val invoiceCollection = Invoice.list(params)
        return invoiceCollection.data.stream()
            .filter { i: Invoice -> i.total != 0L }
            .count().toInt()
    }

    @Throws(StripeException::class)
    fun getCustomer(customerId: String?): Customer {
        return Customer.retrieve(customerId, requestOptions)
    }

}
