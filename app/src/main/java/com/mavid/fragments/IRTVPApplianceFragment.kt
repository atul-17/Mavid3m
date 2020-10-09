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
import com.mavid.irActivites.IRTvpBrandActivity
import com.mavid.libresdk.LibreMavidHelper
import com.mavid.utility.OnRemoteKeyPressedInterface
import kotlinx.android.synthetic.main.fragment_ir_television_appliance.*
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.*
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ibBackButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ibSourceButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.tvExitButtonAlt
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ibFastForwardButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ibInfoButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ibMoreButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ibNextButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ibPauseButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ibPlayButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ibPowerButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ibPrevButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ibRewindButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ibStopButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ibVolumeMuteButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ivDownButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ivLeftButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ivRightButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.ivUpButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.llRemoteUi
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.rlRecButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.tvBlueButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.tvChDownButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.tvChUpButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.tvGreenButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.tvGuideButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.tvHomeButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.tvLangButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.tvMenuButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.tvOkButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.tvOptionButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.tvRedButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.tvVolMinusButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.tvVolPlusButton
import kotlinx.android.synthetic.main.fragment_ir_tvp_layout.tvYellowButton

class IRTVPApplianceFragment : Fragment(), View.OnClickListener {

    private var vibe: Vibrator? = null

    private var TAG = IRTVPApplianceFragment::class.java.simpleName

    var workingTvpRemoteButtonsHashMap: HashMap<String, String> = HashMap()

    var gson: Gson? = Gson()

    var preDefinedRemoteButtonsHashmap: HashMap<String, String> = HashMap()

    var DISABLE_REMOTE_BUTTONS: Int = 0

