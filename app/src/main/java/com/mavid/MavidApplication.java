package com.mavid;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.util.Log;

import com.mavid.utility.DB.MavidNodes;
import com.mavid.utility.FirmwareClasses.UpdatedMavidNodes;
import com.mavid.libresdk.Exceptions.WifiException;
import com.mavid.libresdk.LibreMavidHelper;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.DeviceListener;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.DeviceInfo;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.MessageInfo;
import com.mavid.libresdk.Util.LibreLogger;

import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;


/**
 * Created by bhargav on 14/2/18.
 */

public class MavidApplication extends Application {

    public static byte [] bb ;
    private static DeviceListener deviceListener;
    public static boolean mobileDataEnabled = false;
    public static boolean hotSpotAlexaCalled = false;
    public static boolean  btChangeSource= false;
    public static boolean  btChangeSink= false;
    public static boolean bleSACTimeout=false;
    public static boolean  readWifiStatusNotification= false;
    public static boolean  readWifiStatusNotificationBLE= false;
    public static String broadCastAddress;
   public  static boolean doneLocationChange=false;
    public static boolean  credentialSent= false;

    public static boolean  readWifiStatus= false;
    public static boolean  btChangeConnectedStatus= false;
    private ArrayList<DeviceInfo> deviceInfoMavidApplicationList = new ArrayList<>();
 public static String LOCAL_IP = "";
    public static String mDeviceAddress;
    public static String lookFor;

