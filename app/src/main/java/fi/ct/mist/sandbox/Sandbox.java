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
package fi.ct.mist.sandbox;

import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowManager;

import org.bson.BSONException;
import org.bson.BsonArray;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.BsonWriter;
import org.bson.RawBsonDocument;
import org.bson.io.BasicOutputBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import addon.AddonReceiver;
import fi.ct.mist.advanced.appsSettings.Settings;
import fi.ct.mist.connect.Connect;
import fi.ct.mist.dialogs.SandboxAllowDialog;
import fi.ct.mist.mist.R;
import mist.api.MistApi;
import mist.api.request.Mist;
import mist.api.request.Signals;
import mist.sandbox.AppToMist;
import mist.sandbox.Callback;

import static fi.ct.mist.Util.*;
import static org.acra.ACRA.log;

/**
 * Created by jeppe on 05/01/2017.
 */

public class Sandbox extends Service implements AddonReceiver.Receiver {

    private final String TAG = "Sandbox";
    private CustomAppWatcher customAppWatcher;
    private Loader loader;
    /* The RPC requests send by the sandbox will be saved here */
    private final ConcurrentLinkedQueue<SandboxQueuedRequest> sandboxRequestQueue = new ConcurrentLinkedQueue<>();
    /* Cancellations of RPSs will be saved here */
    private final ConcurrentLinkedQueue<SandboxQueuedCancel> sandboxCancelQueue = new ConcurrentLinkedQueue<>();
    private int nextIdToSandbox = 1; /* FIXME should be moved back to AppToMist stub so that each sandbox has "own" reqId source */

    private HashMap<IBinder, byte[]> sandboxMap = new HashMap<>(16);

    /* RPC id for the Mist.signals request we need for determining if Mist is ready to accept all requests */
    int signalsSandboxId;
    int signalsReadyId;

    public void logout(IBinder binder) {
        sandboxMap.remove(binder);
    }

    /**
     * Enqueue the request to the Sandbox request queue, from which the sandboxWorker thread will retrieve it for processing.
     *
     * @param sandboxId
     * @param data
     * @param callback
     * @return the id of the request which will be presented to the CustomUI in a sandbox
     */
    private int addToRequestQueue(byte[] sandboxId, byte[] data, Callback callback) {
        synchronized (sandboxRequestQueue) {
            int reqId = nextIdToSandbox++;
            SandboxQueuedRequest req = new SandboxQueuedRequest(sandboxId, data, callback, reqId);
            sandboxRequestQueue.add(req);
            synchronized (sandboxWorker) {
                sandboxWorker.notify();
            }
            return reqId;
        }
    }

    private void addToCancelQueue(byte[] _id, int reqId) {
        synchronized (sandboxCancelQueue) {
            sandboxCancelQueue.add(new SandboxQueuedCancel(_id, reqId));
            synchronized (sandboxWorker) {
                sandboxWorker.notify();
            }
        }
    }


    private boolean sandboxRequestQueueRemoveMatching(Callback callback) {
        boolean deleted = false;
        synchronized (sandboxRequestQueue) {
            SandboxQueuedRequest toDelete = null;
            for (SandboxQueuedRequest req : sandboxRequestQueue) {
                if (callback == req.getCallback()) {
                    if (toDelete == null) {
                        toDelete = req;
                    } else {
                        Log.d(TAG, "Unexpected: there are duplicate matching requests! req id = " + req.getIdToSandbox() + " toDelete id " + toDelete.getIdToSandbox());
                        toDelete = null;
                    }
                }
            }
            if (toDelete != null) {
                deleted = sandboxRequestQueue.remove(toDelete);
            }
        }
        return deleted;
    }

