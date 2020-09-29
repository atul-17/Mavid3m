package com.mavid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mavid.BLEApproach.BLEScanActivity;
import com.mavid.BLEApproach.BleCommunication;
import com.mavid.BLEApproach.BleWriteInterface;
import com.mavid.BLEApproach.HotSpotOrSacSetupActivity;
import com.mavid.SAC.SACInstructionActivity;
import com.mavid.libresdk.LibreMavidHelper;
import com.mavid.libresdk.TaskManager.Discovery.CustomExceptions.WrongStepCallException;
import com.mavid.libresdk.Util.LibreLogger;
import com.mavid.models.ModelSaveHotSpotName;
import com.mavid.receivers.GpsLocationReceiver;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by bhargav on 6/2/18.
 */

public class BaseActivity extends AppCompatActivity {
    private boolean listenToNetworkChanges;
    private boolean listenToWifiConnectingStatus;
    private NetworkReciever networkReciever;
    private AlertDialog alertDialog;
    AlertDialog.Builder builder;
    private Dialog alert;
    private static String SSID = "";

    ProgressDialog mProgressDialog;

    private Dialog mDialog;

    AppCompatTextView progress_title;
    ProgressBar progress_bar;

    AppCompatTextView progress_message;


    private static final long SCAN_PERIOD = 10000;

    private boolean mScanning;


    AppCompatTextView tv_alert_title;
    AppCompatTextView tv_alert_message;

    AppCompatButton btn_ok;
    AppCompatButton btn_cancel;


    public GpsLocationReceiver gpsLocationReceiver = new GpsLocationReceiver();

//    public void showLoader(final String title, final String message) {
//
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Log.d("ShowingLoader", "Showing loader method");
//                if (mProgressDialog == null) {
//                    try {
//                        mProgressDialog = ProgressDialog.show(BaseActivity.this, title, message, true, true, null);
//                        mProgressDialog.setCancelable(false);
//                    } catch (WindowManager.BadTokenException e) {
//                        e.printStackTrace();
//                    }
//                }
//                if (!mProgressDialog.isShowing()) {
//                    if (!(BaseActivity.this.isFinishing())) {
//                        mProgressDialog = ProgressDialog.show(BaseActivity.this, title, message, true, true, null);
//                        mProgressDialog.setCancelable(false);
//
//                    }
//                }
//
//            }
//        });
//    }


    public void showLoader(final String title, final String message) {

        if (!(BaseActivity.this.isFinishing())) {

            if (mDialog == null) {
                mDialog = new Dialog(BaseActivity.this);
                mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                mDialog.setContentView(R.layout.custom_progress_bar);

                mDialog.setCancelable(false);
                progress_title = mDialog.findViewById(R.id.progress_title);
                progress_bar = mDialog.findViewById(R.id.progress_bar);
                progress_message = mDialog.findViewById(R.id.progress_message);
            }
            Log.d("ShowingLoader", "Showing loader method");
            progress_title.setText(title);
            progress_message.setText(message);
            progress_bar.setIndeterminate(true);
            progress_bar.setVisibility(View.VISIBLE);
            mDialog.show();

        }
    }


    public boolean checkLocationIsEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && !MavidApplication.doneLocationChange) {
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!isGpsEnabled && !isNetworkEnabled) {
                askToEnableLocationService(context);
                MavidApplication.doneLocationChange = false;
                return false;
            } else {
                MavidApplication.doneLocationChange = true;
            }
        }
//        MavidApplication.doneLocationChange = true;
        return true;
    }

    public void askToEnableLocationService(final Context context) {

        if (alert == null) {

            alert = new Dialog(context);

            alert.requestWindowFeature(Window.FEATURE_NO_TITLE);

            alert.setContentView(R.layout.custom_single_button_layout);

            alert.setCancelable(false);

            tv_alert_title = alert.findViewById(R.id.tv_alert_title);

            tv_alert_message = alert.findViewById(R.id.tv_alert_message);

            btn_ok = alert.findViewById(R.id.btn_ok);
        }

        tv_alert_title.setText(context.getResources().getString(R.string.locationServicesIsOff));

        tv_alert_message.setText(context.getResources().getString(R.string.enableLocation));

        btn_ok.setText(context.getResources().getString(R.string.gotoSettings));

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert.dismiss();
                turnGPSOn(context);
            }
        });

        alert.show();

