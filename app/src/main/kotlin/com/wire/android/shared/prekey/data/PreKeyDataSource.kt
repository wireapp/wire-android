package com.wire.android.shared.prekey.data

import com.wire.android.core.crypto.model.PreKey
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.shared.prekey.PreKeyRepository
import com.wire.android.shared.user.QualifiedId

class PreKeyDataSource: PreKeyRepository {

    override suspend fun preKeysOfClientsByUsers(qualifiedIdsMap: Map<QualifiedId, String>): Either<Failure, PreKey> {
        TODO("Not yet implemented")
    }
}
