package com.wire.android.shared.prekey.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

typealias ClientsOfUsersMap = Map<String, List<String>>
typealias ClientsOfQualifiedUsersMap = Map<String, Map<String, List<String>>>

@JvmSuppressWildcards
interface PreKeyAPI {

    @POST(LIST_PRE_KEYS_QUALIFIED_ENDPOINT)
    suspend fun preKeysByClientsOfQualifiedUsers(@Body preKeyListParameter: ClientsOfQualifiedUsersMap):
            Response<QualifiedPreKeyListResponse>

    @Deprecated(
        "This endpoint does not consider domain, needed for Federation",
        ReplaceWith("preKeysByClientsOfQualifiedUsers")
    )
    @POST(LIST_PRE_KEYS_ENDPOINT)
    suspend fun preKeysByClientsOfUsers(@Body preKeyListParameter: ClientsOfUsersMap): Response<PreKeyListResponse>


    companion object {
        const val LIST_PRE_KEYS_QUALIFIED_ENDPOINT = "/users/list-prekeys"
        const val LIST_PRE_KEYS_ENDPOINT = "/users/prekeys"
    }
}
