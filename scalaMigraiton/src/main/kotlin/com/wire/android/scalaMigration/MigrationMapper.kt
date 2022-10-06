package com.wire.android.scalaMigration

import com.wire.android.scalaMigration.globalDatabase.ScalaSsoIdEntity
import com.wire.kalium.logic.data.user.SsoId

internal object MigrationMapper {
    fun fromScalaSsoID(ssoIdEntity: ScalaSsoIdEntity): SsoId = with(ssoIdEntity) {
        SsoId(
            subject = subject,
            tenant = tenant,
            scimExternalId = null
        )
    }
}
