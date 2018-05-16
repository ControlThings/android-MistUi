package fi.ct.mist.connect.commissioning;

import android.os.Looper;
import android.util.Log;

import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import fi.ct.mist.connect.CommissionItem;
import fi.ct.mist.mist.R;
import wish.LocalDiscovery;
import wish.Peer;
import mist.node.request.Control;
import wish.request.Connection;
import wish.request.Host;
import wish.request.Identity;
import mist.node.request.Manage;
import mist.api.request.Mist;
import wish.request.Wish;
import wish.request.Wld;

/**
 * Created by jan on 3/2/17.
 */

public class CommissioningStateMachine implements ListFriendWifisFragment.Commissioner {
    private final String TAG = "CommissioningFSM";

    private State currentState = State.INITIAL;

    private Stack<State> stateHistory = new Stack<>();
    private Stack<Event> eventHistory = new Stack<>();

    private StateMachineListener listener;
    /**
     * The Singleton instance of the state machine is stored here
     */
    private Timeout timeout;

    private LocalDiscovery friendCandidate;

    private byte[] luid;

    private CommissionItem.WldConnection wldItem;

    private int wishSignalsId;
    private int mistSignalsId;

    private Peer commissioningPeer;
    private ArrayList<Peer> peers = new ArrayList<>();

    private CommissioningType commissioningType = CommissioningType.COMMISSIONING_UNSET;

    private boolean originallyOnClaimingWifi = false;

    public enum CommissioningType {
        COMMISSIONING_UNSET,
        COMMISSIONING_WIFI,
        COMMISSIONING_WLD,
    }

    public enum Info {
        ALWAYS(3),
        SOMETIMES(2),
        NEVER(1);

        private int value;

