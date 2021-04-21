package com.wire.android.crypto.model

import com.wire.android.crypto.utils.Random
import java.util.UUID

interface IDGenerator<A> {

    fun random(): A
    fun decode(value: String): A

}

data class Uid(val value: String) {
    override fun toString() = value

    object Generator : IDGenerator<Uid> {
        override fun random(): Uid {
            return Uid(UUID.randomUUID().toString())
        }

        override fun decode(value: String): Uid {
            return Uid(value)
        }
    }
}

data class UserID(val value: Uid) {
    override fun toString() = value.toString()

    object Generator : IDGenerator<UserID> {
        override fun random(): UserID = UserID(Uid.Generator.random())

        override fun decode(value: String): UserID {
            return UserID(Uid(value))
        }
    }
}


data class ClientID(val value: String) {

    override fun toString() = value

    object Generator : IDGenerator<ClientID> {
        override fun random(): ClientID {
            return ClientID(Random.long().toString(16))
        }

        override fun decode(value: String): ClientID {
            return ClientID(value)
        }
    }
}
