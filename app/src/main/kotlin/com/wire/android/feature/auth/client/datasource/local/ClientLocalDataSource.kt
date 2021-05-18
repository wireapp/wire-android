package com.wire.android.feature.auth.client.datasource.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.db.DatabaseService

class ClientLocalDataSource(private val clientDao: ClientDao) : DatabaseService {

    suspend fun saveClient(clientEntity: ClientEntity) : Either<Failure, Unit> = request {
        clientDao.insert(clientEntity)
    }
}