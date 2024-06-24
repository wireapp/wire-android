/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wire.android.ui.emoji

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.wire.android.R
import com.google.android.material.R as MaterialR

/**
 * Class translated to Kotlin and modified to support custom a [DraggableByHandleBottomSheetBehavior],
 * instead of the default [BottomSheetBehavior].
 * Modified parts of the code are wrapped with `## Modified ##` and `## END Modified ##` comments.
 * Parts related to edge-to-edge have also been removed, as we don't have it turned on.
 *
 * Base class for [android.app.Dialog]s styled as a bottom sheet.
 *
 * Edge to edge window flags are automatically applied if the [android.R.attr.navigationBarColor] is transparent or translucent
 * and `enableEdgeToEdge` is true. These can be set in the theme that is passed to the constructor, or will be taken from the
 * theme of the context (i.e. your application or activity theme).
 *
 * In edge to edge mode, padding will be added automatically to the top when sliding under the
 * status bar. Padding can be applied automatically to the left, right, or bottom if any of
 * `paddingBottomSystemWindowInsets`, `paddingLeftSystemWindowInsets`, or
 * `paddingRightSystemWindowInsets` are set to true in the style.
 */
@Suppress("DEPRECATION")
class HandleDraggableBottomSheetDialog : AppCompatDialog {
    private var behavior: DraggableByHandleBottomSheetBehavior<FrameLayout>? = null

    private var container: FrameLayout? = null
    private var coordinator: CoordinatorLayout? = null
    private var bottomSheet: FrameLayout? = null

    var dismissWithAnimation: Boolean = false

    var cancelable: Boolean = true
        private set
    private var canceledOnTouchOutside = true
    private var canceledOnTouchOutsideSet = false

    constructor(context: Context) : this(context, 0)

