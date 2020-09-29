package com.mavid.BLEApproach;

import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.RequiresApi;

import android.os.Looper;
import android.util.Log;


import com.mavid.MavidApplication;
import com.mavid.MavidHomeTabsActivity;
import com.mavid.R;
import com.mavid.utility.BusLeScanBluetoothDevicesEventProgress;
import com.mavid.libresdk.Util.LibreLogger;

import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;


public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    String finalConnectedArray, finalConnectedString,finalConnectedString1;

    int finalConnectedIndex,finalConnectedIndex1;
//    public interface OnCounterReadListener {
//        void onCounterRead(int value);
//
//        void onConnected(boolean success);
//    }
    // private OnCounterReadListener mListener;

    private BleConnectionStatus connectionStatus;
    private BleReadInterface bleReadInterface;
    boolean gatt_status_133 = false;
    public BluetoothManager mBluetoothManager;
    public static BluetoothAdapter mBluetoothAdapter;
    public String mBluetoothDeviceAddress;
    public static BluetoothGatt mBluetoothGatt;
    public int mConnectionState = STATE_DISCONNECTED;
    int finalPayloadLength;
    BluetoothLeScanner mBleScanner;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

//    private static final Object SampleGattAttributes = ;
//    public final static UUID UUID_HEART_RATE_MEASUREMENT =
//            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);


    private HashMap<String, BluetoothGattCharacteristic> bluetoothDeviceHashMapGattChar = new HashMap<String, BluetoothGattCharacteristic>();

    private ArrayList<BleConnectionStatus> mBleConnectionStatusList = new ArrayList<BleConnectionStatus>();


    public void addBLEServiceToApplicationInterfaceListener(BleConnectionStatus mBleConnectionStatus) {
        Log.d(TAG, "Add Ble Service To app Interface Listener.");
        if (!mBleConnectionStatusList.contains(mBleConnectionStatus))
            mBleConnectionStatusList.add(mBleConnectionStatus);
    }

    public void removeBLEServiceToApplicationInterfaceListener(BleConnectionStatus mBleConnectionStatus) {
        Log.d(TAG, "Remove Ble Service To app Interface Listener.");
        if (mBleConnectionStatusList.contains(mBleConnectionStatus))
            mBleConnectionStatusList.remove(mBleConnectionStatus);
    }

    public void fireOnBLEConnectionSuccess(BluetoothGattCharacteristic status) {
        Log.d(TAG, "fire On BLE Conenction Success");
        for (BleConnectionStatus mListener : mBleConnectionStatusList) {
            mListener.onConnectionSuccess(status);
        }

    }

    public void fireOnBLEDisConnectionSuccess(int status) {
        Log.d(TAG, "fire On BLE Disconenction Success");
        for (BleConnectionStatus mListener : mBleConnectionStatusList) {
            mListener.onDisconnectionSuccess(status);
        }

    }

    public void removelistener(BleConnectionStatus bleConnectionStatus) {
        removeBLEServiceToApplicationInterfaceListener(bleConnectionStatus);
    }


    // connection change and services discovered.
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        //@TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                Log.d(TAG, "Connected to GATT client. Attempting to start service discovery");
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                    gatt.requestMtu(180);
//                }
//                // gatt.discoverServices();
//            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                Log.d(TAG, "Disconnected from GATT client");
//                    if(status == 133)
//                    {
//                        gatt_status_133 = true;
//                    }
//                    else{
//                        mConnectionState = STATE_DISCONNECTED;
//                        LibreLogger.d(this,"BleCommunication in suma ble state turning disconnected");
//                        Log.i(TAG, "Disconnected from GATT server.");
//                        ShowConnectionLost();
//                }
//                // mListener.onConnected(false);
//            }
            String intentAction;
            LibreLogger.d(this, "Connected to GATT client. Attempting for connection state change\n" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT client. Attempting to start service discovery");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    gatt.requestMtu(180);
                }
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
               // broadcastUpdate(intentAction);
             //suma commenting to get ble timeout issue   fireOnBLEConnectionSuccess(bluetoothDeviceHashMapGattChar.get(mBluetoothDeviceAddress));

                BluetoothGattService service = gatt.getService(BLEGattAttributes.MAVID_BLE_SERVICE);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(BLEGattAttributes.MAVID_BLE_CHARACTERISTICS);
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        Log.d("Bluetooth", "characteristics service\n" + characteristic);

                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BLEGattAttributes.MAVID_BLE_CHARACTERISTICS);
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        }
                        putGattCharacteristic(characteristic, mBluetoothDeviceAddress);
                        fireOnBLEConnectionSuccess(characteristic);
                    }
                }
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
//                Runnable runnable = new Runnable() {
//                    @Override
//                    public void run() {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mBluetoothAdapter.isEnabled()) {
//                            mBleScanner = mBluetoothAdapter.getBluetoothLeScanner();
//                            mBleScanner.startScan(null, new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), mLeScanCallback);
//                        }
//                    }
//                };
//                mMainHandler.postDelayed( runnable,5000);

                Log.d(TAG, "Disconnected from GATT client");
                if (status == 133) {
                    gatt_status_133 = true;
//                    if(MavidApplication.isACLDisconnected) {
//                        gatt.close();
//                    }
//                    Runnable runnable = new Runnable() {
//                        @Override
//                        public void run() {
//                       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mBluetoothAdapter.isEnabled()) {
//                                mBleScanner = mBluetoothAdapter.getBluetoothLeScanner();
//                               mBleScanner.stopScan(mLeScanCallback);
//
////                                mBleScanner.startScan(null, new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), mLeScanCallback);
////                            }
//                 }
//                    };
//                    mMainHandler.postDelayed( runnable,5000);

                } else {
                    mConnectionState = STATE_DISCONNECTED;
                    LibreLogger.d(this, "BleCommunication in suma ble state turning disconnected");
                    Log.i(TAG, "Disconnected from GATT server BLE Services.");
                    // mListener.onConnected(false);
                }
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mBluetoothAdapter.isEnabled()) {
//                    mBleScanner = mBluetoothAdapter.getBluetoothLeScanner();
//                    mBleScanner.stopScan(mLeScanCallback);
//
////                                mBleScanner.startScan(null, new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), mLeScanCallback);
////                            }
//                }
                Log.i(TAG, "Disconnected from GATT server else out");
                broadcastUpdate(intentAction);
                fireOnBLEDisConnectionSuccess(status);
            }

        }

        public String byteToHexString(byte[] data) {
            StringBuilder res = new StringBuilder(data.length*2);
            int lower4 = 0x0F; //mask used to get lowest 4 bits of a byte
            for(int i=0; i<data.length; i++) {
                int higher = (data[i] >> 4);
                int lower = (data[i] & lower4);
                if(higher < 10) res.append((char)('0' + higher));
                else            res.append((char)('A' + higher - 10));
                if(lower < 10)  res.append((char)('0' + lower));
                else            res.append((char)('A' + lower - 10));
                res.append(' '); //remove this if you don't want spaces between bytes
            }
            return res.toString();
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {


            if (status == BluetoothGatt.GATT_SUCCESS) {
                boolean connected = false;

                BluetoothGattService service = gatt.getService(BLEGattAttributes.MAVID_BLE_SERVICE);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(BLEGattAttributes.MAVID_BLE_CHARACTERISTICS);
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        Log.d("Bluetooth", "characteristics service\n" + characteristic);

                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BLEGattAttributes.MAVID_BLE_CHARACTERISTICS);
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            connected = gatt.writeDescriptor(descriptor);
                            Log.d("Bluetooth", "characteristics service connected device\n" + connected);

                        }
//                        connectionStatus.onConnectionSuccess(true);
                        putGattCharacteristic(characteristic, mBluetoothDeviceAddress);
                        fireOnBLEConnectionSuccess(characteristic);
                        LibreLogger.d(this, "Disconnected Connectedfrom GATT server on connection success set value");

                    }
                }
