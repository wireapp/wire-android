package com.wire.android.crypto

import android.content.Context
import com.wire.android.crypto.mapper.PreKeyMapper
import com.wire.android.crypto.model.UserID
import com.wire.android.crypto.storage.CryptoPreferencesStorage
import com.wire.android.crypto.utils.plus

object CryptoBoxClientProvider {
    private const val CRYPTOBOX_PARENT_FOLDER_NAME = "otr"

    fun getClient(context: Context, userID: UserID): CryptoBoxClient {
        val cryptoBoxDirectory = context.filesDir + CRYPTOBOX_PARENT_FOLDER_NAME + userID.toString()
        val preKeyRepository = CryptoPreferencesStorage(context, userID)
        return CryptoBoxClient(cryptoBoxDirectory, preKeyRepository, PreKeyMapper())
    }
}
