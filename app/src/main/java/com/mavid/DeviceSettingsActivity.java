package com.mavid;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

import androidx.core.widget.NestedScrollView;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.danimahardhika.cafebar.CafeBar;
import com.mavid.adapters.SsidListAdapter;
import com.mavid.BluetoothActivities.MavidBtDiscoverActivity;
import com.mavid.Constants.Constants;
import com.mavid.utility.DB.MavidNodes;
import com.mavid.utility.FirmwareClasses.CheckFirmwareInfoClass;
import com.mavid.utility.FirmwareClasses.DownloadMyXmlListener;
import com.mavid.utility.FirmwareClasses.FirmwareUpdateHashmap;
import com.mavid.utility.FirmwareClasses.UpdatedMavidNodes;
import com.mavid.utility.FirmwareClasses.XmlParser;
import com.mavid.alexa_signin.AlexaSignInActivity;
import com.mavid.alexa_signin.AlexaThingsToTryDoneActivity;
import com.mavid.irActivites.IRLoginActivity;
import com.mavid.libresdk.LibreMavidHelper;
import com.mavid.libresdk.TaskManager.Communication.Listeners.CommandStatusListenerWithResponse;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.DeviceInfo;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.MessageInfo;
import com.mavid.libresdk.Util.BusEventProgressUpdate;
import com.mavid.libresdk.Util.LibreLogger;
import com.mavid.models.ModelDeviceState;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DeviceSettingsActivity extends BaseActivity implements
        CompoundButton.OnCheckedChangeListener {

    DeviceInfo deviceInfo;
    int countDeviceInfoClicks = 0, countBTInfoClicks = 0, countDebugINfoClicks = 0, countWifiINfoClicks = 0;
    TextView tv_device_name, tv_ip_address, tv_system_firmware, fwURLTextview, tv_connected_ssid_name;
    String ipAddress;
    String fw_URL, SSID;
    private SwitchCompat switch_ota_upgrade, switch_zigbee;
    LinearLayout llBluetoothSettingA2dp;
    JSONArray btScanListArray;
    //    LinearLayout A2dpBluetoothLayout;
    LinearLayout llZigbee;
    Dialog fwUpdateDialog, alertSirenaDialog3, fw1UpdateDialog, alertSirenaDialog2, fwUpdateDialog2;
    UpdatedMavidNodes updateNode;
    private XmlParser myXml;
    TextView dialogTitle, dialogTitle1;
    //    ImageView arrownDown1, arrowdown2, arrowdown3, arrowdown4;
//    LinearLayout mainDeviceNameLyt, BtInfoLayout, debugInfoLayout, debugInfoLayout1;
    FirmwareUpdateHashmap updateHash = FirmwareUpdateHashmap.getInstance();
    CountDownTimer fwCountDownTimer;
    private int PERMISSION_ACCESS_COARSE_LOCATION = 0x100;
//    private AlertDialog alert;

    LinearLayout llSsidList;

//    BottomSheetDialog bottomSheetDialog;

    AppCompatTextView tv_ota_switch_status, tv_zigbee_switch_status, tv_bt_audio_src_switch_status;

    List<ModelDeviceState> modelDeviceStateList = new ArrayList<>();


    SsidListAdapter ssidListAdapter;

    RecyclerView rvWifiList;

//    SwipeRefreshLayout swipeRefresh;

    AppCompatTextView tvNoData;

    AppCompatImageView iv_back;

    CafeBar cafeBar;


    private Dialog alert, customTwoAlertDialog;


    boolean isFwProgressSucessful = false, isBslProgressSucessful = false;


    Handler fwProgressHandler, bslProgressHandler;

    boolean areWeGettingAnyProgressValues = false;

    AppCompatImageView iv_battery_status;

    LinearLayout ll_battery_status;

    AppCompatTextView tv_battery_status;

    Timer batteryStatusTimer;

    LinearLayout main_ll;

    NestedScrollView nested_scroll_view;

    RelativeLayout rl_main;

    LinearLayout ll_alexa_settings;

    String amazonRefreshToken = "";

    AppCompatTextView tv_amazon_login;


    AppCompatImageView ivHideDebugField;

    LinearLayout llOtaUrl;

    int count = 0;

    AppCompatImageView ivShowSsidList;

    LinearLayout llConnectedNetwork;

    LinearLayout ll_ir;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.OTA_UPDATE_RESPONSE_NOT_RECEIVED:
                    closeLoader();
                    break;
                case Constants.BSL_UPDATE_RESPONSE_NOT_RECEIVED:
                    closeLoader();
                    break;
                case Constants.FAILED_TOGET_RESPONSE:
                    closeLoader();
                    buildSnackBar("Response not Received... Please Try Again Later");
//                    Toast.makeText(DeviceSettingsActivity.this, "Response not Received... Please Try Again Later", Toast.LENGTH_SHORT).show();
                    break;
                case Constants.TIMEOUT_FOR_FW_PROGRESS_UPDATE:
                    //showMessageCommunicationStatus("Timeout","Speaker did not respond. Please try again later");
                    // showCustomAlertDialog2("Error","Speaker did not respond. Please try again later");
                    showMessageCommunicationStatus("Error", "Speaker did not respond. Please try again later");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_settings);
        ipAddress = getIntent().getStringExtra(Constants.INTENTS.IP_ADDRESS);
        deviceInfo = MavidNodes.getInstance().getDeviceInfoFromDB(ipAddress);

//        LibreLogger.d(this, "Mavid get fw url \n" + deviceInfo.getFw_url());

        initViews();
        timerTaskForBatteryStatus();
        readAutoOta();
        readZigbeeStatus();
//        getA2dpBtType();
        SSID = getconnectedSSIDname(this);

        appDeviceState();


        ll_ir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DeviceSettingsActivity.this, IRLoginActivity.class);
                startActivity(intent);

            }
        });

        if (isLocationPermissionEnabled()) {
            afterPermit();
            if (!SSID.equals("<unknown ssid>")) {
                tv_connected_ssid_name.setText(SSID);
            } else {
                //assuming the unknown ssid is due to hotSpot connection
                if (getHotSpotName(deviceInfo.getFriendlyName()) != null) {
                    tv_connected_ssid_name.setText(getHotSpotName(deviceInfo.getFriendlyName()));
                }
            }

            // MavidApplication.doneLocationChange=true;
        } else {
            // MavidApplication.doneLocationChange=true;
            askPermit();
            if (!SSID.equals("<unknown ssid>")) {
                tv_connected_ssid_name.setText(SSID);
            } else {
                //assuming the unknown ssid is due to hotSpot connection
                tv_connected_ssid_name.setText(getHotSpotName(deviceInfo.getFriendlyName()));
            }
        }


        EventBus.getDefault().register(this);

        switch_zigbee.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LibreLogger.d(this, "zigbee suma in toggle button zigbee checked");

                if (isChecked) {
                    writeZigbeeStatus("True");
                    readZigbeeStatus();
                } else {
                    writeZigbeeStatus("False");
                    readZigbeeStatus();
                }

            }
        });
        if (deviceInfo == null)
            return;
        LibreLogger.d(this, "suma in device setting start get bt source or sink" + deviceInfo.getA2dpTypeValue());

        llBluetoothSettingA2dp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(DeviceSettingsActivity.this, MavidBtDiscoverActivity.class);
//                                intent.putExtra(Constants.INTENTS.BT_SOURCE_SCANLIST, messageInfo.getMessage().toString());
                intent.putExtra(Constants.INTENTS.IP_ADDRESS, ipAddress);
                startActivity(intent);

//                showLoader("Please wait...", "we are getting the BT scan list");
//
//                LibreMavidHelper.sendCustomCommands(deviceInfo.getIpAddress(), LibreMavidHelper.COMMANDS.START_BT_SEARCH, "", new CommandStatusListenerWithResponse() {
//                    @Override
//                    public void response(final MessageInfo messageInfo) {
//                        LibreLogger.d(this, "suma in device setitng screen bluetooth info" + messageInfo.getMessage());
//
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//
//                                if (!messageInfo.getMessage().isEmpty()) {
//                                    JSONObject mainObj = null;
//                                    try {
//                                        mainObj = new JSONObject(messageInfo.getMessage());
//                                    } catch (JSONException e) {
//                                        e.printStackTrace();
//                                    }
//                                    try {
//                                        btScanListArray = mainObj.getJSONArray("BLUETOOTH DEVICES");
//                                    } catch (JSONException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                                closeLoader();
//
//                            }
//                        });
//                    }
//
//                    @Override
//                    public void failure(Exception e) {
//                        showLoader("Try Again Later...", "Action Failed");
//                        Log.d("atul_bt_settings_excep", e.toString());
//                        final Timer timer3 = new Timer();
//                        timer3.schedule(new TimerTask() {
//                            public void run() {
//                                closeLoader();
//                                Intent intent = new Intent(DeviceSettingsActivity.this, MavidHomeTabsActivity.class);
//                                startActivity(intent);
//                                finish();
//                                timer3.cancel();
//                            }
//                        }, 3000);
//
//                    }
//
//                    @Override
//                    public void success() {
//
//                    }
//                });
//
            }
        });


    }


    private void timerTaskForBatteryStatus() {
        //call battery status function every 30 secs
        batteryStatusTimer = new Timer();
        batteryStatusTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Enter your code
                readBatteryStatus();
            }
        }, 0, 30000);

    }


    public void buildSnackBar(String message) {
        CafeBar.Builder builder = CafeBar.builder(DeviceSettingsActivity.this);
        builder.autoDismiss(true);
        builder.customView(R.layout.custom_snackbar_layout);

        cafeBar = builder.build();
        AppCompatTextView tv_message = cafeBar.getCafeBarView().findViewById(R.id.tv_message);
        tv_message.setText(message);

        cafeBar.show();
    }


    private void afterPermit() {
        if (!isLocationEnabled()) {
            LibreLogger.d(this, "Location is disabled");
            turnOnLocationService();
        }
    }


    public void showFwProgressFailure(String title, String message) {


        alert = new Dialog(DeviceSettingsActivity.this);

        alert.requestWindowFeature(Window.FEATURE_NO_TITLE);

        alert.setContentView(R.layout.custom_single_button_layout);

        alert.setCancelable(false);

        tv_alert_title = alert.findViewById(R.id.tv_alert_title);

        tv_alert_message = alert.findViewById(R.id.tv_alert_message);

        btn_ok = alert.findViewById(R.id.btn_ok);

        tv_alert_title.setText(title);

        tv_alert_message.setText(message);

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert.dismiss();
                Intent ssid = new Intent(DeviceSettingsActivity.this, MavidHomeTabsActivity.class);
                startActivity(ssid);
                finish();
            }
        });

        if (!DeviceSettingsActivity.this.isFinishing()) {
            alert.show();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void askPermit() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                Constants.PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == Constants.PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Do something with granted permission
            afterPermit();
            MavidApplication.doneLocationChange = true;
        } else if (requestCode == Constants.PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // user checked Never Ask again
                Log.d("asking permit", "permit ACCESS_COARSE_LOCATION Denied for ever");
                // show dialog

                if (alert == null) {

                    alert = new Dialog(DeviceSettingsActivity.this);

                    alert.requestWindowFeature(Window.FEATURE_NO_TITLE);

                    alert.setContentView(R.layout.custom_single_button_layout);

                    alert.setCancelable(false);

                    tv_alert_title = alert.findViewById(R.id.tv_alert_title);

                    tv_alert_message = alert.findViewById(R.id.tv_alert_message);

                    btn_ok = alert.findViewById(R.id.btn_ok);
                }

                tv_alert_title.setText(getString(R.string.permitNotAvailable));

                tv_alert_message.setText(getString(R.string.permissionMsg));

                btn_ok.setText(getString(R.string.gotoSettings));

                btn_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        alert.dismiss();
                        alert = null;
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                });

                alert.show();


