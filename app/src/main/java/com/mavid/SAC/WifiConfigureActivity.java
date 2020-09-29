package com.mavid.SAC;

import android.app.ProgressDialog;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.danimahardhika.cafebar.CafeBar;
import com.google.android.material.textfield.TextInputLayout;
import com.mavid.adapters.WifiListBottomSheetAdapter;
import com.mavid.BaseActivity;
import com.mavid.Constants.Constants;
import com.mavid.MavidHomeTabsActivity;
import com.mavid.MavidWifiConfigureSpeakerActivity;
import com.mavid.R;

import com.mavid.libresdk.Constants.BundleConstants;
import com.mavid.libresdk.LibreMavidHelper;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.MessageInfo;
import com.mavid.libresdk.TaskManager.SAC.Listeners.SACListener;
import com.mavid.libresdk.TaskManager.SAC.Listeners.SACListenerWithResponse;
import com.mavid.libresdk.Util.LibreLogger;
import com.mavid.models.ModelWifiScanList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ru.dimorinny.floatingtextbutton.FloatingTextButton;

public class WifiConfigureActivity extends BaseActivity implements View.OnClickListener, WifiConfigurationItemClickInterface {

    //AdapterView.OnItemSelectedListener,

    //    private EditText passphraseTxt;
    String[] securityArray = {"NONE", "WPA2-PSK", "WPA/WPA2"};
    //    private Spinner  ssidSpinner;
//    private Button connectBtn;
//    Button refresh;
    private String passPhrase, SSID = "";
    private String SSIDValue;
    private int security;
    Map<String, String> scanListMap = new TreeMap<>();
    private ArrayList<String> scanList = new ArrayList<>();
    LinearLayout passLyt;
    private final int GET_SCAN_LIST_HANDLER = 0x200;
    private final int CONNECT_TO_SAC = 0x300;
    ArrayAdapter securityAdapter;

    TextInputEditText text_input_wifi_password;

    List<ModelWifiScanList> modelWifiScanList = new ArrayList<>();

    WifiListBottomSheetAdapter wifiListBottomSheetAdapter;

    BottomSheetDialog bottomSheetDialog;

    AppCompatImageView iv_down_arrow;

//    SwipeRefreshLayout swipe_refresh;

    RecyclerView rv_wifi_list;

    TextView tv_no_data;

    TextView tv_selected_wifi;

    CafeBar cafeBar;

    AppCompatButton btn_next, btn_cancel;

    FloatingTextButton fab_refresh;

    AppCompatImageView iv_back;

