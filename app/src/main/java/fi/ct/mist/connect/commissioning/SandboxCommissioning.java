package fi.ct.mist.connect.commissioning;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
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

import org.bson.BSONException;
import org.bson.BsonArray;
import org.bson.BsonBinaryReader;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.RawBsonDocument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import fi.ct.mist.connect.CommissionItem;
import fi.ct.mist.connect.commissioning.CommissioningStateMachine.Event;
import fi.ct.mist.connect.commissioning.CommissioningStateMachine.State;
import fi.ct.mist.connect.commissioning.CommissioningStateMachine.StateMachineListener;
import fi.ct.mist.dialogs.CommissioningInterruptedDialog;
import fi.ct.mist.dialogs.CommissioningTimeoutDialog;
import fi.ct.mist.mist.R;
import mist.api.request.Sandbox;
import mist.api.request.Signals;
import wish.Identity;
import wish.LocalDiscovery;
import wish.Peer;
import wish.request.Wld;


public class SandboxCommissioning implements ListLocalIdentitiesFragment.ListLocalIdentitiesListener, ClaimFragment.ClaimListener {
    public static final String TAG = "SandboxCommissioning";

    private Toolbar toolbar;
    private Snackbar snackbar;
    private ProgressDialog progressDialog;

    private Context _context;
    private byte[] sandboxId;
    private CommissionItem item;
    private int signalsId;
    // private byte[] uid;

    private int originalWifiId;
    private int commissioningWifiId;

    public final static int COMMISSIONING_CANCELLED = 1;
    public final static int COMMISSIONING_SUCCESS = 2;
    public final static int COMMISSIONING_RETRY = 3;

    private ListLocalIdentitiesFragment listLocalIdentitiesFragment;

    private int timoutState;

    public SandboxCommissioning(final byte[] sandboxId, CommissionItem item, Context context) {

        this.sandboxId = sandboxId;
        this.item = item;
        this._context = context;
        //transaction.add(R.id.fragment_container, listLocalIdentitiesFragment);
        CommissioningStateMachine.getInstance().initialize(stateMachineListener);
        CommissioningStateMachine.getInstance().reportEvent(Event.ON_START_COMMISSIONING);

        signalsId = Signals.sandbox(new Signals.SandboxCb() {
            @Override
            public void cb(byte[] id, String hint) {
                super.cb(id, hint);
                if (!Arrays.equals(id, sandboxId)) {
                    return;
                }


            }

            @Override
            public void cb(byte[] id, String hint, BsonDocument opts) {
                super.cb(id, hint, opts);
                if (!Arrays.equals(id, sandboxId)) {
                    return;
                }

                if (hint.equals("commission.setWifi")) {
                    String ssid;
                    String password;
                    try {
                        ssid = opts.getString("ssid").getValue();
                        password = opts.getString("password").getValue();
                    } catch (BSONException e) {
                        BsonDocument bsonDocument = new BsonDocument().append("hint", new BsonString(e.getMessage()));
                        Sandbox.emit(sandboxId, "commission.err", bsonDocument, new Sandbox.EmitCb() {
                            @Override
                            public void cb(boolean b) {
                                CommissioningStateMachine.getInstance().reportEvent(Event.ON_ERROR);
                            }
                        });
                        return;
                    }
                    CommissioningStateMachine.getInstance().sendWifiConfiguration(ssid, password);
                }
            }
        });

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

            BsonDocument bsonDocument = new BsonDocument().append("hint", new BsonString(state.toString()));
            Sandbox.emit(sandboxId, "commission.timeout", bsonDocument, new Sandbox.EmitCb() {
                @Override
                public void cb(boolean b) {
                    CommissioningStateMachine.getInstance().reportEvent(Event.ON_END_COMMISSIONING);
                }
            });
        }

        @Override
        public void onInterrupted(State state) {

            BsonDocument bsonDocument = new BsonDocument().append("hint", new BsonString(state.toString()));
            Sandbox.emit(sandboxId, "commission.interrupted", bsonDocument, new Sandbox.EmitCb() {
                @Override
                public void cb(boolean b) {
                    CommissioningStateMachine.getInstance().reportEvent(Event.ON_END_COMMISSIONING);
                }
            });
        }

