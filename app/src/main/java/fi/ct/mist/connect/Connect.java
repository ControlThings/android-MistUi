package fi.ct.mist.connect;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.ct.mist.Menu;
import fi.ct.mist.connect.commissioning.GuidedCommissioning;
import fi.ct.mist.dialogs.DirectoryPublishDialog;
import fi.ct.mist.mist.R;
import wish.Cert;
import wish.LocalDiscovery;
import wish.Peer;
import wish.request.Identity;
import mist.api.request.Mist;
import mist.api.request.Sandbox;
import wish.request.Wish;
import wish.request.Wld;

/**
 * Created by jeppe on 12/8/16.
 */

public class Connect extends Menu implements LocalDiscoveryFragment.LocalDiscoveryListener, CommissioningListFragment.CommissioningListener, DirectoryFragment.DirectoryListener, InviteFragment.InviteListener{

    private final static String TAG = "GuidedCommissioning";

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FloatingActionButton fab;
    private FloatingActionButton fabExtra;
    private String alias;
    private byte[] uid;

    private LocalDiscoveryFragment localDiscoveryTag;
    private CommissioningListFragment commissioningTag;
    private DirectoryFragment directoryTag;
    private InviteFragment inviteTag;

    private byte[] sandboxId;

    private final int COMMISSIONIG_RESULT_ID = 64352;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Connect.onCreate");
        setContentView(R.layout.menu);

