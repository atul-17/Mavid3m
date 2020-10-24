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
import com.mavid.utility.OnRemoteKeyPressedInterface
import kotlinx.android.synthetic.main.fragment_ir_ac_appliace_layout.*
import kotlinx.android.synthetic.main.fragment_ir_ac_appliace_layout.llRemoteUi
import kotlinx.android.synthetic.main.fragment_ir_television_appliance.*

class IRAcApplianceFragment : Fragment(), View.OnClickListener {

    val TAG = IRAcApplianceFragment::class.java.simpleName

    private var vibe: Vibrator? = null

    var gson: Gson? = Gson()

    var preDefinedRemoteButtonsHashmap: HashMap<String, String> = HashMap()

    var DISABLE_AC_REMOTE_BUTTONS: Int = 0

    var workingTvpRemoteButtonsHashMap: HashMap<String, String> = HashMap()

    val myHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message?) {
            val what: Int = msg!!.what
            when (what) {
                DISABLE_AC_REMOTE_BUTTONS -> {
                    val workingRemoteButtonsString = getActivityObject()?.getSharedPreferences("Mavid", Context.MODE_PRIVATE)!!.getString("workingACRemoteButtons", "")
                    val type = object : TypeToken<HashMap<String?, String?>?>() {}.type

                    if (workingRemoteButtonsString.isNotEmpty()) {
                        addPredefinedControlsToHashMap()
                        workingTvpRemoteButtonsHashMap = gson?.fromJson(workingRemoteButtonsString, type) as HashMap<String, String>
                        disableNotWorkingButtons()
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

//        myHandler.sendEmptyMessage(DISABLE_AC_REMOTE_BUTTONS)

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


    fun addPredefinedControlsToHashMap() {

        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_POWER_ON_BUTTON] = "1"
        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_POWER_OFF_BUTTON] = "1"

        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_TEMP_DOWN] = "1"
        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_TEMP_UP] = "1"

        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_DIRECTION] = "1"
        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_MODE] = "1"

        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_FAN_SPEED] = "1"
        preDefinedRemoteButtonsHashmap[LibreMavidHelper.AC_REMOTE_CONTROLS.AC_SWING] = "1"

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

    fun disableNotWorkingButtons() {
        Log.d(TAG.toString(), "workingRemoteButtonsAC: ".plus(workingTvpRemoteButtonsHashMap))
        for (preDefinedRemoteButtonObject: Map.Entry<String, String> in preDefinedRemoteButtonsHashmap) {
            if (!workingTvpRemoteButtonsHashMap.containsKey(preDefinedRemoteButtonObject.key)) {
                //if the working remote buttons does not contain the preDefined button
                //then disable that button
                when (preDefinedRemoteButtonObject.key) {
                    LibreMavidHelper.AC_REMOTE_CONTROLS.AC_POWER_ON_BUTTON -> {
                        ivPowerOn.isEnabled = false
                        rlPowerOn.isEnabled = false
                    }

                    LibreMavidHelper.AC_REMOTE_CONTROLS.AC_POWER_OFF_BUTTON -> {
                        ivPowerOff.isEnabled = false
                        rlPowerOff.isEnabled = false
                    }
                    LibreMavidHelper.AC_REMOTE_CONTROLS.AC_TEMP_UP -> {
                        ivTempPlus.isEnabled = false
                    }
                    LibreMavidHelper.AC_REMOTE_CONTROLS.AC_TEMP_DOWN -> {
                        ivTempMinus.isEnabled = false
                    }

                    LibreMavidHelper.AC_REMOTE_CONTROLS.AC_MODE -> {
                        tvModeBtn.isEnabled = false
                    }

                    LibreMavidHelper.AC_REMOTE_CONTROLS.AC_FAN_SPEED -> {
                        tvSpeedBtn.isEnabled = false
                    }
                    LibreMavidHelper.AC_REMOTE_CONTROLS.AC_SWING -> {
                        tvSwing.isEnabled = false
                    }
                    LibreMavidHelper.AC_REMOTE_CONTROLS.AC_DIRECTION -> {
                        tvDirectionBtn.isEnabled = false
                    }
                }
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
    }
}