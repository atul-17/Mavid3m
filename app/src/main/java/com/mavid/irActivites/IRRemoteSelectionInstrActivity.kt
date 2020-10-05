package com.mavid.irActivites

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mavid.R
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.DeviceInfo
import kotlinx.android.synthetic.main.ir_selection_instrs_activity.*

class IRRemoteSelectionInstrActivity : AppCompatActivity() {

    var bundle = Bundle()

    var applianceId: Int = 0

    var applianceBrandName: String = ""

    var ipAddress: String = ""

    var deviceInfo: DeviceInfo? = null

    var selectedApplianceType: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ir_selection_instrs_activity)

        bundle = intent.extras

        if (bundle != null) {
            applianceId = bundle.getInt("applianceId", 0)
            applianceBrandName = bundle.getString("applianceBrandName", "")
            deviceInfo = bundle.getSerializable("deviceInfo") as DeviceInfo
            ipAddress = bundle.getString("ipAddress", "")

            selectedApplianceType = bundle.getString("selectedApplianceType", "1")
        }

        when (selectedApplianceType) {
            "1",
            "TV"
            -> {
                tvHeading.text = "TV Remote Selection"
                tvInstructionsMsg.text = applianceBrandName.plus(" TV have several remotes available , please follow the instructions to select the remote that suits your TV")
            }
            "2",
            "TVP"
            -> {
                tvHeading.text = "Setup Box Remote Selection"
                tvInstructionsMsg.text = applianceBrandName.plus(" Setup Box have several remotes available , please follow the instructions to select the remote that suits your Setup Box")
            }
            "3" -> {
                //ac
            }
        }


        btnNext.setOnClickListener {
            val intent = Intent(this@IRRemoteSelectionInstrActivity, IRTvRemoteSelectionActivity::class.java)
            val bundle = Bundle()
            bundle.putInt("applianceId", applianceId)
            bundle.putString("applianceBrandName", applianceBrandName)
            bundle.putString("ipAddress", ipAddress)
            bundle.putSerializable("deviceInfo", deviceInfo)
            bundle.putString("selectedApplianceType", selectedApplianceType)
            intent.putExtras(bundle)
            startActivity(intent)
            finish()
        }
    }
}