    TextInputLayout textInputPasswordLayout;

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case GET_SCAN_LIST_HANDLER:
                    closeLoader();
//                    somethingWentWrong(WifiConfigureActivity.this);
                    ShowAlertDynamicallyGoingToHomeScreen("Scan List", "Fetching scan list failed.Please try again", WifiConfigureActivity.this);
                    break;
            }
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        inItWidgets();
        getIntentParams();

    }

    private void inItWidgets() {


        passLyt = (LinearLayout) findViewById(R.id.passLyt);

        text_input_wifi_password = findViewById(R.id.text_input_wifi_password);

        tv_selected_wifi = findViewById(R.id.tv_selected_wifi);

        iv_down_arrow = findViewById(R.id.iv_down_arrow);

        btn_next = findViewById(R.id.btn_next);

        btn_cancel = findViewById(R.id.btn_cancel);

        iv_back = findViewById(R.id.iv_back);

        fab_refresh = findViewById(R.id.fab_refresh);

        SSID = getconnectedSSIDname(WifiConfigureActivity.this);

        textInputPasswordLayout = findViewById(R.id.textInputPasswordLayout);

        btn_next.setOnClickListener(this);

        btn_cancel.setOnClickListener(this);

        iv_down_arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setupBottomSheetForWifiList();
            }
        });

        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        fab_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshSSIDList();
            }
        });
    }


    public boolean isTextVisible(TextView textView) {
        if (textView.getText().toString().equalsIgnoreCase(getResources().getString(R.string.show))) {
            return true;
        }
        return false;
    }


    public void buildSnackBar(String message) {
        CafeBar.Builder builder = CafeBar.builder(WifiConfigureActivity.this);
        builder.autoDismiss(true);
        builder.customView(R.layout.custom_snackbar_layout);

        cafeBar = builder.build();
        AppCompatTextView tv_message = cafeBar.getCafeBarView().findViewById(R.id.tv_message);
        tv_message.setText(message);


        cafeBar.show();
    }

    private boolean validate() {
        if (textInputPasswordLayout.getVisibility() == View.VISIBLE) {
            if (text_input_wifi_password.getText().length() > 64) {
                buildSnackBar("Password should be  less than 64 characters!");
                return false;
            }

            if (text_input_wifi_password.getText().toString().length() == 0) {
                buildSnackBar("Please enter Password!");
                return false;
            }

            if (text_input_wifi_password.getText().toString().length() < 8) {
                buildSnackBar("Password should be of minimum 8 characters!");
                return false;
            }
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(WifiConfigureActivity.this, MavidHomeTabsActivity.class));
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_next:
                if (!tv_selected_wifi.getText().toString().equals("Select a Wi-fi")) {
                    //wifi is selected from the user - > check for password
                    if (validate()) {
                        buildSnackBar("Please Wait,Sending Credentials...");
                        //showLoader(getResources().getString(R.string.notice),"Please wait...");
                        showLoader(getResources().getString(R.string.pleaseWait), "");
                        final Bundle sacParams = new Bundle();
                        sacParams.putString(BundleConstants.SACConstants.SSID
                                , tv_selected_wifi.getText().toString());
                        sacParams.putString(BundleConstants.SACConstants.PASSPHRASE
                                , text_input_wifi_password.getText().toString());
                        sacParams.putInt(BundleConstants.SACConstants.NETWORK_TYPE
                                , security);

                        LibreMavidHelper.configure(sacParams, new SACListener() {
                            @Override
                            public void success() {
                                updateResponseUI("Credentials succesfully posted.");
                                doNext(sacParams);
                            }

                            @Override
                            public void failure(String message) {
                                updateResponseUI(message);
                                Log.d("wifi_configure", message);
                            }

                            @Override
                            public void successBLE(byte[] b) {

                            }
                        });
                    }
                } else if (tv_selected_wifi.getText().toString().equals("Select a Wi-fi")) {
                    buildSnackBar("Please select a wifi from the list");
                }
                break;

            case R.id.btn_cancel:
                onBackPressed();
                break;

//            case R.id.connectBtn: {
//
//            }
//            case R.id.refresh: {
//                refreshSSIDList();
//            }

        }
    }

    private void doNext(Bundle sacParams) {
        Intent intent = new Intent(WifiConfigureActivity.this, MavidWifiConfigureSpeakerActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sacParams.putString(Constants.INTENTS.MACADDRESS, getMacAddress());
        intent.putExtra(Constants.INTENTS.SAC_PARAMS, sacParams);
        startActivity(intent);
        finish();
    }


    ProgressDialog mProgressDialog;

//    public void showLoader(final String title, final String message) {
//
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Log.d("ShowingLoader", "Showing loader method");
//                if (mProgressDialog == null) {
//                    mProgressDialog = ProgressDialog.show(WifiConfigureActivity.this, title, message, true, true, null);
//                }
//                mProgressDialog.setCancelable(false);
//                if (!mProgressDialog.isShowing()) {
//                    if (!(WifiConfigureActivity.this.isFinishing())) {
//                        mProgressDialog = ProgressDialog.show(WifiConfigureActivity.this, title, message, true, true, null);
//                    }
//                }
//
//            }
//        });
//    }
//
//    public void closeLoader() {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (mProgressDialog != null && mProgressDialog.isShowing()) {
//                    if (!(WifiConfigureActivity.this.isFinishing())) {
//                        mProgressDialog.setCancelable(false);
//                        mProgressDialog.dismiss();
//                        mProgressDialog.cancel();
//                    }
//                }
//            }
//        });
//
//    }


    private void refreshSSIDList() {
        showLoader(getResources().getString(R.string.notice), getResources().getString(R.string.pleaseWait) + "...");
        handler.sendEmptyMessageDelayed(GET_SCAN_LIST_HANDLER, 30000);
        LibreMavidHelper.sendCustomSACMessage("ScanReq", new SACListenerWithResponse() {
            @Override
            public void response(final MessageInfo messageInfo) {
                LibreLogger.d(this, "Recieved scan list " + messageInfo.getMessage());
                handler.removeMessages(GET_SCAN_LIST_HANDLER);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeLoader();
                        populateScanlistMap(messageInfo.getMessage().toString());
//                        scanList = getSSIDList();
//                        securityAdapter.clear();
//                        securityAdapter = new ArrayAdapter(WifiConfigureActivity.this, android.R.layout.simple_spinner_item, scanList);
//                        securityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//                        ssidSpinner.setAdapter(securityAdapter);
                        //securityAdapter.notifyDataSetChanged();
                        //ssidSpinner.setSelection(0);
                    }
                });
            }

            @Override
            public void failure(Exception e) {
                handler.removeMessages(GET_SCAN_LIST_HANDLER);
                closeLoader();
                somethingWentWrong(WifiConfigureActivity.this);
            }
        });
    }

    private void updateResponseUI(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //closeLoader();
                buildSnackBar(message);
            }
        });
    }

    public void getIntentParams() {
        String scanList = getIntent().getStringExtra(Constants.INTENTS.SCAN_LIST);
        populateScanlistMap(scanList);
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
                        || (obj.getString("SSID").isEmpty())) {
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
        }
    }

    public void setupBottomSheetForWifiList() {

        View view = getLayoutInflater().inflate(R.layout.show_wifi_list_bottom_sheet, null);
//        swipe_refresh = view.findViewById(R.id.swipe_refresh);
        tv_no_data = view.findViewById(R.id.tv_no_data);
        rv_wifi_list = view.findViewById(R.id.rv_wifi_list);
        AppCompatImageView iv_close_icon = view.findViewById(R.id.iv_close_icon);


//        swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                refreshSSIDList();
//                if (modelWifiScanList != null && wifiListBottomSheetAdapter != null) {
//                    modelWifiScanList.clear();
//                    wifiListBottomSheetAdapter.notifyDataSetChanged();
//                }
//
//            }
//        });
        setWifiListBottomSheetAdapter();

        iv_close_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog = new BottomSheetDialog(WifiConfigureActivity.this, R.style.BottomSheetDialog);
        bottomSheetDialog.setContentView(view);

        bottomSheetDialog.setCancelable(false);
        bottomSheetDialog.show();
    }