        Info(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum Event {
        ON_START_COMMISSIONING(R.string.ON_START_COMMISSIONING, Info.SOMETIMES),
        ON_CANCELLED_BY_USER(R.string.ON_CANCELLED_BY_USER, Info.SOMETIMES),
        ON_TIMEOUT(R.string.ON_TIMEOUT, Info.SOMETIMES),
        ON_ERROR(R.string.ON_ERROR, Info.ALWAYS),
        ON_WIFI_COMMISSIONING_CHOSEN(R.string.ON_WIFI_COMMISSIONING_CHOSEN, Info.SOMETIMES),
        ON_WIFI_OFF(R.string.ON_WIFI_OFF, Info.ALWAYS),
        ON_WIFI_ENABLED(R.string.ON_WIFI_ENABLED, Info.SOMETIMES),
        ON_WIFI_ADD_FAIL(R.string.ON_WIFI_ADD_FAIL, Info.SOMETIMES),
        ON_CONNECTED_TO_EXPECTED_WIFI(R.string.ON_CONNECTED_TO_EXPECTED_WIFI, Info.SOMETIMES),
        ON_COMMISSIONING_WIFI_ALREADY_JOINED(R.string.ON_COMMISSIONING_WIFI_ALREADY_JOINED, Info.SOMETIMES),
        ON_WLD_CLEAR_CB(R.string.ON_WLD_CLEAR_CB, Info.NEVER),
        ON_WLD_LIST_OK(R.string.ON_WLD_LIST_OK, Info.SOMETIMES), /* wld.list result has exactly one item, which we will use as commissioning target. Send friend req during this transition */
        ON_WLD_COMMISIONING_CHOSEN(R.string.ON_WLD_COMMISIONING_CHOSEN, Info.SOMETIMES), /* We send fried req with this transition */
        ON_LOCAL_ID_CHOSEN(R.string.ON_LOCAL_ID_CHOSEN, Info.SOMETIMES),
        ON_LOCAL_ID_CHOSEN_CASE_WLD(R.string.ON_LOCAL_ID_CHOSEN_CASE_WLD, Info.SOMETIMES),
        ON_FRIEND_REQ_ACCEPTED(R.string.ON_FRIEND_REQ_ACCEPTED, Info.SOMETIMES), /* accepted by peer */
        ON_DETECT_MIST_CONFIG_PEER(R.string.ON_DETECT_MIST_CONFIG_PEER, Info.SOMETIMES),
        ON_CLAIM_CLICKED(R.string.ON_CLAIM_CLICKED, Info.SOMETIMES),
        ON_MANAGE_CLAIM_CB_WIFI_COMMISSIONING(R.string.ON_MANAGE_CLAIM_CB_WIFI_COMMISSIONING, Info.SOMETIMES),
        ON_MANAGE_CLAIM_CB_WLD_COMMISSIONING(R.string.ON_MANAGE_CLAIM_CB_WLD_COMMISSIONING, Info.SOMETIMES),
        ON_WIFI_CONFIGURED(R.string.ON_WIFI_CONFIGURED, Info.SOMETIMES),
        ON_CONNECTED_TO_ORIGINAL_WIFI(R.string.ON_CONNECTED_TO_ORIGINAL_WIFI, Info.SOMETIMES),
        ON_BACK_PRESSED(R.string.ON_BACK_PRESSED, Info.SOMETIMES),
        ON_COMMISSIONED_PEER_ONLINE(R.string.ON_COMMISSIONED_PEER_ONLINE, Info.SOMETIMES),
        ON_END_COMMISSIONING(R.string.ON_END_COMMISSIONING, Info.SOMETIMES);

        private int resourceId;
        private int value;

        public int getResourceId() {
            return resourceId;
        }

        Event(int id, Info info) {
            this.resourceId = id;
            this.value = info.getValue();
        }
    }

    public enum State {
        INITIAL(R.string.INITIAL, Info.NEVER, Timeout.NO_TIMEOUT),
        NO_CALLBACK_LISTENER(R.string.NO_CALLBACK_LISTENER, Info.SOMETIMES, Timeout.NO_TIMEOUT),
        WAIT_WLD_OR_WIFI_SELECT(R.string.WAIT_WLD_OR_WIFI_SELECT, Info.SOMETIMES, Timeout.NO_TIMEOUT),
        WAIT_JOIN_COMMISIONING_WIFI(R.string.WAIT_JOIN_COMMISIONING_WIFI, Info.SOMETIMES, 15), //todo saved wifi
        WAIT_WIFI_ENABLED(R.string.WAIT_WIFI_ENABLED, Info.SOMETIMES, Timeout.NO_TIMEOUT),
        WAIT_WLD_CLEAR(R.string.WAIT_WLD_CLEAR, Info.SOMETIMES, 5),
        WAIT_WLD_LIST(R.string.WAIT_WLD_LIST, Info.SOMETIMES, 30),
        WAIT_SELECT_LOCAL_ID(R.string.WAIT_SELECT_LOCAL_ID, Info.SOMETIMES, Timeout.NO_TIMEOUT),
        WAIT_FRIEND_REQ_RESP(R.string.WAIT_FRIEND_REQ_RESP, Info.SOMETIMES, 70),
        WAIT_FOR_PEERS(R.string.WAIT_FOR_PEERS, Info.SOMETIMES, 70), /* Needs to be this high because of current configuration of Sonoffs (connection test interval) */
        WAIT_FOR_CLAIM_USER_DECISION(R.string.WAIT_FOR_CLAIM_USER_DECISION, Info.SOMETIMES, Timeout.NO_TIMEOUT),
        WAIT_MANAGE_CLAIM_CB(R.string.WAIT_MANAGE_CLAIM_CB, Info.SOMETIMES, 15),
        WAIT_WIFI_CONFIG(R.string.WAIT_WIFI_CONFIG, Info.SOMETIMES, Timeout.NO_TIMEOUT),
        WAIT_JOIN_ORIGINAL_WIFI(R.string.WAIT_JOIN_ORIGINAL_WIFI, Info.SOMETIMES, 20),
        WAIT_COMMISSIONED_PEER_ONLINE(R.string.WAIT_COMMISSIONED_PEER_ONLINE, Info.SOMETIMES, 45),
        COMMISSIONING_FINISHED_OK(R.string.COMMISSIONING_FINISHED_OK, Info.NEVER, Timeout.NO_TIMEOUT),
        COMMISSIONING_FINISHED_FAIL(R.string.COMMISSIONING_FINISHED_FAIL, Info.SOMETIMES, Timeout.NO_TIMEOUT),
        COMMISSIONING_ABORTED(R.string.COMMISSIONING_ABORTED, Info.SOMETIMES, Timeout.NO_TIMEOUT),
        WAIT_WLD_CHECK_CLAIMABLE(R.string.WAIT_WLD_CHECK_CLAIMABLE, Info.SOMETIMES, 60);

        private int resourceId;
        private int value;
        private int timeout;

        public int getResourceId() {
            return resourceId;
        }

        public int getTimeout() {
            return timeout;
        }

        State(int id, Info info, int timeout) {
            this.resourceId = id;
            this.value = info.getValue();
            this.timeout = timeout;
        }
    }


    private static final CommissioningStateMachine INSTANCE = new CommissioningStateMachine();

    private CommissioningStateMachine() {
        if (INSTANCE != null) {
            throw new IllegalStateException("GuidedCommissioning state machine already instantiated");
        } else {
            timeout = new Timeout();
        }
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Cannot clone instance of this class");
    }

    /**
     * Getter function for the singleton instance
     */
    public static CommissioningStateMachine getInstance() {
        return INSTANCE;
    }

    public State getCurrentState() {
        return currentState;
    }

    public void initialize(StateMachineListener stateMachineListener) {
        reset();
        listener = stateMachineListener;
    }

    public void reset() {
        currentState = State.INITIAL;

        stateHistory = new Stack<>();
        eventHistory = new Stack<>();

        peers.clear();
        //listener = null;

        timeout.cancel();

        friendCandidate = null;

        luid = null;

        if (wishSignalsId != 0) {
            Wish.cancel(wishSignalsId);
            wishSignalsId = 0;
        }

        if (mistSignalsId != 0) {
            Mist.cancel(mistSignalsId);
            mistSignalsId = 0;
        }

        if (peerListSignalsId != 0) {
            Mist.cancel(peerListSignalsId);
            peerListSignalsId = 0;
        }

        if (wldListTimer != null) {
            wldListTimer.cancel();
        }

        commissioningPeer = null;

        commissioningType = CommissioningType.COMMISSIONING_UNSET;

        originallyOnClaimingWifi = false;
    }

    private void onBack() {
        transition(State.COMMISSIONING_ABORTED);
        reportEvent(Event.ON_END_COMMISSIONING);
    }

    public void reportEvent(Event e) {

        eventHistory.push(e);
        logEvent(e);
        if (listener == null) {
            /* Don't call reportEvent here, it will recurse for ever until stack overflows! */
            transition(State.NO_CALLBACK_LISTENER);
            logEvent(Event.ON_ERROR);
            //  onError();
            Log.d(TAG, "GuidedCommissioning failed because listener is null!");
            // transition(State.COMMISSIONING_FINISHED_FAIL);
            return;
        }

        if (e == Event.ON_ERROR) {
            onError();
            return;
        }

        if (e == Event.ON_BACK_PRESSED) {
            onBack();
            return;
        }

        if (e == Event.ON_TIMEOUT) {
            if (wishSignalsId != 0) {
                Wish.cancel(wishSignalsId);
                wishSignalsId = 0;
            }
            if (mistSignalsId != 0) {
                Mist.cancel(mistSignalsId);
                mistSignalsId = 0;
            }
            transition(State.COMMISSIONING_FINISHED_FAIL);
            listener.onTimeOut(stateHistory.peek());
            return;
        }

        switch (currentState) {
            case INITIAL:
                if (e == Event.ON_START_COMMISSIONING) {
                    transition(State.WAIT_WLD_OR_WIFI_SELECT);
                    listener.onStartCommissioning();
                }
                break;
            case WAIT_WLD_OR_WIFI_SELECT:
                if (e == Event.ON_WIFI_COMMISSIONING_CHOSEN) {

                    commissioningType = CommissioningType.COMMISSIONING_WIFI;
                    transition(State.WAIT_JOIN_COMMISIONING_WIFI);
                    listener.onWifiCommissioningChosen();
                } else if (e == Event.ON_WLD_COMMISIONING_CHOSEN) {

                    commissioningType = CommissioningType.COMMISSIONING_WLD;
                    transition(State.WAIT_WLD_CHECK_CLAIMABLE);
                    wldItem = listener.onWldCommissioningChosen();
                    onStartWldCommissioning();
                }
                break;
            case WAIT_JOIN_COMMISIONING_WIFI:
                if (e == Event.ON_CONNECTED_TO_EXPECTED_WIFI) {
                    transition(State.WAIT_WLD_CLEAR);
                    onConnectedToExpectedWifi();
                }
                if (e == Event.ON_COMMISSIONING_WIFI_ALREADY_JOINED) {
                    originallyOnClaimingWifi = true;
                    transition(State.WAIT_WLD_CLEAR);
                    onConnectedToExpectedWifi();
                }
                if (e == Event.ON_WIFI_OFF) {
                    transition(State.WAIT_WIFI_ENABLED);
                    listener.infoText(R.string.USER_MSG_ON_WIFI_OFF, null);
                    listener.onWifiOff();
                }
                if (e == Event.ON_WIFI_ADD_FAIL) {
                    //listener.infoText(R.string.USER_MSG_CANNOT_ADD_WIFI, null);
                    Log.d(TAG, "infotext should be shown");

                    listener.onInterrupted(currentState);
                    transition(State.COMMISSIONING_FINISHED_FAIL);
                    break;
                }

                break;
            case WAIT_WIFI_ENABLED:
                if (e == Event.ON_WIFI_ENABLED) {
                    transition(State.WAIT_WLD_OR_WIFI_SELECT);
                    listener.onWifiEnabled();
                }
                break;
            case WAIT_WLD_CLEAR:
                if (e == Event.ON_WLD_CLEAR_CB) {
                    transition(State.WAIT_WLD_LIST);
                }
                /* FALLTHROUGH */
            case WAIT_WLD_CHECK_CLAIMABLE:
            case WAIT_WLD_LIST:
                if (e == Event.ON_WLD_LIST_OK) {
                    transition(State.WAIT_SELECT_LOCAL_ID);
                    onWldListOk();
                }
                break;
            case WAIT_SELECT_LOCAL_ID:
                if (e == Event.ON_LOCAL_ID_CHOSEN) {
                    transition(State.WAIT_FRIEND_REQ_RESP);
                    onLocalIdChosen();
                }
                break;
            case WAIT_FRIEND_REQ_RESP:
                if (e == Event.ON_FRIEND_REQ_ACCEPTED) {
                    if (mistSignalsId != 0) {
                        Mist.cancel(mistSignalsId);
                    }
                    mistSignalsId = 0;


                    transition(State.WAIT_FOR_PEERS);
                    onWaitForPeers();
                }


                if (e == Event.ON_CONNECTED_TO_EXPECTED_WIFI) {
                    /* Retry the friend req */
                    if (mistSignalsId != 0) {
                        Mist.cancel(mistSignalsId);
                    }
                    mistSignalsId = 0;
                    Log.d(TAG, currentState + ": Retrying friend request.");
                    transition(State.WAIT_WLD_CLEAR);
                    onConnectedToExpectedWifi();
                }

                /* In case we would miss friend request accepted signal for some reason, we might still get signal for expected peer online, in which case we can continue. */
                if (e == Event.ON_DETECT_MIST_CONFIG_PEER) {
                    Log.d(TAG, "UNEXPECTED: the friendRequestAccepted signal did not arrive, but expected peer already online!");
                    if (mistSignalsId != 0) {
                        Mist.cancel(mistSignalsId);
                    }
                    mistSignalsId = 0;

                    transition(State.WAIT_FOR_CLAIM_USER_DECISION);
                    listener.showClaimButton();
                }

                break;
            case WAIT_FOR_PEERS:
                if (e == Event.ON_DETECT_MIST_CONFIG_PEER) {
                    if (mistSignalsId != 0) {
                        Mist.cancel(mistSignalsId);
                    }
                    mistSignalsId = 0;
                    transition(State.WAIT_FOR_CLAIM_USER_DECISION);
                    listener.showClaimButton();

                }

                if (e == Event.ON_CONNECTED_TO_EXPECTED_WIFI) {
                    timeout.setTimeout(currentState.getTimeout());
                }
                break;
            case WAIT_FOR_CLAIM_USER_DECISION:
                if (e == Event.ON_CLAIM_CLICKED) {
                    transition(State.WAIT_MANAGE_CLAIM_CB);
                    onClaimClicked();
                }
                break;
            case WAIT_MANAGE_CLAIM_CB:
                if (e == Event.ON_MANAGE_CLAIM_CB_WIFI_COMMISSIONING) {
                    transition(State.WAIT_WIFI_CONFIG);
                    onManageClaimCbWifiCommissioning();
                }
                if (e == Event.ON_MANAGE_CLAIM_CB_WLD_COMMISSIONING) {
                    transition(State.WAIT_COMMISSIONED_PEER_ONLINE);
                    reportEvent(Event.ON_COMMISSIONED_PEER_ONLINE);
                }

                break;
            case WAIT_WIFI_CONFIG:

                if (e == Event.ON_WIFI_CONFIGURED) {
                    if (originallyOnClaimingWifi) {
                        transition(State.COMMISSIONING_FINISHED_OK);
                        reportEvent(Event.ON_END_COMMISSIONING);
                    } else {

                        transition(State.WAIT_JOIN_ORIGINAL_WIFI);
                        listener.onWifiConfigured();
                    }
                }
                break;
            case WAIT_JOIN_ORIGINAL_WIFI:
                if (e == Event.ON_CONNECTED_TO_ORIGINAL_WIFI) {
                    Connection.disconnectAll(new Connection.DisconnectAllCb() {
                        @Override
                        public void cb(boolean b) {

                            Wld.clear(new Wld.ClearCb() {
                                @Override
                                public void cb(boolean b) {
                                    transition(State.WAIT_COMMISSIONED_PEER_ONLINE);
                                    onOriginalWifiJoined();
                                }
                            });
                        }
                    });

                }
                break;
            case WAIT_COMMISSIONED_PEER_ONLINE:
                if (e == Event.ON_COMMISSIONED_PEER_ONLINE) {
                    transition(State.COMMISSIONING_FINISHED_OK);
                    reportEvent(Event.ON_END_COMMISSIONING);
                }
                break;
            case COMMISSIONING_FINISHED_OK:
                if (e == Event.ON_END_COMMISSIONING) {
                    listener.onFinishedOk(peers);
                    reset();
                }
                break;
            case COMMISSIONING_FINISHED_FAIL:
                if (e == Event.ON_END_COMMISSIONING) {
                    listener.onFinishedFaild(commissioningType);
                    reset();
                }
                break;
            case COMMISSIONING_ABORTED:
                if (e == Event.ON_END_COMMISSIONING) {
                    listener.onFinishedAborted(commissioningType);
                    reset();
                }
                break;
        }


    }

    private void transition(State newState) {
        timeout.setTimeout(newState.getTimeout());
        Integer eventString = null;
        Integer stateString = null;


        if (State.NO_CALLBACK_LISTENER != newState) {
            Event e = eventHistory.peek();

            if (newState.value >= Info.NEVER.getValue()) {
                stateString = newState.resourceId;
            }
            listener.infoText(null, stateString);
        }
        if (listener != null) {
            listener.progress(newState);
        }

        if (newState != currentState) {
            State previousState = currentState;
            stateHistory.push(currentState);
            currentState = newState;
            Log.d(TAG, "transition:" + previousState.name() + "->" + currentState.name());
        } else {
            Log.d(TAG, "Illegal transition to state itself:" + currentState.name());
        }
    }

    public void printHistory() {
        /*
        for (String item : history) {
            Log.d(TAG, "history: " + item);
        }
        */
    }

    private void logEvent(Event e) {
        Log.d(TAG, "logEvent " + e.name());
        //history.add("Event: " + e.name());
    }

    public interface StateMachineListener {
        void onStartCommissioning();

        void onCancelledByUser();

        void onTimeOut(State state);

        void onInterrupted(State state);

        void onWifiCommissioningChosen();

        CommissionItem.WldConnection onWldCommissioningChosen();

        void onWifiOff();

        void onWifiEnabled();

        void selectLocalIdentity(List<wish.Identity> list);

        void showClaimButton();

        void showWifiList(List<WifiNetworkEntry> list, Peer peer);

        void onWifiConfigured();

        void onFinishedOk(ArrayList<Peer> peers);

        void onFinishedFaild(CommissioningType commissioningType);

        void onFinishedAborted(CommissioningType commissioningType);

        public void infoText(Integer firstLine, Integer secondLine);

        public void progress(State state);

    }

    private void onStartWldCommissioning() {
        Wld.list(new Wld.ListCb() {
            @Override
            public void cb(List<LocalDiscovery> list) {
                for (LocalDiscovery discovery : list) {
                    if (Arrays.equals(discovery.getRuid(), wldItem.getRuid()) &&
                            Arrays.equals(discovery.getRhid(), wldItem.getRhid())) {
                        friendCandidate = discovery;
                        reportEvent(Event.ON_WLD_LIST_OK);
                    }
                }
            }
        });
    }

    Timer wldListTimer;
    private void onConnectedToExpectedWifi() {
        Wld.clear(new Wld.ClearCb() {
            @Override
            public void cb(boolean value) {
                reportEvent(Event.ON_WLD_CLEAR_CB);
                wldListTimer = new Timer();
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        Wld.list(new Wld.ListCb() {
                            @Override
                            public void cb(final List<LocalDiscovery> arrayList) {
                                Log.d(TAG, "wld list, size is: " + arrayList.size());
                                Host.config(new Host.ConfigCb() {
                                    @Override
                                    public void cb(String localCoreVersion, byte[] localCoreHostId) {
                                        Log.d(TAG, "Host.config cb, core version: " + localCoreVersion);

                                        for (int i = 0; i < arrayList.size(); i++) {

                                            if (!Arrays.equals(arrayList.get(i).getRhid(), localCoreHostId) && currentState == State.WAIT_WLD_LIST) {
                                                friendCandidate = arrayList.get(i);
                                                reportEvent(Event.ON_WLD_LIST_OK);
                                                wldListTimer.cancel();
                                                wldListTimer = null;
                                            }

                                        }
                                    }

                                    @Override
                                    public void err(int i, String s) {

                                    }

                                    @Override
                                    public void end() {

                                    }

                                });
                            }

                            @Override
                            public void err(int i, String s) {
                                Log.d(TAG, "wld list err");
                                reportEvent(Event.ON_ERROR);
                            }

                            @Override
                            public void end() {

                            }
                        });

                    }
                };
                wldListTimer.scheduleAtFixedRate(timerTask, 0, 1000);

            }

            @Override
            public void err(int i, String s) {
                Log.d(TAG, "wld.clear err " + i + " " + s);
                reportEvent(Event.ON_ERROR);
            }

            @Override
            public void end() {

            }
        });
    }

