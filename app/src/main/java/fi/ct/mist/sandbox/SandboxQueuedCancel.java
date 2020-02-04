/**
 * Copyright (C) 2020, ControlThings Oy Ab
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * @license Apache-2.0
 */
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
