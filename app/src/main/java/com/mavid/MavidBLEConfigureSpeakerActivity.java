package com.mavid;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.danimahardhika.cafebar.CafeBar;
import com.mavid.BLEApproach.BLEEvntBus;
import com.mavid.BLEApproach.BleCommunication;
import com.mavid.BLEApproach.BleReadInterface;
import com.mavid.BLEApproach.BleWriteInterface;
import com.mavid.Constants.Constants;
import com.mavid.alexa_signin.AlexaSignInActivity;
import com.mavid.alexa_signin.AlexaThingsToTryDoneActivity;
import com.mavid.alexa_signin.BLEYesAlexaLoginNoHomeScreen;
import com.mavid.libresdk.Constants.BundleConstants;
import com.mavid.libresdk.LibreMavidHelper;
import com.mavid.libresdk.TaskManager.Communication.Listeners.CommandStatusListenerWithResponse;
import com.mavid.libresdk.TaskManager.Discovery.CustomExceptions.WrongStepCallException;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.MessageInfo;
import com.mavid.libresdk.Util.LibreLogger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static android.util.Log.d;
import static com.mavid.Constants.Constants.TIMEOUT_FOR_SEARCHING_DEVICE;
import static com.mavid.Constants.Constants.WIFI_CONNECTING_NOTIFICATION;


public class MavidBLEConfigureSpeakerActivity extends BaseActivity implements BleReadInterface, BleWriteInterface {

    Map<String, String> scanListMap = new TreeMap<>();

    int finalIpIndez, alexaLoginIndex;
    String FilteredfinalIpAddress, filteredAlexaLogin;
    private String mSACConfiguredIpAddress = "", getAlexaLoginStatus = "";
    String sacBleConnecting;
    int count = 0, sacBleConnectingIndex;

    private final int GET_SCAN_LIST_HANDLER = 0x200;

    String finalConnectedArray, finalConnectedString;

    int finalConnectedIndex;

    boolean isSacTimedOutCalled = false;

    String configuratonfailedStringBle;

    int configuratonfailedIndexBle;

    boolean scaCred = false;

    static boolean isActivityActive = false;

    CafeBar cafeBar;

    int finalPayloadLength;

    private boolean alreadyDialogueShown = false;

    boolean isRefreshedAlexaToken = false;

    String sacSsid;
    Bundle bundle;
    byte[] b;
    private Dialog alert;

