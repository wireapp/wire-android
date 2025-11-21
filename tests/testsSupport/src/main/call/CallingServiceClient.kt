/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
@file:Suppress(
    "TooGenericExceptionCaught",
    "LargeClass",
    "LongParameterList",
    "NestedBlockDepth",
    "MagicNumber",
    "TooManyFunctions",
    "TooGenericExceptionThrown"
)
package call

import android.graphics.Bitmap
import backendUtils.BackendClient
import call.models.BackendDTO
import call.models.Call
import call.models.CallRequest
import call.models.CallFlow
import call.models.Instance
import call.models.InstanceRequest
import call.models.InstanceStatus
import call.models.VersionedInstanceType
import user.utils.ClientUser

class CallingServiceClient {

    companion object {
        const val API_ROOT = "https://qa-callingservice-wire.runs.onstackit.cloud"
    }

    private val callingService = CallingService(API_ROOT)

    fun startInstance(
        user: ClientUser,
        verificationCode: String,
        instanceType: VersionedInstanceType,
        name: String,
        beta: Boolean
    ): Instance {
        val callingServiceEnv = "custom"

        var backendDTO = BackendDTO.fromBackend(BackendClient.loadBackend(user.backendName.orEmpty()))

        val request = InstanceRequest(
            email = user.email.orEmpty(),
            password = user.password.orEmpty(),
            verificationCode = verificationCode,
            backend = callingServiceEnv,
            customBackend = backendDTO,
            instanceType = instanceType,
            name = name,
            beta = beta,
            timeout = 1000L * 60 * 10
        )

        return callingService.createInstance(request)
    }

    fun stopInstance(instance: Instance): Instance =
        callingService.destroyInstance(instance)

    fun getInstanceStatus(instance: Instance): InstanceStatus? =
        callingService.getInstance(instance).instanceStatus

    fun acceptNextIncomingCall(instance: Instance): Call =
        callingService.acceptNext(instance, CallRequest())

    fun acceptNextIncomingVideoCall(instance: Instance): Call =
        callingService.acceptNextVideo(instance, CallRequest())

    fun callToUser(instance: Instance, convId: String): Call =
        callingService.start(instance, CallRequest(convId))

    fun videoCallToUser(instance: Instance, convId: String): Call =
        callingService.startVideo(instance, CallRequest(convId))

    fun getCall(instance: Instance, call: Call): Call =
        callingService.getCall(instance, call)

    fun stopCall(instance: Instance, call: Call): Call =
        callingService.stop(instance, call)

    fun declineCall(instance: Instance, convId: String): Call =
        callingService.decline(instance, CallRequest(convId))

    fun getFlows(instance: Instance): List<CallFlow> =
        callingService.getFlows(instance)

    fun getPackets(instance: Instance): String =
        callingService.getPackets(instance)

    fun getLog(instance: Instance): String =
        callingService.getLog(instance)

    fun switchVideoOn(instance: Instance, call: Call): Call =
        callingService.switchVideoOn(instance, call)

    fun switchVideoOff(instance: Instance, call: Call): Call =
        callingService.switchVideoOff(instance, call)

    fun pauseVideoCall(instance: Instance, call: Call): Call =
        callingService.pauseVideoCall(instance, call)

    fun unpauseVideoCall(instance: Instance, call: Call): Call =
        callingService.unpauseVideoCall(instance, call)

    fun switchScreensharingOn(instance: Instance, call: Call): Call =
        callingService.switchScreensharingOn(instance, call)

    fun switchScreensharingOff(instance: Instance, call: Call): Call =
        callingService.switchScreensharingOff(instance, call)

    fun getCurrentCall(instance: Instance): Call? =
        callingService.getInstance(instance).currentCall

    fun getLivePreview(instance: Instance): String? =
        callingService.getInstance(instance).screenshot

    fun getScreenshot(instance: Instance): Bitmap? =
        callingService.getScreenshot(instance)

    fun muteMicrophone(instance: Instance, call: Call): Call =
        callingService.muteMicrophone(instance, call)

    fun unmuteMicrophone(instance: Instance, call: Call): Call =
        callingService.unmuteMicrophone(instance, call)

    fun maximiseCall(instance: Instance, call: Call): Call =
        callingService.maximiseCall(instance, call)

    fun getAllRunningInstances(): List<Instance> =
        callingService.getAllRunningInstances()

    // region moderation

    fun muteParticipant(instance: Instance, call: Call, name: String): Call =
        callingService.muteParticipant(instance, call, name)

    fun muteAllOthers(instance: Instance, call: Call, name: String): Call =
        callingService.muteAllOthers(instance, call, name)

    // endregion
}
