package fi.ct.mist.system.expert;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import fi.ct.mist.Menu;
import fi.ct.mist.dialogs.MappingsDialog;
import fi.ct.mist.endpoint.EndpointFragmentListener;
import fi.ct.mist.endpoint.MappingFragment;
import fi.ct.mist.endpoint.VisualizeFragment;
import fi.ct.mist.mist.R;
import wish.Cert;
import wish.Peer;
import mist.node.request.Control;
import wish.request.Identity;
import mist.api.request.Mist;

/**
 * Created by jeppe on 12/8/16.
 */

public class Expert extends Menu implements DirectoryFragment.DirectoryListener{

    private final static String TAG = "Expert";

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FloatingActionButton fab;

    private DirectoryFragment directoryTab;


    private Peer peer;
    private String name;
    private byte[] uid;
    private String searchString;

    private fi.ct.mist.main.Endpoint endpoint;

    private int signalsId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.menu);

        Intent intent = getIntent();

        name = intent.getStringExtra("name");
        uid = intent.getByteArrayExtra("uid");
        peer = (Peer) intent.getSerializableExtra("peer");
        searchString = intent.getStringExtra("model");

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(name);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        directoryTab = new DirectoryFragment();
        directoryTab.refreshDirectory(searchString);

        tabLayout.setupWithViewPager(viewPager);
        setupViewPager(this, viewPager);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void setupViewPager(final Activity activity, ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(directoryTab, getResources().getString(R.string.expert_experts), null);
        viewPager.setAdapter(adapter);
        changeListener = new OnPageChangeListener(fab);
        viewPager.addOnPageChangeListener(changeListener);
    }


    @Override
    protected void onStart() {
        super.onStart();
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
    public void onFriendRequest(Cert data) {
        Log.d("TEST", "onFriendRequest" + uid + data.getCert() + peer);

        Identity.friendRequest(uid, data.getCert(), peer, new Identity.FriendRequestCb() {
            @Override
            public void cb(boolean b) {
                Log.d(TAG, "onFriendRequest cb " + b);
                onBackPressed();
            }

            @Override
            public void err(int i, String s) {
                Log.d(TAG, "err: " + s + " code: " + i);
            }

            @Override
            public void end() {}
        });
    }
}