//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setTitle()
//                .setMessage()
//                .setPositiveButton(, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        dialogInterface.dismiss();
//
//
//                    }
//                });
//        builder.setCancelable(false);
//        builder.create();
//        builder.show();
    }

    public void turnGPSOn(Context context) {
        Intent intent = new Intent(
                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }

    public void closeLoader() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mDialog != null) {
                    if (!(BaseActivity.this.isFinishing())) {
                        mDialog.dismiss();
                        mDialog = null;
                    }
                }
            }
        });
    }

//    public void closeLoader() {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (mProgressDialog != null && mProgressDialog.isShowing()) {
//                    if (!(BaseActivity.this.isFinishing())) {
//                        // mProgressDialog.setCancelable(false);
//                        mProgressDialog.dismiss();
//                        mProgressDialog.cancel();
//                    }
//                }
//            }
//        });

//}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SSID = getconnectedSSIDname(BaseActivity.this);
        super.onCreate(savedInstanceState);
        enableNetworkChangeCallBack();
        enableNetworkOffCallBack();
        networkReciever = new NetworkReciever();
//        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
//        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
//        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
//        this.registerReceiver(BTReceiver, filter1);
//        this.registerReceiver(BTReceiver, filter2);
//        this.registerReceiver(BTReceiver, filter3);
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
//        try {
//            registerReceiver(networkReciever, intentFilter);
//        }catch (Exception e){
//            Log.d("BaseActivity","receiver "+e.getMessage());
//        }

        registerReceiver(gpsLocationReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("suma in mac", "receiver in onresume base activity");
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(BTReceiver, filter1);
        this.registerReceiver(BTReceiver, filter2);
        this.registerReceiver(BTReceiver, filter3);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        try {
            registerReceiver(networkReciever, intentFilter);
        } catch (Exception e) {
            Log.d("BaseActivity", "receiver " + e.getMessage());
        }

        LibreMavidHelper.setAdvertiserMessage(getMsearchPayload());
        try {
            LibreMavidHelper.advertise();
        } catch (WrongStepCallException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disableNetworkChangeCallBack();
        disableNetworkOffCallBack();
        try {
            this.unregisterReceiver(networkReciever);
            this.unregisterReceiver(gpsLocationReceiver);
        } catch (Exception e) {

        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("suma in mac", "receiver in onstop base activity");

        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }

    public String getMsearchPayload() {
        String mSearchPayload = "M-SEARCH * HTTP/1.1\r\n" +
                "MX: 10\r\n" +
                "ST: urn:schemas-upnp-org:device:DDMSServer:1\r\n" +
                "HOST: 239.255.255.250:1800\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "\r\n";
        return mSearchPayload;
    }

    public void disableNetworkOffCallBack() {
        listenToWifiConnectingStatus = false;
    }

    public void enableNetworkOffCallBack() {
        listenToWifiConnectingStatus = true;
    }

    public void disableNetworkChangeCallBack() {
        listenToNetworkChanges = false;
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver BTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Do something if connected
//                  Toast.makeText(getApplicationContext(), "BT Connected", Toast.LENGTH_SHORT).show();
                MavidApplication.isACLDisconnected=false;

                LibreLogger.d(this, "BleCommunication in suma ble state turning CONNECTED in receiver");

            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //suma commenting toast conn lost
                // Toast.makeText(getApplicationContext(), "Connection Lost to the Device", Toast.LENGTH_SHORT).show();
                LibreLogger.d(this, "BleCommunication in suma ble state turning disconnected baseactivity");
                MavidApplication.isACLDisconnected=true;
                //Do something if disconnected
                // Toast.makeText(getApplicationContext(), "BT Disconnected", Toast.LENGTH_SHORT).show();
            }
        }
    };

    public void enableNetworkChangeCallBack() {
        listenToNetworkChanges = true;
    }


