package com.mavid.alexa_signin;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;

import org.json.JSONException;
import org.json.JSONObject;

import com.amazon.identity.auth.device.authorization.api.AuthzConstants;

import com.danimahardhika.cafebar.CafeBar;
import com.mavid.MavidHomeTabsActivity;
import com.mavid.R;
import com.mavid.libresdk.LibreMavidHelper;
import com.mavid.libresdk.TaskManager.Alexa.CompanionProvisioningInfo;
import com.mavid.libresdk.TaskManager.Alexa.DeviceProvisioningInfo;
import com.mavid.libresdk.TaskManager.Communication.Listeners.CommandStatusListenerWithResponse;
import com.mavid.libresdk.TaskManager.Discovery.CustomExceptions.WrongStepCallException;
import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.MessageInfo;

import java.util.Timer;
import java.util.TimerTask;

import static com.mavid.Dialogs.AlexaDialogs.somethingWentWrong;

public class AlexaSignInActivity extends AppCompatActivity implements View.OnClickListener {

    private Button bt_signIn;
    private AppCompatImageView iv_home;
    private AppCompatImageView iv_back;
    private Dialog m_progressDlg;


    DeviceProvisioningInfo mDeviceProvisioningInfo;

    //    private static DeviceProvisioningInfo mDeviceProvisioningInfo;
    public static final String PRODUCT_ID = "productID";
    public static final String ALEXA_ALL_SCOPE = "alexa:all";
    public static final String DEVICE_SERIAL_NUMBER = "deviceSerialNumber";
    public static final String PRODUCT_INSTANCE_ATTRIBUTES = "productInstanceAttributes";
    public static final String[] APP_SCOPES = {ALEXA_ALL_SCOPE};
    final int ALEXA_META_DATA_TIMER = 0x12;
    private AmazonAuthorizationManager mAuthManager;
    private String speakerIpaddress;
    private String from;
    private Dialog alertDialog;
    private boolean invalidApiKey;
    private int ACCESS_TOKEN_TIMEOUT = 301;
    private boolean isMetaDateRequestSent = false;

    CafeBar cafeBar;

    AppCompatTextView tv_alert_title;
    AppCompatTextView tv_alert_message;

    AppCompatButton btn_ok;

    private Dialog mDialog;

    AppCompatTextView progress_title;
    ProgressBar progress_bar;

    AppCompatTextView progress_message;

    int callMSearchfiveTimes;



    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ALEXA_META_DATA_TIMER:
                    closeLoader();
                    Log.d("atul_alexa_signin", String.valueOf(msg));
                    somethingWentWrong(AlexaSignInActivity.this);

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alexa_signin);
        if (getIntent() != null) {
            from = getIntent().getStringExtra("fromActivity");
            speakerIpaddress = getIntent().getStringExtra("speakerIpaddress");
//            mDeviceProvisioningInfo = (DeviceProvisioningInfo) getIntent().getSerializableExtra("deviceProvisionInfo");
        }

        bt_signIn = (Button) findViewById(R.id.btn_login_amazon);
        iv_home = (AppCompatImageView) findViewById(R.id.iv_home);
//        iv_back = findViewById(R.id.iv_back);

