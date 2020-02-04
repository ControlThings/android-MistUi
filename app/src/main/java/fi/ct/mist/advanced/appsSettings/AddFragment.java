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
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.bson.BsonDocument;

import java.util.ArrayList;
import java.util.List;

import fi.ct.mist.dialogs.AddDialog;
import fi.ct.mist.mist.R;
import wish.Peer;
import mist.node.request.Control;
import wish.request.Identity;
import mist.api.request.Mist;
import mist.api.request.Sandbox;

/**
 * Created by jeppe on 12/5/16.
 */

public class AddFragment extends Fragment {

    private final String TAG = "AddFragment";

    private ContentAdapter adapter = null;
    private ArrayList<PeerInfo> peerInfos;

    private SwipeRefreshLayout swipeRefreshLayout;
    private int signalsId = 0;

    public AddFragment() {
        peerInfos = new ArrayList<>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void signals() {
        signalsId = Mist.signals(new Mist.SignalsCb() {
            @Override
            public void cb(String s, BsonDocument bsonDocument) {
                if (s.equals("peers")) {
                    refresh();
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

    //todo remove this
    private int getOldPeerId(Peer peer) {
        String id = Base64.encodeToString(peer.getLuid(), Base64.NO_WRAP) +
                Base64.encodeToString(peer.getRuid(), Base64.NO_WRAP) +
                Base64.encodeToString(peer.getRsid(), Base64.NO_WRAP) +
                Base64.encodeToString(peer.getRhid(), Base64.NO_WRAP);
        return id.hashCode();
    }

    public void refresh() {
        peerInfos.clear();
        SettingsFragmentListener listener = (SettingsFragmentListener) getActivity();
        Sandbox.listPeers(listener.getSandboxId(), new Sandbox.ListPeersCb() {
            @Override
            public void cb(final List<Peer> sandboxList) {
                final List<Integer> ids = new ArrayList<>();
                for (Peer peer : sandboxList) {
                    ids.add(getOldPeerId(peer));
                }

                Mist.listServices(new Mist.ListServicesCb() {
                    @Override
                    public void cb(List<Peer> arrayList) {
                        for (Peer peer : arrayList) {
                            if (!ids.contains(getOldPeerId(peer)) && peer.isOnline()) {
                                getInfo(peer);
                            }
                        }
                        update();
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
            public void err(int i, String s) {

            }

            @Override
            public void end() {

            }
        });
    }

    private void getInfo(final Peer peer) {
        Control.read(peer, "mist.name", new Control.ReadCb() {
            @Override
            public void cbString(String name) {
                final PeerInfo info = new PeerInfo();
                info.setPeer(peer);
                info.setEndpoint(name);
                Identity.get(peer.getRuid(), new Identity.GetCb() {
                    @Override
                    public void cb(wish.Identity identity) {
                        info.setAlias(identity.getAlias());
                        peerInfos.add(info);
                        update();
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
    /*
    private void getModel(final Peer peer) {
        Control.model(peer, new Control.ModelCb() {
            @Override
            public void cb(JSONObject jsonObject) {
                final PeerInfo info = new PeerInfo();
                info.setPeer(peer);
                try {
                    info.setEndpoint(jsonObject.getString("device"));
                    Identity.get(peer.getRuid(), new Identity.GetCb() {
                        @Override
                        public void cb(Identity mistIdentity) {
                            info.setAlias(mistIdentity.getAlias());
                            peerInfos.add(info);
                            update();
                        }

                        @Override
                        public void err(int i, String s) {
                        }

                        @Override
                        public void end() {
                        }
                    });

                } catch (JSONException e) {
                    Log.d(TAG, "json exeptionn: " + e);
                }
            }

            @Override
            public void err(int i, String s) {
            }

            @Override
            public void end() {
            }
        });
    }*/

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
        signals();
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
                    final SettingsFragmentListener listener = (SettingsFragmentListener) _activity;
                    AddDialog.showDialog(_activity, String.format(_activity.getResources().getString(R.string.advanced_settings_adddialog), info.getEndpoint(), listener.getName()), null, new AddDialog.Cb() {
                        @Override
                        public void add() {
                            listener.addPeer(info.getPeer());
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
