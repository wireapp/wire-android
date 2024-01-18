/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.authentication.login.email

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.authentication.verificationcode.VerificationCode
import com.wire.android.ui.authentication.verificationcode.VerificationCodeState
import com.wire.android.ui.common.Logo
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.CodeFieldValue
import com.wire.android.ui.common.typography
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.UserId

@Composable
fun LoginEmailVerificationCodeScreen(
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean, clientId: ClientId, userId: UserId?) -> Unit,
    viewModel: LoginEmailViewModel = hiltViewModel()
) = LoginEmailVerificationCodeContent(
    viewModel.secondFactorVerificationCodeState,
    viewModel.loginState.emailLoginLoading,
    { viewModel.onCodeChange(it, onSuccess) },
    viewModel::onCodeResend,
    viewModel::onCodeVerificationBackPress
)

@Composable
private fun LoginEmailVerificationCodeContent(
    verificationCodeState: VerificationCodeState,
    isLoading: Boolean,
    onCodeChange: (CodeFieldValue) -> Unit,
    onCodeResend: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onBackPressed() }
    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensions().spacing32x)
    ) {
        val (main, logo) = createRefs()
        Logo(
            modifier = Modifier
                .height(dimensions().spacing24x)
                .constrainAs(logo) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
        )
        MainContent(
            codeState = verificationCodeState,
            isLoading = isLoading,
            onCodeChange = onCodeChange,
            onResendCode = onCodeResend,
            modifier = Modifier.constrainAs(main) {
                top.linkTo(parent.top)
                bottom.linkTo(logo.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                height = Dimension.fillToConstraints
            }
        )
    }
}

@Composable
private fun MainContent(
    codeState: VerificationCodeState,
    isLoading: Boolean,
    onCodeChange: (CodeFieldValue) -> Unit,
    onResendCode: () -> Unit,
    modifier: Modifier = Modifier
) = Column(
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.Center,
    modifier = modifier
) {
    Text(
        text = UIText.StringResource(R.string.second_factor_authentication_title).asString(),
        style = typography().title01,
        textAlign = TextAlign.Start
    )
    VerticalSpace.x24()
    Text(
        text = UIText.StringResource(
            R.string.second_factor_authentication_instructions_label,
            codeState.emailUsed
        ).asString(),
        style = typography().body01,
        textAlign = TextAlign.Start
    )
    VerticalSpace.x32()
    VerificationCode(
        codeLength = codeState.codeLength,
        currentCode = codeState.codeInput.text,
        isLoading = isLoading,
        isCurrentCodeInvalid = codeState.isCurrentCodeInvalid,
        onCodeChange = onCodeChange,
        onResendCode = onResendCode,
    )
}

@Preview(showBackground = true)
@Composable
internal fun LoginEmailVerificationCodeScreenPreview() = LoginEmailVerificationCodeContent(
    VerificationCodeState(
        codeLength = 6,
        codeInput = CodeFieldValue(TextFieldValue("12"), false),
        isCurrentCodeInvalid = false,
        emailUsed = ""
    ),
    false,
    {},
    {},
    {},
)
