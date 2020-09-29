package com.mavid.BLE_SAC;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.danimahardhika.cafebar.CafeBar;
import com.google.android.material.textfield.TextInputLayout;
import com.mavid.adapters.WifiListBottomSheetAdapter;
import com.mavid.BLEApproach.BLEEvntBus;
import com.mavid.BLEApproach.BleCommunication;
import com.mavid.BLEApproach.BleReadInterface;
import com.mavid.BLEApproach.BleWriteInterface;
import com.mavid.BLEApproach.BluetoothLeService;
import com.mavid.BaseActivity;
import com.mavid.Constants.Constants;
import com.mavid.MavidApplication;
import com.mavid.MavidBLEConfigureSpeakerActivity;
import com.mavid.R;
import com.mavid.SAC.BLEConnectToWifiActivity;
import com.mavid.SAC.WifiYesAlexaLoginNoHomeScreen;
import com.mavid.SAC.WifiConfigurationItemClickInterface;
import com.mavid.alexa_signin.BLEYesAlexaLoginNoHomeScreen;
import com.mavid.libresdk.Constants.BundleConstants;
import com.mavid.libresdk.LibreMavidHelper;
import com.mavid.libresdk.TaskManager.Discovery.CustomExceptions.WrongStepCallException;
import com.mavid.libresdk.TaskManager.SAC.Listeners.SACListener;
import com.mavid.libresdk.Util.LibreLogger;
import com.mavid.models.ModelWifiScanList;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import ru.dimorinny.floatingtextbutton.FloatingTextButton;

import static android.util.Log.d;
import static com.mavid.Constants.Constants.WIFI_CONNECTING_NOTIFICATION;

public class BLEConfigureActivity extends BaseActivity implements View.OnClickListener,
        BleReadInterface, BleWriteInterface, WifiConfigurationItemClickInterface {

    //AdapterView.OnItemSelectedListener,
    TextInputLayout textInputPasswordLayout;

    private TextInputEditText textInputWifiPassword;
    String[] securityArray = {"NONE", "WPA2-PSK", "WPA/WPA2"};
    //    private Spinner ssidSpinner;
    private Button btn_next;

    private String passPhrase, SSID = "";
    private String SSIDValue;
    //    private TextView response, deviceName, passwordVisibility;
    private int security;
    Map<String, String> scanListMap = new TreeMap<>();
    private ArrayList<String> scanList = new ArrayList<>();
    //    LinearLayout passLyt;
    private final int GET_SCAN_LIST_HANDLER = 0x200;
    private final int CONNECT_TO_SAC = 0x300;
    ArrayAdapter securityAdapter;
    static boolean isActivityActive = false;
    private ImageView back;
    int count = 0;
    String configuratonfailedStringBle;
    private String mSACConfiguredIpAddress = "", getAlexaLoginStatus = "";
    int configuratonfailedIndexBle;
    String sacBleConnecting, finalConnectedArray, finalConnectedString, FilteredfinalIpAddress, filteredAlexaLogin;
    int sacBleConnectingIndex, finalConnectedIndex, finalIpIndez, alexaLoginIndex;

    BottomSheetDialog bottomSheetDialog;

    AppCompatImageView iv_down_arrow;

    AppCompatTextView tvSelectedWifi;

    AppCompatButton btn_cancel;

    List<ModelWifiScanList> modelWifiScanList = new ArrayList<>();

    CafeBar cafeBar;

    WifiListBottomSheetAdapter wifiListBottomSheetAdapter;

//    SwipeRefreshLayout swipe_refresh;

    TextView tv_no_data;

    RecyclerView rv_wifi_list;

    FloatingTextButton fab_refresh;

    boolean isSacTimedOutCalled = false;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
            switch (message.what) {
                case GET_SCAN_LIST_HANDLER:
                    closeLoader();
                    handler.removeMessages(GET_SCAN_LIST_HANDLER);
                    scanListFailed("Scan List", "Fetching scan list failed.Please try again",
                            BLEConfigureActivity.this);
//                    somethingWentWrong();
                    break;
            }
            if (message.what == WIFI_CONNECTING_NOTIFICATION) {
                closeLoader();

                LibreLogger.d(this, "wifi timer for every 30seconds in wifi connecting HANDLER\n");
                LibreLogger.d(this, "suma in read wifi status intervals connecting timeout for 30 seconds ");
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
                BleCommunication bleCommunication = new BleCommunication(BLEConfigureActivity.this);
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
                            " Please make sure your speaker is blinking multiple colours and then try again.", BLEConfigureActivity.this);

                }
            } else if (message.what == Constants.WIFI_CONNECTED_NOTIFICATION) {
                LibreLogger.d(this, "suma in wifi connecting event send empty msg delayed for 30 seconds two ");

            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        inItWidgets();
        isActivityActive = true;
        handler.sendEmptyMessageDelayed(GET_SCAN_LIST_HANDLER, 30000);
        showLoader("Please wait we are getting the scan list", "");
        EventBus.getDefault().register(this);
    }

    public int getDataLength(byte[] buf) {
        byte b1 = buf[3];
        byte b2 = buf[4];
        short s = (short) (b1 << 8 | b2 & 0xFF);

        LibreLogger.d(this, "Data length is returned as s" + s);
        return s;
    }

    private void inItWidgets() {
        textInputWifiPassword = findViewById(R.id.text_input_wifi_password);
        textInputPasswordLayout = findViewById(R.id.textInputPasswordLayout);

        btn_cancel = findViewById(R.id.btn_cancel);

        btn_next = findViewById(R.id.btn_next);

        iv_down_arrow = findViewById(R.id.iv_down_arrow);

        tvSelectedWifi = findViewById(R.id.tv_selected_wifi);

        fab_refresh = findViewById(R.id.fab_refresh);

        btn_next.setOnClickListener(this);

        scanList = getSSIDList();
        securityAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, scanList);
        securityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        SSID = getconnectedSSIDname(BLEConfigureActivity.this);

        //        fabRefresh = findViewById(R.id.fab_refresh);
        //        ssidSpinner.setAdapter(securityAdapter);
        //        ssidSpinner.setSelection(0);
        //        deviceName.setText(SSID);
        //        deviceName=(TextView)findViewById(R.id.devicename);

