package com.mavid.irActivites

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.mavid.BaseActivity
import com.mavid.R
import com.mavid.fragments.IRTvRemoteSelectionFragment
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.DeviceInfo
import com.mavid.models.ModelLevelCode
import com.mavid.models.ModelLevelData
import com.mavid.models.ModelSelectRemotePayload
import com.mavid.utility.OnCallingGetApiToGetCustomNames
import com.mavid.utility.OnUserButtonSelection
import com.mavid.utility.UIRelatedClass
import com.mavid.viewmodels.ApiViewModel
import kotlinx.android.synthetic.main.activity_ir_tv_remote_selection.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class IRTvRemoteSelectionActivity : BaseActivity() {


    val uiRelatedClass = UIRelatedClass()

    lateinit var apiViewModel: ApiViewModel

    val TAG = IRTvRemoteSelectionActivity::class.java.simpleName

    var applianceId = 0

    var applianceName = ""

    var LEVEL: Int = 1

    var INDEX: Int = 0

    var ipAddress = ""

    var modelSelectRemotePayload: ModelSelectRemotePayload? = ModelSelectRemotePayload()

    lateinit var progressDialog: Dialog

    var deviceInfo: DeviceInfo? = null

    var selectedApplianceType = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ir_tv_remote_selection)

        if (intent.extras != null) {
            applianceId = intent!!.extras!!.getInt("applianceId")
            applianceName = intent!!.extras!!.getString("applianceBrandName", "")
            ipAddress = intent!!.extras!!.getString("ipAddress", "")

            deviceInfo = intent!!.extras!!.getSerializable("deviceInfo") as DeviceInfo

            selectedApplianceType = intent!!.extras.getString("selectedApplianceType", "1")//tv

        }
        apiViewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application)).get(ApiViewModel::class.java)



        buildrogressDialog()
        showProgressBar()

        getTVOrTVPORACSelectionData(applianceId, selectedApplianceType)

    }

    fun addUserCustomNamesToHashMap(applianceInfoJsonObject: JSONObject?, applianceJSONArray: JSONArray?): HashMap<String, String> {

        var customNamesHashMap: HashMap<String, String> = HashMap()

        if (applianceInfoJsonObject != null) {
            try {
                customNamesHashMap[applianceInfoJsonObject.getString("CustomName")] = "1"
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            if (applianceJSONArray != null) {
                for (i in 0 until applianceJSONArray.length()) {
                    val applianceInfoObject = applianceJSONArray[i] as JSONObject
                    customNamesHashMap[applianceInfoObject.getString("CustomName")] = "1"
                }
            }
        }
        return customNamesHashMap
    }


    fun getAppliancesListFromAllDevicesTheUserHasConfigured(sub: String, onCallingGetApiToGetCustomNames: OnCallingGetApiToGetCustomNames) {

        val requestQueue = Volley.newRequestQueue(this@IRTvRemoteSelectionActivity)

        val baseUrl = "https://op4w1ojeh4.execute-api.us-east-1.amazonaws.com/Beta/usermangement?" + "sub=" + sub + "&Mac=" + deviceInfo!!.usn

        Log.d(TAG, "requestedURl: $baseUrl")

        var userAddedCustomNamesHashMap: HashMap<String, String> = HashMap()

        val stringRequest = StringRequest(Request.Method.GET, baseUrl, Response.Listener { response ->

            Log.d(TAG, "getUserManagementDetailsAllDevices: response: $response")


            val responseObject = JSONObject(response)

            val bodyJsonObject: JSONObject? = responseObject.optJSONObject("body")

            if (bodyJsonObject != null) {
                val applianceJsonObject = bodyJsonObject.optJSONObject("Appliance")
                if (applianceJsonObject != null) {
                    userAddedCustomNamesHashMap = addUserCustomNamesToHashMap(applianceJsonObject, null)
                } else {
                    //might be an array
                    val applianceJsonArray = bodyJsonObject.optJSONArray("Appliance")
                    if (applianceJsonArray != null) {
                        if (applianceJsonArray.length() > 0) {
                            userAddedCustomNamesHashMap = addUserCustomNamesToHashMap(null, applianceJsonArray)
                        }
                    }
                }
            } else {
                //body key value is a json array
                val bodyJsonArray: JSONArray? = responseObject.optJSONArray("body")
                if (bodyJsonArray != null) {
                    if (bodyJsonArray.length() > 0) {
                        //appliance key might be json obejct
                        val applianceJsonObject: JSONObject? = bodyJsonObject?.optJSONObject("Appliance")
                        if (applianceJsonObject != null) {
                            userAddedCustomNamesHashMap = addUserCustomNamesToHashMap(applianceJsonObject, null)
                        } else {
                            //might be an array
                            val applianceJsonArray: JSONArray? = bodyJsonObject?.optJSONArray("Appliance")
                            if (applianceJsonArray != null) {
                                if (applianceJsonArray.length() > 0) {
                                    userAddedCustomNamesHashMap = addUserCustomNamesToHashMap(null, applianceJsonArray)
                                }
                            }
                        }
                    }
                }

            }
            onCallingGetApiToGetCustomNames.onResponse(userAddedCustomNamesHashMap)
        }, Response.ErrorListener { volleyError ->
            if (volleyError is TimeoutError || volleyError is NoConnectionError) {
                uiRelatedClass.buildSnackBarWithoutButton(this@IRTvRemoteSelectionActivity,
                        this@IRTvRemoteSelectionActivity.window.decorView.findViewById(android.R.id.content), "Seems your internet connection is slow, please try in sometime")
            } else if (volleyError is AuthFailureError) {
                uiRelatedClass.buildSnackBarWithoutButton(this@IRTvRemoteSelectionActivity,
                        this@IRTvRemoteSelectionActivity.window.decorView.findViewById(android.R.id.content), "AuthFailure error occurred, please try again later")
            } else if (volleyError is ServerError) {
                if (volleyError.networkResponse.statusCode != 302) {
                    uiRelatedClass.buildSnackBarWithoutButton(this@IRTvRemoteSelectionActivity,
                            this@IRTvRemoteSelectionActivity.window.decorView.findViewById(android.R.id.content), "Server error occurred, please try again later")
                }
            } else if (volleyError is NetworkError) {
                uiRelatedClass.buildSnackBarWithoutButton(this@IRTvRemoteSelectionActivity,
                        this@IRTvRemoteSelectionActivity.window.decorView.findViewById(android.R.id.content), "Network error occurred, please try again later")
            } else if (volleyError is ParseError) {
                uiRelatedClass.buildSnackBarWithoutButton(this@IRTvRemoteSelectionActivity,
                        this@IRTvRemoteSelectionActivity.window.decorView.findViewById(android.R.id.content), "Parser error occurred, please try again later")
            }
        })

        stringRequest.setRetryPolicy(DefaultRetryPolicy(30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT))

        requestQueue.add<String>(stringRequest)
    }

    fun addPreDefinedPopularOptionsCustomName(selectedAppliace: String, brandName: String): HashMap<String, String> {
        var popularOptionsHashMap: HashMap<String, String> = HashMap()
        var applianceType = "TV"
        when (selectedAppliace) {
            "1",
            "TV"
            -> {
                //tv
                popularOptionsHashMap[applianceType] = "1"
                applianceType = "TV"
            }
            "2",
            "TVP"
            -> {
                //tvp
                popularOptionsHashMap["My Box"] = "1"
                applianceType = "Set Top Box"
            }
            "3" -> {
                //ac
            }
        }


        popularOptionsHashMap["Living Room $applianceType"] = "1"

        popularOptionsHashMap["$brandName $applianceType"] = "1"

        popularOptionsHashMap["BED room $applianceType"] = "1"

        popularOptionsHashMap["Office $applianceType"] = "1"

        return popularOptionsHashMap
    }


    fun filterOutDuplicateNamesWhichAreUsedByTheUser(customNamesUserIsUsingList:
                                                     HashMap<String, String>, preDefinedPopularOptionsHashMap: HashMap<String, String>): HashMap<String, String> {

        var mutableIterator = preDefinedPopularOptionsHashMap.iterator()

        for (preDefinedHashMapObject: Map.Entry<String, String> in mutableIterator) {
            if (customNamesUserIsUsingList.containsKey(preDefinedHashMapObject.key)) {
                //ie then the user that name for the appliance ie tv.tvp
                //so remove from the suggested options
                mutableIterator.remove()
            }
        }
        return preDefinedPopularOptionsHashMap
    }


    fun buildrogressDialog() {
        progressDialog = Dialog(this@IRTvRemoteSelectionActivity)
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        progressDialog.setContentView(R.layout.custom_progress_bar)
        progressDialog.setCancelable(false)

        val progress_title: AppCompatTextView = progressDialog.findViewById(R.id.progress_title)
        val progress_bar: ProgressBar = progressDialog.findViewById(R.id.progress_bar)
        val progress_message: AppCompatTextView = progressDialog.findViewById(R.id.progress_message)

        progress_message.visibility = View.GONE
        progress_title.text = "Please Wait..."
    }


    fun creteNewFragment(tag: String, modelLevelData: ModelLevelData) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val bundle = Bundle()
        bundle.putSerializable("modelLevelData", modelLevelData)
        val irTelevisionApplianceFragment = IRTvRemoteSelectionFragment()
        irTelevisionApplianceFragment.arguments = bundle


        fragmentTransaction.add(R.id.tvRemoteSelectionFrameLayout, irTelevisionApplianceFragment, tag).addToBackStack(tag)
        fragmentTransaction.commit()
    }

    override fun onBackPressed() {

        val fm: FragmentManager = supportFragmentManager
        if (fm.backStackEntryCount > 1) {

            fm.popBackStackImmediate()

            val fragment = supportFragmentManager.findFragmentById(R.id.tvRemoteSelectionFrameLayout) as IRTvRemoteSelectionFragment?

            fragment?.irButtonListTimerTask?.cancel()
            fragment?.myHandler?.removeCallbacksAndMessages(null)
            //reinit code level index = 0
            fragment?.codeLevelIndex = 0
            fragment?.initViews()
        } else {
            val fragment = supportFragmentManager.findFragmentById(R.id.tvRemoteSelectionFrameLayout) as IRTvRemoteSelectionFragment?
            fragment?.irButtonListTimerTask?.cancel()
            fragment?.myHandler?.removeCallbacksAndMessages(null)

            finish()
        }
    }


    fun showCustomAlertForRemoteSelection(buttonName: String, onUserButtonSelection: OnUserButtonSelection) {
        runOnUiThread {
            val alert: Dialog = Dialog(this@IRTvRemoteSelectionActivity)
            alert.requestWindowFeature(Window.FEATURE_NO_TITLE)
            alert.setContentView(R.layout.custom_user_button_selection_alert)

            alert.setCancelable(false)

            val tvAlertTitle = alert.findViewById<AppCompatTextView>(R.id.tvAlertTitle)
            val tvAlertMessage = alert.findViewById<AppCompatTextView>(R.id.tvAlertMessage)
            var applianceType = ""
            when (selectedApplianceType) {
                "1",
                "TV" -> {
                    applianceType = "TV"
                }
                "2",
                "TVP" -> {
                    applianceType = "Set Top Box"
                }
                "3",
                "AC" -> {
                    //
                }
            }
            if (buttonName.equals("POWER", true)) {
                tvAlertMessage.text = "Does the $applianceType Power ON/OFF?"
            } else {
                //for non power button
                tvAlertMessage.text = "Does $applianceType Responds correctly to $buttonName Button?"
            }
            val btnNo = alert.findViewById<AppCompatButton>(R.id.btnNo)
            btnNo.setOnClickListener {
                alert.dismiss()
                onUserButtonSelection.didTheCommandWork(false)
            }

            val btnYes = alert.findViewById<AppCompatButton>(R.id.btnYes)
            btnYes.setOnClickListener {
                //button worked
                //inc level
                alert.dismiss()
                incLevel()
                onUserButtonSelection.didTheCommandWork(true)
            }
            alert.show()
        }
    }

    fun parseLevelData(levelJsonArray: JSONArray, modelLevelData: ModelLevelData, level: Int, index: Int): ModelLevelData {

        if (index < levelJsonArray.length()) {

            val levelJsonObject: JSONObject = levelJsonArray[index] as JSONObject

            modelLevelData.index = index

            modelLevelData.level = level

            modelLevelData.key = levelJsonObject.getString("key")

            val codeJsonArray = levelJsonObject.getJSONArray("code")

            modelLevelData.modelLevelCodeList = parseCodeLevelData(codeJsonArray, level)

        } else {

            modelLevelData.key = "No More data"

        }

        return modelLevelData
    }


    fun parseCodeLevelData(codeJsonArray: JSONArray, level: Int): MutableList<ModelLevelCode> {

        var modelLevelCodeList: MutableList<ModelLevelCode> = ArrayList()

        for (j in 0 until codeJsonArray.length()) {

            val codeJsonObject: JSONObject = codeJsonArray[j] as JSONObject

            val modelLevelCode: ModelLevelCode = ModelLevelCode()

            modelLevelCode.codeLevelIndex = j

            val commandJsonArray = codeJsonObject.getJSONArray("command")
            for (k in 0 until commandJsonArray.length()) {
                val value: String = commandJsonArray[k] as String
                modelLevelCode.command = value
            }

            val idJsonArray = codeJsonObject.getJSONArray("id")
            val idList: MutableList<Int> = ArrayList()
            for (m in 0 until idJsonArray.length()) {
                val id: Int = idJsonArray[m] as Int
                idList.add(id)
            }
            modelLevelCode.idList = idList
            val level = level + 1
            if (codeJsonObject.has("level".plus(level))) {
                modelLevelCode.subLevelJsonArray = codeJsonObject.getJSONArray("level".plus(level))
            }

            modelLevelCodeList.add(modelLevelCode)
        }
        return modelLevelCodeList
    }

    fun incLevel(): Int {
        LEVEL + 1
        return LEVEL
    }

    fun showProgressBar() {
        runOnUiThread {
            progressDialog.show()
        }
    }

    fun dismissLoader() {
        runOnUiThread {
            progressDialog.dismiss()
        }

    }

    fun getTVOrTVPORACSelectionData(id: Int, selectedApplianceType: String) {

        apiViewModel.getTvSelectJsonDetails(id, selectedApplianceType)?.observe(this, Observer {
            if (it.modelSelectRemotePayload != null) {
                if (it.modelSelectRemotePayload?.applianceBrandId != 0) {
                    if (it.modelSelectRemotePayload?.levelJsonArray != null) {

                        modelSelectRemotePayload = it.modelSelectRemotePayload!!

                        llNoData.visibility = View.GONE

                        creteNewFragment("fragment".plus(LEVEL), parseLevelData(it.modelSelectRemotePayload?.levelJsonArray!!, ModelLevelData(), LEVEL, INDEX))

                    }
                } else {
                    //no data present
                    llNoData.visibility = View.VISIBLE
                    llGoBack.setOnClickListener {
                        finish()
                    }

                    uiRelatedClass.buildSnackBarWithoutButton(this@IRTvRemoteSelectionActivity,
                            window.decorView.findViewById(android.R.id.content), "No Data present at the moment for the particular appliance")
                }
            } else {
                //error
                val volleyError = it?.volleyError

                if (volleyError is TimeoutError || volleyError is NoConnectionError) {

                    uiRelatedClass.buidCustomSnackBarWithButton(this@IRTvRemoteSelectionActivity, "Seems your internet connection is slow, please try in sometime",
                            "Go Back", this@IRTvRemoteSelectionActivity)

                } else if (volleyError is AuthFailureError) {

                    uiRelatedClass.buidCustomSnackBarWithButton(this@IRTvRemoteSelectionActivity, "AuthFailure error occurred, please try again later",
                            "Go Back", this@IRTvRemoteSelectionActivity)

                } else if (volleyError is ServerError) {
                    if (volleyError.networkResponse.statusCode != 302) {

                        uiRelatedClass.buidCustomSnackBarWithButton(this@IRTvRemoteSelectionActivity, "Server error occurred, please try again later",
                                "Go Back", this@IRTvRemoteSelectionActivity)
                    }

                } else if (volleyError is NetworkError) {

                    uiRelatedClass.buidCustomSnackBarWithButton(this@IRTvRemoteSelectionActivity, "Network error occurred, please try again later",
                            "Go Back", this@IRTvRemoteSelectionActivity)


                } else if (volleyError is ParseError) {

                    uiRelatedClass.buidCustomSnackBarWithButton(this@IRTvRemoteSelectionActivity, "Parser error occurred, please try again later",
                            "Go Back", this@IRTvRemoteSelectionActivity)

                } else {
                    uiRelatedClass.buidCustomSnackBarWithButton(this@IRTvRemoteSelectionActivity, "SomeThing is wrong!!.Please Try after some timer",
                            "Go Back", this@IRTvRemoteSelectionActivity)
                }
            }
        })
        dismissLoader()
    }
}