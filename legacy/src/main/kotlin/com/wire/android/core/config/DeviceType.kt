package com.wire.android.core.config

sealed class DeviceType

object Permanent : DeviceType()
object Temporary : DeviceType()
object LegalHold : DeviceType()
object Unknown : DeviceType()