//        passLyt = findViewById(R.id.passLyt);
//        ssidSpinner = findViewById(R.id.ssidSpinner);
//        ssidSpinner.setOnItemSelectedListener(this);
//        passwordVisibility = findViewById(R.id.passwordVisibility);
//        passphraseTxt.setTransformationMethod(new PasswordTransformationMethod());
//        passwordVisibility.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                toggleVisibility(passwordVisibility);
//            }
//        });

        back = findViewById(R.id.iv_back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        iv_down_arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setupBottomSheetForWifiList();
            }
        });


        fab_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                baos.reset();
                finalPayloadLength = 0;
                scaCred = false;
                refreshSSIDList();
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }


//    private void toggleVisibility(TextView passwordVisibility) {
//        if (isTextVisible(passwordVisibility)){
//            //change button text to Hide
//            passwordVisibility.setText(getResources().getString(R.string.hide));
//            //hide password
//            passphraseTxt.setTransformationMethod(null);
//        }else{
//            //change button text to Show
//            passwordVisibility.setText(getResources().getString(R.string.show));
//            //show password
//            passphraseTxt.setTransformationMethod(new PasswordTransformationMethod());
//        }
//    }

    public boolean isTextVisible(TextView textView) {
        return textView.getText().toString().equalsIgnoreCase(getResources().getString(R.string.show));
    }


    public void setupBottomSheetForWifiList() {
        View view = getLayoutInflater().inflate(R.layout.show_wifi_list_bottom_sheet, null);
        tv_no_data = view.findViewById(R.id.tv_no_data);
        rv_wifi_list = view.findViewById(R.id.rv_wifi_list);
        AppCompatImageView iv_close_icon = view.findViewById(R.id.iv_close_icon);


//        swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                try {
//                    baos.flush();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                baos.reset();
//                finalPayloadLength = 0;
//                scaCred = false;
//
//                if (modelWifiScanList != null && wifiListBottomSheetAdapter != null) {
//                    modelWifiScanList.clear();
//                    wifiListBottomSheetAdapter.notifyDataSetChanged();
//                }
//
//                refreshSSIDList();
//
//
//            }
//        });

        for (ModelWifiScanList modelWifiScanList : modelWifiScanList) {
            Log.d("atul", "ssidName: " + modelWifiScanList.getSsid() + " rssi: " + modelWifiScanList.getRssi());
        }
        setWifiListBottomSheetAdapter();

        iv_close_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog = new BottomSheetDialog(BLEConfigureActivity.this);
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.setCancelable(false);

        bottomSheetDialog.show();

    }

