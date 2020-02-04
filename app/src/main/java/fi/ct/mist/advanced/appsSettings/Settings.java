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
package fi.ct.mist.advanced.appsSettings;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.ct.mist.Menu;
import fi.ct.mist.mist.R;
import wish.Peer;
import mist.api.request.Sandbox;

/**
 * Created by jeppe on 12/8/16.
 */

public class Settings extends Menu implements SettingsFragmentListener {

    private final static String TAG = "Settings";

    //private static final int list = 1;
    // private static final int add = 2;

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FloatingActionButton fab;

    private AddFragment addTab;
    private RemoveFragment removeTab;

    private byte[] sandboxId;
    private String name;
//    private int type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.menu);
        Intent intent = getIntent();
        sandboxId = intent.getByteArrayExtra("sandboxId");
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        addTab = new AddFragment();
        removeTab = new RemoveFragment();

        tabLayout.setupWithViewPager(viewPager);
        setupViewPager(this, viewPager);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        setName();
    }

    private void setName() {
        Sandbox.list(new Sandbox.ListCb() {
            @Override
            public void cb(List<mist.api.Sandbox> arrayList) {
                for (mist.api.Sandbox sandbox : arrayList) {
                    if (Arrays.equals(sandbox.getId(), sandboxId)) {
                        name = sandbox.getName();
                        getSupportActionBar().setTitle(name);
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


    private void setupViewPager(Activity activity, ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(addTab, getResources().getString(R.string.advanced_settings_add), null);
        adapter.addFragment(removeTab, getResources().getString(R.string.advanced_settings_remove), null);

        viewPager.setAdapter(adapter);
        changeListener = new OnPageChangeListener(fab);
        viewPager.addOnPageChangeListener(changeListener);
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public byte[] getSandboxId() {
        return sandboxId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addPeer(Peer peer) {
        Sandbox.addPeer(sandboxId, peer, new Sandbox.AddPeerCb() {
            @Override
            public void cb() {
                addTab.refresh();
                removeTab.refresh();
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
    public void removePeer(Peer peer) {
        Sandbox.removePeer(sandboxId, peer, new Sandbox.RemovePeerCb() {
            @Override
            public void cb() {
                addTab.refresh();
                removeTab.refresh();
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
