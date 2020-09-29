package com.mavid.utility;

import com.mavid.Constants.Constants;
import com.mavid.libresdk.LibreMavidHelper;
import com.mavid.libresdk.Util.LibreLogger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by bhargav on 5/3/18.
 */

public class SACUtils {
    private static String ProductIdText;

    public static boolean isSACNetwork(String ssid){
        if (ssid.contains(Constants.WAC_SSID) || ssid.contains(Constants.FAIL_SAFE_WAC_SSID)
                ||ssid.contains(Constants.WAC_SSID2)||ssid.contains(Constants.WAC_SSID3)
                ||ssid.contains(Constants.WAC_SSID4)){
            if(ssid.contains(Constants.WAC_SSID)){
                ProductIdText="Mavid_Libre";
                LibreLogger.d("SACUtils","SAC Utils in MAVID LIBE");
            }
            if(ssid.contains(Constants.WAC_SSID2)){
                ProductIdText="Microdot_Beta";
                LibreLogger.d("SACUtils","SAC Utils in MICRODOT");

            }
            if(ssid.contains(Constants.WAC_SSID3)||ssid.contains(Constants.WAC_SSID4)){
                ProductIdText="Wave_Beta";
                LibreLogger.d("SACUtils","SAC Utils in WAVEConfigure");

            }
            /*
             * Suma: Below Method : Generating the symmentric key from product ID dynamically using MD5 algorithmn
             *

             *
             * Start of code
             *
             * */
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            byte[] hashInBytes = new byte[0];
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                if (md != null) {
                    if(ProductIdText!=null) {
                        hashInBytes = md.digest(ProductIdText.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            // bytes to hex
            StringBuilder sb = new StringBuilder();
            for (byte b : hashInBytes) {
                sb.append(String.format("%02x", b));
            }
            LibreMavidHelper.symmentricKey=sb.toString();
            /*End of the code*/

            return true;
        }
        return false;
    }
}