//    @Override
//    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//        String selectedSSID = scanListMap.get(scanList.get(position));
//        if (selectedSSID.equalsIgnoreCase("NONE")
//                || selectedSSID.equalsIgnoreCase("OPEN")) {
//            security = 0;
//            passPhrase = "";
//            passLyt.setVisibility(View.GONE);
//        } else {
//            passLyt.setVisibility(View.VISIBLE);
//        }
//
//        if (scanListMap.get(scanList.get(position)).equalsIgnoreCase("WPA-PSK")) {
//            security = 8;
//        } else if (scanListMap.get(scanList.get(position)).equalsIgnoreCase("WPA/WPA2")) {
//            security = 4;
//        }
//    }
//
//    @Override
//    public void onNothingSelected(AdapterView<?> parent) {
//
//    }


    private boolean validate() {
      if(security!=0) {
          if (textInputWifiPassword.getText().length() > 64) {
              buildSnackBar("Password should be  less than 64 characters!");
              return false;
          }

          if (textInputWifiPassword.getText().toString().length() == 0) {
              buildSnackBar("Please enter Password!");
              return false;
          }

          if (textInputWifiPassword.getText().toString().length() < 8) {
              buildSnackBar("Password should be of minimum 8 characters!");
          }
      }
        return true;
    }


    public void buildSnackBar(String message) {
        CafeBar.Builder builder = CafeBar.builder(BLEConfigureActivity.this);
        builder.autoDismiss(true);
        builder.customView(R.layout.custom_snackbar_layout);

        cafeBar = builder.build();
        AppCompatTextView tv_message = cafeBar.getCafeBarView().findViewById(R.id.tv_message);
        tv_message.setText(message);


        cafeBar.show();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        /*startActivity(new Intent(BLEConfigureActivity.this, DeviceListFragment.class));
        finish();*/
    }

    int finalPayloadLength;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    boolean scaCred = false;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BLEEvntBus event) {
//        Toast.makeText(BLEConfigureActivity.this, ""+event.message, Toast.LENGTH_SHORT).show();

        byte[] value = event.message;
        d("Bluetooth", "Value received: scan list\n " + getDataLength(value));
        int response2 = getDataLength(value);
        d("Bluetooth", "ble event bus read bluetooth suma in finalpayload  bleconfigure activity on\n " + response2);

        if (response2 == 4) {
            d("BLEConfigureActivity", "bluetooth suma in finalpayload else in if suma in bleconfigure activity connecting\n");
            closeLoader();
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showLoader("Please Wait...", "" + sacBleConnecting.substring(sacBleConnectingIndex));

                    }
                });
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
//                    Toast.makeText(BLEConfigureActivity.this, "" + finalConnectedString, Toast.LENGTH_SHORT).show();
                    // showLoader("Configuration Successfull!", ""+finalConnectedString.substring(finalConnectedIndex));
                    // ShowAlertDynamicallyGoingToHomeScreen("Configuration Successfull!",finalConnectedString.substring(finalConnectedIndex));

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
                BleCommunication bleCommunication = new BleCommunication(BLEConfigureActivity.this);
                BleCommunication.writeInteractorStopSac();
                ShowAlertDynamicallyGoingToHomeScreen("Setup Timeout..",
                        "Please put the device to setup mode", BLEConfigureActivity.this);
                isSacTimedOutCalled = true;
            }
        }