    public void onWldListOk() {
        Identity.list(new Identity.ListCb() {
            @Override
            public void cb(List<wish.Identity> arrayList) {
                List<wish.Identity> identities = new ArrayList<wish.Identity>();
                for (wish.Identity identity : arrayList) {
                    if (identity.isPrivkey()) {
                        identities.add(identity);
                    }
                }
                if (identities.size() >= 2) {
                    listener.selectLocalIdentity(identities);
                } else {
                    setLocalIdentity(identities.get(0).getUid());
                }
            }

            @Override
            public void err(int i, String s) {
                reportEvent(Event.ON_ERROR);
            }

            @Override
            public void end() {

            }
        });


    }

    public void setLocalIdentity(byte[] luid) {
        this.luid = luid;
        reportEvent(Event.ON_LOCAL_ID_CHOSEN);
    }

    private void onLocalIdChosen() {
        wishSignalsId = Mist.signals(new Mist.SignalsCb() {
            @Override
            public void cb(String s, BsonDocument document) {
                Log.d(GuidedCommissioning.TAG, "Got signal " + s);
                if (s.equals("ok")) {
                    if (currentState == State.WAIT_FRIEND_REQ_RESP) {
                        Wld.friendRequest(luid, friendCandidate, new Wld.FriendRequestCb() {
                            @Override
                            public void cb(boolean value) {
                                Log.d(TAG, " Wld.friendRequest callback activated");
                            }

                            @Override
                            public void err(int i, String s) {
                                reportEvent(Event.ON_ERROR);
                            }

                            @Override
                            public void end() {

                            }
                        });
                    }
                }
                if (s.equals("friendRequesteeAccepted")) {
                    Identity.list(new Identity.ListCb() {
                        @Override
                        public void cb(List<wish.Identity> arrayList) {
                            for (wish.Identity identity : arrayList) {
                                if (Arrays.equals(identity.getUid(), friendCandidate.getRuid())) {
                                    reportEvent(Event.ON_FRIEND_REQ_ACCEPTED); /* FIXME: If the identity was not in the list, redo the request, and fail if it is not found after repeated requests */
                                    if (wishSignalsId != 0) {
                                        Wish.cancel(wishSignalsId);
                                    }
                                    wishSignalsId = 0;
                                }
                            }
                        }

                        @Override
                        public void err(int i, String s) {
                            reportEvent(Event.ON_ERROR);
                        }

                        @Override
                        public void end() {

                        }
                    });
                }
            }

            @Override
            public void err(int i, String s) {
                reportEvent(Event.ON_ERROR);
                Log.d(TAG, "signals Error!");
            }

            @Override
            public void end() {
                Log.d(TAG, "signals Error!");
            }
        });

    }

