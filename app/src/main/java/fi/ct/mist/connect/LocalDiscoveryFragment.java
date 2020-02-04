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
package fi.ct.mist.connect;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.ct.mist.dialogs.FriendRequestDialog;
import fi.ct.mist.mist.R;
import fi.ct.mist.system.ManageFragment;
import wish.LocalDiscovery;
import wish.request.Identity;
import wish.request.Wld;

/**
 * Created by jeppe on 12/5/16.
 */

public class LocalDiscoveryFragment extends Fragment {

    private final String TAG = "LocalDiscoveryFragment";

    private final String type = "local";

    private ContentAdapter adapter = null;
    private ArrayList<LocalDiscovery> connections;
    private SwipeRefreshLayout swipeRefreshLayout;

    private boolean visible;

    public LocalDiscoveryFragment() {
        connections = new ArrayList<LocalDiscovery>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        visible = menuVisible;
        setFab();
    }

    private void setFab() {
        if (isAdded()) {
            LocalDiscoveryListener listener = (LocalDiscoveryListener) getActivity();
            listener.onSetFabExtra(visible);
        }
    }

    public void refreshConnections() {
        connections.clear();
        update();
            Wld.list(new Wld.ListCb() {
                @Override
                public void cb(final List<LocalDiscovery> list) {
                    Identity.list(new Identity.ListCb() {
                        @Override
                        public void cb(List<wish.Identity> identiyList) {
                            for (LocalDiscovery localDiscovery : list) {
                                if (!localDiscovery.isClaim()) {
                                    boolean exist = false;
                                    for (wish.Identity identity : identiyList) {
                                        if (Arrays.equals(identity.getUid(), localDiscovery.getRuid())) {
                                            exist = true;
                                            break;
                                        }

                                    }
                                    if (!exist && localDiscovery.getType().equals(type)) {
                                        connections.add(localDiscovery);
                                    }
                                }
                            }
                            update();
                            if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }

                        @Override
                        public void err(int i, String s) {}

                        @Override
                        public void end() {}
                    });
                }

                @Override
                public void err(int i, String s) { }

                @Override
                public void end() {}
            });
    }

    private void update() {
        if (adapter != null) {
            adapter.update();
        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView" );

        setFab();

        swipeRefreshLayout = (SwipeRefreshLayout) inflater.inflate(R.layout.swipe_view, container, false);
        RecyclerView recyclerView = (RecyclerView) swipeRefreshLayout.findViewById(R.id.recycler_view);

        FloatingActionButton fabExtra = (FloatingActionButton)  inflater.inflate(R.layout.menu, container, false).findViewById(R.id.fab_extra);
        fabExtra.setVisibility(View.VISIBLE);

        adapter = new ContentAdapter(getActivity(), connections);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        refreshConnections();
        swipeRefreshLayout.setOnRefreshListener(refreshListener);
        return swipeRefreshLayout;
    }


    private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refreshConnections();

        }
    };

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
        private final String TAG = "LocalDiscoveryAdapter";
        Activity _activity;
        private ArrayList<LocalDiscovery> _connections;

        public ContentAdapter(Activity activity, ArrayList<LocalDiscovery> connections) {
            this._activity = activity;
            this._connections = connections;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            holder.primary.setText(_connections.get(position).getAlias());
            holder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FriendRequestDialog.showDialog(_activity, _connections.get(position).getAlias(), new FriendRequestDialog.Cb() {
                        @Override
                        public void send() {
                            LocalDiscoveryListener listener = (LocalDiscoveryListener) _activity;
                            listener.onFriendRequest(_connections.get(position));
                        }
                    });
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
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    public void onDestroyView() {
        swipeRefreshLayout.removeAllViews();
        super.onDestroyView();
    }

    public interface LocalDiscoveryListener {
        public void onFriendRequest(LocalDiscovery data);
        public void onSetFabExtra(boolean state);
    }
}
