package com.wire.android.shared.prekey.data

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.shared.prekey.PreKeyRepository
import com.wire.android.shared.prekey.data.remote.PreKeyRemoteDataSource
import com.wire.android.shared.user.QualifiedId

class PreKeyDataSource(private val remotePreKeyDataSource: PreKeyRemoteDataSource) : PreKeyRepository {

    override suspend fun preKeysOfClientsByUsers(contactIdsMap: Map<String, List<String>>): Either<Failure, List<UserPreKeyInfo>> {
        return remotePreKeyDataSource.preKeysForMultipleUsers(contactIdsMap)
    }

    override suspend fun preKeysOfClientsByQualifiedUsers(qualifiedIdsMap: Map<QualifiedId, List<String>>):
            Either<Failure, List<QualifiedUserPreKeyInfo>> {
        TODO("Not yet implemented - Federation")
    }
}
