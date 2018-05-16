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

import fi.ct.mist.dialogs.DeleteDialog;
import fi.ct.mist.mist.R;
import wish.request.Connection;
import wish.request.Identity;
import mist.api.request.Sandbox;

/**
 * Created by jeppe on 12/5/16.
 */

public class NativeFragment extends Fragment {

    private final String TAG = "AppsFragment";

    private ContentAdapter adapter = null;
    private ArrayList<mist.api.Sandbox> sandboxes;

    private SwipeRefreshLayout swipeRefreshLayout;

    public NativeFragment() {
        sandboxes = new ArrayList<mist.api.Sandbox>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    public void refreshApps() {
        sandboxes.clear();
        Log.d(TAG, "refreshApps");
        Sandbox.list(new Sandbox.ListCb() {
            @Override
            public void cb(List<mist.api.Sandbox> arrayList) {
                for (mist.api.Sandbox sandbox : arrayList) {
                    sandboxes.add(sandbox);
                    update();
                }
                if (arrayList.size() == 0) {
                    update();
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

        adapter = new ContentAdapter(getActivity(), sandboxes);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        swipeRefreshLayout.setOnRefreshListener(refreshListener);
        return swipeRefreshLayout;
    }

    private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
           refreshApps();
        }
    };


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refreshApps();
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
        private final String TAG = "AppsAdapter";
        Activity _activity;
        private ArrayList<mist.api.Sandbox> _sandboxes;

        public ContentAdapter(Activity activity, ArrayList<mist.api.Sandbox> sandboxes) {
            this._activity = activity;
            this._sandboxes = sandboxes;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final mist.api.Sandbox sandbox = _sandboxes.get(position);

            holder.primary.setText(sandbox.getName());

            holder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AdvancedFragmentListener listener = (AdvancedFragmentListener) _activity;
                    listener.openAppSettings(sandbox.getId());
                }
            });

            holder.button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    DeleteDialog.showDialog(_activity, sandbox.getName(), new DeleteDialog.Cb() {
                        @Override
                        public void delete() {
                            Sandbox.remove(sandbox.getId(), new Sandbox.RemoveCb() {
                                @Override
                                public void cb(boolean data) {
                                    Log.d(TAG,"remove: " + data);
                                    _sandboxes.remove(position);
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
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return _sandboxes.size();
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

}
