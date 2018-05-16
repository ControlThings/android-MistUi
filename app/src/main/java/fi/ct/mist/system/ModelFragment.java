package fi.ct.mist.system;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.SwitchCompat;
import android.text.Layout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.bson.BsonDocument;
import org.bson.RawBsonDocument;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import fi.ct.mist.mist.R;
import fi.ct.mist.dialogs.MappingsDialog;
import fi.ct.mist.main.Endpoint;
import wish.Peer;
import mist.node.request.Control;
import mist.api.request.Mist;

/**
 * Created by jeppe on 12/5/16.
 */

public class ModelFragment extends Fragment {

    private final String TAG = "ModelFragment";

    private final static int typeInteger = 1;
    private final static int typeString = 2;
    private final static int typeBoolean = 3;
    private final static int typeFloat = 4;
    private final static int typeObject = 5;

    private ContentAdapter adapter = null;
    private ArrayList<Endpoint> endpoints;
    private HashMap<String, Integer> positions;
    private ArrayList<EndpiontState> endpiontStates;
    private Peer peer;
    private JSONObject model = null;

    private List<Integer> followids = new ArrayList<>();
    private boolean started = false;

    private SwipeRefreshLayout swipeRefreshLayout;

    public ModelFragment() {
        endpoints = new ArrayList<Endpoint>();
        positions = new HashMap<String, Integer>();
        endpiontStates = new ArrayList<EndpiontState>();
    }

    private int addEndpoint(Endpoint endpoint) {
        endpoints.add(endpoint);
        endpiontStates.add(new EndpiontState());
        return (endpoints.size() - 1);
    }

    private Endpoint getEndpoint(int position) {
        return endpoints.get(position);
    }

    private void setModelOffline() {
        for (Endpoint endpoint : endpoints) {
            endpoint.setOnline(false);
        }
        update();
    }

    private void setModelOnline() {
        for (Endpoint endpoint : endpoints) {
            endpoint.setOnline(true);
        }
        update();
    }


    public void refreshModelFromTree(Peer p,String name, JSONObject endpoint) {
        this.peer = p;
        parseEndpiont(name, endpoint);

        if (peer.isOnline()) {
            setModelOnline();
        } else {
            setModelOffline();
        }
    }

    public void refreshModel(Peer p,String name, JSONObject endpoint) {
        this.peer = p;
        if (model == null) {
            endpoints.clear();
            endpiontStates.clear();
            parseEndpiont(name, endpoint);
        } else {
            if (peer.isOnline()) {
                setModelOnline();
            } else {
                setModelOffline();
            }
        }
    }


    public void refreshModel(Peer p) {
        this.peer = p;
        if (model == null) {
            endpoints.clear();
            endpiontStates.clear();
            getModel();
        } else {
            if (peer.isOnline()) {
                setModelOnline();
            } else {
                setModelOffline();
            }
        }
    }

