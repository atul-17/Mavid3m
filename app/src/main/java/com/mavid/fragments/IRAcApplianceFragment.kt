package com.mavid.fragments

import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mavid.R
import com.mavid.irActivites.IRAddRemoteVPActivity
import com.mavid.irActivites.IRSelectTvOrTVPOrAcRegionalBrandsActivity
import com.mavid.libresdk.LibreMavidHelper
import com.mavid.libresdk.TaskManager.Communication.Listeners.CommandStatusListenerWithResponse
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.MessageInfo
import com.mavid.models.ModelLdapi2AcModes
import com.mavid.models.ModelLdapi6Response
import com.mavid.utility.OnRemoteKeyPressedInterface
import com.mavid.utility.UIRelatedClass
import kotlinx.android.synthetic.main.fragment_ir_ac_appliace_layout.*
import kotlinx.android.synthetic.main.fragment_ir_ac_appliace_layout.llRemoteUi
import kotlinx.android.synthetic.main.fragment_ir_television_appliance.*
import org.json.JSONObject
import java.lang.Exception

class IRAcApplianceFragment : Fragment(), View.OnClickListener {

    val TAG = IRAcApplianceFragment::class.java.simpleName

    private var vibe: Vibrator? = null

    var gson: Gson? = Gson()


    var DISABLE_AC_REMOTE_BUTTONS: Int = 0


    var modelLdapi2AcModesList: MutableList<ModelLdapi2AcModes> = ArrayList()


    var modelLdapi6ResponsModesList: MutableList<ModelLdapi6Response> = ArrayList()

    var uiRelatedClass = UIRelatedClass()

    var currentMode: String = ""

