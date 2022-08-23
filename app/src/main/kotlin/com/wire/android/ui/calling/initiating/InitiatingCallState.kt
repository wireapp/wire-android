package com.wire.android.ui.calling.initiating

data class InitiatingCallState(
    val error: InitiatingCallError = InitiatingCallError.None
) {
    sealed interface InitiatingCallError {
        object None : InitiatingCallError
        object NoConnection : InitiatingCallError
    }
}
