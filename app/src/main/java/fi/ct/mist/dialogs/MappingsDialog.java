package fi.ct.mist.dialogs;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.bson.BsonDocument;
import org.bson.RawBsonDocument;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fi.ct.mist.main.Endpoint;
import fi.ct.mist.mist.R;
import fi.ct.mist.endpoint.EndpointFragmentListener;
import wish.Peer;
import mist.node.request.Control;
import wish.request.Identity;
import mist.api.request.Mist;

/**
 * Created by jeppe on 18/12/2016.
 */

public class MappingsDialog extends DialogFragment {

    public static MappingsDialog newInstance(Endpoint endpoint) {

        MappingsDialog fragment = new MappingsDialog();

        Bundle args = new Bundle();
        args.putSerializable("endpoint", endpoint);

        fragment.setArguments(args);
        fragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.MappingsDialog);

        return fragment;
    }

    private ArrayList<DialogSystem> dialogSystems;

    public MappingsDialog() {
        dialogSystems = new ArrayList<DialogSystem>();
    }

    ContentAdapter adapter;

    private void listServices() {
        Mist.listServices(new Mist.ListServicesCb() {
            @Override
            public void cb(List<Peer> arrayList) {
                for (Peer peer : arrayList) {
                    if (peer.isOnline()) {
                        getInfo(peer);
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

    private void getInfo(final Peer peer) {

        Control.read(peer, "mist.name", new Control.ReadCb() {
            DialogSystem system = new DialogSystem();
            @Override
            public void cbString(String name) {
                system.setPeer(peer);
                system.setName(name);
                Identity.get(peer.getRuid(), new Identity.GetCb() {
                    @Override
                    public void cb(wish.Identity mistIdentity) {
                        system.setAlias(mistIdentity.getAlias());
                        dialogSystems.add(system);
                        adapter.update();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Endpoint endpoint = (Endpoint) getArguments().getSerializable("endpoint");

        String dialogTitle = getResources().getString(R.string.mappings_dialog_title, endpoint.getLabel());
        getDialog().setTitle(dialogTitle); /* Note: you need to add have item "android:windowNoTitle" set to false for MappingsFragment in styles.xml for title to be shown! */
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.recycler_view, container, false);
        adapter = new ContentAdapter(this, getActivity(), dialogSystems, endpoint);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return recyclerView;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listServices();
    }

    public static class ViewHolderOneLine extends RecyclerView.ViewHolder {
        TextView primary;
        LinearLayout button;

        public ViewHolderOneLine(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.one_line_list, parent, false));
            primary = (TextView) itemView.findViewById(R.id.one_line_primary);
            button = (LinearLayout) itemView.findViewById(R.id.one_line_layout);
        }
    }

    private static class ViewHolderTwoLine extends RecyclerView.ViewHolder {

        TextView primary;
        TextView secondary;
        LinearLayout button;

        public ViewHolderTwoLine(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.two_line_list, parent, false));
            primary = (TextView) itemView.findViewById(R.id.two_line_primary);
            secondary = (TextView) itemView.findViewById(R.id.two_line_secondary);
            button = (LinearLayout) itemView.findViewById(R.id.two_line_layout);
        }

    }

    private static class ContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final String TAG = "MappingsAdapter";
        Activity _activity;
        private ArrayList<DialogSystem> _systems;
        private ArrayList<DialogEndpoint> endpoints;
        private DialogFragment _fragment;
        private boolean systemSelected;
        private Endpoint _endpoint;


        public ContentAdapter(DialogFragment fragment, Activity activity, ArrayList<DialogSystem> dialogSystems, Endpoint endpoint) {
            this._fragment = fragment;
            this._activity = activity;
            this._systems = dialogSystems;
            this._endpoint = endpoint;
            this.endpoints = new ArrayList<DialogEndpoint>();
            systemSelected = false;
        }


        @Override
        public int getItemViewType(int position) {
            if (systemSelected) {
                return 0;
            }
            return 1;
        }

        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == 1) {
                return new ViewHolderTwoLine(LayoutInflater.from(parent.getContext()), parent);
            }
            return new ViewHolderOneLine(LayoutInflater.from(parent.getContext()), parent);
        }


        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder vHolder, final int position) {
            if (systemSelected) {
                final String ep = endpoints.get(position).getEndpoint();
                final ViewHolderOneLine holderOneLine = (ViewHolderOneLine) vHolder;
                holderOneLine.primary.setText(ep);
                if (endpoints.get(position).isReadable() && endpoints.get(position).getType().equals(_endpoint.getType())) {
                    holderOneLine.button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            EndpointFragmentListener listener = (EndpointFragmentListener) _activity;
                            listener.setMapping(endpoints.get(position).getPeer(), ep);
                            _fragment.dismiss();
                        }
                    });
                } else {
                    holderOneLine.primary.setTextColor(ContextCompat.getColor(_activity, R.color.ctTextColorInactive));
                }


            } else {
                Log.d(TAG, "create:");
                String dev = _systems.get(position).getName();
                String alias = _systems.get(position).getAlias();
                final ViewHolderTwoLine holder = (ViewHolderTwoLine) vHolder;
                holder.primary.setText(dev);
                holder.secondary.setText(alias);
                holder.button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        systemSelected = true;
                        parseModel(position);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            if (systemSelected) {
                return endpoints.size();
            } else {
                return _systems.size();
            }
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

        private void parseModel(final int position) {
            Peer peer = _systems.get(position).getPeer();
            Control.model(peer, new Control.ModelCb() {
                @Override
                public void cb(byte[] dataBson) {
                    JSONObject jsonObject;
                    try {
                        BsonDocument bsonDocument = new RawBsonDocument(dataBson);
                        jsonObject = new JSONObject(bsonDocument.toJson());
                    } catch (JSONException e) {
                        Log.d(TAG, "Json parsing error: " + e);
                        return;
                    }
                    try {
                        for (Iterator<String> iter = jsonObject.keys(); iter.hasNext(); ) {
                                String ep = iter.next();
                                if (ep.equals("mist") || ep.equals("version")) {
                                    /* Don't add these endpoints */
                                } else { //all other endpoints are added to list.
                                    DialogEndpoint dialogEndpoint = new DialogEndpoint();
                                    dialogEndpoint.setPeer(_systems.get(position).getPeer());
                                    dialogEndpoint.setEndpoint(ep);
                                    dialogEndpoint.setType(jsonObject.getJSONObject(ep).getString("type"));
                                    if (jsonObject.getJSONObject(ep).has("read")) {
                                        boolean readable = jsonObject.getJSONObject(ep).getBoolean("read");
                                        dialogEndpoint.setReadable(readable);
                                    }
                                    else {
                                        dialogEndpoint.setReadable(false);
                                    }

                                    endpoints.add(dialogEndpoint);
                                }


                        }
                        notifyDataSetChanged();
                    } catch (JSONException e) {
                        Log.d(TAG, "Error parsing json model: " + e);
                    }
                }
            });
        }

        private class DialogEndpoint {
            private String endpoint;
            private Peer peer;
            private String type;
            private boolean readable;

            public String getEndpoint() {
                return endpoint;
            }

            public void setEndpoint(String endpoint) {
                this.endpoint = endpoint;
            }

            public Peer getPeer() {
                return peer;
            }

            public void setPeer(Peer peer) {
                this.peer = peer;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public void setReadable(boolean r) { readable = r; }
            public boolean isReadable() { return readable; }

        }

    }


    private class DialogSystem {
        private Peer peer;

        private String name;
        private String alias;

        public Peer getPeer() {
            return peer;
        }

        public void setPeer(Peer peer) {
            this.peer = peer;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }
    }

}
