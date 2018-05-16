package fi.ct.mist.system;

import org.json.JSONObject;

import fi.ct.mist.endpoint.Endpoint;

/**
 * Created by jeppe on 10/12/2016.
 */

public interface SystemsFragmentListener {
    public void openEndpoint(fi.ct.mist.main.Endpoint endpoint);
    public void openObject(fi.ct.mist.main.Endpoint endpoint);

    public String getAlias();
}
