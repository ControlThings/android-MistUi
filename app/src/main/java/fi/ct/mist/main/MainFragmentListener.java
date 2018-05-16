package fi.ct.mist.main;

import wish.Identity;

/**
 * Created by jeppe on 10/12/2016.
 */

public interface MainFragmentListener {
    public void setIdentity(byte[] uid);

    public void setSystem(Device device);

    public Identity getSelectedIdentity();

    public boolean isReady();
}