//        iv_back.setOnClickListener(this);
        bt_signIn.setOnClickListener(this);
        iv_home.setOnClickListener(this);

        showLoader("Please wait..","");
        callMSearchEvery500ms(new Timer());

    }



    public String getMsearchPayload() {
        String mSearchPayload = "M-SEARCH * HTTP/1.1\r\n" +
                "MX: 10\r\n" +
                "ST: urn:schemas-upnp-org:device:DDMSServer:1\r\n" +
                "HOST: 239.255.255.250:1800\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "\r\n";
        return mSearchPayload;
    }



    public void callMSearchEvery500ms(final Timer timer){
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                LibreMavidHelper.setAdvertiserMessage(getMsearchPayload());
                try {
                    if (callMSearchfiveTimes < 5) {
                        LibreMavidHelper.advertiseWithIp(speakerIpaddress,AlexaSignInActivity.this);
                        callMSearchfiveTimes = callMSearchfiveTimes + 1;
                        Log.d("atul_m-search", "count:" + String.valueOf(callMSearchfiveTimes)+"ipAddress: "+speakerIpaddress);
                    } else {
                        closeLoader();
                        timer.cancel();
                    }
                } catch (WrongStepCallException e) {
                    e.printStackTrace();
                    Log.d("atul_m-search_except", e.toString());
                }
            }
        };
        timer.schedule(doAsynchronousTask, 0, 500); //execute in every 500 ms
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            mAuthManager = new AmazonAuthorizationManager(this, Bundle.EMPTY);
            invalidApiKey = false;
        } catch (Exception e) {
            invalidApiKey = true;
            buildSnackBar("" + e.getMessage());
            bt_signIn.setEnabled(false);
            bt_signIn.setAlpha(0.5f);
        }
        setMetaDateRequestSent(false);
    }

    @Override
    public void onClick(View view) {

        int i = view.getId();
        if (i == R.id.btn_login_amazon) {/* startActivity(new Intent(this, WebViewActivity.class));*/

            if (invalidApiKey) {
                somethingWentWrong(AlexaSignInActivity.this);
                return;
            }


            showLoader(getString(R.string.notice), getString(R.string.fetchingDetails));
            handler.sendEmptyMessageDelayed(ALEXA_META_DATA_TIMER, 15000);
            LibreMavidHelper.askMetaDataInfo(speakerIpaddress, new CommandStatusListenerWithResponse() {
                @Override
                public void response(MessageInfo messageInfo) {
                    String alexaMessage = messageInfo.getMessage();
                    Log.d("Alexalogin", "Alexa Value From 234  " + alexaMessage);
                    try {
                        final String TAG_WINDOW_CONTENT = "Window CONTENTS";
                        JSONObject jsonRootObject = new JSONObject(alexaMessage);
                        // JSONArray jsonArray = jsonRootObject.optJSONArray("Window CONTENTS");
                        JSONObject jsonObject = jsonRootObject.getJSONObject(TAG_WINDOW_CONTENT);
                        String productId = jsonObject.optString("PRODUCT_ID").toString();
                        String dsn = jsonObject.optString("DSN").toString();
                        String sessionId = jsonObject.optString("SESSION_ID").toString();
                        String codeChallenge = jsonObject.optString("CODE_CHALLENGE").toString();
                        String codeChallengeMethod = jsonObject.optString("CODE_CHALLENGE_METHOD").toString();
                        String locale = "";
                        if (jsonObject.has("LOCALE"))
                            locale = jsonObject.optString("LOCALE").toString();
                        mDeviceProvisioningInfo = new DeviceProvisioningInfo(productId, dsn, sessionId, codeChallenge, codeChallengeMethod, locale);
                        handler.removeMessages(ALEXA_META_DATA_TIMER);
                        setAlexaViews();
                        closeLoader();
                        Bundle options = new Bundle();

                        JSONObject scopeData = new JSONObject();
                        JSONObject productInfo = new JSONObject();
                        JSONObject productInstanceAttributes = new JSONObject();
                        productInstanceAttributes.put(DEVICE_SERIAL_NUMBER, mDeviceProvisioningInfo.getDsn());
                        productInfo.put(PRODUCT_ID, mDeviceProvisioningInfo.getProductId());
                        productInfo.put(PRODUCT_INSTANCE_ATTRIBUTES, productInstanceAttributes);
                        scopeData.put(ALEXA_ALL_SCOPE, productInfo);

                        options.putString(AuthzConstants.BUNDLE_KEY.SCOPE_DATA.val, scopeData.toString());
                        options.putBoolean(AuthzConstants.BUNDLE_KEY.GET_AUTH_CODE.val, true);
                        options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE.val, codeChallenge);
                        options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE_METHOD.val, codeChallengeMethod);
                        if (mAuthManager != null) {
                            mAuthManager.authorize(APP_SCOPES, options, new AuthListener());
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void failure(Exception e) {
                    Log.d("Alexalogin", e.getMessage());
                }

                @Override
                public void success() {

                }
            });

        }
        else if (i == R.id.iv_home) {

            Intent intent = new Intent(AlexaSignInActivity.this,MavidHomeTabsActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("mSACConfiguredIpAddress",speakerIpaddress);
            intent.putExtras(bundle);
            startActivity(intent);
            finish();
            //go to homescreen
//            if (from != null && !from.isEmpty()) {
//                startActivity(new Intent(this, AlexaThingsToTryDoneActivity.class)
//                        .putExtra("speakerIpaddress", speakerIpaddress)
//                        .putExtra("fromActivity", from)
//                        .putExtra("prevScreen", AlexaSignInActivity.class.getSimpleName()));
//                finish();
//            }

        }
    }


    public void buildSnackBar(String message) {
        CafeBar.Builder builder = CafeBar.builder(AlexaSignInActivity.this);
        builder.autoDismiss(true);
        builder.customView(R.layout.custom_snackbar_layout);

        cafeBar = builder.build();
        AppCompatTextView tv_message = cafeBar.getCafeBarView().findViewById(R.id.tv_message);
        tv_message.setText(message);


        cafeBar.show();
    }

    private void setAlexaViews() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bt_signIn.setEnabled(true);
                bt_signIn.setAlpha(1f);
            }
        });
    }

    private void performSigninClick() {
        findViewById(R.id.btn_login_amazon).performClick();
    }

    private void intentToAlexaLangUpdateActivity() {
        Intent alexaLangScreen = new Intent(AlexaSignInActivity.this, AlexaLangUpdateActivity.class);
        alexaLangScreen.putExtra("current_deviceip", speakerIpaddress);
        alexaLangScreen.putExtra("fromActivity", from);
        alexaLangScreen.putExtra("prevScreen", AlexaSignInActivity.class.getSimpleName());
        startActivity(alexaLangScreen);
        finish();
    }

