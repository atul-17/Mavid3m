package com.mavid.models

import android.util.Log
import java.io.Serializable

data class ModelRemoteDetails(var selectedAppliance: String = "",
                              var selectedBrandName: String = "",
                              var remoteId: String = "",
                              var brandId: String = "",
                              var customName: String = "",
                              var groupId: Int = 1,//by default
                              var groupdName: String = "Scene1") : Serializable {


}
