package com.wire.android.feature.auth.client.datasource

sealed class ClientType

object Permanent : ClientType()
object Temporary : ClientType()
object LegalHold : ClientType()
