/**
 * Copyright (C) 2020, ControlThings Oy Ab
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * @license Apache-2.0
 */
package fi.ct.mist.connect.commissioning;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import org.bson.BsonDocument;
import org.bson.BsonString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.ct.mist.connect.CommissionItem;
import fi.ct.mist.connect.commissioning.CommissioningStateMachine.Event;
import fi.ct.mist.connect.commissioning.CommissioningStateMachine.StateMachineListener;
import fi.ct.mist.connect.commissioning.CommissioningStateMachine.State;
import fi.ct.mist.dialogs.CommissioningInterruptedDialog;
import fi.ct.mist.dialogs.CommissioningTimeoutDialog;
import fi.ct.mist.mist.R;
import mist.api.request.Sandbox;
import wish.Identity;
import wish.LocalDiscovery;
import wish.Peer;
import wish.request.Wld;


public class GuidedCommissioning extends AppCompatActivity implements ListLocalIdentitiesFragment.ListLocalIdentitiesListener, ClaimFragment.ClaimListener {
    public static final String TAG = "GuidedCommissioning";

    private Toolbar toolbar;
    private Snackbar snackbar;
    private ProgressDialog progressDialog;

    private CommissionItem item;
    // private byte[] uid;

    private int originalWifiId;
    private int commissioningWifiId;
    private Activity activity;

    public final static int COMMISSIONING_CANCELLED = 1;
    public final static int COMMISSIONING_SUCCESS = 2;
    public final static int COMMISSIONING_RETRY = 3;

    private ListLocalIdentitiesFragment listLocalIdentitiesFragment;

    private int timoutState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate" + State.INITIAL.ordinal());
        setContentView(R.layout.fragment);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        item = (CommissionItem) bundle.getSerializable("item");

        // uid = intent.getByteArrayExtra("uid");

        snackbar = Snackbar.make(findViewById(R.id.fragment_layout), "", Snackbar.LENGTH_INDEFINITE);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("GuidedCommissioning ...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(State.values().length);

        toolbar = (Toolbar) findViewById(R.id.fragment_toolbar);
        updateTollbar(item.getName(), null);
        setSupportActionBar(toolbar);


        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        listLocalIdentitiesFragment = new ListLocalIdentitiesFragment();
        transaction.add(R.id.fragment_container, listLocalIdentitiesFragment);
        transaction.commit();

        activity = this;

        CommissioningStateMachine.getInstance().initialize(stateMachineListener);
        CommissioningStateMachine.getInstance().reportEvent(Event.ON_START_COMMISSIONING);
    }

    protected void onStart() {
        super.onStart();
    }