//                AlertDialog.Builder requestPermission = new AlertDialog.Builder(DeviceSettingsActivity.this);
//                requestPermission.setTitle(getString(R.string.permitNotAvailable))
//                        .setMessage(getString(R.string.permissionMsg))
//                        .setPositiveButton(getString(R.string.gotoSettings), new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                //navigate to settings
//                                dialog.dismiss();
//                                Intent intent = new Intent();
//                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                                Uri uri = Uri.fromParts("package", getPackageName(), null);
//                                intent.setData(uri);
//                                startActivity(intent);
//                            }
//                        })
//                        .setCancelable(false);
//                if (alert == null) {
//                    alert = requestPermission.create();
//                }
//                if (alert != null && !alert.isShowing())
//                    alert.show();
            }
        }
    }


    public boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            //All location services are disabled
            return false;
        } else {
            return true;
        }
    }

    private void turnGPSOn() {
        Intent intent = new Intent(
                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    private void turnOnLocationService() {

        if (alert == null) {

            alert = new Dialog(DeviceSettingsActivity.this);

            alert.requestWindowFeature(Window.FEATURE_NO_TITLE);

            alert.setContentView(R.layout.custom_single_button_layout);

            alert.setCancelable(false);

            tv_alert_title = alert.findViewById(R.id.tv_alert_title);

            tv_alert_message = alert.findViewById(R.id.tv_alert_message);

            btn_ok = alert.findViewById(R.id.btn_ok);
        }


        tv_alert_title.setText(getResources().getString(R.string.locationServicesIsOff));

        tv_alert_message.setText(getResources().getString(R.string.enableLocationWifi));

        btn_ok.setText(getResources().getString(R.string.gotoSettings));

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert.dismiss();
                alert = null;
                turnGPSOn();
            }
        });

        alert.show();

//        AlertDialog.Builder builder = new AlertDialog.Builder(DeviceSettingsActivity.this);
//        builder.setCancelable(false);
//        builder.setTitle(getResources().getString(R.string.locationServicesIsOff))
//                .setMessage(getResources().getString(R.string.enableLocationWifi))
//                .setPositiveButton(getResources().getString(R.string.gotoSettings), new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        dialogInterface.cancel();
//                        turnGPSOn();
//                    }
//                });
//        builder.create();
//        builder.show();
    }

    private void readZigbeeStatus() {
        LibreMavidHelper.setZigBeeAutoRead(ipAddress, "", new CommandStatusListenerWithResponse() {
            @Override
            public void response(MessageInfo messageInfo) {
                handler.removeMessages(Constants.AUTO_OTA_UPDATE_RESPONSE_NOT_RECEIVED);
                closeLoader();
                final String message = messageInfo.getMessage();
                Log.d("readZigbee", "read message: " + message);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (message.equals("Success:True")) {
                            llZigbee.setVisibility(View.VISIBLE);
                            switch_zigbee.setChecked(true);
                        } else if (message.equals("Success:False")) {
                            llZigbee.setVisibility(View.VISIBLE);
                            switch_zigbee.setChecked(false);
                            Log.d("readZigbeeFalse", "read message: " + message);
                        } else {
                            llZigbee.setVisibility(View.GONE);
                            Log.d("readZigbeeEmpty", "read message: " + message);
                        }
//
                    }
                });
            }

            @Override
            public void failure(Exception e) {
                DeviceSettingsActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        llZigbee.setVisibility(View.GONE);
                        Log.d("readZigbeeFailure", "read message: ");

                    }
                });
                handler.removeMessages(Constants.AUTO_OTA_UPDATE_RESPONSE_NOT_RECEIVED);
                closeLoader();
            }

            @Override
            public void success() {

            }
        });

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageReceived(BusEventProgressUpdate.SendFirmwareProgressEvents progressEvents) {

        if (progressEvents.getDeviceIp().equals(ipAddress)) {


            if (progressEvents.getBLSProgressValue() != null) {

                areWeGettingAnyProgressValues = true;

                closeLoader();

                Log.d("Device Setting bsl", progressEvents.getBLSProgressValue());

                Log.d("Device Setting BSL", "current device ip:  " + ipAddress + "device ip sent from discoveryman:  " + progressEvents.getDeviceIp());

                showFirmWareProgressValues(progressEvents.getBLSProgressValue(), "Stage1 Update in Progress...");

                //extreme case when the device sends fw progress values event before bsl is finished
//                if (progressEvents.getFwOTAProgressValue() != null) {
//                    fw1UpdateDialog.dismiss();
//                    fw1UpdateDialog = null;
//                    showLoader("Please Wait...", "");
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            closeLoader();
//                        }
//                    }, 2000);
//                }


            } else if (progressEvents.getFwOTAProgressValue() != null) {

                areWeGettingAnyProgressValues = true;
                closeLoader();

                Log.d("Device Setting values", progressEvents.getFwOTAProgressValue());

                Log.d("Device Setting fwvalues", "current device ip:  " + ipAddress + "device ip sent from discoveryman:  " + progressEvents.getDeviceIp());


                showFirmWareProgressValues1(progressEvents.getFwOTAProgressValue(), "Stage2 Update in Progress...");
            }
        }
    }

    public void dismissLoaderAfterWaitingForFwBslUpdate() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!areWeGettingAnyProgressValues) {
                            closeLoader();
                            showFwProgressFailure("FirmWare Update", "Firmware update failed");
                        }
                    }
                });
            }
        }, 240000);
    }


    public void showFirmWareProgressValues(String progressValues, String progressType) {
        LibreLogger.d(this, "suma in device settings activity BSL\n" + progressType);

        if (fwUpdateDialog == null) {
            fwUpdateDialog = new Dialog(DeviceSettingsActivity.this);
            fwUpdateDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            fwUpdateDialog.setContentView(R.layout.firmware_update_progress_dialog);
            fwUpdateDialog.setCancelable(false);
            dialogTitle = (TextView) fwUpdateDialog.findViewById(R.id.title);
            dialogTitle.setText(progressType);
            fwUpdateDialog.show();
        }

        ProgressBar progressBar = (ProgressBar) fwUpdateDialog.findViewById(R.id.progressBar);

        Drawable progressDrawable = progressBar.getProgressDrawable().mutate();
        progressDrawable.setColorFilter(Color.parseColor("#D44A4F"), android.graphics.PorterDuff.Mode.SRC_IN);

        progressBar.setProgressDrawable(progressDrawable);
        progressBar.setProgress(Integer.parseInt(progressValues));


        if (progressValues.equals("100")) {
            fwUpdateDialog.dismiss();
            // if (progressType.equals("BSL")) {
            fwUpdateDialog = null;
            isBslProgressSucessful = true;
            showLoader("Please wait...", "Preparing for Stage2 Update");
        } else if (progressValues.equals("102") || progressValues.equals("103") || progressValues.equals("104")) {
            //bsl_PROG_HTTPFAIL
            //bsl_PROG_UPDATE_FAIL
            //bsl_PROG_UPDATE_TIMEOUT
            isBslProgressSucessful = false;
            showFwProgressFailure("BSL Progress", "Stage 1 update  failed");
        }

        if (bslProgressHandler == null) {
            bslProgressHandler = new Handler();
            bslProgressHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isBslProgressSucessful) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fwUpdateDialog.dismiss();
                                showFwProgressFailure("BSL Progress", "Stage 1 update  failed");
                                isBslProgressSucessful = false;
                            }
                        });
                    }
                }
            }, 120000);
        }

    }

    public void showFirmWareProgressValues1(final String progressValues, String progressType) {
        LibreLogger.d(this, "suma in device settings activity FW\n" + progressType);

        if (fwUpdateDialog2 == null) {

            fwUpdateDialog2 = new Dialog(DeviceSettingsActivity.this);
            fwUpdateDialog2.requestWindowFeature(Window.FEATURE_NO_TITLE);
            fwUpdateDialog2.setContentView(R.layout.firmware_update_progress_dialog);
            fwUpdateDialog2.setCancelable(false);
            dialogTitle1 = (TextView) fwUpdateDialog2.findViewById(R.id.title);
            dialogTitle1.setText(progressType);
            fwUpdateDialog2.show();
        }

        ProgressBar progressBar = (ProgressBar) fwUpdateDialog2.findViewById(R.id.progressBar);


        Drawable progressDrawable = progressBar.getProgressDrawable().mutate();
        progressDrawable.setColorFilter(Color.parseColor("#D44A4F"), android.graphics.PorterDuff.Mode.SRC_IN);

        progressBar.setProgressDrawable(progressDrawable);
        progressBar.setProgress(Integer.parseInt(progressValues));

        if (progressValues.equals("100")) {
            fwUpdateDialog2.dismiss();
            isFwProgressSucessful = true;
            showCustomAlertDynamically("Firmware Update Status", "Successful");
        }


        if (fwProgressHandler == null) {
            fwProgressHandler = new Handler();
            fwProgressHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isFwProgressSucessful) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fwUpdateDialog2.dismiss();
                                showFwProgressFailure("FirmWare Progress", "Stage 2 update  failed");
                                isFwProgressSucessful = false;
                            }
                        });
                    }
                }
            }, 120000);
        }
    }

    private void showCustomAlertDynamically(String title, String message) {
        closeLoader();
        alertSirenaDialog3 = new Dialog(DeviceSettingsActivity.this);
        alertSirenaDialog3.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertSirenaDialog3.setContentView(R.layout.vood_custom_dialog);
        alertSirenaDialog3.setCancelable(false);
        TextView title1 = (TextView) alertSirenaDialog3.findViewById(R.id.title);
        title1.setText(title);
        TextView content = (TextView) alertSirenaDialog3.findViewById(R.id.message_content);
        content.setText(message);
        alertSirenaDialog3.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                alertSirenaDialog3.dismiss();
                showLoader("Please Wait...", "Device is rebooting");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent ssid = new Intent(DeviceSettingsActivity.this, MavidHomeTabsActivity.class);
                        startActivity(ssid);
                    }
                }, 10000);
                // showCustomAlertDynamically2("Please wait...","Device is Rebooting");
