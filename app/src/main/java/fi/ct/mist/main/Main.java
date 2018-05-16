package fi.ct.mist.main;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import org.bson.BSONException;
import org.bson.BsonDocument;
import org.bson.RawBsonDocument;

import java.util.Arrays;
import java.util.List;

import addon.AddonReceiver;
import fi.ct.mist.Menu;
import fi.ct.mist.NotificationService;
import fi.ct.mist.Permissions;
import fi.ct.mist.Versions;
import fi.ct.mist.advanced.*;
import fi.ct.mist.dialogs.AboutDialog;
import fi.ct.mist.dialogs.DownloadAppDialog;
import fi.ct.mist.dialogs.InviteFriendRequestDialog;
import fi.ct.mist.dialogs.LicensesDialog;
import fi.ct.mist.mist.R;
import fi.ct.mist.dialogs.IdentityDialog;
import fi.ct.mist.sandbox.CustomWebView;
import fi.ct.mist.settings.Settings;
import fi.ct.mist.system.System;
import fi.ct.mist.connect.Connect;
import mist.api.Service;
import wish.LocalDiscovery;

import mist.api.request.Mist;
import wish.request.Identity;
import wish.request.Wld;


public class Main extends Menu implements MainFragmentListener, UsersFragment.UserFragmentListener, AddonReceiver.Receiver {

    private final String TAG = "Main";

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FloatingActionButton fab;

    private SystemsFragment systemsTab;
    private RolesFragment rolesTab;
    private UsersFragment usersTab;
    private TagsFragment tagsTab;

    private wish.Identity selectedIdentity = null;

    Intent mistService;
    AlertDialog coreNotRunningAlertDialog;

    private boolean isReady = false;
    private boolean isFirst = true;

    private int signalsId = 0;

    private int locationPermisionId;

    private String tagSelected = null;

    Activity _activity;

    private boolean bridgeConnected;

    @Override
    public boolean isReady() {
        return isReady;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate" + mist.api.ui.BuildConfig.GitClean);
        setContentView(R.layout.menu);

        Intent intent = new Intent(this, NotificationService.class);
        startService(intent);

        _activity = this;

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getResources().getString(R.string.no_user));
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        systemsTab = new SystemsFragment();
        rolesTab = new RolesFragment();
        usersTab = new UsersFragment();
        tagsTab = new TagsFragment();

        tabLayout.setupWithViewPager(viewPager);
        setupViewPager(this, viewPager);

        AddonReceiver mistReceiver = new AddonReceiver(this);