        @Override
        public void onWifiCommissioningChosen() {
            WifiManager wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);
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

                /* https://stackoverflow.com/questions/42576742/wifimanager-addnetwork-return-1-in-marshmallow */

                CommissioningStateMachine.getInstance().reportEvent(Event.ON_WIFI_ADD_FAIL);
                return;
            }

            if (originalWifiId == commissioningWifiId) {
                CommissioningStateMachine.getInstance().reportEvent(Event.ON_COMMISSIONING_WIFI_ALREADY_JOINED);
            } else {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                _context.registerReceiver(broadcastReceiver, intentFilter);


                wifiManager.enableNetwork(commissioningWifiId, true);
            }
        }

        @Override
        public void selectLocalIdentity(List<Identity> list) {
            Log.d(TAG, "selectLocalIdentity");
            BsonDocument bsonDocument = new BsonDocument().append("hint", new BsonString("selectLocalIdentity"));
            Sandbox.emit(sandboxId, "commissioning.err", bsonDocument, new Sandbox.EmitCb() {
                @Override
                public void cb(boolean b) {

                }
            });
        }

        @Override
        public CommissionItem.WldConnection onWldCommissioningChosen() {
            final CommissionItem.WldConnection wldItem = (CommissionItem.WldConnection) item;

            return wldItem;
        }

        @Override
        public void showClaimButton() {
            onClaimButtonClicked();
        }

        @Override
        public void showWifiList(List<WifiNetworkEntry> list, Peer peer) {
            BsonDocument bsonDocument = new BsonDocument();
            BsonArray bsonArray = new BsonArray();

            for (WifiNetworkEntry entry : list) {
                BsonDocument document = new BsonDocument();
                document.append("ssid", new BsonString(entry.getSsid()));
                document.append("level", new BsonInt32(entry.getStrength()));
                bsonArray.add(document);
            }
            bsonDocument.append("args", bsonArray);

            Sandbox.emit(sandboxId, "commission.claimed", bsonDocument, new Sandbox.EmitCb() {
                @Override
                public void cb(boolean b) {
                }
            });
        }

        @Override
        public void onWifiConfigured() {
            WifiManager wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);
            if (!wifiManager.isWifiEnabled()) {
                CommissioningStateMachine.getInstance().reportEvent(Event.ON_WIFI_OFF);
                return;
            }
            wifiManager.enableNetwork(originalWifiId, true);
        }

        @Override
        public void onWifiOff() {
            _context.registerReceiver(WifiStateChangedReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        }

        @Override
        public void onWifiEnabled() {
            try {
                _context.unregisterReceiver(WifiStateChangedReceiver);
            } catch (IllegalArgumentException e) {
            }
            CommissioningStateMachine.getInstance().reportEvent(Event.ON_WIFI_COMMISSIONING_CHOSEN);
        }

        int addedPeerCnt = 0;
        @Override
        public void onFinishedOk(final ArrayList<Peer> peers) {
            Log.d(TAG, "onFinishedOk, peers size = "+ peers.size());
            if (peers.size() == 0) {
                BsonDocument bsonDocument = new BsonDocument().append("hint", new BsonString("COMMISSIONING_FINISHED_NO_PEER"));
                Sandbox.emit(sandboxId, "commission.err", bsonDocument, new Sandbox.EmitCb() {
                    @Override
                    public void cb(boolean b) {
                        CommissioningStateMachine.getInstance().reportEvent(Event.ON_ERROR);
                    }
                });
                return;
            } else {
                final BsonArray bsonArray = new BsonArray();
                addedPeerCnt = 0;
                for (Peer peer : peers) {
                    BsonDocument peerDocument = new RawBsonDocument(peer.toBson());
                    bsonArray.add(peerDocument);

                    Sandbox.addPeer(sandboxId, peer, new Sandbox.AddPeerCb() {
                        @Override
                        public void cb() {
                            Log.d(TAG, "onFinishedOk, addPeer cb");
                            addedPeerCnt++;
                            if (addedPeerCnt == peers.size()) {
                                Timer t = new Timer();
                                /* FIXME emit the commission.finished signal to sandboxed app with a timer.
                                This timer was added because it was felt that without the delay, the first control.read request from the sandboxed app could be lost, because
                                it might be sent on a connection which was just closed
                                 */
                                t.schedule(new TimerTask() {
                                    @Override
                                    public void run() {

                                        Log.d(TAG, "emitting commission.finished now!");
                                        Sandbox.emit(sandboxId, "commission.finished", new BsonDocument().append("args", bsonArray), new Sandbox.EmitCb() {
                                            @Override
                                            public void cb(boolean b) {
                                                finished();
                                            }
                                        });
                                    }
                                }, 3*1000);
                            }
                        }

                        @Override
                        public void err(int i, String s) {
                            Log.d(TAG, "onFinishedOk, addPeer err");
                        }

                        @Override
                        public void end() {
                        }
                    });

                }

                /*

*/
            }
        }

        @Override
        public void onFinishedFaild(CommissioningStateMachine.CommissioningType commissiongType) {
            if (commissiongType == CommissioningStateMachine.CommissioningType.COMMISSIONING_WIFI) {
                WifiManager wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);
                if (wifiManager.isWifiEnabled()) {
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if (originalWifiId != wifiInfo.getNetworkId()) {
                        wifiManager.enableNetwork(originalWifiId, true);
                    }
                }
            }


            BsonDocument bsonDocument = new BsonDocument().append("hint", new BsonString("COMMISSIONING_FAILD"));
            Sandbox.emit(sandboxId, "commission.err", bsonDocument, new Sandbox.EmitCb() {
                @Override
                public void cb(boolean b) {
                    CommissioningStateMachine.getInstance().reportEvent(Event.ON_ERROR);
                }
            });
            finished();
        }

        @Override
        public void onFinishedAborted(CommissioningStateMachine.CommissioningType commissiongType) {
            if (commissiongType == CommissioningStateMachine.CommissioningType.COMMISSIONING_WIFI) {
                WifiManager wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);
                if (wifiManager.isWifiEnabled()) {
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if (originalWifiId != wifiInfo.getNetworkId()) {
                        wifiManager.enableNetwork(originalWifiId, true);
                    }
                }
            }

            BsonDocument bsonDocument = new BsonDocument().append("hint", new BsonString("COMMISSIONING_ABORTED"));
            Sandbox.emit(sandboxId, "commission.err", bsonDocument, new Sandbox.EmitCb() {
                @Override
                public void cb(boolean b) {
                    CommissioningStateMachine.getInstance().reportEvent(Event.ON_ERROR);
                }
            });
            finished();
        }

        @Override
        public void infoText(Integer firstLine, Integer secondLine) {
            Log.d(TAG, "infotext: " + firstLine + " : " + secondLine);
        }

        @Override
        public void progress(State state) {
            BsonDocument bsonDocument = new BsonDocument().append("state", new BsonString(state.toString()));
            Sandbox.emit(sandboxId, "commission.progress", bsonDocument, new Sandbox.EmitCb() {
                @Override
                public void cb(boolean b) {
                }
            });
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
                WifiManager wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);
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
        ConnectivityManager connectivityManager = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onIdentityClicked(Identity identity) {
        CommissioningStateMachine.getInstance().setLocalIdentity(identity.getUid());
    }


    @Override
    public void onClaimButtonClicked() {
        CommissioningStateMachine.getInstance().reportEvent(Event.ON_CLAIM_CLICKED);
    }

    protected void finished() {

        CommissioningStateMachine.getInstance().reset();
        try {
            _context.unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
        }
        try {
            _context.unregisterReceiver(WifiStateChangedReceiver);
        } catch (IllegalArgumentException e) {
        }
    }


}
