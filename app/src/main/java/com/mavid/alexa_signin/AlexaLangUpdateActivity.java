package com.mavid.alexa_signin;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.danimahardhika.cafebar.CafeBar;
import com.mavid.adapters.ShowAlexaLanguageAdapter;
import com.mavid.MavidHomeTabsActivity;
import com.mavid.R;
import com.mavid.utility.DB.MavidNodes;
import com.mavid.libresdk.Constants.AlexaConstants;
import com.mavid.libresdk.LibreMavidHelper;
import com.mavid.libresdk.TaskManager.Communication.Listeners.CommandStatusListenerWithResponse;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.DeviceInfo;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.MessageInfo;
import com.mavid.libresdk.Util.BusEventProgressUpdate;
import com.mavid.libresdk.Util.LibreLogger;
import com.mavid.models.ModelAlexaLocalSupported;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class AlexaLangUpdateActivity extends AppCompatActivity {

//    private RadioGroup chooseLangRg;

    private TextView done;

    private AppCompatImageView back;

    private String selectedLang = "";

    private boolean changesDone = false;

    private String currentDeviceIp;

    private String fromActivity;

    private String prevScreen;

    private final int CHANGE_LOCALE_HANDLER = 0x021;

    private final int CHANGE_LOCALE_HANDLER_TIMEOUT = 10000;

    private Handler mHandler = new Handler() {

        @Override

        public void handleMessage(Message msg) {


            switch (msg.what) {

                case CHANGE_LOCALE_HANDLER:

                    //no response from MAVID device for setLocale

                    showSetLocaleFailure();

                    Log.d("atul", "locale failed handler");

                    break;

            }

        }

    };

    Dialog fwUpdateDialog;

//    private RadioButton enUsRb;
//    private RadioButton engUkRb;
//    private RadioButton engINRb;
//    private RadioButton deutschRb;
//    private RadioButton japanRb;
//    private RadioButton spanishRb;
//    private RadioButton italianRb;
//    private RadioButton frenchRb;
//    private RadioButton hindiRb;

    private RecyclerView rvAlexaLanguageList;

    private LinearLayoutManager linearLayoutManager;

    ShowAlexaLanguageAdapter showAlexaLanguageAdapter;

    private Dialog mDialog;

    private Dialog alertSirenaDialog3, alertSirenaDialog4;

    String locale = "";

    DeviceInfo deviceInfo;

    boolean langauageUpdateSucessful = false;


    private ProgressDialog progressDialog;


    boolean isAlertShownForSucessfulLangChange = false;

    private List<ModelAlexaLocalSupported> modelAlexaLocalSupportedList = new ArrayList<>();


    String lanaguageUrl;


    CountDownTimer countDownTimer;

    CafeBar cafeBar;

    AppCompatTextView progress_title;
    ProgressBar progress_bar;

    AppCompatTextView progress_message;

    private Dialog alertDialog;




//    private void closeLoader() {
//
//        if (progressDialog != null && progressDialog.isShowing()) {
//
//            runOnUiThread(new Runnable() {
//
//                @Override
//
//                public void run() {
//
//                    progressDialog.dismiss();
//
//                }
//
//            });
//
//        }

//}


    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alexa_update_lang);


//        chooseLangRg = (RadioGroup) findViewById(R.id.chooseLangRg);

        done = (TextView) findViewById(R.id.tv_done);

        back = findViewById(R.id.iv_back);


        rvAlexaLanguageList = findViewById(R.id.rv_alexa_language_list);

        fromActivity = getIntent().getStringExtra("fromActivity");

        prevScreen = getIntent().getStringExtra("prevScreen");


//        enUsRb = findViewById(R.id.enUsRb);
//
//        engUkRb = findViewById(R.id.engUkRb);
//
//        engINRb = findViewById(R.id.engINRb);
//
//        deutschRb = findViewById(R.id.deutschRb);
//
//        japanRb = findViewById(R.id.japanRb);
//
//        spanishRb = findViewById(R.id.spanishRb);
//
//        italianRb = findViewById(R.id.italianRb);
//
//        frenchRb = findViewById(R.id.frenchRb);

        EventBus.getDefault().register(this);

