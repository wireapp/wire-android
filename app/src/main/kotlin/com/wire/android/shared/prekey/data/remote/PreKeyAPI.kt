package com.wire.android.shared.prekey.data.remote

import retrofit2.Response
import retrofit2.http.POST

interface PreKeyAPI {

    @POST(LIST_PRE_KEYS_QUALIFIED_ENDPOINT)
    suspend fun preKeysByClientsOfQualifiedUsers(preKeyListParameter: Map<String, Map<String, List<String>>>):
            Response<QualifiedPreKeyListResponse>

    @Deprecated(
            "This endpoint does not consider domain, needed for Federation",
            ReplaceWith("preKeysByClientsOfQualifiedUsers")
    )
    @POST(LIST_PRE_KEYS_ENDPOINT)
    suspend fun preKeysByClientsOfUsers(preKeyListParameter: Map<String, List<String>>): Response<PreKeyListResponse>

    companion object {
        const val LIST_PRE_KEYS_QUALIFIED_ENDPOINT = "/users/list-prekeys"
        const val LIST_PRE_KEYS_ENDPOINT = "/users/prekeys"
    }
}
