package com.mavid;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.widget.AppCompatTextView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.danimahardhika.cafebar.CafeBar;
import com.google.gson.Gson;
import com.mavid.adapters.DeviceListAdapter;
import com.mavid.BLEApproach.BLEBlinkingFragment;
import com.mavid.irActivites.IRAddRemoteVPActivity;
import com.mavid.irActivites.IRRestoreSelectionActivity;
import com.mavid.models.ModelGetUserDetailsAppliance;
import com.mavid.models.ModelGetUserDetailsBodySucess;
import com.mavid.models.ModelRemoteDetails;
import com.mavid.utility.FirmwareClasses.CheckFirmwareInfoClass;
import com.mavid.utility.FirmwareClasses.DownloadMyXmlListener;
import com.mavid.utility.FirmwareClasses.FirmwareUpdateHashmap;
import com.mavid.utility.FirmwareClasses.UpdatedMavidNodes;
import com.mavid.utility.FirmwareClasses.XmlParser;
import com.mavid.alexa_signin.AlexaSignInActivity;

import com.mavid.alexa_signin.AlexaThingsToTryDoneActivity;
import com.mavid.libresdk.LibreMavidHelper;
import com.mavid.libresdk.TaskManager.Communication.Listeners.CommandStatusListenerWithResponse;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.DeviceListener;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.DeviceInfo;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.MessageInfo;
import com.mavid.libresdk.TaskManager.Discovery.CustomExceptions.WrongStepCallException;
import com.mavid.libresdk.Util.LibreLogger;
import com.mavid.utility.OnButtonClickCallback;
import com.mavid.utility.OnButtonClickListViewInterface;
import com.mavid.utility.UIRelatedClass;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class DeviceListFragment extends Fragment implements OnButtonClickListViewInterface {

    ListView deviceListView;
    private ArrayList<DeviceInfo> deviceInfoList = new ArrayList<>();
    DeviceListAdapter deviceListAdapter;
    TextView config_item, config_item1;
    //    TextView tvNoSpeakerFound;
    SwipeRefreshLayout refreshLayout;
    //    ImageView imagerefresh;

    private XmlParser myXml;
    private ArrayList<DeviceInfo> deviceInfoListSwipe = new ArrayList<>();

    UpdatedMavidNodes updateNode;

    FirmwareUpdateHashmap updateHash = FirmwareUpdateHashmap.getInstance();

    AppCompatTextView tv_connected_speakers_label;

    FrameLayout no_device_frame_layout;

    AppCompatTextView tv_refresh;

    AppCompatTextView tv_setup_speaker;

    CafeBar cafeBar;

    String TAG = DeviceListFragment.class.getSimpleName();
    UIRelatedClass uiRelatedClass = new UIRelatedClass();

    Gson gson = new Gson();

    private Dialog mDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_mavid_devices_list, container, false);
        deviceInfoList.clear();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);


        if (getActivity() != null) {
            ((MavidHomeTabsActivity) getActivity()).showProgressBar();
            Handler handler = new Handler();
            ((MavidHomeTabsActivity) getActivity()).ivRefresh.setVisibility(View.VISIBLE);

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (getActivity() != null) {
                        if (deviceInfoList.size() > 0) {
                            no_device_frame_layout.setVisibility(View.GONE);
                            tv_connected_speakers_label.setVisibility(View.VISIBLE);
                        } else {
                            tv_connected_speakers_label.setVisibility(View.GONE);
                            no_device_frame_layout.setVisibility(View.VISIBLE);
                            ((MavidHomeTabsActivity) getActivity()).dismissProgressBar();

                        }
                    }
                }
            };
            handler.postDelayed(runnable, 5000);
        }

        if (getContext() != null) {
            deviceListAdapter = new DeviceListAdapter(getContext(), deviceInfoList, DeviceListFragment.this, (MavidHomeTabsActivity) getActivity());
            deviceListView.setAdapter(deviceListAdapter);
            deviceListAdapter.setClickCallback(this);
        }
        setDeviceListenerInterface();

        Log.d(TAG, "sub: " + getActivity().getSharedPreferences("Mavid", Context.MODE_PRIVATE).getString("sub", ""));

    }


    private void initViews(View view) {


        deviceListView = (ListView) view.findViewById(R.id.deviceListView);

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refreshLayout);