//                mListener.onConnected(connected);
            } else {
                Log.d(TAG, "onServicesDiscovered received: " + status);
            }
        }

        private void putGattCharacteristic(BluetoothGattCharacteristic gattCharacteristic, String btMacAddress) {
            Log.d(TAG, "putGattCharacteristic " + btMacAddress + " Id " + gattCharacteristic.hashCode());
            if (!bluetoothDeviceHashMapGattChar.containsKey(btMacAddress)) {
                Log.d(TAG, "putGattCharacteristic " + btMacAddress + " Id " + "is it removing");
                bluetoothDeviceHashMapGattChar.remove(btMacAddress);
            }
            bluetoothDeviceHashMapGattChar.put(btMacAddress, gattCharacteristic);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("Bleeeeee", "call onMtuChanged");
                gatt.discoverServices();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readCounterCharacteristic(characteristic);
                LibreLogger.d(this,"on read characteristics step1");
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            readCounterCharacteristic(characteristic);
            String str = "Hi";
            LibreLogger.d(this,"on read characteristics step2 characteristics\n"+characteristic.toString());
            LibreLogger.d(this, "Writing was successfullTwo read value"+"characteristics\n"+characteristic.toString());

            byte[] byteArr = str.getBytes();
//            WriteValueInternal(byteArr,characteristic,gatt );
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            /*if (DESCRIPTOR_CONFIG.equals(descriptor.getUuid())) {
                BluetoothGattCharacteristic characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(CHARACTERISTIC_COUNTER_UUID);
                gatt.readCharacteristic(characteristic);
            }*/
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            // BluetoothGattCharacteristic tChar = syncService.getCharacteristic(SYNC_HEIGHT_INPUT_CHAR);

            // if (characteristic == null) throw new AssertionError("characteristic null when sync time!");

            // use one of them in regards of the Characteristic's property
            // characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            //tChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);


            //characteristic.setValue(0, BluetoothGattCharacteristic.FORMAT_SINT32, 0);
            // gatt.writeCharacteristic(characteristic);
//            if (status == GattStatus.Success) {
            Log.d(TAG, "Writing was successfullTwo" + status);
//            } else {
//                var errorCode = status;
//                //Process error...
////            }


//            if (status == BluetoothGatt.GATT_SUCCESS ) {
////Write operation successful - process with next chunk!
//                Log.d(TAG,"Writing was successfullTwo success if" + status);
//
//            }
//            else{
//                Log.d(TAG,"Writing was successfullTwo success else" + status);
//
//            }


            if (status == BluetoothGatt.GATT_SUCCESS) {
                boolean connected = false;

                BluetoothGattService service = gatt.getService(BLEGattAttributes.MAVID_BLE_SERVICE);
                if (service != null) {
                    characteristic = service.getCharacteristic(BLEGattAttributes.MAVID_BLE_CHARACTERISTICS);
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true);

                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BLEGattAttributes.MAVID_BLE_CHARACTERISTICS);
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            // descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

                            connected = gatt.writeDescriptor(descriptor);
                        }
                        // connectionStatus.onConnectionSuccess(true);

                    }
                }
