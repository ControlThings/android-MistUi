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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.bson.BsonDocument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fi.ct.mist.connect.Connect;
import fi.ct.mist.dialogs.DeleteDialog;
import fi.ct.mist.dialogs.IdentityAccepFriendDialog;
import fi.ct.mist.dialogs.QrDialog;
import fi.ct.mist.main.MainFragmentListener;
import fi.ct.mist.mist.R;
import wish.Connection;
import wish.Request;
import wish.request.Identity;
import mist.api.request.Mist;
import wish.request.Wish;

import static utils.Util.byteArrayToZipString;

/**
 * Created by jeppe on 12/5/16.
 */

public class IdentitiesFragment extends Fragment {

    private final String TAG = "IdentityFragment";

    private ContentAdapter adapter = null;

    private String alias;

    private List<ListItem> identities;
    private List<OwnIdentity> ownIdentities;
    private List<TrustedIdentity> trustedIdentities;
    private List<QuarentinedIdentity> quarentinedIdentities;

    private SwipeRefreshLayout swipeRefreshLayout;
    boolean refreshReady;

    private Connection connection = null;

    private Integer wishSignal;

    public IdentitiesFragment() {
        identities = new ArrayList<>();
        ownIdentities = new ArrayList<>();
        trustedIdentities = new ArrayList<>();
        quarentinedIdentities = new ArrayList<>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void onFriendrequest(final Context context) {
        if (wishSignal != null) {
            wishSignal = Wish.signals(connection, new Wish.SignalsCb() {
                @Override
                public void cb(String s) {
                    if (s.equals("friendRequest")) {
                        refreshIdentities(context);
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
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void refreshIdentities(final Context context) {
        if (connection == null) {
            ownIdentities.clear();
            trustedIdentities.clear();
            quarentinedIdentities.clear();
            update(context);
        } else {
            refreshReady = false;

            Identity.list(connection, new Identity.ListCb() {
                @Override
                public void cb(List<wish.Identity> arrayList) {
                    ownIdentities.clear();
                    trustedIdentities.clear();
                    for (wish.Identity identity : arrayList) {
                        if (identity.isPrivkey()) {
                            OwnIdentity ownIdentity = new OwnIdentity();
                            ownIdentity.setAlias(identity.getAlias());
                            ownIdentity.setUid(identity.getUid());
                            ownIdentities.add(ownIdentity);

                        } else {
                            TrustedIdentity trustedIdentity = new TrustedIdentity();
                            trustedIdentity.setAlias(identity.getAlias());
                            trustedIdentity.setUid(identity.getUid());
                            trustedIdentities.add(trustedIdentity);
                        }
                    }
                    if (refreshReady) {
                        update(context);
                    } else {
                        refreshReady = true;
                    }
                }

                @Override
                public void err(int i, String s) {
                }

                @Override
                public void end() {
                }
            });
            Identity.friendRequestList(connection, new Identity.FriendRequestListCb() {
                @Override
                public void cb(List<Request> arrayList) {
                    quarentinedIdentities.clear();
                    for (Request identity : arrayList) {
                        QuarentinedIdentity quarentinedIdentity = new QuarentinedIdentity();
                        quarentinedIdentity.setAlias(identity.getAlias());
                        quarentinedIdentity.setUid(null);
                        quarentinedIdentity.setLuid(identity.getLuid());
                        quarentinedIdentity.setRuid(identity.getRuid());
                        quarentinedIdentities.add(quarentinedIdentity);
                    }
                    if (refreshReady) {
                        update(context);
                    } else {
                        refreshReady = true;
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
    }

    private void update(Context context) {
        identities.clear();
        if (ownIdentities.size() != 0) {
            Header header = new Header();
            header.setText(context.getResources().getString(R.string.remote_user_identities, alias));
            if (alias != null) {
                header.setAliasLength(alias.length());
            }
            identities.add(header);
            for (OwnIdentity identity : ownIdentities) {
                identities.add(identity);
            }
        }
        if (trustedIdentities.size() != 0) {
            Header header = new Header();
            header.setText(context.getResources().getString(R.string.remote_user_friends, alias));
            if (alias != null) {
                header.setAliasLength(alias.length());
            }
            identities.add(header);
            for (TrustedIdentity identity : trustedIdentities) {
                identities.add(identity);
            }
        }
        if (quarentinedIdentities.size() != 0) {
            Header header = new Header();
            header.setText(context.getResources().getString(R.string.remote_user_quarantined, alias));
            if (alias != null) {
                header.setAliasLength(alias.length());
            }
            identities.add(header);
            for (QuarentinedIdentity identity : quarentinedIdentities) {
                identities.add(identity);
            }
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if (adapter != null) {
            adapter.update();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        alias = getArguments().getString("alias");
        wishSignal = null;
        swipeRefreshLayout = (SwipeRefreshLayout) inflater.inflate(R.layout.swipe_view, container, false);
        RecyclerView recyclerView = (RecyclerView) swipeRefreshLayout.findViewById(R.id.recycler_view);

        adapter = new ContentAdapter(connection, this, identities);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        swipeRefreshLayout.setOnRefreshListener(refreshListener);
        return swipeRefreshLayout;
    }

    private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refreshIdentities(getContext());
        }
    };

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        refreshIdentities(getContext());
        onFriendrequest(getContext());
    }

    private static class ViewHolderHeader extends RecyclerView.ViewHolder {

        TextView primary;
        LinearLayout button;

        public ViewHolderHeader(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_header, parent, false));
            primary = (TextView) itemView.findViewById(R.id.header_primary);
            button = (LinearLayout) itemView.findViewById(R.id.header_layout);
        }
    }

    private static class ViewHolderIdentity extends RecyclerView.ViewHolder {

        TextView primary;
        RelativeLayout button;
        RelativeLayout qrButton;
        ImageView state;
        ImageView qr;

        public ViewHolderIdentity(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_users, parent, false));
            primary = (TextView) itemView.findViewById(R.id.list_item_users_title);
            button = (RelativeLayout) itemView.findViewById(R.id.list_item_users_click);
            qrButton = (RelativeLayout) itemView.findViewById(R.id.list_item_users_qr_click);
            state = (ImageView) itemView.findViewById(R.id.list_item_users_left_icon);
            qr = (ImageView) itemView.findViewById(R.id.list_item_users_right_icon);
        }
    }


    private static class ContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final String TAG = "UsersAdapter";
        Activity _activity;
        IdentitiesFragment fragment;
        private List<ListItem> _identites;
        Connection _connection;

        public ContentAdapter(Connection connection, IdentitiesFragment usersFragment, List<ListItem> identites) {
            this._connection = connection;
            this.fragment = usersFragment;
            this._activity = usersFragment.getActivity();
            this._identites = identites;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == ListItem.TYPE_HEADER) {
                return new ViewHolderHeader(LayoutInflater.from(parent.getContext()), parent);
            }
            return new ViewHolderIdentity(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {

            int type = getItemViewType(position);

            if (type == ListItem.TYPE_HEADER) {
                Header header = (Header) _identites.get(position);
                ViewHolderHeader viewHolder = (ViewHolderHeader) holder;
                //Change the alias collor to black
                if (header.getAliasLength() != 0) {
                    SpannableString ss = new SpannableString(header.getText());
                    int textLength = header.getText().length();
                    ss.setSpan(new ForegroundColorSpan(Color.BLACK),textLength-header.getAliasLength(),textLength,0);
                    viewHolder.primary.setText(ss);
                } else {
                    viewHolder.primary.setText(header.getText());
                }

            } else if (type == ListItem.TYPE_QUARANTINED) {
                final QuarentinedIdentity quarentinedIdentity = (QuarentinedIdentity) _identites.get(position);
                ViewHolderIdentity viewHolder = (ViewHolderIdentity) holder;
                viewHolder.primary.setText(quarentinedIdentity.getAlias());

                viewHolder.button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        IdentityAccepFriendDialog.showDialog(_activity, quarentinedIdentity.getAlias(), new IdentityAccepFriendDialog.Cb() {
                            @Override
                            public void accept() {
                                Identity.friendRequestAccept(_connection, quarentinedIdentity.getLuid(), quarentinedIdentity.getRuid(), new Identity.FriendRequestAcceptCb() {
                                    @Override
                                    public void cb(boolean b) {
                                        Log.d("friend", "res");
                                        fragment.refreshIdentities(fragment.getContext());
                                    }

                                    @Override
                                    public void err(int i, String s) {}

                                    @Override
                                    public void end() {}
                                });
                            }

                            @Override
                            public void decline() {
                                Identity.friendRequestDecline(_connection, quarentinedIdentity.getLuid(), quarentinedIdentity.getRuid(), new Identity.FriendRequestDeclineCb() {
                                    @Override
                                    public void cb(boolean b) {
                                        fragment.refreshIdentities(fragment.getContext());
                                    }

                                    @Override
                                    public void err(int i, String s) {}

                                    @Override
                                    public void end() {}
                                });
                            }
                        });
                    }
                });
            } else {
                final UserIdentity userIdentity = (UserIdentity) _identites.get(position);
                ViewHolderIdentity viewHolder = (ViewHolderIdentity) holder;
                viewHolder.primary.setText(userIdentity.getAlias());

                viewHolder.button.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        DeleteDialog.showDialog(_activity, userIdentity.getAlias(), new DeleteDialog.Cb() {
                            @Override
                            public void delete() {
                                Identity.remove(_connection, userIdentity.getUid(), new Identity.RemoveCb() {
                                    @Override
                                    public void cb(boolean b) {
                                        fragment.refreshIdentities(fragment.getContext());
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
                        return true;
                    }
                });

                if (type == ListItem.TYPE_OWN) {
                    viewHolder.state.setImageResource(R.drawable.ic_person_black_24dp);
                    //todo add functionality to select identity on remote core
                   /*
                    viewHolder.button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            MainFragmentListener listener = (MainFragmentListener) _activity;
                            listener.setIdentity(((UserIdentity) _identites.get(position)).alias);
                            Snackbar snackbar = Snackbar
                                    .make(_activity.findViewById(R.id.coordinatorLayout), userIdentity.getAlias() + " selected", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    });
                    */
                    viewHolder.qr.setVisibility(View.VISIBLE);
                    viewHolder.qrButton.setVisibility(View.VISIBLE);
                    viewHolder.qrButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Identity.export(_connection, userIdentity.getUid(), new Identity.ExportCb() {
                                @Override
                                public void cb(byte[] bytes, byte[] raw) {
                                    try {
                                        String s = "wish://friendRequest?";
                                        QrDialog.showDialog(_activity, s + byteArrayToZipString(bytes));
                                    } catch (IOException e) {
                                        Log.d(TAG, "Error parsing identity: " + e);
                                        return;
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
                    });
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            return _identites.get(position).getType();
        }

        @Override
        public int getItemCount() {
            return _identites.size();
        }


        public void update() {
            _activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "update");
                    notifyDataSetChanged();
                }
            });
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        swipeRefreshLayout.removeAllViews();
        if (wishSignal != null) {
            Wish.cancel(wishSignal);
        }
    }


    private abstract class ListItem {

        public static final int TYPE_HEADER = 1;
        public static final int TYPE_OWN = 2;
        public static final int TYPE_TRUSTED = 3;
        public static final int TYPE_QUARANTINED = 4;

        abstract public int getType();

    }

    private class Header extends ListItem {
        String text;
        int aliasLength;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getAliasLength() {
            return aliasLength;
        }

        public void setAliasLength(int aliasLength) {
            this.aliasLength = aliasLength;
        }

        @Override
        public int getType() {
            return TYPE_HEADER;
        }
    }

    private class UserIdentity extends ListItem {

        private byte[] uid;
        private String alias;

        public byte[] getUid() {
            return uid;
        }

        public void setUid(byte[] uid) {
            this.uid = uid;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        @Override
        public int getType() {
            return 0;
        }

    }

    private class OwnIdentity extends UserIdentity {

        @Override
        public int getType() {
            return TYPE_OWN;
        }
    }

    private class TrustedIdentity extends UserIdentity {
        @Override
        public int getType() {
            return TYPE_TRUSTED;
        }
    }

    private class QuarentinedIdentity extends UserIdentity {

        private byte[] luid;
        private byte[] ruid;

        public byte[] getLuid() {
            return luid;
        }

        public void setLuid(byte[] luid) {
            this.luid = luid;
        }

        public byte[] getRuid() {
            return ruid;
        }

        public void setRuid(byte[] ruid) {
            this.ruid = ruid;
        }

        @Override
        public int getType() {
            return TYPE_QUARANTINED;
        }
    }
}