    private Timer readMistNameTimer;
    private int peerListSignalsId;
    private void onWaitForPeers() {



        /* First, do a Mist.ListServices to check if we already have a connection the the peer. If peers are found, add the the list 'peers' */
        Mist.listServices(new Mist.ListServicesCb() {
            @Override
            public void cb(List<Peer> arrayList) {
                Log.d(TAG, "(1) list services cb, num services (peers) is " + arrayList.size());
                for (final Peer peer : arrayList) {
                    //Log.d(TAG, "(1) A peer which is online: " + peer.isOnline());
                    if (Arrays.equals(peer.getLuid(), luid) && Arrays.equals(peer.getRuid(), friendCandidate.getRuid()) && Arrays.equals(peer.getRhid(), friendCandidate.getRhid())) {
                        Log.d(TAG, "(1) Found a peer candidate, which is online:" + peer.isOnline());
                        boolean found = false;
                        for (Peer p : peers) {
                            if (p.equals(peer)) {
                                found = true;
                            }
                        }
                        if (!found) {
                            peers.add(peer);
                        }
                        Log.d(TAG, "(1) Peers list len = " + peers.size());
                        Control.read(peer, "mist.name", new Control.ReadCb() {
                            @Override
                            public void cbString(String s) {
                                Log.d(TAG, "(1) Got mist.name response: " + s);
                                if (s.equals("MistConfig")) {
                                    commissioningPeer = peer;
                                    reportEvent(Event.ON_DETECT_MIST_CONFIG_PEER);
                                }

                            }
                        });


                    }
                }
            }
        });

        /* Start listening for peers signals, do listServices, and for each peer that are from the commissioned device, add to the peers list.  */
        peerListSignalsId = Mist.signals(new Mist.SignalsCb() {
            @Override
            public void cb(String s, BsonDocument bsonDocument) {
                if (s.equals("peers")) {
                    Mist.listServices(new Mist.ListServicesCb() {
                        @Override
                        public void cb(List<Peer> arrayList) {
                            Log.d(TAG, "list services cb" + arrayList.size());
                            for (final Peer peer : arrayList) {
                                //Log.d(TAG, "A peer which is online: " + peer.isOnline());
                                if (Arrays.equals(peer.getLuid(), luid) && Arrays.equals(peer.getRuid(), friendCandidate.getRuid()) && Arrays.equals(peer.getRhid(), friendCandidate.getRhid())) {
                                    Log.d(TAG, "Found a peer candidate, is online: " + peer.isOnline());
                                    boolean found = false;
                                    for (Peer p : peers) {
                                        if (p.equals(peer)) {
                                            found = true;
                                        }
                                    }
                                    if (!found) {
                                        peers.add(peer);
                                    }

                                }
                            }
                        }
                    });
                }
            }
        });

        /* Start to listen for signals, and when a peer signal arrives, list services and check if it is the MistConfig peer that had appeared */

        final int oldSignalsId = mistSignalsId;
        if (readMistNameTimer != null) {
            readMistNameTimer.cancel();
        }
        readMistNameTimer = new Timer();
        mistSignalsId = Mist.signals(new Mist.SignalsCb() {
            @Override
            public void cb(String s, BsonDocument bsonDocument) {
                if (s.equals("ok")) {
                    Mist.cancel(oldSignalsId);
                }
                if (s.equals("peers")) {
                    Log.d(TAG, "got peers signal");
                    /* Every time we get a peers signal, we issue ListServices, and for each listServices callback, we iterate through the list of services.
                    Once we find a service with model name "MistConfig", we report event "MistConfigFound"
                     */
                    Mist.listServices(new Mist.ListServicesCb() {
                        @Override
                        public void cb(List<Peer> arrayList) {
                            Log.d(TAG, "list services cb" + arrayList.size());
                            for (final Peer peer : arrayList) {
                                Log.d(TAG, "A peer which is online: " + peer.isOnline());
                                if (Arrays.equals(peer.getLuid(), luid) && Arrays.equals(peer.getRuid(), friendCandidate.getRuid()) && Arrays.equals(peer.getRhid(), friendCandidate.getRhid())) {
                                    Log.d(TAG, "A peer candidate which is online: " + peer.isOnline());
                                }
                                if (Arrays.equals(peer.getLuid(), luid) && Arrays.equals(peer.getRuid(), friendCandidate.getRuid()) && Arrays.equals(peer.getRhid(), friendCandidate.getRhid()) && peer.isOnline()) {
                                    Log.d(TAG, "Peers list len = " + peers.size());
                                    /* FIXME This timed control.read for mist.name was added because it was seen that mist.read could go missing, which manifests as a commissioning hang in "WAIT_FOR_PEERS" */
                                    readMistNameTimer.scheduleAtFixedRate(new TimerTask() {
                                        @Override
                                        public void run() {
                                            Control.read(peer, "mist.name", new Control.ReadCb() {
                                                @Override
                                                public void cbString(String s) {
                                                    Log.d(TAG, "Got mist.name response: " + s);
                                                    if (s.equals("MistConfig")) {
                                                        commissioningPeer = peer;
                                                        reportEvent(Event.ON_DETECT_MIST_CONFIG_PEER);
                                                        readMistNameTimer.cancel();
                                                    }

                                                }

                                                @Override
                                                public void err(int i, String s) {
                                                    Log.d(TAG, "control.read error when reading mist.name");
                                                    reportEvent(Event.ON_ERROR);
                                                }

                                            });
                                        }
                                    }, 0, 1000);


                                }
                            }
                        }

                        @Override
                        public void err(int i, String s) {

                        }

                        @Override
                        public void end() {

                        }
                    });
                }
            }

            @Override
            public void err(int i, String s) {
                reportEvent(Event.ON_ERROR);
            }

            @Override
            public void end() {
            }
        });
    }

