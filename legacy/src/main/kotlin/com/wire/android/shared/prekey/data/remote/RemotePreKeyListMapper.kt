package com.wire.android.shared.prekey.data.remote

import com.wire.android.shared.prekey.data.ClientPreKeyInfo
import com.wire.android.shared.prekey.data.QualifiedUserPreKeyInfo
import com.wire.android.shared.prekey.data.UserPreKeyInfo
import com.wire.android.shared.user.QualifiedId

class RemotePreKeyListMapper(private val remotePreKeyMapper: RemotePreKeyMapper) {

    fun fromRemoteQualifiedPreKeyInfoMap(qualifiedPreKeyListResponse: QualifiedPreKeyListResponse): List<QualifiedUserPreKeyInfo> =
            qualifiedPreKeyListResponse.entries.flatMap { domainEntry ->
                domainEntry.value.mapKeys { userEntry ->
                    QualifiedId(domainEntry.key, userEntry.key)
                }.mapValues { userEntry ->
                    userEntry.value.mapValues { clientEntry ->
                        remotePreKeyMapper.fromRemoteResponse(clientEntry.value)
                    }
                }.map { entry ->
                    val clientsInfo = entry.value.map { clientEntry -> ClientPreKeyInfo(clientEntry.key, clientEntry.value) }
                    QualifiedUserPreKeyInfo(entry.key, clientsInfo)
                }
            }

    fun fromRemotePreKeyInfoMap(preKeyListResponse: PreKeyListResponse): List<UserPreKeyInfo> =
            preKeyListResponse.entries.map { userEntry ->
                val clientsInfo = userEntry.value.entries.map { clientEntry ->
                    val preKey = remotePreKeyMapper.fromRemoteResponse(clientEntry.value)
                    ClientPreKeyInfo(clientEntry.key, preKey)
                }
                UserPreKeyInfo(userEntry.key, clientsInfo)
            }
}
