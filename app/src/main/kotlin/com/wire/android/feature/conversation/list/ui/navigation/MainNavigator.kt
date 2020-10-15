package com.wire.android.feature.conversation.list.ui.navigation

import android.content.Context
import com.wire.android.core.extension.clearStack
import com.wire.android.feature.conversation.list.MainActivity

class MainNavigator {

    fun openMainScreen(context: Context) = context.startActivity(MainActivity.newIntent(context).clearStack())
}
