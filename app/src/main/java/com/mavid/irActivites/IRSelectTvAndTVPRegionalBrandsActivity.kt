package com.mavid.irActivites

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.*
import com.mavid.R
import com.mavid.adapters.IRSelectRegionalTvpAdapter
import com.mavid.adapters.IRSelectTvBrandsAdapter
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.DeviceInfo
import com.mavid.models.ModelGetRegionalTvpPayloadSucess
import com.mavid.models.ModelGetTvpBrandsSucessResponse
import com.mavid.models.TvBrandsSucessRepoModel
import com.mavid.utility.RecyclerItemClickListener
import com.mavid.utility.UIRelatedClass
import com.mavid.viewmodels.ApiViewModel
import kotlinx.android.synthetic.main.activity_ir_select_tv_or_tvp_regional_brands.*
import kotlinx.android.synthetic.main.activity_ir_select_tv_or_tvp_regional_brands.progressBar


class IRSelectTvAndTVPRegionalBrandsActivity : AppCompatActivity() {

    lateinit var apiViewModel: ApiViewModel

    val uiRelatedClass = UIRelatedClass()

    var deviceInfo: DeviceInfo? = null

    var bundle = Bundle()

    var isTvp: Boolean = false

    var tvpBrandId: String = ""

    var tvpBrandName: String = ""

    companion object {
        var irSelectTvAndTVPRegionalBrandsActivity: IRSelectTvAndTVPRegionalBrandsActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ir_select_tv_or_tvp_regional_brands)

        irSelectTvAndTVPRegionalBrandsActivity = this


        bundle = intent.extras!!

        if (bundle != null) {
            deviceInfo = bundle.getSerializable("deviceInfo") as DeviceInfo
            isTvp = bundle.getBoolean("isTvp", false)
            tvpBrandId = bundle.getString("tvpBrandId", "")
            tvpBrandName = bundle.getString("tvpBrandName", "")
        }

        apiViewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application)).get(ApiViewModel::class.java)

        showProgressBar()
        if (!isTvp) {
            activityHeading.text = "Select your TV Manufacturer"
            getTvBrandsList()
        } else {
            activityHeading.text = "Select your Regional Setup Box"
            getTVPBrandList(tvpBrandId.toInt())
        }
    }

    fun getTVPBrandList(id: Int) {
        apiViewModel.getRegionalTvpBrandListApiResponse(id)?.observe(this, Observer {
            if (it.modelGetTvpBrandsSucessResponseList != null) {

                val sortedList = it.modelGetTvpBrandsSucessResponseList?.sortedBy { it.title } as MutableList<ModelGetRegionalTvpPayloadSucess>

                setTVPAdapter(sortedList)
            } else {
                val volleyError = it?.volleyError


                if (volleyError is TimeoutError || volleyError is NoConnectionError) {

                    uiRelatedClass.buidCustomSnackBarWithButton(this@IRSelectTvAndTVPRegionalBrandsActivity, "Seems your internet connection is slow, please try in sometime",
                            "Go Back", this@IRSelectTvAndTVPRegionalBrandsActivity)


                } else if (volleyError is AuthFailureError) {

                    uiRelatedClass.buildSnackBarWithoutButton(this@IRSelectTvAndTVPRegionalBrandsActivity,
                            window.decorView.findViewById(android.R.id.content), "AuthFailure error occurred, please try again later")


                } else if (volleyError is ServerError) {
                    if (volleyError.networkResponse.statusCode != 302) {
                        uiRelatedClass.buidCustomSnackBarWithButton(this@IRSelectTvAndTVPRegionalBrandsActivity, "Server error occurred, please try again later",
                                "Go Back", this@IRSelectTvAndTVPRegionalBrandsActivity)
                    }

                } else if (volleyError is NetworkError) {

                    uiRelatedClass.buidCustomSnackBarWithButton(this@IRSelectTvAndTVPRegionalBrandsActivity, "Network error occurred, please try again later",
                            "Go Back", this@IRSelectTvAndTVPRegionalBrandsActivity)


                } else if (volleyError is ParseError) {

                    uiRelatedClass.buidCustomSnackBarWithButton(this@IRSelectTvAndTVPRegionalBrandsActivity, "Parser error occurred, please try again later",
                            "Go Back", this@IRSelectTvAndTVPRegionalBrandsActivity)

                } else {

                    uiRelatedClass.buidCustomSnackBarWithButton(this@IRSelectTvAndTVPRegionalBrandsActivity, "SomeThing is wrong!!.Please Try after some time",
                            "Go Back", this@IRSelectTvAndTVPRegionalBrandsActivity)

                }
            }
            dismissLoader()
        })
    }


    fun getTvBrandsList() {
        apiViewModel.getTvBrandsDetails()?.observe(this, Observer {
            if (it.tvBrandsDetailsRepoModelList != null) {
                //they are tv brands

                val sortedList: MutableList<TvBrandsSucessRepoModel> = it.tvBrandsDetailsRepoModelList!!.sortedBy { it.name } as MutableList<TvBrandsSucessRepoModel>

                setTVAdapter(sortedList)
            } else {

                val volleyError = it?.volleyError


                if (volleyError is TimeoutError || volleyError is NoConnectionError) {

                    uiRelatedClass.buidCustomSnackBarWithButton(this@IRSelectTvAndTVPRegionalBrandsActivity, "Seems your internet connection is slow, please try in sometime",
                            "Go Back", this@IRSelectTvAndTVPRegionalBrandsActivity)


                } else if (volleyError is AuthFailureError) {

                    uiRelatedClass.buildSnackBarWithoutButton(this@IRSelectTvAndTVPRegionalBrandsActivity,
                            window.decorView.findViewById(android.R.id.content), "AuthFailure error occurred, please try again later")


                } else if (volleyError is ServerError) {
                    if (volleyError.networkResponse.statusCode != 302) {
                        uiRelatedClass.buidCustomSnackBarWithButton(this@IRSelectTvAndTVPRegionalBrandsActivity, "Server error occurred, please try again later",
                                "Go Back", this@IRSelectTvAndTVPRegionalBrandsActivity)
                    }

                } else if (volleyError is NetworkError) {

                    uiRelatedClass.buidCustomSnackBarWithButton(this@IRSelectTvAndTVPRegionalBrandsActivity, "Network error occurred, please try again later",
                            "Go Back", this@IRSelectTvAndTVPRegionalBrandsActivity)


                } else if (volleyError is ParseError) {

                    uiRelatedClass.buidCustomSnackBarWithButton(this@IRSelectTvAndTVPRegionalBrandsActivity, "Parser error occurred, please try again later",
                            "Go Back", this@IRSelectTvAndTVPRegionalBrandsActivity)

                } else {

                    uiRelatedClass.buidCustomSnackBarWithButton(this@IRSelectTvAndTVPRegionalBrandsActivity, "SomeThing is wrong!!.Please Try after some time",
                            "Go Back", this@IRSelectTvAndTVPRegionalBrandsActivity)

                }
            }
            dismissLoader()
        })
    }

    fun setTVAdapter(tvBrandsList: MutableList<TvBrandsSucessRepoModel>) {
        /** <ID>: 1 : TV
        2 : STB
        3 : AC*/

        val itemDecorator = DividerItemDecoration(this@IRSelectTvAndTVPRegionalBrandsActivity, DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(ContextCompat.getDrawable(this@IRSelectTvAndTVPRegionalBrandsActivity, R.drawable.orange_divider)!!)

        rvTvBrandsList.layoutManager = LinearLayoutManager(this@IRSelectTvAndTVPRegionalBrandsActivity)

        rvTvBrandsList.adapter = IRSelectTvBrandsAdapter(this@IRSelectTvAndTVPRegionalBrandsActivity, tvBrandsList)
        rvTvBrandsList.addItemDecoration(itemDecorator)

        rvTvBrandsList.addOnItemTouchListener(RecyclerItemClickListener(this@IRSelectTvAndTVPRegionalBrandsActivity, rvTvBrandsList, object : RecyclerItemClickListener.OnItemClickListener {
            override fun onLongItemClick(view: View?, position: Int) {
            }

            override fun onItemClick(view: View?, position: Int) {
                //taking the user for remote selection instructions
                val intent = Intent(this@IRSelectTvAndTVPRegionalBrandsActivity, IRRemoteSelectionInstrActivity::class.java)
                val bundle = Bundle()
                bundle.putInt("applianceId", tvBrandsList[position].id)
                bundle.putString("applianceBrandName", tvBrandsList[position].name)
                bundle.putString("ipAddress", deviceInfo?.ipAddress)
                bundle.putSerializable("deviceInfo", deviceInfo)
                bundle.putString("selectedApplianceType","1")
                intent.putExtras(bundle)
                startActivity(intent)

            }
        }))
    }

    fun setTVPAdapter(modelGetTvpBrandsSucessResponseList: MutableList<ModelGetRegionalTvpPayloadSucess>) {

        /** <ID>: 1 : TV
        2 : STB
        3 : AC*/

        val itemDecorator = DividerItemDecoration(this@IRSelectTvAndTVPRegionalBrandsActivity, DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(ContextCompat.getDrawable(this@IRSelectTvAndTVPRegionalBrandsActivity, R.drawable.orange_divider)!!)

        rvTvBrandsList.layoutManager = LinearLayoutManager(this@IRSelectTvAndTVPRegionalBrandsActivity)

        rvTvBrandsList.adapter = IRSelectRegionalTvpAdapter(this@IRSelectTvAndTVPRegionalBrandsActivity, modelGetTvpBrandsSucessResponseList)
        rvTvBrandsList.addItemDecoration(itemDecorator)

        rvTvBrandsList.addOnItemTouchListener(RecyclerItemClickListener(this@IRSelectTvAndTVPRegionalBrandsActivity, rvTvBrandsList, object : RecyclerItemClickListener.OnItemClickListener {
            override fun onLongItemClick(view: View?, position: Int) {
            }

            override fun onItemClick(view: View?, position: Int) {
                //taking the user for remote selection screen
                val intent = Intent(this@IRSelectTvAndTVPRegionalBrandsActivity, IRRemoteSelectionInstrActivity::class.java)
                val bundle = Bundle()
                bundle.putInt("applianceId", modelGetTvpBrandsSucessResponseList[position].regionalId)
                bundle.putString("applianceBrandName", modelGetTvpBrandsSucessResponseList[position].modelTvpGetRegionalBrand.tvpBrandTitle)
                bundle.putString("ipAddress", deviceInfo?.ipAddress)
                bundle.putSerializable("deviceInfo", deviceInfo)
                bundle.putString("selectedApplianceType","2")
                intent.putExtras(bundle)
                startActivity(intent)

            }
        }))
    }


    override fun onDestroy() {
        super.onDestroy()
        irSelectTvAndTVPRegionalBrandsActivity = null
    }

    fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
        progressBar.show()
    }

    fun dismissLoader() {
        progressBar.visibility = View.GONE
        progressBar.hide()
    }

}