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
package com.wire.android.tests.core.utils

import com.wire.android.tests.core.exceptions.RESTError
import com.wire.android.tests.core.services.RESTResponseHandler
import org.apache.commons.lang3.StringUtils
import java.net.HttpURLConnection
import java.net.URL
import java.util.function.Supplier
import java.util.logging.Logger
import javax.ws.rs.ProcessingException
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation.Builder
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class CommonRESTHandlers @JvmOverloads constructor(
    private val responseHandler: RESTResponseHandler,
    private val maxRetries: Int = 1
) {
    companion object {
        private val log: Logger = ZetaLogger.getLog(CommonRESTHandlers::class.simpleName)
        private const val EMPTY_LOG_RECORD = "EMPTY"
        private const val IS_ALIVE_VERIFICATION_TIMEOUT_MS = 5000
        private const val MAX_SINGLE_ENTITY_LENGTH_IN_LOG = 400

        fun isAlive(siteURL: URL): Boolean {
            return try {
                (siteURL.openConnection() as HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    connectTimeout = IS_ALIVE_VERIFICATION_TIMEOUT_MS
                    readTimeout = IS_ALIVE_VERIFICATION_TIMEOUT_MS
                    connect()
                }.let { connection ->
                    val responseCode = connection.responseCode
                    log.info("Response code from ${siteURL}: $responseCode")
                    responseCode == HttpURLConnection.HTTP_OK
                }
            } catch (e: Exception) {
                false
            }
        }

        private fun formatLogRecord(entity: Any?): String {
            if (entity == null) return EMPTY_LOG_RECORD

            val result = when (entity) {
                is String -> entity
                else -> entity.toString()
            }

            return if (result.isEmpty()) EMPTY_LOG_RECORD
            else StringUtils.abbreviate(result, MAX_SINGLE_ENTITY_LENGTH_IN_LOG)
        }
    }

    private fun retryRequest(requestFunc: Supplier<Response>): Response {
        var tryNum = 0
        var savedException: ProcessingException? = null
        do {
            try {
                return requestFunc.get()
            } catch (e: ProcessingException) {
                savedException = e
            }
        } while (++tryNum < maxRetries)
        throw savedException!!
    }

    fun <T> httpPost(
        webResource: Builder,
        entity: Any?,
        responseEntityType: Class<T>,
        acceptableResponseCodes: IntArray
    ): T = httpPost(webResource, MediaType.APPLICATION_JSON, entity, responseEntityType, acceptableResponseCodes)

    fun <T> httpPost(
        webResource: Builder,
        contentType: String,
        entity: Any?,
        responseEntityType: Class<T>,
        acceptableResponseCodes: IntArray
    ): T {
        log.info("POST REQUEST...")
        log.info(" >>> Input data: ${formatLogRecord(entity)}")
        val response = retryRequest {
            webResource.post(Entity.entity(entity, contentType), Response::class.java)
        }
        return getEntityFromResponse(response, responseEntityType, acceptableResponseCodes)
    }

    fun httpPostFullResponse(
        webResource: Builder,
        contentType: String,
        entity: Any?
    ): Response {
        log.info("POST REQUEST...")
        log.info(" >>> Input data: ${formatLogRecord(entity)}")
        return retryRequest {
            webResource.post(Entity.entity(entity, contentType), Response::class.java)
        }
    }

    fun <T> getEntityFromResponse(
        response: Response,
        responseEntityType: Class<T>,
        acceptableResponseCodes: IntArray
    ): T {
        return try {
            response.bufferEntity()
            val responseEntity = response.readEntity(responseEntityType)
            log.info(" >>> Response: ${formatLogRecord(responseEntity)}")
            responseHandler.verifyRequestResult(
                response.status,
                acceptableResponseCodes,
                formatLogRecord(responseEntity)
            )
            responseEntity
        } catch (e: ProcessingException) {
            handleResponseError(response, responseEntityType, acceptableResponseCodes)
            throw RESTError("Failed to process response", 500)
        } catch (e: IllegalStateException) {
            handleResponseError(response, responseEntityType, acceptableResponseCodes)
            throw RESTError("Invalid response state", 400)
        } catch (e: NullPointerException) {
            handleResponseError(response, responseEntityType, acceptableResponseCodes)
            throw RESTError("Null response", 500)
        }
    }

    private fun <T> handleResponseError(
        response: Response,
        responseEntityType: Class<T>,
        acceptableResponseCodes: IntArray
    ) {
        if (responseEntityType.name != "java.lang.String") {
            try {
                val responseString = response.readEntity(String::class.java)
                log.info(" >>> Response: ${formatLogRecord(responseString)}")
                responseHandler.verifyRequestResult(
                    response.status,
                    acceptableResponseCodes,
                    formatLogRecord(responseString))
            } catch (ex: Exception) {
                log.warning(ex.message)
            }
        }
    }

    fun httpPost(
        webResource: Builder,
        entity: Any?,
        acceptableResponseCodes: IntArray
    ): String = httpPost(webResource, entity, String::class.java, acceptableResponseCodes) ?: ""

    fun httpPost(
        webResource: Builder,
        entity: Any?,
        contentType: String,
        acceptableResponseCodes: IntArray
    ): String = httpPost(webResource, contentType, entity, String::class.java, acceptableResponseCodes) ?: ""

    fun <T> httpPut(
        webResource: Builder,
        entity: Any?,
        responseEntityType: Class<T>,
        acceptableResponseCodes: IntArray
    ): T {
        log.info("PUT REQUEST...")
        log.info(" >>> Input data: ${formatLogRecord(entity)}")
        val response = retryRequest {
            webResource.put(Entity.entity(entity, MediaType.APPLICATION_JSON), Response::class.java)
        }
        return getEntityFromResponse(response, responseEntityType, acceptableResponseCodes)
    }

    fun httpPut(
        webResource: Builder,
        entity: Any?,
        acceptableResponseCodes: IntArray
    ): String = httpPut(webResource, entity, String::class.java, acceptableResponseCodes) ?: ""

    fun <T> httpDelete(
        webResource: Builder,
        entity: Any?,
        responseEntityType: Class<T>,
        acceptableResponseCodes: IntArray
    ): T {
        log.info("DELETE REQUEST...")
        val response = retryRequest {
            webResource.method("DELETE", Entity.entity(entity, MediaType.APPLICATION_JSON), Response::class.java)
        }
        return getEntityFromResponse(response, responseEntityType, acceptableResponseCodes)
    }

    fun httpDelete(
        webResource: Builder,
        entity: Any?,
        acceptableResponseCodes: IntArray
    ): String = httpDelete(webResource, entity, String::class.java, acceptableResponseCodes) ?: ""

    fun httpDelete(
        webResource: Builder,
        acceptableResponseCodes: IntArray
    ): String = httpDelete(webResource, null, String::class.java, acceptableResponseCodes) ?: ""

    fun <T> httpGet(
        webResource: Builder,
        responseEntityType: GenericType<T>,
        acceptableResponseCodes: IntArray
    ): T? {
        log.info("GET REQUEST...")
        val response = retryRequest {
            webResource.get(Response::class.java)
        }
        return try {
            response.bufferEntity()
            val responseEntity = response.readEntity(responseEntityType)
            log.info(" >>> Response: ${formatLogRecord(responseEntity)}")
            responseHandler.verifyRequestResult(
                response.status,
                acceptableResponseCodes,
                formatLogRecord(responseEntity))
                        responseEntity
        } catch (e: ProcessingException) {
            handleGetResponseError(response, responseEntityType, acceptableResponseCodes)
            throw RESTError("Failed to process response", 500)
        } catch (e: IllegalStateException) {
            handleGetResponseError(response, responseEntityType, acceptableResponseCodes)
            throw RESTError("Invalid response state", 400)
        } catch (e: NullPointerException) {
            handleGetResponseError(response, responseEntityType, acceptableResponseCodes)
            throw RESTError("Null response", 500)
        }
    }

    private fun <T> handleGetResponseError(
        response: Response,
        responseEntityType: GenericType<T>,
        acceptableResponseCodes: IntArray
    ) {
        if (responseEntityType.type == String::class.java){
            try {
                val responseString = response.readEntity(String::class.java)
                log.info(" >>> Response: ${formatLogRecord(responseString)}")
                responseHandler.verifyRequestResult(
                    response.status,
                    acceptableResponseCodes,
                    formatLogRecord(responseString))
            } catch (ex: Exception) {
                log.warning(ex.message)
            }
        }
    }

    fun httpGet(
        webResource: Builder,
        acceptableResponseCodes: IntArray
    ): String = httpGet(webResource, GenericType<String>(String.Companion::class.java), acceptableResponseCodes) ?: ""

    fun getResponseCode(
        webResource: Builder,
        contentType: String,
        entity: Any?
    ): Int {
        log.info("POST REQUEST...")
        log.info(" >>> Input data: ${formatLogRecord(entity)}")
        return retryRequest {
            webResource.post(Entity.entity(entity, contentType), Response::class.java)
        }.status
    }

    fun getResponseCode(webResource: Builder): Int {
        log.info("GET REQUEST...")
        return retryRequest {
            webResource.get(Response::class.java)
        }.status
    }
}
