package fi.ct.mist.sandbox;

import mist.sandbox.Callback;

/**
 * Created by jan on 2/2/17.
 */

class SandboxQueuedRequest {
    private byte[] data;
    private Callback callback;
    private int idToSandbox;
    private int actualReqId;
    private byte[] sandboxId;
    private boolean handled;

    SandboxQueuedRequest(byte[] sandboxId, byte[] data, Callback callback, int idToSandbox) {
        this.data = data;
        this.callback = callback;
        this.idToSandbox = idToSandbox;
        this.sandboxId = sandboxId;
    }

    byte[] getData() {
        return data;
    }

    Callback getCallback() {
        return callback;
    }

    int getIdToSandbox() {
        return idToSandbox;
    }

    byte[] getSandboxId() {
        return sandboxId;
    }

    void setHandled(int reqId) {
        handled = true;
        actualReqId = reqId;
    }

    boolean isHandled() {
        return handled;
    }

    public int getActualReqId() {
        return actualReqId;
    }
}
