package fi.ct.mist.endpoint;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.bson.BsonDocument;
import org.bson.RawBsonDocument;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import fi.ct.mist.mist.R;
import wish.Peer;
import mist.node.request.Control;

/**
 * Created by jeppe on 12/5/16.
 */

public class MappingFragment extends Fragment {

    private final String TAG = "MappingFragment";

    private final static int typeInteger = 1;
    private final static int typeString = 2;
    private final static int typeBoolean = 3;
    private final static int typeFloat = 4;

    private ContentAdapter adapter = null;
    private ArrayList<Mapping> mappings;
    private Peer peer;
    private String endpoint;
    private JSONObject model = null;


    public MappingFragment() {
        mappings = new ArrayList<Mapping>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private int addEndpoint(Mapping mapping) {
        mappings.add(mapping);
        return (mappings.size() - 1);
    }

    private Mapping getMapping(int position) {
        return mappings.get(position);
    }


    public void refreshModel() {
        mappings.clear();
        getModel();
    }

    public void setMappings(Peer p, String endpoint) {
        this.peer = p;
        this.endpoint = endpoint;
        if (model == null) {
            getModel();
        }
    }

    private void update() {
        if (adapter != null) {
            adapter.update();
        }
    }

    private void getModel() {
        Control.model(peer, new Control.ModelCb() {
            @Override
            public void cb(byte[] dataBson) {
                JSONObject jsonObject;
                try {
                    BsonDocument bsonDocument = new RawBsonDocument(dataBson);
                    model = new JSONObject(bsonDocument.toJson());
                    parseModel();
                } catch (JSONException e) {
                    Log.d(TAG, "Json parsing error: " + e);
                    return;
                }
            }
        });
    }
/*
    "state": {
        "label": "Switch",
                "type": "bool",
                "mappings": {
            "m1": {
                "endpoint": {
                    "url": "wish://e56eff47801e84afdfc677be3346c7ebce0b23d867aee2804b2aa1b9849e1bec>e1adcb2d9fee348e64340d35e7b0574aa3ce649d60afdaf60c5b789c8fc728cc@d0a378c281bbe727248b45af4771ef881393368e085a5a5bd379c6496dde0981/536f6e6f66662053323000000000000000000000000000000000000000000000",
                            "epid": "state"
                },
                "opts": {
                    "type": "write"
                }
            }
        },
  */

    private void parseModel() {
        try {
            for (Iterator<String> iter = model.keys(); iter.hasNext(); ) {
                String key = iter.next();
                if (key.equals("model")) {
                    JSONObject endpoints = model.getJSONObject(key);
                    for (Iterator<String> it = endpoints.keys(); it.hasNext(); ) {
                        String ep = it.next();
                        if (ep.equals(endpoint)) {
                            JSONObject jsonEndpoint = endpoints.getJSONObject(ep);
                            for (Iterator<String> mp = jsonEndpoint.keys(); mp.hasNext(); ) {
                                String mapp = mp.next();
                                if (mapp.equals("mappings")) {
                                    JSONObject jsonMapps = jsonEndpoint.getJSONObject(mapp);
                                    for (Iterator<String> mappIt = jsonMapps.keys(); mappIt.hasNext(); ) {
                                        Mapping mapping = new Mapping();
                                        String mappEp = mappIt.next();
                                        JSONObject jsonMappEp = jsonMapps.getJSONObject(mappEp);
                                        mapping.setUrl(jsonMappEp.getJSONObject("endpoint").getString("url"));
                                        mapping.setEpid(jsonMappEp.getJSONObject("endpoint").getString("epid"));
                                        mapping.setType(jsonMappEp.getJSONObject("opts").getString("type"));
                                        mappings.add(mapping);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            update();

        } catch (JSONException e) {
            Log.d(TAG, "Error parsing json model: " + e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        Log.d(TAG, "onCreateView");
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.recycler_view, container, false);
        adapter = new ContentAdapter(getActivity(), mappings);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        return recyclerView;
    }


    private static class ViewHolder extends RecyclerView.ViewHolder {

        TextView primary;

        public ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.one_line_list, parent, false));
            primary = (TextView) itemView.findViewById(R.id.one_line_primary);
        }
    }

    private static class ContentAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final String TAG = "MappingAdapter";


        Activity _activity;
        private ArrayList<Mapping> _mappings;


        public ContentAdapter(Activity activity, ArrayList<Mapping> mappings) {
            this._activity = activity;
            this._mappings = mappings;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            String ep = _mappings.get(position).getEpid();
            holder.primary.setText(ep);
        }

        @Override
        public int getItemCount() {
            return _mappings.size();
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

}