//    private void closeLoader() {
//        if (m_progressDlg != null) {
//            if (m_progressDlg.isShowing()) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        m_progressDlg.dismiss();
//                    }
//                });
//            }
//        }
//    }

    public void closeLoader() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mDialog != null) {
                    if (!(AlexaSignInActivity.this.isFinishing())) {
                        mDialog.dismiss();
                        mDialog = null;
                    }
                }
            }
        });
    }

//    private void showLoader() {
//        if (AlexaSignInActivity.this.isFinishing())
//            return;
//        if (m_progressDlg == null)
//            m_progressDlg = ProgressDialog.show(AlexaSignInActivity.this, getString(R.string.notice), getString(R.string.fetchingDetails), true, true, null);
//        if (!m_progressDlg.isShowing()) {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    m_progressDlg.show();
//                }
//            });
//        }
//    }

    public void showLoader(final String title, final String message) {
        if (mDialog == null) {
            mDialog = new Dialog(AlexaSignInActivity.this);
            mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mDialog.setContentView(R.layout.custom_progress_bar);

            mDialog.setCancelable(false);
            progress_title = mDialog.findViewById(R.id.progress_title);
            progress_bar = mDialog.findViewById(R.id.progress_bar);
            progress_message = mDialog.findViewById(R.id.progress_message);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("ShowingLoader", "Showing loader method");
                progress_title.setText(title);
                progress_message.setText(message);
                progress_bar.setIndeterminate(true);
                progress_bar.setVisibility(View.VISIBLE);
                if (!(AlexaSignInActivity.this.isFinishing())) {
                    mDialog.show();
                }
            }
        });
    }


    public boolean isMetaDateRequestSent() {
        return isMetaDateRequestSent;
    }

    public void setMetaDateRequestSent(boolean metaDateRequestSent) {
        isMetaDateRequestSent = metaDateRequestSent;
    }

    private class AuthListener implements AuthorizationListener {
        @Override
        public void onSuccess(Bundle response) {
            try {
                final String authorizationCode = response.getString(AuthzConstants.BUNDLE_KEY.AUTHORIZATION_CODE.val);
                final String redirectUri = mAuthManager.getRedirectUri();
                final String clientId = mAuthManager.getClientId();
                final String sessionId = mDeviceProvisioningInfo.getSessionId();

                final CompanionProvisioningInfo companionProvisioningInfo = new CompanionProvisioningInfo(sessionId, clientId, redirectUri, authorizationCode);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showLoader(getString(R.string.notice), getString(R.string.fetchingDetails));
                    }
                });
                LibreMavidHelper.sendAlexaAuthDetails(speakerIpaddress, companionProvisioningInfo, new CommandStatusListenerWithResponse() {
                    @Override
                    public void response(final MessageInfo messageInfo) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                closeLoader();
                                Log.d("Alexa sign in", " got 235 response : " + messageInfo.getMessage());
                                intentToAlexaThingsToTryActivity();
                            }
                        });
                    }

                    @Override
                    public void failure(final Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("atul_alexa_signin", e.getMessage());
                            }
                        });

                    }

                    @Override
                    public void success() {
                        //intentToAlexaLangUpdateActivity();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                intentToAlexaThingsToTryActivity();
                            }
                        });
                    }
                });
                Log.e("Alexa sign in", "Alexa authorization successfull : " + companionProvisioningInfo.toJson().toString());

            } catch (final AuthError authError) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("atul_alexa_auth_error", authError.getMessage());
                        authError.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(final AuthError ae) {
            Log.e("LSSDPNETWORK", "AuthError during authorization", ae);
            String error = ae.getMessage();
            if (error == null || error.isEmpty())
                error = ae.toString();
            final String finalError = error;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing()) {
                        showAlertDialog(finalError);
                    }
                }
            });
        }

        @Override
        public void onCancel(Bundle cause) {
            Log.e("LSSDPNETWORK", "User cancelled authorization");
            final String finalError = "User cancelled signin";
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing()) {
                        showAlertDialog(finalError);
                    }
                }
            });
        }
    }

    private void intentToAlexaThingsToTryActivity() {
        Intent i = new Intent(AlexaSignInActivity.this, AlexaThingsToTryDoneActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("speakerIpaddress", speakerIpaddress);
        i.putExtra("fromActivity", MavidHomeTabsActivity.class.getSimpleName());
        startActivity(i);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeLoader();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);

    }

    private void showAlertDialog(String error) {

        if (alertDialog != null && alertDialog.isShowing())
            alertDialog.dismiss();

        alertDialog = null;

        alertDialog = new Dialog(AlexaSignInActivity.this);

        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        alertDialog.setContentView(R.layout.custom_single_button_layout);

        alertDialog.setCancelable(false);

        tv_alert_title = alertDialog.findViewById(R.id.tv_alert_title);

        tv_alert_message = alertDialog.findViewById(R.id.tv_alert_message);

        btn_ok = alertDialog.findViewById(R.id.btn_ok);

        btn_ok.setText("Close");

        tv_alert_title.setText("Alexa Signin Error");

        tv_alert_message.setText(error);

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();

//        AlertDialog.Builder builder = new AlertDialog.Builder(AlexaSignInActivity.this);
//        builder.setTitle("Alexa Signin Error");
//        builder.setMessage(error);
//        builder.setNeutralButton("Close", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialogInterface, int i) {
//                alertDialog.dismiss();
//            }
//        });
//
//        if (alertDialog == null) {
//            alertDialog = builder.create();
//            alertDialog.show();
//        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handleBack();
    }

    private void handleBack() {
        startActivity(new Intent(AlexaSignInActivity.this, MavidHomeTabsActivity.class));
        finish();
    }
}
