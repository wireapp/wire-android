package com.wire.android.feature.auth.client.datasource.remote

import com.wire.android.feature.auth.client.datasource.remote.api.QualifiedIdDTO
import com.wire.android.shared.user.QualifiedId

class ClientRemoteMapper {

    fun fromQualifiedIdToQualifiedIdDTO(qualifiedId: QualifiedId): QualifiedIdDTO = qualifiedId.run { QualifiedIdDTO(domain, id) }

    fun fromQualifiedIdDTOToQualifiedId(qualifiedId: QualifiedIdDTO): QualifiedId = qualifiedId.run { QualifiedId(domain, id) }

}
