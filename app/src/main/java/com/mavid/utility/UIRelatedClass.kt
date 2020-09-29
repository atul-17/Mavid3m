package com.mavid.utility

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.danimahardhika.cafebar.CafeBar
import com.google.android.material.snackbar.Snackbar
import com.mavid.R

class UIRelatedClass {

    fun buildSnackBarWithoutButton(context: Context, view: View, message: String) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent))
        snackbar.show()
    }


    fun buidCustomSnackBarWithButton(context: Context, textViewMessage: String, btnMessage: String, appCompatActivity: AppCompatActivity) {

        val builder = CafeBar.builder(context)
        builder.autoDismiss(false)
        builder.customView(R.layout.custom_snackbar_with_button_layout)
        var cafeBar: CafeBar? = builder.build();

        if (cafeBar != null) {
            val tvMessage: AppCompatTextView = cafeBar?.cafeBarView!!.findViewById(R.id.tvMessage)
            tvMessage.text = textViewMessage

            val btnOK: AppCompatButton = cafeBar?.cafeBarView.findViewById(R.id.btnOk)
            btnOK.setOnClickListener {

                appCompatActivity.finish()
            }
            cafeBar.show()
        }
    }


    fun showCustomDialogForUUIDMismatch(appCompatActivity: AppCompatActivity, onButtonClickCallback: OnButtonClickCallback) {
        val builder: AlertDialog.Builder =
                AlertDialog.Builder(appCompatActivity)

        val viewGroup: ViewGroup = appCompatActivity.findViewById(android.R.id.content)

        val dialogView: View =
                LayoutInflater.from(appCompatActivity)
                        .inflate(R.layout.custom_alert_uuid_mismatch_layout, viewGroup, false)

        builder.setView(dialogView);

        builder.setCancelable(false)

        val alertDialog: AlertDialog = builder.create()


        val btnProceed: AppCompatButton = dialogView.findViewById(R.id.btnProceed)


        btnProceed.setOnClickListener {
            alertDialog.dismiss()
            onButtonClickCallback.onClick(true)
        }

        alertDialog.show()
    }


    fun showUserCustomDialogForPrevSelectedRemote(appCompatActivity: AppCompatActivity,
                                                  onButtonClickCallback: OnButtonClickCallback) {
        if (!appCompatActivity.isFinishing) {

            var alert = Dialog(appCompatActivity)

            alert.requestWindowFeature(Window.FEATURE_NO_TITLE)

            alert.setContentView(R.layout.custom_single_button_layout)

            alert.setCancelable(false)

            val tv_alert_title: AppCompatTextView = alert.findViewById(R.id.tv_alert_title)

            val tv_alert_message: AppCompatTextView = alert.findViewById(R.id.tv_alert_message)

            val btn_ok: AppCompatButton = alert.findViewById(R.id.btn_ok)

            tv_alert_title.text = "Remote is already selected"

            tv_alert_message.text = "The remote for the appliance you have  selected  is already configured\nPlease go back  select another remote."

            btn_ok.setOnClickListener {
                alert.dismiss()
                onButtonClickCallback.onClick(true)
            }

            alert!!.show()
        }
    }


    fun showCustomAlertDialogForDeleteConfirmation(appCompatActivity: AppCompatActivity,
                                                   onButtonClickCallback: OnButtonClickCallback) {

        if (!appCompatActivity.isFinishing) {

            var alert = Dialog(appCompatActivity)

            alert.requestWindowFeature(Window.FEATURE_NO_TITLE)

            alert.setContentView(R.layout.custom_alert_two_buttons_layout)

            alert.setCancelable(false)

            val tv_alert_title: AppCompatTextView = alert.findViewById(R.id.tv_alert_title)

            val tv_alert_message: AppCompatTextView = alert.findViewById(R.id.tv_alert_message)

            val btn_ok: AppCompatButton = alert.findViewById(R.id.btn_ok)

            val btn_cancel: AppCompatButton = alert.findViewById(R.id.btn_cancel)

            tv_alert_title.text = "Are you sure?"

            tv_alert_message.text = "This will delete the your already configured appliance. "

            btn_cancel.setOnClickListener {
                alert.dismiss()
                onButtonClickCallback.onClick(false)
            }

            btn_ok.setOnClickListener {
                alert.dismiss()
                onButtonClickCallback.onClick(true)
            }


            alert!!.show()
        }
    }
}