package com.wire.android.migration

import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.migration.feature.MigrateClientsDataUseCase
import com.wire.android.migration.userDatabase.ScalaUserDatabaseProvider
import com.wire.android.migration.util.ScalaCryptoBoxDirectoryProvider
import com.wire.kalium.logic.CoreLogic
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MigrateClientsDataUseCaseTest {

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
            MigrateClientsDataUseCase(coreLogic, scalaCryptoBoxDirectoryProvider, scalaUserDBProvider, userDataStoreProvider)
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun arrange() = this to useCase
    }
}
