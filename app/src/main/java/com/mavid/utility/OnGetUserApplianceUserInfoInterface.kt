package com.mavid.utility

import com.google.gson.JsonObject
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.DeviceInfo

interface OnGetUserApplianceUserInfoInterface {
    fun onApiResponseCallback(deviceInfo: DeviceInfo,bodyObject: String)
}