package fi.ct.mist.connect.commissioning;

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

import java.util.ArrayList;
import java.util.List;

import fi.ct.mist.mist.R;
import wish.request.Identity;

/**
 * Created by jeppe on 12/5/16.
 */

public class ListLocalIdentitiesFragment extends Fragment {

    private final String TAG = "ListLocalIdentitiesF";

    private ContentAdapter adapter = null;
    private List<wish.Identity> identities;
    private SwipeRefreshLayout swipeRefreshLayout;

    public ListLocalIdentitiesFragment() {
        identities = new ArrayList<>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setConnections(List<wish.Identity> list) {
        identities = list;
        update();
    }

    private void refreshConnections() {
        identities.clear();
        Identity.list(new Identity.ListCb() {
            @Override
            public void cb(List<wish.Identity> arrayList) {
                for (wish.Identity identity : arrayList) {
                    if (identity.isPrivkey()) {
                        identities.add(identity);
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

    private void update() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if (adapter != null) {
            // FIXME Figure out why this identity update is needed
            // Fixes weird identity list not updating problem
            adapter._identities = identities;

            adapter.update();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        swipeRefreshLayout = (SwipeRefreshLayout) inflater.inflate(R.layout.swipe_view, container, false);
        RecyclerView recyclerView = (RecyclerView) swipeRefreshLayout.findViewById(R.id.recycler_view);

        adapter = new ContentAdapter(getActivity(), identities, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        swipeRefreshLayout.setOnRefreshListener(refreshListener);
        swipeRefreshLayout.setRefreshing(true);
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
        private final String TAG = "ListLocalIdentitiesA";
        Activity _activity;
        private List<wish.Identity> _identities;
        private ListLocalIdentitiesFragment _fragment;

        public ContentAdapter(Activity activity, List<wish.Identity> identities, ListLocalIdentitiesFragment fragment) {
            this._activity = activity;
            this._identities = identities;
            this._fragment = fragment;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            holder.primary.setText(_identities.get(position).getAlias());
            holder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "clicked...");
                    _fragment.swipeRefreshLayout.setRefreshing(true);
                    ListLocalIdentitiesListener listener = (ListLocalIdentitiesListener) _activity;
                    listener.onIdentityClicked(_identities.get(position));
                }
            });
        }

        @Override
        public int getItemCount() {
            return _identities.size();
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

    public interface ListLocalIdentitiesListener {
        public void onIdentityClicked(wish.Identity identity);
    }
}
