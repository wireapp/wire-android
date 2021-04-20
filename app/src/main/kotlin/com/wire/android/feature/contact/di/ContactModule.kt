package com.wire.android.feature.contact.di

import com.wire.android.core.network.NetworkClient
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.feature.contact.ContactRepository
import com.wire.android.feature.contact.datasources.ContactDataSource
import com.wire.android.feature.contact.datasources.local.ContactLocalDataSource
import com.wire.android.feature.contact.datasources.mapper.ContactMapper
import com.wire.android.feature.contact.datasources.remote.ContactRemoteDataSource
import com.wire.android.feature.contact.datasources.remote.ContactsApi
import org.koin.dsl.module

val contactModule = module {
    factory<ContactRepository> { ContactDataSource(get(), get(), get(), get()) }
    single { ContactRemoteDataSource(get(), get()) }
    single { get<NetworkClient>().create(ContactsApi::class.java) }
    factory { ContactLocalDataSource(get()) }
    factory { get<UserDatabase>().contactDao() }
    factory { ContactMapper() }
}
