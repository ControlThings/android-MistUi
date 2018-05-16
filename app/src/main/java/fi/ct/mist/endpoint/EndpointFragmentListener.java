package fi.ct.mist.endpoint;

import wish.Peer;

/**
 * Created by jeppe on 10/12/2016.
 */

public interface EndpointFragmentListener {
    public void setMapping(Peer peerTo, String endpointTo);
}
