package com.mavid.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mavid.R
import com.mavid.irActivites.IRAddRemoteVPActivity
import com.mavid.irActivites.IRSelectTvOrTVPOrAcRegionalBrandsActivity
import kotlinx.android.synthetic.main.fragment_ir_ac_appliace_layout.*
import kotlinx.android.synthetic.main.fragment_ir_ac_appliace_layout.llRemoteUi
import kotlinx.android.synthetic.main.fragment_ir_television_appliance.*

class IRAcApplianceFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_ir_ac_appliace_layout, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        tvSelectAc.setOnClickListener {
            val intent = Intent(activity, IRSelectTvOrTVPOrAcRegionalBrandsActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable("deviceInfo", getActivityObject()?.deviceInfo)
            bundle.putBoolean("isAc",true)
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

}