    private StateMachineListener stateMachineListener = new StateMachineListener() {
        @Override
        public void onStartCommissioning() {
            if (item.getType() == CommissionItem.TYPE_WIFI) {
                CommissioningStateMachine.getInstance().reportEvent(Event.ON_WIFI_COMMISSIONING_CHOSEN);
            } else if (item.getType() == CommissionItem.TYPE_WLD) {
                CommissioningStateMachine.getInstance().reportEvent(Event.ON_WLD_COMMISIONING_CHOSEN);
            } else {
                CommissioningStateMachine.getInstance().reportEvent(Event.ON_ERROR);
            }
        }

        @Override
        public void onCancelledByUser() {
        }

        @Override
        public void onTimeOut(State state) {

            int hint = 0;
            switch (state) {
                case WAIT_JOIN_COMMISIONING_WIFI:
                    hint = R.string.USER_MSG_WAIT_JOIN_COMMISIONING_WIFI;
                    break;
            }
            CommissioningTimeoutDialog.showDialog(activity, state.getResourceId(), hint, new CommissioningTimeoutDialog.Cb() {
                @Override
                public void onResult(int state) {
                    timoutState = state;
                    CommissioningStateMachine.getInstance().reportEvent(Event.ON_END_COMMISSIONING);
                }
            });
        }

        @Override
        public void onInterrupted(State state) {

            int hint = 0;
            switch (state) {
            case WAIT_JOIN_COMMISIONING_WIFI:
                hint = R.string.USER_MSG_CANNOT_ADD_WIFI;
                break;
            }
            CommissioningInterruptedDialog.showDialog(activity, state.getResourceId(), hint, new CommissioningInterruptedDialog.Cb() {
                @Override
                public void onResult(int state) {
                    timoutState = state;
                    CommissioningStateMachine.getInstance().reportEvent(Event.ON_END_COMMISSIONING);
                }
            });
        }

        @Override
        public void onWifiCommissioningChosen() {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (!wifiManager.isWifiEnabled()) {
                CommissioningStateMachine.getInstance().reportEvent(Event.ON_WIFI_OFF);
                return;
            }

            CommissionItem.WifiConnection wifiConnection = (CommissionItem.WifiConnection) item;
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            originalWifiId = wifiInfo.getNetworkId();

            commissioningWifiId = wifiManager.addNetwork(wifiConfiguration(wifiConnection));
            if (commissioningWifiId == -1) {
                Log.d(TAG, "Could not add the network to WifiManager!");
                CommissioningStateMachine.getInstance().reportEvent(Event.ON_WIFI_ADD_FAIL);
                return;
            }

            if (originalWifiId == commissioningWifiId) {
                CommissioningStateMachine.getInstance().reportEvent(Event.ON_COMMISSIONING_WIFI_ALREADY_JOINED);
            } else {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                registerReceiver(broadcastReceiver, intentFilter);


                wifiManager.enableNetwork(commissioningWifiId, true);
            }
        }

        @Override
        public void selectLocalIdentity(List<Identity> list) {
            Log.d(TAG, "selectLocalIdentity");
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            updateTollbar(getString(R.string.commissioning_identity_title), item.getName());
            listLocalIdentitiesFragment.setConnections(list);
        }

        @Override
        public CommissionItem.WldConnection onWldCommissioningChosen() {
            final CommissionItem.WldConnection wldItem = (CommissionItem.WldConnection) item;

            return wldItem;
        }

        @Override
        public void showClaimButton() {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.fragment_container, new ClaimFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        }

        @Override
        public void showWifiList(List<WifiNetworkEntry> list, Peer peer) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            ListFriendWifisFragment listFriendWifisFragment = new ListFriendWifisFragment();
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.fragment_container, listFriendWifisFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            updateTollbar(getString(R.string.commissioning_wifi_title), item.getName());

            listFriendWifisFragment.setCommissioner(CommissioningStateMachine.getInstance());
            listFriendWifisFragment.setWifiNetworks(list, peer);

            infoText(null, null);
        }

        @Override
        public void onWifiConfigured() {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (!wifiManager.isWifiEnabled()) {
                CommissioningStateMachine.getInstance().reportEvent(Event.ON_WIFI_OFF);
                return;
            }
            wifiManager.enableNetwork(originalWifiId, true);
        }

        @Override
        public void onWifiOff() {
            registerReceiver(WifiStateChangedReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        }

        @Override
        public void onWifiEnabled() {
            try {
                unregisterReceiver(WifiStateChangedReceiver);
            } catch (IllegalArgumentException e) {
            }
            CommissioningStateMachine.getInstance().reportEvent(Event.ON_WIFI_COMMISSIONING_CHOSEN);
        }

        @Override
        public void onFinishedOk(ArrayList<Peer> peers) {
            if (peers.size() == 0) {
                setResult(COMMISSIONING_RETRY); //fixme return some meaningful error
            } else {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("commissionedPeers", peers);
                setResult(COMMISSIONING_SUCCESS, returnIntent);
            }
            finish();
        }

        @Override
        public void onFinishedFaild(CommissioningStateMachine.CommissioningType commissiongType) {
            if (commissiongType == CommissioningStateMachine.CommissioningType.COMMISSIONING_WIFI) {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifiManager.isWifiEnabled()) {
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if (originalWifiId != wifiInfo.getNetworkId()) {
                        wifiManager.enableNetwork(originalWifiId, true);
                    }
                }
            }

            if (timoutState == CommissioningTimeoutDialog.RETRY) {
                setResult(COMMISSIONING_RETRY);
            } else {
                setResult(COMMISSIONING_CANCELLED);
            }
            finish();
        }

