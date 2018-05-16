package fi.ct.mist.advanced.appsSettings;

import wish.Peer;

/**
 * Created by jeppe on 10/12/2016.
 */

public interface SettingsFragmentListener {
    public void addPeer(Peer peer);
    public void removePeer(Peer peer);

    public byte[] getSandboxId();
    public String getName();
}
