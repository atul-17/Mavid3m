package com.mavid.libresdk.TaskManager.Communication;

import android.content.Context;
import android.net.Network;
import android.os.AsyncTask;
import android.util.Log;

import com.mavid.libresdk.TaskManager.Communication.Listeners.CommandStatusListener;
import com.mavid.libresdk.TaskManager.Communication.Listeners.CommandStatusListenerWithResponse;
import com.mavid.libresdk.TaskManager.Communication.Packet.Decoder.MavidPacketDecoder;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.MessageInfo;
import com.mavid.libresdk.Util.LibreLogger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by bhargav on 9/2/18.
 */

public class HttpMavidClient extends AsyncTask<String, Void, String> {

    private String deviceIp;
    private Context context;
    CommandStatusListenerWithResponse commandStatusListenerWithResponse;
    CommandStatusListener commandStatusListener;
    byte[] dataPacket;
    Network network;
    public HttpMavidClient(String deviceIp , byte[] dataPacket, CommandStatusListenerWithResponse commandStatusListener){
        this.context = context;
        this.deviceIp = deviceIp;
        this.commandStatusListenerWithResponse = commandStatusListener;
        this.dataPacket = dataPacket;
    }

    public HttpMavidClient(String deviceIp , byte[] dataPacket, CommandStatusListenerWithResponse commandStatusListener,Network network){
        this.context = context;
        this.deviceIp = deviceIp;
        this.commandStatusListenerWithResponse = commandStatusListener;
        this.dataPacket = dataPacket;
        this.network = network;
    }

    public HttpMavidClient(String deviceIp , byte[] dataPacket, CommandStatusListener commandStatusListener){
        this.context = context;
        this.deviceIp = deviceIp;
        this.commandStatusListener = commandStatusListener;
        this.dataPacket = dataPacket;
    }
    @Override
    protected String doInBackground(String... strings) {

        Log.d("MavidCommunication","firing api");
        final String BASE_URL = "http://" + deviceIp + "/msgbox";
        Log.d("MavidCommunication","POST to "+deviceIp+". For base URL :"+BASE_URL);
        String inputLine;
        URL myUrl = null;
        HttpURLConnection connection = null;
        try {

            myUrl = new URL(BASE_URL);
            if (network!=null){
                connection = (HttpURLConnection) network.openConnection(myUrl);
            }else{
                connection =(HttpURLConnection) myUrl.openConnection();
            }

            connection.setRequestMethod("POST");
            connection.setReadTimeout(30000);
            connection.setConnectTimeout(30000);
            String urlParameters = "";
            // Send post request
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.write(this.dataPacket);
            wr.flush();
            wr.close();

            int responseCode = connection.getResponseCode();
            Log.d("MavidCommunication","\nSending 'POST' request to URL : " + myUrl);
            Log.d("MavidCommunication","Post parameters : " + urlParameters);
            Log.d("MavidCommunication","Response Code : " + responseCode);

            if (responseCode==200) {
                // sending http data is successfull
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendSuccess();
                    }
                }).start();
               if (commandStatusListenerWithResponse==null){
                   return null;
               }
            }else{
                sendFailure(new Exception("Error "+responseCode));
                LibreLogger.d("MavidCommunication","suma in Http mavid client errror \n"+responseCode);
                return null;
            }
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            byte[] dataArr = new byte[4026];

            InputStream is = connection.getInputStream();
            int t = -1;
            int datalen = 0;

            while ((t =  is.read(dataArr,datalen,dataArr.length-datalen)) > 0) {
                // t is the length of read data
                datalen+=t;
            }
            in.close();
            if (commandStatusListenerWithResponse!=null ) {
                commandStatusListenerWithResponse.response(new MessageInfo(deviceIp, getPayload(dataArr, datalen)));
            }
        } catch (java.net.SocketTimeoutException e) {
            sendFailure(e);
        }  catch (Exception e) {
            e.printStackTrace();
            Log.d("MavidCommunication"," cred res: "+e.toString());
            sendFailure(e);
        }
        return null;
    }

    private void sendSuccess(){
        if (commandStatusListener!=null){
            Log.d("MavidCommunication","Sending success");
            this.commandStatusListener.success();
        }else if(commandStatusListenerWithResponse!=null) {
            Log.d("MavidCommunication","Sending success. Expect response");
            commandStatusListenerWithResponse.success();
        }
    }
    private void sendFailure(Exception e){
        if (commandStatusListener!=null){
            Log.d("MavidCommunication","Sending failure");
            commandStatusListener.failure(e);
        }else if(commandStatusListenerWithResponse!=null) {
            Log.d("MavidCommunication","Sending failure . Expect response");
            commandStatusListenerWithResponse.failure(e);
        }
    }
    private String getPayload(byte[] dataArr,int dataLength) {
        MavidPacketDecoder mavidPacketDecoder = new MavidPacketDecoder(dataArr,dataLength);
        return mavidPacketDecoder.getPayload();
    }


    @Override
    protected void onPostExecute(String result) {
        //  httpHandler.onResponse(result);

    }
}
