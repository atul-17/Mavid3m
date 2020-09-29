package com.mavid.irActivites

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.mavid.R
import com.mavid.libresdk.LibreMavidHelper
import com.mavid.libresdk.TaskManager.Communication.Listeners.CommandStatusListenerWithResponse
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.DeviceInfo
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.MessageInfo
import com.mavid.models.ModelRemoteDetails
import com.mavid.utility.ApiConstants
import com.mavid.utility.OnButtonClickCallback
import com.mavid.utility.RestApiSucessFailureCallbacks
import com.mavid.utility.UIRelatedClass
import kotlinx.android.synthetic.main.activity_layout_ir_restore_selection.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.Charset

class IRRestoreSelectionActivity : AppCompatActivity() {

    var bundle = Bundle()

    var deviceInfo: DeviceInfo? = null

    var applianceInfo: String? = null


    var gson = Gson()

    var modelRemoteDetailsList: MutableList<ModelRemoteDetails> = ArrayList()

    var index: Int = 0

    var selectedAppliance = ""

    lateinit var progressDialog: Dialog

    var uiRelatedClass = UIRelatedClass()

    val TAG = IRRestoreSelectionActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout_ir_restore_selection)

        bundle = intent.extras

        if (bundle != null) {
            deviceInfo = bundle.getSerializable("deviceInfo") as DeviceInfo
            applianceInfo = bundle.getString("applianceInfo")
        }

        var bodyJsonObject = JSONObject(applianceInfo)

        var sub = bodyJsonObject.getString("sub")

        Log.d(TAG, sub)

        if (bodyJsonObject.has("Appliance")) {

            val applianceObject: JSONObject? = bodyJsonObject.optJSONObject("Appliance")

            if (applianceObject != null) {
                //it is a json object
                val modelRemoteDetails: ModelRemoteDetails = parseApplianceJsonObject(applianceObject)!!
                modelRemoteDetailsList.add(modelRemoteDetails)

            } else {
                //it might be an array
                val applianceJsonArray: JSONArray? = bodyJsonObject.optJSONArray("Appliance")
                if (applianceJsonArray != null) {
                    for (i in 0 until applianceJsonArray.length()) {
                        //updating the tv or tvp details
                        val modelRemoteDetails: ModelRemoteDetails = parseApplianceJsonObject(applianceJsonArray[i] as JSONObject)!!
                        modelRemoteDetailsList.add(modelRemoteDetails)

                    }
                    Log.d(TAG, "remoteData".plus(modelRemoteDetailsList.size.toString()))
                }
            }
        }



        buildrogressDialog()


        updateSelectedApplianceTextView()


        btnNo.setOnClickListener {
            if (index < modelRemoteDetailsList.size) {
                showCustomAlertForDeleteConfirmation(modelRemoteDetailsList[index])
            }
        }


        btnYes.setOnClickListener {
            if (index < modelRemoteDetailsList.size) {
                sendRemoteDetailsToMavid3m(deviceInfo!!.ipAddress, modelRemoteDetailsList[index])
            }
        }


        btnProceed.setOnClickListener {
            val intent = Intent(this@IRRestoreSelectionActivity, IRAddRemoteVPActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable("deviceInfo", deviceInfo)
            intent.putExtras(bundle)
            startActivity(intent)
            finish()
        }

    }


    private fun updateSelectedApplianceTextView() {
        Log.d(TAG, "seletedAppliance".plus(index))
        if (modelRemoteDetailsList[index].selectedAppliance == "TV") {

            selectedAppliance = "TV"

        } else if (modelRemoteDetailsList[index].selectedAppliance == "TVP") {

            selectedAppliance = "Setup Box"

        }

        selectedApplianceAndBrand.text = modelRemoteDetailsList[index].selectedBrandName.plus(" $selectedAppliance")
    }

    private fun showCustomAlertForDeleteConfirmation(modelRemoteDetails: ModelRemoteDetails) {
        uiRelatedClass.showCustomAlertDialogForDeleteConfirmation(this@IRRestoreSelectionActivity, object : OnButtonClickCallback {
            override fun onClick(isSucess: Boolean) {
                if (isSucess) {
                    //call the delete api
                    showProgressBar()
                    deleteUserDevice(getSharedPreferences("Mavid", Context.MODE_PRIVATE).getString("sub", ""),
                            deviceInfo!!.usn, modelRemoteDetails, object : RestApiSucessFailureCallbacks {
                        override fun onSucessFailureCallbacks(isSucess: Boolean) {
                            if (isSucess) {
                                //remove the data present in the app
                                removeTvORTvpDetailsInSharedPref(modelRemoteDetailsList[index].selectedAppliance)
                                //inc to appliance present in the list
                                index += 1

                                dismissLoader()
                                //check whether the remote config is done
                                checkWhetherRemoteConfigurationIsDone()

                                updateSelectedApplianceTextView()
                            }
                        }
                    })
                }
            }
        })
    }


    fun updateTVRemoteDetailsInSharedPref(modelRemoteDetails: ModelRemoteDetails) {

        var sharedPreferences = getSharedPreferences("Mavid", Context.MODE_PRIVATE)
        var editor: SharedPreferences.Editor
        editor = sharedPreferences!!.edit()

        val gson = Gson()
        val modelRemoteDetailsString: String = gson.toJson(buidlRemoteDetails(modelRemoteDetails))

        editor.putString("tvRemoteDetails", modelRemoteDetailsString)
        editor.apply()

    }


    fun buidlRemoteDetails(modelRemoteDetails: ModelRemoteDetails): ModelRemoteDetails {

        modelRemoteDetails.selectedAppliance = modelRemoteDetails.selectedAppliance

        if (modelRemoteDetails.selectedAppliance == "1") {
            modelRemoteDetails.customName = "TV"//for now hardcoding the customa name
        } else if (modelRemoteDetails.selectedAppliance == "2") {
            modelRemoteDetails.customName = "My Box"//for now hardcoding the customa name
        }

        modelRemoteDetails.groupId = 1
        modelRemoteDetails.groupdName = "Scene1"

        modelRemoteDetails.remoteId = modelRemoteDetails.remoteId

        modelRemoteDetails.selectedBrandName = modelRemoteDetails.selectedBrandName!!

        modelRemoteDetails.brandId = modelRemoteDetails.remoteId

        return modelRemoteDetails
    }

    fun updateTVPRemoteDetailsInSharedPref(modelRemoteDetails: ModelRemoteDetails) {
        var sharedPreferences = getSharedPreferences("Mavid", Context.MODE_PRIVATE)
        var editor: SharedPreferences.Editor
        editor = sharedPreferences!!.edit()

        val gson = Gson()
        val modelRemoteDetailsString: String = gson.toJson(buidlRemoteDetails(modelRemoteDetails))

        editor.putString("tvpRemoteDetails", modelRemoteDetailsString)
        editor.apply()
    }

    fun checkWhetherRemoteConfigurationIsDone() {
        runOnUiThread {
            if (index < modelRemoteDetailsList.size) {
                //show restore selection
                llRestoreAppliance.visibility = View.VISIBLE
                rlRemoteSuccessful.visibility = View.GONE
            } else {
                rlRemoteSuccessful.visibility = View.VISIBLE
                llRestoreAppliance.visibility = View.GONE
            }
        }
    }

    fun buildJsonForUserManagmentApis(sub: String, macAddress: String,
                                      modelRemoteDetails: ModelRemoteDetails, operation: String): JSONObject {
        var payLoadObject: JSONObject = JSONObject()

        payLoadObject.put("Appliance", modelRemoteDetails.selectedAppliance)
        payLoadObject.put("RemoteID", modelRemoteDetails.remoteId)
        payLoadObject.put("BrandId", modelRemoteDetails.brandId)

        payLoadObject.put("GroupID", modelRemoteDetails.groupId.toString())

        payLoadObject.put("GroupName", modelRemoteDetails.groupdName)

        payLoadObject.put("BrandName", modelRemoteDetails.selectedBrandName)
        payLoadObject.put("CustomName", modelRemoteDetails.customName)

        var bodyObject: JSONObject = JSONObject()



        bodyObject.put("operation", operation)
        bodyObject.put("sub", sub)
        bodyObject.put("Mac", macAddress)
        bodyObject.put("payload", payLoadObject)

        Log.d(TAG, "body: ".plus(bodyObject))

        return bodyObject
    }


    fun deleteUserDevice(sub: String, macAddress: String,
                         modelRemoteDetails: ModelRemoteDetails?, restApiSucessFailureCallbacks: RestApiSucessFailureCallbacks) {

        if (modelRemoteDetails != null) {

            val requestQueue = Volley.newRequestQueue(this@IRRestoreSelectionActivity)

            val url = ApiConstants.BASE_URL_USER_MGT + "Beta/usermangement"

            Log.d(TAG, "deleteBody_IR_RestoreSelectionActivity" + buildJsonForUserManagmentApis(sub, macAddress, modelRemoteDetails!!, "delete").toString())

            var requestBody: String = buildJsonForUserManagmentApis(sub, macAddress, modelRemoteDetails!!, "delete").toString()


            var deleteUserDetailsStringRequest = object : StringRequest(Request.Method.POST, url, Response.Listener { response ->


                val responseObject = JSONObject(response)


                if (responseObject.has("body")) {

                    uiRelatedClass.buildSnackBarWithoutButton(this@IRRestoreSelectionActivity,
                            window?.decorView!!.findViewById(android.R.id.content), responseObject.getString("body"))
                }

                restApiSucessFailureCallbacks.onSucessFailureCallbacks(true)

                ;
                Log.d(TAG, "deleteResponse:".plus(response))


            }, Response.ErrorListener { volleyError ->

                restApiSucessFailureCallbacks.onSucessFailureCallbacks(false)

                Log.d(TAG, "Error: ${volleyError.networkResponse.statusCode}")

                dismissLoader()

                if (volleyError is TimeoutError || volleyError is NoConnectionError) {

                    uiRelatedClass.buildSnackBarWithoutButton(this@IRRestoreSelectionActivity,
                            window?.decorView!!.findViewById(android.R.id.content), "Seems your internet connection is slow, please try in sometime")

                } else if (volleyError is AuthFailureError) {

                    uiRelatedClass.buildSnackBarWithoutButton(this@IRRestoreSelectionActivity,
                            window?.decorView!!.findViewById(android.R.id.content), "AuthFailure error occurred, please try again later")


                } else if (volleyError is ServerError) {
                    if (volleyError.networkResponse.statusCode != 302) {
                        uiRelatedClass.buildSnackBarWithoutButton(this@IRRestoreSelectionActivity,
                                window?.decorView!!.findViewById(android.R.id.content), "Server error occurred, please try again later")
                    }

                } else if (volleyError is NetworkError) {
                    uiRelatedClass.buildSnackBarWithoutButton(this@IRRestoreSelectionActivity,
                            window?.decorView!!.findViewById(android.R.id.content), "Network error occurred, please try again later")

                } else if (volleyError is ParseError) {

                    uiRelatedClass.buildSnackBarWithoutButton(this@IRRestoreSelectionActivity,
                            window?.decorView!!.findViewById(android.R.id.content), "Parser error occurred, please try again later")
                }
            }) {
                override fun getBodyContentType(): String? {
                    return "application/json; charset=utf-8"
                }

                override fun getBody(): ByteArray {
                    return requestBody.toByteArray(Charset.forName("utf-8"));
                }
            }

            deleteUserDetailsStringRequest.retryPolicy = DefaultRetryPolicy(
                    30000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            requestQueue.add(deleteUserDetailsStringRequest)
        } else {
            restApiSucessFailureCallbacks.onSucessFailureCallbacks(true)
        }
    }

    fun buildrogressDialog() {
        progressDialog = Dialog(this@IRRestoreSelectionActivity)
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
            progressDialog.show()
        }
    }

    fun dismissLoader() {
        runOnUiThread {
            progressDialog.dismiss()
        }

    }


    /** value: 1 for LDAPI#1 ie sending the remote api*/
    fun buildRemotePayloadJson(modelRemoteDetails: ModelRemoteDetails): JSONObject {
        val payloadJsonObject: JSONObject = JSONObject()
        payloadJsonObject.put("ID", 1)

        val dataJsonObject = JSONObject()

        /**Now we have only one set of devices,
        tomorrow we might give provision for user
        to add another group, say group#2 with second TV, STB..etc. For now keep it as 1*/
        dataJsonObject.put("group", 1)

        /** <ID>: 1 : TV
        2 : STB
        3 : AC*/

        when (modelRemoteDetails.selectedAppliance) {
            "TV" -> {
                dataJsonObject.put("appliance", 1)
            }
            "TVP" -> {
                dataJsonObject.put("appliance", 2)
            }
            "AC" -> {
                dataJsonObject.put("appliance", 3)
            }
        }

        dataJsonObject.put("bName", modelRemoteDetails.selectedBrandName)
        dataJsonObject.put("bId", modelRemoteDetails.brandId.toInt())
        dataJsonObject.put("rId", modelRemoteDetails.remoteId.toInt())

        payloadJsonObject.put("data", dataJsonObject)

        return payloadJsonObject
    }

    /** LDAPI#1 */
    fun sendRemoteDetailsToMavid3m(ipAddress: String, modelRemoteDetails: ModelRemoteDetails) {
        showProgressBar()
        val payloadString = buildRemotePayloadJson(modelRemoteDetails).toString()
        LibreMavidHelper.sendCustomCommands(ipAddress, LibreMavidHelper.COMMANDS.SEND_IR_REMOTE_DETAILS_AND_RETRIVING_BUTTON_LIST, payloadString,
                object : CommandStatusListenerWithResponse {
                    override fun response(messageInfo: MessageInfo?) {
                        if (messageInfo != null) {

                            val dataJsonObject = JSONObject(messageInfo?.message)

                            val status = dataJsonObject.getInt("Status")

                            //device acknowledged
                            if (status == 2) {

//                           timerTaskToReadDeviceStatus(ipAddress, modelRemoteDetails)

                                Log.d(TAG, "response_from_ldapi1" + messageInfo?.message)

                                //updating the appliance info in the app
                                if (modelRemoteDetailsList[index].selectedAppliance == "1") {
                                    //tv
                                    updateTVRemoteDetailsInSharedPref(modelRemoteDetailsList[index])

                                } else if (modelRemoteDetailsList[index].selectedAppliance == "2") {
                                    //tvp
                                    updateTVPRemoteDetailsInSharedPref(modelRemoteDetailsList[index])
                                }

                                //inc to next appliance from the list
                                index += 1

                                dismissLoader()

                                //check if the config is done
                                checkWhetherRemoteConfigurationIsDone()

                                updateSelectedApplianceTextView()


                            } else if (status == 3) {
                                //error
                                dismissLoader()
                                uiRelatedClass.buidCustomSnackBarWithButton(this@IRRestoreSelectionActivity, "There sees to be an error!!.Please try after some time",
                                        "Go Back", this@IRRestoreSelectionActivity)
                            }
                        } else {
                            dismissLoader()
                            uiRelatedClass.buidCustomSnackBarWithButton(this@IRRestoreSelectionActivity, "There sees to be an error!!.Please try after some time",
                                    "Go Back", this@IRRestoreSelectionActivity)
                        }
                    }


                    override fun failure(e: Exception?) {
                        dismissLoader()
                        Log.d(TAG, "sendingRemoteDetailsException$e")
                    }

                    override fun success() {
                        dismissLoader()
                    }
                })
    }

    private fun removeTvORTvpDetailsInSharedPref(selectedAppliance: String) {
        val editor: SharedPreferences.Editor = getSharedPreferences("Mavid", Context.MODE_PRIVATE).edit()
        when (selectedAppliance) {
            "1" -> {
                //tv
                editor.remove("tvRemoteDetails")
                editor.apply()
            }
            "2" -> {
                //tvp
                editor.remove("tvpRemoteDetails")
                editor.apply()
            }
        }
    }

    private fun parseApplianceJsonObject(applianceObject: JSONObject): ModelRemoteDetails? {
        val modelRemoteDetails = ModelRemoteDetails()
        try {
            modelRemoteDetails.selectedBrandName = applianceObject.getString("BrandName")
            modelRemoteDetails.remoteId = applianceObject.getString("RemoteID")
            modelRemoteDetails.brandId = applianceObject.getString("BrandId")
            modelRemoteDetails.selectedAppliance = applianceObject.getString("Appliance")
            modelRemoteDetails.customName = applianceObject.getString("CustomName")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return modelRemoteDetails
    }

}