package com.wire.android.feature.auth.registration.datasource.remote

import com.google.gson.annotations.SerializedName

class RegisterPersonalAccountWithEmailRequest(
    @SerializedName("email") val email: String,
    @SerializedName("locale") val locale: String,
    @SerializedName("name") val name: String,
    @SerializedName("password") val password: String,
    @SerializedName("email_code") val emailCode: String,
    @SerializedName("label") val label: String
)
