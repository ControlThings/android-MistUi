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
package fi.ct.mist.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import fi.ct.mist.connect.commissioning.ManualCommissioning;
import fi.ct.mist.mist.R;
import fi.ct.mist.sandbox.CustomWebView;
import wish.Peer;
import wish.request.Identity;
import mist.api.request.Mist;
import mist.node.request.Control;

/**
 * Created by jeppe on 12/5/16.
 */

public class SystemsFragment extends Fragment {

    private final String TAG = "DevicesFragment";
    private ContentAdapter adapter;

    private ArrayList<Device> devices;
    private HashMap<Integer, Integer> positions;
    private SwipeRefreshLayout swipeRefreshLayout;

    public SystemsFragment() {
        devices = new ArrayList<Device>();
        positions = new HashMap<Integer, Integer>();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private int addDevice(Device device) {
        if (!devices.contains(device)) {
            devices.add(device);
        }
        return (devices.size() - 1);
    }

    private Device getDevice(int position) {
        return devices.get(position);
    }

    public void updateSystems() {
        listServices();
    }

    public void refreshSystems() {
        listServices();
    }

    private int getPeerHash(Peer peer) {
        String id = Base64.encodeToString(peer.getLuid(), Base64.NO_WRAP) +
                Base64.encodeToString(peer.getRuid(), Base64.NO_WRAP) +
                Base64.encodeToString(peer.getRsid(), Base64.NO_WRAP) +
                Base64.encodeToString(peer.getRhid(), Base64.NO_WRAP);
        return id.hashCode();
    }

    private void listServices() {
        devices.clear();
        positions.clear();
        Log.d("Main", "DeviceFragment listServices: ");
        Mist.listServices(new Mist.ListServicesCb() {
            @Override
            public void cb(List<Peer> arrayList) {


                Log.d(TAG, "peer size " + arrayList.size());
                if (arrayList.size() == 0) {
                    update();
                } else {
                    for (Peer peer : arrayList) {
                        Log.d(TAG,  "list size: " +  arrayList.size() + " saved size: " + positions.size() + " "+ devices.size());

                        if (positions.containsKey(getPeerHash(peer))) {
                            Device currentDevice = getDevice(positions.get(getPeerHash(peer)));
                            currentDevice.setOnline(peer.isOnline());
                            update();
                        } else if (peer.isOnline()) {
                            Log.d(TAG, "is online " + peer.getRuid());
                            Device device = new Device();
                            device.setPeer(peer);
                            device.setOnline(peer.isOnline());

                            Identity.get(peer.getRuid(), new Identity.GetCb() {
                                private Device device;

                                @Override
                                public void cb(wish.Identity mistIdentity) {
                                    device.setAlias(mistIdentity.getAlias());
                                    device.setPrivkey(mistIdentity.isPrivkey());

                                    Log.d(TAG, "geting model");

                                    getName(device);
                                    getUi(device);
                                }

                                @Override
                                public void err(int i, String s) {
                                    Log.d(TAG, "err in get " + i + " : " + s);

                                }

                                @Override
                                public void end() {
                                }


                                private Identity.GetCb init(Device device) {
                                    this.device = device;
                                    return this;
                                }
                            }.init(device));
                        } else {
                            update();
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
    }

    private void getName(Device device) {

        Control.read(device.getPeer(), "mist.name", new Control.ReadCb() {
            private Device device;
            @Override
            public void cbString(String name) {
                device.setName(name);
                if (positions.containsKey(getPeerHash(device.getPeer()))) {
                    update();
                } else {
                    if (name.equals("Mist UI")) {
                        update();
                    } else if (name.equals("MistConfig")) {
                        /* FIXME: Add some kind of fancy things for detecting that it actually is a commissioning EP */
                        device.setConfig(true);
                        int position = addDevice(device);
                        positions.put(getPeerHash(device.getPeer()), position);
                        update();
                    } else {
                        int position = addDevice(device);
                        positions.put(getPeerHash(device.getPeer()), position);
                        update();
                    }
                }
            }

            @Override
            public void err(int i, String s) {
                Log.d(TAG, "read mist.name error: " + i + " : " + s);
            }
            private Control.ReadCb init(Device device) {
                this.device = device;
                return this;
            }
        }.init(device));
    }

    private void getUi(Device device) {

        Control.read(device.getPeer(), "mist.ui.url", new Control.ReadCb() {
            private Device device;

            @Override
            public void cbString(String url) {
                device.setUi(url);
                update();
            }

            @Override
            public void err(int i, String s) {
                Log.d(TAG, "read mist.ui.url error: " + i + " : " + s);
            }

            private Control.ReadCb init(Device device) {
                this.device = device;
                return this;
            }
        }.init(device));
    }

    private void update() {

        Collections.sort(devices, new Comparator<Device>(){
            public int compare(Device o1, Device o2){
                return o1.getName().compareTo(o2.getName());
            }
        });

        positions.clear();
        for (Device d : devices) {
            positions.put(getPeerHash(d.getPeer()), positions.size());
        }

        if (adapter != null) {
            adapter.update();
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        swipeRefreshLayout = (SwipeRefreshLayout) inflater.inflate(R.layout.swipe_view, container, false);
        RecyclerView recyclerView = (RecyclerView) swipeRefreshLayout.findViewById(R.id.recycler_view);
        adapter = new ContentAdapter(getActivity(), devices, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        swipeRefreshLayout.setOnRefreshListener(refreshListener);
        return swipeRefreshLayout;
    }

    private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            updateSystems();
        }
    };

    private static class ViewHolder extends RecyclerView.ViewHolder {
        TextView primary;
        TextView secondary;
        RelativeLayout button;
        RelativeLayout custom;
        ImageView customUi;
        ImageView customCommision;
        ImageView state;
        RelativeLayout layout;

        public ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_customui, parent, false));
            primary = (TextView) itemView.findViewById(R.id.list_item_customui_title);
            secondary = (TextView) itemView.findViewById(R.id.list_item_customui_subtitle);
            button = (RelativeLayout) itemView.findViewById(R.id.list_item_customui_click);
            custom = (RelativeLayout) itemView.findViewById(R.id.list_item_customui_right_icon);
            customUi = (ImageView) itemView.findViewById(R.id.list_item_customui_right_icon_ui);
            customCommision = (ImageView) itemView.findViewById(R.id.list_item_customui_right_icon_commision);
            state = (ImageView) itemView.findViewById(R.id.list_item_customui_left_icon);
            layout = (RelativeLayout) itemView.findViewById(R.id.list_item_customui_layout);
        }
    }

    private static class ContentAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final String TAG = "DevicesAdapter";
        Activity _activity;
        private ArrayList<Device> _devices;
        SystemsFragment _fragment;

        public ContentAdapter(Activity activity, ArrayList<Device> devices, SystemsFragment fragment) {
            this._activity = activity;
            this._devices = devices;
            this._fragment = fragment;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final Device device = _devices.get(position);
            holder.primary.setText(device.getName());
            holder.secondary.setText(device.getAlias());
            if (device.isPrivkey()) {
                holder.state.setImageResource(R.drawable.ic_person_black_24dp);
            }


            if (device.getUi() != null) {
                holder.customCommision.setVisibility(View.GONE);
                holder.customUi.setVisibility(View.VISIBLE);
                holder.custom.setVisibility(View.VISIBLE);
                holder.custom.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                         Intent htmlUi = new Intent(_activity, CustomWebView.class);
                         htmlUi.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT );
                         htmlUi.putExtra("peer", device.getPeer());
                         htmlUi.putExtra("alias", device.getAlias());
                         htmlUi.putExtra("name", device.getName());
                         htmlUi.putExtra("url", device.getUi());
                         _activity.startActivity(htmlUi);
                    }
                });
            } else if (device.isConfig()){
                holder.customUi.setVisibility(View.GONE);
                holder.customCommision.setVisibility(View.VISIBLE);
                holder.custom.setVisibility(View.VISIBLE);
                holder.custom.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(_activity, ManualCommissioning.class);
                        intent.putExtra("peer", device.getPeer());
                        intent.putExtra("alias", device.getAlias());
                        _activity.startActivity(intent);
                    }
                });
            } else {
                holder.custom.setVisibility(View.GONE);
            }

            holder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (device.isOnline()) {
                        MainFragmentListener listener = (MainFragmentListener) _activity;
                        listener.setSystem(device);
                    } else {
                        String snackbarString = _activity.findViewById(R.id.coordinatorLayout).getResources().getString(R.string.system_peer_offline, device.getName());
                        Snackbar snackbar = Snackbar
                                .make(_activity.findViewById(R.id.coordinatorLayout),  snackbarString, Snackbar.LENGTH_LONG);

                        snackbar.show();
                    }
                }
            });

            if (device.isOnline() == false) {
                holder.layout.setAlpha(0.3f);
            } else {
                holder.layout.setAlpha(1f);
            }

        }

        @Override
        public int getItemCount() {
            return _devices.size();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
