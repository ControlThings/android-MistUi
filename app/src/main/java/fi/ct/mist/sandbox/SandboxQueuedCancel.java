package fi.ct.mist.sandbox;

/**
 * Created by jan on 2/2/17.
 */
class SandboxQueuedCancel {
    byte[] _id;
    int reqId;
    public SandboxQueuedCancel(byte[] _id, int id) {
        this._id = _id;
        this.reqId = id;
    }

    public byte[] get_id() {
        return _id;
    }

    public int getReqId() {
        return reqId;
    }
}
