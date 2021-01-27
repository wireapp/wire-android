package com.wire.android.feature.contact

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface ContactRepository {
    suspend fun fetchContactsById(ids: Set<String>): Either<Failure, Unit>

    suspend fun contactsById(ids: Set<String>): Either<Failure, List<Contact>>
}