    public static LinkedHashMap<String, UpdatedMavidNodes> updateHashMap = new LinkedHashMap<>();
    public static boolean isACLDisconnected=false;
    public static boolean isDeviceFound=false;
    public static boolean isFwNeeded=false,checkIsFwBtnLatest=false;
    public static boolean fwupdatecheck=false;
    public static boolean alreadyExecuted=false;
    public static boolean urlFailure=false;
    public static boolean fwupdatecheckPrivateBuild=false;
    /*this exception saves the exception received from startDiscovery() and throws it when
    a new interface is referred through setDeviceListener*/
    private static ArrayList<Exception> cacheException =  new ArrayList<>();
    @Override
    public void onCreate() {

        super.onCreate();
       // Fabric.with(this,new Crashlytics());
       // Crashlytics.getInstance().crash();
        try {
            LibreMavidHelper.startDiscovery(new DeviceListener() {
                @Override
                public void newDeviceFound(final DeviceInfo deviceInfo) {
                    DeviceListener deviceListener1 = getDeviceListener();
                    if (deviceListener1!=null){
                        deviceListener1.newDeviceFound(deviceInfo);
                        deviceInfoMavidApplicationList.add(deviceInfo);

                    }
                    // add this to DB
                    MavidNodes.getInstance().addToDeviceInfoMap(deviceInfo.getIpAddress(),deviceInfo);

                    /*For a robust listening*/
                }

                @Override
                public void deviceGotRemoved(DeviceInfo deviceInfo) {

                }

                @Override
                public void deviceDataReceived(MessageInfo messageInfo) {

                }

                @Override
                public void failures(Exception e) {
                    DeviceListener deviceListener1 = getDeviceListener();
                    if (deviceListener1!=null) {
                        deviceListener1.failures(e);
                    }else{
                        cacheException.add(e);
                    }
                }

                @Override
                public void checkFirmwareInfo(DeviceInfo deviceInfo) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        final ConnectivityManager connection_manager =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest.Builder request = null;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

//            request = new NetworkRequest.Builder();
//
//            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

//            connection_manager.registerNetworkCallback(request.build(), new ConnectivityManager.NetworkCallback() {
//
//                @Override
//                public void onAvailable(Network network) {
//                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
//                        if(isMobileDataEnabled()) {
//                            ConnectivityManager.setProcessDefaultNetwork(network);
//                            LibreLogger.d(this,"suma in disable nw first");
//                        }
//                        else{
//                            ConnectivityManager.setProcessDefaultNetwork(network);
//                            LibreLogger.d(this,"suma in disable nw sec");
//
//                        }
//                    }
//
//                    WifiManager wifiMan = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//                    WifiInfo wifiInf = wifiMan.getConnectionInfo();
//                    int ipAddress = wifiInf.getIpAddress();
//                    Log.d("suma_ip_address_if",String.valueOf(wifiInf.getIpAddress()));
//                    String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
//                    MavidApplication.LOCAL_IP = ip;
//
//                    LibreLogger.d(this,"Karuna" + "App Called Here Device");
//
//                }
//           });


//        } else {
//            LibreLogger.d(this, "Karuna" + "App Called Here Device 1");
//            WifiManager wifiMan = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//            WifiInfo wifiInf = wifiMan.getConnectionInfo();
//            int ipAddress = wifiInf.getIpAddress();
//            Log.d("suma_ip_address_else",String.valueOf(wifiInf.getIpAddress()));
//            String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
//            MavidApplication.LOCAL_IP = ip;
//
//
//        }
        if(MavidApplication.LOCAL_IP.isEmpty()) {
            try {
                MavidApplication.LOCAL_IP = getIPAddress();
            } catch (WifiException e) {
                e.printStackTrace();
            }
        }
        LibreLogger.d(this,"Karuna" + "App Called Here Device::: " + MavidApplication.LOCAL_IP);

     //   musicServer = getMusicServer();
    }



    @Override
    public void onTerminate() {
        super.onTerminate();
    }
        private boolean isMobileDataEnabled() {
            boolean mobileDataEnabled = false;
            ConnectivityManager cm1 = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info1 = cm1.getActiveNetworkInfo();
            if (info1 != null) {
                if (info1.getType() == ConnectivityManager.TYPE_MOBILE) {
                    try {
                        Class cmClass = Class.forName(cm1.getClass().getName());
                        Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
                        method.setAccessible(true);
                        mobileDataEnabled = (Boolean) method.invoke(cm1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
            return mobileDataEnabled;
        }


        public String getIPAddress() throws WifiException {
        try {
            InetAddress mAddress = getLocalV4Address(getActiveNetworkInterface());
            String ipAddress =  mAddress.getHostAddress();
           Log.d("suma_ip_address3",ipAddress);
            return ipAddress;
        }catch (Exception e){
            WifiException wifiException = new WifiException("Getting invalid IPAddress. Check if wifi is enabled");
            wifiException.setStackTrace(e.getStackTrace());
            throw wifiException;
        }
    }
    public static InetAddress getLocalV4Address(NetworkInterface netif) {


        Enumeration addrs;
        try {
            addrs = netif.getInetAddresses();
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
        while (addrs.hasMoreElements()) {
            InetAddress addr = (InetAddress) addrs.nextElement();
            if (addr instanceof Inet4Address && !addr.isLoopbackAddress())
                return addr;
        }
        return null;
    }
//    public static NetworkInterface getActiveNetworkInterface() throws SocketException {
//
//        Enumeration<NetworkInterface> interfaces = null;
//        try {
//            interfaces = NetworkInterface.getNetworkInterfaces();
//        } catch (SocketException e) {
//            return null;
//        }
//
//        while (interfaces.hasMoreElements()) {
//            NetworkInterface iface = interfaces.nextElement();
//            Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
//            Log.d("ip check"," in getActiveInterface "+iface.getName());
//
//            /* Check if we have a non-local address. If so, this is the active
//             * interface.
//             *
//             * This isn't a perfect heuristic: I have devices which this will
//             * still detect the wrong interface on, but it will handle the
//             * common cases of wifi-only and Ethernet-only.
//             */
//            //for wlan0 interface
//            if ((iface.getName().startsWith("w")&& iface.isUp())
//                    //for softAP interface
//                    || (iface.getName().startsWith("sof") && iface.isUp())
//                    //for p2p interface
//                    || (iface.getName().startsWith("p")&& iface.isUp())) {
//                //this is a perfect hack for getting wifi alone
//
//                while (inetAddresses.hasMoreElements()) {
//                    InetAddress addr = inetAddresses.nextElement();
//
//                    if (!(addr.isLoopbackAddress() || addr.isLinkLocalAddress())) {
//                        Log.d("LSSDP", "DisplayName" + iface.getDisplayName() + "Name" + iface.getName()+"addr" + addr+" Host Address" + addr.getHostAddress());
//
//                        return iface;
//                    }
//                }
//            }
//        }
//
//        return null;
//    }

    public NetworkInterface getActiveNetworkInterface() {

        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return null;
        }

        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();


            /* Check if we have a non-local address. If so, this is the active
             * interface.
             *
             * This isn't a perfect heuristic: I have devices which this will
             * still detect the wrong interface on, but it will handle the
             * common cases of wifi-only and Ethernet-only.
             */
            if (iface.getName().startsWith("w")) {
                //this is a perfect hack for getting wifi alone

                while (inetAddresses.hasMoreElements()) {
                    InetAddress addr = inetAddresses.nextElement();

                    if (!(addr.isLoopbackAddress() || addr.isLinkLocalAddress())) {
                        Log.d("LSSDP", "DisplayName" + iface.getDisplayName() + "Name" + iface.getName());

                        return iface;
                    }
                }
            }
        }

        return null;
    }



    private boolean isDiscoveredAlready(DeviceInfo deviceInfo) {
        deviceInfoMavidApplicationList.contains(deviceInfo);
        for (DeviceInfo info : deviceInfoMavidApplicationList){
            if (info.getIpAddress().equals(deviceInfo.getIpAddress())){
                return true;
            }
        }
        return false;
    }

    public DeviceListener getDeviceListener() {
        return deviceListener;
    }

    public static void setDeviceListener(DeviceListener deviceListen) {

        if (deviceListen==null){
            //first time register of devicelistener. Look for any exception in cache.
             hitExceptionsFromList(cacheException,deviceListen);
        }
        deviceListener = deviceListen;
    }

    private static void hitExceptionsFromList(ArrayList<Exception> cacheException, DeviceListener deviceListen) {
        //some exceptions are there in the cache. Hit all of them from the list.
        if (cacheException!=null && cacheException.size() > 0) {
            for (Exception e : cacheException){
                Log.d("hitExceptionsFromList","Hitting exception "+e.getMessage());
                deviceListen.failures(e);
            }
            cacheException.clear();
        }
    }



}
