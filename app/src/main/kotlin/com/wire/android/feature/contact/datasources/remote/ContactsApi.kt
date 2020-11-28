package com.wire.android.feature.contact.datasources.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ContactsApi {

    @GET(USERS)
    suspend fun contactsById(@Query(IDS_QUERY_KEY) ids: Set<String>): Response<List<ContactResponse>>

    companion object {
        private const val USERS = "/users"
        private const val IDS_QUERY_KEY = "ids"
    }
}
