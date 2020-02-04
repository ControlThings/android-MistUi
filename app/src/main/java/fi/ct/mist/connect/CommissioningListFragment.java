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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import fi.ct.mist.mist.R;
import wish.LocalDiscovery;
import wish.request.Wld;


/**
 * Created by jeppe on 12/5/16.
 */

public class CommissioningListFragment extends Fragment {

    private final String TAG = "CommissioningList";

    private ContentAdapter adapter = null;

    private List<CommissionItem> connections;
    private List<CommissionItem.WifiConnection> wifiConnections;
    private List<CommissionItem.WldConnection> wldConnections;

    private SwipeRefreshLayout swipeRefreshLayout;

    private WifiManager wifiManager;
    private IntentFilter networkFilter;
    private final static String ssidPrefix = "mist-";

    public CommissioningListFragment() {
        connections = new ArrayList<>();
        wldConnections = new ArrayList<>();
        wifiConnections = new ArrayList<>();

        networkFilter = new IntentFilter();
        networkFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void refresh() {
        try {
            getActivity().unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
        }
        getActivity().registerReceiver(broadcastReceiver, networkFilter);
        refreshWldConnections();
        wifiManager.startScan();
    }

    private void refreshWldConnections() {
        wldConnections.clear();
        Wld.list(new Wld.ListCb() {
            @Override
            public void cb(List<LocalDiscovery> arrayList) {
                for (LocalDiscovery localDiscovery : arrayList) {
                    if (localDiscovery.isClaim()) {
                        CommissionItem.WldConnection wldConnection = new CommissionItem().new WldConnection();
                        wldConnection.setName(localDiscovery.getAlias());
                        wldConnection.setRuid(localDiscovery.getRuid());
                        wldConnection.setRhid(localDiscovery.getRhid());
                        wldConnections.add(wldConnection);
                        update();
                    }
                }
            }

            @Override
            public void err(int i, String s) {}

            @Override
            public void end() {}
        });
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");
            //Lists all wifi networks
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Log.d(TAG, "onReceive, we have scan results");
                List<ScanResult> wifiScanList = wifiManager.getScanResults();
                wifiConnections.clear();
                if (wifiScanList != null) {
                    for (ScanResult network : wifiScanList) {
                        String ssid = network.SSID.toString();
                        Log.d(TAG, "ssid: " + ssid );
                        if (ssid.contains(ssidPrefix)) {  //&& !("\""+ssid+"\"").equals( wifiManager.getConnectionInfo().getSSID())
                            CommissionItem.WifiConnection wifiConnection = new CommissionItem().new WifiConnection();
                            wifiConnection.setSsid(ssid);
                            wifiConnection.setName(ssid.substring(5, ssid.length()));
                            wifiConnection.setLevel(WifiManager.calculateSignalLevel(network.level, 5));
                            if (network.capabilities.toUpperCase().contains("WPA") ||
                                    network.capabilities.toUpperCase().contains("WPA2")) {
                                wifiConnection.setSecurity("WPA");
                            } else if (network.capabilities.toUpperCase().contains("WEP")) {
                                wifiConnection.setSecurity("WEP");
                            } else {
                                wifiConnection.setSecurity("OPEN");
                            }
                            wifiConnections.add(wifiConnection);
                        }
                    }
                }
                update();
            }




        }

    };

    private void update() {
        connections.clear();
        if (wldConnections.size() != 0) {
            CommissionItem.Header header = new CommissionItem().new Header();
            header.setText(getResources().getString(R.string.commissioning_divider_wld));
            connections.add(header);
            for (CommissionItem.WldConnection wldConnection : wldConnections) {
                connections.add(wldConnection);
            }
        }
        if (wifiConnections.size() != 0) {
            CommissionItem.Header header = new CommissionItem().new Header();
            header.setText(getResources().getString(R.string.commissioning_divider_wifi));
            connections.add(header);
            for (CommissionItem.WifiConnection wifiConnection : wifiConnections) {
                connections.add(wifiConnection);
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

        swipeRefreshLayout = (SwipeRefreshLayout) inflater.inflate(R.layout.swipe_view, container, false);
        RecyclerView recyclerView = (RecyclerView) swipeRefreshLayout.findViewById(R.id.recycler_view);

        adapter = new ContentAdapter(this, connections);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        swipeRefreshLayout.setOnRefreshListener(refreshListener);

        wifiManager = (WifiManager)  getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        getActivity().registerReceiver(broadcastReceiver, networkFilter);

        return swipeRefreshLayout;
    }

    private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refreshWldConnections();
            wifiManager.startScan();
        }
    };

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        refreshWldConnections();
        wifiManager.startScan();
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

    private static class ViewHolderWld extends RecyclerView.ViewHolder {

        TextView primary;
        LinearLayout button;

        public ViewHolderWld(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.one_line_list, parent, false));
            primary = (TextView) itemView.findViewById(R.id.one_line_primary);
            button = (LinearLayout) itemView.findViewById(R.id.one_line_layout);
        }
    }

    private static class ViewHolderWifi extends RecyclerView.ViewHolder {

        TextView title;
        TextView subTitle;
        RelativeLayout button;
        ImageView icon;

        public ViewHolderWifi(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_wifi, parent, false));
            title = (TextView) itemView.findViewById(R.id.list_item_wifi_title);
            button = (RelativeLayout) itemView.findViewById(R.id.list_item_wifi_click);
            subTitle = (TextView) itemView.findViewById(R.id.list_item_wifi_subtitle);
            icon = (ImageView) itemView.findViewById(R.id.list_item_wifi_right_icon);
        }
    }


    private static class ContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final String TAG = "UsersAdapter";
        Activity _activity;
        CommissioningListFragment fragment;
        private List<CommissionItem> _connections;

        public ContentAdapter(CommissioningListFragment usersFragment, List<CommissionItem> connections) {
            this.fragment = usersFragment;
            this._activity = usersFragment.getActivity();
            this._connections = connections;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == CommissionItem.TYPE_HEADER) {
                return new ViewHolderHeader(LayoutInflater.from(parent.getContext()), parent);
            }
            if (viewType == CommissionItem.TYPE_WLD) {
                return new ViewHolderWld(LayoutInflater.from(parent.getContext()), parent);
            }
            if (viewType == CommissionItem.TYPE_WIFI) {
                return new ViewHolderWifi(LayoutInflater.from(parent.getContext()), parent);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {

            int type = getItemViewType(position);

            if (type == CommissionItem.TYPE_HEADER) {
                CommissionItem.Header header = (CommissionItem.Header) _connections.get(position);
                ViewHolderHeader viewHolder = (ViewHolderHeader) holder;
                viewHolder.primary.setText(header.getText());
            } else if (type == CommissionItem.TYPE_WLD) {
                final CommissionItem.WldConnection wldConnection = (CommissionItem.WldConnection) _connections.get(position);
                ViewHolderWld viewHolder = (ViewHolderWld) holder;
                viewHolder.primary.setText(wldConnection.getName());
                viewHolder.button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CommissioningListener listener = (CommissioningListener) _activity;
                        listener.onCommissionListItemClicked(wldConnection);
                    }
                });
            } else if (type == CommissionItem.TYPE_WIFI) {

                final CommissionItem.WifiConnection wifiConnection = (CommissionItem.WifiConnection) _connections.get(position);
                ViewHolderWifi viewHolder = (ViewHolderWifi) holder;

                String ssid = wifiConnection.getSsid();
                viewHolder.title.setText(wifiConnection.getName());

                String mac = ssid.substring(ssid.length() - 6, ssid.length());
                viewHolder.subTitle.setText(mac.substring(0, 2) + ":" + mac.substring(2, 4) + ":" + mac.substring(4, 6));

                switch (wifiConnection.getLevel()) {
                    case 0:
                        viewHolder.icon.setImageResource(R.drawable.ic_signal_wifi_0_bar_black_24dp);
                        break;
                    case 1:
                        viewHolder.icon.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_24dp);
                        break;
                    case 2:
                        viewHolder.icon.setImageResource(R.drawable.ic_signal_wifi_2_bar_black_24dp);
                        break;
                    case 3:
                        viewHolder.icon.setImageResource(R.drawable.ic_signal_wifi_3_bar_black_24dp);
                        break;
                    case 4:
                        viewHolder.icon.setImageResource(R.drawable.ic_signal_wifi_4_bar_black_24dp);
                        break;
                }

                viewHolder.button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CommissioningListener listener = (CommissioningListener) _activity;
                        listener.onCommissionListItemClicked(wifiConnection);
                    }
                });
            };
        }

        @Override
        public int getItemViewType(int position) {
            return _connections.get(position).getType();
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
        super.onDestroyView();
        swipeRefreshLayout.removeAllViews();
        try {
            getActivity().unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
        }

    }



    public interface CommissioningListener {
        public void onCommissionListItemClicked(CommissionItem item);
    }
}
