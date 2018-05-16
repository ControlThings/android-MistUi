package fi.ct.mist.connect.commissioning;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fi.ct.mist.mist.R;
import wish.Peer;
import mist.node.request.Control;
import mist.api.request.Mist;

/**
 * Created by jan on 9/20/17.
 */

public class ManualCommissioning extends AppCompatActivity implements ListFriendWifisFragment.Commissioner {
    private Toolbar toolbar;
    private String TAG = "ManualCommissioning";

    private Peer commissioningPeer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");
        setContentView(R.layout.fragment);

        Intent intent = getIntent();
        commissioningPeer = (Peer) intent.getSerializableExtra("peer");
        String alias = intent.getStringExtra("alias");
        updateToolbar(alias);



        updateFriendWifis();
    }

    private void updateFriendWifis() {
        Control.invoke(commissioningPeer, "mistWifiListAvailable", new Control.InvokeCb() {
            @Override
            public void cbDocument(BsonDocument bsonDocument) {
                Log.d(TAG, "Got list");
                List<WifiNetworkEntry> wifiList = new ArrayList<>();
                for (Map.Entry<String, BsonValue> entry : bsonDocument.entrySet()) {
                    String key = entry.getKey();

                    WifiNetworkEntry wifiNetworkEntry = new WifiNetworkEntry();
                    BsonDocument document = entry.getValue().asDocument();
                    wifiNetworkEntry.setSsid(document.get("ssid").asString().getValue());
                    wifiNetworkEntry.setStrength(document.get("rssi").asInt32().getValue());
                    wifiList.add(wifiNetworkEntry);
                }
                Log.d(TAG, "Got list, num entries is " + wifiList.size());

                FragmentManager manager = getSupportFragmentManager();
                FragmentTransaction transaction = manager.beginTransaction();
                ListFriendWifisFragment listFriendWifisFragment = new ListFriendWifisFragment();
                transaction.add(R.id.fragment_container, listFriendWifisFragment);
                //transaction.addToBackStack(null);
                transaction.commit();

                listFriendWifisFragment.setCommissioner(ManualCommissioning.this);
                listFriendWifisFragment.setWifiNetworks(wifiList, commissioningPeer);

            }
        });
    }

    private void updateToolbar(String alias) {
        toolbar = (Toolbar) findViewById(R.id.fragment_toolbar);
        toolbar.setTitle(getResources().getString(R.string.manual_commissioning_title, alias));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    public class WifiComissioningCridentials {
        public String ssid;
        public String wifi_Credentials;

        public WifiComissioningCridentials(String ssid, String wifi_Credentials) {
            this.ssid = ssid;
            this.wifi_Credentials = wifi_Credentials;
        }
    }



    public void sendWifiConfiguration(String ssid, String password) {

        BsonDocument bsonDocument = new BsonDocument();
        bsonDocument.append("ssid", new BsonString(ssid));
        bsonDocument.append("wifi_Credentials", new BsonString(password));

        Control.invoke(commissioningPeer, "mistWifiCommissioning", bsonDocument, new Control.InvokeCb() {});

        Context context = getApplicationContext();
        CharSequence text = "Please wait...";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();

        finish();
    }
}
