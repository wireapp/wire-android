package com.wire.android.ui.home.conversationslist.model

import androidx.annotation.StringRes
import com.wire.android.R

enum class Membership(@StringRes val stringResourceId: Int) {
    Guest(R.string.label_membership_guest),
    Federated(R.string.label_federated_membership),
    External(R.string.label_membership_external),
    Service(R.string.label_membership_service),
    Owner(-1),
    Admin(-1),
    Internal(-1), //TODO Kubaz rename to member
    None(-1)
}

fun Membership.hasLabel(): Boolean = stringResourceId != -1;
