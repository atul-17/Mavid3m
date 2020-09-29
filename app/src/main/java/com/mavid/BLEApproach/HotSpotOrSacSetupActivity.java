package com.mavid.BLEApproach;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import com.mavid.BLE_SAC.BLEConfigureActivity;
import com.mavid.BaseActivity;

import com.mavid.MavidHomeTabsActivity;
import com.mavid.OtherSetupApproach.BLEHotSpotCredentialsActivity;
import com.mavid.R;
import com.mavid.libresdk.Util.LibreLogger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


/**
 * BLE diffrent options screen
 */

public class HotSpotOrSacSetupActivity extends BaseActivity implements OnClickListener, BleWriteInterface, BleReadInterface {

    private Button btnYes, btnNo;
    private ImageView back;
    private BLEManager bleManager;




    BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hot_spot_or_sac_setup);

        inItWidgets();

        EventBus.getDefault().register(this);

        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

    }

    private void inItWidgets() {
        btnYes = findViewById(R.id.btnYes);
        btnYes.setOnClickListener(this);
        btnNo = findViewById(R.id.btnNo);
        btnNo.setOnClickListener(this);
        back = findViewById(R.id.iv_back);
        bleManager = BLEManager.getInstance(HotSpotOrSacSetupActivity.this);
        back.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (bleManager.isBluetoothEnabled()) {
            super.onBackPressed();
        } else {
            //go to ble blinking fragment
            Bundle bundle = new Bundle();
            bundle.putBoolean("isbleBlinkingFragment", true);
            startActivity(new Intent(HotSpotOrSacSetupActivity.this, MavidHomeTabsActivity.class).putExtras(bundle));
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnYes:
                startActivity(new Intent(HotSpotOrSacSetupActivity.this, BLEHotSpotCredentialsActivity.class));
                break;
            case R.id.btnNo:
                BleCommunication bleCommunication = new BleCommunication(HotSpotOrSacSetupActivity.this);
                BleCommunication.writeInteractor();
                startActivity(new Intent(HotSpotOrSacSetupActivity.this, BLEConfigureActivity.class));
                LibreLogger.d(this, "suma in bleconfigure actvivity");

                break;
        }
    }


    @Override
    public void onWriteSuccess() {
    }

    @Override
    public void onWriteFailure() {

    }

    @Override
    public void onReadSuccess(byte[] data) {
        Log.d("HotSpotOrSacSetup", "data : " + data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BLEEvntBus event) {
//        Toast.makeText(BLEHotSpotCredentialsActivity.this, "hotspot : " + event.message, Toast.LENGTH_SHORT).show();

        byte[] value = event.message;
        Log.d("Hotspot", "Value received: " + value);
        int response = getDataLength(value);
        LibreLogger.d(this, "suma in ble configure activity get the value in sac setup activity\n" + response);
        if (response == 14) {
            showSacTimeoutAlert(HotSpotOrSacSetupActivity.this, this);
        }
    }

    public int getDataLength(byte[] buf) {
        byte b1 = buf[3];
        byte b2 = buf[4];
        short s = (short) (b1 << 8 | b2 & 0xFF);

        LibreLogger.d(this, "Data length is returned as s" + s);
        return s;
    }

}
