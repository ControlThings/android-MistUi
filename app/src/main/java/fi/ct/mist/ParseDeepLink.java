package fi.ct.mist;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.Set;

import fi.ct.mist.connect.CommissionItem;
import fi.ct.mist.connect.commissioning.GuidedCommissioning;
import fi.ct.mist.connect.commissioning.CommissioningStateMachine;
import fi.ct.mist.main.Main;
import fi.ct.mist.mist.R;

/**
 * Created by jeppe on 2/6/17.
 */

public class ParseDeepLink extends Activity {

    private static final String TAG = "DeepLink";

    public static final String friendRequest = "friendRequest";
    public static final String commissioningString = "commissioning";

    public static final int COMMISSIONIG_RESULT_ID = 42;

    //public static final String Identity_Import = "/identity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null || intent.getData() == null) {
            finish();
        }
        openDeepLink(intent.getData());
        finish();
    }

    /* Parse URLs of form wish://

    For example, URLs which can start a Wifi commissioning are of form: wish://commissioning?ssid=my_ssid_name&security=OPEN
     */
    private void openDeepLink(Uri data) {

        Log.d(TAG, data.getHost() + " : " + data.getQuery() + " : " + data.getAuthority() + " : " + data.getEncodedQuery() + " : " + data.getPath());

        if (data.getHost() != null && data.getQuery() != null ) {
            String host = data.getHost();
            if (friendRequest.equals(host)) {
                Intent intent = new Intent(this, Main.class);
                intent.putExtra("tag", getResources().getString(R.string.main_users));
                intent.putExtra("type", "url");
                intent.putExtra("url_type", host);
                intent.putExtra("data", data.getQuery());
                startActivity(intent);
            } else if (host.equals(commissioningString)) {
                Intent intent = new Intent(this, GuidedCommissioning.class);
                Bundle bundle = new Bundle();

                /* Parse the 'query' part of the URL */
                String[] queryElems = data.getQuery().split("&");
                if (queryElems.length < 2) {
                    Log.d(TAG, "Illegal format for SSID in URL query part (bad split for &), at least 2 options are required, as in ssid=<ssid-name>&security=<sec-type>");
                    finish();
                    return;
                }
                String ssid = queryElems[0].split("=")[1];
                if (ssid == null || ssid.length() == 0) {
                    Log.d(TAG, "Illegal format for SSID parameter in query URL");
                    finish();
                    return;
                }
                String security = queryElems[1].split("=")[1];
                if (security == null || security.length() == 0) {
                    Log.d(TAG, "Illegal format for security parameter in query URL");
                    finish();
                    return;
                }

                CommissionItem.WifiConnection item = new CommissionItem().new WifiConnection();
                item.setSsid(ssid);
                item.setSecurity(security);
                bundle.putSerializable("item", item);
                intent.putExtras(bundle);

                /* If we detect that a commissioning was already on-going, programmatically abort the ongoing-commissioning before we start a new one */
                if (CommissioningStateMachine.getInstance().getCurrentState() != CommissioningStateMachine.State.INITIAL) {

                    CommissioningStateMachine.getInstance().reportEvent(CommissioningStateMachine.Event.ON_BACK_PRESSED);
                }
                startActivityForResult(intent, COMMISSIONIG_RESULT_ID);
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == COMMISSIONIG_RESULT_ID) {
            // Handle commissioning result
        }
        finish();
    }

    private Bundle parseOptions(Uri data) {
        Bundle options = new Bundle();
        Set<String> keys = data.getQueryParameterNames();
        if (keys.isEmpty()) {
            return options;
        }
        for (String key : keys) {
            if (key != "op") {
                String uriData = data.getQueryParameter(key);
                uriData = uriData.replaceAll(" ", "+");
                options.putString(key, uriData);
            }
        }
        return options;
    }

}