        @Override
        public void onFinishedAborted(CommissioningStateMachine.CommissioningType commissiongType) {
            if (commissiongType == CommissioningStateMachine.CommissioningType.COMMISSIONING_WIFI) {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifiManager.isWifiEnabled()) {
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if (originalWifiId != wifiInfo.getNetworkId()) {
                        wifiManager.enableNetwork(originalWifiId, true);
                    }
                }
            }

            setResult(COMMISSIONING_RETRY);
            finish();
        }


        @Override
        public void infoText(Integer firstLine, Integer secondLine) {
            if (firstLine == null && secondLine == null) {
                if (snackbar.isShown()) {
                    snackbar.dismiss();
                }
            } else {
                if (firstLine == null) {
                    snackbar.setText("\n" + getResources().getString(secondLine));
                } else if (secondLine == null) {
                    snackbar.setText(getResources().getString(firstLine));
                } else {
                    snackbar.setText(getResources().getString(firstLine) + "\n" +
                            getResources().getString(secondLine));
                }

                if (!snackbar.isShown()) {
                    snackbar.show();
                }
            }
        }

        @Override
        public void progress(State state) {
            progressDialog.setProgress(state.ordinal()+1);
            if (!progressDialog.isShowing()) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        progressDialog.show();
                    }
                });

            }
        }

    };

    private WifiConfiguration wifiConfiguration(CommissionItem.WifiConnection connection) {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", connection.getSsid());
        switch (connection.getSecurity()) {
            case "OPEN":
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wifiConfig.allowedAuthAlgorithms.clear();
                wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                break;
            case "WPA":
                wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                //todo get password
                    /*
                    getPassword(networkSSID);
                    wifiConfig.preSharedKey = networkPass;
                    */
                break;
        }
        return wifiConfig;
    }

    private BroadcastReceiver WifiStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int extraWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            switch (extraWifiState) {
                case WifiManager.WIFI_STATE_DISABLED:
                    break;
                case WifiManager.WIFI_STATE_DISABLING:
                    break;
                case WifiManager.WIFI_STATE_ENABLED:
                    CommissioningStateMachine.getInstance().reportEvent(Event.ON_WIFI_ENABLED);
                    break;
                case WifiManager.WIFI_STATE_ENABLING:
                    break;
                case WifiManager.WIFI_STATE_UNKNOWN:
                    break;
            }
        }
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //check if network is available
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();



                if (wifiInfo.getNetworkId() == commissioningWifiId) {
                    if (isNetworkAvailable()) {
                        CommissioningStateMachine.getInstance().reportEvent(Event.ON_CONNECTED_TO_EXPECTED_WIFI);
                    }
                }
                if (wifiInfo.getNetworkId() == originalWifiId) {
                    if (isNetworkAvailable()) {
                        CommissioningStateMachine.getInstance().reportEvent(Event.ON_CONNECTED_TO_ORIGINAL_WIFI);
                    }
                }
            }
        }
    };

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void updateTollbar(String title, String subtitle) {
        if (title != null) {
            toolbar.setTitle(title);
        }
        if (subtitle != null) {
            toolbar.setSubtitle(subtitle);
        }

    }


    @Override
    public void onIdentityClicked(Identity identity) {
        CommissioningStateMachine.getInstance().setLocalIdentity(identity.getUid());
    }

    @Override
    public void onClaimButtonClicked() {
        CommissioningStateMachine.getInstance().reportEvent(Event.ON_CLAIM_CLICKED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        CommissioningStateMachine.getInstance().reset();

        try {
            unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
        }
        try {
            unregisterReceiver(WifiStateChangedReceiver);
        } catch (IllegalArgumentException e) {
        }

        if (snackbar.isShown()) {
            snackbar.dismiss();
        }
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onBackPressed() {
        CommissioningStateMachine.getInstance().reportEvent(Event.ON_BACK_PRESSED);
    }
}