//                mListener.onConnected(connected);
            } else {
                Log.d(TAG, "onServicesDiscovered received: " + status);
            }

        }

        private void readCounterCharacteristic(BluetoothGattCharacteristic characteristic) {
           if (BLEGattAttributes.MAVID_BLE_CHARACTERISTICS.equals(characteristic.getUuid())) {
               byte[] data = characteristic.getValue();
               Log.d("Bluetooth", "data  length:\n " + data.length + "real data value" + data.getClass().getName() + "characteristics\n" + characteristic.getUuid());
               // int value = Ints.fromByteArray(data);
               try {
                   Thread.sleep(200);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
               try {
                   bleReadInterface.onReadSuccess(data);
                   //getDataLengthSuma(value);
                   int response2 = getDataLength(data);
                   for ( int i = 0; i< data.length; i++){
//                       if ( arrayOFInt[i]  == key ){
//                           System.out.println("Key Found in arrayOFInt = " + arrayOFInt[i] );
//                           count ++;
//                       }
                   }
                   LibreLogger.d(this, "suma in ble configure activity get the value\n" + response2);
                   if (response2 == 14) {
                       MavidApplication.bleSACTimeout = true;
                       //StopSAC
//                    BleCommunication bleCommunication = new BleCommunication(this);
//                    BleCommunication.writeInteractorStopSac();
                   } else {
                       MavidApplication.bleSACTimeout = false;
                   }
//                   try {
//                       Thread.sleep(500);
//                   } catch (InterruptedException e) {
//                       e.printStackTrace();
//                   }


                  EventBus.getDefault().post(new BLEEvntBus(data));

                  int  finalPayloadLength = getDataLength(data);
                 //  finalPayloadLength = getDataLength(data);
                   int response = getDataLength(data);
                 // baos.flush();
                  // if (baos.size() <= finalPayloadLength) {
                      // if (getDataLength(data) != 10 /*&& value[2] != 1*/) {

                           baos.write(data);
//                           LibreLogger.d(this, "SUMA IN SSID in read  response data value in writting  DATA\n");
//                        getSSIDList();
                   bytesToHex(data);
                   String hex = "75546f7272656e745c436f6d706c657465645c6e667375635f6f73745f62795f6d757374616e675c50656e64756c756d2d392c303030204d696c65732e6d7033006d7033006d7033004472756d202620426173730050656e64756c756d00496e2053696c69636f00496e2053696c69636f2a3b2a0050656e64756c756d0050656e64756c756d496e2053696c69636f303038004472756d2026204261737350656e64756c756d496e2053696c69636f30303800392c303030204d696c6573203c4d757374616e673e50656e64756c756d496e2053696c69636f3030380050656e64756c756d50656e64756c756d496e2053696c69636f303038004d50330000";
                  String hex1=bytesToHex(data);
                   StringBuilder output = new StringBuilder();
                   for (int i = 0; i < hex1.length(); i+=2) {
                       String str = hex1.substring(i, i+2);
                       output.append((char)Integer.parseInt(str, 16));
                   }
                   System.out.println(output);
                   LibreLogger.d(this, "Swetha BT and SUMA IN SSID in read response dataValue READ DATA\n"+bytesToHex(data));

                   LibreLogger.d(this, "Swetha BT and SUMA IN SSID in read response dataValue conversion\n"+output);

                   String finalSsidList = null;
                           if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                               finalSsidList = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                               String input = "test string (67hghjgigliugliugliug)";
                               input = finalSsidList.substring(finalSsidList.indexOf(" {\"Items\"")+1, finalSsidList.lastIndexOf("]}"));
                               System.out.println(input);
                               LibreLogger.d(this, "Swetha BT and SUMA IN SSID in READ DATA\n"+input);

//                               // strips off all non-ASCII characters
//                               finalSsidList = finalSsidList.replaceAll("[^\\x00-\\x7F]", "");
//                               LibreLogger.d(this, "SUMA IN SSID in read  response data READING DATA NONASCII\n"+finalSsidList.trim());
//
//                               // erases all the ASCII control characters
//                               finalSsidList = finalSsidList.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
//                               LibreLogger.d(this, "SUMA IN SSID in read  response data READING DATA ASCII CONTROL CHARSXC\n"+finalSsidList.trim());
//
//                               // removes non-printable characters from Unicode
//                               finalSsidList = finalSsidList.replaceAll("\\p{C}", "");
//                               LibreLogger.d(this, "SUMA IN SSID in read  response data READING DATA UNICODE CHARScj\n"+finalSsidList.trim());

                               //  LibreLogger.d(this, "SUMA IN SSID in read  response reading VALUE BYTEARRAY\n"+ Arrays.toString(baos.toByteArray()));

                             //  LibreLogger.d(this, "SUMA IN SSID in read  response reading VALUE\n"+finalSsidList);

                           }
                           //getIntentParams(finalSsidList);
                           //suma
                           //   d("Bluetooth", "bluetooth suma in finalpayload else in if try response  do next response scanlist getdatalengthvalue! 10" + response);
                     //  }
                 //  }




//                   String finalSsidList = null,finalSsidList1 = null;
//                   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                       finalSsidList = new String(data, StandardCharsets.ISO_8859_1);
//                   }
                 // if(finalSsidList.equals(""))
                   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                       finalSsidList = new String(data, StandardCharsets.ISO_8859_1);
                     //  finalSsidList1 = new String(finalSsidList.getBytes(), StandardCharsets.UTF_8);
                        Log.d("READ SCAN BLE DATA", "SUMA IN SSID in read response junk remove ISO\n" + finalSsidList);
                     //   Log.d("READ SCAN BLE DATA", "SUMA IN SSID in read response junk remove UTF8\n" + finalSsidList1);

//                       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                           String encodeBytes = Base64.getEncoder().encodeToString(finalSsidList.getBytes());
//                       }
                       if (getDataLength(data) != 10 /*&& value[2] != 1*/) {
                           ByteArrayOutputStream baos_SUMA = new ByteArrayOutputStream();
                           baos_SUMA.write(data);
                          // getSSIDList();
                           String finalSsidListSUMA= null;
                           finalSsidListSUMA = new String(baos_SUMA.toByteArray(), StandardCharsets.UTF_8);
                           LibreLogger.d(this,"suma in ssid read response data value"+finalSsidListSUMA);
                           //suma
                           //   d("Bluetooth", "bluetooth suma in finalpayload else in if try response  do next response scanlist getdatalengthvalue! 10" + response);
                       }


                       // "When I do a getbytes(encoding) and "

//                       System.Text.Encoding iso_8859_1 =finalSsidList.Encoding.GetEncoding("iso-8859-1");
//                       System.Text.Encoding utf_8 = System.Text.Encoding.UTF8;
//
//                       // Unicode string.
//                       string s_unicode = "abcéabc";
//
//                       // Convert to ISO-8859-1 bytes.
//                       byte[] isoBytes = iso_8859_1.GetBytes(s_unicode);
//
//                       // Convert to UTF-8.
//                       byte[] utf8Bytes = System.Text.Encoding.Convert(iso_8859_1, utf_8, isoBytes);





                      // String finalSUMAString= new String(finalSsidList("ISO-8859-1"), "UTF-8");

                     //  finalConnectedArray = new String(value, "UTF-8");
//                       finalConnectedIndex = finalSsidList.indexOf("{\"Items\"");
//                       finalConnectedString = finalSsidList.substring(finalConnectedIndex);
//
//                       finalConnectedIndex1 = finalSsidList.indexOf("]}");
//                       finalConnectedString1 = finalSsidList.substring(finalConnectedIndex1);
//                       if(finalSsidList.startsWith("{\"Items\"") || finalSsidList.endsWith("]}")) {
//                           EventBus.getDefault().post(new BLEEvntBus(data));
//                           Log.d("READ SCAN BLE DATA", "SUMA IN SSID in read response check substring last ONE\n"+finalSsidList);
//                       }
//                      if(finalSsidList.contains(Character.UnicodeBlock.SPECIALS.toString())){
//                          Log.d("READ SCAN BLE DATA", "SUMA IN SSID in read response check substring last ONE\n"+finalSsidList.contains(Character.UnicodeBlock.SPECIALS.toString()));
//                      }
                      // Log.d("READ SCAN BLE DATA", "SUMA IN SSID in read response check substring last ONE\n" +finalConnectedString1+"first half\n" +finalConnectedString);

//                       sumaString = finalSsidList.replaceAll("��\u0001\u0001;", "");
//                       sumaString1 = finalSsidList.replaceAll("��", "");
//                       sumaString3 = finalSsidList.replaceAll("\uFFFD", "\"");

                      // Log.d("READ SCAN BLE DATA", "SUMA IN SSID in read response junk remove\n" + sumaString3);

                   }

                   }
                   catch (IOException e) {
                e.printStackTrace();
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                Log.d("Bluetooth", "data  length exception:\n " + data.length + "real data value" + data.getClass().getName());
            }
//                mListener.onCounterRead(value);
        }
        }

    };

    public int getDataLength(byte[] buf) {
        byte b1 = buf[3];
        byte b2 = buf[4];
        short s = (short) (b1 << 8 | b2 & 0xFF);

        LibreLogger.d("Bluetooth", "Data length is returned as s" + s);
        return s;
    }

    private static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");

        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }

        return output.toString();
    }
    public int getDataLengthSuma(byte[] buf) {
        short s = 0;
        int buf0Unsigned = buf[0] & 0xFF;
        int buf1Unsigned = buf[1] & 0xFF;
        int totalBuf0AndBuf1 = buf0Unsigned + buf1Unsigned;
        if (totalBuf0AndBuf1 == 342) {
            byte b1 = buf[3];
            byte b2 = buf[4];
            s = (short) (b1 << 8 | b2 & 0xFF);

            LibreLogger.d(this, "Data length is returned as BLEServices as s" + s + "get buff 0\n" + buf0Unsigned + "buff 1\n" + buf1Unsigned);
            return s;
        }
        return s;

    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind ");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        if(MavidApplication.isACLDisconnected) {
            close();
            LibreLogger.d(this, "BleCommunication in suma ble state disconnected unbind services BLEServices ");
        }
        close();
        return super.onUnbind(intent);
    }

    public final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize(BleConnectionStatus connectionStatus, BleReadInterface bleReadInterface) {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.d(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        addBLEServiceToApplicationInterfaceListener(connectionStatus);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        //  if(mBluetoothAdapter.getProfileConnectionState(0));
        if (mBluetoothAdapter == null) {

            Log.d(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        this.connectionStatus = connectionStatus;
        this.bleReadInterface = bleReadInterface;

        return true;
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
//    public boolean connect(final String address) {
//        if (mBluetoothAdapter == null || address == null) {
//            Log.d(TAG, "BluetoothAdapter not initialized or unspecified address.");
//            return false;
//        }
//
//        // Previously connected device.  Try to reconnect.
//        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
//                && mBluetoothGatt != null) {
//            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
//            if (mBluetoothGatt.connect()) {
//                mConnectionState = STATE_CONNECTING;
//                return true;
//            } else {
//                return false;
//            }
//        }
//
//        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//        if (device == null) {
//            Log.d(TAG, "Device not found.  Unable to connect.");
//            return false;
//        }
//        // We want to directly connect to the device, so we are setting the autoConnect
//        // parameter to false.
//        mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
//        Log.d(TAG, "Trying to create a new connection.");
//        mBluetoothDeviceAddress = address;
//        mConnectionState = STATE_CONNECTING;
//        return true;
//    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean connect(final String address) {

        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
//            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            LibreLogger.d(this, "BleCommunication in suma ble state turning existing conn");
//            if (mBluetoothGatt.connect()) {
//                mConnectionState = STATE_CONNECTING;
//                return true;
//            } else {
//                mConnectionState = STATE_DISCONNECTED;
//                LibreLogger.d(this,"BleCommunication in suma ble state turning disconnected in connect to gatt");
//                return false;
//            }
            if (bluetoothDeviceHashMapGattChar.containsKey(address)) {
                Log.d(TAG, "connect");
                fireOnBLEConnectionSuccess(bluetoothDeviceHashMapGattChar.get(address));
                return true;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
//        if(TTTUtilities.isLollipopOrAbove()) {
//            // Little hack with reflect to use the connect gatt with defined transport in Lollipop
//            Method connectGattMethod = null;
//
//            try {
//                connectGattMethod = device.getClass().getMethod("connectGatt", Context.class, boolean.class, BluetoothGattCallback.class, int.class);
//            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
//            }
//
//            try {
//                mBluetoothGatt = (BluetoothGatt) connectGattMethod.invoke(device, BluetoothConnectService.this, false, mGattCallback, TRANSPORT_LE);
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            } catch (IllegalArgumentException e) {
//                e.printStackTrace();
//            } catch (InvocationTargetException e) {
//                e.printStackTrace();
//            }
//        } else  {
//            mBluetoothGatt = device.connectGatt(BluetoothConnectService.this, true, mGattCallback);
//        }
//

        mBluetoothGatt = device.connectGatt(this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
//        ScanSettings settings = new ScanSettings.Builder()
//                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
//                .setReportDelay(400)
//                .build();
//        settings.getScanResultType();
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;

//        Handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
        if (gatt_status_133) {
            Log.d(TAG, "Catch issue");
            //close();
//            mBluetoothGatt.discoverServices();
//            mBluetoothGatt.getDevice();
            mBluetoothGatt = device.connectGatt(this, true, mGattCallback, BluetoothDevice.TRANSPORT_LE);
//            settings = new ScanSettings.Builder()
//                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
//                    .setReportDelay(400)
//                    .build();
//            settings.getScanResultType();

//            Runnable runnable = new Runnable() {
//                @Override
//                public void run() {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mBluetoothAdapter.isEnabled()) {
//                        mBleScanner = mBluetoothAdapter.getBluetoothLeScanner();
//                        mBleScanner.startScan(null, new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), mLeScanCallback);
//                    }
//                }
//            };
//            mMainHandler.postDelayed( runnable,5000);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mBluetoothAdapter.isEnabled()) {
//                mBleScanner = mBluetoothAdapter.getBluetoothLeScanner();
//                mBleScanner.stopScan(mLeScanCallback);
//
////                                mBleScanner.startScan(null, new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), mLeScanCallback);
////                            }
//            }
            connect(address);
            gatt_status_133 = false;
        }

//            }
//        }, 4000);

        return true;
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
//        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
//            int flag = characteristic.getProperties();
//            int format = -1;
//            if ((flag & 0x01) != 0) {
//                format = BluetoothGattCharacteristic.FORMAT_UINT16;
//                Log.d(TAG, "Heart rate format UINT16.");
//            } else {
//                format = BluetoothGattCharacteristic.FORMAT_UINT8;
//                Log.d(TAG, "Heart rate format UINT8.");
//            }
//            final int heartRate = characteristic.getIntValue(format, 1);
//            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
//            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
//        } else {
//            // For all other profiles, writes the data formatted in HEX.
//            final byte[] data = characteristic.getValue();
//            if (data != null && data.length > 0) {
//                final StringBuilder stringBuilder = new StringBuilder(data.length);
//                for(byte byteChar : data)
//                    stringBuilder.append(String.format("%02X ", byteChar));
//                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
//            }
//        }
        sendBroadcast(intent);
    }

    private void ShowConnectionLost() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle("");
        builder.setMessage("Connection Lost to the Device");
        builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                startActivity(new Intent(getApplicationContext(), MavidHomeTabsActivity.class));
                // finish();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
      //  mBluetoothGatt.disconnect();
        mBluetoothGatt = null;
        // Toast.makeText(getApplicationContext(), "BT Disconnected", Toast.LENGTH_SHORT).show();

    }
    private static String bytesToHex(byte[] hashInBytes) {

        StringBuilder sb = new StringBuilder();
        for (byte b : hashInBytes) {
            //System.out.println(b & 0xFF);
            // System.out.println(String.format("%02x", b));
            sb.append(String.format("%02x", b));


        }

        return sb.toString();

    }
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (result.getDevice().getAddress().equals(mBluetoothDeviceAddress)) {
                  //  mMainHandler.post(mConnectRunnable);
                    if (mBleScanner != null) {
                        mBleScanner.stopScan(mLeScanCallback);
                        mBleScanner = null;
                    }
                }
            }
        }
    };

    public BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {

                }
            };


}