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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.WindowManager;

import org.bson.BSONException;
import org.bson.BsonArray;
import org.bson.BsonBinary;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonString;
import org.bson.BsonWriter;
import org.bson.RawBsonDocument;
import org.bson.io.BasicOutputBuffer;

import java.util.ArrayList;
import java.util.Arrays;

import fi.ct.mist.advanced.appsSettings.Settings;
import fi.ct.mist.connect.CommissionItem;
import fi.ct.mist.connect.Connect;
import fi.ct.mist.connect.commissioning.CommissioningStateMachine;
import fi.ct.mist.connect.commissioning.GuidedCommissioning;
import fi.ct.mist.connect.commissioning.SandboxCommissioning;
import fi.ct.mist.dialogs.SandboxAllowDialog;
import fi.ct.mist.mist.R;
import mist.api.MistApi;
import mist.api.request.*;
import mist.api.request.Sandbox;
import wish.WishApp;

/**
 * Created by jeppe on 1/10/17.
 */

class Loader {

    private static final String TAG = "Loader";

    private Context _context;
    private int signalsId;

    private final String COMMISSION = "commission";
    private final String ADDPEER = "addPeer";

    Loader(Context context) {
        this._context = context;
        signal();
    }

    private void signal() {
        if (signalsId != 0 ) {
            Mist.cancel(signalsId);
        }
        signalsId = Signals.sandbox(new Signals.SandboxCb() {


            @Override
            public void cb(byte[] id, String hint) {
                super.cb(id, hint);
                Log.d("Test", " sandbox signal " + hint);
                if (hint.equals(COMMISSION)) {
                    Intent intent = new Intent(_context, Connect.class);
                    intent.putExtra("sandboxId", id);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    _context.startActivity(intent);
                } else if (hint.equals(ADDPEER)) {
                    Intent intent = new Intent(_context, Settings.class);
                    intent.putExtra("sandboxId", id);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    _context.startActivity(intent);
                } else {
                    return;
                }
            }

            @Override
            public void cb(byte[] id, String hint, BsonDocument opts) {
                super.cb(id, hint);
                if (hint.equals(COMMISSION)) {
                    try {
                        if (opts.getString("type").getValue().equals("wifi")) {
                            CommissionItem.WifiConnection wifiConnection = new CommissionItem().new WifiConnection();
                            wifiConnection.setSsid(opts.getString("ssid").getValue());
                            wifiConnection.setLevel(WifiManager.calculateSignalLevel(-10, 5));
                            wifiConnection.setSecurity("OPEN");

                            new SandboxCommissioning(id, wifiConnection, _context);
                        }
                        if (opts.getString("type").getValue().equals("local")) {
                            CommissionItem.WldConnection wldConnection = new CommissionItem().new WldConnection();
                            wldConnection.setRuid(opts.getBinary("ruid").getData());
                            wldConnection.setRhid(opts.getBinary("rhid").getData());
                            wldConnection.setName(opts.getString("alias").getValue());

                            new SandboxCommissioning(id, wldConnection, _context);
                        }
                    } catch (BSONException be) {
                        Log.d(TAG, "BSON Exception when parsing commission request from sandbox");
                        BsonDocument bsonDocument = new BsonDocument().append("hint", new BsonString(be.getMessage()));
                        Sandbox.emit(id, "commission.err", bsonDocument, new Sandbox.EmitCb() {
                            @Override
                            public void cb(boolean b) {

                            }
                        });
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

    void close() {
        if (signalsId != 0) {
            Mist.cancel(signalsId);
        }
    }
}