//        if(response2==17){
//            LibreLogger.d(this,"Getting the HotSpot Status suma in gettin response value Read Device status");
//            if(value[2]==0&&value.length>6){
//                for (int i = 5; i < value.length-5; i++){
//                    LibreLogger.d(this,"suma in gettin index value"+value[i]);
//                    try {
//                       String  deviceStatus = new String(value, "UTF-8");
//                        int deviceStatusIndex=deviceStatus.indexOf("FRIENDLY_NAME");
//                        LibreLogger.d(this,"Getting the HotSpot Status suma final status\n"+deviceStatus+"clear\n"+deviceStatus.substring(deviceStatusIndex));
//
//                        LibreLogger.d(this,"Getting the HotSpot Status suma in gettin response read device status\n"+deviceStatus+"filtered string\n"+deviceStatus.substring(deviceStatusIndex));
//                       String getFinalPacketString=deviceStatus.substring(deviceStatusIndex);
//                        Scanner sc = new Scanner(getFinalPacketString);
//                        HashMap<String,String> dataMap = getDataMapFromMessage(sc);
//                        LibreLogger.d(this,"Getting the HotSpot Status dataMap value\n"+dataMap);
//
//                        if (dataMap.containsKey("FRIENDLY_NAME")) {
//                            LibreLogger.d(this,"Getting the HotSpot Status suma in gettin response read get friendlyName");
//
//                            //deviceInfo.setFwVersion(dataMap.get("FW_VERSION"));
//                        }
//                        if(dataMap.containsKey("AlexaLogin")){
//                            LibreLogger.d(this,"Getting the HotSpot Status suma in gettin response read get AlexaLogin");
//
//                            // deviceInfo.setA2dpTypeValue(dataMap.get("A2DP_TYPE"));
//                        }
//                        // getDataMapFromMessage(deviceStatus);
//                        String[] dataSplitArr = deviceStatus.substring(deviceStatusIndex).split(":");
//                        LibreLogger.d(this,"Getting the HotSpot Status device status\n"+dataSplitArr[0]+"filtered string\n"+"array 1\n"+dataSplitArr[1]+"array 2\n"+dataSplitArr[2]);
//
//
//                    } catch (UnsupportedEncodingException e) {
//                        e.printStackTrace();
//                    }
//
//                }
//            }
//        }
        if (response2 == 17) {
            handler.removeMessages(WIFI_CONNECTING_NOTIFICATION);
            handler.removeCallbacksAndMessages(null);
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
                            LibreMavidHelper.advertiseWithIp(mSACConfiguredIpAddress, BLEConfigureActivity.this);
                            LibreLogger.d(this, "Getting the ble Status getting advertising IP\n" + mSACConfiguredIpAddress);

                        } catch (WrongStepCallException e) {
                            e.printStackTrace();
                        }

                        alexaLoginIndex = deviceStatus.indexOf("AlexaLogin");
                        filteredAlexaLogin = deviceStatus.substring(alexaLoginIndex);
                        String[] splittedAlexaLogin = filteredAlexaLogin.split(":");
                        getAlexaLoginStatus = splittedAlexaLogin[1];
                        LibreLogger.d(this, "Hotspot Credential Getting the ble Status getting AlexaLogin\n" + splittedAlexaLogin[1] + "login status\n" + getAlexaLoginStatus);

                        BleCommunication bleCommunication = new BleCommunication(BLEConfigureActivity.this);
                        BleCommunication.writeInteractorStopSac();