//    private void scanLeDevice(final Handler handler, final boolean enable, final Context context,
//                              final BluetoothAdapter mBluetoothAdapter) {
//
//
//        if (enable) {
//            // Stops scanning after a pre-defined scan period.
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    closeLoader();
//                    mScanning = false;
//                    mBluetoothAdapter.stopLeScan(leScanCallback);
//                    if (leDeviceListAdapter.getCount()==0){
//                        Intent intent  = new Intent(context, NoBLEDeviceFragment.class);
//                        startActivity(intent);
//                        finish();
//                    }
//                }
//            }, SCAN_PERIOD);
//
//            mScanning = true;
//            mBluetoothAdapter.startLeScan(leScanCallback);
//        } else {
//            mScanning = false;
//            mBluetoothAdapter.stopLeScan(leScanCallback);
//        }
//    }
//
//    private BluetoothAdapter.LeScanCallback leScanCallback =
//            new BluetoothAdapter.LeScanCallback() {
//                @Override
//                public void onLeScan(final BluetoothDevice device, int rssi,
//                                     byte[] scanRecord) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (device.getName() != null) {
//                                String modelType1 = "Libre";
//                                String modelType2 = "Microdot";
//                                String modelType3 = "Wave";
//
//                                if (device.getName().toLowerCase().contains(modelType1.toLowerCase())
//                                        || device.getName().toLowerCase().contains(modelType2.toLowerCase())
//                                        || device.getName().toLowerCase().contains(modelType3.toLowerCase())) {
////                                    closeLoader();
//                                    runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            //add a bus to
////                                            leDeviceListAdapter.addDevice(device);
////                                            leDeviceListAdapter.notifyDataSetChanged();
//                                        }
//                                    });
//
//                                }
////        device.connectGatt()
////                                }
//                            }
//                        }
//                    });
//                }
//            };

    public String getconnectedSSIDname(Context mContext) {
        WifiManager wifiManager;
        wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();

        Log.d("BaseActivity", "getconnectedSSIDname wifiInfo = " + wifiInfo.toString()+"locationChangeValue:");
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }

    public String getExpectedMSearchResponse() {
        String message = "MSEARCH RESPONSE";
        return message;
    }


    public void refreshDiscovery() {
        try {
            LibreMavidHelper.advertise();
        } catch (WrongStepCallException e) {
            e.printStackTrace();
        }
    }

    public void showSacTimeoutAlert(final AppCompatActivity appCompatActivity, final BleWriteInterface bleWriteInterface) {

        if (alert == null) {

            alert = new Dialog(appCompatActivity);

            alert.requestWindowFeature(Window.FEATURE_NO_TITLE);

            alert.setContentView(R.layout.custom_single_button_layout);

            alert.setCancelable(false);

            tv_alert_title = alert.findViewById(R.id.tv_alert_title);

            tv_alert_message = alert.findViewById(R.id.tv_alert_message);

            btn_ok = alert.findViewById(R.id.btn_ok);
        }

        tv_alert_title.setText("SAC Timeout");

        tv_alert_message.setText("Please put the device into the setup mode");

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert.dismiss();
                //StopSAC
                alert = null;
                BleCommunication bleCommunication = new BleCommunication(bleWriteInterface);
                BleCommunication.writeInteractorStopSac();
                startActivity(new Intent(appCompatActivity, MavidHomeTabsActivity.class));
                finish();
            }
        });

        alert.show();

//        AlertDialog.Builder builder = new AlertDialog.Builder(appCompatActivity);
//        builder.setCancelable(true);
//        builder.setTitle("SAC Timeout");
//        builder.setCancelable(false);
//        builder.setMessage("Please put the device into the setup mode");
//        builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//                //StopSAC
//                BleCommunication bleCommunication = new BleCommunication(bleWriteInterface);
//                BleCommunication.writeInteractorStopSac();
//                startActivity(new Intent(appCompatActivity, MavidHomeTabsActivity.class));
//                finish();
//            }
//        });
//        AlertDialog alert = builder.create();
//        if (!(appCompatActivity).isFinishing()) {
//            //show dialog
//            alert.show();
//
//        }
    }

    public void mobileDataOnOff(Context context) {
        try {

            if (alert == null) {

                alert = new Dialog(context);

                alert.requestWindowFeature(Window.FEATURE_NO_TITLE);

                alert.setContentView(R.layout.custom_single_button_layout);

                alert.setCancelable(false);

                tv_alert_title = alert.findViewById(R.id.tv_alert_title);

                tv_alert_message = alert.findViewById(R.id.tv_alert_message);

                btn_ok = alert.findViewById(R.id.btn_ok);

            }

            tv_alert_title.setText("");

            tv_alert_message.setText(getResources().getString(R.string.switchOffMobiledata));


            btn_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ////startActivity(new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS));
                    alert.dismiss();
                    alert = null;
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setClassName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");
                    startActivity(intent);
                }
            });

            alert.show();


