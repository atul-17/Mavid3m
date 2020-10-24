package com.mavid.irActivites

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.mavid.R
import com.mavid.adapters.ApplianceFragmentAdapter
import com.mavid.libresdk.LibreMavidHelper
import com.mavid.libresdk.TaskManager.Communication.Listeners.CommandStatusListenerWithResponse
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.DeviceInfo
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.MessageInfo
import com.mavid.models.ModelRemoteDetails
import com.mavid.models.ModelRemoteSubAndMacDetils
import com.mavid.utility.OnRemoteKeyPressedInterface
import com.mavid.utility.UIRelatedClass
import com.mavid.viewmodels.ApiViewModel
import kotlinx.android.synthetic.main.activity_ir_add_remote_vp.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList


class IRAddRemoteVPActivity : AppCompatActivity() {

    var TAG = IRAddRemoteVPActivity::class.java.simpleName

    // tab titles
    private var titles: MutableList<String> = ArrayList()

    lateinit var progressDialog: Dialog

    var bundle = Bundle()

    var deviceInfo: DeviceInfo? = null

    var uIRelatedClass = UIRelatedClass()

    lateinit var frameContent: FrameLayout

    lateinit var sharedPreferences: SharedPreferences

    var selectedAppliance = "2"//by default tvp

    var tvSelectedBrand: String = ""
    var tvRemoteId: String = "0"

    var tvpSelectedBrand = ""
    var tvpRemoteId = ""


    var acSelectedBrand = ""
    var acRemoteId = ""


    var modelTvRemoteDetails = ModelRemoteDetails()

    var modelTvpRemoteDetails = ModelRemoteDetails()

    var modelAcRemoteDetails = ModelRemoteDetails()

    var gson: Gson? = Gson()


    var selectedTabIndex: Int = 0//by default selecting tvp

    lateinit var apiViewModel: ApiViewModel

    companion object {
        var irAddRemoteVPActivity: IRAddRemoteVPActivity? = null
    }


    val LDAPI2_TIMOUT = 1

    var irButtonListTimerTask: Timer? = Timer()

    var modelRemoteSubAndMacDetils = ModelRemoteSubAndMacDetils()

    var workingRemoteButtonsHashMap = HashMap<String, String>()

    val uiRelatedClass = UIRelatedClass()

    var myHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            val what = msg.what
            when (msg.what) {
                LDAPI2_TIMOUT -> runOnUiThread(Runnable {
                    dismissLoader()
                    if (irButtonListTimerTask != null) {
                        irButtonListTimerTask!!.cancel()
                    }
                    Log.d(TAG, "Error")
                    uiRelatedClass.buidCustomSnackBarWithButton(this@IRAddRemoteVPActivity, "There sees to be an error!!.Please try after some time",
                            "Go Back", this@IRAddRemoteVPActivity)
                })
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ir_add_remote_vp)


        apiViewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application)).get(ApiViewModel::class.java)


        irAddRemoteVPActivity = this

        bundle = intent.extras!!

        buildrogressDialog()

        if (bundle != null) {
            deviceInfo = bundle.getSerializable("deviceInfo") as DeviceInfo
            selectedTabIndex = bundle.getInt("selectedTabIndex", 0)
        }

        frameContent = findViewById(R.id.frameContent)


        frameContent.visibility = View.GONE

        sharedPreferences = getSharedPreferences("Mavid", Context.MODE_PRIVATE)


        setApplianceInfoDetails()

        titles.add("Set Top Box")

        titles.add("Television")

        titles.add("Ac")


        appliancesSelectionVp.adapter = ApplianceFragmentAdapter(this@IRAddRemoteVPActivity, titles.toList())

        // attaching tab mediator
        TabLayoutMediator(tabLayout, appliancesSelectionVp) { tab, position ->
            tab.setCustomView(R.layout.custom_tab_view_layout)

            val tvApplianceType = tab.customView?.findViewById<AppCompatTextView>(R.id.tvApplianceType)
            val tvBrandName = tab.customView?.findViewById<AppCompatTextView>(R.id.tvBrandName)

            val ivShowBrands = tab.customView?.findViewById<AppCompatImageView>(R.id.ivShowBrands)

            tvApplianceType?.text = titles[position]

            tvApplianceType?.isSelected = true
            tvBrandName?.isSelected = true

            when (position) {
                0 -> {
                    //tvp
                    tvBrandName?.text = tvpSelectedBrand
                    Log.d(TAG, "tvpBrand ".plus(tvpSelectedBrand))
                    ivShowBrands?.setOnClickListener {
                        val intent = Intent(this@IRAddRemoteVPActivity, IRTvpBrandActivity::class.java)
                        var bundle = Bundle()
                        bundle.putSerializable("deviceInfo", deviceInfo)
                        intent.putExtras(bundle)
                        startActivity(intent)
                    }
                }
                1 -> {
                    //tv
                    tvBrandName?.text = tvSelectedBrand
                    ivShowBrands?.setOnClickListener {
                        val intent = Intent(this@IRAddRemoteVPActivity, IRSelectTvOrTVPOrAcRegionalBrandsActivity::class.java)
                        val bundle = Bundle()
                        bundle.putSerializable("deviceInfo", deviceInfo)
                        bundle.putBoolean("isTv", true)
                        intent.putExtras(bundle)
                        startActivity(intent)
                    }
                }
                2 -> {
                    //ac
                    tvBrandName?.text = acSelectedBrand
                    ivShowBrands?.setOnClickListener {
                        val intent = Intent(this@IRAddRemoteVPActivity, IRSelectTvOrTVPOrAcRegionalBrandsActivity::class.java)
                        val bundle = Bundle()
                        bundle.putSerializable("deviceInfo", deviceInfo);
                        bundle.putBoolean("isAc", true)
                        intent.putExtras(bundle)
                        startActivity(intent)
                    }
                }
            }
        }.attach()


        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        selectedAppliance = "2" //tvp
                    }
                    1 -> {
                        selectedAppliance = "1"//tv
                    }

                    2 -> {
                        selectedAppliance = "3"//ac
                    }
                }
            }
        })

        appliancesSelectionVp.currentItem = selectedTabIndex
    }


    fun hideTabLayout() {
        tabLayout.visibility = View.GONE
    }


    fun setApplianceInfoDetails() {
        //getting from the local storage

        setDefaultApplianceInfo()

        var sharedPreferences = getSharedPreferences("Mavid", Context.MODE_PRIVATE)

        val gson = Gson()

        var modelRemoteDetailsString = sharedPreferences?.getString("applianceInfoList", "")

        if (modelRemoteDetailsString!!.isNotEmpty()) {
            //user has added data //might be tv.tvp.or.ac

            modelRemoteSubAndMacDetils = ModelRemoteSubAndMacDetils()


            modelRemoteSubAndMacDetils = gson?.fromJson<ModelRemoteSubAndMacDetils>(modelRemoteDetailsString,
                    ModelRemoteSubAndMacDetils::class.java) as ModelRemoteSubAndMacDetils

            if (modelRemoteSubAndMacDetils.mac == deviceInfo?.usn) {
                //data is present for the mavid device user has selected
                for (modelRemoteDetails: ModelRemoteDetails in modelRemoteSubAndMacDetils.modelRemoteDetailsList) {

                    if (modelRemoteDetails.selectedAppliance == "1" || modelRemoteDetails.selectedAppliance == "TV") {

                        modelTvRemoteDetails = modelRemoteDetails

                        tvRemoteId = modelTvRemoteDetails.remoteId

                        tvSelectedBrand = modelTvRemoteDetails.selectedBrandName
                        //tv data is there check if we have remoteButtons in internal storage
                        if (!checkIfWeHaveWorkingTVRemoteButtonsIntheApp()) {
                            showProgressBar()
                            timerTaskToReadDeviceStatus(deviceInfo?.ipAddress, tvRemoteId.toInt(), modelRemoteDetails.selectedAppliance)
                        }

                    } else if (modelRemoteDetails.selectedAppliance == "2" || modelRemoteDetails.selectedAppliance == "TVP") {

                        modelTvpRemoteDetails = modelRemoteDetails

                        tvpRemoteId = modelTvpRemoteDetails.remoteId

                        tvpSelectedBrand = modelTvpRemoteDetails.selectedBrandName

                        //tvp data is there check if we have remoteButtons in internal storage
                        if (!checkIfWeHaveWorkingTVPRemoteButtonsIntheApp()) {
                            showProgressBar()
                            timerTaskToReadDeviceStatus(deviceInfo?.ipAddress, tvpRemoteId.toInt(), modelRemoteDetails.selectedAppliance)
                        }

                    } else if (modelRemoteDetails.selectedAppliance == "3" || modelRemoteDetails.selectedAppliance == "AC") {
                        //ac
                        modelAcRemoteDetails = modelRemoteDetails
                        acRemoteId = modelRemoteDetails.remoteId
                        acSelectedBrand = modelAcRemoteDetails.selectedBrandName

                        //TODO:Ldapi2 is no yet implemented in device side as of now update once done
                    }
                }

            } else {
                //diff mavid device ..so data might not be present
                setDefaultApplianceInfo()
            }
        } else {
            //user hasnt added any device
            setDefaultApplianceInfo()
        }

    }


    fun checkIfWeHaveWorkingTVRemoteButtonsIntheApp(): Boolean {
        if (getSharedPreferences("Mavid", Context.MODE_PRIVATE) != null) {
            if (getSharedPreferences("Mavid", Context.MODE_PRIVATE).getString("workingTVRemoteButtons", "").isNotEmpty()) {
                return true
            }
        }
        return false
    }


    fun checkIfWeHaveWorkingTVPRemoteButtonsIntheApp(): Boolean {
        if (getSharedPreferences("Mavid", Context.MODE_PRIVATE) != null) {
            if (getSharedPreferences("Mavid", Context.MODE_PRIVATE).getString("workingTVPRemoteButtons", "").isNotEmpty()) {
                return true
            }
        }
        return false
    }


    fun setDefaultApplianceInfo() {
        tvpRemoteId = "0"
        tvpSelectedBrand = "Not Set"

        tvRemoteId = "0"
        tvSelectedBrand = "Not Set"

        acRemoteId = "0"
        acSelectedBrand = "Not Set"
    }


    override fun onDestroy() {
        super.onDestroy()
        irAddRemoteVPActivity = null
    }

    fun hideApplianceVp() {
        appliancesSelectionVp.visibility = View.GONE
    }

    fun buildrogressDialog() {
        progressDialog = Dialog(this@IRAddRemoteVPActivity)
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        progressDialog.setContentView(R.layout.custom_progress_bar)
        progressDialog.setCancelable(false)

        val progress_title: AppCompatTextView = progressDialog.findViewById(R.id.progress_title)
        val progress_bar: ProgressBar = progressDialog.findViewById(R.id.progress_bar)
        val progress_message: AppCompatTextView = progressDialog.findViewById(R.id.progress_message)

        progress_message.visibility = View.GONE
        progress_title.text = "Please Wait..."
    }

    fun showProgressBar() {
        runOnUiThread {
            if (!progressDialog.isShowing) {
                progressDialog.show()
            }
        }
    }

    fun dismissLoader() {
        runOnUiThread {
            progressDialog.dismiss()
        }
    }

    /**
     * Call the LDAPI#2 every 3 seconds and check the status
     */
    fun timerTaskToReadDeviceStatus(ipdAddress: String?, remoteId: Int, selectedApplianceType: String) {

        myHandler.sendEmptyMessageDelayed(LDAPI2_TIMOUT, 25000)
        irButtonListTimerTask!!.schedule(object : TimerTask() {
            override fun run() {
                Log.d(TAG, "calling_Ldapi#2_every_5_secs")
                getButtonPayload(ipdAddress, remoteId, selectedApplianceType)
            }
        }, 0, 5000)
    }

    fun getButtonPayload(ipdAddress: String?, remoteId: Int, selectedApplianceType: String) {
        LibreMavidHelper.sendCustomCommands(ipdAddress, LibreMavidHelper.COMMANDS.SEND_IR_REMOTE_DETAILS_AND_RETRIVING_BUTTON_LIST,
                buildPayloadForRemoteJsonListForLdapi2(remoteId, selectedApplianceType).toString(), object : CommandStatusListenerWithResponse {
            override fun response(messageInfo: MessageInfo) {
                Log.d(TAG, "ldapi#2_" + messageInfo.message)
                try {
                    val responseJSONObject = JSONObject(messageInfo.message)
                    val statusCode = responseJSONObject.getInt("Status")
                    when (statusCode) {
                        3 -> {
                            myHandler.removeCallbacksAndMessages(LDAPI2_TIMOUT)
                            myHandler.removeCallbacksAndMessages(null)
                            irButtonListTimerTask!!.cancel()

                            /** get the button list from the data json object  */
                            val payloadJsonObject = responseJSONObject.getJSONObject("payload")
                            val buttonJsonArray = payloadJsonObject.getJSONArray("keys")
                            Log.d(TAG, "buttonList: $buttonJsonArray")
                            var i = 0
                            while (i < buttonJsonArray.length()) {
                                val buttonNameString = buttonJsonArray.getString(i)
                                workingRemoteButtonsHashMap[buttonNameString] = "1"
                                i++
                            }

                            //updating the appliance info in the app
                            updateWorkingRemoteButtons(selectedApplianceType)
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    irButtonListTimerTask!!.cancel()
                    myHandler.removeCallbacksAndMessages(LDAPI2_TIMOUT)
                    dismissLoader()
                    Log.d(TAG, "exeception:$e")
                    uiRelatedClass.buidCustomSnackBarWithButton(this@IRAddRemoteVPActivity, "There sees to be an error!!.Please try after some time",
                            "Go Back", (this@IRAddRemoteVPActivity))
                }
            }

            override fun failure(e: java.lang.Exception) {
                irButtonListTimerTask!!.cancel()
                myHandler.removeCallbacksAndMessages(LDAPI2_TIMOUT)
                dismissLoader()
                Log.d(TAG, "exeception:$e")
                uiRelatedClass.buidCustomSnackBarWithButton(this@IRAddRemoteVPActivity, "There sees to be an error!!.Please try after some time",
                        "Go Back", (this@IRAddRemoteVPActivity))
            }

            override fun success() {}
        })
    }


    fun updateWorkingRemoteButtons(applianceType: String) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("Mavid", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val workingRemoteButtonsString = gson!!.toJson(workingRemoteButtonsHashMap)
        if (applianceType == "1" || applianceType == "TV") {
            editor.putString("workingTVRemoteButtons", workingRemoteButtonsString)
        } else if (applianceType == "2" || applianceType == "TVP") {
            editor.putString("workingTVPRemoteButtons", workingRemoteButtonsString)
        } else {
            //Todo Ac needs to be implemented
        }
        dismissLoader()

        editor.apply()
    }

    /**
     * value: 2 for LDAPI#2 ie sending the remote api
     */
    fun buildPayloadForRemoteJsonListForLdapi2(remoteId: Int, selectedApplianceType: String): JSONObject {
        val payloadJsonObject = JSONObject()
        val dataJsonObject = JSONObject()
        /** <ID>: 1 : TV
         * 2 : STB
         * 3 : AC</ID> */
        try {
            payloadJsonObject.put("ID", 2)
            if (selectedApplianceType == "1" || selectedApplianceType == "TV") {
                dataJsonObject.put("appliance", 1) //tv//tvp//ac
            } else if (selectedApplianceType == "2" || selectedApplianceType == "TVP") {
                dataJsonObject.put("appliance", 2) //tv//tvp//ac
            } else if (selectedApplianceType == "3" || selectedApplianceType == "AC") {
                dataJsonObject.put("appliance", 3) //tv//tvp//ac
            }
            dataJsonObject.put("rId", remoteId)
            payloadJsonObject.put("data", dataJsonObject)
            Log.d(TAG, "ldapi#2_payload$payloadJsonObject")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return payloadJsonObject
    }

    fun showSucessfullMessage() {
        uIRelatedClass?.buildSnackBarWithoutButton(this@IRAddRemoteVPActivity,
                window?.decorView!!.findViewById(android.R.id.content), "Successfully sent data to the device")
    }

    fun showErrorMessage() {
        uIRelatedClass?.buildSnackBarWithoutButton(this@IRAddRemoteVPActivity,
                window?.decorView!!.findViewById(android.R.id.content), "There was an error while sending data to the device")
    }

    /** LDApi#4*/
    private fun buiidJsonForSendingTheKeyPressed(apiId: Int,
                                                 applianceType: String, remoteId: String, keys: String): JSONObject {
        val paylodJsonObject = JSONObject()
        paylodJsonObject.put("ID", apiId)

        val dataJSONObject = JSONObject()
        dataJSONObject.put("appliance", applianceType.toInt())//type of the appliance
        dataJSONObject.put("rId", remoteId.toInt())//remote if the selected user

        var keysJsonArray = JSONArray()
        keysJsonArray.put(keys)

        dataJSONObject.put("keys", keysJsonArray)//button name ie pressed

        paylodJsonObject.put("data", dataJSONObject)

        return paylodJsonObject
    }


    /** LDApi#4*/
    fun sendTheKeysPressedIntoTheMavid3MDevice(keysPressed: String, onRemoteKeyPressedInterface: OnRemoteKeyPressedInterface) {
        showProgressBar()

        var remoteId = "0"
        //device acknowledged or device sucess response
        when (selectedAppliance) {
            //tv
            "1",
            "TV"
            -> {
                remoteId = tvRemoteId
            }
            "2",
            "TVP"
            -> {
                //tvp
                remoteId = tvpRemoteId
            }
            //ac
            "3",
            "AC"
            -> {
                remoteId = acRemoteId
            }
        }

        Log.d(TAG, "sendingRemoteButton: ".plus(buiidJsonForSendingTheKeyPressed(4,
                selectedAppliance, remoteId, keysPressed.plus(",1")).toString()))

        LibreMavidHelper.sendCustomCommands(deviceInfo?.ipAddress,
                LibreMavidHelper.COMMANDS.SEND_IR_REMOTE_DETAILS_AND_RETRIVING_BUTTON_LIST,
                buiidJsonForSendingTheKeyPressed(4, selectedAppliance, remoteId, keysPressed.plus(",1")).toString(),
                object : CommandStatusListenerWithResponse {
                    override fun response(messageInfo: MessageInfo?) {
                        if (messageInfo != null) {
                            val dataJsonObject = JSONObject(messageInfo?.message)
                            Log.d(TAG, "ldapi_#4_Response".plus(dataJsonObject).toString())
                            val status = dataJsonObject.getInt("Status")

                            //device acknowledged or device sucess response
                            if (status == 2 || status == 1) {
                                onRemoteKeyPressedInterface.onKeyPressed(true)
                            } else {
                                onRemoteKeyPressedInterface.onKeyPressed(false)
                            }
                        }
                    }

                    override fun failure(e: Exception?) {
                        dismissLoader()
                        Log.d(TAG, "ExceptionWhileSendingKeyPressed$e")
                        onRemoteKeyPressedInterface.onKeyPressed(false)
                    }

                    override fun success() {

                    }
                })
    }


    override fun onBackPressed() {
        val fm: FragmentManager = supportFragmentManager
        if (fm.backStackEntryCount > 0) {
            appliancesSelectionVp.visibility = View.VISIBLE
            tabLayout.visibility = View.VISIBLE
            fm.popBackStackImmediate()

        } else {
            finish()
        }
    }
}