    private void update() {
        if (adapter != null) {
            adapter.update(peer);
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void update(int position) {
        if (adapter != null) {
            adapter.update(position, peer);
        }
    }

    private void getModel() {
        Control.model(peer, new Control.ModelCb() {
            @Override
            public void cb(byte[] dataBson) {
                update();
                try {
                    BsonDocument bsonDocument = new RawBsonDocument(dataBson);
                    model = new JSONObject(bsonDocument.toJson());
                } catch (JSONException e) {
                    Log.d(TAG, "Json parsing error: " + e);
                    return;
                }
                parseModel();
            }
        });
    }

    private void parseEndpiont(String name, JSONObject jsonEndpoint) {
        try {
            Log.d(TAG, "endpoint: " + jsonEndpoint.toString());

            if (name.equals("directory") || name.equals("context")) {
            } else {
                Endpoint endpoint = new Endpoint();
                endpoint.setName(name);
                endpoint.setLabel(jsonEndpoint.getString("label"));
                endpoint.setType(jsonEndpoint.getString("type"));
                if (jsonEndpoint.has("#")) {
                    endpoint.setObject(jsonEndpoint.getJSONObject("#"));
                } else {
                    if (jsonEndpoint.has("read")) {
                        endpoint.setRead(jsonEndpoint.getBoolean("read"));
                    }
                    if (jsonEndpoint.has("write")) {
                        endpoint.setWrite(jsonEndpoint.getBoolean("write"));
                    }
                    if (jsonEndpoint.has("invoke")) {
                        endpoint.setInvoke(jsonEndpoint.getBoolean("invoke"));
                    }
                }
                int position = addEndpoint(endpoint);
                positions.put(name, position);
            }

            update();
            started = true;
        } catch (JSONException e) {
            Log.d(TAG, "Error parsing json model: " + e);
        }
    }

    private void parseModel() {
        try {
            for (Iterator<String> iter = model.keys(); iter.hasNext(); ) {
                String key = iter.next();
                if (!key.equals("mist")) {
                    JSONObject endpoint = model.getJSONObject(key);
                    parseEndpiont(key, endpoint);
                }
            }
            follow();

        } catch (JSONException e) {
            Log.d(TAG, "Error parsing json model: " + e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        swipeRefreshLayout = (SwipeRefreshLayout) inflater.inflate(R.layout.swipe_view, container, false);
        RecyclerView recyclerView = (RecyclerView) swipeRefreshLayout.findViewById(R.id.recycler_view);

        // RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.recycler_view, container, false);
        adapter = new ContentAdapter(getActivity(), endpoints, endpiontStates);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        //removes flickering
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }


        swipeRefreshLayout.setOnRefreshListener(refreshListener);
        return swipeRefreshLayout;
    }

    private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            model = null;
            for (int id : followids) {
                Mist.cancel(id);
            }
            refreshModel(peer);
        }
    };


    private void follow() {
        int followId = Control.follow(peer, new Control.FollowCb() {
            @Override
            public void cbBool(String s, boolean b) {
                try {
                    if (positions.containsKey(s)) {
                        getEndpoint(positions.get(s)).setBooleanValue(b);
                        update(positions.get(s));
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Error: " + Log.getStackTraceString(e));
                }
            }

            @Override
            public void cbInt(String s, int i) {
                try {
                    Log.d(TAG, "int follow");
                    if (positions.containsKey(s)) {
                        getEndpoint(positions.get(s)).setIntegerValue(i);
                        update(positions.get(s));
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Error: " + Log.getStackTraceString(e));
                }
            }

            @Override
            public void cbFloat(String s, double v) {
                try {
                    if (positions.containsKey(s)) {
                        getEndpoint(positions.get(s)).setFloatValue(v);
                        update(positions.get(s));
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Error: " + Log.getStackTraceString(e));
                }
            }

            @Override
            public void cbString(String s, String s1) {
                try {
                    Log.d(TAG, "str follow");
                    if (positions.containsKey(s)) {
                        getEndpoint(positions.get(s)).setStringValue(s1);
                        update(positions.get(s));
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Error: " + Log.getStackTraceString(e));
                }
            }

            @Override
            public void err(int i, String s) {
            }

            @Override
            public void end() {
            }
        });

        followids.add(followId);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (started) {
            follow();
        }
    }

    public static class ViewHolderFloat extends RecyclerView.ViewHolder {

        TextView primary;
        RelativeLayout button;
        ImageView expand;
        TextView value;
        TextView valueNot;
        LinearLayout area;
        TextView text;

        public ViewHolderFloat(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_expandable, parent, false));
            primary = (TextView) itemView.findViewById(R.id.list_item_expandable_title);
            button = (RelativeLayout) itemView.findViewById(R.id.list_item_expandable_click);
            expand = (ImageView) itemView.findViewById(R.id.list_item_expandable_right_icon);
            area = (LinearLayout) itemView.findViewById(R.id.list_item_expandable_expand_area);
            value = (TextView) itemView.findViewById(R.id.list_item_expandable_value);
            valueNot = (TextView) itemView.findViewById(R.id.list_item_expandable_value_not_write);

            area.addView(inflater.inflate(R.layout.item_float, null));
            text = (TextView) itemView.findViewById(R.id.item_float_text);
        }
    }

    public static class ViewHolderInt extends RecyclerView.ViewHolder {

        TextView primary;
        RelativeLayout button;
        ImageView expand;
        TextView value;
        TextView valueNot;
        LinearLayout area;
        TextView text;

        public ViewHolderInt(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_expandable, parent, false));
            primary = (TextView) itemView.findViewById(R.id.list_item_expandable_title);
            button = (RelativeLayout) itemView.findViewById(R.id.list_item_expandable_click);
            expand = (ImageView) itemView.findViewById(R.id.list_item_expandable_right_icon);
            area = (LinearLayout) itemView.findViewById(R.id.list_item_expandable_expand_area);
            value = (TextView) itemView.findViewById(R.id.list_item_expandable_value);
            valueNot = (TextView) itemView.findViewById(R.id.list_item_expandable_value_not_write);

            area.addView(inflater.inflate(R.layout.item_int, null));
            text = (TextView) itemView.findViewById(R.id.item_int_text);
        }
    }

    public static class ViewHolderString extends RecyclerView.ViewHolder {

        TextView primary;
        RelativeLayout button;
        ImageView expand;
        TextView value;
        TextView valueNot;
        LinearLayout area;
        TextView text;

        public ViewHolderString(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_expandable, parent, false));
            primary = (TextView) itemView.findViewById(R.id.list_item_expandable_title);
            button = (RelativeLayout) itemView.findViewById(R.id.list_item_expandable_click);
            expand = (ImageView) itemView.findViewById(R.id.list_item_expandable_right_icon);
            area = (LinearLayout) itemView.findViewById(R.id.list_item_expandable_expand_area);
            value = (TextView) itemView.findViewById(R.id.list_item_expandable_value);
            valueNot = (TextView) itemView.findViewById(R.id.list_item_expandable_value_not_write);

            area.addView(inflater.inflate(R.layout.item_string, null));
            text = (TextView) itemView.findViewById(R.id.item_string_text);
        }
    }

    public static class ViewHolderBool extends RecyclerView.ViewHolder {

        TextView primary;
        TextView secondary;
        SwitchCompat value;
        LinearLayout button;

        public ViewHolderBool(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_bool, parent, false));
            primary = (TextView) itemView.findViewById(R.id.list_item_bool_primary);
            secondary = (TextView) itemView.findViewById(R.id.list_item_bool_secondary);
            value = (SwitchCompat) itemView.findViewById(R.id.list_item_bool_value);
            button = (LinearLayout) itemView.findViewById(R.id.list_item_bool_button);
        }
    }

    public static class ViewHolderEmpty extends RecyclerView.ViewHolder {

        TextView primary;
        RelativeLayout button;
        ImageView expand;

        public ViewHolderEmpty(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_expandable, parent, false));
            primary = (TextView) itemView.findViewById(R.id.list_item_expandable_title);
            button = (RelativeLayout) itemView.findViewById(R.id.list_item_expandable_click);
            expand = (ImageView) itemView.findViewById(R.id.list_item_expandable_right_icon);
        }
    }

    public static class ContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final String TAG = "ModelAdapter";

        private Peer _peer;
        Activity _activity;
        private ArrayList<Endpoint> _endpoints;
        private ArrayList<EndpiontState> _endpiontStates;

        public ContentAdapter(Activity activity, ArrayList<Endpoint> endpoints, ArrayList<EndpiontState> endpiontStates) {
            this._activity = activity;
            this._endpoints = endpoints;
            this._endpiontStates = endpiontStates;
        }

        @Override
        public int getItemViewType(int position) {
            String op = _endpoints.get(position).getType();

            if (op.equals("int")) {
                return typeInteger;
            } else if (op.equals("string")) {
                return typeString;
            } else if (op.equals("float")) {
                return typeFloat;
            } else if (op.equals("bool")) {
                return typeBoolean;
            } else if (op.equals("object")) {
                return typeObject;
            } else {
                Log.d(TAG, "unknown type: " + op);
                return 0;
            }


        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Log.d(TAG, "onCreateViewHolder " + viewType);

            if (viewType == typeInteger) {
                return new ViewHolderInt(LayoutInflater.from(parent.getContext()), parent);
            } else if (viewType == typeString) {
                return new ViewHolderString(LayoutInflater.from(parent.getContext()), parent);
            } else if (viewType == typeFloat) {
                return new ViewHolderFloat(LayoutInflater.from(parent.getContext()), parent);
            } else if (viewType == typeBoolean) {
                return new ViewHolderBool(LayoutInflater.from(parent.getContext()), parent);
            } else if (viewType == typeObject) {
                return new ViewHolderEmpty(LayoutInflater.from(parent.getContext()), parent);
            } else {
                return new ViewHolderEmpty(LayoutInflater.from(parent.getContext()), parent);
            }
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {


            String op = _endpoints.get(position).getType();
            Log.d(TAG, "onBindViewHolder " + position + " op " + op);

            if (op.equals("int")) {
                final ViewHolderInt holderInt = (ViewHolderInt) holder;
                holderInt.primary.setText(_endpoints.get(position).getLabel());


                if (_endpoints.get(position).getIntegerValue() != null) {
                    holderInt.value.setVisibility(View.VISIBLE);
                    holderInt.value.setText(String.valueOf(_endpoints.get(position).getIntegerValue()));
                }

                if (_endpiontStates.get(position).getInputvalue() != null) {
                    //holderInt.text.setHint(_endpiontStates.get(position).getInputvalue());
                    holderInt.text.setText("");
                }
                if (!_endpoints.get(position).isWrite()) {
                    holderInt.expand.setVisibility(View.INVISIBLE);
                    holderInt.value.setVisibility(View.INVISIBLE);
                    holderInt.valueNot.setVisibility(View.VISIBLE);
                    holderInt.valueNot.setText(String.valueOf(_endpoints.get(position).getIntegerValue()));
                } else {
                    if (_endpiontStates.get(position).isExpanded()) {
                        holderInt.area.setVisibility(View.VISIBLE);
                        holderInt.expand.setImageResource(R.drawable.ic_expand_less_black_24dp);
                    } else {
                        holderInt.area.setVisibility(View.GONE);
                        holderInt.expand.setImageResource(R.drawable.ic_expand_more_black_24dp);
                    }

                    holderInt.expand.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            EndpiontState endpiontState = _endpiontStates.get(position);
                            endpiontState.setExpanded(endpiontState.isExpanded() ? false : true);
                            notifyItemChanged(position);
                        }
                    });

                    holderInt.text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                            if (i == EditorInfo.IME_ACTION_DONE) {

                                final int value = Integer.parseInt(textView.getText().toString());
                                _endpiontStates.get(position).setInputvalue(String.valueOf(value));
                                Control.write(_peer, _endpoints.get(position).getName(), value, new Control.WriteCb() {
                                    @Override
                                    public void cb() {}
                                });
                                _endpiontStates.get(position).setExpanded(false);
                                notifyItemChanged(position);
                            }
                            return false;
                        }
                    });
                }
                /*
                if (_endpoints.get(position).getObject() != null) {
                    holderInt.button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (_endpoints.get(position).isOnline()) {
                                SystemsFragmentListener listener = (SystemsFragmentListener) _activity;
                                listener.openObject(_endpoints.get(position));
                            } else {
                                Snackbar snackbar = Snackbar
                                        .make(_activity.findViewById(R.id.coordinatorLayout), _endpoints.get(position) + " is offline", Snackbar.LENGTH_LONG);

                                snackbar.show();
                            }
                        }
                    });
                } else {

                    holderInt.button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (_endpoints.get(position).isOnline()) {
                                SystemsFragmentListener listener = (SystemsFragmentListener) _activity;
                                listener.openEndpoint(_endpoints.get(position));
                                //listener.openEndpoint(_endpoints.get(position).getName(), _endpoints.get(position).getLabel());
                            } else {
                                Snackbar snackbar = Snackbar
                                        .make(_activity.findViewById(R.id.coordinatorLayout), _endpoints.get(position) + " is offline", Snackbar.LENGTH_LONG);

                                snackbar.show();
                            }
                        }
                    });
                } */

            } else if (op.equals("string")) {
                final ViewHolderString holderString = (ViewHolderString) holder;
                holderString.primary.setText(_endpoints.get(position).getLabel());

                if (_endpoints.get(position).getStringValue() != null) {
                    holderString.value.setVisibility(View.VISIBLE);
                    holderString.value.setText(_endpoints.get(position).getStringValue());
                }

                if (_endpiontStates.get(position).getInputvalue() != null) {
                    //holderInt.text.setHint(_endpiontStates.get(position).getInputvalue());
                    holderString.text.setText("");
                }
                if (!_endpoints.get(position).isWrite()) {
                    holderString.expand.setVisibility(View.INVISIBLE);
                    holderString.value.setVisibility(View.INVISIBLE);
                    holderString.valueNot.setVisibility(View.VISIBLE);
                    holderString.valueNot.setText(_endpoints.get(position).getStringValue());
                } else {
                    if (_endpiontStates.get(position).isExpanded()) {
                        holderString.area.setVisibility(View.VISIBLE);
                        holderString.expand.setImageResource(R.drawable.ic_expand_less_black_24dp);
                    } else {
                        holderString.area.setVisibility(View.GONE);
                        holderString.expand.setImageResource(R.drawable.ic_expand_more_black_24dp);
                    }

                    holderString.expand.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            EndpiontState endpiontState = _endpiontStates.get(position);
                            endpiontState.setExpanded(endpiontState.isExpanded() ? false : true);
                            notifyItemChanged(position);
                        }
                    });

                    holderString.text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                            if (i == EditorInfo.IME_ACTION_DONE) {

                                final String value = textView.getText().toString();
                                _endpiontStates.get(position).setInputvalue(String.valueOf(value));
                                Control.write(_peer, _endpoints.get(position).getName(), value, new Control.WriteCb() {
                                    @Override
                                    public void cb() {}
                                });
                                _endpiontStates.get(position).setExpanded(false);
                                notifyItemChanged(position);
                            }
                            return false;
                        }
                    });
                }/*
                if (_endpoints.get(position).getObject() != null) {
                    holderString.button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (_endpoints.get(position).isOnline()) {
                                SystemsFragmentListener listener = (SystemsFragmentListener) _activity;
                                listener.openObject(_endpoints.get(position));
                            } else {
                                Snackbar snackbar = Snackbar
                                        .make(_activity.findViewById(R.id.coordinatorLayout), _endpoints.get(position) + " is offline", Snackbar.LENGTH_LONG);

                                snackbar.show();
                            }
                        }
                    });
                } else {
                    holderString.button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (_endpoints.get(position).isOnline()) {
                                SystemsFragmentListener listener = (SystemsFragmentListener) _activity;
                                listener.openEndpoint(_endpoints.get(position));
                                //listener.openEndpoint(_endpoints.get(position).getName(), _endpoints.get(position).getLabel());
                            } else {
                                Snackbar snackbar = Snackbar
                                        .make(_activity.findViewById(R.id.coordinatorLayout), _endpoints.get(position) + " is offline", Snackbar.LENGTH_LONG);

                                snackbar.show();
                            }
                        }
                    });
                }*/
            } else if (op.equals("float")) {
                final ViewHolderFloat holderFloat = (ViewHolderFloat) holder;
                holderFloat.primary.setText(_endpoints.get(position).getLabel());
                if (_endpoints.get(position).getFloatValue() != null) {
                    holderFloat.value.setVisibility(View.VISIBLE);
                    holderFloat.value.setText(String.format("%.4f", _endpoints.get(position).getFloatValue()));
                }

                if (_endpiontStates.get(position).getInputvalue() != null) {
                    //holderInt.text.setHint(_endpiontStates.get(position).getInputvalue());
                    holderFloat.text.setText("");
                }
                if (!_endpoints.get(position).isWrite()) {
                    holderFloat.expand.setVisibility(View.INVISIBLE);
                    holderFloat.value.setVisibility(View.INVISIBLE);
                    holderFloat.valueNot.setVisibility(View.VISIBLE);
                    holderFloat.valueNot.setText(String.format("%.4f", _endpoints.get(position).getFloatValue()));
                } else {
                    if (_endpiontStates.get(position).isExpanded()) {
                        holderFloat.area.setVisibility(View.VISIBLE);
                        holderFloat.expand.setImageResource(R.drawable.ic_expand_less_black_24dp);
                    } else {
                        holderFloat.area.setVisibility(View.GONE);
                        holderFloat.expand.setImageResource(R.drawable.ic_expand_more_black_24dp);
                    }

                    holderFloat.expand.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            EndpiontState endpiontState = _endpiontStates.get(position);
                            endpiontState.setExpanded(endpiontState.isExpanded() ? false : true);
                            notifyItemChanged(position);
                        }
                    });

                    holderFloat.text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                            if (i == EditorInfo.IME_ACTION_DONE) {

                                final float value = Float.parseFloat(textView.getText().toString());
                                _endpiontStates.get(position).setInputvalue(String.valueOf(value));
                                Control.write(_peer, _endpoints.get(position).getName(), value, new Control.WriteCb() {
                                    @Override
                                    public void cb() {}
                                });
                                _endpiontStates.get(position).setExpanded(false);
                                notifyItemChanged(position);
                            }
                            return false;
                        }
                    });
                }
