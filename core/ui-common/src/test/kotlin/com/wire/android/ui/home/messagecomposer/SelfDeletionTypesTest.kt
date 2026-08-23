package com.wire.android.ui.home.messagecomposer

import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class SelfDeletionTypesTest {
    @Test
    fun customValuesPreserveTheDeveloperFeatureFilter() {
        assertEquals(SelfDeletionDuration.values().toList(), SelfDeletionDuration.customValues(true))
        assertFalse(SelfDeletionDuration.OneMinute in SelfDeletionDuration.customValues(false))
        assertEquals(
            SelfDeletionDuration.values().filter { it != SelfDeletionDuration.OneMinute },
            SelfDeletionDuration.customValues(false),
        )
    }

    @Test
    fun durationMapperPreservesEveryEnumValue() {
        SelfDeletionDuration.values().forEach { duration ->
            assertEquals(duration, duration.value.toSelfDeletionDuration())
        }
    }
}
