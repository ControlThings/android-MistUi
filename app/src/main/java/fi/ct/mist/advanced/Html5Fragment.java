package fi.ct.mist.advanced;

import android.app.Activity;
import android.content.Intent;
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
import java.util.Formatter;
import java.util.List;

import fi.ct.mist.dialogs.DeleteDialog;
import fi.ct.mist.mist.R;
import fi.ct.mist.sandbox.CustomWebView;
import mist.api.request.Sandbox;

/**
 * Created by jeppe on 12/5/16.
 */

public class Html5Fragment extends Fragment {

    private final String TAG = "AppsFragment";

    private ContentAdapter adapter = null;
    private ArrayList<Box> sandboxes;

    private SwipeRefreshLayout swipeRefreshLayout;

    public Html5Fragment() {
        sandboxes = new ArrayList<Box>();
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
            public void cb(List<mist.api.Sandbox> list) {
                for (mist.api.Sandbox sandbox : list) {
                    Formatter formatter = new Formatter();
                    for(int i=0;i<20;i++){
                        formatter.format("%02x", sandbox.getId()[i]);
                    }
                    File file = new File(getActivity().getFilesDir(), "/app/" + formatter.toString());
                    if (file.isDirectory()) {
                        Box box = new Box();
                        box.setName(sandbox.getName());
                        box.setId(sandbox.getId());
                        box.setOnline(sandbox.isOnline());
                        box.setPath(file);
                        sandboxes.add(box);
                        update();
                    }
                }
                update();
                if (list.size() == 0) {
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
        private ArrayList<Box> _sandboxes;

        public ContentAdapter(Activity activity, ArrayList<Box> sandboxes) {
            this._activity = activity;
            this._sandboxes = sandboxes;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final Box sandbox = _sandboxes.get(position);

            holder.primary.setText(sandbox.getName());

            holder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent htmlUi = new Intent(_activity, CustomWebView.class);
                    htmlUi.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT );
                    htmlUi.putExtra("sandboxName", _sandboxes.get(position).getName());
                    htmlUi.putExtra("sandboxPath", _sandboxes.get(position).getPath().toString());
                    htmlUi.putExtra("sandboxId", _sandboxes.get(position).getId());
                    _activity.startActivity(htmlUi);
                }
            });

            holder.button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    DeleteDialog.showDialog(_activity, sandbox.getName(), new DeleteDialog.Cb() {
                        @Override
                        public void delete() {
                        Log.d(TAG, "long click");
                            Sandbox.remove(_sandboxes.get(position).getId(), new Sandbox.RemoveCb() {
                                @Override
                                public void cb(boolean b) {
                                    File dir = _sandboxes.get(position).getPath();
                                     if  (dir.isDirectory()) {
                                         String[] children = dir.list();
                                         for (int i = 0; i < children.length; i++)
                                         {
                                             new File(dir, children[i]).delete();
                                         }
                                     }
                                    Html5FragmentListener listener = (Html5FragmentListener) _activity;
                                    listener.onAppRemoved();
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

    private class Box extends mist.api.Sandbox{

        private File path;

        public File getPath() {
            return path;
        }

        public void setPath(File path) {
            this.path = path;
        }
    }

    public interface Html5FragmentListener {
        public void onAppRemoved();
    }

}