    private boolean sandboxWorkerRunning = true;
    private boolean mistReady = false;
    private final Thread sandboxWorker = new Thread(new Runnable() {
        @Override
        public void run() {
            /* First, check if already are ready.
            Note, that we have already registered a Mist.signals in onCreate().
             * If we are already "ready", we can proceed immediately to process requests in the queue.
             * Else the thread goes to sleep until we get "ready" signal.  */

            Mist.ready(new Mist.ReadyCb() {
                @Override
                public void cb(boolean readyStatus) {
                    Log.d(TAG, "ReadyCB" + readyStatus);
                    mistReady = readyStatus;
                    synchronized (sandboxWorker) {
                        sandboxWorker.notify();
                    }
                }

                @Override
                public void err(int i, String s) {

                }

                @Override
                public void end() {

                }
            });

            while (sandboxWorkerRunning) {
                if (mistReady == false) {
                    /* We have not received ready signal from Mist. We cannot safely make all RPC requests.
                    For instance, sandbox login depends on that Mist Service has started up entirely (WishApiBridgeJni.register() must be run first) */

                    Log.d(TAG, "Mist was not ready... worker thread goes to sleep.");
                    synchronized (sandboxWorker) {
                        try {
                            sandboxWorker.wait();
                        } catch (InterruptedException ie) {
                            Log.d(TAG, "worker thread unexpected wake-up");
                        }
                    }
                    continue;
                }
                /* If we get this far, it means Mist is ready */
                Log.d(TAG, "worker thread wake up");
                /* Cancel Mist.signals, if we have not done so already, now that we know that Mist is totally ready to accept all RPCs from us. */
                if (signalsReadyId != 0) {
                    Mist.cancel(signalsReadyId);
                    signalsReadyId = 0;
                }

                //request permission from generic ui

                if (signalsSandboxId != 0) {
                    Signals.cancel(signalsSandboxId);
                }
                signalsSandboxId = Signals.sandbox(new Signals.SandboxCb() {
                    @Override
                    public void cb(final byte[] id, String hint, final BsonDocument opts) {
                        super.cb(id, hint, opts);
                        if (hint.equals("permission")) {
                            try {
                                final String op = opts.getString("op").getValue();
                                mist.api.request.Sandbox.list(new mist.api.request.Sandbox.ListCb() {
                                    @Override
                                    public void cb(List<mist.api.Sandbox> list) {
                                        for (final mist.api.Sandbox sandbox : list) {
                                            if (Arrays.equals(id, sandbox.getId())) {
                                                Handler mHandler = new Handler(Looper.getMainLooper());
                                                mHandler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        AlertDialog alertDialog = new AlertDialog.Builder(getBaseContext())
                                                                .setTitle(R.string.sandbox_allow_dialog_title)
                                                                .setMessage(getBaseContext().getResources().getString(R.string.sandbox_allow_dialog_friend_request, sandbox.getName()))
                                                                .setCancelable(false)
                                                                .setNegativeButton(R.string.sandbox_allow_dialog_button_cancel, new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                        Log.d("Dialog", "cancel");
                                                                        mist.api.request.Sandbox.denyRequest(id, opts, new mist.api.request.Sandbox.DenyRequestCb() {
                                                                            @Override
                                                                            public void cb(boolean b) {
                                                                            }

                                                                            @Override
                                                                            public void err(int i, String s) {
                                                                            }

                                                                            @Override
                                                                            public void end() {
                                                                            }
                                                                        });
                                                                        dialog.dismiss();
                                                                    }
                                                                })
                                                                .setPositiveButton(R.string.sandbox_allow_dialog_button_ok, new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                        Log.d("Dialog", "ok");

                                                                        mist.api.request.Sandbox.allowRequest(id, opts, new mist.api.request.Sandbox.AllowRequestCb() {
                                                                            @Override
                                                                            public void cb(boolean b) {
                                                                            }

                                                                            @Override
                                                                            public void err(int i, String s) {
                                                                            }

                                                                            @Override
                                                                            public void end() {
                                                                            }
                                                                        });
                                                                        dialog.dismiss();
                                                                    }
                                                                })
                                                                .create();
                                                        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                                        alertDialog.show();
                                                    }
                                                });
                                                break;
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
                            } catch (BSONException e) {
                                Log.d(TAG, "bson error: " + e.getMessage());
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

                /* Process any unhandled requests from sandbox */

                byte[] sandboxId = null;
                byte[] data = null;
                Callback cb = null;
                SandboxQueuedRequest nextInQueue = null;

                synchronized (sandboxRequestQueue) {
                    Iterator<SandboxQueuedRequest> iterator = sandboxRequestQueue.iterator();
                    while (iterator.hasNext()) {
                        SandboxQueuedRequest tmp = iterator.next();
                        if (!tmp.isHandled()) {
                            nextInQueue = tmp;
                            data = nextInQueue.getData();
                            cb = nextInQueue.getCallback();
                            sandboxId = nextInQueue.getSandboxId();

                            Log.d(TAG, "found unhandled request, cb " + cb);
                            break;
                        }
                    }
                }

                int reqId = 0;
                if (sandboxId != null && data != null && cb != null && nextInQueue != null) {
                    reqId = request(sandboxId, data, cb);
                }

                synchronized (sandboxRequestQueue) {
                    if (nextInQueue != null) {
                        nextInQueue.setHandled(reqId);
                    }
                }

                /* Process any unhandled cancel requests from sandbox */

                SandboxQueuedRequest requestToRemove = null;
                synchronized (sandboxCancelQueue) {
                    Iterator<SandboxQueuedCancel> iterator = sandboxCancelQueue.iterator();
                    SandboxQueuedCancel cancelToRemove = null;
                    while (iterator.hasNext()) {
                        SandboxQueuedCancel cancelRequest = iterator.next();
                        synchronized (sandboxRequestQueue) {
                            Iterator<SandboxQueuedRequest> reqIterator = sandboxRequestQueue.iterator();

                            while (reqIterator.hasNext()) {
                                SandboxQueuedRequest req = reqIterator.next();
                                if (req.isHandled()) {
                                    if (req.getIdToSandbox() == cancelRequest.getReqId()) {
                                        /* Note: Don't call Mist.cancel here, because it could cause a deadlock. Instead, call it after we have release the locks on request and cancel queues! */
                                        if (requestToRemove == null) {
                                            requestToRemove = req;
                                        } else {
                                            Log.d(TAG, "remove from req list: unexpected, a duplicate!");
                                        }

                                        if (cancelToRemove == null) {
                                            cancelToRemove = cancelRequest;
                                        } else {
                                            Log.d(TAG, "remove from cancel req list: unexpected, a duplicate!");
                                        }

                                    }
                                }
                            }
                            if (requestToRemove != null) {
                                sandboxRequestQueue.remove(requestToRemove);
                            }
                        }
                    }
                    if (cancelToRemove != null) {
                        sandboxCancelQueue.remove(cancelToRemove);
                    }
                }

                if (requestToRemove != null) {
                    MistApi.getInstance().sandboxedRequestCancel(requestToRemove.getSandboxId(), requestToRemove.getActualReqId());
                }

                /* After we have handled requests, check if there are unhandled requests in the queue.
                If there are none, we can go to sleep until somebody else notify() us again.
                 */
                boolean queueHasUnhandledReqs = false;
                synchronized (sandboxRequestQueue) {
                    for (SandboxQueuedRequest req : sandboxRequestQueue) {
                        if (req.isHandled() == false) {
                            queueHasUnhandledReqs = true;
                            break;
                        }
                    }
                }
                synchronized (sandboxWorker) {
                    if (!queueHasUnhandledReqs) {
                        Log.d(TAG, "worker thread: sleep");
                        try {
                            sandboxWorker.wait();
                        } catch (InterruptedException ie) {
                            Log.d(TAG, "worker was interrupted while sleeping");
                        }
                    }
                }

            }
        }
    }, "sandboxWorker");

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        synchronized (sandboxWorker) {
            sandboxWorker.notify();
        }


        AddonReceiver mistReceiver = new AddonReceiver(this);

        Intent mistService = new Intent(this, mist.api.Service.class);
        mistService.putExtra("receiver", mistReceiver);
        startService(mistService);

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        sandboxWorker.start();
    }

    @Override
    public void onConnected() {
        Log.d(TAG, "onConnected");
        customAppWatcher = new CustomAppWatcher(this);
        loader = new Loader(this);

        /* Subscribe to Mist.signals. This is needed to get 'ready' signal in case it is the CustomApp that starts mistService */

        signalsReadyId = Signals.ready(new Signals.ReadyCb() {
            @Override
            public void cb(boolean b) {
                mistReady = true;
                synchronized (sandboxWorker) {
                    sandboxWorker.notify();
                }
            }
        });


    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected");

        /* FIXME: Do something when disconnected from core */
        if (signalsReadyId != 0) {
            Signals.cancel(signalsReadyId);
        }
    }

    private int request(byte[] id, byte[] data, Callback callback) {
        Log.d(TAG, "Making request" + callback);
        return MistApi.getInstance().sandboxedRequest(id, data, new MistApi.RequestCb() {
            private Callback _callback;

            @Override
            public void ack(final byte[] bytes) {
                final String name = "Sandbox Callback.ack()";

                /* Create a thread for running the CustomUI's ack method over the bridge */
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            _callback.ack(bytes);
                        } catch (RemoteException e) {
                            Log.d(TAG, name + " RemoteException: " + e);
                        } catch (Exception e) {
                            Log.d(TAG, name + " threw exception: " + e);
                        }
                    }
                }, name).start();

                if (sandboxRequestQueueRemoveMatching(_callback)) {
                    Log.d(TAG, "ack: Removed element from queue");
                } else {
                    Log.d(TAG, "ack: Unexpected: element not found in queue! (1) " + _callback);
                }

            }

            @Override
            public void sig(final byte[] bytes) {
                final String name = "Sandbox Callback.sig()";
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            _callback.sig(bytes);
                        } catch (Exception e) {
                            Log.d(TAG, name + " execute exception: " + e + " - Cancelling request!");
                            /* We got an exception, cancel the sig */
                            synchronized (sandboxRequestQueue) {
                                for (SandboxQueuedRequest req : sandboxRequestQueue) {
                                    if (req.getCallback() == _callback) {
                                        addToCancelQueue(req.getSandboxId(), req.getIdToSandbox());
                                    }
                                }

                            }
                        }


                    }
                }, name).start();
            }

            @Override
            public void err(final int i, final String s) {
                final String name = "Sandbox Callback.err()";

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            _callback.err(i, s);
                        } catch (RemoteException e) {
                            Log.d(TAG, name + " RemoteException: " + e);
                        } catch (Exception e) {
                            Log.d(TAG, name + " threw exception: " + e);
                        }
                    }
                }, name).start();

                if (sandboxRequestQueueRemoveMatching(_callback)) {
                    //Log.d(TAG, "err: Removed element from queue");
                } else {
                    Log.d(TAG, "err: Unexpected: element not found in queue!: " + _callback);
                }

            }

            @Override
            public void response(byte[] bytes) {
            }

            @Override
            public void end() {
                if (sandboxRequestQueueRemoveMatching(_callback)) {
                    Log.d(TAG, "end: Removed element from queue");
                } else {
                    Log.d(TAG, "end: Unexpected: element not found in queue! (2) " + _callback);
                }
            }

            private MistApi.RequestCb init(Callback callback) {
                this._callback = callback;
                return this;
            }
        }.init(callback));
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind" + isServiceRunning(getBaseContext(), mist.api.Service.class));

        return new AppToMist.Stub() {

            @Override
            public int mistApiRequest(final IBinder binder, String op, byte[] data, final Callback callback) throws RemoteException {
                Log.d(TAG, "mistApiRequest op:" + op);

                BsonDocument argsDocument = new BsonDocument();
                BsonDocument document = new RawBsonDocument(data);

                BasicOutputBuffer buffer = new BasicOutputBuffer();
                try {
                    argsDocument.append("args", document.getArray("args"));
                    argsDocument.append("op", new BsonString("sandboxed." + op));
                    argsDocument.append("id", new BsonInt32(0));

                    BsonWriter writer = new BsonBinaryWriter(buffer);
                    BsonDocumentReader bsonDocumentReader = new BsonDocumentReader(argsDocument);
                    writer.pipe(bsonDocumentReader);
                    writer.flush();
                } catch (Exception e) {
                    Log.d(TAG, "error creating bson: " + e.getMessage());
                }


                if (sandboxMap.containsKey(binder)) {
                    return addToRequestQueue(sandboxMap.get(binder), buffer.toByteArray(), callback);
                }

                if (op.equals("login")) {
                    Log.d(TAG, "login");

                    String name = "n/a";
                    final byte[] id;

                    try {
                        BsonDocument bsonDocument = new RawBsonDocument(data);
                        BsonArray bsonArray = bsonDocument.getArray("args");
                        id = bsonArray.get(0).asBinary().getData();
                        name = bsonArray.get(1).asString().getValue();

                        Log.d(TAG, "Login id: " + bytesToHex(id) + " name: " + name);
                    } catch (Exception e) {
                        Log.d(TAG, "Failed to read name or id from App login BSON message.");
                        callback.err(101, "Failed to read name or id from App login BSON message.");
                        return 0;
                    }

                    // Construct BSON login message for MistApi sandboxed.login
                    //   { op: sandboxed.login, args: [name] }
                    BasicOutputBuffer outputBuffer = new BasicOutputBuffer();
                    BsonWriter writer = new BsonBinaryWriter(outputBuffer);
                    writer.writeStartDocument();

                    writer.writeString("op", "sandboxed." + op);

                    writer.writeStartArray("args");
                    writer.writeString(name);
                    writer.writeEndArray();

                    writer.writeInt32("id", 0);

                    writer.writeEndDocument();
                    writer.flush();

                    Log.d(TAG, "id: " + bytesToHex(id));



                    return addToRequestQueue(id, outputBuffer.toByteArray(), new Callback.Stub() {
                        @Override
                        public void ack(byte[] data) throws RemoteException {
                            Log.d(TAG, "login res");

                            BsonDocument bsonDocument = new RawBsonDocument(data);
                            boolean state = bsonDocument.get("data").asBoolean().getValue();

                            if (state) {
                                customAppWatcher.login(binder, id);
                                sandboxMap.put(binder, id);
                            } else {
                                // FIXME Should close and terminate the binder
                            }

                            callback.ack(data);
                        }

                        @Override
                        public void sig(byte[] data) throws RemoteException {
                            callback.sig(data);
                        }

                        @Override
                        public void err(int code, String msg) throws RemoteException {
                            callback.err(code, msg);
                        }
                    });
                } else {
                    return 0;
                }
            }

            @Override
            public void mistApiCancel(IBinder binder, int reqId) throws RemoteException {

                if (sandboxMap.containsKey(binder)) {
                    addToCancelQueue(sandboxMap.get(binder), reqId);
                }
            }

            @Override
            public void kill(IBinder binder) {
                sandboxMap.remove(binder);
                customAppWatcher.logout(binder);
            }
        };
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy logind boxes: " + sandboxMap.size());
        super.onDestroy();
        loader.close();
        if (signalsReadyId != 0) {
            Mist.cancel(signalsReadyId);
        }
        if (signalsSandboxId != 0) {
            Signals.cancel(signalsSandboxId);
        }

        synchronized (sandboxWorker) {
            sandboxWorkerRunning = false;
            sandboxWorker.notify();
        }

    }
}
