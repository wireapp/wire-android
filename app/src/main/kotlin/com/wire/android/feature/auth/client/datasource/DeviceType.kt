package com.wire.android.feature.auth.client.datasource

sealed class DeviceType

object Permanent : DeviceType()
object Temporary : DeviceType()
object LegalHold : DeviceType()