        Intent mistService = new Intent(this, Service.class);
        mistService.putExtra("receiver", mistReceiver);
        this.mistService = mistService;
        startService(mistService);

    }

    private void onMistReady() {
        isReady = true;
        getIdentity();
        Versions.getInstance().setVersions();
    }

    private void getIdentity() {
        Log.d(TAG, "getIdentity");
        Identity.list(new Identity.ListCb() {
            @Override
            public void cb(List<wish.Identity> arrayList) {
                Log.d(TAG, "getIdentity list size " + arrayList.size());
                if (arrayList.size() == 0) {
                    IdentityDialog.showDialog(_activity, new IdentityDialog.onIdentityDialogListener() {
                        @Override
                        public void onOk() {
                            getIdentity();
                        }

                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                        }
                    });

                } else {
                    try {
                        for (wish.Identity identity : arrayList) {
                            if (identity.isPrivkey()) {
                                Log.d(TAG, "getIdentity sets selected identity");
                                toolbar.setTitle(identity.getAlias());
                                selectedIdentity = identity;
                            }
                        }
                        checkIfExtra();
                        refresh();
                    } catch (Exception e) {
                        Log.d(TAG, "Error: " + Log.getStackTraceString(e));
                    }
                }
            }

            @Override
            public void err(int i, String s) {

                Log.e(TAG, "getIdentity: " + s + " code: " + i);

            }

            @Override
            public void end() {
            }

        });
    }

    private void checkIfExtra() {
        Intent intent = getIntent();

        String type = intent.getStringExtra("type");

        Log.d(TAG, "check type: " + type);

        if (type != null) {
            switch (type) {
                case "url":
                    UrlHandler.getInstance().parse(this, intent.getExtras(), new UrlHandler.Cb() {
                        @Override
                        public void ready() {
                            refresh();
                        }
                    });
                case "tag":
                    if (intent.getStringExtra("tag") != null) {
                        tagSelected = intent.getStringExtra("tag");
                        intent.removeExtra("tag");
                        if (tagSelected != null) {
                            int position = 0;
                            if (tagSelected.equals(getResources().getString(R.string.main_devices))) {
                                position = 0;
                            }
                            if (tagSelected.equals(getResources().getString(R.string.main_users))) {
                                position = 1;
                            }
                          /*  if (tagSelected.equals(getResources().getString(R.string.main_apps))) {
                                position = 2;
                            } */
                            viewPager.setCurrentItem(position);
                        }
                    }
                default:
                    intent.removeExtra("type");

            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }


    boolean mistServiceStarting = false;

    @Override
    public void onConnected() {
        Log.d(TAG, "onConnected");
        mistServiceStarting = false;
        if (coreNotRunningAlertDialog != null) {
            if (coreNotRunningAlertDialog.isShowing()) {
                coreNotRunningAlertDialog.dismiss();
            }
        }
        bridgeConnected = true;

        registerSignals();
    }

    private void registerSignals() {
        if (signalsId != 0) {
            Mist.cancel(signalsId);
        }
        signalsId = Mist.signals(new Mist.SignalsCb() {
            @Override
            public void cb(String signal, BsonDocument bsonDocument) {
                try {
                    if (signal.equals("ok")) {
                    }
                    if (signal.equals("ready")) {
                        if (isFirst) {
                            Mist.ready(new Mist.ReadyCb() {
                                @Override
                                public void cb(boolean b) {
                                    if (b) {
                                        isFirst = false;
                                        onMistReady();
                                    }
                                }
                            });
                        }
                    }
                    if (signal.equals("peers")) {
                        systemsTab.updateSystems();

                    }
                    if (signal.equals("identity")) {
                        usersTab.refreshIdentities(getBaseContext());
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Error: " + Log.getStackTraceString(e));
                }
            }

            @Override
            public void err(int code, String msg) {
                Log.d(TAG, "Mist signal error: code " + code + " msg " + msg);
            }

            @Override
            public void end() {

            }
        });
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected");


        /* FIXME: Do something when disconnected from core! */
        stopService(mistService);
        bridgeConnected = false;

        /* Pop up an alert dialog telling about that Wish core is no longer running */
        final Main main = this;

        if (coreNotRunningAlertDialog != null && coreNotRunningAlertDialog.isShowing()) {
            /* Dialog already shown */
        } else {
            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    coreNotRunningAlertDialog = new AlertDialog.Builder(getBaseContext())
                            .setTitle(R.string.wish_core_down)
                            .setMessage(R.string.wish_core_down_info)
                            .setPositiveButton(R.string.wish_core_down_restart, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (!mistServiceStarting) {
                                        startService(mistService);
                                    }
                                    mistServiceStarting = true;
                                    dialog.dismiss();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .create();
                    coreNotRunningAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    coreNotRunningAlertDialog.show();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume ");

        refresh();

        if (bridgeConnected) {
            onConnected();
        }
        checkIfExtra();
    }

    private void refresh() {
        if (!isFirst) {
            systemsTab.refreshSystems();
            usersTab.refreshIdentities(getBaseContext());
        }
    }

    private void setupTabIcons() {
      /*  TextView tabOne = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabOne.setText(getResources().getString(R.string.main_devices));

        final ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.MistTheme);
        final Drawable drawable = VectorDrawableCompat.create(getResources(), R.drawable.ic_gamepad_black_24dp, wrapper.getTheme());


        tabOne.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gamepad_black_24dp, 0, 0, 0);
        tabLayout.getTabAt(0).setCustomView(tabOne);*/
    }

    private void openSystemTab() {
        if (selectedIdentity == null) {
            Snackbar snackbar = Snackbar
                    .make(_activity.findViewById(R.id.coordinatorLayout), getResources().getString(R.string.no_user_info), Snackbar.LENGTH_LONG);
            snackbar.show();
        } else {
            Intent intent = new Intent(_activity, Connect.class);
            intent.putExtra("alias", selectedIdentity.getAlias());
            intent.putExtra("uid", selectedIdentity.getUid());
            _activity.startActivity(intent);
        }
    }

    private void setupViewPager(final Activity activity, ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(systemsTab, getResources().getString(R.string.main_devices), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Permissions.hasPermission(_activity, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    openSystemTab();
                } else {
                    locationPermisionId = Permissions.requestPermission(_activity, Manifest.permission.ACCESS_COARSE_LOCATION);
                }
            }
        });


        //adapter.addFragment(rolesTab, getResources().getString(R.string.main_roles), null);
        adapter.addFragment(usersTab, getResources().getString(R.string.main_users), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IdentityDialog.showDialog(activity, new IdentityDialog.onIdentityDialogListener() {
                    @Override
                    public void onOk() {
                        usersTab.refreshIdentities(getBaseContext());
                    }

                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                    }
                });
            }
        });

        //adapter.addFragment(tagsTab, getResources().getString(R.string.main_tags), null);
        viewPager.setAdapter(adapter);


        changeListener = new OnPageChangeListener(fab);
        viewPager.addOnPageChangeListener(changeListener);

        //todo hack to get fab to work at start
        viewPager.setCurrentItem(1);
        viewPager.setCurrentItem(0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        /* Unsubscribe Mist.signals() here, because it is subscribed in onResume(), which is the matching lifecycle callback to onResume() when Activity is moved to background */
       if (signalsId != 0) {
           Mist.cancel(signalsId);
       }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

        isReady = false;
        if (tagSelected != null) {
            tagSelected = null;
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void setSystem(Device device) {
        if (selectedIdentity == null) {
            Snackbar snackbar = Snackbar
                    .make(_activity.findViewById(R.id.coordinatorLayout), getResources().getString(R.string.no_user_info), Snackbar.LENGTH_LONG);
            snackbar.show();
        } else {
            Intent intent = new Intent(this, System.class);
            intent.putExtra("peer", device.getPeer());
            intent.putExtra("name", device.getName());
            intent.putExtra("alias", device.getAlias());
            intent.putExtra("uid", selectedIdentity.getUid());
            startActivity(intent);
        }
    }

    @Override
    public void setIdentity(byte[] uid) {
        if (uid == null) {
            toolbar.setTitle(getResources().getString(R.string.no_user));
            selectedIdentity = null;
        } else {
            Identity.get(uid, new Identity.GetCb() {
                @Override
                public void cb(wish.Identity identity) {
                    selectedIdentity = identity;
                    toolbar.setTitle(identity.getAlias());
                }
            });

        }
    }

    @Override
    public wish.Identity getSelectedIdentity() {
        return selectedIdentity;
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings_advanced:
                Intent advancedIntent = new Intent(this, Advanced.class);
                startActivity(advancedIntent);
                return true;
            case R.id.settings_about:
                AboutDialog.showDialog(this);
                return true;
            case R.id.settings_settings:
                Intent settingsIntent = new Intent(this, Settings.class);
                startActivity(settingsIntent);
                return true;
            case R.id.settings_license:
                LicensesDialog.showDialog(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (locationPermisionId == requestCode) {
            if (grantResults[0] == 0) {
                openSystemTab();
            } else {
                Snackbar snackbar = Snackbar
                        .make(_activity.findViewById(R.id.coordinatorLayout), getResources().getString(R.string.main_snackbar_permission), Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        }

    }

    @Override
    public void onInviteFriendRequest(String alias, final byte[] luid, final byte[] ruid, BsonDocument meta) {

        if (selectedIdentity != null) {

            final byte[] invRuid;
            final byte[] invRhid;
            final String invAlias;

            try {
                //bsonConsolePrettyPrinter("frindrquest: ", meta.getBinary("data").getData());
                BsonDocument data = new RawBsonDocument(meta.getBinary("data").getData());

                invRuid = data.getBinary("uid").getData();
                invRhid = data.getBinary("hid").getData();
                invAlias = data.getString("alias").getValue();
            } catch (BSONException e) {
                Log.d(TAG, "BSON parse error: " + e.getMessage());
                return;
            }

            InviteFriendRequestDialog.showDialog(_activity, alias, selectedIdentity.getAlias(), invAlias, new InviteFriendRequestDialog.Cb() {
                @Override
                public void accept(boolean frendrequest, final boolean invite) {
                    if (frendrequest) {
                        Identity.friendRequestAccept(luid, ruid, new Identity.FriendRequestAcceptCb() {
                            @Override
                            public void cb(boolean b) {
                                if (b && invite) {
                                    Wld.list(new Wld.ListCb() {
                                        @Override
                                        public void cb(final List<LocalDiscovery> list) {
                                            Log.d(TAG, "wld list cb");
                                            for (final LocalDiscovery localDiscovery : list) {
                                                if (localDiscovery.getType().equals("friendReq")) {
                                                    if (Arrays.equals(invRuid, localDiscovery.getRuid()) && Arrays.equals(invRhid, localDiscovery.getRhid())) {
                                                        Wld.friendRequest(selectedIdentity.getUid(), localDiscovery, new Wld.FriendRequestCb() {

                                                            @Override
                                                            public void cb(boolean value) {
                                                                Wld.clear(new Wld.ClearCb() {
                                                                    @Override
                                                                    public void cb(boolean value) {
                                                                    }

                                                                    @Override
                                                                    public void err(int i, String s) {
                                                                    }

                                                                    @Override
                                                                    public void end() {
                                                                    }
                                                                });
                                                                usersTab.refreshIdentities(getBaseContext());
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
                                            }
                                        }

                                        @Override
                                        public void err(int i, String s) {
                                        }

                                        @Override
                                        public void end() {
                                        }
                                    });

                                } else {
                                    usersTab.refreshIdentities(getBaseContext());
                                }
                            }

                            @Override
                            public void err(int i, String s) {
                                Log.d(TAG, "error" + s);
                            }

                            @Override
                            public void end() {
                            }
                        });
                    }
                }

                @Override
                public void decline() {
                    Identity.friendRequestDecline(luid, ruid, new Identity.FriendRequestDeclineCb() {
                        @Override
                        public void cb(boolean b) {
                            usersTab.refreshIdentities(getBaseContext());
                        }

                        @Override
                        public void err(int i, String s) {
                        }

                        @Override
                        public void end() {
                        }
                    });
                }
            });
        }
    }
}