/*
                if (_endpoints.get(position).getObject() != null) {
                    holderFloat.button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (_endpoints.get(position).isOnline()) {
                                SystemsFragmentListener listener = (SystemsFragmentListener) _activity;
                                listener.openObject(_endpoints.get(position));
                            } else {
                                Snackbar snackbar = Snackbar
                                        .make(_activity.findViewById(R.id.coordinatorLayout), _endpoints.get(position) + " is offline", Snackbar.LENGTH_LONG);

                                snackbar.show();
                            }
                        }
                    });
                } else {
                    holderFloat.button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (_endpoints.get(position).isOnline()) {
                                SystemsFragmentListener listener = (SystemsFragmentListener) _activity;
                                listener.openEndpoint(_endpoints.get(position));
                                //listener.openEndpoint(_endpoints.get(position).getName(), _endpoints.get(position).getLabel());
                            } else {
                                Snackbar snackbar = Snackbar
                                        .make(_activity.findViewById(R.id.coordinatorLayout), _endpoints.get(position) + " is offline", Snackbar.LENGTH_LONG);

                                snackbar.show();
                            }
                        }
                    });
                }
*/
            } else if (op.equals("bool")) {
                final ViewHolderBool holderBool = (ViewHolderBool) holder;
                holderBool.primary.setText(_endpoints.get(position).getLabel());
                holderBool.secondary.setText(_endpoints.get(position).getName());
                if (_endpoints.get(position).getBooleanValue() != null) {
                    holderBool.value.setVisibility(View.VISIBLE);
                    holderBool.value.setChecked(_endpoints.get(position).getBooleanValue());
                    holderBool.value.setEnabled(_endpoints.get(position).isOnline());
                    holderBool.value.setEnabled(_endpoints.get(position).isWrite());
                    holderBool.value.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            /* When clicked, first disable control, and re-enable when callback activates. */
                            if (holderBool.value.isEnabled()) {
                                holderBool.value.setEnabled(false);
                                Control.write(_peer, _endpoints.get(position).getName(), holderBool.value.isChecked(), new Control.WriteCb() {
                                    @Override
                                    public void cb() {
                                    /* Re-enable    control */
                                        holderBool.value.setEnabled(true);
                                    }
                                });
                            }

                        }
                    });
