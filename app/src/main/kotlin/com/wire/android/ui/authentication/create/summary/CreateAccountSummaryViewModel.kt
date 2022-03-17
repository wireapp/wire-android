package com.wire.android.ui.authentication.create.summary

interface CreateAccountSummaryViewModel {
    val summaryState: CreateAccountSummaryViewState
    fun goBackToPreviousStep()
    fun onSummaryContinue()
}
