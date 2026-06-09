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
package com.wire.android.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.metro.scopedMetroViewModel
import com.wire.android.ui.analytics.AnalyticsUsageViewModel
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentViewModel
import com.wire.android.ui.e2eiEnrollment.GetE2EICertificateViewModel
import com.wire.android.ui.initialsync.InitialSyncViewModel
import com.wire.android.ui.joinConversation.JoinConversationViaCodeViewModel
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedViewModel
import com.wire.android.ui.settings.devices.e2ei.E2eiCertificateDetailsViewModel
import com.wire.android.ui.sharing.ImportMediaAuthenticatedViewModel

interface MiscViewModelGraph : MetroViewModelGraph {
    val miscViewModelFactory: MiscViewModelFactory
}

@Composable
inline fun <reified VM> miscViewModel(): VM where VM : ViewModel =
    scopedMetroViewModel()

@Composable
fun analyticsUsageViewModel(): AnalyticsUsageViewModel =
    miscViewModel()

@Composable
fun initialSyncViewModel(): InitialSyncViewModel =
    miscViewModel()

@Composable
fun legalHoldRequestedViewModel(): LegalHoldRequestedViewModel =
    miscViewModel()

@Composable
fun e2EIEnrollmentViewModel(): E2EIEnrollmentViewModel =
    miscViewModel()

@Composable
fun getE2EICertificateViewModel(): GetE2EICertificateViewModel =
    miscViewModel()

@Composable
fun e2eiCertificateDetailsViewModel(): E2eiCertificateDetailsViewModel =
    miscViewModel()

@Composable
fun importMediaAuthenticatedViewModel(): ImportMediaAuthenticatedViewModel =
    miscViewModel()

@Composable
fun joinConversationViaCodeViewModel(): JoinConversationViaCodeViewModel =
    miscViewModel()