    val myHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message?) {
            val what: Int = msg!!.what
            when (what) {
                DISABLE_AC_REMOTE_BUTTONS -> {
                    val workingRemoteButtonsString = getActivityObject()?.getSharedPreferences("Mavid", Context.MODE_PRIVATE)!!.getString("workingACRemoteButtons", "")
                    val type = object : TypeToken<MutableList<ModelLdapi2AcModes>?>() {}.type

                    if (workingRemoteButtonsString.isNotEmpty()) {
                        modelLdapi2AcModesList = gson?.fromJson(workingRemoteButtonsString, type) as MutableList<ModelLdapi2AcModes>
                        disableNotWorkingButtons(currentMode)
                    }
                }
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_ir_ac_appliace_layout, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAcControlsButtons()

        tvSelectAc.setOnClickListener {
            val intent = Intent(activity, IRSelectTvOrTVPOrAcRegionalBrandsActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable("deviceInfo", getActivityObject()?.deviceInfo)
            bundle.putBoolean("isAc", true)
            intent.putExtras(bundle)
            startActivity(intent)
        }


        if (getActivityObject()?.acSelectedBrand.equals("Not Set")) {
            llSelectAc.visibility = View.VISIBLE
            llRemoteUi.visibility = View.GONE
        } else {
            llSelectAc.visibility = View.GONE
            llRemoteUi.visibility = View.VISIBLE
            getActivityObject()?.showProgressBar()
            getDeviceDetailsLdapi6ToReadAcCurrentConfigs(getActivityObject()!!.deviceInfo!!.ipAddress)
        }


    }

    fun getActivityObject(): IRAddRemoteVPActivity? {
        return activity as IRAddRemoteVPActivity
    }

    private fun vibrateOnButtonClick() {
        if (Build.VERSION.SDK_INT >= 26) {
            vibe = getActivityObject()?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibe?.vibrate(VibrationEffect.createOneShot(150, 10))
        } else {
            vibe = getActivityObject()?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibe?.vibrate(150)
        }
    }


//    fun addPredefinedControlsToHashMap() {
//
//        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_POWER_ON_BUTTON] = "1"
//        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_POWER_OFF_BUTTON] = "1"
//
//        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_TEMP_DOWN] = "1"
//        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_TEMP_UP] = "1"
//
//        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_DIRECTION] = "1"
//        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_MODE] = "1"
//
//        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_FAN_SPEED] = "1"
//        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_SWING] = "1"
//
//    }

    override fun onDestroy() {
        super.onDestroy()
        myHandler?.removeCallbacksAndMessages(null)
    }

    fun getDeviceDetailsLdapi6ToReadAcCurrentConfigs(ip: String) {
        LibreMavidHelper.sendCustomCommands(ip, LibreMavidHelper.COMMANDS.SEND_IR_REMOTE_DETAILS_AND_RETRIVING_BUTTON_LIST,
                buildPayloadForLdapi6().toString(), object : CommandStatusListenerWithResponse {
            override fun response(messageInfo: MessageInfo?) {

                getActivityObject()?.dismissLoader()

                val dataJsonObject = JSONObject(messageInfo?.message)

                Log.d(TAG, "ldapi_#6_Response".plus(dataJsonObject).toString())


                var status = dataJsonObject.getString("Status")

                if (status == "3") {
                    //sucess

                    var payloadJSONObject = dataJsonObject.getJSONObject("payload")

                    var currentMode = payloadJSONObject.getString("current_mode")

                    var modesJsonArray = payloadJSONObject.getJSONArray("modes")

                    modelLdapi6ResponsModesList = ArrayList()

                    for (i in 0 until modesJsonArray.length()) {
                        modelLdapi6ResponsModesList.add(parseTheLdapi6ModeResponse(modesJsonArray[i] as JSONObject))
                    }

                    setTheAcCurrentSettings(currentMode)
                } else {
                    uiRelatedClass.buildSnackBarWithoutButton(getActivityObject()!!,
                            getActivityObject()?.window?.decorView!!.findViewById(android.R.id.content), "There seems to be an error")
                }


            }

            override fun failure(e: Exception?) {
                getActivityObject()?.dismissLoader()
                Log.d(TAG, "ldapi6_exception".plus(e.toString()))
                getActivityObject()?.dismissLoader()
            }

            override fun success() {

            }
        })
    }


    fun parseTheLdapi6ModeResponse(jsonObject: JSONObject): ModelLdapi6Response {
        var modelLdapi6Response = ModelLdapi6Response()

        modelLdapi6Response.mode = jsonObject.getString("mode")
        modelLdapi6Response.temperature = jsonObject.getString("temperature")

        modelLdapi6Response.fanSpeed = jsonObject.getString("fan_speed")
        modelLdapi6Response.swing = jsonObject.getString("swing")

        modelLdapi6Response.direction = jsonObject.getString("direction")

        return modelLdapi6Response
    }

    fun setTheAcCurrentSettings(currentMode: String) {
        for (modelLdapi6Response: ModelLdapi6Response in modelLdapi6ResponsModesList) {
            /** current mode is equal to the one
             * ie current mode = Cooling and
             * modelLdapi6Response.mode is also the same
             * */
            if (modelLdapi6Response.mode == currentMode) {
                getActivityObject()?.runOnUiThread {
                    setTheUIForAc(modelLdapi6Response.temperature,
                            modelLdapi6Response.mode, modelLdapi6Response.fanSpeed, modelLdapi6Response.swing, modelLdapi6Response.direction)

                }
                break
            }
        }
    }


    fun setTheUIForAc(temp: String, acMode: String, acFanSpeed: String, acSwing: String, acDirection: String) {

        if (temp.equals("default", true)) {
            tvTempUnit.visibility = View.GONE
            tvCurrentTemp.text = "NA"
        } else {
            tvTempUnit.visibility = View.VISIBLE
            tvCurrentTemp.text = temp
        }

        if (getActivityObject() != null) {
            when (acMode) {
                "Heat" -> {
                    ivModeIcon.setImageDrawable(getActivityObject()?.resources?.getDrawable(R.drawable.ic_sun_white))
                    frameBg.setBackgroundColor(resources.getColor(R.color.brand_orange))
                }

                "Dehumidify" -> {
                    ivModeIcon.setImageDrawable(getActivityObject()?.resources?.getDrawable(R.drawable.ic_ac_cooling))
                    frameBg.setBackgroundColor(resources.getColor(R.color.alexaBlue))
                }
                "Auto" -> {
                    ivModeIcon.setImageDrawable(getActivityObject()?.resources?.getDrawable(R.drawable.ic_iv_ac_mode_fan))
                    frameBg.setBackgroundColor(resources.getColor(R.color.alexaBlue))
                }
                "Cooling" -> {
                    ivModeIcon.setImageDrawable(getActivityObject()?.resources?.getDrawable(R.drawable.ic_ac_cooling))
                    frameBg.setBackgroundColor(resources.getColor(R.color.alexaBlue))
                }

                //default
                else -> {
                    ivModeIcon.setImageDrawable(getActivityObject()?.resources?.getDrawable(R.drawable.ic_sun_white))
                    frameBg.setBackgroundColor(resources.getColor(R.color.alexaBlue))
                }
            }

            tvAcMode.text = acMode

            if (acFanSpeed.equals("default", true)) {
                tvFanSpeed.text = "NA"
            } else {
                tvFanSpeed.text = acFanSpeed
            }

            if (acDirection.equals("default", true)) {
                tvAcDirection.text = "NA"
            } else {
                tvAcDirection.text = acDirection
            }

            if (acSwing.equals("default", true)) {
                tvAcSwing.text = "NA"
            } else {
                tvAcSwing.text = acSwing
            }

            currentMode = acMode
            myHandler.sendEmptyMessage(DISABLE_AC_REMOTE_BUTTONS)
        }
    }


    fun buildPayloadForLdapi6(): JSONObject {
        val paylodJsonObject = JSONObject()
        paylodJsonObject.put("ID", 6)

        val dataJSONObject = JSONObject()
        dataJSONObject.put("appliance", 3)//type of the appliance
        dataJSONObject.put("rId", getActivityObject()?.acRemoteId?.toInt())//remote if the selected user
        dataJSONObject.put("group", getActivityObject()?.modelAcRemoteDetails?.groupId)

        paylodJsonObject.put("data", dataJSONObject)

        Log.d(TAG, "ldapi6_payload_data".plus(paylodJsonObject.toString()))

        return paylodJsonObject
    }

    fun initAcControlsButtons() {

        rlPowerOn.setOnClickListener(this)
        rlPowerOff.setOnClickListener(this)

        tvModeBtn.setOnClickListener(this)

        tvSpeedBtn.setOnClickListener(this)
        tvDirectionBtn.setOnClickListener(this)
        tvSwing.setOnClickListener(this)

        llTempMinus.setOnClickListener(this)
        llTempPlus.setOnClickListener(this)
    }

    fun setMaxAndMinTemp(minTemp: Int, maxTemp: Int) {

    }

    fun disableNotWorkingButtons(currentMode: String) {
        for (modelLdapi2AcModes: ModelLdapi2AcModes in modelLdapi2AcModesList) {

            Log.d(TAG.toString(), "workingModesOfAc: ".plus(modelLdapi2AcModes.mode))

            if (modelLdapi2AcModes.mode == currentMode) {

                tvDirectionBtn.isEnabled = modelLdapi2AcModes.tempAllowed

                if (modelLdapi2AcModes.tempAllowed) {
                    ivTempMinus.isEnabled = true
                    ivTempPlus.isEnabled = true
                } else {
                    ivTempMinus.isEnabled = false
                    ivTempPlus.isEnabled = false
                }

                tvSpeedBtn.isEnabled = modelLdapi2AcModes.speedAllowed

                tvSwing.isEnabled = modelLdapi2AcModes.swingAllowed

            }
        }
    }

    override fun onClick(p0: View?) {
        vibrateOnButtonClick()
        getActivityObject()?.showProgressBar()
        when (p0?.id) {

            R.id.rlPowerOn -> {
                getActivityObject()
                        ?.sendTheKeysPressedIntoTheMavid3MDevice(LibreMavidHelper.AC_REMOTE_CONTROLS.AC_POWER_ON_BUTTON, object : OnRemoteKeyPressedInterface {
                            override fun onKeyPressed(isSuccess: Boolean) {
                                getActivityObject()?.dismissLoader()
                                if (isSuccess) {
                                    getActivityObject()?.showSucessfullMessage()
                                } else {
                                    getActivityObject()?.showErrorMessage()
                                }
                            }
                        })
            }


            R.id.rlPowerOff -> {
                getActivityObject()
                        ?.sendTheKeysPressedIntoTheMavid3MDevice(LibreMavidHelper.AC_REMOTE_CONTROLS.AC_POWER_OFF_BUTTON, object : OnRemoteKeyPressedInterface {
                            override fun onKeyPressed(isSuccess: Boolean) {
                                getActivityObject()?.dismissLoader()
                                if (isSuccess) {
                                    getActivityObject()?.showSucessfullMessage()
                                } else {
                                    getActivityObject()?.showErrorMessage()
                                }
                            }
                        })
            }

            R.id.tvSwing -> {
                getActivityObject()
                        ?.sendTheKeysPressedIntoTheMavid3MDevice(LibreMavidHelper.AC_REMOTE_CONTROLS.AC_SWING, object : OnRemoteKeyPressedInterface {
                            override fun onKeyPressed(isSuccess: Boolean) {
                                getActivityObject()?.dismissLoader()
                                if (isSuccess) {
                                    getActivityObject()?.showSucessfullMessage()
                                } else {
                                    getActivityObject()?.showErrorMessage()
                                }
                            }
                        })
            }


            R.id.tvDirectionBtn -> {
                getActivityObject()
                        ?.sendTheKeysPressedIntoTheMavid3MDevice(LibreMavidHelper.AC_REMOTE_CONTROLS.AC_DIRECTION, object : OnRemoteKeyPressedInterface {
                            override fun onKeyPressed(isSuccess: Boolean) {
                                getActivityObject()?.dismissLoader()
                                if (isSuccess) {
                                    getActivityObject()?.showSucessfullMessage()
                                } else {
                                    getActivityObject()?.showErrorMessage()
                                }
                            }
                        })
            }


            R.id.tvModeBtn -> {
                getActivityObject()
                        ?.sendTheKeysPressedIntoTheMavid3MDevice(LibreMavidHelper.AC_REMOTE_CONTROLS.AC_MODE, object : OnRemoteKeyPressedInterface {
                            override fun onKeyPressed(isSuccess: Boolean) {
                                getActivityObject()?.dismissLoader()
                                if (isSuccess) {
                                    getActivityObject()?.showSucessfullMessage()
                                } else {
                                    getActivityObject()?.showErrorMessage()
                                }
                            }
                        })
            }

            R.id.tvSpeedBtn -> {
                getActivityObject()
                        ?.sendTheKeysPressedIntoTheMavid3MDevice(LibreMavidHelper.AC_REMOTE_CONTROLS.AC_FAN_SPEED, object : OnRemoteKeyPressedInterface {
                            override fun onKeyPressed(isSuccess: Boolean) {
                                getActivityObject()?.dismissLoader()
                                if (isSuccess) {
                                    getActivityObject()?.showSucessfullMessage()
                                } else {
                                    getActivityObject()?.showErrorMessage()
                                }
                            }
                        })
            }
            R.id.llTempMinus -> {
                getActivityObject()
                        ?.sendTheKeysPressedIntoTheMavid3MDevice(LibreMavidHelper.AC_REMOTE_CONTROLS.AC_TEMP_DOWN, object : OnRemoteKeyPressedInterface {
                            override fun onKeyPressed(isSuccess: Boolean) {
                                getActivityObject()?.dismissLoader()
                                if (isSuccess) {
                                    getActivityObject()?.showSucessfullMessage()
                                } else {
                                    getActivityObject()?.showErrorMessage()
                                }
                            }
                        })
            }

            R.id.llTempPlus -> {
                getActivityObject()
                        ?.sendTheKeysPressedIntoTheMavid3MDevice(LibreMavidHelper.AC_REMOTE_CONTROLS.AC_TEMP_UP, object : OnRemoteKeyPressedInterface {
                            override fun onKeyPressed(isSuccess: Boolean) {
                                getActivityObject()?.dismissLoader()
                                if (isSuccess) {
                                    getActivityObject()?.showSucessfullMessage()
                                } else {
                                    getActivityObject()?.showErrorMessage()
                                }
                            }
                        })
            }

        }

        /**adding a delay of half a second */
        Handler().postDelayed({
            getDeviceDetailsLdapi6ToReadAcCurrentConfigs(getActivityObject()?.deviceInfo!!.ipAddress)
        }, 500)
    }
}