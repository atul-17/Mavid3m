package com.mavid.irActivites

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mavid.R
import com.mavid.adapters.ApplianceFragmentAdapter
import com.mavid.libresdk.LibreMavidHelper
import com.mavid.libresdk.TaskManager.Communication.Listeners.CommandStatusListenerWithResponse
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.DeviceInfo
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.MessageInfo
import com.mavid.models.ModelGetUserDetailsAppliance
import com.mavid.models.ModelRemoteDetails
import com.mavid.utility.OnRemoteKeyPressedInterface
import com.mavid.utility.UIRelatedClass
import com.mavid.viewmodels.ApiViewModel
import kotlinx.android.synthetic.main.activity_ir_add_remote_vp.*
import kotlinx.android.synthetic.main.custom_tab_view_layout.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Type


class IRAddRemoteVPActivity : AppCompatActivity() {

    var TAG = IRAddRemoteVPActivity::class.java.simpleName

    // tab titles
    private var titles: MutableList<String> = ArrayList()

    private var selectedSetupBox = "Not Set"
    private var selectedTv = "Not Set"
    private var selectedAc = "Not Set"


    lateinit var progressDialog: Dialog

    var bundle = Bundle()
    var deviceInfo: DeviceInfo? = null


    var uIRelatedClass = UIRelatedClass()
    lateinit var frameContent: FrameLayout

    lateinit var sharedPreferences: SharedPreferences

    var userRemoteDetailsList: MutableList<ModelGetUserDetailsAppliance> = ArrayList()


    var selectedAppliance = "2"//by default tvp

    var tvSelectedBrand: String = ""
    var tvRemoteId: String = "0"

    var tvpSelectedBrand = ""
    var tvpRemoteId = ""

    var modelTvRemoteDetails = ModelRemoteDetails()

    var modelTvpRemoteDetails = ModelRemoteDetails()

    var gson: Gson? = Gson()


    var selectedTabIndex: Int = 0//by default selecting tvp

    lateinit var apiViewModel: ApiViewModel

    companion object {
        var irAddRemoteVPActivity: IRAddRemoteVPActivity? = null
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



        setTVRemoteDetails()

        setTVPRemoteDetails()

        titles.add("Setup Box")

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
                        val intent = Intent(this@IRAddRemoteVPActivity, IRSelectTvAndTVPRegionalBrandsActivity::class.java)
                        val bundle = Bundle()
                        bundle.putSerializable("deviceInfo", deviceInfo)
                        intent.putExtras(bundle)
                        startActivity(intent)
                    }
                }
                2 -> {
                    //ac
                    tvBrandName?.text = selectedAc
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


    fun setTVPRemoteDetails() {
        //getting from the local storage
        val modelTvpRemoteDetailsString: String = sharedPreferences.getString("tvpRemoteDetails", "")

        when {
            modelTvpRemoteDetailsString.isNotEmpty() -> {

                gson = Gson()

                appliancesSelectionVp.visibility = View.VISIBLE

                modelTvpRemoteDetails = gson?.fromJson<ModelRemoteDetails>(modelTvpRemoteDetailsString, ModelRemoteDetails::class.java) as ModelRemoteDetails

                tvpRemoteId = modelTvpRemoteDetails.remoteId

                tvpSelectedBrand = modelTvpRemoteDetails.selectedBrandName

                Log.d(TAG, "getttingTvpName ".plus(tvpSelectedBrand))
            }

            else -> {
                tvpRemoteId = "0"
                tvpSelectedBrand = "Not Set"
            }
        }


    }


    fun setTVRemoteDetails() {

        //getting from the local storage
        val modelTvRemoteDetailsString: String = sharedPreferences.getString("tvRemoteDetails", "")

        //remote is already configured
        //there is no remote configured from local storage
        when {
            modelTvRemoteDetailsString.isNotEmpty() -> {

                gson = Gson()

                appliancesSelectionVp.visibility = View.VISIBLE

                modelTvRemoteDetails = gson?.fromJson<ModelRemoteDetails>(modelTvRemoteDetailsString, ModelRemoteDetails::class.java) as ModelRemoteDetails

                tvRemoteId = modelTvRemoteDetails.remoteId

                tvSelectedBrand = modelTvRemoteDetails.selectedBrandName

            }

            else -> {
                tvRemoteId = "0"
                tvSelectedBrand = "Not Set"
            }
        }
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
            progressDialog.show()
        }
    }

    fun dismissLoader() {
        runOnUiThread {
            progressDialog.dismiss()
        }
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
            "1" -> {
                remoteId = tvRemoteId
            }
            "2" -> {
                //tvp
                remoteId = tvpRemoteId
            }
            //ac
            "3" -> {
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
                            Log.d(TAG, "ldapi#4 response".plus(dataJsonObject).toString())
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