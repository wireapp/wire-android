package com.wire.android.scalaMigration.util

import com.wire.kalium.logic.data.user.UserId

object ScalaDBNameProvider {
    fun globalDB() = SCALA_GLOBAL_DATABASE_NAME
    fun userDB(userId: UserId) = userId.value

    private const val SCALA_GLOBAL_DATABASE_NAME = "ZGlobal.db"
}