//            alertDialog = null;
//            AlertDialog.Builder builder = new AlertDialog.Builder(context);
//
//            builder.setMessage(getResources().getString(R.string.switchOffMobiledata))
//                    .setCancelable(false)
//                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                            //startActivity(new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS));
//                            Intent intent = new Intent(Intent.ACTION_MAIN);
//                            intent.setClassName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");
//                            startActivity(intent);
//                        }
//                    });
//
//            if (alertDialog == null) {
//                alertDialog = builder.show();
//            }
//
//            alertDialog.show();

        } catch (Exception e) {

        }
    }

    public void somethingWentWrong(final Context context) {
        try {

            if (alert == null) {

                alert = new Dialog(context);

                alert.requestWindowFeature(Window.FEATURE_NO_TITLE);

                alert.setContentView(R.layout.custom_single_button_layout);

                alert.setCancelable(false);

                tv_alert_title = alert.findViewById(R.id.tv_alert_title);

                tv_alert_message = alert.findViewById(R.id.tv_alert_message);

                btn_ok = alert.findViewById(R.id.btn_ok);
            }

            tv_alert_title.setText("");
            tv_alert_message.setText(getResources().getString(R.string.somethingWentWrong));

            btn_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alert.dismiss();
                    ((AppCompatActivity) context).finish();
                    alert = null;
                }
            });
            alert.show();

//            alertDialog = null;
//            AlertDialog.Builder builder = new AlertDialog.Builder(context);
//
//            builder.setMessage(getResources().getString(R.string.somethingWentWrong))
//                    .setCancelable(false)
//                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                            alertDialog.dismiss();
//                        }
//                    });
//
//            if (alertDialog == null) {
//                alertDialog = builder.show();
//                TextView messageView = (TextView) alertDialog.findViewById(android.R.id.message);
//                messageView.setGravity(Gravity.CENTER);
//            }
//
//            alertDialog.show();

        } catch (Exception e) {

        }
    }

    private void alertForNetworkChange(final Context context) {
        if (!BaseActivity.this.isFinishing()) {
            /* If Restarting of Network Is happening We can discard the Network Change */
            alert = new Dialog(context);

            alert.requestWindowFeature(Window.FEATURE_NO_TITLE);

            alert.setContentView(R.layout.custom_single_button_layout);

            alert.setCancelable(false);

            tv_alert_title = alert.findViewById(R.id.tv_alert_title);

            tv_alert_message = alert.findViewById(R.id.tv_alert_message);

            btn_ok = alert.findViewById(R.id.btn_ok);

            tv_alert_title.setText("");

            tv_alert_message.setText(context.getResources().getString(R.string.restartTitle));

            btn_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alert.dismiss();
                    alert = null;
                    restartApp(getApplicationContext());
                }
            });
            alert.show();
        }
    }


    public void ShowAlertDynamicallyGoingToHomeScreen(String title, String message, final AppCompatActivity appCompatActivity) {


        if (alert == null) {


            alert = new Dialog(appCompatActivity);

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
                startActivity(new Intent(appCompatActivity.getApplicationContext(), MavidHomeTabsActivity.class));
                finish();
            }
        });


        alert.show();
        Log.d("atul", "alert dialog");


    }


    public void scanListFailed(String title, String message, final AppCompatActivity appCompatActivity) {
        if (alert == null) {


            alert = new Dialog(appCompatActivity);

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
               appCompatActivity.onBackPressed();
            }
        });


        alert.show();
    }

    //// receiver ////
    class NetworkReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Context newContext = getApplicationContext();

            Log.d("Receiver", "action = " + intent.getAction() + ", listenToNetworkChanges = " + listenToNetworkChanges);

            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (!listenToNetworkChanges)
                return;

            if ((activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting()) == false &&
                    listenToWifiConnectingStatus) {
//                Toast.makeText(getApplicationContext(), "Network is disconnected", Toast.LENGTH_SHORT).show();

                try {

                    if (activeNetworkInfo != null) {
                        Log.d("Receiver", "Active network info:" + activeNetworkInfo.isConnectedOrConnecting());
                        Log.d("Receiver", "Active network info:" + activeNetworkInfo.isConnectedOrConnecting());

                        if (activeNetworkInfo.isConnected() && activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE)
                            Log.d("Receiver", "Active network type:MOBILE");
                    } else {
                        Log.d("Receiver", "Active network interface is null...and hence we will show the alert box");
                    }
                } catch (Exception e) {

                }

                SSID = "";
                //alertBoxForNetworkOff();
                return;
            }

            if (!intent.getAction().equals("android.net.conn.TETHER_STATE_CHANGED")
                    && !intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
                return;

            if (SSID.equals(getconnectedSSIDname(newContext))) {
                return;
            }

            // network change has happened
            WifiManager wifiManager =
                    (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            final String ssid = getconnectedSSIDname(newContext);
            //|| ssid.equals("<unknownssid>")
            if (ssid == null || ssid.equals("") || ssid.equals("<unknown ssid>"))
                return;
            if (!MavidApplication.doneLocationChange) {
                alertForNetworkChange(BaseActivity.this);
            } else {
                if ((!(context instanceof BLEScanActivity))) {
                    //other than ble activity call network change alert box
                    alertForNetworkChange(context);
                }
            }
        }

    }


