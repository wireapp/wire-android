package com.wire.android.shared.user.datasources

import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.SQLiteFailure
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.shared.user.User
import com.wire.android.shared.user.datasources.local.UserEntity
import com.wire.android.shared.user.datasources.local.UserLocalDataSource
import com.wire.android.shared.user.datasources.remote.SelfUserResponse
import com.wire.android.shared.user.datasources.remote.UserRemoteDataSource
import com.wire.android.shared.user.mapper.UserMapper
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*

class UserDataSourceTest : UnitTest() {

    @Mock
    private lateinit var localDataSource: UserLocalDataSource

    @Mock
    private lateinit var remoteDataSource: UserRemoteDataSource

    @Mock
    private lateinit var userMapper: UserMapper

    @Mock
    private lateinit var selfUserResponse: SelfUserResponse

    @Mock
    private lateinit var user: User

    @Mock
    private lateinit var userEntity: UserEntity

    private lateinit var userDataSource: UserDataSource

    @Before
    fun setUp() {
        userDataSource = UserDataSource(localDataSource, remoteDataSource, userMapper)
    }

    @Test
    fun `given selfUser is called, when remote and local data sources return success, then returns user`() {
        runBlocking {
            `when`(remoteDataSource.selfUser(TEST_ACCESS_TOKEN, TEST_TOKEN_TYPE)).thenReturn(Either.Right(selfUserResponse))
            `when`(userMapper.fromSelfUserResponse(selfUserResponse)).thenReturn(user)
            `when`(userMapper.toUserEntity(user)).thenReturn(userEntity)
            `when`(localDataSource.save(userEntity)).thenReturn(Either.Right(Unit))

            val result = userDataSource.selfUser(TEST_ACCESS_TOKEN, TEST_TOKEN_TYPE)

            result.assertRight {
                assertThat(it).isEqualTo(user)
            }
            verify(remoteDataSource).selfUser(accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE)
            verify(userMapper).fromSelfUserResponse(selfUserResponse)
            verify(userMapper).toUserEntity(user)
            verify(localDataSource).save(userEntity)
        }
    }

    @Test
    fun `given selfUser is called, when remoteDataSource returns success but localDataSource fails, then returns the failure`() {
        runBlocking {
            val failure = SQLiteFailure()
            `when`(remoteDataSource.selfUser(TEST_ACCESS_TOKEN, TEST_TOKEN_TYPE)).thenReturn(Either.Right(selfUserResponse))
            `when`(userMapper.fromSelfUserResponse(selfUserResponse)).thenReturn(user)
            `when`(userMapper.toUserEntity(user)).thenReturn(userEntity)
            `when`(localDataSource.save(userEntity)).thenReturn(Either.Left(failure))

            val result = userDataSource.selfUser(TEST_ACCESS_TOKEN, TEST_TOKEN_TYPE)

            result.assertLeft {
                assertThat(it).isEqualTo(failure)
            }
            verify(remoteDataSource).selfUser(accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE)
            verify(userMapper).fromSelfUserResponse(selfUserResponse)
            verify(userMapper).toUserEntity(user)
            verify(localDataSource).save(userEntity)
        }
    }

    @Test
    fun `given selfUser is called, when remoteDataSource returns failure, then directly returns the failure`() {
        runBlocking {
            `when`(remoteDataSource.selfUser(TEST_ACCESS_TOKEN, TEST_TOKEN_TYPE)).thenReturn(Either.Left(ServerError))

            val result = userDataSource.selfUser(TEST_ACCESS_TOKEN, TEST_TOKEN_TYPE)

            result.assertLeft {
                assertThat(it).isEqualTo(ServerError)
            }
            verify(remoteDataSource).selfUser(accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE)
            verifyNoInteractions(userMapper)
            verifyNoInteractions(localDataSource)
        }
    }

    @Test
    fun `given save is called, when localDataSource returns success, then returns success`() {
        runBlocking {
            `when`(userMapper.toUserEntity(user)).thenReturn(userEntity)
            `when`(localDataSource.save(userEntity)).thenReturn(Either.Right(Unit))

            userDataSource.save(user).assertRight()

            verify(localDataSource).save(userEntity)
        }
    }

    @Test
    fun `given save is called, when localDataSource returns a failure, then returns that failure`() {
        runBlocking {
            val failure = DatabaseFailure()
            `when`(userMapper.toUserEntity(user)).thenReturn(userEntity)
            `when`(localDataSource.save(userEntity)).thenReturn(Either.Left(failure))

            userDataSource.save(user).assertLeft {
                assertThat(it).isEqualTo(failure)
            }
            verify(localDataSource).save(userEntity)
        }
    }

    companion object {
        private const val TEST_ACCESS_TOKEN = "access-token-567"
        private const val TEST_TOKEN_TYPE = "token-type-bearer"
    }
}