    val myHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message?) {
            val what: Int = msg!!.what
            when (what) {
                DISABLE_REMOTE_BUTTONS -> {
                    val workingRemoteButtonsString = getActivityObject()?.getSharedPreferences("Mavid", Context.MODE_PRIVATE)!!.getString("workingTVPRemoteButtons", "")
                    val type = object : TypeToken<HashMap<String?, String?>?>() {}.type

                    if (workingRemoteButtonsString.isNotEmpty()) {
                        addPreDefinedButtonsToHashmap()
                        workingTvpRemoteButtonsHashMap = gson?.fromJson(workingRemoteButtonsString, type) as HashMap<String, String>
                        disableNotWorkingButtons()
                    }
                }
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_ir_tvp_layout, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        /** checking whether tvBrand is not empty */
        if (getActivityObject()?.tvpSelectedBrand.equals("Not Set")) {
            llSelectTvp.visibility = View.VISIBLE
            llRemoteUi.visibility = View.GONE
        } else {
            llSelectTvp.visibility = View.GONE
            llRemoteUi.visibility = View.VISIBLE
        }

        myHandler.sendEmptyMessage(DISABLE_REMOTE_BUTTONS)

        tvSelectTVP.setOnClickListener {
            val intent = Intent(getActivityObject(), IRTvpBrandActivity::class.java)
            var bundle = Bundle()
            bundle.putSerializable("deviceInfo", getActivityObject()?.deviceInfo)
            intent.putExtras(bundle)
            startActivity(intent)
        }
        initClickListnersForRemoteButtons()

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


    fun disableNotWorkingButtons() {

        Log.d(TAG, "workingRemoteButtonsTVP: ".plus(workingTvpRemoteButtonsHashMap))

        for (preDefinedRemoteButtonObject: Map.Entry<String, String> in preDefinedRemoteButtonsHashmap) {
            if (!workingTvpRemoteButtonsHashMap.containsKey(preDefinedRemoteButtonObject.key)) {
                //if the working remote buttons does not contain the preDefined button
                //then disable that button
                when (preDefinedRemoteButtonObject.key) {

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.SOURCE_BUTTON -> {
                        ibSourceButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.BACK_BUTTON -> {
                        ibBackButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.CHANNEL_DOWN -> {
                        tvChDownButton.isEnabled = false
                    }


                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.REC_BUTTON -> {
                        rlRecButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.EXIT_BUTTON -> {
                        tvExitButtonAlt.isEnabled = false
                    }
                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.STOP_BUTTON -> {
                        ibStopButton.isEnabled = false
                    }


                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.INFO_BUTTON -> {
                        ibInfoButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.FAST_FORWARD_BUTTON -> {
                        ibFastForwardButton.isEnabled = false
                    }
                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.PAUSE_BUTTON -> {
                        ibPauseButton.isEnabled = false
                    }
                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.PLAY_BUTTON -> {
                        ibPlayButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.REWIND_BUTTON -> {
                        ibRewindButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.RED_BUTTON -> {
                        tvRedButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.YELLOW_BUTTON -> {
                        tvYellowButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.GREEN_BUTTON -> {
                        tvGreenButton.isEnabled = false
                    }
                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.BLUE_BUTTON -> {
                        tvBlueButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.OK_BUTTON
                    -> {
                        if (getActivityObject()?.tvpSelectedBrand == "Tata Sky") {
                            if (!workingTvpRemoteButtonsHashMap.containsKey(LibreMavidHelper.REMOTECONTROLBUTTONNAME.SELECT_BUTTON)) {
                                tvOkButton.isEnabled = false
                                tvOkButton.setTextColor(getActivityObject()!!.resources.getColor(R.color.light_gray))
                            } else {
                                tvOkButton.isEnabled = true
                                tvOkButton.setTextColor(getActivityObject()!!.resources.getColor(R.color.black))
                            }
                        } else {
                            tvOkButton.isEnabled = false
                            tvOkButton.setTextColor(getActivityObject()!!.resources.getColor(R.color.light_gray))
                        }
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.CHANNEL_UP -> {
                        tvChUpButton.isEnabled = false
                    }


                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.RIGHT_BUTTON -> {
                        ivRightButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.LEFT_BUTTON -> {
                        ivLeftButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.DOWN_BUTTON -> {
                        ivDownButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.UP_BUTTON -> {
                        ivUpButton.isEnabled = false
                    }


                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.VOLUME_DOWN -> {
                        tvVolMinusButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.VOLUME_MUTE_BUTTON -> {
                        ibVolumeMuteButton.isEnabled = false
                    }


                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.VOLUME_UP -> {
                        tvVolPlusButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.NEXT_BUTTON -> {
                        ibNextButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.PREV_BUTTON -> {
                        ibPrevButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.LANG_BUTTON -> {
                        tvLangButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.GUIDE_BUTTON -> {
                        tvGuideButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.OPTION_BUTTON -> {
                        tvOptionButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.MENU_BUTTON -> {
                        tvMenuButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.HOME_BUTTON -> {
                        tvHomeButton.isEnabled = false
                    }

                    LibreMavidHelper.REMOTECONTROLBUTTONNAME.POWER_BUTTON -> {
                        ibPowerButton.isEnabled = false
                    }
                }
            }
        }
    }

    fun addPreDefinedButtonsToHashmap() {
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.SOURCE_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.BACK_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.CHANNEL_DOWN, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.REC_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.EXIT_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.STOP_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.INFO_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.FAST_FORWARD_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.PAUSE_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.PLAY_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.REWIND_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.RED_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.YELLOW_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.GREEN_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.BLUE_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.OK_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.SELECT_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.CHANNEL_UP, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.CHANNEL_DOWN, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.RIGHT_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.LEFT_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.DOWN_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.UP_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.VOLUME_DOWN, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.VOLUME_MUTE_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.VOLUME_UP, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.NEXT_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.PREV_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.LANG_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.GUIDE_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.OPTION_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.MENU_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.HOME_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.POWER_BUTTON, "1")


        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.ZERO_NOS_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.ONE_NOS_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.TWO_NOS_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.THREE_NOS_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.FOUR_NOS_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.FIVE_NOS_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.SIX_NOS_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.SEVEN_NOS_BUTTON, "1")

        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.EIGHT_NOS_BUTTON, "1")
        preDefinedRemoteButtonsHashmap.put(LibreMavidHelper.REMOTECONTROLBUTTONNAME.NINE_NOS_BUTTON, "1")

    }


    private fun initClickListnersForRemoteButtons() {

        ibPowerButton.setOnClickListener(this)
        ibMoreButton.setOnClickListener(this)

        ibSourceButton.setOnClickListener(this)
        tvHomeButton.setOnClickListener(this)

        tvMenuButton.setOnClickListener(this)
        tvOptionButton.setOnClickListener(this)

        tvGuideButton.setOnClickListener(this)
        tvLangButton.setOnClickListener(this)

        ibVolumeMuteButton.setOnClickListener(this)
        ibBackButton.setOnClickListener(this)

        ibPrevButton.setOnClickListener(this)
        ibNextButton.setOnClickListener(this)

        tvVolPlusButton.setOnClickListener(this)
        tvVolMinusButton.setOnClickListener(this)


        ivUpButton.setOnClickListener(this)
        ivDownButton.setOnClickListener(this)


        ivLeftButton.setOnClickListener(this)
        ivRightButton.setOnClickListener(this)


        tvChUpButton.setOnClickListener(this)
        tvChDownButton.setOnClickListener(this)


        tvOkButton.setOnClickListener(this)
        tvBlueButton.setOnClickListener(this)

        tvGreenButton.setOnClickListener(this)
        tvYellowButton.setOnClickListener(this)

        tvRedButton.setOnClickListener(this)
        ibRewindButton.setOnClickListener(this)

        ibPlayButton.setOnClickListener(this)
        ibPauseButton.setOnClickListener(this)

        ibFastForwardButton.setOnClickListener(this)
        ibInfoButton.setOnClickListener(this)

        ibStopButton.setOnClickListener(this)
        tvExitButtonAlt.setOnClickListener(this)

        rlRecButton.setOnClickListener(this)

    }

    fun getActivityObject(): IRAddRemoteVPActivity? {
        return activity as IRAddRemoteVPActivity
    }

    fun showRemoteControlNumberButton() {
        getActivityObject()?.hideApplianceVp()

        getActivityObject()?.hideTabLayout()
        getActivityObject()?.frameContent?.visibility = View.VISIBLE

        val fragmentManager = getActivityObject()?.supportFragmentManager
        val fragmentTransaction = fragmentManager?.beginTransaction()

        fragmentTransaction?.setCustomAnimations(R.anim.fade_in, R.anim.fade_in, R.anim.fade_out, R.anim.fade_out);


        val iRRemoteControlNumberFragment = IRRemoteControlNumberFragment()


        fragmentTransaction?.add(R.id.frameContent, iRRemoteControlNumberFragment, TAG)
                ?.addToBackStack(TAG)
        fragmentTransaction?.commit()
    }


    override fun onClick(view: View?) {
        vibrateOnButtonClick()
        getActivityObject()?.showProgressBar()
        when (view?.id) {
            R.id.ibPowerButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.POWER_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.ibMoreButton -> {
                getActivityObject()?.dismissLoader()
                showRemoteControlNumberButton()
            }

            R.id.ibSourceButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.SOURCE_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.tvHomeButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.HOME_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.tvMenuButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.MENU_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.tvOptionButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.OPTION_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.tvGuideButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.GUIDE_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.tvLangButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.LANG_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.ibVolumeMuteButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.VOLUME_MUTE_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.ibBackButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.BACK_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.ibPrevButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.PREV_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.ibNextButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.NEXT_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.tvVolPlusButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.VOLUME_UP, object : OnRemoteKeyPressedInterface {
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

            R.id.tvVolMinusButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.VOLUME_DOWN, object : OnRemoteKeyPressedInterface {
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

            R.id.ivUpButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.UP_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.ivDownButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.DOWN_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.ivLeftButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.LEFT_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.ivRightButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.RIGHT_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.tvChUpButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.CHANNEL_UP, object : OnRemoteKeyPressedInterface {
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

            R.id.tvChDownButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.CHANNEL_DOWN, object : OnRemoteKeyPressedInterface {
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


            R.id.tvOkButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.OK_BUTTON, object : OnRemoteKeyPressedInterface {
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


            R.id.tvBlueButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.BLUE_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.tvGreenButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.GREEN_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.tvYellowButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.YELLOW_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.tvRedButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.RED_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.ibRewindButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.REWIND_BUTTON, object : OnRemoteKeyPressedInterface {
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


            R.id.ibPlayButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.PLAY_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.ibPauseButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.PAUSE_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.ibFastForwardButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.FAST_FORWARD_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.ibInfoButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.INFO_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.ibStopButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.STOP_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.tvExitButtonAlt -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.EXIT_BUTTON, object : OnRemoteKeyPressedInterface {
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

            R.id.rlRecButton -> {
                getActivityObject()?.sendTheKeysPressedIntoTheMavid3MDevice(
                        LibreMavidHelper.REMOTECONTROLBUTTONNAME.REC_BUTTON, object : OnRemoteKeyPressedInterface {
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