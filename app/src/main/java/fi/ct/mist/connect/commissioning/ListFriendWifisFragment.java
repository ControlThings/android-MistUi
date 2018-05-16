package fi.ct.mist.connect.commissioning;

import android.app.Activity;
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
import android.widget.RelativeLayout;
import android.widget.TextView;


import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fi.ct.mist.dialogs.PasswordDialog;
import fi.ct.mist.mist.R;
import wish.Peer;
import mist.node.request.Control;

/**
 * Created by jeppe on 12/5/16.
 */

public class ListFriendWifisFragment extends Fragment {

    public interface Commissioner {
        public void sendWifiConfiguration(String ssid, String password);
    }

    private Commissioner commissioner;
    private Peer peer;

    public void setCommissioner(Commissioner commissioner) {
        this.commissioner = commissioner;
    }

    private final String TAG = "ListFriendWifisFragment";

    private ContentAdapter adapter = null;
    private List<WifiNetworkEntry> wifiNetworkEntries;
    private SwipeRefreshLayout swipeRefreshLayout;

    public ListFriendWifisFragment() {
        wifiNetworkEntries = new ArrayList<>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setWifiNetworks(List<WifiNetworkEntry> wifiNetworkEntries, Peer peer) {
        this.wifiNetworkEntries = wifiNetworkEntries;
        this.peer = peer;
        update();
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

        adapter = new ContentAdapter(getActivity(), wifiNetworkEntries, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        swipeRefreshLayout.setRefreshing(false);
        swipeRefreshLayout.setOnRefreshListener(refreshListener);
        return swipeRefreshLayout;
    }

    private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            if (peer == null) {
                update();
            } else {
                Control.invoke(peer, "mistWifiListAvailable", new Control.InvokeCb() {
                    @Override
                    public void cbDocument(BsonDocument bsonDocument) {
                        List<WifiNetworkEntry> wifiList = new ArrayList<>();
                        wifiNetworkEntries.clear();
                        for (Map.Entry<String, BsonValue> entry : bsonDocument.entrySet()) {
                            WifiNetworkEntry wifiNetworkEntry = new WifiNetworkEntry();
                            BsonDocument document = entry.getValue().asDocument();
                            wifiNetworkEntry.setSsid(document.get("ssid").asString().getValue());
                            wifiNetworkEntry.setStrength(document.get("rssi").asInt32().getValue());
                            wifiNetworkEntries.add(wifiNetworkEntry);
                        }
                        update();
                    }
                });
            }

        }
    };



    private static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        RelativeLayout button;
        ImageView icon;

        public ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_one_line_wifi, parent, false));
            title = (TextView) itemView.findViewById(R.id.list_item_one_line_wifi_title);
            button = (RelativeLayout) itemView.findViewById(R.id.list_item_one_line_wifi_click);
            icon = (ImageView) itemView.findViewById(R.id.list_item_one_line_wifi_right_icon);
        }
    }

    private class ContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final String TAG = "NodeWifiAdapter";
        Activity _activity;
        private List<WifiNetworkEntry> _wifiNetworkEntries;
        ListFriendWifisFragment _fragment;


        public ContentAdapter(Activity activity, List<WifiNetworkEntry> wifiNetworkEntries, ListFriendWifisFragment fragment) {
            this._activity = activity;
            this._wifiNetworkEntries = wifiNetworkEntries;
            this._fragment = fragment;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            final ViewHolder viewHolder = (ViewHolder) holder;
            viewHolder.title.setText(_wifiNetworkEntries.get(position).getSsid());

            switch (WifiManager.calculateSignalLevel(_wifiNetworkEntries.get(position).getStrength(), 5)) {
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
                    PasswordDialog.showDialog(_activity, _wifiNetworkEntries.get(position).getSsid(), new PasswordDialog.Cb() {
                        @Override
                        public void onOk(String ssid, String password) {
                            if (commissioner != null) {
                                commissioner.sendWifiConfiguration(ssid, password);
                            }
                            else {
                                Log.d(TAG, "WARNING: commissioner is null!");
                            }

                            _fragment.swipeRefreshLayout.setRefreshing(true);
                        }
                    });
                }
            });
        }

        @Override
        public int getItemCount() {
            return _wifiNetworkEntries.size();
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
        super.onDestroyView();
        swipeRefreshLayout.removeAllViews();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

}
