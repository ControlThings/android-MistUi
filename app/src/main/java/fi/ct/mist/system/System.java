package fi.ct.mist.system;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import org.bson.BsonDocument;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.ct.mist.Menu;
import fi.ct.mist.connect.Connect;
import fi.ct.mist.dialogs.DirectoryPublishDialog;
import fi.ct.mist.dialogs.DirectoryPublishServiceDialog;
import fi.ct.mist.endpoint.Endpoint;
import fi.ct.mist.mist.R;
import fi.ct.mist.system.expert.Expert;

import wish.request.Connection;
import wish.Peer;
import mist.api.request.Mist;

/**
 * Created by jeppe on 12/8/16.
 */

public class System extends Menu implements SystemsFragmentListener, ManageFragment.ManageListener{

    private final static String TAG = "System";

    public final static int openEndpoint = 1;
    public final static int openObject = 2;

    Activity _activity;

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FloatingActionButton fab;

    //private ViewPagerAdapter adapter;
    private ModelFragment modelTab;
    private ManageFragment manageTab;
    private IdentitiesFragment identitysTab;
   // private MappingsFragment mappingsTab;

    private Peer peer;
    private String name;
    private String alias;
    private byte[] uid;

    private int signalsId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.menu);

        _activity = this;

        Intent intent = getIntent();
        peer = (Peer) intent.getSerializableExtra("peer");
        name = intent.getStringExtra("name");
        alias = intent.getStringExtra("alias");
        uid = intent.getByteArrayExtra("uid");

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(name);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        Bundle identitysBundel = new Bundle();
        identitysBundel.putString("alias", alias);

        //systemsTab = new SystemFragment();
        modelTab = new ModelFragment();
        manageTab = new ManageFragment();
        identitysTab = new IdentitiesFragment();
        identitysTab.setArguments(identitysBundel);
       // mappingsTab = new MappingsFragment();

        tabLayout.setupWithViewPager(viewPager);
        setupViewPager(this, viewPager);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void setupViewPager(Activity activity, ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
       // adapter.addFragment(systemsTab, getResources().getString(R.string.system_system), null);
        adapter.addFragment(modelTab, getResources().getString(R.string.system_model), null);
        adapter.addFragment(manageTab, getResources().getString(R.string.system_manage), null);
        adapter.addFragment(identitysTab, getResources().getString(R.string.system_identities), null);
       // adapter.addFragment(mappingsTab, getResources().getString(R.string.main_mappings), null);

        viewPager.setAdapter(adapter);
        changeListener = new OnPageChangeListener(fab);
        viewPager.addOnPageChangeListener(changeListener);
    }

    private void listServices() {



       Connection.list(new Connection.ListCb() {
            @Override
            public void cb(List<wish.Connection> arrayList) {
                for (wish.Connection conn: arrayList ) {
                    if (Arrays.equals(conn.getLuid(), peer.getLuid()) && Arrays.equals(conn.getRuid(), peer.getRuid()) && Arrays.equals(conn.getRhid(), peer.getRhid())) {
                        identitysTab.setConnection(conn);
                        identitysTab.refreshIdentities(getBaseContext());
                        //break;
                    }

                }
                modelTab.refreshModel(peer);
                manageTab.refreshModel(peer);
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
        //intent.putExtra("id", peer.getId());
        //intent.putExtra("name", name + "." + ep);
        try {
            intent.putExtra("peer", peer);
            intent.putExtra("endpoint", endpoint);
           // intent.putExtra("json", data.toString());
            startActivity(intent);
        } catch (Exception e) {
            Log.d(TAG, "load tree error" + e.getMessage());
        }
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
        return name;
    }

    @Override
    public void onInviteExpert(String model) {
        if (model == null) {
            Snackbar snackbar = Snackbar
                    .make(_activity.findViewById(R.id.coordinatorLayout), getResources().getString(R.string.system_manage_no_model), Snackbar.LENGTH_LONG);
            snackbar.show();
        } else {
            Intent intent = new Intent(_activity, Expert.class);
            intent.putExtra("uid", uid);
            intent.putExtra("name", name);
            intent.putExtra("peer", peer);
            intent.putExtra("model", model);
            _activity.startActivity(intent);
        }
    }

    @Override
    public void onPublishService() {
        DirectoryPublishServiceDialog.showDialog(_activity, peer, name, new DirectoryPublishServiceDialog.onDirectoryPublishDialogListener() {
            @Override
            public void onOk() {
                Snackbar snackbar = Snackbar
                        .make(_activity.findViewById(R.id.coordinatorLayout), name + " " + getResources().getString(R.string.directory_publish_dialog_onOk), Snackbar.LENGTH_LONG);
                snackbar.show();
            }

            @Override
            public void onErr() {
                Snackbar snackbar = Snackbar
                        .make(_activity.findViewById(R.id.coordinatorLayout), getResources().getString(R.string.directory_publish_dialog_onErr), Snackbar.LENGTH_LONG);
                snackbar.show();
            }

            @Override
            public void onCancel(DialogInterface dialog) {
                Snackbar snackbar = Snackbar
                        .make(_activity.findViewById(R.id.coordinatorLayout), getResources().getString(R.string.directory_publish_dialog_onErr), Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        });
    }
}
