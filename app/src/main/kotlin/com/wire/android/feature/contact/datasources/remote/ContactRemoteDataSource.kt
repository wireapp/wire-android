package com.wire.android.feature.contact.datasources.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler

class ContactRemoteDataSource(private val contactsApi: ContactsApi, override val networkHandler: NetworkHandler) : ApiService() {

    suspend fun contactsById(ids: Set<String>): Either<Failure, List<ContactResponse>> = request {
        contactsApi.contactsById(ids)
    }
}
