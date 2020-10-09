package com.mavid;

import android.app.Dialog;
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
import com.mavid.models.ModelRemoteDetails;
import com.mavid.models.ModelRemoteSubAndMacDetils;
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
import com.mavid.utility.OnGetUserApplianceUserInfoInterface;
import com.mavid.utility.UIRelatedClass;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

    ModelRemoteSubAndMacDetils modelRemoteSubAndMacDetils = new ModelRemoteSubAndMacDetils();

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
                        .getSharedPreferences("Mavid", Context.MODE_PRIVATE).getString("sub", ""), deviceInfo, new OnGetUserApplianceUserInfoInterface() {
                    @Override
                    public void onApiResponseCallback(@NotNull DeviceInfo deviceInfo, @NotNull String bodyObject) {
                        //UUID is empty
                        closeLoader();
                        //data is present
                        gotoRestoreSelectionActivity(deviceInfo, bodyObject);
                    }
                });
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
                 * */

                checkIfThereIsApplianceMismatchWithDevice(applianceJsonArray, deviceInfo);


                break;
        }
    }


    private HashMap<ModelRemoteDetails, String> addAppAppliancesDataToList(String macId) {

//        List<ModelRemoteDetails> appliancesList = new ArrayList<>();
        HashMap<ModelRemoteDetails, String> appliancesListHashmap = new HashMap<>();

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Mavid", Context.MODE_PRIVATE);

        Gson gson = new Gson();

        String modelRemoteDetailsString = sharedPreferences.getString("applianceInfoList", "");

        if (!modelRemoteDetailsString.isEmpty()) {

            modelRemoteSubAndMacDetils = new ModelRemoteSubAndMacDetils();

            modelRemoteSubAndMacDetils = gson.fromJson(modelRemoteDetailsString, ModelRemoteSubAndMacDetils.class);

            if (modelRemoteSubAndMacDetils.getMac().equals(macId)) {
                if (modelRemoteSubAndMacDetils.getModelRemoteDetailsList() != null) {
                    if (modelRemoteSubAndMacDetils.getModelRemoteDetailsList().size() > 0) {
                        for (ModelRemoteDetails modelRemoteDetails : modelRemoteSubAndMacDetils.getModelRemoteDetailsList()) {

                            appliancesListHashmap.put(modelRemoteDetails, "1");

//                            appliancesList = modelRemoteSubAndMacDetils.getModelRemoteDetailsList();
                        }
                    }
                }
            }
        }

        return appliancesListHashmap;
    }

    private HashMap<ModelRemoteDetails, String> addDeviceApplianceInfoToAList(JSONArray deviceApplianceJsonArray) {
//        List<ModelRemoteDetails> mavidDeviceAppliancesList = new ArrayList();

        HashMap<ModelRemoteDetails, String> mavidDeviceAppliancesHashmap = new HashMap<>();

        for (int i = 0; i < deviceApplianceJsonArray.length(); i++) {
            ModelRemoteDetails modelRemoteDetails = new ModelRemoteDetails();

            try {
                JSONObject applianceJsonObject = (JSONObject) deviceApplianceJsonArray.get(i);

                modelRemoteDetails.setSelectedAppliance(String.valueOf(applianceJsonObject.getInt("appliance")));

                modelRemoteDetails.setSelectedBrandName(applianceJsonObject.getString("bName"));

                String brandName = modelRemoteDetails.getSelectedBrandName();

                modelRemoteDetails.setSelectedBrandName(brandName.replaceAll(",$", ""));


                modelRemoteDetails.setRemoteId(String.valueOf(applianceJsonObject.getInt("rId")));

                modelRemoteDetails.setGroupId(applianceJsonObject.getInt("group"));


                modelRemoteDetails.setGroupdName("Scene1");

                if (modelRemoteDetails.getSelectedAppliance().equals("1") || modelRemoteDetails.getSelectedAppliance().equals("TV")) {
                    modelRemoteDetails.setCustomName("TV");
                } else if (modelRemoteDetails.getSelectedAppliance().equals("2") || modelRemoteDetails.getSelectedAppliance().equals("TVP")) {
                    modelRemoteDetails.setCustomName("My Box");
                }

                modelRemoteDetails.setBrandId(String.valueOf(applianceJsonObject.getInt("bId")));

//                mavidDeviceAppliancesList.add(modelRemoteDetails);
                mavidDeviceAppliancesHashmap.put(modelRemoteDetails, "1");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return mavidDeviceAppliancesHashmap;
    }


    private void deleteTvORTvpDetailsInSharedPref(String macId) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Mavid", Context.MODE_PRIVATE);

        Gson gson = new Gson();

        String modelRemoteDetailsString = sharedPreferences.getString("applianceInfoList", "");

        if (!modelRemoteDetailsString.isEmpty()) {
            //if data is present
            modelRemoteSubAndMacDetils = new ModelRemoteSubAndMacDetils();

            modelRemoteSubAndMacDetils = gson.fromJson(modelRemoteDetailsString, ModelRemoteSubAndMacDetils.class);

            if (modelRemoteSubAndMacDetils.getMac().equals(macId)) {
                //removing all the appliances present for that mavid device
                Log.d(TAG, "deletedAllDataFromSharedPref");
                modelRemoteSubAndMacDetils.getModelRemoteDetailsList().removeAll(modelRemoteSubAndMacDetils.getModelRemoteDetailsList());
            }
        }
        //saving the details
        modelRemoteDetailsString = gson.toJson(modelRemoteSubAndMacDetils);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("applianceInfoList", modelRemoteDetailsString);
        editor.apply();
    }


    private void checkIfThereIsApplianceMismatchWithDevice(final JSONArray deviceApplianceJsonArray, DeviceInfo deviceInfo) {

        //if the device data and  app data is not present
        ///TODO:call get api and  check if that is also empty
        //then goto the vp tabs
        if (deviceApplianceJsonArray.length() == 0 && checkIfAppDataIsEmpty(deviceInfo.getUSN())) {

            getUserApplianceInfoDetails(getActivity()
                    .getSharedPreferences("Mavid", Context.MODE_PRIVATE).getString("sub", ""), deviceInfo, new OnGetUserApplianceUserInfoInterface() {
                @Override
                public void onApiResponseCallback(@NotNull DeviceInfo deviceInfo, @NotNull String bodyObject) {
                    //if there is data in the cloud
                    //goto to restore selection activity
                    gotoRestoreSelectionActivity(deviceInfo, bodyObject);
                }
            });
            Log.d(TAG, "bothDeviceAndAppDataIsEmpty");
        } else {

            HashMap<ModelRemoteDetails, String> deviceApplianceInfoHashMap = addDeviceApplianceInfoToAList(deviceApplianceJsonArray);

            HashMap<ModelRemoteDetails, String> appDeviceApplianceInfoHashMap = addAppAppliancesDataToList(deviceInfo.getUSN());

            //if the mavid device and app data list size are smae
            if (deviceApplianceInfoHashMap.size() == appDeviceApplianceInfoHashMap.size()) {
                //compare the data objects of mavidData and appData
                boolean isMatching = commpareMavidDeviceDataAndAppData(deviceApplianceInfoHashMap, appDeviceApplianceInfoHashMap);
                if (isMatching) {
                    //goto vp activity
                    gotoIRAddRemoteVPActivity(deviceInfo);
                } else {
                    //if it is not matching
                    //call the get api and update the data of the app
                    getUserApplianceInfoDetails(getActivity()
                                    .getSharedPreferences("Mavid", Context.MODE_PRIVATE).getString("sub", ""),
                            deviceInfo, new OnGetUserApplianceUserInfoInterface() {
                                @Override
                                public void onApiResponseCallback(@NotNull DeviceInfo deviceInfo, @NotNull String bodyObject) {
                                    try {
                                        JSONObject bodyJsonObject = new JSONObject(bodyObject);

                                        JSONObject applianceJsonObject = bodyJsonObject.optJSONObject("Appliance");
                                        if (applianceJsonObject != null) {
                                            ModelRemoteDetails modelRemoteDetails = parseApplianceJsonObject((JSONObject) applianceJsonObject);
                                            //update the cloud data to the app
                                            updateApplianceInfoInSharedPref(modelRemoteDetails, deviceInfo.getUSN());
                                        } else {
                                            JSONArray applianceJsonArray = bodyJsonObject.optJSONArray("Appliance");
                                            //deleteAll the data present in the app
                                            deleteTvORTvpDetailsInSharedPref(deviceInfo.getUSN());

                                            for (int i = 0; i < applianceJsonArray.length(); i++) {
                                                /**
                                                 * 1)remove the old data present in the app
                                                 * 2)updating the tv or tvp details
                                                 * */

                                                ModelRemoteDetails modelRemoteDetails = parseApplianceJsonObject((JSONObject) applianceJsonArray.get(i));

                                                //update the cloud data to the app
                                                updateApplianceInfoInSharedPref(modelRemoteDetails, deviceInfo.getUSN());
                                            }
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    gotoIRAddRemoteVPActivity(deviceInfo);
                                }
                            });
                }
            }
            /**
             * 1) device factory reset has happened
             * or
             * 2) user has unintsalled the app
             * or
             * 3) another user has installed the same app
             *    and moddified the contents of the device
             *
             * */
            else if (deviceApplianceInfoHashMap.size() == 0 && appDeviceApplianceInfoHashMap.size() > 0) {
                //device reset or user has modfied the data in another phone
                getUserApplianceInfoDetails(getActivity()
                        .getSharedPreferences("Mavid", Context.MODE_PRIVATE).getString("sub", ""), deviceInfo, new OnGetUserApplianceUserInfoInterface() {
                    @Override
                    public void onApiResponseCallback(@NotNull DeviceInfo deviceInfo, @NotNull String bodyObject) {
                        //there is data in the cloud
                        //but no data in mavid
                        //and there is data in app
                        //goto restore selectionActivity
                        gotoRestoreSelectionActivity(deviceInfo, bodyObject);
                    }
                });
            } else {
                //app has no data and mavid device has data
                //it is the case  of appUninstall or
                //modified app data in another app

                getUserApplianceInfoDetails(getActivity()
                        .getSharedPreferences("Mavid", Context.MODE_PRIVATE).getString("sub", ""), deviceInfo, new OnGetUserApplianceUserInfoInterface() {
                    @Override
                    public void onApiResponseCallback(@NotNull DeviceInfo deviceInfo, @NotNull String bodyObject) {
                        try {
                            JSONObject bodyJsonObject = new JSONObject(bodyObject);

                            JSONObject applianceJsonObject = bodyJsonObject.optJSONObject("Appliance");
                            if (applianceJsonObject != null) {
                                ModelRemoteDetails modelRemoteDetails = parseApplianceJsonObject((JSONObject) applianceJsonObject);
                                updateApplianceInfoInSharedPref(modelRemoteDetails, deviceInfo.getUSN());
                            } else {
                                JSONArray applianceJsonArray = bodyJsonObject.optJSONArray("Appliance");

                                deleteTvORTvpDetailsInSharedPref(deviceInfo.getUSN());

                                for (int i = 0; i < applianceJsonArray.length(); i++) {
                                    /**
                                     * 1)remove the old data present in the app
                                     * 2)updating the tv or tvp details
                                     * */
                                    ModelRemoteDetails modelRemoteDetails = parseApplianceJsonObject((JSONObject) applianceJsonArray.get(i));
                                    updateApplianceInfoInSharedPref(modelRemoteDetails, deviceInfo.getUSN());
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        gotoIRAddRemoteVPActivity(deviceInfo);
                    }
                });
            }
        }
    }

    private boolean commpareMavidDeviceDataAndAppData(HashMap<ModelRemoteDetails, String> mavidApplianceInfoHashMapList, HashMap<ModelRemoteDetails, String> appApplianceInfoHashmapList) {
        for (Map.Entry<ModelRemoteDetails, String> mavidApplianceHashMap : mavidApplianceInfoHashMapList.entrySet()) {
            if (appApplianceInfoHashmapList.containsKey(mavidApplianceHashMap.getKey())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkIfAppDataIsEmpty(String macId) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Mavid", Context.MODE_PRIVATE);

        Gson gson = new Gson();

        String modelRemoteDetailsString = sharedPreferences.getString("applianceInfoList", "");

        if (!modelRemoteDetailsString.isEmpty()) {

            modelRemoteSubAndMacDetils = new ModelRemoteSubAndMacDetils();


            modelRemoteSubAndMacDetils = gson.fromJson(modelRemoteDetailsString, ModelRemoteSubAndMacDetils.class);

            if (modelRemoteSubAndMacDetils.getMac().equals(macId)) {
                if (modelRemoteSubAndMacDetils.getModelRemoteDetailsList() != null) {
                    if (modelRemoteSubAndMacDetils.getModelRemoteDetailsList().size() > 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }


    private ModelRemoteDetails parseApplianceJsonObject(JSONObject applianceObject) {

        ModelRemoteDetails modelRemoteDetails = new ModelRemoteDetails();

        try {
            modelRemoteDetails.setSelectedBrandName(applianceObject.getString("BrandName"));
            modelRemoteDetails.setRemoteId(applianceObject.getString("RemoteID"));
            modelRemoteDetails.setBrandId(applianceObject.getString("BrandID"));
            if (applianceObject.get("Appliance").equals("TV")) {
                modelRemoteDetails.setSelectedAppliance("1");
            } else if (applianceObject.get("Appliance").equals("TVP")) {
                modelRemoteDetails.setSelectedAppliance("2");
            }
            modelRemoteDetails.setCustomName(applianceObject.getString("CustomName"));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return modelRemoteDetails;
    }


    private void gotoRestoreSelectionActivity(DeviceInfo deviceInfo, String applianceInfo) {
        closeLoader();
        Intent intent = new Intent(getActivity(), IRRestoreSelectionActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("deviceInfo", deviceInfo);
        bundle.putString("applianceInfo", applianceInfo);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void getUserApplianceInfoDetails(String sub, final DeviceInfo deviceInfo, final OnGetUserApplianceUserInfoInterface onGetUserApplianceUserInfoInterface) {
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());

        String baseUrl = "https://op4w1ojeh4.execute-api.us-east-1.amazonaws.com/Beta/usermangement?" + ("sub=") + (sub) + "&Mac=" + deviceInfo.getUSN();


        Log.d(TAG, "requestedURl: " + baseUrl);

        StringRequest getUserMgtDetailsRequest = new StringRequest(Request.Method.GET, baseUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "getUserManagementDetails: response" + (response));

                try {
                    JSONObject responseObject = new JSONObject(response);

                    JSONObject bodyJsonObject = responseObject.optJSONObject("body");

                    if (bodyJsonObject != null) {
                        //body key value is a json object
                        JSONArray applianceJsonArray = bodyJsonObject.optJSONArray("Appliance");
                        if (applianceJsonArray != null) {
                            if (applianceJsonArray.length() > 0) {
                                onGetUserApplianceUserInfoInterface.onApiResponseCallback(deviceInfo, String.valueOf(bodyJsonObject));
                            } else {
                                closeLoader();
                                //if cloud data is also empty—first time user—Go ahead with normal flowr

                                //deleteAll the data present in the app if cloud has no data
                                deleteTvORTvpDetailsInSharedPref(deviceInfo.getUSN());

                                gotoIRAddRemoteVPActivity(deviceInfo);
                            }
                        } else {
                            //appliance key might be json obejct
                            JSONObject applianceJsonObject = bodyJsonObject.optJSONObject("Appliance");
                            if (applianceJsonObject != null) {
                                onGetUserApplianceUserInfoInterface.onApiResponseCallback(deviceInfo, String.valueOf(bodyJsonObject));
                            }
                        }
                    } else {
                        //body key value is a json array
                        JSONArray bodyJsonArray = responseObject.optJSONArray("body");

                        if (bodyJsonArray != null) {

                            if (bodyJsonArray.length() > 0) {

                                bodyJsonObject = bodyJsonArray.getJSONObject(0);

                                JSONArray applianceJsonArray = bodyJsonObject.optJSONArray("Appliance");
                                if (applianceJsonArray != null) {
                                    if (applianceJsonArray.length() > 0) {

                                        onGetUserApplianceUserInfoInterface.onApiResponseCallback(deviceInfo, String.valueOf(bodyJsonObject));

                                    } else {
                                        closeLoader();
                                        //if cloud data is also empty—first time user—Go ahead with normal flowr

                                        //deleteAll the data present in the app if cloud has no data
                                        deleteTvORTvpDetailsInSharedPref(deviceInfo.getUSN());

                                        gotoIRAddRemoteVPActivity(deviceInfo);
                                    }
                                }
                            } else {
                                closeLoader();
                                //if cloud data is also empty—first time user—Go ahead with normal flowr

                                //deleteAll the data present in the app if cloud has no data
                                deleteTvORTvpDetailsInSharedPref(deviceInfo.getUSN());

                                gotoIRAddRemoteVPActivity(deviceInfo);
                            }
                        } else {
                            //body key is a string
                            closeLoader();
                            //new user or sub has been deleted form cloud
                            gotoIRAddRemoteVPActivity(deviceInfo);
                        }
                    }

                } catch (
                        JSONException e) {
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


        getUserMgtDetailsRequest.setRetryPolicy(new

                DefaultRetryPolicy(30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(getUserMgtDetailsRequest);


    }


    private void updateApplianceInfoInSharedPref(ModelRemoteDetails modelRemoteDetails, String macId) {

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Mavid", Context.MODE_PRIVATE);

        Gson gson = new Gson();

        String modelRemoteDetailsString = sharedPreferences.getString("applianceInfoList", "");


        if (!modelRemoteDetailsString.isEmpty()) {

            modelRemoteSubAndMacDetils = new ModelRemoteSubAndMacDetils();


            modelRemoteSubAndMacDetils = gson.fromJson(modelRemoteDetailsString, ModelRemoteSubAndMacDetils.class);

            if (modelRemoteSubAndMacDetils.getMac().equals(macId)) {

                //update the appliance list  details in the list to the exsting device
                modelRemoteSubAndMacDetils.getModelRemoteDetailsList().add(buidlRemoteDetails(modelRemoteDetails));

                Log.d(TAG, "updatedApplianceList" + modelRemoteDetails.getSelectedBrandName());
            } else {
                //new device
                modelRemoteSubAndMacDetils.setSub(sharedPreferences.getString("sub", ""));

                modelRemoteSubAndMacDetils.setMac(macId);

                List<ModelRemoteDetails> appllianceInfoList = new ArrayList();

                appllianceInfoList.add(buidlRemoteDetails(modelRemoteDetails));

                modelRemoteSubAndMacDetils.setModelRemoteDetailsList(appllianceInfoList);
            }
        } else {
            //new user and first device
            modelRemoteSubAndMacDetils = new ModelRemoteSubAndMacDetils();

            modelRemoteSubAndMacDetils.setSub(sharedPreferences.getString("sub", ""));

            modelRemoteSubAndMacDetils.setMac(macId);

            List<ModelRemoteDetails> appllianceInfoList = new ArrayList();

            appllianceInfoList.add(buidlRemoteDetails(modelRemoteDetails));

            modelRemoteSubAndMacDetils.setModelRemoteDetailsList(appllianceInfoList);
        }


        modelRemoteDetailsString = gson.toJson(modelRemoteSubAndMacDetils);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("applianceInfoList", modelRemoteDetailsString);
        editor.apply();
    }


    private ModelRemoteDetails buidlRemoteDetails(ModelRemoteDetails modelRemoteDetails) {
        modelRemoteDetails.setSelectedAppliance(modelRemoteDetails.getSelectedAppliance());

        if (modelRemoteDetails.getSelectedAppliance().equals("1") || modelRemoteDetails.getSelectedAppliance().equals("TV")) {
            //for now hardcoding the customa name
            modelRemoteDetails.setCustomName("TV");
        } else if (modelRemoteDetails.getSelectedAppliance().equals("2") || modelRemoteDetails.getSelectedAppliance().equals("TVP")) {
            //for now hardcoding the customa name
            modelRemoteDetails.setCustomName("My Box");
        }
        modelRemoteDetails.setGroupId(1);

        modelRemoteDetails.setGroupdName("Scene1");

        modelRemoteDetails.setRemoteId(modelRemoteDetails.getRemoteId());

        modelRemoteDetails.setSelectedBrandName(modelRemoteDetails.getSelectedBrandName());

        modelRemoteDetails.setBrandId(modelRemoteDetails.getBrandId());

        return modelRemoteDetails;
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
