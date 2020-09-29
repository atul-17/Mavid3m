package com.mavid.BLEApproach;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.os.DeadObjectException;
import android.os.Handler;
import android.util.Log;

import com.mavid.MavidApplication;
import com.mavid.libresdk.Util.LibreLogger;

import java.lang.reflect.Method;

import static com.mavid.BLEApproach.BLEGattAttributes.MAVID_BLE_CHARACTERISTICS;
import static com.mavid.BLEApproach.BluetoothLeService.mBluetoothGatt;

public class BleCommunication {

    static BleWriteInterface bleWriteInterface;

    public BleCommunication(BleWriteInterface bleWriteInterface) {

        this.bleWriteInterface = bleWriteInterface;

    }
//"ReadDeviceStatus

    public static void writeInteractor() {
        try {
//            mBluetoothGatt.discoverServices();
//            mBluetoothGatt.getServices();

            BluetoothGattCharacteristic interactor = mBluetoothGatt
                    .getService(BLEGattAttributes.MAVID_BLE_SERVICE)
                    .getCharacteristic(MAVID_BLE_CHARACTERISTICS);
            if(interactor!=null) {
                interactor.setValue("ScanReq");
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                BluetoothGattCharacteristic tChar = syncService.getCharacteristic(SYNC_HEIGHT_INPUT_CHAR);
//                if (tChar == null) throw new AssertionError("characteristic null when sync time!");
//
//                // use one of them in regards of the Characteristic's property
//                tChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//                //tChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
//
//
//                tChar.setValue(/*another int*/, BluetoothGattCharacteristic.FORMAT_SINT32, 0);
//                gatt.writeCharacteristic(tChar);

                interactor.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//            if ((interactor.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0
//                    && (interactor.getProperties() &
//                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
//                Log.d("Bluetooth", "write interactor if");
//            } else {
//                Log.d("Bluetooth", "write interactor else");
                //interactor.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                BluetoothLeService.mBluetoothGatt.connect();
                mBluetoothGatt.writeCharacteristic(interactor);
                bleWriteInterface.onWriteSuccess();
                Log.d("Bluetooth", "SUMA IN SCANREQ write interactor Success");
            }
           // }


        } catch (NullPointerException e) {
            e.printStackTrace();
        }

    }

    public static void writeInteractorDeviceStatus() {
        BluetoothGattCharacteristic interactor = mBluetoothGatt
                .getService(BLEGattAttributes.MAVID_BLE_SERVICE)
                .getCharacteristic(MAVID_BLE_CHARACTERISTICS);
        interactor.setValue("ReadDeviceStatus");
        //interactor.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        if ((interactor.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0
                && (interactor.getProperties() &
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
            Log.d("Bluetooth", "write interactor if");
        } else {
            Log.d("Bluetooth", "write interactor else");
            //interactor.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            BluetoothLeService.mBluetoothGatt.connect();

            mBluetoothGatt.writeCharacteristic(interactor);
            bleWriteInterface.onWriteSuccess();
        }

    }

    public static void writeInteractorStopSac() {
        try {
            BluetoothGattCharacteristic interactor = mBluetoothGatt
                    .getService(BLEGattAttributes.MAVID_BLE_SERVICE)
                    .getCharacteristic(MAVID_BLE_CHARACTERISTICS);
            interactor.setValue("StopSAC");
            interactor.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

            mBluetoothGatt.writeCharacteristic(interactor);
            bleWriteInterface.onWriteSuccess();
            LibreLogger.d("BleCommunication", "suma in wifi connecting event in stopsac successs");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public static void writeInteractor(byte[] b) {

        BluetoothGattCharacteristic interactor = mBluetoothGatt
                .getService(BLEGattAttributes.MAVID_BLE_SERVICE)
                .getCharacteristic(MAVID_BLE_CHARACTERISTICS);

        interactor.setValue(b);

        mBluetoothGatt.writeCharacteristic(interactor);
        bleWriteInterface.onWriteSuccess();
        LibreLogger.d("BleCommunication", "suma in bleCommunication writting Sac");
    }

    public static void writeInteractorWifiConnected() {
        BluetoothGattCharacteristic interactor = mBluetoothGatt
                .getService(BLEGattAttributes.MAVID_BLE_SERVICE)
                .getCharacteristic(MAVID_BLE_CHARACTERISTICS);
        interactor.setValue("Wifi_Conenction");
        interactor.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        mBluetoothGatt.writeCharacteristic(interactor);
        if (interactor == null) {
            bleWriteInterface.onWriteFailure();
        } else {
            bleWriteInterface.onWriteSuccess();
        }
    }

    //ReadWIFIStatus
    public static void writeInteractorReadWifiStatus() {
        BluetoothGattCharacteristic interactor = mBluetoothGatt
                .getService(BLEGattAttributes.MAVID_BLE_SERVICE)
                .getCharacteristic(MAVID_BLE_CHARACTERISTICS);
        interactor.setValue("ReadWIFIStatus");
        interactor.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        mBluetoothGatt.writeCharacteristic(interactor);
        bleWriteInterface.onWriteSuccess();
        MavidApplication.readWifiStatus = true;
        LibreLogger.d("BleCommunication", "suma in wifi connecting event in read wifi status success");

    }
}
