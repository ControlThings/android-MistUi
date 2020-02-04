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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import fi.ct.mist.dialogs.DeleteDialog;
import fi.ct.mist.mist.R;

import wish.Peer;
import mist.node.request.Control;
import mist.api.request.Mist;
import mist.api.request.Mist;
import mist.api.request.Sandbox;
import wish.request.Identity;

import static utils.Util.*;
/**
 * Created by jeppe on 12/5/16.
 */

public class RemoveFragment extends Fragment {

    private final String TAG = "RemoveFragment";

    private ContentAdapter adapter = null;
    private ArrayList<PeerInfo> peerInfos;

    private SwipeRefreshLayout swipeRefreshLayout;
    private int signalsId = 0;

    public RemoveFragment() {
        peerInfos = new ArrayList<>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void refresh() {
        peerInfos.clear();
        Log.d(TAG, "refreshIdentities");
        SettingsFragmentListener listener = (SettingsFragmentListener) getActivity();
        Sandbox.listPeers(listener.getSandboxId(), new Sandbox.ListPeersCb() {
            @Override
            public void cb(List<Peer> arrayList) {
                for (Peer peer : arrayList) {
                    if (peer.isOnline()) {
                        getName(peer);
                    } else {
                       PeerInfo info = new PeerInfo();
                        info.setEndpoint(byteArrayToHexString(peer.getLuid()).substring(0, 10));
                        info.setPeer(peer);
                        getIdentity(info, peer);
                     //   update();
                    }
                }
                update();
            }
            @Override
            public void err(int i, String s) {}
            @Override
            public void end() {}
        });
    }

    private void getName(final Peer peer) {
        Control.read(peer, "mist.name", new Control.ReadCb() {
            @Override
            public void cbString(String name) {
                PeerInfo info = new PeerInfo();
                info.setPeer(peer);
                info.setEndpoint(name);
                getIdentity(info, peer);
            }

        });
    }
    /*
    private void getModel(final Peer peer) {
        Control.model(peer, new Control.ModelCb() {
            @Override
            public void cb(JSONObject jsonObject) {
                PeerInfo info = new PeerInfo();
                info.setPeer(peer);
                try {
                    info.setEndpoint(jsonObject.getString("device"));
                    getIdentity(info, peer);
                } catch (JSONException e) {
                    Log.d(TAG, "json exeptionn: " + e);
                    update();
                }
            }
            @Override
            public void err(int i, String s) {}

            @Override
            public void end() {}
        });
    }
*/
    private void getIdentity(final PeerInfo info, Peer peer) {
        Identity.get(peer.getRuid(), new Identity.GetCb() {
            @Override
            public void cb(wish.Identity mistIdentity) {
                info.setAlias(mistIdentity.getAlias());
                peerInfos.add(info);
                update();
            }

            @Override
            public void err(int i, String s) {
                Log.d(TAG, "e: " + s + " "+ i) ;
                info.setAlias(getResources().getString(R.string.advanced_settings_remove_unknown));
                peerInfos.add(info);
                update();
            }

            @Override
            public void end() {}
        });
    }

    private void update() {
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

        swipeRefreshLayout = (SwipeRefreshLayout) inflater.inflate(R.layout.swipe_view, container, false);
        RecyclerView recyclerView = (RecyclerView) swipeRefreshLayout.findViewById(R.id.recycler_view);

        adapter = new ContentAdapter(getActivity(), peerInfos);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        swipeRefreshLayout.setOnRefreshListener(refreshListener);
        return swipeRefreshLayout;
    }

    private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refresh();
        }
    };


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refresh();
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        TextView primary;
        TextView secondary;
        LinearLayout button;

        public ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.two_line_list, parent, false));
            primary = (TextView) itemView.findViewById(R.id.two_line_primary);
            secondary = (TextView) itemView.findViewById(R.id.two_line_secondary);
            button = (LinearLayout) itemView.findViewById(R.id.two_line_layout);
        }
    }


    private static class ContentAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final String TAG = "ConnectionsAdapter";
        Activity _activity;
        private ArrayList<PeerInfo> _peerInfos;

        public ContentAdapter(Activity activity, ArrayList<PeerInfo> peerInfos) {
            this._activity = activity;
            this._peerInfos = peerInfos;
        }


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final PeerInfo info = _peerInfos.get(position);
            holder.primary.setText(info.getEndpoint());
            holder.secondary.setText(info.getAlias());
            holder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DeleteDialog.showDialog(_activity, info.getEndpoint(), new DeleteDialog.Cb() {
                        @Override
                        public void delete() {
                            SettingsFragmentListener listener = (SettingsFragmentListener) _activity;
                            listener.removePeer(info.peer);
                        }
                    });
                }
            });
        }

        @Override
        public int getItemCount() {
            return _peerInfos.size();
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
    public void onDestroyView() {
        if (signalsId != 0) {
            Mist.cancel(signalsId);
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.removeAllViews();
        }
        super.onDestroyView();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }


    private class PeerInfo extends wish.Connection {

        private Peer peer;
        private String endpoint;
        private String alias;

        public Peer getPeer() {
            return peer;
        }

        public void setPeer(Peer peer) {
            this.peer = peer;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }
    }

}