        Intent intent = getIntent();

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (intent.hasExtra("sandboxId")) {
            sandboxId = intent.getByteArrayExtra("sandboxId");
            setName();
            //toolbar.setTitle(getResources().getString(R.string.connect_sandbox_title));
        } else {
            alias = intent.getStringExtra("alias");
            uid = intent.getByteArrayExtra("uid");
            toolbar.setTitle(alias);
            localDiscoveryTag = new LocalDiscoveryFragment();
            directoryTag = new DirectoryFragment();
            inviteTag = new InviteFragment();
        }

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);

        fabExtra = (FloatingActionButton) findViewById(R.id.fab_extra);
        fabExtra.setImageResource(R.drawable.ic_undo_black_24dp);
        fabExtra.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Wld.clear(new Wld.ClearCb() {
                    @Override
                    public void cb(boolean value) {
                        if (localDiscoveryTag != null) {
                            localDiscoveryTag.refreshConnections();
                        }
                        else {
                             /* TODO this must be reviewed, see onCreate, localDiscoveryTag is not created if sandbox is involved */
                        }
                    }
                    @Override
                    public void err(int i, String s) {}
                    @Override
                    public void end() {}
                });
            }
        });

        commissioningTag = new CommissioningListFragment();

        tabLayout.setupWithViewPager(viewPager);
        setupViewPager(this, viewPager);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void setName() {
        Sandbox.list(new Sandbox.ListCb() {
            @Override
            public void cb(List<mist.api.Sandbox> arrayList) {
                for (mist.api.Sandbox sandbox : arrayList) {
                    if (Arrays.equals(sandbox.getId(), sandboxId)) {
                        toolbar.setTitle(sandbox.getName());
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

    private void setupViewPager(final Activity activity, ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        if (sandboxId == null) {
            adapter.addFragment(localDiscoveryTag, getResources().getString(R.string.connect_localdiscovery), null);
            adapter.addFragment(inviteTag, getResources().getString(R.string.connect_invite), null);

            adapter.addFragment(directoryTag, getResources().getString(R.string.connect_directory), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DirectoryPublishDialog.showDialog(activity, uid, alias, new DirectoryPublishDialog.onDirectoryPublishDialogListener() {
                        @Override
                        public void onOk() {
                            Snackbar snackbar = Snackbar
                                    .make(activity.findViewById(R.id.coordinatorLayout), alias + " " + getResources().getString(R.string.directory_publish_dialog_onOk), Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }

                        @Override
                        public void onErr() {
                            Snackbar snackbar = Snackbar
                                    .make(activity.findViewById(R.id.coordinatorLayout), getResources().getString(R.string.directory_publish_dialog_onErr), Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }

                        @Override
                        public void onCancel(DialogInterface dialog) {
                            Snackbar snackbar = Snackbar
                                    .make(activity.findViewById(R.id.coordinatorLayout), getResources().getString(R.string.directory_publish_dialog_onErr), Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    });
                }
            });
        }
        adapter.addFragment(commissioningTag, getResources().getString(R.string.connect_commissioning), null);
        viewPager.setAdapter(adapter);
        changeListener = new OnPageChangeListener(fab);
        viewPager.addOnPageChangeListener(changeListener);

    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Connect.onResume connect");
        super.onResume();
        if (wishSignalsId == 0) {
            setListener();
        }
    }

    private int wishSignalsId;

    private void setListener() {
        wishSignalsId = Wish.signals(null, new Wish.SignalsCb() {
            @Override
            public void cb(String s) {
                if (s.equals("localDiscovery")) {
                    if (localDiscoveryTag != null) {
                        localDiscoveryTag.refreshConnections();
                    }
                    else {
                        /* TODO this must be reviewed, see onCreate, localDiscoveryTag is not created if sandbox is involved */
                    }
                }
            }

            @Override
            public void err(int i, String s) {}

            @Override
            public void end() {}
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "Connect.onStop");
        fabExtra.setVisibility(View.GONE);
        if (wishSignalsId != 0) {
            Mist.cancel(wishSignalsId);
            wishSignalsId = 0;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Connect.onDestroy");
    }

    @Override
    public void onCommissionListItemClicked(CommissionItem item) {
        Intent intent = new Intent(this, GuidedCommissioning.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("item", item);
        intent.putExtras(bundle);
        startActivityForResult(intent, COMMISSIONIG_RESULT_ID);
    }

    @Override
    public void onFriendRequest(LocalDiscovery localDiscovery) {
        Wld.friendRequest(uid, localDiscovery, new Wld.FriendRequestCb() {
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
    }

    @Override
    public void onInviteFriendRequest(LocalDiscovery localDiscovery) {
        Wld.friendRequest(uid, localDiscovery, new Wld.FriendRequestCb() {
            @Override
            public void cb(boolean value) {
                Wld.clear(new Wld.ClearCb() {
                    @Override
                    public void cb(boolean value) {
                        inviteTag.refreshConnections();
                    }

                    @Override
                    public void err(int i, String s) {}

                    @Override
                    public void end() {}
                });
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
    public void onFriendRequest(Cert cert) {
        Identity.friendRequest(uid, cert.getCert(), new Identity.FriendRequestCb() {
            @Override
            public void cb(boolean b) {}

            @Override
            public void err(int i, String s) {}

            @Override
            public void end() {}
        });
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      //  super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == COMMISSIONIG_RESULT_ID) {
            switch (resultCode) {
                case GuidedCommissioning.COMMISSIONING_SUCCESS:

                    if (sandboxId == null) {
                        finish();
                    } else {
                        ArrayList<Peer> commissionedPeers = (ArrayList<Peer>) data.getSerializableExtra("commissionedPeers");
                        for (Peer commissionedPeer : commissionedPeers) {
                            Sandbox.addPeer(sandboxId, commissionedPeer, new Sandbox.AddPeerCb() {
                                @Override
                                public void cb() {
                                }

                                @Override
                                public void err(int i, String s) {
                                }

                                @Override
                                public void end() {
                                }
                            });
                        }
                        finish();
                    }
                    break;
                case GuidedCommissioning.COMMISSIONING_RETRY:
                    commissioningTag.refresh();
                    break;
                case GuidedCommissioning.COMMISSIONING_CANCELLED:
                    finish();
                    break;
            }
        }
    }

    @Override
    public void onSetFabExtra(boolean state) {
        if (state) {
            fabExtra.show();
        } else {
            fabExtra.hide();
        }
    }
}
