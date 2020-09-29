package com.mavid.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mavid.Constants.Constants;
import com.mavid.DeviceListFragment;
import com.mavid.MavidHomeTabsActivity;
import com.mavid.R;
import com.mavid.DeviceSettingsActivity;
import com.mavid.irActivites.IRAddRemoteVPActivity;
import com.mavid.libresdk.LibreMavidHelper;
import com.mavid.libresdk.TaskManager.Communication.Listeners.CommandStatusListenerWithResponse;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.DeviceInfo;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.MessageInfo;
import com.mavid.utility.OnButtonClickCallback;
import com.mavid.utility.OnButtonClickListViewInterface;
import com.mavid.utility.UIRelatedClass;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by bhargav on 20/2/18.
 */

public class DeviceListAdapter extends BaseAdapter {
    ArrayList<DeviceInfo> deviceInfoArrayList;
    Context context;
    DeviceListFragment fragment;
    MavidHomeTabsActivity homeTabsActivity;


    private OnButtonClickListViewInterface clickCallback;

    public void setClickCallback(OnButtonClickListViewInterface clickCallback) {
        this.clickCallback = clickCallback;
    }

    String TAG = DeviceListAdapter.class.getSimpleName();

    public DeviceListAdapter(Context context, ArrayList<DeviceInfo> deviceInfoArrayList, DeviceListFragment deviceListFragment, MavidHomeTabsActivity mavidHomeTabsActivity) {
        this.context = context;
        this.deviceInfoArrayList = deviceInfoArrayList;
        this.fragment = deviceListFragment;
        this.homeTabsActivity = mavidHomeTabsActivity;
    }

    private ArrayList<DeviceInfo> getDeviceInfoArrayList() {
        return this.deviceInfoArrayList;
    }

    private Context getContext() {
        return this.context;
    }

    @Override
    public int getCount() {
        return getDeviceInfoArrayList().size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            //create view
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            view = vi.inflate(R.layout.device_list_item, null);
        }
        TextView deviceFriendlyName = (TextView) view.findViewById(R.id.deviceName);
        deviceFriendlyName.setSelected(true);
        deviceFriendlyName.setText(deviceInfoArrayList.get(i).getFriendlyName());
        Button otaUpgradeButton = (Button) view.findViewById(R.id.otaUpgrade);
        otaUpgradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setCancelable(false)
                        .setMessage(getContext().getResources().getString(R.string.stelle_firmware_confirmalert))
                        .setPositiveButton(getContext().getResources().getString(R.string.proceed), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int ii) {
                                try {
                                    otaUpgrade(deviceInfoArrayList.get(i).getIpAddress(), i);
                                    dialogInterface.dismiss();
                                } catch (Exception e) {

                                }
                            }
                        })
                        .setNegativeButton(getContext().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int ii) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create()
                        .show();
            }
        });
        ImageView advancedSettings = view.findViewById(R.id.advancedSettings);

        advancedSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, DeviceSettingsActivity.class);
                intent.putExtra(Constants.INTENTS.IP_ADDRESS, deviceInfoArrayList.get(i).getIpAddress());
                context.startActivity(intent);
            }
        });

        ImageView ivRemote = view.findViewById(R.id.ivRemote);

        final SharedPreferences sharedPreferences = context.getSharedPreferences("Mavid", Context.MODE_PRIVATE);

        ivRemote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickCallback.onClickListview(i);
            }
        });


        return view;
    }




    private void otaUpgrade(final String ipAddress, final int index) {
        //send command to device
        LibreMavidHelper.sendCustomCommands(ipAddress, LibreMavidHelper.COMMANDS.START_OTA_UPGRADE, "", new CommandStatusListenerWithResponse() {
            @Override
            public void response(MessageInfo messageInfo) {
                String message = messageInfo.getMessage();
                try {
                    JSONObject messageJson = new JSONObject(message);
                    if (messageJson.getString("status").equalsIgnoreCase("success")) {
                        showMessageCommunicationStatus(
                                getContext().getResources().getString(R.string.success)
                                , getOTAMessage(messageJson.getString("message")));
                        fragment.removeDevicesFromDeviceList(deviceInfoArrayList.get(index));
                    } else {
                        showMessageCommunicationStatus(
                                getContext().getResources().getString(R.string.actionFailed)
                                , getOTAMessage(messageJson.getString("message")));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(Exception e) {

            }

            @Override
            public void success() {

            }
        });
    }

    private String getOTAMessage(String message) {
        String tempArr[] = message.split(":");
        if (tempArr.length > 1) {
            return tempArr[1];
        }
        return null;
    }

    public void showMessageCommunicationStatus(final String title, final String message) {

        homeTabsActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(getContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create();
                alert.show();
            }
        });

    }
}