//        tvNoSpeakerFound = view.findViewById(R.id.tv_no_speaker_found);

        tv_connected_speakers_label = view.findViewById(R.id.tv_connected_speakers_label);

        no_device_frame_layout = view.findViewById(R.id.no_device_frame_layout);

        tv_refresh = view.findViewById(R.id.tv_refresh);

        tv_setup_speaker = view.findViewById(R.id.tv_setup_speaker);

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {


                ((MavidHomeTabsActivity) getActivity()).showProgressBar();

                no_device_frame_layout.setVisibility(View.GONE);

                buildSnackBar("Refreshing..");

//                Toast.makeText(getContext(), "Refreshing..", Toast.LENGTH_SHORT).show();
                Log.d("DeviceListFragment", "Refreshing");

                if (deviceListAdapter != null) {
                    deviceInfoList.clear();
                    deviceListAdapter.notifyDataSetChanged();
                }

                if (getActivity() != null) {
                    ((MavidHomeTabsActivity) getActivity()).refreshDiscovery();
                }

                try {
                    MavidApplication.setDeviceListener(new DeviceListener() {
                        @Override
                        public void newDeviceFound(final DeviceInfo deviceInfo) {
                            LibreLogger.d(this, "suma in newdevice found refresh1" + deviceInfo.getFriendlyName());

                            if (!isDiscoveredAlreadySwipeLog(deviceInfo)) {

                                LibreLogger.d(this, "suma in newdevice found refresh2" + deviceInfo.getFriendlyName());

                                deviceInfoList.add(deviceInfo);

                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (deviceInfoList.size() > 0) {
                                                no_device_frame_layout.setVisibility(View.GONE);
                                                tv_connected_speakers_label.setVisibility(View.VISIBLE);
                                            } else {
                                                no_device_frame_layout.setVisibility(View.VISIBLE);
                                                tv_connected_speakers_label.setVisibility(View.GONE);
                                            }
                                        }
                                    });
                                }

                                LibreLogger.d(this, " A new device is found whose ip discovered already 2" + deviceInfo.getIpAddress() + "friendlyname" + deviceInfo.getFriendlyName());
                                LibreMavidHelper.sendCustomCommands(deviceInfo.getIpAddress(), LibreMavidHelper.COMMANDS.FW_URL, "OTA_XML_URL", new CommandStatusListenerWithResponse() {
                                    @Override
                                    public void failure(Exception e) {

                                    }

                                    @Override
                                    public void success() {

                                    }

                                    @Override
                                    public void response(MessageInfo messageInfo) {
                                        LibreLogger.d(this, "suma in response device list 2\n" + messageInfo.getMessage() + "device ip" + deviceInfo.getFriendlyName());

                                        deviceInfo.setFw_url(messageInfo.getMessage());
                                        LibreLogger.d(this, "suma in mavid application check device url" + messageInfo.getMessage() + "device ip" + deviceInfo.getFriendlyName());
                                        myXml = new XmlParser(deviceInfo.getFw_url());
                                        myXml.fetchXml(new DownloadMyXmlListener() {

                                            @Override
                                            public void success(String fw_version, String bsl_version) {
                                                LibreLogger.d(this, "getFw_version: " + fw_version);
                                                if (!fw_version.isEmpty()) {
                                                    MavidApplication.urlFailure = false;
                                                    deviceInfo.setUpdatedFwVersion(fw_version);
                                                    deviceInfo.setBslInfoAfterSplit(bsl_version);
                                                    CheckFirmwareInfoClass compareFirmwareClass = new CheckFirmwareInfoClass(deviceInfo);
                                                    updateNode = getMavidNodesUpdate(deviceInfo.getIpAddress());
                                                    updateNode.setFriendlyname(deviceInfo.getFriendlyName());
                                                    updateNode.setFwVersion(deviceInfo.getFwVersion());
                                                    FirmwareUpdateHashmap.getInstance().checkIfNodeAlreadyPresentinList(deviceInfo.getIpAddress());

                                                    LibreLogger.d(this, "suma in mavid application already notpresent" + deviceInfo.getFriendlyName());
                                                    updateNode.setFwUpdateNeededBtnEnable(compareFirmwareClass.checkFirmwareUpdateButtonEnableorDisable(deviceInfo.getIpAddress()));
                                                    if (updateNode.isFwUpdateNeededBtnEnableCheck()) {
                                                        MavidApplication.checkIsFwBtnLatest = true;
                                                        LibreLogger.d(this, "suma need fwupdatebtn has latest no need update secondcheck" + updateNode.getFriendlyname());
                                                    } else {
                                                        MavidApplication.checkIsFwBtnLatest = false;
                                                        LibreLogger.d(this, "suma need fwupdatebtn latest yes need update secondcheck" + updateNode.getFriendlyname());
                                                    }

                                                    updateNode.setFirmwareUpdateNeeded(compareFirmwareClass.checkIfFirmwareUpdateNeeded(deviceInfo.getIpAddress()));
                                                    if (updateNode.isFirmwareUpdateNeeded()) {
                                                        LibreLogger.d(this, "suma need fw" + updateNode.getFriendlyname());
                                                        MavidApplication.isFwNeeded = true;
                                                    } else {
                                                        LibreLogger.d(this, "suma need fw no" + updateNode.getFriendlyname());
                                                        MavidApplication.isFwNeeded = false;
                                                    }


                                                    // }
                                                }

                                            }

                                            @Override
                                            public void failure(Exception e) {
                                                if (!MavidApplication.fwupdatecheckPrivateBuild) {
                                                    MavidApplication.urlFailure = true;
                                                }
                                                LibreLogger.d(this, "exception while parsing the XML 1 check suma" + e.toString());
                                                // myXml = new XmlParser(deviceInfo.getFw_url());
                                            }
                                        });
                                    }
                                });
                            }
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((MavidHomeTabsActivity) getActivity()).dismissProgressBar();
                                    }
                                });
                            }
                        }

                        @Override
                        public void deviceGotRemoved(DeviceInfo deviceInfo) {

                        }

                        @Override
                        public void deviceDataReceived(MessageInfo messageInfo) {

                        }

                        @Override
                        public void failures(Exception e) {

                        }

                        @Override
                        public void checkFirmwareInfo(DeviceInfo deviceInfo) {
                            LibreLogger.d(this, "suma in mavid application check firmwareinfo" + deviceInfo.getFriendlyName());
                            LibreLogger.d(this, "suma in devicesetting activity" + deviceInfo.getBslOldValue());
                            LibreLogger.d(this, "suma in devicesetting activity" + deviceInfo.getBslNewValue());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }


                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (deviceInfoList.size() > 0) {
                            no_device_frame_layout.setVisibility(View.GONE);
                            tv_connected_speakers_label.setVisibility(View.VISIBLE);
                        } else {
                            tv_connected_speakers_label.setVisibility(View.GONE);
                            no_device_frame_layout.setVisibility(View.VISIBLE);
                        }
                        if (getActivity() != null) {
                            ((MavidHomeTabsActivity) getActivity()).dismissProgressBar();
                        }
                        endRefresh();
                    }
                }, 5000);

            }
        });

        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                LibreMavidHelper.askRefreshToken(deviceInfoList.get(i).getIpAddress(), new CommandStatusListenerWithResponse() {
                    @Override
                    public void response(MessageInfo messageInfo) {
                        String messages = messageInfo.getMessage();
                        Log.d("DeviceManager", " got alexa token " + messages);
                        handleAlexaRefreshTokenStatus(messageInfo.getIpAddressOfSender(), messageInfo.getMessage());
                    }

                    @Override
                    public void failure(Exception e) {

                    }

                    @Override
                    public void success() {

                    }
                });
            }
        });
        if (getActivity() != null) {
            tv_refresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((MavidHomeTabsActivity) getActivity()).showProgressBar();

                    if (deviceListAdapter != null) {
                        deviceInfoList.clear();
                        deviceListAdapter.notifyDataSetChanged();
                    }

                    no_device_frame_layout.setVisibility(View.GONE);

                    ((MavidHomeTabsActivity) getActivity()).refreshDiscovery();

                    setDeviceListenerInterface();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity() != null) {
                                if (deviceInfoList.size() > 0) {
                                    no_device_frame_layout.setVisibility(View.GONE);
                                    tv_connected_speakers_label.setVisibility(View.VISIBLE);
                                } else {
                                    tv_connected_speakers_label.setVisibility(View.GONE);
                                    no_device_frame_layout.setVisibility(View.VISIBLE);
                                }
                                ((MavidHomeTabsActivity) getActivity()).dismissProgressBar();
                            }
                            endRefresh();
                        }
                    }, 5000);
                }
            });
        }


        tv_setup_speaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity() != null) {
                    ((MavidHomeTabsActivity) getActivity()).bottomNavigation.setSelectedItemId(R.id.action_add);//add a new device
                    ((MavidHomeTabsActivity) getActivity()).inflateFragments(BLEBlinkingFragment.class.getSimpleName());
                }
            }
        });

        if (getActivity() != null) {

            ((MavidHomeTabsActivity) getActivity()).ivRefresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {


                    if (deviceListAdapter != null) {
                        deviceInfoList.clear();
                        deviceListAdapter.notifyDataSetChanged();
                    }
                    buildSnackBar("Refreshing..");

//                    Toast.makeText(getContext(), "Refreshing..", Toast.LENGTH_SHORT).show();
                    Log.d("DeviceListFragment", "Refreshing");
                    if (getActivity() != null) {
                        ((MavidHomeTabsActivity) getActivity()).showProgressBar();
                    }
                    no_device_frame_layout.setVisibility(View.GONE);


                    ((MavidHomeTabsActivity) getActivity()).refreshDiscovery();

                    setDeviceListenerInterface();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity() != null) {

                                if (deviceInfoList.size() > 0) {
                                    no_device_frame_layout.setVisibility(View.GONE);
                                    tv_connected_speakers_label.setVisibility(View.VISIBLE);
                                } else {
                                    tv_connected_speakers_label.setVisibility(View.GONE);
                                    no_device_frame_layout.setVisibility(View.VISIBLE);
                                    ((MavidHomeTabsActivity) getActivity()).dismissProgressBar();

                                }
                            }
                            endRefresh();
                            if (((MavidHomeTabsActivity) getActivity()) != null) {
                                ((MavidHomeTabsActivity) getActivity()).dismissProgressBar();
                            }
                        }
                    }, 5000);
                }
            });
        }
    }

    public void setDeviceListenerInterface() {
        try {
            MavidApplication.setDeviceListener(new DeviceListener() {
                @Override
                public void newDeviceFound(final DeviceInfo deviceInfo) {
                    Log.d("DeviceListFragment", " A new device is found whose ip address is " + deviceInfo.getIpAddress());
                    if (getActivity() != null) {

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isDiscoveredAlready(deviceInfo)) {
                                    LibreLogger.d(this, " A new device is found whose ip discovered already 3" + deviceInfo.getIpAddress() + "friendlyname" + deviceInfo.getFriendlyName());
                                    deviceInfoList.add(deviceInfo);
                                    LibreLogger.d(this, "suma in device list activity" + deviceInfoList.size());
                                    if (deviceInfoList.size() > 0) {
                                        no_device_frame_layout.setVisibility(View.GONE);
                                        tv_connected_speakers_label.setVisibility(View.VISIBLE);
                                    } else {
                                        tv_connected_speakers_label.setVisibility(View.GONE);
                                        no_device_frame_layout.setVisibility(View.VISIBLE);
                                    }
                                    LibreMavidHelper.sendCustomCommands(deviceInfo.getIpAddress(), LibreMavidHelper.COMMANDS.FW_URL, "OTA_XML_URL", new CommandStatusListenerWithResponse() {
                                        @Override
                                        public void failure(Exception e) {
                                            if (((MavidHomeTabsActivity) getActivity()) != null) {
                                                ((MavidHomeTabsActivity) getActivity()).dismissProgressBar();
                                            }
                                        }

                                        @Override
                                        public void success() {

                                        }

                                        @Override
                                        public void response(MessageInfo messageInfo) {
                                            LibreLogger.d(this, "suma in response device list 2\n" + messageInfo.getMessage() + "device ip" + deviceInfo.getFriendlyName());
                                            deviceInfo.setFw_url(messageInfo.getMessage());
                                            LibreLogger.d(this, "suma in mavid application check device url" + messageInfo.getMessage() + "device ip" + deviceInfo.getFriendlyName());
                                            myXml = new XmlParser(deviceInfo.getFw_url());
                                            myXml.fetchXml(new DownloadMyXmlListener() {

                                                @Override
                                                public void success(String fw_version, String bsl_version) {
                                                    LibreLogger.d(this, "getFw_version: " + fw_version);
                                                    if (!fw_version.isEmpty()) {
                                                        MavidApplication.urlFailure = false;
                                                        deviceInfo.setUpdatedFwVersion(fw_version);
                                                        deviceInfo.setBslInfoAfterSplit(bsl_version);
                                                        CheckFirmwareInfoClass compareFirmwareClass = new CheckFirmwareInfoClass(deviceInfo);
                                                        updateNode = getMavidNodesUpdate(deviceInfo.getIpAddress());
                                                        updateNode.setFriendlyname(deviceInfo.getFriendlyName());
                                                        updateNode.setFwVersion(deviceInfo.getFwVersion());

                                                        FirmwareUpdateHashmap.getInstance().checkIfNodeAlreadyPresentinList(deviceInfo.getIpAddress());

                                                        LibreLogger.d(this, "suma in mavid application already notpresent" + deviceInfo.getFriendlyName());
                                                        updateNode.setFwUpdateNeededBtnEnable(compareFirmwareClass.checkFirmwareUpdateButtonEnableorDisable(deviceInfo.getIpAddress()));
                                                        if (updateNode.isFwUpdateNeededBtnEnableCheck()) {
                                                            MavidApplication.checkIsFwBtnLatest = true;
                                                            LibreLogger.d(this, "suma need fwupdatebtn has latest no need update secondcheck" + updateNode.getFriendlyname());
                                                        } else {
                                                            MavidApplication.checkIsFwBtnLatest = false;
                                                            LibreLogger.d(this, "suma need fwupdatebtn latest yes need update secondcheck" + updateNode.getFriendlyname());
                                                        }

                                                        updateNode.setFirmwareUpdateNeeded(compareFirmwareClass.checkIfFirmwareUpdateNeeded(deviceInfo.getIpAddress()));
                                                        if (updateNode.isFirmwareUpdateNeeded()) {
                                                            LibreLogger.d(this, "suma need fw" + updateNode.getFriendlyname());
                                                            MavidApplication.isFwNeeded = true;
                                                        } else {
                                                            LibreLogger.d(this, "suma need fw no" + updateNode.getFriendlyname());
                                                            MavidApplication.isFwNeeded = false;
                                                        }


                                                        // }
                                                    }

                                                }

                                                @Override
                                                public void failure(Exception e) {
                                                    if (!MavidApplication.fwupdatecheckPrivateBuild) {
                                                        MavidApplication.urlFailure = true;
                                                    }
                                                    LibreLogger.d(this, "exception while parsing the XML 2mcheck suma" + e.toString());
                                                    // myXml = new XmlParser(deviceInfo.getFw_url());
                                                }
                                            });
                                        }
                                    });

                                }


                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            deviceListAdapter.notifyDataSetChanged();
                                            ((MavidHomeTabsActivity) getActivity()).dismissProgressBar();
                                        }
                                    });

                                }
                            }
                        });
                    }
                }

                @Override
                public void deviceGotRemoved(DeviceInfo deviceInfo) {

                }

                @Override
                public void deviceDataReceived(MessageInfo messageInfo) {

                }

                @Override
                public void failures(Exception e) {
                    if (getActivity() != null) {
                        ((MavidHomeTabsActivity) getActivity()).dismissProgressBar();
                    }
                }

                @Override
                public void checkFirmwareInfo(DeviceInfo deviceInfo) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void buildSnackBar(String message) {
        if (getContext() != null) {
            CafeBar.Builder builder = CafeBar.builder(getContext());
            builder.autoDismiss(true);
            builder.customView(R.layout.custom_snackbar_layout);

            cafeBar = builder.build();
            AppCompatTextView tv_message = cafeBar.getCafeBarView().findViewById(R.id.tv_message);
            tv_message.setText(message);

            cafeBar.show();
        }
    }

    public void endRefresh() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshLayout.setRefreshing(false);
                }
            });
        }
    }

    private boolean isDiscoveredAlready(DeviceInfo deviceInfo) {
        deviceInfoList.contains(deviceInfo);
        for (DeviceInfo info : deviceInfoList) {
            if (info.getIpAddress().equals(deviceInfo.getIpAddress())) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void onResume() {
        super.onResume();
        /*Suma : Adding here just to be not dependant on irrespective of device discovered or no*/
        /* Suma : Below method : Dynamically fetching the product id from asset folder*/
        /*Start of code*/
        InputStream input = null;
        try {
            input = getContext().getAssets().open("symmentrickey_of_productid.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //symmentrickey_of_productid.txt can't be more than 2 gigs.
        int size = 0;
        try {
            if (input != null) {
                size = input.available();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] buffer = new byte[size];
        try {
            if (input != null) {
                input.read(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // byte buffer into a string
        String text = new String(buffer);
        text = text.replaceAll("\n", "");
        LibreLogger.d(this, "msg digest symmentric  standard product ID mavidapplication" + text);
        /*End of code*/


//        MessageDigest md = null;
//        try {
//            md = MessageDigest.getInstance("MD5");
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
//        byte[] hashInBytes = new byte[0];
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
//            if (md != null) {
//                hashInBytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
//            }
//        }
//        // bytes to hex
//        StringBuilder sb = new StringBuilder();
//        for (byte b : hashInBytes) {
//            sb.append(String.format("%02x", b));
//        }
//        LibreMavidHelper.symmentricKey=sb.toString();
//        LibreLogger.d(this,"suma in msg digest symmentric mavidapplication\n"+sb.toString());
//        LibreLogger.d(this,"suma in libre mavid helper\n"+LibreMavidHelper.symmentricKey);
///*End of the code*/

        try {
            LibreMavidHelper.advertise();
        } catch (WrongStepCallException e) {
            e.printStackTrace();
        }
    }

    public void removeDevicesFromDeviceList(DeviceInfo deviceInfo) {
        if (deviceInfoList != null && deviceInfoList.contains(deviceInfo)) {
            deviceInfoList.remove(deviceInfo);
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        deviceListAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }


    private void handleAlexaRefreshTokenStatus(String current_ipaddress, String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            /*not logged in*/
            Intent i = new Intent(getContext(), AlexaThingsToTryDoneActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("speakerIpaddress", current_ipaddress);
            i.putExtra("fromActivity", MavidHomeTabsActivity.class.getSimpleName());
            startActivity(i);

        } else {
            Intent newIntent = new Intent(getContext(), AlexaSignInActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            newIntent.putExtra("speakerIpaddress", current_ipaddress);
            newIntent.putExtra("fromActivity", MavidHomeTabsActivity.class.getSimpleName());
            startActivity(newIntent);
        }
    }

    private boolean isDiscoveredAlreadySwipeLog(DeviceInfo deviceInfo) {
        deviceInfoList.contains(deviceInfo);
        for (DeviceInfo info : deviceInfoList) {
            if (info.getIpAddress().equals(deviceInfo.getIpAddress())) {
                return true;
            }
        }
        return false;
    }


    public UpdatedMavidNodes getMavidNodesUpdate(String ipAddress) {
        if (updateHash.getUpdateNode(ipAddress) != null) {
            LibreLogger.d(this, "updating old node" + updateNode.getFriendlyname());
            return updateHash.getUpdateNode(ipAddress);
        }
        LibreLogger.d(this, "creating new node");
        return new UpdatedMavidNodes();
    }


    /**
     * This device api to check the uuid
     */
    private void CallLDAPI5ToCheckIfTheStatusOfUUid(String ipAddress, String payload, final int position) {

        LibreMavidHelper.sendCustomCommands(ipAddress,
                LibreMavidHelper.COMMANDS.SEND_IR_REMOTE_DETAILS_AND_RETRIVING_BUTTON_LIST, payload, new CommandStatusListenerWithResponse() {
                    @Override
                    public void response(MessageInfo messageInfo) {
                        JSONObject dataJsonObject = null;
                        try {
                            dataJsonObject = new JSONObject(messageInfo.getMessage());
                            Log.d(TAG, "ldapi#5_response" + (dataJsonObject).toString());

                            int statusCode = dataJsonObject.getInt("Status");

                            JSONObject payloadObject = dataJsonObject.getJSONObject("payload");


                            switch (statusCode) {
                                case 3:
                                    //sucess // no_error //ack

                                    int UuidStatus = payloadObject.getInt("UuidStatus");

                                    JSONArray applianceJsonArray = null;


                                    if (payloadObject.has("ApplianceInfo")) {
                                        applianceJsonArray = payloadObject.getJSONArray("ApplianceInfo");
                                    }

                                    UUIDStatusChecking(UuidStatus, applianceJsonArray, deviceInfoList.get(position));
                                    break;


                                case -3:
                                case -2:
                                case -1:
                                case 0:
                                    //error
                                    uiRelatedClass.buidCustomSnackBarWithButton(getActivity(),
                                            "There seems to be an error,Please try after sometime", "OK", (AppCompatActivity) getActivity());
                                    break;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d(TAG, "json_error_lsdapi5" + e.getMessage());
                        }
                    }

                    @Override
                    public void failure(Exception e) {
                        closeLoader();
                        Log.d(TAG, "ldapi_response_exception" + e.toString());
                    }

                    @Override
                    public void success() {

                    }
                });

    }


    private void UUIDStatusChecking(int UuidStatus, JSONArray applianceJsonArray, DeviceInfo deviceInfo) {
        switch (UuidStatus) {
            case 1:
                /**UUID empty (status code was not present,
                 * device updated the UUID what App provided)
                 * Factory reset case,
                 * If "Yes",
                 * Call LDAPI#1,
                 * update the device the info one by on
                 * or delete from cloud if the user says
                 * "No"
                 * */
                getUserApplianceInfoDetails(getActivity()
                        .getSharedPreferences("Mavid", Context.MODE_PRIVATE).getString("sub", ""), deviceInfo, UuidStatus);
                break;

            case 2:
                /** Different status code
                 *  present—show the error message
                 *  show a dialog
                 *  */
                closeLoader();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uiRelatedClass.showCustomDialogForUUIDMismatch((AppCompatActivity) getActivity(), new OnButtonClickCallback() {
                            @Override
                            public void onClick(boolean isSucess) {
                                Log.d(TAG, "show the error and restrict user.");
                            }
                        });
                    }
                });
                break;

            case 3:
                /**
                 * UUID Matches.
                 * Go ahead and read the payload.
                 * Read the appliance info and check
                 * if the appliances match with the one
                 * present in the app
                 * only in uuid_status = 2 we get
                 * appliance json array
                 * */

                checkIfThereIsApplianceMismatchWithDevice(applianceJsonArray, deviceInfo, UuidStatus);
                break;
        }
    }


    private void checkIfThereIsApplianceMismatchWithDevice(JSONArray applianceJsonArray, DeviceInfo deviceInfo, int UuidStatus) {
        boolean isTVDetailsMatching = false;
        boolean isTVPDetailsMatching = false;
        for (int i = 0; i < applianceJsonArray.length(); i++) {
            try {
                JSONObject applianceJsonObject = (JSONObject) applianceJsonArray.get(i);
                int applianceType = applianceJsonObject.getInt("appliance");
                int brandId = applianceJsonObject.getInt("bId");

                int remoteId = applianceJsonObject.getInt("rId");

                switch (applianceType) {
                    case 1:
                        //tv
                        Log.d(TAG, "tvApplianceInfoStoredInDevice: " + applianceJsonObject);
                        isTVDetailsMatching = checkIfTvDetailsMatchesWithTheOneStoredInApp(String.valueOf(brandId), String.valueOf(remoteId));

                        break;
                    case 2:
                        //tvp
                        isTVPDetailsMatching = checkIfTVPDetailsMatchesWithOneStoredInApp(String.valueOf(brandId), String.valueOf(remoteId));

                        break;
                    case 3:
                        //ac
                        break;
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (!isTVDetailsMatching || !isTVPDetailsMatching) {
            /** If tv or tvp details do not match with the one which app has
             * then call the GET lcapi3
             * */
            //call the GET lcapi3
            getUserApplianceInfoDetails(getActivity()
                    .getSharedPreferences("Mavid", Context.MODE_PRIVATE).getString("sub", ""), deviceInfo, UuidStatus);
        } else {
            //if it matches with one stored in the app
            //then goto tabs activity
            gotoIRAddRemoteVPActivity(deviceInfo);
        }
    }

    private boolean checkIfTVPDetailsMatchesWithOneStoredInApp(String brandId, String remoteId) {
        String modelTVPRemoteDetailsString = getActivity().getSharedPreferences("Mavid", Context.MODE_PRIVATE)
                .getString("tvpRemoteDetails", "");

        if (!modelTVPRemoteDetailsString.isEmpty()) {

            ModelRemoteDetails tvpRemoteDetails = new ModelRemoteDetails();

            tvpRemoteDetails = gson.fromJson(modelTVPRemoteDetailsString, ModelRemoteDetails.class);

            if (!tvpRemoteDetails.getBrandId().equals(String.valueOf(brandId))
                    && !tvpRemoteDetails.getRemoteId().equals(String.valueOf(remoteId))) {
                //if the brandId and remote are not equal
                //to the one which the app has stored
                return false;
            }
        } else {
            //because there is no tvp data present
            //in the app
            return false;
        }
        return true;
    }


    private boolean checkIfTvDetailsMatchesWithTheOneStoredInApp(String brandId, String remoteId) {
        String modelTvRemoteDetailsString = getActivity().getSharedPreferences("Mavid", Context.MODE_PRIVATE)
                .getString("tvRemoteDetails", "");
        if (!modelTvRemoteDetailsString.isEmpty()) {
            ModelRemoteDetails tvRemoteDetails = new ModelRemoteDetails();

            tvRemoteDetails = gson.fromJson(modelTvRemoteDetailsString, ModelRemoteDetails.class);

            Log.d(TAG, "tvApplianceInfoInApp: " + tvRemoteDetails);

            if (!tvRemoteDetails.getBrandId().equals(String.valueOf(brandId))
                    && !tvRemoteDetails.getRemoteId().equals(String.valueOf(remoteId))) {
                //if the brandId and remote are not equal
                //to the one which the app has stored
                return false;
            }
        } else {
            //because there is no tv data present
            //in the app
            return false;
        }
        return true;
    }

    private ModelRemoteDetails parseApplianceJsonObject(JSONObject applianceObject) {

        ModelRemoteDetails modelRemoteDetails = new ModelRemoteDetails();

        try {
            modelRemoteDetails.setSelectedBrandName(applianceObject.getString("BrandName"));
            modelRemoteDetails.setRemoteId(applianceObject.getString("RemoteID"));
            modelRemoteDetails.setBrandId(applianceObject.getString("BrandId"));
            modelRemoteDetails.setSelectedAppliance(applianceObject.getString("Appliance"));
            modelRemoteDetails.setCustomName(applianceObject.getString("CustomName"));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return modelRemoteDetails;
    }


    private void getUserApplianceInfoDetails(String sub, final DeviceInfo deviceInfo, final int uuidStatus) {
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());

        String baseUrl = "https://op4w1ojeh4.execute-api.us-east-1.amazonaws.com/Beta/usermangement?" + ("sub=") + (sub);


        //+ "Mac=" + deviceInfo.getUSN()

        Log.d(TAG, "requestedURl: " + baseUrl);

        StringRequest getUserMgtDetailsRequest = new StringRequest(Request.Method.GET, baseUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "getUserManagementDetails: response" + (response));

                try {
                    JSONObject responseObject = new JSONObject(response);

                    JSONArray bodyJsonArraay = responseObject.getJSONArray("body");

                    if (bodyJsonArraay.length() > 0) {
                        //there is appliance info data
                        JSONObject bodyJsonObject = bodyJsonArraay.getJSONObject(0);
                        if (uuidStatus == 1) {
                            //UUID is empty
                            if (bodyJsonObject.has("Appliance")) {
                                /** cloud has data
                                 if cloud have data—Factory reset case,
                                 Call LDAPI#1 and update the device the info one by one
                                 Take the user to a screen "to restore selection of tv/tvps"
                                 */
                                closeLoader();
                                Intent intent = new Intent(getActivity(), IRRestoreSelectionActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("deviceInfo", deviceInfo);
                                bundle.putString("applianceInfo", String.valueOf(bodyJsonObject));
                                intent.putExtras(bundle);
                                startActivity(intent);
                            }
                        } else if (uuidStatus == 3) {
                            if (bodyJsonObject.has("Appliance")) {
                                JSONObject applianceObject = bodyJsonObject.optJSONObject("Appliance");
                                if (applianceObject != null) {
                                    //it is a json object
                                    ModelRemoteDetails modelRemoteDetails = parseApplianceJsonObject(applianceObject);
                                    //updating the tv or tvp details
                                    updateTvORTvpDetailsInSharedPref(modelRemoteDetails, deviceInfo);
                                } else {
                                    //it might be an array
                                    JSONArray applianceJsonArray = bodyJsonObject.optJSONArray("Appliance");

                                    if (applianceJsonArray != null) {
                                        for (int i = 0; i < applianceJsonArray.length(); i++) {
                                            //updating the tv or tvp details
                                            ModelRemoteDetails modelRemoteDetails = parseApplianceJsonObject((JSONObject) applianceJsonArray.get(i));
                                            updateTvORTvpDetailsInSharedPref(modelRemoteDetails, deviceInfo);
                                        }
                                    }
                                }
                                gotoIRAddRemoteVPActivity(deviceInfo);
                            }
                        }
                    } else {
                        closeLoader();
                        //cloud has no data
                        //if cloud data is also empty—first time user—Go ahead with normal flow
                        gotoIRAddRemoteVPActivity(deviceInfo);
                        Log.d(TAG, "no_appliance_data_cloud");
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

                if (volleyError instanceof TimeoutError || volleyError instanceof NoConnectionError) {

                    uiRelatedClass.buildSnackBarWithoutButton(getActivity(),
                            getActivity().getWindow().getDecorView().findViewById(android.R.id.content), "Seems your internet connection is slow, please try in sometime");

                } else if (volleyError instanceof AuthFailureError) {

                    uiRelatedClass.buildSnackBarWithoutButton(getActivity(),
                            getActivity().getWindow().getDecorView().findViewById(android.R.id.content), "AuthFailure error occurred, please try again later");


                } else if (volleyError instanceof ServerError) {
                    if (volleyError.networkResponse.statusCode != 302) {
                        uiRelatedClass.buildSnackBarWithoutButton(getActivity(),
                                getActivity().getWindow().getDecorView().findViewById(android.R.id.content), "Server error occurred, please try again later");
                    }

                } else if (volleyError instanceof NetworkError) {
                    uiRelatedClass.buildSnackBarWithoutButton(getActivity(),
                            getActivity().getWindow().getDecorView().findViewById(android.R.id.content), "Network error occurred, please try again later");

                } else if (volleyError instanceof ParseError) {
                    uiRelatedClass.buildSnackBarWithoutButton(getActivity(),
                            getActivity().getWindow().getDecorView().findViewById(android.R.id.content), "Parser error occurred, please try again later");
                }

            }
        });


        getUserMgtDetailsRequest.setRetryPolicy(new DefaultRetryPolicy(30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(getUserMgtDetailsRequest);


    }


    private void updateTvORTvpDetailsInSharedPref(ModelRemoteDetails modelRemoteDetails, DeviceInfo deviceInfo) {
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("Mavid", Context.MODE_PRIVATE).edit();
        switch (modelRemoteDetails.getSelectedAppliance()) {
            case "1":
            case "TV":
                //tv
                String updatedTVRemoteDetailsString = gson.toJson(modelRemoteDetails);

                editor.putString("tvRemoteDetails", updatedTVRemoteDetailsString);
                editor.apply();

                break;

            case "2":
            case "TVP":
                //tvp
                String updatedTVPRemoteDetailsString = gson.toJson(modelRemoteDetails);

                editor.putString("tvpRemoteDetails", updatedTVPRemoteDetailsString);
                editor.apply();

                break;
        }

    }


    private void gotoIRAddRemoteVPActivity(DeviceInfo deviceInfo) {
        closeLoader();
        Log.d(TAG, "gotoIRAddRemoteVPActivity");
        Intent intent = new Intent(getActivity(), IRAddRemoteVPActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("deviceInfo", deviceInfo);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void showLoader(final String title, final String message) {

        if (!(getActivity().isFinishing())) {

            if (mDialog == null) {
                mDialog = new Dialog(getActivity());
                mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                mDialog.setContentView(R.layout.custom_progress_bar);

                mDialog.setCancelable(false);
                AppCompatTextView progress_title = mDialog.findViewById(R.id.progress_title);
                ProgressBar progress_bar = mDialog.findViewById(R.id.progress_bar);
                AppCompatTextView progress_message = mDialog.findViewById(R.id.progress_message);
                progress_title.setText(title);
                progress_message.setText(message);
                progress_bar.setIndeterminate(true);
                progress_bar.setVisibility(View.VISIBLE);
            }

            mDialog.show();
        }
    }

    public void closeLoader() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mDialog != null) {
                    if (!(getActivity().isFinishing())) {
                        mDialog.dismiss();
                        mDialog = null;
                    }
                }
            }
        });
    }

    private String buildPayloadForLdapi() {
        JSONObject payloadObject = new JSONObject();
        try {
            payloadObject.put("ID", 5);

            JSONObject dataObject = new JSONObject();

            dataObject.put("uuid", getActivity().getSharedPreferences("Mavid", Context.MODE_PRIVATE)
                    .getString("sub", ""));

            payloadObject.put("data", dataObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "ldapi5_payload" + payloadObject.toString());
        return payloadObject.toString();
    }

    @Override
    public void onClickListview(int position) {
        Log.d(TAG, "clicked_on_remoteIcon");
        showLoader("Please wait", "");
        CallLDAPI5ToCheckIfTheStatusOfUUid(deviceInfoList.get(position).getIpAddress(), buildPayloadForLdapi(), position);
    }
}


//    public void disconnect() {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w("Disconnect", "BluetoothAdapter not initialized");
//            return;
//        }
//        mBluetoothGatt.disconnect();
