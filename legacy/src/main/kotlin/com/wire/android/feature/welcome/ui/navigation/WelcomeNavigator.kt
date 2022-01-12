package com.wire.android.feature.welcome.ui.navigation

import android.content.Context
import com.wire.android.core.extension.clearStack
import com.wire.android.feature.welcome.ui.WelcomeActivity

class WelcomeNavigator {

    fun openWelcomeScreen(context: Context) = context.startActivity(WelcomeActivity.newIntent(context).clearStack())
}
