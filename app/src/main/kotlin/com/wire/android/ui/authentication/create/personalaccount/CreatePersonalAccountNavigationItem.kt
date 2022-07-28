package com.wire.android.ui.authentication.create.personalaccount

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.androidx.AndroidScreen
import cafe.adriel.voyager.core.screen.Screen
import com.wire.android.ui.authentication.create.code.CreateAccountCodeScreen
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsScreen
import com.wire.android.ui.authentication.create.email.CreateAccountEmailScreen
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewScreen

// TODO maybe we can somehow avoid passing ViewModel to each one
sealed class CreatePersonalAccountNavigationItem : AndroidScreen() {

    data class Overview(val viewModel: CreatePersonalAccountViewModel) : CreatePersonalAccountNavigationItem() {

        @Composable
        override fun Content() { CreateAccountOverviewScreen(viewModel) }
    }

    data class Email(val viewModel: CreatePersonalAccountViewModel) : CreatePersonalAccountNavigationItem() {

        @Composable
        override fun Content() { CreateAccountEmailScreen(viewModel) }
    }

    data class Details(val viewModel: CreatePersonalAccountViewModel) : CreatePersonalAccountNavigationItem() {

        @Composable
        override fun Content() { CreateAccountDetailsScreen(viewModel) }
    }

    data class Code(val viewModel: CreatePersonalAccountViewModel) : CreatePersonalAccountNavigationItem() {

        @Composable
        override fun Content() { CreateAccountCodeScreen(viewModel) }
    }
}
