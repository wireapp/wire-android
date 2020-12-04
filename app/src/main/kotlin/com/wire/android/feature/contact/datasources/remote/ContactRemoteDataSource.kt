package com.wire.android.feature.contact.datasources.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler

class ContactRemoteDataSource(
    private val contactsApi: ContactsApi,
    override val networkHandler: NetworkHandler,
    private val contactCountThreshold: Int = CONTACT_COUNT_THRESHOLD
) : ApiService() {

    suspend fun contactsById(ids: Set<String>): Either<Failure, List<ContactResponse>> {
        var failure: Failure? = null
        val contactResponses = mutableListOf<ContactResponse>()

        ids.chunked(contactCountThreshold).map { chunkOfIds ->
            request { contactsApi.contactsById(chunkOfIds.joinToString(separator = ",", truncated = "")) }
                .onSuccess { contactResponses.addAll(it) }
                .onFailure { failure = it }
        }

        return if (contactResponses.isEmpty() && failure != null) Either.Left(failure!!)
        else Either.Right(contactResponses)
    }

    companion object {
        private const val CONTACT_COUNT_THRESHOLD = 64
    }
}
