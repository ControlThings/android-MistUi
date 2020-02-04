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
package fi.ct.mist.system;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import org.bson.BsonDocument;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fi.ct.mist.mist.R;
import fi.ct.mist.endpoint.Endpoint;
import wish.Peer;
import mist.api.request.Mist;


public class Tree extends AppCompatActivity implements SystemsFragmentListener {

    private final String TAG = "Tree";
    private Toolbar toolbar;

    private ModelFragment modelTab;

    //private int peerId;
    private Peer peer;
   private fi.ct.mist.main.Endpoint endpoint;
    // private String name;
    //private JSONObject jsonEndpoint;

    private int signalsId;
    private boolean first = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment);

        Intent intent = getIntent();
        endpoint = (fi.ct.mist.main.Endpoint) intent.getSerializableExtra("endpoint");
        peer = (Peer) intent.getSerializableExtra("peer");

      //  peerId = intent.getIntExtra("id", 0);
      //  name = intent.getStringExtra("name");

        Log.d(TAG, "onCreate " + endpoint.getObject().toString());

        toolbar = (Toolbar) findViewById(R.id.fragment_toolbar);
        toolbar.setTitle(endpoint.getName());
        setSupportActionBar(toolbar);

        modelTab = new ModelFragment();
    }

    private void listServices() {
        Mist.listServices(new Mist.ListServicesCb() {
            @Override
            public void cb(List<Peer> arrayList) {
                for (Peer p : arrayList) {
                    if (p.equals(peer)) {
                        peer = p;
                        try {

                            for (Iterator<String> iter = endpoint.getObject().keys(); iter.hasNext(); ) {
                                String key = iter.next();
                                JSONObject jsonEndpoint = endpoint.getObject().getJSONObject(key);
                                modelTab.refreshModelFromTree(p, endpoint.getName(), jsonEndpoint);

                            }

                        } catch (JSONException e) {
                            Log.d(TAG, "Error parsing json model: " + e);
                        }



                        if (first) {
                            first = false;
                            FragmentManager manager = getSupportFragmentManager();
                            FragmentTransaction transaction = manager.beginTransaction();
                            transaction.add(R.id.fragment_container, modelTab);
                            transaction.commit();
                        }
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
    protected void onStart() {
        super.onStart();
        signalsId = Mist.signals(new Mist.SignalsCb() {
            @Override
            public void cb(String signal, BsonDocument bsonDocument) {
                if (signal.equals("peers")) {
                    Log.d(TAG, "peers");
                    listServices();
                    // systemsTab.updateSystems();
                }
            }

            @Override
            public void err(int i, String s) {

            }

            @Override
            public void end() {

            }
        });
        listServices();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        Mist.cancel(signalsId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void openObject(fi.ct.mist.main.Endpoint endpoint) {
        Intent intent = new Intent(this, Tree.class);
        intent.putExtra("peer", peer);
        intent.putExtra("endpoint", endpoint);
        //intent.putExtra("id", peer.getId());
        //intent.putExtra("name", name + "." + ep);
      //  intent.putExtra("json", data.toString());
        startActivity(intent);
    }

    @Override
    public void openEndpoint(fi.ct.mist.main.Endpoint endpoint) {
        Intent intent = new Intent(this, Endpoint.class);
        intent.putExtra("peer", peer);
        intent.putExtra("endpoint", endpoint);
        startActivity(intent);
    }

    @Override
    public String getAlias() {
        return null;
    }
}