//                        Intent intent = new Intent(BLEConfigureActivity.this, BLEYesAlexaLoginNoHomeScreen.class);
//                        intent.putExtra("IPADDRESS", mSACConfiguredIpAddress);
//                        intent.putExtra(Constants.INTENTS.ALEXA_LOGIN_STATUS, getAlexaLoginStatus);
//                        //ssidSpinner.getSelectedItem().toString().trim()
//                        intent.putExtra(BundleConstants.SACConstants.SSID, tvSelectedWifi.getText().toString());
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
                        "Please make sure Credential entered is correct and Try Again!", BLEConfigureActivity.this);
                BleCommunication.writeInteractorStopSac();
            }
        }
        if (response2 == 13) {
            closeLoader();
//            ShowAlertDynamicallyGoingToHomeScreen("Sac Not Allowed...",
//                    "Please make sure your device is in setup mode", BLEConfigureActivity.this);
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
                    if (response == 10) {
                        closeLoader();
                        if (isActivityActive) {
                            //updateResponseUI("Scan list received");
                            d("Bluetooth", "bluetooth suma in finalpayload else in if try response  do next response  10" + response);

                        }
                        getIntentParams(baos.toString());
                        d("Bluetooth", "invalid  response after 10 response\n" + baos.toString() + "baoas\n" + baos);

                        scanList = getSSIDList();
//                        securityAdapter.clear();
//
//                        securityAdapter = new ArrayAdapter(BLEConfigureActivity.this, android.R.layout.simple_spinner_item, scanList);
//                        securityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//                        ssidSpinner.setAdapter(securityAdapter);
                        d("Bluetooth", "invalid credentials inactive2" + response);
                        if (response == 0) {
                            d("Bluetooth", "sac credential ok vlaue in after 10\n" + response);

                            d("Bluetooth", "bluetooth suma in finalpayload else in if try response2 0" + response);
                            if (isActivityActive) {
                                updateResponseUI("Credentials succesfully posted.");
                            }
                            d("Bluetooth", "bluetooth suma in finalpayload else in if try response  do next2" + response);

                        }
                    }
                    if (response == 14) {
                        if (!isSacTimedOutCalled) {
                            LibreLogger.d(this, "Getting the HotSpot Status suma in gettin response value SAC timeout dialogue");
                            ShowAlertDynamicallyGoingToHomeScreen("Setup Timeout..",
                                    "Please put the device to setup mode", BLEConfigureActivity.this);
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
                        String finalSsidList= null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                            finalSsidList = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                        }
//
                      //  getIntentParams(finalSsidList);
                        getSSIDList();
                        //suma
                        d("Bluetooth", "bluetooth suma in finalpayload else in if try response  do next response scanlist getdatalengthvalue! 10" + response);
                    }

                } catch (IOException e) {
                    Log.d("BLEConfigureActivity", e.getMessage());

                }
                d("Bluetooth", "baos STEP1 " + baos);
                d("BLEConfigureActivity", "bluetooth suma in finalpayload after exception! 10" + response);

            } else {
                d("Bluetooth", "strAscii: " + new String(cd, 0, finalPayloadLength));
                d("Bluetooth", "baos STEP2 " + baos);
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
//                else if (getDataLength(value) != 10 /*&& value[2] != 1*/) {
//
//                    try {
//                        baos.write(value);
//                        String finalSsidList= null;
//                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
//                            finalSsidList = new String(baos.toByteArray(), StandardCharsets.UTF_8);
//                        }
//                        getSSIDList();
//                        //suma
//                        d("Bluetooth", "bluetooth suma in finalpayload else in if try response  do next response scanlist getdatalengthvalue! 10" + response);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                }
                else if (response == 10) {
                    closeLoader();
                    if (isActivityActive) {
                        // updateResponseUI("Scan list received");
                    }
//                    try {
//                        String finalSsidList=new String(baos.toByteArray(),"UTF-8");
//                        LibreLogger.d(this,"invalid  response 10 getting encoded scan list\n"+finalSsidList);
//                    } catch (UnsupportedEncodingException e) {
//                        e.printStackTrace();
//                    }

                    d("Bluetooth", "invalid  response 10\n" + baos.toString() + "baos\n" + baos);
//                    scanList = getSSIDList();
//                    securityAdapter.clear();

                    String finalSsidList= null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        finalSsidList = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    }
//                    input = finalSsidList.substring(finalSsidList.indexOf(" {\"Items\"")+1, finalSsidList.lastIndexOf("]}"));
//                    System.out.println(input);
//                    LibreLogger.d(this, "Swetha BT and SUMA IN SSID in READ DATA\n"+input);
//
                    getIntentParams(finalSsidList);
                    securityAdapter = new ArrayAdapter(BLEConfigureActivity.this, android.R.layout.simple_spinner_item, scanList);
                    securityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//                    ssidSpinner.setAdapter(securityAdapter);
                    d("Bluetooth", "bluetooth suma in finalpayload response 10 in else" + response);

                }

            }
        }

     /*   else{
            int response = getDataLength(value);
            Log.d("BLEConfigureActivity","get data"+response);
            if(response ==  0){
                Log.d("BLEConfigureActivity","credentials ok"+value);
                updateResponseUI("Credentials succesfully posted. Please restart the app.");
//                doNext(sacParams);
                doNext();
            }else if(response == 1){
                closeLoader();
                updateResponseUI("Invalid credentials");
                Log.d("BLEConfigureActivity","invalid credentials"+value);
            }
        }
*/


       /* LibreLogger.d(this,"Recieved scan list "+messageInfo.getMessage());
        handler.removeMessages(GET_SCAN_LIST_HANDLER);
        closeLoader();
        Intent intent = new Intent(WifiHotSpotOrSacSetupActivity.this, WifiConfigureActivity.class);
        intent.putExtra(Constants.INTENTS.SCAN_LIST,messageInfo.getMessage().toString());
        startActivity(intent);
        finish();*/
    }

    private HashMap<String, String> getDataMapFromMessage(Scanner sc) {
        HashMap<String, String> dataMap = new HashMap<>();
        while (sc.hasNext()) {
            String message = sc.nextLine().toString();
            LibreLogger.d(this, "Getting the HotSpot Status suma final status in getDataMap msg\n" + message);
            String[] dataSplitArr = message.split(":");
            LibreLogger.d(this, "suma in gettin index value splitArrayy0\n" + dataSplitArr[0] + "splitArray1\n" + dataSplitArr[1] + "splitArray2\n" + dataSplitArr[2]);
            dataMap.put(dataSplitArr[0], dataSplitArr[1]);
        }
        return dataMap;
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_next: {

                if (validate()) {
                    buildSnackBar(("Please Wait,Sending Credentials..."));
                    //showLoader(getResources().getString(R.string.notice),"Please wait...");
                   /* if(isActivityActive) {
                        showLoader(getResources().getString(R.string.pleaseWait), "");
                    }*/
                    final Bundle sacParams = new Bundle();
                    sacParams.putString(BundleConstants.SACConstants.SSID
                            , tvSelectedWifi.getText().toString());

                    sacParams.putString(BundleConstants.SACConstants.PASSPHRASE
                            , textInputWifiPassword.getText().toString());
                    sacParams.putInt(BundleConstants.SACConstants.NETWORK_TYPE
                            , security);
                    d("Bluetooth", "suma in connect btn bluetooth credential posted  Noww1!!!...");

                    LibreMavidHelper.configureBLE(sacParams, new SACListener() {
                        @Override
                        public void success() {
                            d("Bluetooth", "suma in connect btn bluetooth credential posted  Noww2!!!...");

                        }

                        @Override
                        public void failure(String message) {
                            updateResponseUI(message);
                        }

                        @Override
                        public void successBLE(byte[] b) {
                            d("Bluetooth", "suma in connect btn bluetooth credential posted  Noww3!!!...");

                            if (isActivityActive) {
                                updateResponseUI("Credentials succesfully posted.");
                            }
                            Intent intent = new Intent(BLEConfigureActivity.this, MavidBLEConfigureSpeakerActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putByteArray(BundleConstants.Constants.bleByes, b);
                            bundle.putString(BundleConstants.SACConstants.SSID, tvSelectedWifi.getText().toString());
                            intent.putExtras(bundle);
                            startActivity(intent);



                            // doNext();
                        }
                    });

                }
                break;
            }
//            case R.id.fab_refresh: {

        }

    }


