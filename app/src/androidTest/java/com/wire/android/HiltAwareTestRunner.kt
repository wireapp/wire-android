package com.wire.android

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class HiltAwareTestRunner : AndroidJUnitRunner() {
    override fun newApplication(classloader: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(classloader, HiltTestApplication::class.java.name, context)
    }
}
