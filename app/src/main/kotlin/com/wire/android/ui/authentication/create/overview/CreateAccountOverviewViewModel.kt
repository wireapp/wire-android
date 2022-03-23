package com.wire.android.ui.authentication.create.overview

import com.wire.android.ui.authentication.create.common.CreateAccountFlowType

interface CreateAccountOverviewViewModel {
    fun onOverviewContinue()
    fun goBackToPreviousStep()
    val type: CreateAccountFlowType
}
