package com.wire.android.shared.prekey

import com.wire.android.core.crypto.model.PreKey
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.shared.user.QualifiedId

interface PreKeyRepository {
    /**
     * Given a map of qualified user IDs to client IDs, fetches one PreKey for each client.
     * @param qualifiedIdsMap, a map of qualified user IDs to the wanted client IDs
     */
    suspend fun preKeysOfClientsByUsers(qualifiedIdsMap: Map<QualifiedId, List<String>>): Either<Failure, PreKey>
}
