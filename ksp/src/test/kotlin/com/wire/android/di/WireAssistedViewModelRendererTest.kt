/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.di

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WireAssistedViewModelRendererTest {
    @Test
    fun `derives factory method from implementation class name`() {
        assertEquals("newMeetingViewModel", "NewMeetingViewModelImpl".defaultFactoryMethod())
    }

    @Test
    fun `renders a group interface and Metro binding for every assisted factory`() {
        val bindings = listOf(
            binding(
                method = "newMeetingViewModel",
                factory = "example.NewMeetingViewModelImpl.Factory",
                result = "example.NewMeetingViewModelImpl",
                parameter = "navArgs",
                type = "example.NewMeetingNavArgs",
            ),
            binding(
                method = "meetingListViewModel",
                factory = "example.MeetingListViewModelImpl.Factory",
                result = "example.MeetingListViewModelImpl",
                parameter = "type",
                type = "example.MeetingsTabItem",
            ),
        )

        val result = WireAssistedViewModelRenderer.render("example", "MeetingsManualViewModelFactory", bindings)

        assertEquals(EXPECTED.trimIndent(), result.trim())
    }

    @Test
    fun `keeps an internal factory hidden while exposing its binding container`() {
        val bindings = listOf(
            binding(
                method = "detailsViewModel",
                factory = "example.DetailsViewModel.Factory",
                result = "example.DetailsViewModel",
                parameter = "id",
                type = "kotlin.String",
            ).copy(isInternal = true),
        )

        val result = WireAssistedViewModelRenderer.render("example", "InternalManualViewModelFactory", bindings)

        assertTrue(result.contains("internal interface InternalManualViewModelFactory"))
        assertTrue(result.contains("object InternalManualViewModelFactoryMetroBindings"))
        assertTrue(result.contains("internal fun bind("))
    }

    private fun binding(method: String, factory: String, result: String, parameter: String, type: String) = AssistedBinding(
        groupQualifiedName = "example.MeetingsViewModelFactoryGroup",
        groupPackage = "example",
        factoryName = "MeetingsManualViewModelFactory",
        factoryMethod = method,
        metroFactoryType = factory,
        returnType = result,
        parameters = listOf(AssistedParameter(parameter, type)),
    )

    private companion object {
        const val EXPECTED = """
            package example

            import dev.zacsweers.metro.BindingContainer
            import dev.zacsweers.metro.IntoMap
            import dev.zacsweers.metro.Provides
            import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
            import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey

            interface MeetingsManualViewModelFactory : ManualViewModelAssistedFactory {
                fun meetingListViewModel(type: example.MeetingsTabItem): example.MeetingListViewModelImpl
                fun newMeetingViewModel(navArgs: example.NewMeetingNavArgs): example.NewMeetingViewModelImpl
            }

            @BindingContainer
            object MeetingsManualViewModelFactoryMetroBindings {
                @Provides
                @IntoMap
                @ManualViewModelAssistedFactoryKey(MeetingsManualViewModelFactory::class)
                fun bind(
                    factory0: example.MeetingListViewModelImpl.Factory,
                    factory1: example.NewMeetingViewModelImpl.Factory,
                ): ManualViewModelAssistedFactory =
                    object : MeetingsManualViewModelFactory {
                        override fun meetingListViewModel(type: example.MeetingsTabItem): example.MeetingListViewModelImpl = factory0.create(type)
                        override fun newMeetingViewModel(navArgs: example.NewMeetingNavArgs): example.NewMeetingViewModelImpl = factory1.create(navArgs)
                    }
            }
        """
    }
}
