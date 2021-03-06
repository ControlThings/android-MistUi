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
package fi.ct.mist.endpoint;

import wish.Peer;

/**
 * Created by jeppe on 10/12/2016.
 */

public interface EndpointFragmentListener {
    public void setMapping(Peer peerTo, String endpointTo);
}
