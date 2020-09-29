package com.mavid.models

import java.io.Serializable

class ModelRemoteDetails : Serializable {
    var selectedAppliance: String = ""
    var selectedBrandName: String = ""
    var remoteId: String = ""
    var brandId: String = ""
    var customName = ""
    var groupId = 1//by default
    var groupdName = "Scene1"
}