//                                            intentToThingsToTryActivity();
                // intentToHome();
            }
        }, 5000);


    }

//    private void getA2dpBtType() {
//        showLoader("", "Please Wait...");
//        LibreMavidHelper.sendCustomCommands(deviceInfo.getIpAddress(), LibreMavidHelper.COMMANDS.START_BT_STATUS, "", new CommandStatusListenerWithResponse() {
//            @Override
//            public void response(MessageInfo messageInfo) {
//                Log.d("atul", "in mavid application get the status" + messageInfo.getMessage() + "friendly name:  " + deviceInfo.getFriendlyName());
//                if (!messageInfo.getMessage().isEmpty()) {
//
//                    JSONObject jObject = null;
//                    try {
//                        jObject = new JSONObject(messageInfo.getMessage());
//
//                        String btTypeSource = jObject.getString("BT_TYPE");
//                        LibreLogger.d(this, "suma in A2dp sink jsonobject next" + btTypeSource);
//
//                        deviceInfo.setA2dpTypeValue(btTypeSource);
//                        closeLoader();
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                        Log.d("atul json ex", e.getMessage());
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//                            }
//                        });
//
//                    }
//
//                }
//
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        setBTSourceVisibility();
//                    }
//                });
//
//            }
//
//            @Override
//            public void failure(final Exception e) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.d("atul  ex", e.getMessage());
//                        llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//                    }
//                });
//
//            }
//
//            @Override
//            public void success() {
//
//            }
//        });
//    }

