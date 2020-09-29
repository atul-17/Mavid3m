package com.mavid.fragments

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mavid.R
import com.mavid.irActivites.IRAddRemoteVPActivity
import com.mavid.irActivites.IRSelectTvAndTVPRegionalBrandsActivity
import com.mavid.libresdk.LibreMavidHelper
import com.mavid.utility.OnRemoteKeyPressedInterface
import kotlinx.android.synthetic.main.fragment_ir_television_appliance.*


class IRTelevisionApplianceFragment : Fragment(), View.OnClickListener {

    private var vibe: Vibrator? = null

    private var TAG = IRTelevisionApplianceFragment::class.java.simpleName

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_ir_television_appliance, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /** checking whether tvBrand is not empty */
        if (getActivityObject()?.tvSelectedBrand.equals("Not Set")) {
            llSelectTv.visibility = View.VISIBLE
            llRemoteUi.visibility = View.GONE
        } else {
            llSelectTv.visibility = View.GONE
            llRemoteUi.visibility = View.VISIBLE
        }



        tvSelectTV.setOnClickListener {
            val intent = Intent(activity, IRSelectTvAndTVPRegionalBrandsActivity::class.java)
            val bundle = Bundle()
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

    fun getActivityObject(): IRAddRemoteVPActivity? {
        return activity as IRAddRemoteVPActivity
    }

    private fun initClickListnersForRemoteButtons() {

        ibPowerButton.setOnClickListener(this)
        ibMoreButton.setOnClickListener(this@IRTelevisionApplianceFragment)

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


}