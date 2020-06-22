package com.wire.android.feature.auth.activation.datasource

import com.wire.android.UnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before

import org.junit.Test

class ActivationDataSourceTest : UnitTest() {

    private lateinit var activationDataSource: ActivationDataSource

    @Before
    fun setUp() {
        activationDataSource = ActivationDataSource()
    }

    //TODO: add tests!!
    @Test
    fun dummy() {
        assertThat(2 + 2).isEqualTo(4)
    }
}
