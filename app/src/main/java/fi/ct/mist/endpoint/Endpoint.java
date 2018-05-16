package fi.ct.mist.endpoint;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import org.bson.BsonDocument;

import java.util.ArrayList;
import java.util.List;

import fi.ct.mist.Menu;
import fi.ct.mist.mist.R;
import fi.ct.mist.dialogs.MappingsDialog;
import wish.Peer;
import mist.node.request.Control;
import mist.api.request.Mist;

/**
 * Created by jeppe on 12/8/16.
 */

public class Endpoint extends Menu implements EndpointFragmentListener {

    private final static String TAG = "System";

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FloatingActionButton fab;

    //private ViewPagerAdapter adapter;
    private VisualizeFragment endpointTab;
    private MappingFragment mappingTab;

  //  private int peerId;
  //  private String endpointString;
   // private String label;
    private Peer peer;
    private fi.ct.mist.main.Endpoint endpoint;

    private int signalsId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.menu);

        Intent intent = getIntent();

        peer = (Peer) intent.getSerializableExtra("peer");
        endpoint = (fi.ct.mist.main.Endpoint) intent.getSerializableExtra("endpoint");

       // peerId = intent.getIntExtra("id", 0);
       // endpointString = intent.getStringExtra("ep");
       // label = intent.getStringExtra("label");

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(endpoint.getName());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        endpointTab = new VisualizeFragment();
        mappingTab = new MappingFragment();

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
        adapter.addFragment(endpointTab, getResources().getString(R.string.endpoint_visualize), null);
        adapter.addFragment(mappingTab, getResources().getString(R.string.endpoint_mapping), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MappingsDialog dialog = MappingsDialog.newInstance(endpoint);
                dialog.show(getSupportFragmentManager(), "mapping");
             /*   IdentityDialog.showDialog(activity, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        //identitiesTab.refreshIdentities();
                    }
                });*/
            }
        });

        viewPager.setAdapter(adapter);
        changeListener = new OnPageChangeListener(fab);
        viewPager.addOnPageChangeListener(changeListener);
    }

    private void listServices() {
        Mist.listServices(new Mist.ListServicesCb() {
            @Override
            public void cb(List<Peer> arrayList) {
                for (Peer p : arrayList) {
                    if (p.equals(peer)) {
                        peer = p;
                        mappingTab.setMappings(p, endpoint.getName());
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
    public void setMapping(final Peer peerFrom, final String endpointFrom) {
        Control.requestMapping(peer, peerFrom, endpointFrom, endpoint.getName(), new Control.RequestMappingCB() {
            @Override
            public void cb() {
                Log.d(TAG, "requestmapping cb");
                mappingTab.refreshModel();
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