//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
//    public void restartApp(Context context) {
//        Log.d("NetworkChanged", "App is Restarting");
//
//        Intent mStartActivity = new Intent(context, SplashScreenActivity.class);
//        /*sending to let user know that app is restarting*/
//        mStartActivity.putExtra(SplashScreenActivity.APP_RESTARTING, true);
//        int mPendingIntentId = 123456;
//        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
//        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 200, mPendingIntent);
//
//        /* * Finish this activity, and tries to finish all activities immediately below it
//         * in the current task that have the same affinity.*/
//        ActivityCompat.finishAffinity(this);
//        /* Killing our Android App with The PID For the Safe Case */
//        int pid = android.os.Process.myPid();
//        android.os.Process.killProcess(pid);
//
////         System.exit(0);
//    }


    public static void restartApp(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            ComponentName componentName = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(componentName);
            context.startActivity(mainIntent);
            Runtime.getRuntime().exit(0);
        }
    }

    public String getHotSpotName(String deviceName) {
        SharedPreferences mPrefs = getSharedPreferences("Mavid", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String hotspotNamesListString = mPrefs.getString("hotspotNamesList", "");

        Type type = new TypeToken<List<ModelSaveHotSpotName>>() {
        }.getType();

        if (hotspotNamesListString != null) {
            if (!hotspotNamesListString.isEmpty()) {
                List<ModelSaveHotSpotName> modelSaveHotSpotNameList = gson.fromJson(hotspotNamesListString, type);
                if (modelSaveHotSpotNameList.size() > 0) {
                    for (ModelSaveHotSpotName modelSaveHotSpotName : modelSaveHotSpotNameList) {
                        if (modelSaveHotSpotName.friendlyName!=null) {
                            if (modelSaveHotSpotName.friendlyName.equals(deviceName)) {
                                return modelSaveHotSpotName.hotSpotName;
                            }
                        }
                    }
                }
            }
        }
        return "Phone's Personal HotSpot";
    }


    public void removeOlderHotSpotName(String deviceName){

    }

    public void alertBoxForNetworkOff() {
        if (!BaseActivity.this.isFinishing()) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    BaseActivity.this);

            // set title
            alertDialogBuilder.setTitle(getString(R.string.wifiConnectivityStatus));

            // set dialog message
            alertDialogBuilder
                    .setMessage(getString(R.string.connectToWifi))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // if this button is clicked, close
                            // current activity
                            dialog.cancel();
                            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivityForResult(intent, 1234);

                        }
                    });

            // create alert dialog
            if (alertDialog == null)
                alertDialog = alertDialogBuilder.create();

            // show it

            try {
                alertDialog.show();
            } catch (Exception e) {

            }
            Log.d("Receiver", "connect to wifi dialog shown");
        }
    }

}
