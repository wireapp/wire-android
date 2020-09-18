package com.wire.android.feature.auth.login.email.datasource.remote

import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import com.wire.android.shared.auth.remote.LabelGenerator

class LoginRemoteDataSource(
    private val loginApi: LoginApi,
    private val labelGenerator: LabelGenerator,
    override val networkHandler: NetworkHandler
) : ApiService() {

    suspend fun loginWithEmail(email: String, password: String) = rawRequest {
        val label = labelGenerator.newLabel()
        loginApi.loginWithEmail(LoginWithEmailRequest(email = email, password = password, label = label))
    }
}
