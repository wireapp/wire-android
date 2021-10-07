package com.wire.android.shared.prekey.data.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import com.wire.android.shared.prekey.data.QualifiedUserPreKeyInfo
import com.wire.android.shared.prekey.data.UserPreKeyInfo
import com.wire.android.shared.user.QualifiedId

class PreKeyRemoteDataSource(
        override val networkHandler: NetworkHandler,
        private val preKeyAPI: PreKeyAPI,
        private val remotePreKeyListMapper: RemotePreKeyListMapper
) : ApiService() {

    suspend fun preKeysForMultipleQualifiedUsers(qualifiedIdMap: Map<QualifiedId, List<String>>): Either<Failure, List<QualifiedUserPreKeyInfo>> =
            request {
                val mapOfIds = qualifiedIdMap
                        .mapValues { entry -> mapOf(entry.key.id to entry.value) }
                        .mapKeys { entry -> entry.key.domain }

                preKeyAPI.preKeysByClientsOfQualifiedUsers(mapOfIds)
            }.map(remotePreKeyListMapper::fromRemoteQualifiedPreKeyInfoMap)

    @Deprecated(
            "This function does not consider domain, needed for Federation",
            ReplaceWith("preKeysForMultipleQualifiedUsers")
    )
    suspend fun preKeysForMultipleUsers(idMap: Map<String, List<String>>): Either<Failure, List<UserPreKeyInfo>> =
            request {
                preKeyAPI.preKeysByClientsOfUsers(idMap)
            }.map(remotePreKeyListMapper::fromRemotePreKeyInfoMap)

}
