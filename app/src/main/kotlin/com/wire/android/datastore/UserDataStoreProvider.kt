package com.wire.android.datastore

import android.content.Context
import com.wire.kalium.logic.data.user.UserId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDataStoreProvider @Inject constructor(@ApplicationContext private val context: Context) {

    private val dataStoreMap: ConcurrentMap<UserId, UserDataStore> by lazy { ConcurrentHashMap() }

    fun getOrCreate(userId: UserId): UserDataStore = dataStoreMap.computeIfAbsent(userId) { UserDataStore(context, userId) }
}
