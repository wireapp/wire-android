package com.wire.android.feature.auth.activation.datasource

import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.activation.datasource.remote.ActivationRemoteDataSource
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before

import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
class ActivationDataSourceTest : UnitTest() {

    private lateinit var activationDataSource: ActivationDataSource

    @Mock
    private lateinit var remoteDataSource: ActivationRemoteDataSource

    @Before
    fun setUp() {
        activationDataSource = ActivationDataSource(remoteDataSource)
    }

    @Test
    fun `Given sendEmailActivationCode() is called and remote request fails then return failure`() = runBlocking {
        `when`(remoteDataSource.sendEmailActivationCode(TEST_EMAIL)).thenReturn(Either.Left(ServerError))

        val response = activationDataSource.sendEmailActivationCode(TEST_EMAIL)

        verify(remoteDataSource).sendEmailActivationCode(TEST_EMAIL)
        response.assertLeft { assertThat(it).isEqualTo(ServerError) }
    }

    @Test
    fun `Given sendEmailActivationCode() is called and remote request is success, then return success`() = runBlocking {
        `when`(remoteDataSource.sendEmailActivationCode(TEST_EMAIL)).thenReturn(Either.Right(Unit))

        val response = activationDataSource.sendEmailActivationCode(TEST_EMAIL)

        verify(remoteDataSource).sendEmailActivationCode(TEST_EMAIL)
        response.assertRight()
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
    }
}