//    private HashMap<String,String> getDataMapFromMessage(String sc) {
//        HashMap<String,String> dataMap = new HashMap<>();
//       // while(sc.hasNext()){
//          //  String message = sc.nextLine().toString();
//
//            String[] dataSplitArr = sc.split(":");
//            if (dataSplitArr.length < 2){
//                continue;
//            }
//            dataMap.put(dataSplitArr[0],dataSplitArr[1]);
//       // }
//        return dataMap;
//    }

    private void doNext(Bundle sacParams) {
        Intent intent = new Intent(BLEConfigureActivity.this, WifiYesAlexaLoginNoHomeScreen.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sacParams.putString(Constants.INTENTS.MACADDRESS, getMacAddress());
        intent.putExtra(Constants.INTENTS.SAC_PARAMS, sacParams);
        startActivity(intent);
        finish();
    }

//    private void sendMSearchInIntervalOfTime() {
//        mHandler.sendEmptyMessageDelayed(Constants.SEARCHING_FOR_DEVICE, 500);
//        mTaskHandlerForSendingMSearch.postDelayed(mMyTaskRunnableForMSearch, MSEARCH_TIMEOUT_SEARCH);
//        Log.d("MavidCommunication", "My task is Sending 1 Minute Once M-Search msearch interval of time");
//    }

    private void doNext() {
        Intent intent = new Intent(BLEConfigureActivity.this, BLEConnectToWifiActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.INTENTS.MACADDRESS, getMacAddress());
//        intent.putExtra(BundleConstants.SACConstants.SSID, ssidSpinner.getSelectedItem().toString().trim());
        intent.putExtra(BundleConstants.SACConstants.PASSPHRASE, textInputWifiPassword.getText().toString());
        intent.putExtra(BundleConstants.SACConstants.NETWORK_TYPE, 8);
        startActivity(intent);
        finish();
    }

    private void refreshSSIDList() {
        showLoader("Please wait we are getting the scan list", "");
        handler.sendEmptyMessageDelayed(GET_SCAN_LIST_HANDLER, 30000);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BleCommunication bleCommunication = new BleCommunication(BLEConfigureActivity.this);
                BleCommunication.writeInteractor();
            }
        });

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

    public void getIntentParams(String scanList) {
//        String scanList = getIntent().getStringExtra(Constants.INTENTS.SCAN_LIST);
        populateScanlistMap(scanList);
    }

    public void setWifiListBottomSheetAdapter() {

        Collections.sort(modelWifiScanList, new Comparator<ModelWifiScanList>() {
            @Override
            public int compare(ModelWifiScanList modelWifiScanList, ModelWifiScanList modelWifiScanList1) {
                //sorting via rssi values
                return Integer.parseInt(modelWifiScanList1.getRssi()) - Integer.parseInt(modelWifiScanList.getRssi());
            }
        });
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(BLEConfigureActivity.this);
        wifiListBottomSheetAdapter = new WifiListBottomSheetAdapter(BLEConfigureActivity.this, modelWifiScanList);

        rv_wifi_list.setAdapter(wifiListBottomSheetAdapter);
        wifiListBottomSheetAdapter.setWifiConfigurationItemClickInterface(this);
        rv_wifi_list.setLayoutManager(linearLayoutManager);
//        swipe_refresh.setRefreshing(false);
        if (modelWifiScanList.size() > 0) {
            tv_no_data.setVisibility(View.GONE);

        } else {
            tv_no_data.setVisibility(View.VISIBLE);
        }
    }

    public void populateScanlistMap(final String scanList) {
        scanListMap.clear();
        modelWifiScanList = new ArrayList<>();

        try {
            JSONObject mainObj = new JSONObject(scanList);
            JSONArray scanListArray = mainObj.getJSONArray("Items");
            for (int i = 0; i < scanListArray.length(); i++) {
                JSONObject obj = (JSONObject) scanListArray.get(i);
                if (obj.getString("SSID") == null
                        || (obj.getString("SSID").isEmpty())|obj.getString("Security") == null||obj.getString("Security").isEmpty()||obj.getString("RSSI") == null||obj.getString("RSSI").isEmpty()) {
                    continue;
                }
                scanListMap.put(obj.getString("SSID"), obj.getString("Security"));
                modelWifiScanList.add(new ModelWifiScanList(obj.getString("SSID")
                        , obj.getString("Security"),
                        obj.getString("RSSI")));
            }
            if (bottomSheetDialog != null) {
                if (bottomSheetDialog.isShowing()) {
                    setWifiListBottomSheetAdapter();
                }
            }
            if (modelWifiScanList.size() > 0) {
                iv_down_arrow.setVisibility(View.VISIBLE);
            } else {
                iv_down_arrow.setVisibility(View.INVISIBLE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            LibreLogger.d(this,"suma in exception logs "+e);
//            BluetoothLeService.mBluetoothGatt.disconnect();
//            BluetoothLeService.mBluetoothGatt.close();
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

    public String getMacAddress() {
        String ssid = getconnectedSSIDname(BLEConfigureActivity.this);
        ssid = ssid.substring(ssid.length() - 5);
        return ssid;
    }

    @Override
    public void onReadSuccess(byte[] data) {
        d("onReadSccBLEConfigure: ", "data : " + data);
    }


    @Override
    public void onWriteSuccess() {

    }

    @Override
    public void onWriteFailure() {

    }

//    @Override
//    public void onStart() {
//        super.onStart();
//        EventBus.getDefault().register(this);
//    }

    @Override
    public void onDestroy() {
        // isActivityActive = false;
        //  EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onItemClicked(int pos) {
        if (bottomSheetDialog != null) {
            bottomSheetDialog.dismiss();
        }

        tvSelectedWifi.setText(modelWifiScanList.get(pos).getSsid());

//        switch (modelWifiScanList.get(pos).getSecurity()) {
//
//            default:
//
//            case "NONE":
//
//            case "OPEN":
//                security = 0;
//
//                break;
//
//
//            case "WPA-PSK":
//                security = 8;
//                break;
//
//            case "WPA/WPA2":
//                security = 4;
//                break;
//
//        }
//    }
        switch (modelWifiScanList.get(pos).getSecurity()) {

            default:

            case "NONE":

            case "OPEN":
                security = 0;
                textInputPasswordLayout.setVisibility(View.GONE);
                break;


            case "WPA-PSK":
                security = 8;
                textInputPasswordLayout.setVisibility(View.VISIBLE);
                break;

            case "WPA/WPA2":
                security = 4;
                textInputPasswordLayout.setVisibility(View.VISIBLE);
                break;

        }
    }
}