    constructor(context: Context, @StyleRes theme: Int) : super(context, getThemeResId(context, theme)) {
        // We hide the title bar for any style configuration. Otherwise, there will be a gap
        // above the bottom sheet when it is expanded.
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    override fun setContentView(@LayoutRes layoutResID: Int) {
        super.setContentView(wrapInBottomSheet(layoutResID, null, null)!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = window
        if (window != null) {
            if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                // The status bar should always be transparent because of the window animation.
                window.statusBarColor = 0

                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                if (Build.VERSION.SDK_INT < VERSION_CODES.M) {
                    // It can be transparent for API 23 and above because we will handle switching the status
                    // bar icons to light or dark as appropriate. For API 21 and API 22 we just set the
                    // translucent status bar.
                    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                }
            }
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    override fun setContentView(view: View) {
        super.setContentView(wrapInBottomSheet(0, view, null)!!)
    }

    override fun setContentView(view: View, params: ViewGroup.LayoutParams?) {
        super.setContentView(wrapInBottomSheet(0, view, params)!!)
    }

    override fun setCancelable(cancelable: Boolean) {
        super.setCancelable(cancelable)
        if (this.cancelable != cancelable) {
            this.cancelable = cancelable
            if (behavior != null) {
                behavior!!.isHideable = cancelable
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (behavior != null && behavior!!.state == BottomSheetBehavior.STATE_HIDDEN) {
            behavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
        }
    }

    override fun cancel() {
        val behavior = getBehavior()

        if (!dismissWithAnimation || behavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            super.cancel()
        } else {
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN)
        }
    }

    override fun setCanceledOnTouchOutside(cancel: Boolean) {
        super.setCanceledOnTouchOutside(cancel)
        if (cancel && !cancelable) {
            cancelable = true
        }
        canceledOnTouchOutside = cancel
        canceledOnTouchOutsideSet = true
    }

    fun getBehavior(): DraggableByHandleBottomSheetBehavior<FrameLayout> {
        if (behavior == null) {
            // The content hasn't been set, so the behavior doesn't exist yet. Let's create it.
            ensureContainerAndBehavior()
        }
        return behavior!!
    }

    /** Creates the container layout which must exist to find the behavior  */
    private fun ensureContainerAndBehavior(): FrameLayout? {
        if (container == null) {
            // ## Modified ##
            container =
                View.inflate(context, R.layout.dialog_bottom_sheet_custom_behavior, null) as FrameLayout
            // ## END Modified ##

            coordinator = container!!.findViewById<View>(R.id.coordinator) as CoordinatorLayout
            bottomSheet = container!!.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout

            behavior = BottomSheetBehavior.from(bottomSheet!!) as DraggableByHandleBottomSheetBehavior<FrameLayout>

            behavior!!.addBottomSheetCallback(bottomSheetCallback)
            behavior!!.isHideable = cancelable
        }
        return container
    }

    private fun wrapInBottomSheet(
        layoutResId: Int,
        view: View?,
        params: ViewGroup.LayoutParams?
    ): View? {
        var view = view
        ensureContainerAndBehavior()
        val coordinator = container!!.findViewById<View>(R.id.coordinator) as CoordinatorLayout
        if (layoutResId != 0 && view == null) {
            view = layoutInflater.inflate(layoutResId, coordinator, false)
        }

        bottomSheet!!.removeAllViews()
        if (params == null) {
            bottomSheet!!.addView(view)
        } else {
            bottomSheet!!.addView(view, params)
        }
        // We treat the CoordinatorLayout as outside the dialog though it is technically inside
        coordinator
            .findViewById<View>(R.id.touch_outside)
            .setOnClickListener {
                if (cancelable && isShowing && shouldWindowCloseOnTouchOutside()) {
                    cancel()
                }
            }
        // Handle accessibility events
        ViewCompat.setAccessibilityDelegate(
            bottomSheet!!,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    if (cancelable) {
                        info.addAction(AccessibilityNodeInfoCompat.ACTION_DISMISS)
                        info.isDismissable = true
                    } else {
                        info.isDismissable = false
                    }
                }

                override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
                    if (action == AccessibilityNodeInfoCompat.ACTION_DISMISS && cancelable) {
                        cancel()
                        return true
                    }
                    return super.performAccessibilityAction(host, action, args)
                }
            }
        )
        bottomSheet!!.setOnTouchListener { _, _ -> // Consume the event and prevent it from falling through
            true
        }
        return container
    }

    fun shouldWindowCloseOnTouchOutside(): Boolean {
        if (!canceledOnTouchOutsideSet) {
            val a =
                context.obtainStyledAttributes(intArrayOf(android.R.attr.windowCloseOnTouchOutside))
            canceledOnTouchOutside = a.getBoolean(0, true)
            a.recycle()
            canceledOnTouchOutsideSet = true
        }
        return canceledOnTouchOutside
    }

    private val bottomSheetCallback: BottomSheetCallback = object : BottomSheetCallback() {
        override fun onStateChanged(
            bottomSheet: View,
            @BottomSheetBehavior.State newState: Int
        ) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                cancel()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
    }

    companion object {
        private fun getThemeResId(context: Context, themeId: Int): Int {
            var themeId = themeId
            if (themeId == 0) {
                // If the provided theme is 0, then retrieve the dialogTheme from our theme
                val outValue = TypedValue()
                themeId = if (context.theme.resolveAttribute(MaterialR.attr.bottomSheetDialogTheme, outValue, true)) {
                    outValue.resourceId
                } else {
                    // bottomSheetDialogTheme is not provided; we default to our light theme
                    MaterialR.style.Theme_Design_Light_BottomSheetDialog
                }
            }
            return themeId
        }

        @Deprecated("use {@link EdgeToEdgeUtils#setLightStatusBar(Window, boolean)} instead")
        fun setLightStatusBar(view: View, isLight: Boolean) {
            if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                var flags = view.systemUiVisibility
                flags = if (isLight) {
                    flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
                view.systemUiVisibility = flags
            }
        }
    }
}