//        hindiRb = findViewById(R.id.hindiRb);


        progressDialog = new ProgressDialog(AlexaLangUpdateActivity.this);

        progressDialog.setCancelable(false);

        progressDialog.setMessage("Please Wait...");

//        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(R.color.appbar_bg));




        progressDialog.setContentView(R.layout.custom_progress_bar);


//        chooseLangRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
//
//                changesDone = true;
//
//                done.setText("Change");
//
//                if (i == R.id.enUsRb) {
//
//                    selectedLang = AlexaConstants.Languages.ENG_US;
//
//
//                } else if (i == R.id.engUkRb) {
//
//                    selectedLang = AlexaConstants.Languages.ENG_GB;
//
//
//                } else if (i == R.id.engINRb) {
//
//                    selectedLang = AlexaConstants.Languages.ENG_IN;
//
//
//                } else if (i == R.id.deutschRb) {
//
//                    selectedLang = AlexaConstants.Languages.DE;
//
//
//                } else if (i == R.id.japanRb) {
//
//                    selectedLang = AlexaConstants.Languages.JP;
//
//
//                } else if (i == R.id.spanishRb) {
//
//                    selectedLang = AlexaConstants.Languages.SPANISH;
//
//                } else if (i == R.id.italianRb) {
//
//                    selectedLang = AlexaConstants.Languages.ITALY;
//                }
//
//                else if (i == R.id.frenchRb) {
//                    selectedLang = AlexaConstants.Languages.FRENCH;
//                }
//
//                else if (i == R.id.hindiRb) {
//                    selectedLang = AlexaConstants.Languages.HINDI;
//                }
//
//            }
//
//        });


        done.setOnClickListener(new View.OnClickListener() {

            @Override

            public void onClick(View view) {
                LibreLogger.d(this, "suma in done changes\n" + changesDone + "selected language\n" + selectedLang);
//                if (!changesDone && selectedLang.isEmpty()) {
//                    intentToThingsToTryActivity();
//                }

//                if (done.getText().equals("Skip")) {
//
//                    Intent newIntent = new Intent(AlexaLangUpdateActivity.this, AlexaThingsToTryDoneActivity.class);
//                    //.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    newIntent.putExtra("speakerIpaddress", currentDeviceIp);
//
//                    newIntent.putExtra("fromActivity", fromActivity);
//
//                    startActivity(newIntent);
//
//                    finish();
//
//                    return;
//
//                }

                selectedLang = getSelectedLang();

                if (!selectedLang.isEmpty()) {
                    if (!locale.equals(selectedLang)) {
                        Log.d("atul lang update", "sending updated lang");
                        sendUpdatedLangToDevice(selectedLang);
                    } else {
                        intentToThingsToTryActivity();
                    }
                } else {
                    buildSnackBar("Change Language and try again");
                }
            }

        });


        back.setOnClickListener(new View.OnClickListener() {

            @Override

            public void onClick(View view) {

                onBackPressed();

            }

        });


        if (getIntent() != null

                &&

                getIntent().

                        getStringExtra("current_deviceip") != null

                && !

                getIntent().

                        getStringExtra("current_deviceip").

                        isEmpty()) {

            currentDeviceIp = getIntent().getStringExtra("current_deviceip");

        }

        deviceInfo = MavidNodes.getInstance().getDeviceInfoFromDB(currentDeviceIp);

        showLoader("Please Wait...", "");


        LibreMavidHelper.sendCustomCommands(currentDeviceIp, LibreMavidHelper.COMMANDS.FW_URL, "OTA_XML_URL", new

                CommandStatusListenerWithResponse() {


                    @Override

                    public void failure(Exception e) {
                        closeLoader();
                        Log.d("atul", "exception" + e.getMessage());

                    }


                    @Override

                    public void success() {


                    }


                    @Override

                    public void response(MessageInfo messageInfo) {


                        Log.d("atul_updateLang", messageInfo.getMessage());


                        if (messageInfo.getMessage() != null && messageInfo.getMessage().contains("amazonaws.com")) {

                            //removed firmware path
                            lanaguageUrl = messageInfo.getMessage().substring(0, messageInfo.getMessage().indexOf("/firmware_download.xml"));

                            //removed the last part ie the selected lang path
                            lanaguageUrl = lanaguageUrl.substring(0, lanaguageUrl.lastIndexOf("/"));

                            if (!lanaguageUrl.isEmpty()) {

                                lanaguageUrl = lanaguageUrl + "/locale_supported.json";

                                Log.d("atul_updated_lang_url_mod", lanaguageUrl);

                                getAlexaLanguages(lanaguageUrl);

                            }
                        } else {
                            closeLoader();
                            buildSnackBar("Not able to  fetch FW URL");
                            onBackPressed();
                        }


                    }

                });


    }

    public String getSelectedLang(){

        for (ModelAlexaLocalSupported modelAlexaLocalSupported : modelAlexaLocalSupportedList){
            if (modelAlexaLocalSupported.isChecked){
                Log.d("atul_model_langs", "lang: " + modelAlexaLocalSupported.Language + "country: " + modelAlexaLocalSupported.Country + "selected: ");
                return modelAlexaLocalSupported.Locale;
            }
        }
        return "";
    }




    public void showLoader(final String title, final String message) {
        if (mDialog == null) {
            mDialog = new Dialog(AlexaLangUpdateActivity.this);
            mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mDialog.setContentView(R.layout.custom_progress_bar);

            mDialog.setCancelable(false);
            progress_title = mDialog.findViewById(R.id.progress_title);
            progress_bar = mDialog.findViewById(R.id.progress_bar);
            progress_message = mDialog.findViewById(R.id.progress_message);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("ShowingLoader", "Showing loader method");
                progress_title.setText(title);
                progress_message.setText(message);
                progress_bar.setIndeterminate(true);
                progress_bar.setVisibility(View.VISIBLE);
                if (!(AlexaLangUpdateActivity.this.isFinishing())) {
                    mDialog.show();
                }
            }
        });
    }

    public void closeLoader() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mDialog != null) {
                    if (!(AlexaLangUpdateActivity.this.isFinishing())) {
                        mDialog.dismiss();
                        mDialog = null;
                    }
                }
            }
        });
    }


    public void buildSnackBar(String message) {
        CafeBar.Builder builder = CafeBar.builder(AlexaLangUpdateActivity.this);
        builder.autoDismiss(true);
        builder.customView(R.layout.custom_snackbar_layout);

        cafeBar = builder.build();
        AppCompatTextView tv_message = cafeBar.getCafeBarView().findViewById(R.id.tv_message);
        tv_message.setText(message);


        cafeBar.show();
    }

    public void setAlexaLanguageAdapter(List<ModelAlexaLocalSupported> modelAlexaLocalSupportedList, String locale) {

        linearLayoutManager = new LinearLayoutManager(AlexaLangUpdateActivity.this);

        rvAlexaLanguageList.setLayoutManager(linearLayoutManager);


        showAlexaLanguageAdapter = new ShowAlexaLanguageAdapter(AlexaLangUpdateActivity.this, modelAlexaLocalSupportedList, locale);
        rvAlexaLanguageList.setAdapter(showAlexaLanguageAdapter);

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageReceived(final BusEventProgressUpdate.SendFirmwareProgressEvents sendFirmwareProgressEvents) {

        if (sendFirmwareProgressEvents.getFwOTAProgressValue() != null && sendFirmwareProgressEvents.getDeviceIp().equals(currentDeviceIp)) {

            Log.d("atul", "current device ip:  " + currentDeviceIp + "device ip sent from discoveryman:  " + sendFirmwareProgressEvents.getDeviceIp());

            closeLoader();

            if (fwUpdateDialog == null) {
                fwUpdateDialog = new Dialog(AlexaLangUpdateActivity.this);
                fwUpdateDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                fwUpdateDialog.setCancelable(false);
                fwUpdateDialog.setContentView(R.layout.firmware_update_progress_dialog);
                fwUpdateDialog.show();
            }

            ProgressBar progressBar = (ProgressBar) fwUpdateDialog.findViewById(R.id.progressBar);

            Drawable progressDrawable = progressBar.getProgressDrawable().mutate();
            progressDrawable.setColorFilter(Color.parseColor("#D44A4F"), android.graphics.PorterDuff.Mode.SRC_IN);

            progressBar.setProgressDrawable(progressDrawable);
            progressBar.setProgress(Integer.parseInt(sendFirmwareProgressEvents.getFwOTAProgressValue()));


            if (sendFirmwareProgressEvents.getFwOTAProgressValue().equals("100")) {
                fwUpdateDialog.dismiss();
                langauageUpdateSucessful = true;
                if (!isAlertShownForSucessfulLangChange)
                    showCustomAlertDynamically("Changing locale", "Changing language successful.");
            }
            if (countDownTimer == null) {

                countDownTimer = new CountDownTimer(180000, 180000) {

                    public void onTick(long millisUntilFinished) {
                        if (sendFirmwareProgressEvents.getFwOTAProgressValue().equals("100")) {
                            langauageUpdateSucessful = true;
                            fwUpdateDialog.dismiss();
                            //closeLoader();
                            if (!isAlertShownForSucessfulLangChange)
                                showCustomAlertDynamically("Changing locale", "Changing language successful.");
                            // showSetLocaleSuccess();
                        }
                    }

                    public void onFinish() {
                        closeLoader();
                        if (!langauageUpdateSucessful) {
                            Log.d("atul", "failure finish countdown");
                            fwUpdateDialog.dismiss();
                            showSetLocaleFailure();
                        }
                    }

                }.start();
            }


            Log.d("atul", sendFirmwareProgressEvents.getFwOTAProgressValue());
        }
    }

//    public void showAlexaLanguages() {
//
//        for (ModelAlexaLocalSupported modelAlexaLocalSupported : modelAlexaLocalSupportedList) {
//
//            switch (modelAlexaLocalSupported.Locale) {
//
//                case AlexaConstants.Languages.ENG_US:
//
//                    enUsRb.setVisibility(View.VISIBLE);
//                    break;
//
//                case AlexaConstants.Languages.ENG_GB:
//                    engUkRb.setVisibility(View.VISIBLE);
//                    break;
//
//                case AlexaConstants.Languages.ENG_IN:
//
//                    engINRb.setVisibility(View.VISIBLE);
//                    break;
//
//
//                case AlexaConstants.Languages.DE:
//
//                    deutschRb.setVisibility(View.VISIBLE);
//                    break;
//
//
//                case AlexaConstants.Languages.JP:
//                    japanRb.setVisibility(View.VISIBLE);
//                    break;
//
//                case AlexaConstants.Languages.SPANISH:
//                    spanishRb.setVisibility(View.VISIBLE);
//
//                    break;
//
//
//                case AlexaConstants.Languages.ITALY:
//                    italianRb.setVisibility(View.VISIBLE);
//                    break;
//
//                case AlexaConstants.Languages.FRENCH:
//                    frenchRb.setVisibility(View.VISIBLE);
//                    break;
//
//                case AlexaConstants.Languages.HINDI:
//                    hindiRb.setVisibility(View.VISIBLE);
//                    break;
//
//
//                default:
//                    enUsRb.setVisibility(View.GONE);
//                    engUkRb.setVisibility(View.GONE);
//                    engINRb.setVisibility(View.GONE);
//                    deutschRb.setVisibility(View.GONE);
//                    japanRb.setVisibility(View.GONE);
//                    spanishRb.setVisibility(View.GONE);
//                    italianRb.setVisibility(View.GONE);
//                    frenchRb.setVisibility(View.GONE);
//                    hindiRb.setVisibility(View.GONE);
//
//            }
//        }
//
//        getUpdatedLangauge();
//    }


    public void getAlexaLanguages(String url) {


        RequestQueue requestQueue = Volley.newRequestQueue(AlexaLangUpdateActivity.this);


        StringRequest stringRequest = new StringRequest(

                Request.Method.GET,

                url,

                new Response.Listener<String>() {

                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)

                    @Override

                    public void onResponse(String response) {

                        // Do something with response string

                        Log.d("atul", response);

                        pareseAlexaLangaugaesJsonArray(response);


                    }

                },

                new Response.ErrorListener() {

                    @Override

                    public void onErrorResponse(VolleyError error) {
                        // Do something when get error
                        closeLoader();
                        try {
                            //may get an exception of ui thead
                            if (error instanceof AuthFailureError) {
                                languageUpdateAcessDenied("Access Denied", "Please try again later");
                            } else {
                                languageUpdateAcessDenied("Action failed", "Please try again later");
                            }
                        } catch (Exception e) {
                            Log.d("atul", e.toString());
                            languageUpdateAcessDenied("Action failed", "Please try again later");
                            e.printStackTrace();
                        }

                    }

                }

        );


        // Add StringRequest to the RequestQueue

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                300000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(stringRequest);


    }


    public void languageUpdateAcessDenied(String title, String message) {
        alertDialog = new Dialog(AlexaLangUpdateActivity.this);

        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        alertDialog.setContentView(R.layout.custom_single_button_layout);

        alertDialog.setCancelable(false);

        AppCompatTextView tv_alert_title = alertDialog.findViewById(R.id.tv_alert_title);

        AppCompatTextView tv_alert_message = alertDialog.findViewById(R.id.tv_alert_message);

        AppCompatButton btn_ok = alertDialog.findViewById(R.id.btn_ok);

        tv_alert_title.setText(title);

        tv_alert_message.setText(message);

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
                intentToHome();
            }
        });

        alertDialog.show();
    }

    public void pareseAlexaLangaugaesJsonArray(String jsonResponse) {


        try {


            JSONObject jsonObject = new JSONObject(jsonResponse);


            JSONArray jsonArray = jsonObject.getJSONArray("locale_supported");


            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject languageObject = jsonArray.getJSONObject(i);

                modelAlexaLocalSupportedList.

                        add(new ModelAlexaLocalSupported(

                                languageObject.getString("Locale"),

                                languageObject.getString("Language"),

                                languageObject.getString("Country")

                        ));

            }


        } catch (JSONException e) {

            e.printStackTrace();

        }
