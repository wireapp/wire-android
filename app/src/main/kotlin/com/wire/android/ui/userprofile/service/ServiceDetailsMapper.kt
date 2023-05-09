package com.wire.android.ui.userprofile.service

import com.wire.kalium.logic.data.id.VALUE_DOMAIN_SEPARATOR
import com.wire.kalium.logic.data.service.ServiceId
import javax.inject.Inject

class ServiceDetailsMapper @Inject constructor() {
    fun fromStringToServiceId(id: String): ServiceId {
        val components = id.split(VALUE_DOMAIN_SEPARATOR).filter { it.isNotBlank() }
        val count = id.count { it == VALUE_DOMAIN_SEPARATOR }

        return when {
            count == 1 && components.size == 2 -> {
                ServiceId(
                    id = components.first(),
                    provider = components.last()
                )
            }
            else -> ServiceId(id = "", provider = "")
        }
    }
}
