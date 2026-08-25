/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.runtime

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import com.wire.android.R
import com.wire.android.navigation.routes.auth.AuthenticationTeamAccountCreationRequest
import com.wire.android.navigation.routes.auth.NewLoginPasswordRoute
import com.wire.android.ui.WireActivity
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.EmailComposer
import com.wire.android.util.SupportPage
import com.wire.android.util.SupportUrlResolver
import com.wire.android.util.getDeviceIdString
import com.wire.android.util.getGitBuildId
import com.wire.android.util.sha256
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavigationCommand

/**
 * Resolved Android effects for semantic Navigation 3 external destinations.
 *
 * URL selection is kept separate from execution so the mapping can be tested without an Activity
 * and no generated legacy route type needs to cross the Navigation 3 boundary.
 */
internal sealed interface WireNavigation3PlatformEffect {
    data class CustomTab(val url: String) : WireNavigation3PlatformEffect
    data object GiveFeedbackEmail : WireNavigation3PlatformEffect
    data object None : WireNavigation3PlatformEffect
}

internal data class WireNavigation3ExternalLinks(
    val support: String,
    val teamManagement: String,
    val teamPlan: String,
    val termsOfUse: String,
    val wireWebsite: String,
    val privacyPolicy: String,
    val reportMisuse: String,
    val welcomeAndroid: String,
    val androidReleaseNotes: String,
)

internal fun resolveWireNavigation3PlatformEffect(
    destination: WireNavigation3ExternalIntent,
    links: WireNavigation3ExternalLinks,
): WireNavigation3PlatformEffect = when (destination) {
    WireNavigation3ExternalIntent.SUPPORT ->
        WireNavigation3PlatformEffect.CustomTab(links.support)
    WireNavigation3ExternalIntent.TEAM_MANAGEMENT ->
        links.teamManagement.toCustomTabOrNone()
    WireNavigation3ExternalIntent.TEAM_PLAN ->
        WireNavigation3PlatformEffect.CustomTab(links.teamPlan)
    WireNavigation3ExternalIntent.TERMS_OF_USE ->
        WireNavigation3PlatformEffect.CustomTab(links.termsOfUse)
    WireNavigation3ExternalIntent.WIRE_WEBSITE ->
        WireNavigation3PlatformEffect.CustomTab(links.wireWebsite)
    WireNavigation3ExternalIntent.PRIVACY_POLICY ->
        WireNavigation3PlatformEffect.CustomTab(links.privacyPolicy)
    WireNavigation3ExternalIntent.REPORT_MISUSE ->
        WireNavigation3PlatformEffect.CustomTab(links.reportMisuse)
    WireNavigation3ExternalIntent.GIVE_FEEDBACK ->
        WireNavigation3PlatformEffect.GiveFeedbackEmail
    WireNavigation3ExternalIntent.WELCOME_ANDROID ->
        WireNavigation3PlatformEffect.CustomTab(links.welcomeAndroid)
    WireNavigation3ExternalIntent.ANDROID_RELEASE_NOTES ->
        WireNavigation3PlatformEffect.CustomTab(links.androidReleaseNotes)
}

private fun String.toCustomTabOrNone(): WireNavigation3PlatformEffect =
    takeIf(String::isNotBlank)
        ?.let(WireNavigation3PlatformEffect::CustomTab)
        ?: WireNavigation3PlatformEffect.None

internal fun wireNavigation3ExternalLinks(
    resources: Resources,
    teamManagementUrl: String?,
): WireNavigation3ExternalLinks = WireNavigation3ExternalLinks(
    support = SupportUrlResolver.resolve(resources, SupportPage.SUPPORT),
    teamManagement = teamManagementUrl.orEmpty(),
    teamPlan = resources.getString(R.string.url_wire_plans),
    termsOfUse = resources.getString(R.string.url_terms_of_use_legal),
    wireWebsite = resources.getString(R.string.url_wire_website),
    privacyPolicy = resources.getString(R.string.url_privacy_policy),
    reportMisuse = SupportUrlResolver.resolve(resources, SupportPage.REPORT_MISUSE),
    welcomeAndroid = SupportUrlResolver.resolve(resources, SupportPage.WELCOME_ANDROID),
    androidReleaseNotes = resources.getString(R.string.url_android_release_notes),
)

/**
 * Payload handed to WireActivity's StartActivityForResult launcher.
 *
 * Returning from the custom tab must update the existing login-flow entry rather than push a
 * duplicate. WireActivity remains the owner of the runtime mutation.
 */
internal data class WireNavigation3TeamAccountWebLaunch(
    val intent: Intent,
    val returnRoute: NewLoginPasswordRoute,
) {
    fun returnCommand(): WireNavigationCommand = WireNavigationCommand(
        destination = returnRoute,
        backStackMode = WireBackStackMode.UPDATE_EXISTING,
    )
}

internal fun buildWireNavigation3TeamAccountWebLaunch(
    context: Context,
    request: AuthenticationTeamAccountCreationRequest,
): WireNavigation3TeamAccountWebLaunch {
    val customTabIntent = CustomTabsHelper.buildCustomTabIntent(context).intent.apply {
        data = Uri.parse(request.url)
    }
    return WireNavigation3TeamAccountWebLaunch(
        intent = customTabIntent,
        returnRoute = request.returnRoute,
    )
}

internal fun buildWireNavigation3FeedbackIntent(context: Context): Intent {
    val emailIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(
            Intent.EXTRA_EMAIL,
            arrayOf("wire-newandroid-feedback@wearezeta.zendesk.com")
        )
        putExtra(Intent.EXTRA_SUBJECT, "Feedback - Wire Beta")
        putExtra(
            Intent.EXTRA_TEXT,
            EmailComposer.giveFeedbackEmailTemplate(
                context.getDeviceIdString()?.sha256(),
                context.getGitBuildId(),
            )
        )
        selector = Intent(Intent.ACTION_SENDTO).setData(Uri.parse("mailto:"))
    }
    return Intent.createChooser(
        emailIntent,
        context.getString(R.string.send_feedback_choose_email),
    )
}

internal fun buildWireNavigation3RestartIntent(context: Context): Intent =
    Intent(context, WireActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

/**
 * Android-bound executor whose method references fit the external members of
 * [WireNavigation3ActivityCallbacks].
 */
internal class WireNavigation3ActivityEffects(
    private val activity: Activity,
    private val teamManagementUrl: () -> String? = { null },
) {
    fun openUrl(url: String) {
        CustomTabsHelper.launchUrl(activity, url)
    }

    fun openExternal(destination: WireNavigation3ExternalIntent) {
        when (
            val effect = resolveWireNavigation3PlatformEffect(
                destination = destination,
                links = wireNavigation3ExternalLinks(
                    resources = activity.resources,
                    teamManagementUrl = teamManagementUrl(),
                ),
            )
        ) {
            is WireNavigation3PlatformEffect.CustomTab -> openUrl(effect.url)
            WireNavigation3PlatformEffect.GiveFeedbackEmail ->
                activity.startActivity(buildWireNavigation3FeedbackIntent(activity))
            WireNavigation3PlatformEffect.None -> Unit
        }
    }

    fun teamAccountWebLaunch(
        request: AuthenticationTeamAccountCreationRequest,
    ): WireNavigation3TeamAccountWebLaunch =
        buildWireNavigation3TeamAccountWebLaunch(activity, request)

    fun restartAfterLogout() {
        activity.startActivity(buildWireNavigation3RestartIntent(activity))
        activity.finish()
    }
}
