package com.wire.android.migration

import com.wire.android.config.CoroutineTestExtension
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import org.amshove.kluent.internal.assertEquals
import org.junit.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class MigrationMapperTest {

    private lateinit var migrationMapper: MigrationMapper
    @BeforeEach
    fun setUp() {
        migrationMapper = MigrationMapper()
    }

    @Test
    fun givenEmptyStringDomain_whenMappingIdFromScala_thenUserSelfDomain() {
        val scalaId = "123-321_123"
        val domain = ""
        val selfUser = UserId("self_id", "self_domain")

        val expected = QualifiedID(scalaId, selfUser.domain)
        migrationMapper.toQualifiedId(scalaId, domain, selfUser).also {
            assertEquals(expected, it)
        }
    }

    @Test
    fun givenNonEmptyStringDomain_whenMappingIdFromScala_thenKeepTheDomainFromScala() {
        val scalaId = "123-321_123"
        val domain = "domain.com"
        val selfUser = UserId("self_id", "self_domain")

        val expected = QualifiedID(scalaId, domain)
        migrationMapper.toQualifiedId(scalaId, domain, selfUser).also {
            assertEquals(expected, it)
        }
    }
}
