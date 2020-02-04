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
package fi.ct.mist.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
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

import fi.ct.mist.NotificationService;
import fi.ct.mist.dialogs.DeleteDialog;
import fi.ct.mist.main.MainFragmentListener;
import fi.ct.mist.mist.R;
import fi.ct.mist.system.ModelFragment;
import wish.Identity;
import mist.node.request.Control;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by jeppe on 12/5/16.
 */

public class SettingsFragment extends Fragment {

    public static final String SETTINGS_KEY = "mist.settings.key";
    public static final String NOTIFICATION_KEY = "mist.notification.key";
    public static final String RPC_ERROR_KEY = "mist.rpc.error.key";

    private final String TAG = "SettingsFragment";

    private ContentAdapter adapter = null;

    private List<ListItem> items;

    public SettingsFragment() {
        items = new ArrayList<>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    private void refreshItems() {
        Notifications notifications = new Notifications(getActivity());
        notifications.setName(getString(R.string.settings_notifications));

        RpcErrorMsg rpcErrorMsg = new RpcErrorMsg(getActivity());
        rpcErrorMsg.setName(getString(R.string.settings_rpc_errors));

        items.add(notifications);
        items.add(rpcErrorMsg);
        update();
    }

    private void update() {
        if (adapter != null) {
            adapter.update();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.recycler_view, container, false);
        adapter = new ContentAdapter(getActivity(), items);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        return recyclerView;
    }



    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refreshItems();
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

    private static class ViewHolderboolean extends RecyclerView.ViewHolder {

        TextView primary;
        SwitchCompat state;

        public ViewHolderboolean(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.settings_bool, parent, false));
            primary = (TextView) itemView.findViewById(R.id.settings_bool_primary);
            state = (SwitchCompat) itemView.findViewById(R.id.settings_bool_value);
        }
    }


    private static class ContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final String TAG = "UsersAdapter";
        Activity _activity;
        private List<ListItem> _items;

        public ContentAdapter(Activity activity, List<ListItem> items) {
            this._activity = activity;
            this._items = items;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == ListItem.TYPE_HEADER) {
                return new ViewHolderHeader(LayoutInflater.from(parent.getContext()), parent);
            }
            if (viewType == ListItem.TYPE_BOOLEAN) {
                return new ViewHolderboolean(LayoutInflater.from(parent.getContext()), parent);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {

            int type = getItemViewType(position);

            if (type == ListItem.TYPE_HEADER) {
                Header header = (Header) _items.get(position);
                ViewHolderHeader viewHolder = (ViewHolderHeader) holder;
                viewHolder.primary.setText(header.getText());
            }
            if (type == ListItem.TYPE_BOOLEAN) {
                final SettingsBoolean settingsBoolean = (SettingsBoolean) _items.get(position);
                final ViewHolderboolean viewHolder = (ViewHolderboolean) holder;
                viewHolder.primary.setText(settingsBoolean.getName());

                viewHolder.state.setEnabled(true);
                viewHolder.state.setChecked(settingsBoolean.isState());


                viewHolder.state.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                       if (viewHolder.state.isChecked()) {
                           settingsBoolean.on();
                       } else {
                           settingsBoolean.off();
                       }
                    }
                });
            }
        }

        @Override
        public int getItemViewType(int position) {
            return _items.get(position).getType();
        }

        @Override
        public int getItemCount() {
            return _items.size();
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
    }


    private abstract class ListItem {

        public static final int TYPE_HEADER = 1;
        public static final int TYPE_BOOLEAN = 2;

        abstract public int getType();

    }

    private class Header extends ListItem {
        String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        @Override
        public int getType() {
            return TYPE_HEADER;
        }
    }

    private abstract class SettingsObj extends ListItem {
        private String name;

        public String getName() {
            return this.name;
        };

        public void setName(String name) {
            this.name = name;
        }
    }

    private abstract class SettingsBoolean extends SettingsObj {
        private boolean state;
        abstract public void on();
        abstract public void off();

        public boolean isState() {
            return state;
        }

        public void setState(boolean state) {
            this.state = state;
        }
    }

    private class Notifications extends SettingsBoolean {

        private Activity activity;

        public Notifications(Activity activity) {
            this.activity = activity;
            SharedPreferences preferences = activity.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE);
            setState(preferences.getBoolean(NOTIFICATION_KEY, true));
        }

        @Override
        public void on() {
            setPreference(activity, true);
            Intent intent = new Intent(activity, NotificationService.class);
            activity.startService(intent);
        }

        @Override
        public void off() {
            setPreference(activity, false);
            Intent intent = new Intent(activity, NotificationService.class);
            activity.stopService(intent);
        }

        private void setPreference(Activity activity, boolean value) {
            SharedPreferences.Editor editor = activity.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE).edit();
            editor.putBoolean(NOTIFICATION_KEY, value);
            editor.commit();
        }

        @Override
        public int getType() {
            return TYPE_BOOLEAN;
        }
    }

    private class RpcErrorMsg extends SettingsBoolean {


        private Activity activity;

        public RpcErrorMsg(Activity activity) {
            this.activity = activity;
            SharedPreferences preferences = activity.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE);
            setState(preferences.getBoolean(RPC_ERROR_KEY, true));
        }

        @Override
        public void on() {
            setPreference(activity, true);
        }

        @Override
        public void off() {
            setPreference(activity, false);
        }

        private void setPreference(Activity activity, boolean value) {
            SharedPreferences.Editor editor = activity.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE).edit();
            editor.putBoolean(RPC_ERROR_KEY, value);
            editor.commit();
        }

        @Override
        public int getType() {
            return TYPE_BOOLEAN;
        }
    }
}
