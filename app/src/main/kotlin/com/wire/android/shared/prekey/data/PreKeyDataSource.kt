package com.wire.android.shared.prekey.data

import com.wire.android.core.crypto.model.PreKey
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.shared.prekey.PreKeyRepository
import com.wire.android.shared.prekey.data.remote.PreKeyRemoteDataSource
import com.wire.android.shared.user.QualifiedId

class PreKeyDataSource(private val remotePreKeyDataSource: PreKeyRemoteDataSource) : PreKeyRepository {

    override suspend fun preKeysOfClientsByUsers(qualifiedIdsMap: Map<QualifiedId, List<String>>): Either<Failure, PreKey> {
        TODO("Get from local PreKey data source first, and fetch the missing ones from remote data source")
        // remotePreKeyDataSource.preKeysForMultipleUsers(qualifiedIdsMap - local ones)
    }
}
