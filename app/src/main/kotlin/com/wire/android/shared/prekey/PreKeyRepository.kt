package com.wire.android.shared.prekey

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.shared.prekey.data.QualifiedUserPreKeyInfo
import com.wire.android.shared.prekey.data.UserPreKeyInfo
import com.wire.android.shared.user.QualifiedId

interface PreKeyRepository {
    /**
     * Given a map of qualified user IDs to client IDs, fetches one PreKey for each client.
     * @param qualifiedIdsMap, a map of qualified user IDs to the wanted client IDs
     */
    suspend fun preKeysOfClientsByQualifiedUsers(
        qualifiedIdsMap: Map<QualifiedId, List<String>>
    ): Either<Failure, List<QualifiedUserPreKeyInfo>>

    /**
     * Given a map of user IDs to client IDs, fetches one PreKey for each client.
     * @param contactIdsMap, a map of user IDs to the wanted client IDs
     */
    @Deprecated("Qualified ID should be used", ReplaceWith("preKeysOfClientsByQualifiedUsers"))
    suspend fun preKeysOfClientsByUsers(
        contactIdsMap: Map<String, List<String>>
    ): Either<Failure, List<UserPreKeyInfo>>
}