    public void onClaimClicked() {
        Control.invoke(commissioningPeer, "claimCore", new Control.InvokeCb() {
            @Override
            public void cbDocument(BsonDocument value) {
                switch (commissioningType) {
                case COMMISSIONING_WIFI:
                    reportEvent(Event.ON_MANAGE_CLAIM_CB_WIFI_COMMISSIONING);
                    break;
                case COMMISSIONING_WLD:
                    reportEvent(Event.ON_MANAGE_CLAIM_CB_WLD_COMMISSIONING);
                    break;
                }
            }

            @Override
            public void err(int code, String msg) {
                Log.d(TAG, "claimCore error:" + code + " " + msg);
                switch (commissioningType) {
                case COMMISSIONING_WIFI:
                    reportEvent(Event.ON_MANAGE_CLAIM_CB_WIFI_COMMISSIONING);
                    break;
                case COMMISSIONING_WLD:
                    reportEvent(Event.ON_MANAGE_CLAIM_CB_WLD_COMMISSIONING);
                    break;
                }
            }
        });
    }

    private void onManageClaimCbWifiCommissioning() {
        Control.invoke(commissioningPeer, "mistWifiListAvailable", new Control.InvokeCb() {
            @Override
            public void cbDocument(BsonDocument bsonDocument) {
                List<WifiNetworkEntry> wifiList = new ArrayList<>();
                for (Map.Entry<String, BsonValue> entry : bsonDocument.entrySet()) {
                    String key = entry.getKey();

                    WifiNetworkEntry wifiNetworkEntry = new WifiNetworkEntry();
                    BsonDocument document = entry.getValue().asDocument();
                    wifiNetworkEntry.setSsid(document.get("ssid").asString().getValue());
                    wifiNetworkEntry.setStrength(document.get("rssi").asInt32().getValue());
                    wifiList.add(wifiNetworkEntry);
                }
                listener.showWifiList(wifiList, commissioningPeer);
            }
        });
    }

