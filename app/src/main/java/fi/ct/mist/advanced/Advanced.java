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
package fi.ct.mist.advanced;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.View;

import fi.ct.mist.Menu;
import fi.ct.mist.advanced.appsSettings.Settings;
import fi.ct.mist.dialogs.DownloadAppDialog;
import fi.ct.mist.main.AppsFragment;
import fi.ct.mist.mist.R;
import fi.ct.mist.sandbox.CustomWebView;

/**
 * Created by jeppe on 12/8/16.
 */

public class Advanced extends Menu implements AdvancedFragmentListener, CacheFragment.UiFragmentListener, Html5Fragment.Html5FragmentListener{

    private final static String TAG = "Settings";

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FloatingActionButton fab;

    private ConnectionsFragment connectionsTab;
    private NativeFragment nativeTab;
    private CacheFragment cacheTab;
    private Html5Fragment html5Tab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.menu);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.advanced_title));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        connectionsTab = new ConnectionsFragment();
        nativeTab = new NativeFragment();
        cacheTab = new CacheFragment();
        html5Tab = new Html5Fragment();

        tabLayout.setupWithViewPager(viewPager);
        setupViewPager(this, viewPager);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }


    private void setupTabIcons() {
/*
        TextView tabOne = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabOne.setText("ONE");
        tabOne.setCompoundDrawablesWithIntrinsicBounds(R.drawable.wifi, 0, 0, 0);
        tabLayout.getTabAt(0).setCustomView(tabOne);
*/
    }

    private void setupViewPager(final Activity activity, ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(connectionsTab, getResources().getString(R.string.advanced_connections), null);
        adapter.addFragment(nativeTab, getResources().getString(R.string.advanced_native), null);
        adapter.addFragment(html5Tab, getResources().getString(R.string.advanced_html5), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadAppDialog.showDialog(activity, new DownloadAppDialog.onDownloadAppDialogListener() {
                    @Override
                    public void onOk(String url) {
                        Intent htmlUi = new Intent(activity, CustomWebView.class);
                        htmlUi.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                        htmlUi.putExtra("url", url);
                        activity.startActivity(htmlUi);
                    }

                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                });
            }
        });
        adapter.addFragment(cacheTab, getResources().getString(R.string.advanced_cache), null);

        viewPager.setAdapter(adapter);
        changeListener = new OnPageChangeListener(fab);
        viewPager.addOnPageChangeListener(changeListener);
    }

    @Override
    public void openAppSettings(byte[] id) {
        Intent intent = new Intent(this, Settings.class);
        intent.putExtra("sandboxId", id);
        startActivity(intent);
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
    public void onUiDeleted() {
        cacheTab.refreshApps();
    }


    @Override
    public void onAppRemoved() {
        html5Tab.refreshApps();
    }
}