    CountDownTimer countDownTimer;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
            switch (message.what) {
                case GET_SCAN_LIST_HANDLER:
                    handler.removeMessages(GET_SCAN_LIST_HANDLER);
//                    scanListFailed("Scan List", "Fetching scan list failed.Please try again", BLEConfigureActivity.this);
//                    somethingWentWrong();
                    break;
            }
            if (message.what == WIFI_CONNECTING_NOTIFICATION) {

                LibreLogger.d(this, "wifi timer for every 30seconds in wifi connecting HANDLER\n");
                LibreLogger.d(this, "suma in read wifi status intervals connecting timeout for 30 seconds ");
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
                BleCommunication bleCommunication = new BleCommunication(MavidBLEConfigureSpeakerActivity.this);
                BleCommunication.writeInteractorReadWifiStatus();
                LibreLogger.d(this, "wifi timer for every 30seconds write interactor reas wifi status");
                if (MavidApplication.readWifiStatus == true) {
                    LibreLogger.d(this, "suma in wifi connecting write interactor reas wifi status success");
                } else {
                    LibreLogger.d(this, "suma in wifi connecting write interactor reas wifi status failure");


                }
//                    }
//                });
                LibreLogger.d(this, "wifi timer for every 30seconds getting count \n" + count);

                if (count == 5 || MavidApplication.readWifiStatusNotificationBLE) {
                    LibreLogger.d(this, "wifi timer for every 30seconds getting count \n");
//                    bleCommunication = new BleCommunication(BLEConfigureActivity.this);
//                    BleCommunication.writeInteractorStopSac();
                    ShowAlertDynamicallyGoingToHomeScreen("Configuration failed",
                            " Please make sure your speaker is blinking multiple colours and then try again.", MavidBLEConfigureSpeakerActivity.this);

                }
            } else if (message.what == Constants.WIFI_CONNECTED_NOTIFICATION) {
                LibreLogger.d(this, "suma in wifi connecting event send empty msg delayed for 30 seconds two ");

            } else if ((message.what == TIMEOUT_FOR_SEARCHING_DEVICE)) {
                handler.removeMessages(TIMEOUT_FOR_SEARCHING_DEVICE);
                showDialogifDeviceNotFound(getString(R.string.noDeviceFound));
                LibreLogger.d(this, "suma in connectTowifi nodevicefound");

            }

        }
    };


    private void showDialogifDeviceNotFound(final String Message) {
        if (!MavidBLEConfigureSpeakerActivity.this.isFinishing()) {

            alreadyDialogueShown = true;

            alert = new Dialog(MavidBLEConfigureSpeakerActivity.this);

            alert.requestWindowFeature(Window.FEATURE_NO_TITLE);

            alert.setContentView(R.layout.custom_single_button_layout);

            alert.setCancelable(false);


            tv_alert_title = alert.findViewById(R.id.tv_alert_title);

            tv_alert_message = alert.findViewById(R.id.tv_alert_message);

            btn_ok = alert.findViewById(R.id.btn_ok);


            tv_alert_title.setText("");

            tv_alert_message.setText(Message);

            btn_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alert.dismiss();
                    alert = null;
                    if (Message.contains(getString(R.string.noDeviceFound))) {
                        Intent ssid = new Intent(MavidBLEConfigureSpeakerActivity.this,
                                MavidHomeTabsActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(ssid);
                        finish();
                        return;
                    }
                }
            });

            if (alert != null) {
                if (!alert.isShowing()) {
                    alert.show();
                }
            }
        }
    }

    public int getDataLength(byte[] buf) {
        byte b1 = buf[3];
        byte b2 = buf[4];
        short s = (short) (b1 << 8 | b2 & 0xFF);

        LibreLogger.d(this, "Data length is returned as s" + s);
        return s;
    }


    private void getDataLengthString(byte[] value) {
        if (value[2] == 0 && value.length > 6) {
            LibreLogger.d(this, "suma in data length\n" + value.length);
            for (int i = 5; i < value.length - 5; i++) {
                LibreLogger.d(this, "suma in gettin index value" + value[i]);
                try {
                    String finalConnecting = new String(value, "UTF-8");
                    int finalConnectingIndex = finalConnecting.indexOf("Connecting");
                    LibreLogger.d(this, "suma wifi connecting only splitted array string\n" + finalConnecting.substring(finalConnectingIndex));

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mavid_configure_speaker_activity);
        bundle = new Bundle();
        bundle = getIntent().getExtras();
        if (bundle != null) {
            b = bundle.getByteArray(BundleConstants.Constants.bleByes);
            sacSsid = bundle.getString(BundleConstants.SACConstants.SSID, "");
        }

        disableNetworkChangeCallBack();
        disableNetworkOffCallBack();
        handler.sendEmptyMessageDelayed(GET_SCAN_LIST_HANDLER, 30000);
        isActivityActive = true;
        EventBus.getDefault().register(this);

        BleCommunication bleCommunication = new BleCommunication(MavidBLEConfigureSpeakerActivity.this);
        BleCommunication.writeInteractor(b);


        countDownTimer = new CountDownTimer(45000, 1000) {

            public void onTick(long millisUntilFinished) {
                //here you can have your logic to set text to edittext
                Log.d("atul_countdown", String.valueOf(millisUntilFinished));
            }

            public void onFinish() {
                Log.d("atul_countdown", "done");
                //show a message
                //configuration sucessful
                showConfigurationSucessfulMessage();

            }

        }.start();

    }

    public void showConfigurationSucessfulMessage() {
        if (!MavidBLEConfigureSpeakerActivity.this.isFinishing()) {

            alert = new Dialog(MavidBLEConfigureSpeakerActivity.this);

            alert.requestWindowFeature(Window.FEATURE_NO_TITLE);

            alert.setContentView(R.layout.custom_single_button_layout);

            alert.setCancelable(false);


            tv_alert_title = alert.findViewById(R.id.tv_alert_title);

            tv_alert_message = alert.findViewById(R.id.tv_alert_message);

            btn_ok = alert.findViewById(R.id.btn_ok);


            tv_alert_title.setText("");

            tv_alert_message.setText("Configuration Successful");

            btn_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alert.dismiss();
                    Intent ssid = new Intent(MavidBLEConfigureSpeakerActivity.this,
                            MavidHomeTabsActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(ssid);
                    finish();
                }
            });

            alert.show();
        }

    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BLEEvntBus event) {
        byte[] value = event.message;

        int response2 = getDataLength(value);


        if (response2 == 4) {
            d("BLEConfigureActivity", "bluetooth suma in finalpayload else in if suma in bleconfigure activity connecting\n");

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    count++;
                    if (count < 6) {
                        handler.sendEmptyMessage(WIFI_CONNECTING_NOTIFICATION);
                        LibreLogger.d(this, "wifi timer for every 30seconds" + count);
                        handler.postDelayed(this, 30000);
                    }
                }
            }, 30000);

            if (value[2] == 0 && value.length > 6) {
                LibreLogger.d(this, "suma in data length\n" + value.length);
                for (int i = 5; i < value.length - 5; i++) {
                    LibreLogger.d(this, "suma in gettin index value" + value[i]);
                    try {
                        sacBleConnecting = new String(value, "UTF-8");
                        sacBleConnectingIndex = sacBleConnecting.indexOf("Connecting");
                        LibreLogger.d(this, "suma wifi connecting only splitted array string\n" + sacBleConnecting.substring(sacBleConnectingIndex));

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        showLoader("Please Wait...", "" + sacBleConnecting.substring(sacBleConnectingIndex));
//
//                    }
//                });


            }

        }


        if (response2 == 5) {
            d("BLEConfigureActivity", "bluetooth suma in finalpayload else in if suma in bleconfigure activity connected\n" + response2);
            //closeLoader();
            handler.removeMessages(WIFI_CONNECTING_NOTIFICATION);
            handler.removeCallbacksAndMessages(null);

            LibreLogger.d(this, "suma in wifi connecting close loader 5");
            if (value[2] == 0 && value.length > 6) {
                for (int i = 5; i < value.length - 5; i++) {
                    try {
                        finalConnectedArray = new String(value, "UTF-8");
                        finalConnectedIndex = finalConnectedArray.indexOf("Conn");
                        finalConnectedString = finalConnectedArray.substring(finalConnectedIndex);
                        LibreLogger.d(this, "wifi timer for every 30seconds connected status\n" + finalConnectedArray.substring(finalConnectedIndex));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateResponseUI(finalConnectedString);

                }
            });
            BleCommunication.writeInteractorDeviceStatus();
            try {
                LibreMavidHelper.advertise();
            } catch (WrongStepCallException e) {
                e.printStackTrace();
            }
            //doNext();
//            //StopSAC
//            BleCommunication bleCommunication = new BleCommunication(BLEConfigureActivity.this);
//            BleCommunication.writeInteractorStopSac();


        }

        if (response2 == 14) {
            closeLoader();
            LibreLogger.d(this, "Getting the HotSpot Status suma in gettin response value SAC timeout dialogue");
            //StopSAC
            if (!isSacTimedOutCalled) {
                BleCommunication bleCommunication = new BleCommunication(MavidBLEConfigureSpeakerActivity.this);
                BleCommunication.writeInteractorStopSac();
                ShowAlertDynamicallyGoingToHomeScreen("Setup Timeout..",
                        "Please put the device to setup mode", MavidBLEConfigureSpeakerActivity.this);
                isSacTimedOutCalled = true;
            }
        }


        if (response2 == 17) {
//                handler.removeMessages(WIFI_CONNECTING_NOTIFICATION);
//                handler.removeCallbacksAndMessages(null);
            LibreLogger.d(this, "Getting the ble Status suma in gettin response value Read Device status");
            if (value[2] == 0 && value.length > 6) {
                for (int i = 5; i < value.length - 5; i++) {
                    LibreLogger.d(this, "suma in gettin index value in 17\n" + value[i]);
                    try {
                        String deviceStatus = new String(value, "UTF-8");
                        LibreLogger.d(this, "Getting the ble Status getting device status\n" + deviceStatus);

                        finalIpIndez = deviceStatus.indexOf("IpAdd:");
                        LibreLogger.d(this, "Getting the ble Status getting ipAddress\n" + deviceStatus.substring(finalIpIndez));
                        FilteredfinalIpAddress = deviceStatus.substring(finalIpIndez);
                        String[] splittedIpValue = FilteredfinalIpAddress.split(":");
                        mSACConfiguredIpAddress = splittedIpValue[1];

                        // MavidApplication.broadCastAddress=mSACConfiguredIpAddress;

                        LibreLogger.d(this, "Getting the ble Status getting ipAddress index 1\n" + splittedIpValue[1] + "mSacConfiguredIp" + mSACConfiguredIpAddress);
                        try {
                            LibreMavidHelper.advertiseWithIp(mSACConfiguredIpAddress, MavidBLEConfigureSpeakerActivity.this);
                            LibreLogger.d(this, "Getting the ble Status getting advertising IP\n" + mSACConfiguredIpAddress);

                        } catch (WrongStepCallException e) {
                            e.printStackTrace();
                        }

                        alexaLoginIndex = deviceStatus.indexOf("AlexaLogin");
                        filteredAlexaLogin = deviceStatus.substring(alexaLoginIndex);
                        String[] splittedAlexaLogin = filteredAlexaLogin.split(":");
                        getAlexaLoginStatus = splittedAlexaLogin[1];
                        LibreLogger.d(this, "Hotspot Credential Getting the ble Status getting AlexaLogin\n" + splittedAlexaLogin[1] + "login status\n" + getAlexaLoginStatus);

                        BleCommunication bleCommunication = new BleCommunication(MavidBLEConfigureSpeakerActivity.this);
                        BleCommunication.writeInteractorStopSac();

                        Log.d("atul_before_alexa_token", "called");
                        checkForAlexaToken(mSACConfiguredIpAddress, getAlexaLoginStatus);
//                        Intent intent = new Intent(MavidBLEConfigureSpeakerActivity.this, BLEYesAlexaLoginNoHomeScreen.class);
//                        intent.putExtra("IPADDRESS", mSACConfiguredIpAddress);
//                        intent.putExtra(Constants.INTENTS.ALEXA_LOGIN_STATUS, getAlexaLoginStatus);
//                        //ssidSpinner.getSelectedItem().toString().trim()
//                        intent.putExtra(BundleConstants.SACConstants.SSID, sacSsid);
//                        startActivity(intent);

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
        if (response2 == 15) {
            closeLoader();
            handler.removeCallbacksAndMessages(WIFI_CONNECTING_NOTIFICATION);
            handler.removeCallbacksAndMessages(null);
            LibreLogger.d(this, "wifi timer for every 30seconds in read wifi status Notification TWO 15\n");
            if (value[2] == 0 && value.length > 6) {
                for (int i = 5; i < value.length - 5; i++) {
                    LibreLogger.d(this, "suma in gettin index value" + value[i]);
                    try {
                        configuratonfailedStringBle = new String(value, "UTF-8");
                        configuratonfailedIndexBle = configuratonfailedStringBle.indexOf("Configuration");

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                }
                MavidApplication.readWifiStatusNotificationBLE = true;
                closeLoader();
//                BleCommunication bleCommunication = new BleCommunication(BLEConfigureActivity.this);
//                bleCommunication = new BleCommunication(BLEConfigureActivity.this);
                ShowAlertDynamicallyGoingToHomeScreen(configuratonfailedStringBle.substring(configuratonfailedIndexBle),
                        "Please make sure Credential entered is correct and Try Again!", MavidBLEConfigureSpeakerActivity.this);
                BleCommunication.writeInteractorStopSac();
            }
        }

        if (response2 == 13) {
            closeLoader();
//            ShowAlertDynamicallyGoingToHomeScreen("Sac Not Allowed...",
//                    "Please make sure your device is in setup mode", MavidBLEConfigureSpeakerActivity.this);
        }
        if (response2 == 16) {
            LibreLogger.d(this, "Getting the HotSpot Status suma in gettin response value SAC connection wifi failed");
            // ShowAlertDynamicallyGoingToHomeScreen("Setup Timeout..","Please put the device to setup mode");
        }
        if (!scaCred) {
            if (value[2] == 1) {
                scaCred = true;
                finalPayloadLength = getDataLength(value);
                d("Bluetooth", "finalPayloadLength size: " + finalPayloadLength);
                d("Bluetooth", "bluetooth suma in finalpayload" + finalPayloadLength);
                LibreLogger.d(this, "suma check the condition in sacred if ");
            }
        } else {
            byte[] cd = new byte[finalPayloadLength];
            LibreLogger.d(this, "suma check the condition in sacred else");
            if (value[2] == 0 && value.length > 6) {
                LibreLogger.d(this, "suma in data length\n" + value.length);
                getDataLengthString(value);

            }
            if (baos.size() <= finalPayloadLength) {
                int response = getDataLength(value);
                d("Bluetooth", "finalPayloadLength size: get response " + finalPayloadLength + "getting baos size\n" + baos.size());
                d("Bluetooth", "bluetooth suma in finalpayload else in if suma in bleconfigure activity" + response);
                LibreLogger.d(this, "suma check the condition in sacred else one more if check baos length if");

                try {
                    if (response == 0) {
                        d("Bluetooth", "sac credential ok vlaue in less than size baos\n" + response);
                        showLoader("", "Waiting for device response...");
                    }
                    if (response == 1) {
                        closeLoader();
                        if (isActivityActive) {
                            updateResponseUI("Invalid credentials");
                            d("Bluetooth", "bluetooth suma in finalpayload else in if try response  do next response 1" + response);
                        }
                        d("Bluetooth", "invalid credentials" + response);
                    }


                    //commented now need to check
//                    if (response == 10) {
//                        closeLoader();
//                        if (isActivityActive) {
//                            //updateResponseUI("Scan list received");
//                            d("Bluetooth", "bluetooth suma in finalpayload else in if try response  do next response  10" + response);
//
//                        }
//                        getIntentParams(baos.toString());
//                        d("Bluetooth", "invalid  response after 10 response\n" + baos.toString() + "baoas\n" + baos);
//
//                        scanList = getSSIDList();
////                        securityAdapter.clear();
////
////                        securityAdapter = new ArrayAdapter(BLEConfigureActivity.this, android.R.layout.simple_spinner_item, scanList);
////                        securityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
////                        ssidSpinner.setAdapter(securityAdapter);
//                        d("Bluetooth", "invalid credentials inactive2" + response);
//                        if (response == 0) {
//                            d("Bluetooth", "sac credential ok vlaue in after 10\n" + response);
//
//                            d("Bluetooth", "bluetooth suma in finalpayload else in if try response2 0" + response);
//                            if (isActivityActive) {
//                                updateResponseUI("Credentials succesfully posted.");
//                            }
//                            d("Bluetooth", "bluetooth suma in finalpayload else in if try response  do next2" + response);
//
//                        }
//                    }


                    if (response == 14) {
                        if (!isSacTimedOutCalled) {
                            LibreLogger.d(this, "Getting the HotSpot Status suma in gettin response value SAC timeout dialogue");
                            ShowAlertDynamicallyGoingToHomeScreen("Setup Timeout..",
                                    "Please put the device to setup mode", MavidBLEConfigureSpeakerActivity.this);
                            isSacTimedOutCalled = true;
                        }
                    }
//                    if(response==12){
//                        if(value[2]==0&&value.length>6){
//                            for (int i = 5; i < value.length-5; i++){
//                                LibreLogger.d(this,"suma in gettin index value"+value[i]);
//                                try {
//
////                                    configuratonfailedStringBle = new String(value, "UTF-8");
////                                    configuratonfailedIndexBle=configuratonfailedStringBle.indexOf(\"Configuration");
//
//                                } catch (UnsupportedEncodingException e) {
//                                    e.printStackTrace();
//                                }
//
//                            }
//                            MavidApplication.readWifiStatusNotificationBLE=true;
//                            LibreLogger.d(this,"wifi timer for every 30seconds in read wifi status Notification 15\n");
//
//                        }
//                    }

                    if (response == 15) {
                        if (value[2] == 0 && value.length > 6) {
                            for (int i = 5; i < value.length - 5; i++) {
                                LibreLogger.d(this, "suma in gettin index value" + value[i]);
                                try {
                                    configuratonfailedStringBle = new String(value, "UTF-8");
                                    configuratonfailedIndexBle = configuratonfailedStringBle.indexOf("Configuration");

                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }

                            }
                            MavidApplication.readWifiStatusNotificationBLE = true;
                            LibreLogger.d(this, "wifi timer for every 30seconds in read wifi status Notification 15\n");

                        }
                    } else if (getDataLength(value) != 10 /*&& value[2] != 1*/) {

                        baos.write(value);
                        //commnedted need to check
//                        getSSIDList();
                        //suma
                        d("Bluetooth", "bluetooth suma in finalpayload else in if try response  do next response scanlist getdatalengthvalue! 10" + response);
                    }

                } catch (IOException e) {
                    Log.d("BLEConfigureActivity", e.getMessage());
                }
                d("Bluetooth", "baos " + baos);
                d("BLEConfigureActivity", "bluetooth suma in finalpayload after exception! 10" + response);

            } else {
                d("Bluetooth", "strAscii: " + new String(cd, 0, finalPayloadLength));
                d("Bluetooth", "baos " + baos);
                d("Bluetooth", "baos.size(): " + baos.size());
                int response = getDataLength(value);
                LibreLogger.d(this, "suma check the condition in sacred else one more if check baos length else");

                d("Bluetooth", "bluetooth suma in finalpayload response 0  in else getdatalength" + response);
                if (response == 0) {
                    d("Bluetooth", "sac credential ok vlaue else part\n" + response);

                    d("BLEConfigureActivity", "credentials ok" + value);
                    if (isActivityActive) {
                        updateResponseUI("Credentials succesfully posted.");
                    }
                    d("Bluetooth", "credentials ok next value do next3\n" + response);

                } else if (response == 1) {
                    closeLoader();
                    if (isActivityActive) {
                        updateResponseUI("Invalid credentials");
                    }
                    d("Bluetooth", "bluetooth suma in finalpayload response 1 in else" + response);

                }


//                else if (response == 10) {
//
//                    closeLoader();
//                    if (isActivityActive) {
//                        // updateResponseUI("Scan list received");
//                    }
////                    try {
////                        String finalSsidList=new String(baos.toByteArray(),"UTF-8");
////                        LibreLogger.d(this,"invalid  response 10 getting encoded scan list\n"+finalSsidList);
////                    } catch (UnsupportedEncodingException e) {
////                        e.printStackTrace();
////                    }
//
//                    d("Bluetooth", "invalid  response 10\n" + baos.toString() + "baos\n" + baos);
//                    scanList = getSSIDList();
//                    securityAdapter.clear();
//                    securityAdapter = new ArrayAdapter(BLEConfigureActivity.this, android.R.layout.simple_spinner_item, scanList);
//                    securityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
////                    ssidSpinner.setAdapter(securityAdapter);
//                    d("Bluetooth", "bluetooth suma in finalpayload response 10 in else" + response);
//
//                }

            }
        }
    }


    public ArrayList<String> getSSIDList() {
        ArrayList<String> scanList = new ArrayList<>();
        Set<String> keySet = scanListMap.keySet();
        for (String ssid : keySet) {
//             modelWifiScanList.add(new ModelWifiScanList())
            scanList.add(ssid);
        }
        handler.removeMessages(GET_SCAN_LIST_HANDLER);
        return scanList;
    }

    private void updateResponseUI(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //closeLoader();
                buildSnackBar(message);
//                Toast.makeText(BLEConfigureActivity.this, "" + message, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void checkForAlexaToken(String mSACConfiguredIpAddress, final String alexaLoginStatus) {
        LibreMavidHelper.askRefreshToken(mSACConfiguredIpAddress, new CommandStatusListenerWithResponse() {
            @Override
            public void response(MessageInfo messageInfo) {
                String messages = messageInfo.getMessage();
                Log.d("atul_after_alexa_token", "called");
                if (!isRefreshedAlexaToken) {
                    isRefreshedAlexaToken = true;
                    Log.d("DeviceManager", " got alexa token " + messages);
                    handleAlexaRefreshTokenStatus(messageInfo.getIpAddressOfSender(), messageInfo.getMessage(), alexaLoginStatus);
                }
            }

            @Override
            public void failure(Exception e) {

                Log.d("alexa_ex", e.toString());
            }

            @Override
            public void success() {

            }
        });
    }

    private void handleAlexaRefreshTokenStatus(String current_ipaddress, String refreshToken, String alexaLoginStatus) {
        countDownTimer.cancel();
        if (refreshToken != null && !refreshToken.isEmpty() && alexaLoginStatus != null && alexaLoginStatus.contains("Yes")) {
            /*not logged in*/
            Intent i = new Intent(MavidBLEConfigureSpeakerActivity.this, AlexaThingsToTryDoneActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("speakerIpaddress", current_ipaddress);
            i.putExtra("fromActivity", MavidHomeTabsActivity.class.getSimpleName());
            startActivity(i);
            finish();
        } else {
            Intent newIntent = new Intent(MavidBLEConfigureSpeakerActivity.this, AlexaSignInActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            newIntent.putExtra("speakerIpaddress", current_ipaddress);
            newIntent.putExtra("fromActivity", MavidHomeTabsActivity.class.getSimpleName());
            startActivity(newIntent);
            finish();
        }
    }

    public void buildSnackBar(String message) {
        CafeBar.Builder builder = CafeBar.builder(MavidBLEConfigureSpeakerActivity.this);
        builder.autoDismiss(true);
        builder.customView(R.layout.custom_snackbar_layout);

        cafeBar = builder.build();
        AppCompatTextView tv_message = cafeBar.getCafeBarView().findViewById(R.id.tv_message);
        tv_message.setText(message);


        cafeBar.show();
    }

    @Override
    public void onReadSuccess(byte[] data) throws IOException {
        d("onReadSccBLEConfigure: ", "data : " + data);
    }

    @Override
    public void onWriteSuccess() {

    }

    @Override
    public void onWriteFailure() {

    }
}
