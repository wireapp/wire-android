package com.wire.android.ui.authentication.create.code

import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CreateAccountCodePresentationPolicyTest {
    @Test
    fun `creation restriction distinguishes personal and team flows`() {
        val error = CreateAccountCodeResult.Error.DialogError.CreationRestrictedError

        assertEquals(
            CreateAccountCodeDialogKind.PERSONAL_CREATION_RESTRICTED,
            error.dialogKind(CreateAccountRouteFlowType.PERSONAL),
        )
        assertEquals(
            CreateAccountCodeDialogKind.TEAM_CREATION_RESTRICTED,
            error.dialogKind(CreateAccountRouteFlowType.TEAM),
        )
    }
}