    public void sendWifiConfiguration(String ssid, String password) {

        BsonDocument bsonDocument = new BsonDocument();
        bsonDocument.append("ssid", new BsonString(ssid));
        bsonDocument.append("wifi_Credentials", new BsonString(password));

        Log.d(TAG, "mistWifiCommissioning will move to ssid: " + ssid + " password " + password);
        Control.invoke(commissioningPeer, "mistWifiCommissioning", bsonDocument, new Control.InvokeCb() {
        });
        /* FIXME claimed() should be called in a callback above. */

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "No callback");
                reportEvent(Event.ON_WIFI_CONFIGURED);
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 500);

    }

    public void onError() {

        if (currentState == State.NO_CALLBACK_LISTENER) {
            reset();
            Log.d(TAG, "Unrecoverable state machine error! " + "state: " + currentState);
        } else if (currentState != State.COMMISSIONING_FINISHED_FAIL) {
            transition(State.COMMISSIONING_FINISHED_FAIL);
            reportEvent(Event.ON_END_COMMISSIONING);
        }
    }

    private class Timeout {
        private TimerTask timeoutTask;
        private Timer timeoutTimer;
        public static final int NO_TIMEOUT = -1;

        private Timeout() {
        }

        public void setTimeout(int timeoutSeconds) {
            if (timeoutTimer != null) {
                timeoutTimer.cancel();
            }

            if (timeoutSeconds > 0) {
                timeoutTimer = new Timer("GuidedCommissioning timeout Timer");
                timeoutTask = new TimerTask() {
                    @Override
                    public void run() {
                        new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                reportEvent(Event.ON_TIMEOUT);
                            }
                        });
                    }
                };
                timeoutTimer.schedule(timeoutTask, timeoutSeconds * 1000);
            }
        }

        /**
         * Cancel the timeout.
         */
        public void cancel() {
            if (timeoutTimer != null) {
                timeoutTimer.cancel();
                timeoutTimer = null;
            }
        }
    }

    public class WifiComissioningCredentials {
        public String ssid;
        public String wifi_Credentials;

        public WifiComissioningCredentials(String ssid, String wifi_Credentials) {
            this.ssid = ssid;
            this.wifi_Credentials = wifi_Credentials;
        }
    }

    private void findPeerAfterOriginalWifiJoined() {
        Mist.listServices(new Mist.ListServicesCb() {
            @Override
            public void cb(List<Peer> arrayList) {

                for (Peer peer : arrayList) {
                    if (Arrays.equals(peer.getLuid(), luid) && Arrays.equals(peer.getRuid(), friendCandidate.getRuid()) && Arrays.equals(peer.getRhid(), friendCandidate.getRhid()) && peer.isOnline()) {
                        reportEvent(Event.ON_COMMISSIONED_PEER_ONLINE);
                    }
                }
            }

            @Override
            public void err(int i, String s) {

            }

            @Override
            public void end() {

            }
        });
    }

    private void onOriginalWifiJoined() {

        findPeerAfterOriginalWifiJoined();

        mistSignalsId = Mist.signals(new Mist.SignalsCb() {
            @Override
            public void cb(String s, BsonDocument bsonDocument) {
                Log.d(TAG, "onOriginalWifiJoined: signal: " + s);
                if (s.equals("peers")) {
                    findPeerAfterOriginalWifiJoined();
                }
            }



            @Override
            public void err(int i, String s) {

            }

            @Override
            public void end() {

            }
        });
    }
}
