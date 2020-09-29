package com.mavid.BLEApproach;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.danimahardhika.cafebar.CafeBar;
import com.mavid.BaseActivity;
import com.mavid.BleListFragment;
import com.mavid.MavidHomeTabsActivity;
import com.mavid.NoBLEDeviceFragment;
import com.mavid.R;
import com.mavid.utility.BusLeScanBluetoothDevicesEventProgress;
import com.mavid.libresdk.Util.LibreLogger;


import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BLEScanActivity extends BaseActivity implements BleConnectionStatus, BleReadInterface {

    private static byte[] bb;
    private final static String TAG = BLEScanActivity.class.getSimpleName();
    CountDownTimer countDownTimer;

    public BluetoothAdapter mBluetoothAdapter;
    private Handler handler;
    private boolean mScanning;
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 20000;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;
    static boolean sacCred = false;
    ImageView back;

    BLEManager bleManager;

    String ProductIdText;

    CafeBar cafeBar;
    BleListFragment bleListFragment;



    public ArrayList<BluetoothDevice> bluetoothDeviceList = new ArrayList<>();

    private Dialog mDialog;

    AppCompatTextView progress_title;
    ProgressBar progress_bar;

    AppCompatTextView progress_message;



    public final int BLE_CONNECTING_STATUS = 0x300;

    public Handler handlerSac = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == BLE_CONNECTING_STATUS) {
                closeLoader();
                if (!BLEScanActivity.this.isFinishing()) {
                    ShowAlertDynamicallyGoingToHomeScreen("Ble Connection",
                            "Ble Connection timed out.Please try again", BLEScanActivity.this);
                }
            }
            return true;
        }
    });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blescan);


        handler = new Handler();


        back = (ImageView) findViewById(R.id.iv_back);


        bleManager = BLEManager.getInstance(BLEScanActivity.this);


        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //goto sacInstructionsBlinking screen
                Bundle bundle = new Bundle();
                bundle.putBoolean("isbleBlinkingFragment", true);
                Intent intent = new Intent(BLEScanActivity.this, MavidHomeTabsActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
                Log.d("Suma", "is ble enabled check yes or no on back pressed  bleScan Activity" + bleManager.isBluetoothEnabled());
            }
        });


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            buildSnackBar(getResources().getString(R.string.ble_not_supported));
//            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            buildSnackBar(getResources().getString(R.string.error_bluetooth_not_supported));
            finish();
            return;
        }


        loadFragments(new BleListFragment());


        if (checkLocationPermission() && checkLocationIsEnabled(BLEScanActivity.this) ) {
            showLoader("Please wait, we are trying to scan the device.", "");
            scanLeDevice(true);
        }


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);



    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver BTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Do something if connected
                // Toast.makeText(getApplicationContext(), "BT Connected", Toast.LENGTH_SHORT).show();
                LibreLogger.d(this, "BleCommunication in suma ble state turning CONNECTED in receiver BLESCAN");
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Do something if disconnected
                LibreLogger.d(this, "suma in ");
                // Toast.makeText(getApplicationContext(), "Connection Lost to the Device", Toast.LENGTH_SHORT).show();
                LibreLogger.d(this, "BleCommunication in suma ble state turning disconnected blescan");

                //  LibreLogger.d(this,"BleCommunication in suma ble state turning Disconnected in receiver BLESCAN");

            }
            //else if...
        }
    };
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize(BLEScanActivity.this, BLEScanActivity.this)) {
                Log.d(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mBluetoothLeService.connect(mDeviceAddress);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    //calling scan method to look for ble enabled devices
    public void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //after 10 secs the following piece of code is executed
                    closeLoader();
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(leScanCallback);
                    final Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.ble_frame_layout);
                    if (currentFragment instanceof BleListFragment) {
                        bleListFragment = (BleListFragment) getSupportFragmentManager().findFragmentById(R.id.ble_frame_layout);
                        if (bleListFragment != null) {
                            //if there are no ble devices then it goes to noBle fragment
                            if (bleListFragment.leDeviceListAdapter.getCount() == 0) {
                                loadFragments(new NoBLEDeviceFragment());
                            } else {
                                //else stays in the same fragment ie bleList fragment
                                if (!bleListFragment.isVisible())
                                    loadFragments(new BleListFragment());
                            }
                        }
                    }
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(leScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(leScanCallback);

        }

    }


    public boolean loadFragments(Fragment fragment) {
        if (fragment != null) {
            try {
                getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                        .replace(R.id.ble_frame_layout, fragment, fragment.getClass().getSimpleName())
                        .commit();
                return true;

            } catch (Exception e) {
                e.printStackTrace();
                LibreLogger.d(this, "loadFragment exception" + e.getMessage());
            }
        }

        return false;
    }


    public List<BluetoothDevice> getBluetoothDeviceList() {
        return bluetoothDeviceList = new ArrayList<>();
    }

    public BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (device.getName() != null) {
                                String modelType1 = "Libre";
                                String modelType2 = "Microdot";
                                String modelType3 = "Wave";

                                if (device.getName().toLowerCase().contains(modelType1.toLowerCase())
                                        || device.getName().toLowerCase().contains(modelType2.toLowerCase())
                                        || device.getName().toLowerCase().contains(modelType3.toLowerCase())) {
//                                    closeLoader();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            //bus keeps posting if new ble devices are found.
                                            closeLoader();
                                            BusLeScanBluetoothDevicesEventProgress.BusScanBluetoothDevices busScanBluetoothDevices = new BusLeScanBluetoothDevicesEventProgress
                                                    .BusScanBluetoothDevices();
                                            busScanBluetoothDevices.setBluetoothDevice(device);

                                            EventBus.getDefault().post(busScanBluetoothDevices);
                                        }
                                    });
                                }
