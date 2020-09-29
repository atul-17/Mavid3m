package com.mavid.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mavid.R
import com.mavid.irActivites.IRAddRemoteVPActivity
import com.mavid.irActivites.IRSelectTvAndTVPRegionalBrandsActivity
import kotlinx.android.synthetic.main.fragment_ir_ac_appliace_layout.*

class IRAcApplianceFragment : Fragment(){
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_ir_ac_appliace_layout,container,false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvSelectTV.setOnClickListener {
            val intent = Intent(activity, IRSelectTvAndTVPRegionalBrandsActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable("deviceInfo", getActivityObject()?.deviceInfo)
            intent.putExtras(bundle)
            startActivity(intent)
        }
    }

    fun getActivityObject(): IRAddRemoteVPActivity? {
        return activity as IRAddRemoteVPActivity
    }

}