//        showAlexaLanguages();

        getUpdatedLangauge();

    }


    private void sendUpdatedLangToDevice(String locale) {

        showLoader("Please Wait...", "");

        mHandler.sendEmptyMessageDelayed(CHANGE_LOCALE_HANDLER, CHANGE_LOCALE_HANDLER_TIMEOUT);


        LibreMavidHelper.setAlexaLocale(currentDeviceIp, locale, new CommandStatusListenerWithResponse() {

            @Override

            public void response(MessageInfo messageInfo) {

                mHandler.removeMessages(CHANGE_LOCALE_HANDLER);

                String message = messageInfo.getMessage();

                Log.d("atul + my lang", messageInfo.getMessage());


                if (message != null) {


                    if (message.contains(AlexaConstants.SET_LOCALE_SUCCESS)) {

                        //Setting locale successful

                        setLocaleInUI(getLocaleFromSuccessMessage(message));


//                        showSetLocaleSuccess();


                    } else if (message.contains(AlexaConstants.SET_LOCALE_FAILURE)) {

                        //Setting locale failure

                        Log.d("atul", "locale failed updated language");

                        showSetLocaleFailure();


                    }

                }

            }


            @Override

            public void failure(Exception e) {
                closeLoader();

                mHandler.removeMessages(CHANGE_LOCALE_HANDLER);

                Log.d("atul", "locale failed updated language failure");

                showSetLocaleFailure();

            }


            @Override

            public void success() {


            }

        });

    }


    private String getLocaleFromSuccessMessage(String message) {

        String[] arr = message.split(":");

        if (arr == null || arr.length < 2 || arr[1] == null || arr[1].isEmpty()) {

            return "";

        }

        return arr[1];

    }

    private void showCustomAlertDynamically(String title, String message) {
        closeLoader();
        alertSirenaDialog3 = new Dialog(AlexaLangUpdateActivity.this);
        alertSirenaDialog3.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertSirenaDialog3.setContentView(R.layout.vood_custom_dialog);
        alertSirenaDialog3.setCancelable(false);
        TextView title1 = (TextView) alertSirenaDialog3.findViewById(R.id.title);
        title1.setText(title);
        TextView content = (TextView) alertSirenaDialog3.findViewById(R.id.message_content);
        content.setText(message);
        alertSirenaDialog3.show();

        isAlertShownForSucessfulLangChange = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                alertSirenaDialog3.dismiss();
                showCustomAlertDynamically2("Please wait...", "Device is Rebooting");
//                                            intentToThingsToTryActivity();
                // intentToHome();
            }
        }, 10000);


    }

    private void showCustomAlertDynamically2(String title, String message) {
        closeLoader();
        alertSirenaDialog4 = new Dialog(AlexaLangUpdateActivity.this);
        alertSirenaDialog4.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertSirenaDialog4.setContentView(R.layout.vood_custom_dialog);
        alertSirenaDialog4.setCancelable(false);
        TextView title1 = (TextView) alertSirenaDialog4.findViewById(R.id.title);
        title1.setText(title);
        TextView content = (TextView) alertSirenaDialog4.findViewById(R.id.message_content);
        content.setText(message);
        alertSirenaDialog4.show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                alertSirenaDialog4.dismiss();
                intentToHome();
            }
        }, 10000);

    }

