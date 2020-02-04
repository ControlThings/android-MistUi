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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fi.ct.mist.mist.R;
import fi.ct.mist.dialogs.DeleteDialog;
import wish.request.Connection;
import wish.request.Identity;

/**
 * Created by jeppe on 12/5/16.
 */

public class ConnectionsFragment extends Fragment {

    private final String TAG = "ConnectionsFragment";

    private ContentAdapter adapter = null;
    private ArrayList<ConnectionInfo> connections;

    private SwipeRefreshLayout swipeRefreshLayout;

    public ConnectionsFragment() {
        connections = new ArrayList<ConnectionInfo>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    public void refreshIdentities() {
        final HashMap<ByteBuffer, String> identities = new HashMap<>();
        connections.clear();
        Log.d(TAG, "refreshIdentities1");
        Identity.list(new Identity.ListCb() {
            @Override
            public void cb(List<wish.Identity> arrayList) {
                Log.d(TAG, "refreshIdentities2");
                for (wish.Identity identity : arrayList) {
                    identities.put(ByteBuffer.wrap(identity.getUid()), identity.getAlias());
                }
                Connection.list(new Connection.ListCb() {
                    @Override
                    public void cb(List<wish.Connection> arrayList) {
                        Log.d(TAG, "refreshIdentities3");
                       /* for (Map.Entry<byte[], String> e : identities.entrySet()) {
                            Log.d(TAG, "key: " + e.getKey());
                            Log.d(TAG, "v: " + e.getValue());
                        }*/

                        for (wish.Connection connection : arrayList) {
                            ConnectionInfo info = new ConnectionInfo();
                            if (identities.containsKey(ByteBuffer.wrap(connection.getLuid())) && identities.containsKey(ByteBuffer.wrap(connection.getRuid()))) {
                                info.setLuidAlias(identities.get(ByteBuffer.wrap(connection.getLuid())));
                                info.setRuidAlias(identities.get(ByteBuffer.wrap(connection.getRuid())));
                                info.setCid(connection.getCid());
                                info.setOutgoing(connection.isOutgoing());
                                info.setLuid(connection.getLuid());
                                info.setRuid(connection.getRuid());
                                info.setRhid(connection.getRhid());
                            } else {
                                break;
                            }



                            connections.add(info);
                        }
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
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

    private void update() {
        if (adapter != null) {
            adapter.update();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        swipeRefreshLayout = (SwipeRefreshLayout) inflater.inflate(R.layout.swipe_view, container, false);
        RecyclerView recyclerView = (RecyclerView) swipeRefreshLayout.findViewById(R.id.recycler_view);

        adapter = new ContentAdapter(getActivity(), connections);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        swipeRefreshLayout.setOnRefreshListener(refreshListener);
        return swipeRefreshLayout;
    }

    private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refreshIdentities();
        }
    };


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refreshIdentities();
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        TextView primary;
        LinearLayout button;

        public ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.one_line_list, parent, false));
            primary = (TextView) itemView.findViewById(R.id.one_line_primary);
            button = (LinearLayout) itemView.findViewById(R.id.one_line_layout);
        }
    }


    private static class ContentAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final String TAG = "ConnectionsAdapter";
        Activity _activity;
        private ArrayList<ConnectionInfo> _connections;

        public ContentAdapter(Activity activity, ArrayList<ConnectionInfo> connections) {
            this._activity = activity;
            this._connections = connections;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final ConnectionInfo connection = _connections.get(position);
            String outgoing = (connection.isOutgoing() ? " > " : " < ");
            String viaRelay = (connection.isRelayed()) ? "(relayed)" : "";
            final String connectonString = connection.getLuidAlias() + outgoing + connection.getRuidAlias() + " " + viaRelay;
            holder.primary.setText(connectonString);

            holder.button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    DeleteDialog.showDialog(_activity, connectonString, new DeleteDialog.Cb() {
                        @Override
                        public void delete() {
                            Connection.disconnect(connection.getCid(), new Connection.DisconnectCb() {
                                @Override
                                public void cb(boolean b) {
                                    _connections.remove(position);
                                    update();
                                }

                                @Override
                                public void err(int i, String s) {
                                    Log.d(TAG, "connections BSON error");
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
        }

        @Override
        public int getItemCount() {
            return _connections.size();
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


    private class ConnectionInfo extends wish.Connection {

        private String luidAlias;
        private String ruidAlias;

        public String getLuidAlias() {
            return luidAlias;
        }

        public void setLuidAlias(String luidAlias) {
            this.luidAlias = luidAlias;
        }

        public String getRuidAlias() {
            return ruidAlias;
        }

        public void setRuidAlias(String ruidAlias) {
            this.ruidAlias = ruidAlias;
        }

    }

}
