package com.wire.android.shared.user.datasources

import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.SQLiteFailure
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.user.User
import com.wire.android.shared.user.datasources.local.UserEntity
import com.wire.android.shared.user.datasources.local.UserLocalDataSource
import com.wire.android.shared.user.datasources.remote.SelfUserResponse
import com.wire.android.shared.user.datasources.remote.UserRemoteDataSource
import com.wire.android.shared.user.mapper.UserMapper
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class UserDataSourceTest : UnitTest() {

    @MockK
    private lateinit var localDataSource: UserLocalDataSource

    @MockK
    private lateinit var remoteDataSource: UserRemoteDataSource

    @MockK
    private lateinit var userMapper: UserMapper

    @MockK
    private lateinit var selfUserResponse: SelfUserResponse

    @MockK
    private lateinit var user: User

    @MockK
    private lateinit var userEntity: UserEntity

    private lateinit var userDataSource: UserDataSource

    @Before
    fun setUp() {
        userDataSource = UserDataSource(localDataSource, remoteDataSource, userMapper)
    }

    @Test
    fun `given selfUser is called, when remote and local data sources return success, then returns user`() {
        coEvery { remoteDataSource.selfUser(TEST_ACCESS_TOKEN, TEST_TOKEN_TYPE) } returns Either.Right(selfUserResponse)
        every { userMapper.fromSelfUserResponse(selfUserResponse) } returns user
        every { userMapper.toUserEntity(user) } returns userEntity
        coEvery { localDataSource.save(userEntity) } returns Either.Right(Unit)

        val result = runBlocking { userDataSource.selfUser(TEST_ACCESS_TOKEN, TEST_TOKEN_TYPE) }

        result shouldSucceed { it shouldBe user }
        coVerify(exactly = 1) { remoteDataSource.selfUser(accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE) }
        verify(exactly = 1) { userMapper.fromSelfUserResponse(selfUserResponse) }
        verify(exactly = 1) { userMapper.toUserEntity(user) }
        coVerify(exactly = 1) { localDataSource.save(userEntity) }
    }

    @Test
    fun `given selfUser is called, when remoteDataSource returns success but localDataSource fails, then returns the failure`() {
        val failure = SQLiteFailure()
        coEvery { remoteDataSource.selfUser(TEST_ACCESS_TOKEN, TEST_TOKEN_TYPE) } returns Either.Right(selfUserResponse)
        every { userMapper.fromSelfUserResponse(selfUserResponse) } returns user
        every { userMapper.toUserEntity(user) } returns userEntity
        coEvery { localDataSource.save(userEntity) } returns Either.Left(failure)

        val result = runBlocking { userDataSource.selfUser(TEST_ACCESS_TOKEN, TEST_TOKEN_TYPE) }

        result shouldFail { it shouldBe failure }

        coVerify(exactly = 1) { remoteDataSource.selfUser(accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE) }
        verify(exactly = 1) { userMapper.fromSelfUserResponse(selfUserResponse) }
        verify(exactly = 1) { userMapper.toUserEntity(user) }
        coVerify(exactly = 1) { localDataSource.save(userEntity) }
    }

    @Test
    fun `given selfUser is called, when remoteDataSource returns failure, then directly returns the failure`() {
        coEvery { remoteDataSource.selfUser(TEST_ACCESS_TOKEN, TEST_TOKEN_TYPE) } returns Either.Left(ServerError)

        val result = runBlocking { userDataSource.selfUser(TEST_ACCESS_TOKEN, TEST_TOKEN_TYPE) }

        result shouldFail { it shouldBe ServerError }
        coVerify(exactly = 1) { remoteDataSource.selfUser(accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE) }
        verify { userMapper wasNot Called }
        verify { localDataSource wasNot Called }
    }

    @Test
    fun `given save is called, when localDataSource returns success, then returns success`() {
        every { userMapper.toUserEntity(user) } returns userEntity
        coEvery { localDataSource.save(userEntity) } returns Either.Right(Unit)

        val result = runBlocking { userDataSource.save(user) }

        result shouldSucceed { it shouldBe Unit }
        coVerify(exactly = 1) { localDataSource.save(userEntity) }
    }

    @Test
    fun `given save is called, when localDataSource returns a failure, then returns that failure`() {
        val failure = DatabaseFailure()
        every { userMapper.toUserEntity(user) } returns userEntity
        coEvery { localDataSource.save(userEntity) } returns Either.Left(failure)

        val result = runBlocking { userDataSource.save(user) }

        result shouldFail { it shouldBe failure }
        coVerify(exactly = 1) { localDataSource.save(userEntity) }
    }

    @Test
    fun `given doesUsernameExist, then request remote data source doesUsernameExist`() = runBlocking {
        userDataSource.doesUsernameExist(TEST_USERNAME)

        coVerify { remoteDataSource.doesUsernameExist(eq(TEST_USERNAME)) }
    }

    companion object {
        private const val TEST_ACCESS_TOKEN = "access-token-567"
        private const val TEST_TOKEN_TYPE = "token-type-bearer"
        private const val TEST_USERNAME = "username"
    }
}
