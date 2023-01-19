package com.wire.android.migration

import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.migration.feature.MigrateClientsDataUseCase
import com.wire.android.migration.userDatabase.ScalaUserData
import com.wire.android.migration.userDatabase.ScalaUserDatabaseProvider
import com.wire.android.migration.util.ScalaCryptoBoxDirectoryProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File


@OptIn(ExperimentalCoroutinesApi::class)
class MigrateClientsDataUseCaseTest {

    @TempDir
    val proteusDir = File("proteus")

    @Test
    fun `given session file name without domain when fixing session file names then return updated session file with domain`() = runTest {
        // given
        val sessionFileNameWithoutDomain = "123-321_123"
        val domain = "domain.com"
        val expected = "123-321@domain.com_123"
        // when
        val (arrangement, useCase) = Arrangement().arrange()
        val result = useCase.fixSessionFileName(sessionFileNameWithoutDomain, domain)
        // then
        assertEquals(result, expected)
    }

    @Test
    fun `given session directory with files when getting session files without domain return proper list`() = runTest {
        // given
        val sessionsDir = File(proteusDir, "sessions").apply { mkdirs() }
        val sessionFileWithDomain = File(sessionsDir, "1@domain.com_123").apply { createNewFile() }
        val sessionFileWithoutDomain = File(sessionsDir, "2_123").apply { createNewFile() }
        // when
        val (arrangement, useCase) = Arrangement().arrange()
        val result = useCase.getSessionFileNamesWithoutDomain(sessionsDir)
        // then
        assertEquals(listOf(sessionFileWithoutDomain), result)
    }

    @Test
    fun `given session file without domain when renaming if needed then add domain to the file name`() = runTest {
        // given
        val sessionsDir = File(proteusDir, "sessions").apply { mkdirs() }
        val sessionFile = File(sessionsDir, "1_123").apply { createNewFile() }
        val domain = "domain.com"
        val expected = "1@domain.com_123"
        // when
        val (arrangement, useCase) = Arrangement().arrange()
        val result = useCase.renameSessionFileIfNeeded(sessionsDir, sessionFile, domain)
        // then
        assertEquals(expected, result.name)
    }

    @Test
    fun `given session file with domain when renaming if needed then do nothing with the file name`() = runTest {
        // given
        val sessionsDir = File(proteusDir, "sessions").apply { mkdirs() }
        val sessionFile = File(sessionsDir, "1@domain.com_123").apply { createNewFile() }
        val domain = "domain.com"
        val expected = "1@domain.com_123"
        // when
        val (arrangement, useCase) = Arrangement().arrange()
        val result = useCase.renameSessionFileIfNeeded(sessionsDir, sessionFile, domain)
        // then
        assertEquals(expected, result.name)
    }

    @Test
    fun `given not federated server when fixing session files then set current user domain`() = runTest {
        // given
        val userId = UserId("id", "domain.com")
        val sessionUserDomain = "otherdomain.com"
        val sessionsDir = File(proteusDir, "sessions").apply { mkdirs() }
        val sessionFile = File(sessionsDir, "1_123").apply { createNewFile() }
        val expected = "1@domain.com_123"
        // when
        val (arrangement, useCase) = Arrangement()
            .withGetUsers { it.map { fakeScalaUserData(it, sessionUserDomain) } }
            .arrange()
        useCase.fixSessionFileNames(userId, proteusDir, false, arrangement.scalaUserDBProvider)
        // then
        sessionsDir.listFiles()?.let {
            assert(it.none { !it.name.contains("@") })
        }
        assertEquals(listOf(expected), sessionsDir.listFiles()?.map { it.name })
        verify(exactly = 0) { arrangement.scalaUserDBProvider.userDAO(any())?.users(any()) }
    }

    @Test
    fun `given federated server when fixing session files then set proper user domain`() = runTest {
        // given
        val userId = UserId("id", "domain.com")
        val sessionUserDomain = "otherdomain.com"
        val sessionsDir = File(proteusDir, "sessions").apply { mkdirs() }
        val sessionFile = File(sessionsDir, "1_123").apply { createNewFile() }
        val expected = "1@otherdomain.com_123"
        // when
        val (arrangement, useCase) = Arrangement()
            .withGetUsers { it.map { fakeScalaUserData(it, sessionUserDomain) } }
            .arrange()
        useCase.fixSessionFileNames(userId, proteusDir, true, arrangement.scalaUserDBProvider)
        // then
        assertEquals(listOf(expected), sessionsDir.listFiles()?.map { it.name })
        verify(atLeast = 1) { arrangement.scalaUserDBProvider.userDAO(any())?.users(any()) }
    }

    private fun fakeScalaUserData(id: String, domain: String) =  ScalaUserData(
        id = id,
        domain = domain,
        teamId = null,
        name = id,
        handle = null,
        email = null,
        phone = null,
        accentId = 0,
        connection = "",
        pictureAssetId = null,
        availability = 0,
        deleted = false,
        serviceProviderId = null,
        serviceIntegrationId = null
    )

    private class Arrangement {

        @MockK
        lateinit var coreLogic: CoreLogic
        @MockK
        lateinit var scalaCryptoBoxDirectoryProvider: ScalaCryptoBoxDirectoryProvider
        @MockK
        lateinit var scalaUserDBProvider: ScalaUserDatabaseProvider
        @MockK
        lateinit var userDataStoreProvider: UserDataStoreProvider

        private val useCase: MigrateClientsDataUseCase by lazy {
            MigrateClientsDataUseCase(
                coreLogic,
                scalaCryptoBoxDirectoryProvider,
                scalaUserDBProvider,
                userDataStoreProvider
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withGetUsers(result: (List<String>) -> List<ScalaUserData>): Arrangement {
            every { scalaUserDBProvider.userDAO(any())?.users(any()) } answers {
                result(firstArg())
            }
            return this
        }

        fun arrange() = this to useCase
    }
}