/*
                    if (_endpoints.get(position).getObject() != null) {
                        holderBool.button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (_endpoints.get(position).isOnline()) {
                                    SystemsFragmentListener listener = (SystemsFragmentListener) _activity;
                                    listener.openObject(_endpoints.get(position));
                                } else {
                                    Snackbar snackbar = Snackbar
                                            .make(_activity.findViewById(R.id.coordinatorLayout), _endpoints.get(position) + " is offline", Snackbar.LENGTH_LONG);

                                    snackbar.show();
                                }
                            }
                        });
                    } else {
                        holderBool.button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (_endpoints.get(position).isOnline()) {
                                    SystemsFragmentListener listener = (SystemsFragmentListener) _activity;
                                    listener.openEndpoint(_endpoints.get(position));
                                    //listener.openEndpoint(_endpoints.get(position).getName(), _endpoints.get(position).getLabel());
                                } else {
                                    Snackbar snackbar = Snackbar
                                            .make(_activity.findViewById(R.id.coordinatorLayout), _endpoints.get(position) + " is offline", Snackbar.LENGTH_LONG);

                                    snackbar.show();
                                }
                            }
                        });
                    }
                    */
                }


            } else {
                final ViewHolderEmpty holderEmpty = (ViewHolderEmpty) holder;
                holderEmpty.primary.setText(_endpoints.get(position).getLabel());
                holderEmpty.expand.setVisibility(View.GONE);
/*
                if (_endpoints.get(position).getObject() != null) {
                    holderEmpty.button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (_endpoints.get(position).isOnline()) {
                                SystemsFragmentListener listener = (SystemsFragmentListener) _activity;
                                listener.openObject(_endpoints.get(position));
                            } else {
                                Snackbar snackbar = Snackbar
                                        .make(_activity.findViewById(R.id.coordinatorLayout), _endpoints.get(position) + " is offline", Snackbar.LENGTH_LONG);

                                snackbar.show();
                            }
                        }
                    });
                } else {
                    holderEmpty.button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (_endpoints.get(position).isOnline()) {
                                SystemsFragmentListener listener = (SystemsFragmentListener) _activity;
                                listener.openEndpoint(_endpoints.get(position));
                                //listener.openEndpoint(_endpoints.get(position).getName(), _endpoints.get(position).getLabel());
                            } else {
                                Snackbar snackbar = Snackbar
                                        .make(_activity.findViewById(R.id.coordinatorLayout), _endpoints.get(position) + " is offline", Snackbar.LENGTH_LONG);

                                snackbar.show();
                            }
                        }
                    });
                }
*/
            }
        }

        @Override
        public int getItemCount() {
            return _endpoints.size();
        }


        public void update(Peer peer) {
            this._peer = peer;
            _activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "update");
                    notifyDataSetChanged();
                }
            });
        }

        public void update(final int position, Peer peer) {
            this._peer = peer;
            _activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "update");
                    notifyItemChanged(position);
                }
            });
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        swipeRefreshLayout.removeAllViews();
        for (int id : followids) {
            if (id != 0) {
                Log.d(TAG, "onStop (canceling follow with id: " + id + ")");
                Mist.cancel(id);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Control.cancel(followId);
    }

    class EndpiontState {
        private boolean expanded = false;
        private String inputvalue = null;

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
        }

        public String getInputvalue() {
            return inputvalue;
        }

        public void setInputvalue(String inputvalue) {
            this.inputvalue = inputvalue;
        }
    }

}
