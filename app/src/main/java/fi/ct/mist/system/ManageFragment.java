package fi.ct.mist.system;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.bson.BsonDocument;
import org.bson.RawBsonDocument;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Iterator;

import fi.ct.mist.mist.R;
import wish.Peer;
import mist.node.request.Control;

import static org.apache.commons.compress.utils.IOUtils.copy;

/**
 * Created by jeppe on 12/5/16.
 */

public class ManageFragment extends Fragment {

    private final String TAG = "ManageFragment";

    TextView textView;
    ImageView imageView;
    Button buttonInvite;
    Button buttonPublish;

    private String deviceModel = "";
    private String description = null;
    private String imageUrl = null;

    private Peer peer;
    private JSONObject model = null;


    public ManageFragment() {

    }

    public void refreshModel(Peer p) {



        this.peer = p;
        Control.read(peer, "mist.product.model", new Control.ReadCb() {
            @Override
            public void cbString(String s) {
                deviceModel = s;
                update();
            }
        });

        Control.read(peer, "mist.product.imageUrl", new Control.ReadCb() {
            @Override
            public void cbString(String s) {
                imageUrl = s;
                update();
            }
        });

        Control.read(peer, "mist.product.description", new Control.ReadCb() {
            @Override
            public void cbString(String s) {
                description = s;
                update();
            }
        });

/*
        if (model == null) {
            getModel();
        } else {

        }*/
    }

    private void getModel() {
        Control.model(peer, new Control.ModelCb() {
            @Override
            public void cb(byte[] dataBson) {

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

    private void parseEndpiont(JSONObject endpoints) {
        try {
            for (Iterator<String> it = endpoints.keys(); it.hasNext();) {
                String ep = it.next();
                    JSONObject endpoint = endpoints.getJSONObject(ep);
                    if(ep.equals("model")) {
                        deviceModel = endpoint.has("label")?endpoint.getString("label"):"";
                    }
                    if(ep.equals("desc")) {
                        description = endpoint.has("label")?endpoint.getString("label"):null;
                    }
                    if(ep.equals("img")) {
                        imageUrl = endpoint.has("label")?endpoint.getString("label"):null;
                    }
            }
            update();
        } catch (JSONException e) {
            Log.d(TAG, "Error parsing json model: " + e);
        }
    }

    private void parseModel() {
        try {
            for (Iterator<String> iter = model.keys(); iter.hasNext(); ) {
                String key = iter.next();
                if (key.equals("model")) {
                    JSONObject endpoints = model.getJSONObject(key);
                    parseEndpiont(endpoints);
                }
            }

        } catch (JSONException e) {
            Log.d(TAG, "Error parsing json model: " + e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_manage, container, false);
        textView = (TextView) v.findViewById(R.id.manage_description);
        imageView = (ImageView) v.findViewById(R.id.manage_img);
        buttonInvite = (Button) v.findViewById(R.id.manage_invite_button);
        buttonPublish = (Button) v.findViewById(R.id.manage_publish_button);

        buttonInvite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ManageListener listener = (ManageListener) getActivity();
                listener.onInviteExpert(deviceModel);
            }
        });

        buttonPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ManageListener listener = (ManageListener) getActivity();
                listener.onPublishService();
            }
        });

        return v;
    }

    public void update() {

        if (description != null) {
            textView.setText(description);
        }
        if (imageUrl != null) {
           new DownloadImageTask(imageView).execute(imageUrl);
        }

    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    public interface ManageListener{
        public void onInviteExpert(String model);
        public void onPublishService();
    }
}