//    public void setBTSourceVisibility() {
//
//        if (!deviceInfo.getA2dpTypeValue().equals("")) {
//            llBluetoothSettingA2dp.setVisibility(View.VISIBLE);
//
//            if (deviceInfo.getA2dpTypeValue().contains("SINK")) {
//                llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//                switch_bt_audio_src.setChecked(false);
//
//            } else if (deviceInfo.getA2dpTypeValue().contains("SOURCE")) {
//                llBluetoothSettingA2dp.setVisibility(View.VISIBLE);
//                switch_bt_audio_src.setChecked(true);
//
//            } else {
//                llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//            }
//        } else {
//            llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//
//        }
//
//    }

    private void initViews() {
        switch_ota_upgrade = findViewById(R.id.switch_ota_upgrade);

        switch_zigbee = findViewById(R.id.switch_zigbee);

        switch_ota_upgrade.setOnCheckedChangeListener(this);
        //switch_zigbee.setOnCheckedChangeListener(this);

        llZigbee = findViewById(R.id.ll_zigbee);

        tv_device_name = (TextView) findViewById(R.id.tv_device_name);

        tv_device_name.setText(deviceInfo.getFriendlyName());

        tv_ip_address = (TextView) findViewById(R.id.tv_ip_address);

        tv_ip_address.setText(deviceInfo.getIpAddress());

        tv_system_firmware = (TextView) findViewById(R.id.tv_system_firmware);

        tv_amazon_login = findViewById(R.id.tv_amazon_login);
        tv_system_firmware.setText(deviceInfo.getFwVersion());

        llSsidList = findViewById(R.id.ll_ssid_list);

        tv_battery_status = findViewById(R.id.tv_battery_status);

        LinearLayout llOtaUpgrade = findViewById(R.id.ll_soft_update);

        iv_back = findViewById(R.id.iv_back);

        main_ll = findViewById(R.id.main_ll);

        rl_main = findViewById(R.id.rl_main);

        nested_scroll_view = findViewById(R.id.nested_scroll_view);

        iv_battery_status = findViewById(R.id.iv_battery_status);

        ll_battery_status = findViewById(R.id.ll_battery_status);

        tv_bt_audio_src_switch_status = findViewById(R.id.tv_bt_audio_src_switch_status);

        tv_zigbee_switch_status = findViewById(R.id.tv_zigbee_switch_status);

//        mainDeviceNameLyt = (LinearLayout) findViewById(R.id.deviceInfoLayout);

        llBluetoothSettingA2dp = findViewById(R.id.ll_blutooth_settings);

//        A2dpBluetoothLayout = (LinearLayout) findViewById(R.id.A2dpBluetoothLayout);

//        switch_bt_audio_src = findViewById(R.id.switch_bt_audio_src);

        fwURLTextview = (TextView) findViewById(R.id.tv_ota_url);

        tv_connected_ssid_name = (TextView) findViewById(R.id.tv_connected_ssid_name);

        ivShowSsidList = findViewById(R.id.iv_show_ssid_list);


        ll_ir = findViewById(R.id.ll_ir);

//        llZigbee = (LinearLayout) findViewById(R.id.zibBeeLyt);

//        arrownDown1 = (ImageView) findViewById(R.id.arrowdown1);
//
//        BtInfoLayout = findViewById(R.id.BtInfoLayout);
//
//        arrowdown2 = findViewById(R.id.arrowdown2);
//
//        arrowdown3 = findViewById(R.id.arrowdown3);
//
//        debugInfoLayout = findViewById(R.id.debugInfoLayout);
//        debugInfoLayout1 = findViewById(R.id.wifiInfo);
//
//        arrowdown4 = findViewById(R.id.arrowdown4);

        ll_alexa_settings = findViewById(R.id.ll_alexa_settings);

        tv_ota_switch_status = findViewById(R.id.tv_ota_switch_status);

        tv_connected_ssid_name.setSelected(true);

        tv_system_firmware.setSelected(true);

        fwURLTextview.setSelected(true);

        ivHideDebugField = findViewById(R.id.iv_hide_debug_field);

        llOtaUrl = findViewById(R.id.ll_ota_url);


        llConnectedNetwork = findViewById(R.id.ll_connected_network);

//        if(!deviceInfo.getA2dpTypeValue().equals("")) {
//            A2dpBluetoothLayout.setVisibility(View.VISIBLE);
//            llBluetoothSettingA2dp.setVisibility(View.VISIBLE);
//            if (deviceInfo.getA2dpTypeValue().contains("SINK")) {
//                llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//                switch_bt_audio_src.setChecked(false);
//
//            } else if (deviceInfo.getA2dpTypeValue().contains("SOURCE")) {
//                llBluetoothSettingA2dp.setVisibility(View.VISIBLE);
//                switch_bt_audio_src.setChecked(true);
//
//            } else {
//                llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//
//            }
//        }
//        else {
//            //suma
//            A2dpBluetoothLayout.setVisibility(View.GONE);
//            llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//
//        }

//        llOtaUpgrade.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (/*updateNode.isFwUpdateNeededBtnEnableCheck()||*/MavidApplication.checkIsFwBtnLatest||MavidApplication.urlFailure) {
//                    LibreLogger.d(this, "suma in devicesetting check fwbtn has latest fw");
//                    final AlertDialog.Builder builder = new AlertDialog.Builder(DeviceSettingsActivity.this);
//                    builder.setMessage("Speaker already has updated firmware")
//                            .setCancelable(false)
//                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int id) {
//                                    //do things
//                                    dialog.dismiss();
//                                }
//                            });
//                    AlertDialog alert = builder.create();
//                    alert.show();
//                }
//                else{
//                    final AlertDialog.Builder alert = new AlertDialog.Builder(DeviceSettingsActivity.this);
//                    alert.setCancelable(false)
//                            .setMessage(getResources().getString(R.string.stelle_firmware_confirmalert))
//                            .setPositiveButton(getResources().getString(R.string.proceed), new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialogInterface, int ii) {
//                                    try {
//                                        if (deviceInfo.getBslInfoBeforeSplit().equalsIgnoreCase("PRIVATE_BUILD") ||/* updateNode.isFirmwareUpdateNeeded()*/MavidApplication.isFwNeeded) {
//                                            showCustomAlertDialog2("Please Wait...", "Waiting for Response");
//                                            final Timer timer3 = new Timer();
//                                            timer3.schedule(new TimerTask() {
//                                                public void run() {
//                                                    alertSirenaDialog2.dismiss();
//                                                    timer3.cancel();
//                                                    closeSirenaLoader2();
//                                                    //showCustomAlertDialog3("Please wait...","Connecting to Server");
////this will cancel the timer of the system
//                                                }
//                                            }, 7000);
//                                            closeSirenaLoader2();
//                                            BSLUpgradeWithoutCustomDialogs(ipAddress);
//                                        }
//                                        else{
//                                            showCustomAlertDialog2("Please Wait...", "Waiting for Response");
//                                            final Timer timer3 = new Timer();
//                                            timer3.schedule(new TimerTask() {
//                                                public void run() {
//                                                    alertSirenaDialog2.dismiss();
//                                                    timer3.cancel();
//                                                    closeSirenaLoader2();
//                                                    //showCustomAlertDialog3("Please wait...","Connecting to Server");
////this will cancel the timer of the system
//                                                }
//                                            }, 7000);
//                                            closeSirenaLoader2();
//                                            OTAUpgradeWithoutCustomDialogs(ipAddress);
//                                        }
//                                    } catch (Exception e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            })
//                            .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialogInterface, int ii) {
//                                    dialogInterface.dismiss();
//                                }
//                            })
//                            .create()
//                            .show();
//                }
//           }
//        });

        llOtaUpgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LibreLogger.d(this, "suma in device setting activity latest fw btn\n" + MavidApplication.checkIsFwBtnLatest);

                if (/*updateNode.isFwUpdateNeededBtnEnableCheck()||*/MavidApplication.checkIsFwBtnLatest || MavidApplication.urlFailure) {
                    LibreLogger.d(this, "suma in devicesetting check fwbtn has latest fw");

                    speakerAlreadyUpdatedAlert();

                } else {

                    showFirmWareCheckCustomAlert("", getResources().getString(R.string.proceed), getResources().getString(R.string.stelle_firmware_confirmalert),
                            new ButtonCallback() {
                                @Override
                                public void onButtonClickedCallback(boolean isProceed) {
                                    if (isProceed) {
                                        try {
                                            //add count down timer for 4- 5 minutes
                                            //close the timeout
                                            if (deviceInfo.getBslInfoBeforeSplit().equalsIgnoreCase("PRIVATE_BUILD")
                                                    ||/* updateNode.isFirmwareUpdateNeeded()*/MavidApplication.isFwNeeded) {

                                                showCustomAlertDialog2("Please Wait...", "Waiting for Response");
                                                final Timer timer3 = new Timer();
                                                timer3.schedule(new TimerTask() {
                                                    public void run() {
                                                        alertSirenaDialog2.dismiss();
                                                        timer3.cancel();
                                                        closeSirenaLoader2();
                                                        //showCustomAlertDialog3("Please wait...","Connecting to Server");
//this will cancel the timer of the system
                                                    }
                                                }, 7000);
                                                closeSirenaLoader2();
                                                BSLUpgradeWithoutCustomDialogs(ipAddress);
                                            } else {
                                                showCustomAlertDialog2("Please Wait...", "Waiting for Response");
                                                final Timer timer3 = new Timer();
                                                timer3.schedule(new TimerTask() {
                                                    public void run() {
                                                        alertSirenaDialog2.dismiss();
                                                        timer3.cancel();
                                                        closeSirenaLoader2();
                                                        //showCustomAlertDialog3("Please wait...","Connecting to Server");
//this will cancel the timer of the system
                                                    }
                                                }, 7000);
                                                closeSirenaLoader2();
                                                OTAUpgradeWithoutCustomDialogs(ipAddress);
                                            }

                                            //waiting for 4 minutes and dismissing the loader
                                            //if we are not getting any firmware update values
                                            dismissLoaderAfterWaitingForFwBslUpdate();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });


                }
            }
        });


//        switch_bt_audio_src.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
//               // Toast.makeText(DeviceSettingsActivity.this,"checked yes or no\n"+isChecked+"a2dptype"+deviceInfo.getA2dpTypeValue(),Toast.LENGTH_SHORT).show();
//                showLoader("Please Wait...","");
//
//              if(deviceInfo.getA2dpTypeValue().contains("SOURCE")) {
//                  LibreMavidHelper.sendCustomCommands(deviceInfo.getIpAddress(), LibreMavidHelper.COMMANDS.START_BT_SOURCE, "A2DP-SINK", new CommandStatusListenerWithResponse() {
//                      @Override
//                      public void response(final MessageInfo messageInfo) {
//                          Thread thread = new Thread() {
//                              @Override
//                              public void run() {
//                                  try {
//                                      Thread.sleep(2000);
////                                      switch_bt_audio_src.setChecked(false);
////                                      llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//                                      MavidApplication.btChangeSink = true;
//                                      LibreLogger.d(this, "bt status suma in device setting bt switch to Sink" + messageInfo.getMessage());
//                                  } catch (InterruptedException e) {
//
//                                  }
//
//
//                              }
//                          };
//                          thread.start(); //start  thread
//
//                          runOnUiThread(new Runnable() {
//                              @Override
//                              public void run() {
//                                  closeLoader();
//                                  //suma
//                                  llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//
//                              }
//                          });
//                          if (!messageInfo.getMessage().isEmpty()) {
//                              JSONObject jObject = null;
//                              try {
//                                  jObject = new JSONObject(messageInfo.getMessage());
//                              } catch (JSONException e) {
//                                  e.printStackTrace();
//                              }
//
//                              try {
//                                  String btTypeSink = jObject.getString("BT_TYPE");
//                                  LibreLogger.d(this,"suma in A2dp sink jsonobject next"+btTypeSink);
//                                  deviceInfo.setA2dpTypeValue(btTypeSink);
//                              } catch (JSONException e) {
//                                  e.printStackTrace();
//                              }
//
//                          }
//
//
//                      }
//
//                      @Override
//                      public void failure(Exception e) {
//                          showLoader("Try Again Later...","Action Failed");
//                          final Timer timer3 = new Timer();
//                          timer3.schedule(new TimerTask() {
//                              public void run() {
//                                  closeLoader();
//                                  Intent intent = new Intent(DeviceSettingsActivity.this, DeviceListFragment.class);
//                                  startActivity(intent);
//                                  finish();
//                                  timer3.cancel();
//                              }
//                          }, 3000);                      }
//
//                      @Override
//                      public void success() {
//
//                      }
//                  });
//              }
//              else if(deviceInfo.getA2dpTypeValue().contains("SINK")){
//                  LibreMavidHelper.sendCustomCommands(deviceInfo.getIpAddress(), LibreMavidHelper.COMMANDS.START_BT_SOURCE, "A2DP-SOURCE", new CommandStatusListenerWithResponse() {
//                      @Override
//                      public void response(final MessageInfo messageInfo) {
//                         // showLoader("please wait","");
//
//                          Thread thread = new Thread() {
//                              @Override
//                              public void run() {
//                                  try {
//                                      Thread.sleep(2000);
//                                      LibreLogger.d(this, "bt status suma in device setting bt switch to Source " + messageInfo.getMessage());
//                                      MavidApplication.btChangeSource=true;
//
//
//                                  } catch (InterruptedException e) {
//
//                                  }
//
//                              }
//                          };
//                          thread.start(); //start  thread
//                          runOnUiThread(new Runnable() {
//                              @Override
//                              public void run() {
//                                  closeLoader();
//                                  llBluetoothSettingA2dp.setVisibility(View.VISIBLE);
//
//                              }
//                          });
//                          if (!messageInfo.getMessage().isEmpty()) {
//
//                              JSONObject jObject = null;
//                              try {
//                                  jObject = new JSONObject(messageInfo.getMessage());
//                              } catch (JSONException e) {
//                                  e.printStackTrace();
//                              }
//
//                              try {
//                                  String btTypeSource= jObject.getString("BT_TYPE");
//                                  LibreLogger.d(this,"suma in A2dp sink jsonobject next"+btTypeSource);
//                                  deviceInfo.setA2dpTypeValue(btTypeSource);
//                              } catch (JSONException e)
//                              {
//                                  e.printStackTrace();
//                              }
//
//                          }
//
//
//                      }
//
//                      @Override
//                      public void failure(Exception e) {
//                          showLoader("Try Again Later...", "Action Failed");
//                        final Timer timer3 = new Timer();
//                        timer3.schedule(new TimerTask() {
//                            public void run() {
//                                closeLoader();
//                                Intent intent = new Intent(DeviceSettingsActivity.this, DeviceListFragment.class);
//                                // intent.putExtra(Constants.INTENTS.IP_ADDRESS,me)
//                                startActivity(intent);
//                                finish();
//                                timer3.cancel();
//                            }
//                        }, 3000);
//
//                      }
//
//                      @Override
//                      public void success() {
//
//                      }
//                  });
//              }
//
//
//
//                if(deviceInfo.getA2dpTypeValue().contains("SINK")){
//                    llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//                }
//
//                else if(deviceInfo.getA2dpTypeValue().contains("SOURCE"))
//                {
//                    if(!isChecked){
//                        llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//
//                    }
//                    else{
//                        llBluetoothSettingA2dp.setVisibility(View.VISIBLE);
//
//                    }
//
//                }
//                else{
//                    llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//
//                }
//
//                if(deviceInfo.getA2dpTypeValue().contains("SOURCE")) {
//                    if (isChecked) {
//                        LibreLogger.d(this, " a2dp status is checked is yes or no  source if ");
//
//                    } else {
//                        if (!isChecked) {
//                            LibreLogger.d(this, " a2dp status is checked is yes or no  source else ");
//
//                        }
//                    }
//                }
//
//                if(deviceInfo.getA2dpTypeValue().contains("SINK")) {
//                    if (isChecked) {
//                        LibreLogger.d(this, " a2dp status is checked is yes or no sink if ");
//
//                    } else {
//                        if (!isChecked) {
//                            LibreLogger.d(this, " a2dp status is checked is yes or no  sink else ");
//
//                        }
//                    }
//                }
//            }
//        });
//        switch_bt_audio_src.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showLoader("Please Wait...", "");
//
////                if (switch_bt_audio_src.isChecked()) {
////                    switch_bt_audio_src.setChecked(false);
////                } else {
////                    switch_bt_audio_src.setChecked(true);x
////                }
//
//
//                if (deviceInfo.getA2dpTypeValue().contains("SOURCE")) {
//                    LibreMavidHelper.sendCustomCommands(deviceInfo.getIpAddress(),
//                            LibreMavidHelper.COMMANDS.START_BT_SOURCE, "A2DP-SINK", new CommandStatusListenerWithResponse() {
//                                @Override
//                                public void response(final MessageInfo messageInfo) {
////                                    Thread thread = new Thread() {
////                                        @Override
////                                        public void run() {
////                                            try {
////                                                Thread.sleep(2000);
//////                                      switch_bt_audio_src.setChecked(false);
//////                                      llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
////                                                MavidApplication.btChangeSink = true;
////                                                LibreLogger.d(this, "bt status suma in device setting bt switch to Sink" + messageInfo.getMessage());
////                                            } catch (InterruptedException e) {
////
////                                            }
////                                        }
////                                    };
////                                    thread.start(); //start  thread
////                                    runOnUiThread(new Runnable() {
////                                        @Override
////                                        public void run() {
////                                            closeLoader();
////                                            //suma
////                                            llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
////                                        }
////                                    });
//
//
//                                    if (!messageInfo.getMessage().isEmpty()) {
//                                        JSONObject jObject = null;
//                                        try {
//                                            jObject = new JSONObject(messageInfo.getMessage());
//                                        } catch (JSONException e) {
//                                            e.printStackTrace();
//                                        }
//                                        try {
//                                            String btTypeSink = jObject.getString("BT_TYPE");
//                                            LibreLogger.d(this, "suma in A2dp sink jsonobject next" + btTypeSink);
//                                            deviceInfo.setA2dpTypeValue(btTypeSink);
//                                            runOnUiThread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    closeLoader();
//                                                    Log.d("suma in A2dp sink", String.valueOf(switch_bt_audio_src.isChecked()));
//                                                    checkAndUnCheckBluetoothSettingsA2dPButton(switch_bt_audio_src.isChecked());
//
//
//                                                }
//                                            });
//
//                                        } catch (JSONException e) {
//                                            e.printStackTrace();
//                                        }
//
//                                    }
//                                }
//
//                                @Override
//                                public void failure(Exception e) {
//                                    Log.d("atul_2", e.getLocalizedMessage());
//                                    showLoader("Try Again Later...", "Action Failed");
//                                    final Timer timer3 = new Timer();
//                                    timer3.schedule(new TimerTask() {
//                                        public void run() {
//                                            closeLoader();
//                                            Intent intent = new Intent(DeviceSettingsActivity.this, MavidHomeTabsActivity.class);
//                                            startActivity(intent);
//                                            finish();
//                                            timer3.cancel();
//                                        }
//                                    }, 3000);
//                                }
//
//                                @Override
//                                public void success() {
//
//                                }
//                            });
//                } else if (deviceInfo.getA2dpTypeValue().contains("SINK")) {
//                    LibreMavidHelper.sendCustomCommands(deviceInfo.getIpAddress(), LibreMavidHelper.COMMANDS.START_BT_SOURCE, "A2DP-SOURCE", new CommandStatusListenerWithResponse() {
//                        @Override
//                        public void response(final MessageInfo messageInfo) {
//                            // showLoader("please wait","");
//
////                            Thread thread = new Thread() {
////                                @Override
////                                public void run() {
////                                    try {
////                                        Thread.sleep(6000);
////                                        LibreLogger.d(this, "bt status suma in device setting bt switch to Source " + messageInfo.getMessage());
////                                        MavidApplication.btChangeSource = true;
////
////
////                                    } catch (InterruptedException e) {
////
////                                    }
////
////                                }
////                            };
////                            thread.start(); //start  thread
//
//
//                            if (!messageInfo.getMessage().isEmpty()) {
//
//                                JSONObject jObject = null;
//                                try {
//                                    jObject = new JSONObject(messageInfo.getMessage());
//                                } catch (JSONException e) {
//                                    e.printStackTrace();
//                                }
//
//                                try {
//                                    String btTypeSource = jObject.getString("BT_TYPE");
//                                    LibreLogger.d(this, "suma in A2dp sink jsonobject next" + btTypeSource);
//                                    deviceInfo.setA2dpTypeValue(btTypeSource);
//
//                                    runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
////                                            closeLoader();
////                                            llBluetoothSettingA2dp.setVisibility(View.VISIBLE);
//                                            closeLoader();
//                                            Log.d("suma in A2dp sink", String.valueOf(switch_bt_audio_src.isChecked()));
//                                            checkAndUnCheckBluetoothSettingsA2dPButton(switch_bt_audio_src.isChecked());
//
//                                        }
//                                    });
//                                } catch (JSONException e) {
//                                    e.printStackTrace();
//                                }
//
//                            }
//
//                        }
//
//                        @Override
//                        public void failure(Exception e) {
//                            Log.d("atul_3", e.toString());
//                            showLoader("Try Again Later...", "Action Failed");
//                            final Timer timer3 = new Timer();
//                            timer3.schedule(new TimerTask() {
//                                public void run() {
//                                    closeLoader();
//                                    Intent intent = new Intent(DeviceSettingsActivity.this, MavidHomeTabsActivity.class);
//                                    // intent.putExtra(Constants.INTENTS.IP_ADDRESS,me)
//                                    startActivity(intent);
//                                    finish();
//                                    timer3.cancel();
//                                }
//                            }, 3000);
//
//                        }
//
//                        @Override
//                        public void success() {
//
//                        }
//                    });
//                }
//
//
//                if (deviceInfo.getA2dpTypeValue().contains("SOURCE")) {
//                    if (switch_bt_audio_src.isChecked()) {
//                        LibreLogger.d(this, " a2dp status is checked is yes or no  source if ");
//
//                    } else {
//                        if (!switch_bt_audio_src.isChecked()) {
//                            LibreLogger.d(this, " a2dp status is checked is yes or no  source else ");
//
//                        }
//                    }
//                }
//
//                if (deviceInfo.getA2dpTypeValue().contains("SINK")) {
//                    if (switch_bt_audio_src.isChecked()) {
//                        LibreLogger.d(this, " a2dp status is checked is yes or no sink if ");
//
//                    } else {
//                        if (!switch_bt_audio_src.isChecked()) {
//                            LibreLogger.d(this, " a2dp status is checked is yes or no  sink else ");
//
//                        }
//                    }
//                }
//            }
//        });

//        llSsidList.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showWifiListBottomSheet();
//            }
//        });


        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        ll_alexa_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleAlexaRefreshTokenStatus(deviceInfo.getIpAddress(), amazonRefreshToken);
            }
        });


        ivHideDebugField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                count++;
                if (count % 2 == 0) {
                    llOtaUrl.setVisibility(View.VISIBLE);
                    fwURLTextview.setFocusable(true);

                    ivHideDebugField.setImageDrawable(getResources().getDrawable(R.drawable.ic_orange_down_arrow));

                } else {
                    llOtaUrl.setVisibility(View.GONE);
                    ivHideDebugField.setImageDrawable(getResources().getDrawable(R.drawable.ic_orange_up_arrow_));
                }
            }
        });

        llConnectedNetwork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DeviceSettingsActivity.this, ShowWifiListActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(Constants.INTENTS.IP_ADDRESS, deviceInfo.getIpAddress());
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

    }

    public void getAmazonRefreshToken() {
        LibreMavidHelper.askRefreshToken(deviceInfo.getIpAddress(), new CommandStatusListenerWithResponse() {
            @Override
            public void response(MessageInfo messageInfo) {
                amazonRefreshToken = messageInfo.getMessage();
                if (amazonRefreshToken.isEmpty()) {
                    tv_amazon_login.setText("Log-In");
                } else {
                    tv_amazon_login.setText("Logged In");
                }
                Log.d("DeviceSettings", " got alexa token " + amazonRefreshToken);
                closeLoader();
            }

            @Override
            public void failure(Exception e) {
                closeLoader();
                Log.d("DeviceSettings", "Exception: " + e);
            }

            @Override
            public void success() {
                closeLoader();
            }
        });
    }

    private void handleAlexaRefreshTokenStatus(String current_ipaddress, String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            /*not logged in*/
            Intent i = new Intent(DeviceSettingsActivity.this, AlexaThingsToTryDoneActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("speakerIpaddress", current_ipaddress);
            i.putExtra("fromActivity", MavidHomeTabsActivity.class.getSimpleName());
            startActivity(i);

        } else {
            Intent newIntent = new Intent(DeviceSettingsActivity.this, AlexaSignInActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            newIntent.putExtra("speakerIpaddress", current_ipaddress);
            newIntent.putExtra("fromActivity", MavidHomeTabsActivity.class.getSimpleName());
            startActivity(newIntent);
        }
    }


    public void closeSirenaLoader3() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertSirenaDialog3.cancel();
            }
        });

    }

    public void closeSirenaLoader2() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertSirenaDialog2.cancel();
            }
        });

    }


    public void showFirmWareCheckCustomAlert(String title, String buttonText, String message, final ButtonCallback buttonCallback) {


        customTwoAlertDialog = new Dialog(DeviceSettingsActivity.this);

        customTwoAlertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        customTwoAlertDialog.setContentView(R.layout.custom_alert_two_buttons_layout);

        customTwoAlertDialog.setCancelable(false);

        tv_alert_title = customTwoAlertDialog.findViewById(R.id.tv_alert_title);

        tv_alert_message = customTwoAlertDialog.findViewById(R.id.tv_alert_message);

        btn_ok = customTwoAlertDialog.findViewById(R.id.btn_ok);

        btn_cancel = customTwoAlertDialog.findViewById(R.id.btn_cancel);


        tv_alert_title.setText(title);

        tv_alert_message.setText(message);

        btn_ok.setText(buttonText);

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonCallback.onButtonClickedCallback(true);
                customTwoAlertDialog.dismiss();
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonCallback.onButtonClickedCallback(false);
                customTwoAlertDialog.dismiss();
            }
        });

        customTwoAlertDialog.show();


    }

    public interface ButtonCallback {
        void onButtonClickedCallback(boolean isProceed);
    }

    private void otaUpgrade(final String ipAddress) {
        handler.sendEmptyMessageDelayed(Constants.OTA_UPDATE_RESPONSE_NOT_RECEIVED, 5000);
        showLoader(getResources().getString(R.string.pleaseWait), "");
        //send command to device
        LibreMavidHelper.sendCustomCommands(ipAddress, LibreMavidHelper.COMMANDS.START_OTA_UPGRADE, "", new CommandStatusListenerWithResponse() {
            @Override
            public void response(MessageInfo messageInfo) {
                handler.removeMessages(Constants.OTA_UPDATE_RESPONSE_NOT_RECEIVED);
                closeLoader();
                String message = messageInfo.getMessage();
                try {
                    JSONObject messageJson = new JSONObject(message);
                    if (messageJson.getString("status").equalsIgnoreCase("success")) {
                        showMessageCommunicationStatus(
                                getResources().getString(R.string.success)
                                , getOTAMessage(messageJson.getString("message")));
                    } else {
                        showMessageCommunicationStatus(
                                getResources().getString(R.string.actionFailed)
                                , getOTAMessage(messageJson.getString("message")));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(Exception e) {
                handler.removeMessages(Constants.OTA_UPDATE_RESPONSE_NOT_RECEIVED);
                closeLoader();
            }

            @Override
            public void success() {

            }
        });
    }

    private void OTAUpgradeWithoutCustomDialogs(final String ipAddress) {
        handler.sendEmptyMessageDelayed(Constants.OTA_UPDATE_RESPONSE_NOT_RECEIVED, 5000);
        //showLoader(getResources().getString(R.string.pleaseWait),"");
        handler.sendEmptyMessageDelayed(Constants.FAILED_TOGET_RESPONSE, 5000);

        //send command to device
        LibreMavidHelper.sendCustomCommands(ipAddress, LibreMavidHelper.COMMANDS.START_OTA_UPGRADE, "", new CommandStatusListenerWithResponse() {
            @Override
            public void response(MessageInfo messageInfo) {
                //LibreLogger.d(this,"suma in software upgrade"+messageInfo);
                handler.removeMessages(Constants.OTA_UPDATE_RESPONSE_NOT_RECEIVED);
                handler.removeMessages(Constants.FAILED_TOGET_RESPONSE);
                //closeLoader();
                String message = messageInfo.getMessage();
                LibreLogger.d(this, "suma in software upgrade" + message);
                try {
                    JSONObject messageJson = new JSONObject(message);
                    LibreLogger.d(this, "suma in software upgrade2" + messageJson);

                    if (messageJson.getString("status").equalsIgnoreCase("success")) {
                        showMessageCommunicationStatus(
                                getResources().getString(R.string.firmware_upgrade_status)
                                , getResources().getString(R.string.firmware_upgrade_msg));

                    } else {
                        showMessageCommunicationStatus(
                                getResources().getString(R.string.actionFailed)
                                , getOTAMessage(messageJson.getString("message")));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(Exception e) {
                handler.removeMessages(Constants.OTA_UPDATE_RESPONSE_NOT_RECEIVED);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showLoader("Try Again Later...", "Action Failed");
                    }
                });
                final Timer timer3 = new Timer();
                timer3.schedule(new TimerTask() {
                    public void run() {
                        closeLoader();
                        timer3.cancel();
                    }
                }, 3000);


            }

            @Override
            public void success() {
                LibreLogger.d(this, "suma in success fw check");

            }
        });
    }

    private void BSLUpgradeWithoutCustomDialogs(String ipAddress) {
        handler.sendEmptyMessageDelayed(Constants.BSL_UPDATE_RESPONSE_NOT_RECEIVED, 5000);
        handler.sendEmptyMessageDelayed(Constants.FAILED_TOGET_RESPONSE, 5000);
        showLoader(getResources().getString(R.string.pleaseWait), "");
        //send command to device
        LibreMavidHelper.sendCustomCommands(ipAddress, LibreMavidHelper.COMMANDS.START_BSL_UPGRADE, "", new CommandStatusListenerWithResponse() {
            @Override
            public void response(MessageInfo messageInfo) {
                //LibreLogger.d(this,"suma in software upgrade"+messageInfo);
                handler.removeMessages(Constants.BSL_UPDATE_RESPONSE_NOT_RECEIVED);
                handler.removeMessages(Constants.FAILED_TOGET_RESPONSE);
                closeLoader();
                String message = messageInfo.getMessage();
                LibreLogger.d(this, "suma in software upgrade" + message);
                try {
                    JSONObject messageJson = new JSONObject(message);
                    LibreLogger.d(this, "suma in software upgrade2" + messageJson);

                    if (messageJson.getString("status").equalsIgnoreCase("success")) {
                        showMessageCommunicationStatus(
                                getResources().getString(R.string.firmware_upgrade_status)
                                , getResources().getString(R.string.firmware_upgrade_msg));

                    } else {
                        showMessageCommunicationStatus(
                                getResources().getString(R.string.actionFailed)
                                , getOTAMessage(messageJson.getString("message")));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(Exception e) {
                handler.removeMessages(Constants.BSL_UPDATE_RESPONSE_NOT_RECEIVED);
                showLoader("Try Again Later...", "Action Failed");
                final Timer timer3 = new Timer();
                timer3.schedule(new TimerTask() {
                    public void run() {
                        closeLoader();
                        timer3.cancel();
                    }
                }, 3000);


            }

            @Override
            public void success() {
                LibreLogger.d(this, "suma in success fw check");

            }
        });


    }

    public void showCustomAlertDialog2(final String title, final String message) {

        if (alertSirenaDialog2 != null && alertSirenaDialog2.isShowing())
            alertSirenaDialog2.cancel();
        alertSirenaDialog2 = new Dialog(DeviceSettingsActivity.this);
        alertSirenaDialog2.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertSirenaDialog2.setContentView(R.layout.vood_custom_dialog);
        alertSirenaDialog2.setCancelable(false);
        TextView title1 = (TextView) alertSirenaDialog2.findViewById(R.id.title);
        title1.setText(title);
        TextView content = (TextView) alertSirenaDialog2.findViewById(R.id.message_content);
        content.setText(message);
        alertSirenaDialog2.show();
    }

    private String getOTAMessage(String message) {
        String tempArr[] = message.split(":");
        if (tempArr.length > 1) {
            return tempArr[1];
        }
        return null;
    }


    public void speakerAlreadyUpdatedAlert() {

        if (alert == null) {

            alert = new Dialog(DeviceSettingsActivity.this);

            alert.requestWindowFeature(Window.FEATURE_NO_TITLE);

            alert.setContentView(R.layout.custom_single_button_layout);

            alert.setCancelable(false);

            tv_alert_title = alert.findViewById(R.id.tv_alert_title);

            tv_alert_message = alert.findViewById(R.id.tv_alert_message);

            btn_ok = alert.findViewById(R.id.btn_ok);
        }

        tv_alert_title.setText("");

        tv_alert_message.setText("Speaker already has updated firmware");

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert.dismiss();
            }
        });
        alert.show();


    }

    public void showMessageCommunicationStatus(final String title, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (alert == null) {

                    alert = new Dialog(DeviceSettingsActivity.this);

                    alert.requestWindowFeature(Window.FEATURE_NO_TITLE);

                    alert.setContentView(R.layout.custom_single_button_layout);

                    alert.setCancelable(false);

                    tv_alert_title = alert.findViewById(R.id.tv_alert_title);

                    tv_alert_message = alert.findViewById(R.id.tv_alert_message);

                    btn_ok = alert.findViewById(R.id.btn_ok);
                }

                tv_alert_title.setText(title);

                tv_alert_message.setText(message);

                btn_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showLoader("", "Please Wait...");
                        final Timer timer3 = new Timer();
                        timer3.schedule(new TimerTask() {
                            public void run() {
                                alert.dismiss();
//                                        closeLoader();
//                                        Intent ssid = new Intent(DeviceSettingsActivity.this,
//                                                DeviceListFragment.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                        startActivity(ssid);
//                                        finish();
                                timer3.cancel();

                            }
                        }, 4000);
                    }
                });
                alert.show();

//                AlertDialog.Builder alert = new AlertDialog.Builder(DeviceSettingsActivity.this);
//                alert.setTitle(title)
//                        .setMessage(message)
//                        .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialogInterface, int i) {
//                                //dialogInterface.dismiss();
//
//
//                            }
//                        })
//                        .create();
//                alert.show();
            }
        });

    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            writeAutoOta("True");
        } else {
            writeAutoOta("False");
        }
    }

    public void readBatteryStatus() {

        LibreMavidHelper.sendCustomCommands(deviceInfo.getIpAddress(), LibreMavidHelper.COMMANDS.BATTERY_STATUS, "COMP_APP_BATTERY", new CommandStatusListenerWithResponse() {
            @Override
            public void response(MessageInfo messageInfo) {
                String message = messageInfo.getMessage();
                LibreLogger.d(this, "suma in mavidapplication split charging" + message + " " + deviceInfo.getIpAddress());
                String[] msg = message.split("\n");
                LibreLogger.d(this, "Mavid" + message + "array msg" + msg);

                String chargingValueMain = msg[2];

                String chargingStatus = msg[1];

                LibreLogger.d(this, "Mavid charging value" + chargingValueMain);

                if (chargingValueMain != null && chargingStatus != null) {

                    String[] chargingValue = chargingValueMain.split(":");

                    String[] chargingStatusValue = chargingStatus.split(":");

                    if (chargingValue[1] != null && chargingStatusValue[1] != null) {

                        deviceInfo.setBatteryValue(chargingValue[1]);
                        deviceInfo.setBatteryStatus(chargingStatusValue[1]);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (deviceInfo.getBatteryValue() != null && deviceInfo.getBatteryStatus() != null) {
                                        LibreLogger.d(this, "atul" + deviceInfo.getBatteryValue());
                                        ll_battery_status.setVisibility(View.VISIBLE);
                                        tv_battery_status.setText(deviceInfo.getBatteryValue() + "%");
                                        if (deviceInfo.getBatteryStatus().equals("0")) {
                                            //DisConnected state
                                            if (Integer.parseInt(deviceInfo.getBatteryValue()) <= 25) {
                                                //low state
                                                iv_battery_status.setImageDrawable(getResources().getDrawable(R.drawable.battery_status_low));
                                            } else if (Integer.parseInt(deviceInfo.getBatteryValue()) > 25
                                                    && Integer.parseInt(deviceInfo.getBatteryValue()) <= 50) {
                                                //mid range
                                                iv_battery_status.setImageDrawable(getResources().getDrawable(R.drawable.batter_status_mid));
                                            } else if (Integer.parseInt(deviceInfo.getBatteryValue()) > 50
                                                    && Integer.parseInt(deviceInfo.getBatteryValue()) <= 75) {

                                                iv_battery_status.setImageDrawable(getResources().getDrawable(R.drawable.battery_status_almost_full));


                                            } else if (Integer.parseInt(deviceInfo.getBatteryValue()) > 75) {

                                                iv_battery_status.setImageDrawable(getResources().getDrawable(R.drawable.battery_status_full));

                                            }


                                        } else if (deviceInfo.getBatteryStatus().equals("1")) {
                                            //charging state

                                            if (Integer.parseInt(deviceInfo.getBatteryValue()) <= 25) {
                                                //low state
                                                iv_battery_status.setImageDrawable(getResources().getDrawable(R.drawable.battery_charing_low));

                                            } else if (Integer.parseInt(deviceInfo.getBatteryValue()) > 25
                                                    && Integer.parseInt(deviceInfo.getBatteryValue()) <= 50) {
                                                //mid range
                                                iv_battery_status.setImageDrawable(getResources().getDrawable(R.drawable.battery_charging_mid));
                                            } else if (Integer.parseInt(deviceInfo.getBatteryValue()) > 50
                                                    && Integer.parseInt(deviceInfo.getBatteryValue()) <= 75) {
                                                iv_battery_status.setImageDrawable(getResources().getDrawable(R.drawable.battery_charging_almost_full));

                                            } else if (Integer.parseInt(deviceInfo.getBatteryValue()) > 75) {
                                                iv_battery_status.setImageDrawable(getResources().getDrawable(R.drawable.battery_charging_full));
                                            }

                                        }

                                        LibreLogger.d(this, "suma in device setting activity more\n" + deviceInfo.getBatteryValue());
                                    } else {
                                        ll_battery_status.setVisibility(View.GONE);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        LibreLogger.d(this, "suma in mavidapplication split 2: " + deviceInfo.getBatteryValue());
                    }
                }

            }

            @Override
            public void failure(Exception e) {
                LibreLogger.d(this, "suma in device setting battery value" + deviceInfo.getBatteryValue());
            }

            @Override
            public void success() {

            }
        });
    }

    public void readAutoOta() {
        showLoader(getResources().getString(R.string.pleaseWait), "");
        //send command to device
        LibreMavidHelper.setAUtoOTARead(ipAddress, "", new CommandStatusListenerWithResponse() {
            @Override
            public void response(MessageInfo messageInfo) {
                handler.removeMessages(Constants.AUTO_OTA_UPDATE_RESPONSE_NOT_RECEIVED);
                closeLoader();
                String message = messageInfo.getMessage();
                Log.d("readAutoOta", "read message: " + message);
                if (message.contains("True")) {
                    switch_ota_upgrade.setChecked(true);
                } else if (message.contains("False")) {
                    switch_ota_upgrade.setChecked(false);
                } else if (message.contains("Fail")) {
                    switch_ota_upgrade.setChecked(false);
                }
            }

            @Override
            public void failure(Exception e) {
                handler.removeMessages(Constants.AUTO_OTA_UPDATE_RESPONSE_NOT_RECEIVED);
                closeLoader();
            }

            @Override
            public void success() {

            }
        });
    }

    public void writeAutoOta(String value) {
        showLoader(getResources().getString(R.string.pleaseWait), "");
        //send command to device
        LibreMavidHelper.setAUtoOTAWrite(ipAddress, value, new CommandStatusListenerWithResponse() {
            @Override
            public void response(MessageInfo messageInfo) {
                handler.removeMessages(Constants.AUTO_OTA_UPDATE_RESPONSE_NOT_RECEIVED);
                closeLoader();
                String message = messageInfo.getMessage();
                Log.d("readAutoOta", "write message: " + message);
                if (message.contains("True")) {
                    switch_ota_upgrade.setChecked(true);
                } else if (message.contains("False")) {
                    switch_ota_upgrade.setChecked(false);
                } else if (message.contains("Fail")) {
                    switch_ota_upgrade.setChecked(false);
                }
            }

            @Override
            public void failure(Exception e) {
                handler.removeMessages(Constants.AUTO_OTA_UPDATE_RESPONSE_NOT_RECEIVED);
                closeLoader();
            }

            @Override
            public void success() {

            }
        });
    }

    public void writeZigbeeStatus(String value) {
        showLoader(getResources().getString(R.string.pleaseWait), "Getting Response...");
        //send command to device
        LibreMavidHelper.setZigBeeWrite(ipAddress, value, new CommandStatusListenerWithResponse() {
            @Override
            public void response(MessageInfo messageInfo) {
                // handler.sendEmptyMessageDelayed(Constants.ZIGBEE_WRITE_RESPONSE_NOT_RECEIVED, 3000);
                String message = messageInfo.getMessage();
                closeLoader();
                Log.d("read zigbee Write", "write message: " + message);
                if (message.contains("True")) {
                    switch_zigbee.setChecked(true);
                } else if (message.contains("False")) {
                    switch_zigbee.setChecked(false);
                } else if (message.contains("Fail")) {
                    switch_zigbee.setChecked(false);
                } else if (message.isEmpty()) {
                    llZigbee.setVisibility(View.GONE);
                }

            }

            @Override
            public void failure(Exception e) {
                // handler.removeMessages(Constants.AUTO_OTA_UPDATE_RESPONSE_NOT_RECEIVED);
                closeLoader();
            }

            @Override
            public void success() {

            }
        });
    }

    public void appDeviceState() {
        showLoader("Please Wait...", "");
        LibreMavidHelper.sendCustomCommands(deviceInfo.getIpAddress(), LibreMavidHelper.COMMANDS.DEVICE_STATE, "", new CommandStatusListenerWithResponse() {

            @Override
            public void failure(Exception e) {

            }

            @Override
            public void success() {

            }

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void response(MessageInfo messageInfo) {
                closeLoader();
                Log.d("atul_device_state ", messageInfo.getMessage());
                try {
                    JSONArray deviceStateArray = new JSONArray(messageInfo.getMessage());
                    for (int i = 0; i < deviceStateArray.length(); i++) {

                        JSONObject deviceStateObject = deviceStateArray.getJSONObject(i);
                        if (deviceStateObject.getString("Name").equals("WiFi")) {

                            JSONArray ssidArray = new JSONArray(deviceStateObject.getString("SSID_list"));
                            for (int j = 0; j < ssidArray.length(); j++) {
                                JSONObject ssidObject = ssidArray.getJSONObject(j);
                                Log.d("atul_in_ssidobject ", ssidObject.toString());
                                modelDeviceStateList.add(
                                        new ModelDeviceState(ssidObject.getString("ssid"), ssidObject.getString("profile")));

                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        batteryStatusTimer.cancel();
    }

//    @Override
//    public void onItemClicked(final int pos) {
//        showLoader("", "Please Wait...");
//        JSONObject ssidObject = new JSONObject();
//        try {
//            ssidObject.put("ssid", modelDeviceStateList.get(pos).getSsid());
//            ssidObject.put("profile", Integer.parseInt(modelDeviceStateList.get(pos).getProfile()));
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        Log.d("atul on ssid clicked", ssidObject.toString());
//
//        LibreMavidHelper.sendCustomCommands(deviceInfo.getIpAddress(), LibreMavidHelper.COMMANDS.APP_WIFI_CONNECT, ssidObject.toString(), new CommandStatusListenerWithResponse() {
//            @Override
//            public void response(MessageInfo messageInfo) {
//                closeLoader();
//                bottomSheetDialog.dismiss();
//                if (messageInfo.getMessage().equals("success")) {
//                    if (!getconnectedSSIDname(DeviceSettingsActivity.this)
//                            .equals(modelDeviceStateList.get(pos).getSsid())) {
//                        //connectd ssidName and the selected ssid does not match
//                        //then take the user to change the wifiSettings
//                        new Handler(Looper.getMainLooper()).post(new Runnable() {
//                            @Override
//                            public void run() {
//                                //diffrent network
//                                showCustomAlertToChangeSSIDProfile("Notice", "Device has been successfully connected to "
//                                        + modelDeviceStateList.get(pos).getSsid() + ", please reconnect to " + modelDeviceStateList.get(pos).getSsid());
//                            }
//                        });
//                    } else {
//                        //same network
//                        new Handler(Looper.getMainLooper()).post(new Runnable() {
//                            @Override
//                            public void run() {
//                                showUserPresentInTheSameNetwork("Notice", "Device is already present in the same network");
//                            }
//                        });
//                    }
//                } else {
//                    buildSnackBar("Action failed, Please try again later");
//                }
//                Log.d("atul_app_wifi_cnt", messageInfo.getMessage());
//            }
//
//            @Override
//            public void failure(Exception e) {
//                closeLoader();
//                Log.d("atul_exception", e.toString());
//            }
//
//            @Override
//            public void success() {
//
//            }
//        });
//    }


    public void showUserPresentInTheSameNetwork(String title, String message) {
        if (alert == null) {

            alert = new Dialog(DeviceSettingsActivity.this);

            alert.requestWindowFeature(Window.FEATURE_NO_TITLE);

            alert.setContentView(R.layout.custom_single_button_layout);

            alert.setCancelable(false);

            tv_alert_title = alert.findViewById(R.id.tv_alert_title);

            tv_alert_message = alert.findViewById(R.id.tv_alert_message);

            btn_ok = alert.findViewById(R.id.btn_ok);
        }

        tv_alert_title.setText(title);

        tv_alert_message.setText(message);

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert.dismiss();
            }
        });

        alert.show();
    }

    public void showCustomAlertToChangeSSIDProfile(String title, String message) {
        if (alert == null) {

            alert = new Dialog(DeviceSettingsActivity.this);

            alert.requestWindowFeature(Window.FEATURE_NO_TITLE);

            alert.setContentView(R.layout.custom_single_button_layout);

            alert.setCancelable(false);

            tv_alert_title = alert.findViewById(R.id.tv_alert_title);

            tv_alert_message = alert.findViewById(R.id.tv_alert_message);

            btn_ok = alert.findViewById(R.id.btn_ok);
        }

        tv_alert_title.setText(title);

        tv_alert_message.setText(message);

        btn_ok.setText(getResources().getString(R.string.gotoSettings));

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert.dismiss();
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });

        alert.show();
    }

//    public void showWifiListBottomSheet() {
//        View view = getLayoutInflater().inflate(R.layout.show_wifi_list_bottom_sheet, null);
//        AppCompatTextView tvHeadingLabel = view.findViewById(R.id.tv_heading_label);
//        AppCompatImageView iv_close_icon = view.findViewById(R.id.iv_close_icon);
////        swipeRefresh = view.findViewById(R.id.swipe_refresh);
//        rvWifiList = view.findViewById(R.id.rv_wifi_list);
//        tvNoData = view.findViewById(R.id.tv_no_data);
//
//
//        tvHeadingLabel.setText("Wifi List");
//
//        iv_close_icon.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                bottomSheetDialog.dismiss();
//            }
//        });
//
//        bottomSheetDialog = new BottomSheetDialog(DeviceSettingsActivity.this);
//        bottomSheetDialog.setContentView(view);
//        bottomSheetDialog.setCancelable(false);
//
//        bottomSheetDialog.show();
//
//        setWifiListBottomSheetAdapter();
//    }

//    public void setWifiListBottomSheetAdapter() {
//
//        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(DeviceSettingsActivity.this);
//        ssidListAdapter = new SsidListAdapter(DeviceSettingsActivity.this, modelDeviceStateList);
//        rvWifiList.setAdapter(ssidListAdapter);
//
//        ssidListAdapter.setWifiConfigurationItemClickInterface(this);
//        rvWifiList.setLayoutManager(linearLayoutManager);
////        swipeRefresh.setRefreshing(false);
//        if (modelDeviceStateList.size() > 0) {
//            tvNoData.setVisibility(View.GONE);
//        } else {
//            tvNoData.setVisibility(View.VISIBLE);
//        }
//    }

//    public void checkAndUnCheckBluetoothSettingsA2dPButton(Boolean isChecked) {
//        if (deviceInfo.getA2dpTypeValue().contains("SINK")) {
//            llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//        } else if (deviceInfo.getA2dpTypeValue().contains("SOURCE")) {
//            if (!isChecked) {
//                llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//            } else {
//                llBluetoothSettingA2dp.setVisibility(View.VISIBLE);
//                llBluetoothSettingA2dp.setFocusable(true);
//
//            }
//        } else {
//            llBluetoothSettingA2dp.setVisibility(View.INVISIBLE);
//        }
//        rl_main.invalidate();
//    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        SSID = getconnectedSSIDname(this);
        if (!SSID.equals("<unknown ssid>")) {
            tv_connected_ssid_name.setText(SSID);
        } else {
            tv_connected_ssid_name.setText(getHotSpotName(deviceInfo.getFriendlyName()));
        }

        getAmazonRefreshToken();
        //check if ACCESS_COARSE_LOCATION permission is allowed
//        if (isLocationPermissionEnabled()) {
//            tv_connected_ssid_name.setText(SSID);
//            afterPermit();
//        }else{
//            askLocationPermission();
//        }

        LibreMavidHelper.sendCustomCommands(deviceInfo.getIpAddress(), LibreMavidHelper.COMMANDS.FW_URL, "OTA_XML_URL", new CommandStatusListenerWithResponse() {
            @Override
            public void failure(Exception e) {

            }

            @Override
            public void success() {

            }

            @Override
            public void response(MessageInfo messageInfo) {
                deviceInfo.setFw_url(messageInfo.getMessage());
                LibreLogger.d(this, "suma in mavid application check device url" + messageInfo.getMessage() + "device ip" + deviceInfo.getFriendlyName());
                if (messageInfo.getMessage() != null) {
                    fw_URL = messageInfo.getMessage();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fwURLTextview.setText(fw_URL);
                        }
                    });

                }
                myXml = new XmlParser(deviceInfo.getFw_url());
                myXml.fetchXml(new DownloadMyXmlListener() {

                    @Override
                    public void success(String fw_version, String bsl_version) {
                        LibreLogger.d(this, "getFw_version: " + fw_version);
                        if (!fw_version.isEmpty()) {
                            MavidApplication.urlFailure = false;
                            deviceInfo.setUpdatedFwVersion(fw_version);
                            deviceInfo.setBslInfoAfterSplit(bsl_version);

//                                            updateHashMap.put(deviceInfo.getIpAddress(),updateNode);
//                                            if(!updateHashMap.containsKey(deviceInfo.getIpAddress())) {
//                                                LibreLogger.d(this,"suma in device setting activity update hashmap"+deviceInfo.getFriendlyName());
//                                            }
//                                            else{
//                                                LibreLogger.d(this,"suma in device setting activity update hashmap not present"+deviceInfo.getFriendlyName());
//
//                                            }
                            CheckFirmwareInfoClass compareFirmwareClass = new CheckFirmwareInfoClass(deviceInfo);
                            updateNode = getMavidNodesUpdate(deviceInfo.getIpAddress());
                            updateNode.setFriendlyname(deviceInfo.getFriendlyName());
                            updateNode.setFwVersion(deviceInfo.getFwVersion());

                            FirmwareUpdateHashmap.getInstance().checkIfNodeAlreadyPresentinList(deviceInfo.getIpAddress());

                            LibreLogger.d(this, "suma in mavid application already notpresent" + deviceInfo.getFriendlyName());
                            updateNode.setFwUpdateNeededBtnEnable(compareFirmwareClass.checkFirmwareUpdateButtonEnableorDisable(deviceInfo.getIpAddress()));
                            //compareFirmwareClass.splitOldBslVersion();
                            if (updateNode.isFwUpdateNeededBtnEnableCheck()) {
                                MavidApplication.checkIsFwBtnLatest = true;
                                LibreLogger.d(this, "suma need fwupdatebtn has latest no need update secondcheck" + updateNode.getFriendlyname());
                            } else {
                                MavidApplication.checkIsFwBtnLatest = false;
                                LibreLogger.d(this, "suma need fwupdatebtn latest yes need update secondcheck" + updateNode.getFriendlyname());
                            }

                            updateNode.setFirmwareUpdateNeeded(compareFirmwareClass.checkIfFirmwareUpdateNeeded(deviceInfo.getIpAddress()));
                            if (updateNode.isFirmwareUpdateNeeded()) {
                                LibreLogger.d(this, "suma need fw" + updateNode.getFriendlyname());
                                MavidApplication.isFwNeeded = true;
                            } else {
                                LibreLogger.d(this, "suma need fw no" + updateNode.getFriendlyname());
                                MavidApplication.isFwNeeded = false;
                            }


                            // }
                        }
                    }

                    @Override
                    public void failure(Exception e) {
                        //comment suma
                        if (!MavidApplication.fwupdatecheckPrivateBuild) {
                            MavidApplication.urlFailure = true;
                        }
                        LibreLogger.d(this, "exception while parsing the XML 3 check suma" + e.toString());
                    }
                });
            }
        });

    }

    public UpdatedMavidNodes getMavidNodesUpdate(String ipAddress) {
        if (updateHash.getUpdateNode(ipAddress) != null) {
            LibreLogger.d(this, "updating old node" + updateNode.getFriendlyname());
            return updateHash.getUpdateNode(ipAddress);
        }
        LibreLogger.d(this, "creating new node");
        return new UpdatedMavidNodes();
    }

    public boolean isLocationPermissionEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LibreLogger.d(this, "checking permission ACCESS_COARSE_LOCATION permission is enabled");
                return true;
            }
        } else {
            //Android OS version is less than M. So permission is always enabled
            LibreLogger.d(this, "checking permission OS is less than M, ACCESS_COARSE_LOCATION permission is enabled");
            return true;
        }
        LibreLogger.d(this, "checking permission ACCESS_COARSE_LOCATION permission is not enabled");

        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void askLocationPermission() {
        if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // user checked Never Ask again
            LibreLogger.d(this, "askLocationPermission permit READ_EXTERNAL_STORAGE Denied for ever");
            // show dialog
            AlertDialog.Builder requestPermission = new AlertDialog.Builder(DeviceSettingsActivity.this);
            requestPermission.setTitle(getString(R.string.permitNotAvailable))
                    .setMessage(getString(R.string.enableStoragePermit))
                    .setPositiveButton(getString(R.string.gotoSettings), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //navigate to settings
                            alert.dismiss();
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    })
                    .setCancelable(false);
            if (alert == null) {
                alert = requestPermission.create();
            }
            if (alert != null && !alert.isShowing())
                alert.show();

            return;
        }
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_ACCESS_COARSE_LOCATION);

    }


}