//        device.connectGatt()
//                                }
                            }
                        }
                    });
                }
            };


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Bundle bundle = new Bundle();
        bundle.putBoolean("isbleBlinkingFragment", true);
        Intent intent = new Intent(BLEScanActivity.this, MavidHomeTabsActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
        //finish();

    }


    public void buildSnackBar(String message) {
        CafeBar.Builder builder = CafeBar.builder(BLEScanActivity.this);
        builder.autoDismiss(true);
        builder.customView(R.layout.custom_snackbar_layout);

        cafeBar = builder.build();
        AppCompatTextView tv_message = cafeBar.getCafeBarView().findViewById(R.id.tv_message);
        tv_message.setText(message);

        cafeBar.show();
    }


    @Override
    protected void onStop() {
        super.onStop();
        mBluetoothLeService.removelistener(this);
        LibreLogger.d("Bluetooth","suma in getting characteristics null on stop");
    }


    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(BTReceiver, filter1);
        this.registerReceiver(BTReceiver, filter2);
        this.registerReceiver(BTReceiver, filter3);

//        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
//        }

       /* if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            locationManager.requestLocationUpdates(provider, 400, 1, this);
        }*/

//        if (checkLocationPermission()
//                && checkLocationIsEnabled(BLEScanActivity.this)) {
//            showLoader("Please wait, we are trying to scan the device.", "");
//            scanLeDevice(true);
//        }

        if (mBluetoothLeService != null) {
            mBluetoothLeService.addBLEServiceToApplicationInterfaceListener(this);
        }
    }


    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle(R.string.locationServicesIsOff)
                        .setMessage(R.string.enableLocationBLE)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(BLEScanActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        showLoader("Please wait, we are trying to scan the device.", "");
                        scanLeDevice(true);
                    }

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

        }
    }


    @Override
    protected void onPause() {
        super.onPause();
//        unregisterReceiver(mGattUpdateReceiver);
        scanLeDevice(false);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothLeService != null) {
             mBluetoothLeService.disconnect();
             mBluetoothLeService.close();
            mBluetoothLeService.removelistener(this);
            LibreLogger.d("Bluetooth","disconnect suma in getting characteristics null on Destroy");

        }
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    @Override
    public void onConnectionSuccess(BluetoothGattCharacteristic btGattChar) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                handlerSac.removeMessages(BLE_CONNECTING_STATUS);
                closeLoader();
                startActivity(new Intent(BLEScanActivity.this, HotSpotOrSacSetupActivity.class));

//                if (countDownTimer == null) {
//
//                    countDownTimer = new CountDownTimer(4000, 4000) {
//                        public void onTick(long millisUntilFinished) {
//                            LibreLogger.d(this,"suma in  count down timer ontick timer");
//                          //  showLoader("Connecting", "");
//                            }
//
//                        public void onFinish() {
//                            LibreLogger.d(this,"suma in  count down timer onFinish");
//                            closeLoader();
//                            handlerSac.removeMessages(BLE_CONNECTING_STATUS);
//                            startActivity(new Intent(BLEScanActivity.this, HotSpotOrSacSetupActivity.class));
//                        }
//
//                    }.start();
//                }
//                closeLoader();
//                startActivity(new Intent(BLEScanActivity.this, HotSpotOrSacSetupActivity.class));


//                final Handler handler = new Handler();
//                handler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        //your code here
//                        closeLoader();
//                        startActivity(new Intent(BLEScanActivity.this, HotSpotOrSacSetupActivity.class));
//                    }
//                }, 2000);

            }
        });
       // LibreLogger.d(this, "Disconnected from GATT server on connection success");
    }


    @Override
    public void onDisconnectionSuccess(final int status) {
        handlerSac.removeMessages(BLE_CONNECTING_STATUS);
        mBluetoothLeService.removelistener(this);
        LibreLogger.d("Bluetooth","suma in getting characteristics null on Disconnection success");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                closeLoader();
                buildSnackBar("Connection lost to the device");
//                Toast.makeText(BLEScanActivity.this, , Toast.LENGTH_LONG).show();
                onBackPressed();
            }
        });
    }

    @Override
    public void onReadSuccess(byte[] data) throws IOException {
        Log.d("BLEScan", "data : " + data.length);

    }


}