//    AlertDialog alertDialog = null;


    private void showSetLocaleFailure() {

        closeLoader();

        runOnUiThread(new Runnable() {

            @Override

            public void run() {

                if (!AlexaLangUpdateActivity.this.isFinishing()) {

                    try {

                        if (alertDialog != null && alertDialog.isShowing()) {

                            alertDialog.dismiss();

                        }

                    } catch (Exception e) {

                        e.printStackTrace();

                    }


                    alertDialog = new Dialog(AlexaLangUpdateActivity.this);


                    alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                    alertDialog.setContentView(R.layout.custom_single_button_layout);

                    alertDialog.setCancelable(false);

                    AppCompatTextView tv_alert_title = alertDialog.findViewById(R.id.tv_alert_title);

                    AppCompatTextView tv_alert_message = alertDialog.findViewById(R.id.tv_alert_message);

                    AppCompatButton btn_ok = alertDialog.findViewById(R.id.btn_ok);

                    tv_alert_title.setText(getString(R.string.changingLocale));

                    tv_alert_message.setText(getString(R.string.changingLocaleFailed));

                    btn_ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            alertDialog.dismiss();

                            alertDialog = null;
                            intentToHome();
                        }
                    });
                    alertDialog.show();

//                    AlertDialog.Builder builder = new AlertDialog.Builder(AlexaLangUpdateActivity.this);
//
//                    String mMessage = String.format();
//
//                    builder.setTitle()
//
//                            .setMessage(mMessage)
//
//                            .setCancelable(false)
//
//                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
//
//                                public void onClick(DialogInterface dialog, int id) {
//
//
//
//                                }
//
//                            });


                }
            }

        });

    }


    @Override

    protected void onStart() {

        super.onStart();

    }


    @Override

    protected void onResume() {

        super.onResume();
//        showloader();
//        getUpdatedLangauge();
    }


    public void getUpdatedLangauge() {


        LibreMavidHelper.askMetaDataInfo(currentDeviceIp, new CommandStatusListenerWithResponse() {

            @Override

            public void response(MessageInfo messageInfo) {


                //get locale information

                String alexaMessage = messageInfo.getMessage();

                Log.d("Alexalogin", "Alexa Value From 234  " + alexaMessage);

                closeLoader();

                try {

                    final String TAG_WINDOW_CONTENT = "Window CONTENTS";

                    JSONObject jsonRootObject = new JSONObject(alexaMessage);

                    JSONObject jsonObject = jsonRootObject.getJSONObject(TAG_WINDOW_CONTENT);


                    if (jsonObject.has("LOCALE"))

                        locale = jsonObject.optString("LOCALE").toString();
                    selectedLang = locale;

                    setLocaleInUI(locale);


                } catch (Exception e) {


                }

            }


            @Override

            public void failure(Exception e) {

                closeLoader();

            }


            @Override

            public void success() {


            }

        });
    }

    private void setLocaleInUI(final String locale) {

        runOnUiThread(new Runnable() {

            @Override

            public void run() {

//                updateLangUI(locale);
                setAlexaLanguageAdapter(modelAlexaLocalSupportedList, locale);
            }

        });


    }


    private void showSetLocaleSuccess() {

        closeLoader();

        runOnUiThread(new Runnable() {

            @Override

            public void run() {

                if (!AlexaLangUpdateActivity.this.isFinishing()) {

                    try {

                        if (alertDialog != null && alertDialog.isShowing()) {

                            alertDialog.dismiss();

                        }

                    } catch (Exception e) {

                        e.printStackTrace();

                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(AlexaLangUpdateActivity.this);

                    String mMessage = String.format(getString(R.string.changingLocaleSuccess));

                    builder.setTitle(getString(R.string.changingLocale))

                            .setMessage(mMessage)

                            .setCancelable(false)

                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int id) {

                                    alertDialog.dismiss();


                                    alertSirenaDialog3 = new Dialog(AlexaLangUpdateActivity.this);
                                    alertSirenaDialog3.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                    alertSirenaDialog3.setContentView(R.layout.vood_custom_dialog);
                                    alertSirenaDialog3.setCancelable(false);
                                    TextView title1 = (TextView) alertSirenaDialog3.findViewById(R.id.title);
                                    title1.setText("Please Wait...");
                                    TextView content = (TextView) alertSirenaDialog3.findViewById(R.id.message_content);
                                    content.setText("Device is Rebooting");
                                    alertSirenaDialog3.show();
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            alertSirenaDialog3.dismiss();
//                                            intentToThingsToTryActivity();
                                            intentToHome();
                                        }
                                    }, 15000);


                                }

                            });

                    if (alertDialog == null || !alertDialog.isShowing())

                        alertDialog = builder.create();


                    alertDialog.show();

                }

            }

        });

    }


    private void intentToHome() {
        Intent newIntent = new Intent(AlexaLangUpdateActivity.this, MavidHomeTabsActivity.class);
        startActivity(newIntent);

        finish();
    }

    private void intentToThingsToTryActivity() {

        if (fromActivity != null && !fromActivity.isEmpty()) {

            Intent newIntent = new Intent(AlexaLangUpdateActivity.this, AlexaThingsToTryDoneActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            newIntent.putExtra("speakerIpaddress", currentDeviceIp);

            newIntent.putExtra("fromActivity", fromActivity);

            startActivity(newIntent);

            finish();

        }

    }


//    private void showloader() {
//
//
//
//
//        /*If you happen to get the error : "requestFeature() must be called before adding content",
//
//        the solution is to call progressDialog.show() BEFORE you call progressDialog.setContentView(R.layout.progressdialog)*/
//
//        runOnUiThread(new Runnable() {
//
//            @Override
//
//            public void run() {
//                if (progressDialog != null) {
//                    progressDialog.show();
//
//                }
//            }
//
//        });
//
//    }


//    private void updateLangUI(String locale) {
//
//        switch (locale) {
//
//            case AlexaConstants.Languages.ENG_US:
//
//                chooseLangRg.check(R.id.enUsRb);
//
//                break;
//
//            case AlexaConstants.Languages.ENG_GB:
//
//                chooseLangRg.check(R.id.engUkRb);
//
//                break;
//
//            case AlexaConstants.Languages.ENG_IN:
//
//                chooseLangRg.check(R.id.engINRb);
//
//                break;
//
//            case AlexaConstants.Languages.DE:
//
//                chooseLangRg.check(R.id.deutschRb);
//
//                break;
//
//            case AlexaConstants.Languages.JP:
//
//                chooseLangRg.check(R.id.japanRb);
//
//                break;
//
//            case AlexaConstants.Languages.SPANISH:
//
//                chooseLangRg.check(R.id.spanishRb);
//
//                break;
//
//            case AlexaConstants.Languages.ITALY:
//
//                chooseLangRg.check(R.id.italianRb);
//
//                break;
//
//            case AlexaConstants.Languages.FRENCH:
//                chooseLangRg.check(R.id.frenchRb);
//                break;
//
//            case AlexaConstants.Languages.HINDI:
//                chooseLangRg.check(R.id.hindiRb);
//                break;
//
//        }
//
//
//    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override

    public void onBackPressed() {

        super.onBackPressed();

        intentToThingsToTryActivity();

    }

}