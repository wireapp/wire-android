package com.wire.android.shared.prekey.data.remote

import com.wire.android.shared.prekey.data.ClientPreKeyInfo
import com.wire.android.shared.prekey.data.UserPreKeyInfo
import com.wire.android.shared.user.QualifiedId

class RemotePreKeyListMapper(private val remotePreKeyMapper: RemotePreKeyMapper) {

    fun fromRemotePreKeyInfoMap(preKeyListResponse: PreKeyListResponse): List<UserPreKeyInfo> {
        return preKeyListResponse.entries.flatMap { domainEntry ->
            domainEntry.value.mapKeys { userEntry ->
                QualifiedId(domainEntry.key, userEntry.key)
            }.mapValues { userEntry ->
                userEntry.value.mapValues { clientEntry ->
                    remotePreKeyMapper.fromRemoteResponse(clientEntry.value)
                }
            }.map { entry ->
                val clientsInfo = entry.value.map { clientEntry -> ClientPreKeyInfo(clientEntry.key, clientEntry.value) }
                UserPreKeyInfo(entry.key, clientsInfo)
            }
        }
    }
}
