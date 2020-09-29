package com.mavid.adapters

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mavid.fragments.IRAcApplianceFragment
import com.mavid.fragments.IRCustomApplianceFragment
import com.mavid.fragments.IRTVPApplianceFragment
import com.mavid.fragments.IRTelevisionApplianceFragment

class ApplianceFragmentAdapter(activity: AppCompatActivity, val titlesList:List<String>) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int {
        return titlesList.size
    }




    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return IRTVPApplianceFragment()

            1 ->{
                return IRTelevisionApplianceFragment()
            }

            2 -> return IRAcApplianceFragment()

            3 -> return IRCustomApplianceFragment()
        }
        return IRTVPApplianceFragment()
    }

}