//    public ArrayList<String> getSSIDList() {
//        ArrayList<String> scanList = new ArrayList<>();
//        Set<String> keySet = scanListMap.keySet();
//
//        for ()
//
//            for (String ssid : keySet) {
//                scanList.add(ssid);
//
//            }
//        return scanList;
//    }


    public void setWifiListBottomSheetAdapter() {
        Collections.sort(modelWifiScanList, new Comparator<ModelWifiScanList>() {
            @Override
            public int compare(ModelWifiScanList modelWifiScanList, ModelWifiScanList modelWifiScanList1) {
                //sorting via rssi values
                return Integer.parseInt(modelWifiScanList1.getRssi()) - Integer.parseInt(modelWifiScanList.getRssi());
            }
        });
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(WifiConfigureActivity.this);
        wifiListBottomSheetAdapter = new WifiListBottomSheetAdapter(WifiConfigureActivity.this, modelWifiScanList);
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

    public String getMacAddress() {
        String ssid = getconnectedSSIDname(WifiConfigureActivity.this);
        ssid = ssid.substring(ssid.length() - 5);
        return ssid;
    }

    //    @Override
//    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//    String selectedSSID = scanListMap.get(scanList.get(position));
//        if(selectedSSID.equalsIgnoreCase("NONE")
//                ||selectedSSID.equalsIgnoreCase("OPEN"))
//
//    {
//        security = 0;
//        passPhrase = "";
//        passLyt.setVisibility(View.GONE);
//    } else
//
//    {
//        passLyt.setVisibility(View.VISIBLE);
//    }
//
//        if(scanListMap.get(scanList.get(position)).
//
//    equalsIgnoreCase("WPA-PSK"))
//
//    {
//
//    } else if(scanListMap.get(scanList.get(position)).
//
//    equalsIgnoreCase("WPA/WPA2"))
//
//    {
//
//    }
//    }
//
//    @Override
//    public void onNothingSelected(AdapterView<?> parent) {
//
//    }

    @Override
    public void onItemClicked(int pos) {
        if (bottomSheetDialog != null) {
            bottomSheetDialog.dismiss();
        }

        tv_selected_wifi.setText(modelWifiScanList.get(pos).getSsid());

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
