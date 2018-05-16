package fi.ct.mist.advanced;

import android.app.Activity;
import android.nfc.Tag;
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

import java.io.File;
import java.util.ArrayList;

import fi.ct.mist.dialogs.DeleteDialog;
import fi.ct.mist.mist.R;
import mist.api.request.Sandbox;

/**
 * Created by jeppe on 12/5/16.
 */

public class CacheFragment extends Fragment {

    private final String TAG = "AppsFragment";

    private ContentAdapter adapter = null;
    private ArrayList<Ui> uis;

    private SwipeRefreshLayout swipeRefreshLayout;

    public CacheFragment() {
        uis = new ArrayList<>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    public void refreshApps() {
        uis.clear();
        File dir = new File(getActivity().getFilesDir().toString() + "/ui");


        try {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    Ui ui = new Ui();
                    String str = file.toString();
                    ui.setName(str.substring(str.lastIndexOf("/") + 1));
                    ui.setDir(file);
                    uis.add(ui);
                    update();
                }
            }
            update();
        } catch (NullPointerException e) {
            update();
        }
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

        adapter = new ContentAdapter(getActivity(), uis);
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
        private ArrayList<Ui> _uis;

        public ContentAdapter(Activity activity, ArrayList<Ui> uis) {
            this._activity = activity;
            this._uis = uis;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final Ui ui = _uis.get(position);

            holder.primary.setText(ui.getName());

            holder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                }
            });

            holder.button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    DeleteDialog.showDialog(_activity, ui.getName(), new DeleteDialog.Cb() {
                        @Override
                        public void delete() {
                            deleteRecursive(_uis.get(position).getDir());
                            i = 0;
                        }
                    });
                    return true;
                }
            });
        }

        private  int i;
        private void deleteRecursive(File fileOrDirectory) {
            i++;
            if (fileOrDirectory.isDirectory()) {
                for (File child : fileOrDirectory.listFiles()) {
                    deleteRecursive(child);
                }
            }
            fileOrDirectory.delete();
            i--;
            if (i == 0){
                UiFragmentListener listener = (UiFragmentListener) _activity;
                listener.onUiDeleted();
            }
        }

        @Override
        public int getItemCount() {
            return _uis.size();
        }


        public void update() {
            _activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
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

    public interface UiFragmentListener{
        public void onUiDeleted();
    }

    private class Ui {

        String name;
        File dir;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public File getDir() {
            return dir;
        }

        public void setDir(File dir) {
            this.dir = dir;
        }
    }

}
