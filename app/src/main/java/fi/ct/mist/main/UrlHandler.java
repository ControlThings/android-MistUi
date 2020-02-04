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
package fi.ct.mist.main;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import fi.ct.mist.dialogs.IdentityDialog;
import fi.ct.mist.dialogs.IdentityImportDialog;
import wish.request.Identity;

import static fi.ct.mist.ParseDeepLink.friendRequest;
import static utils.Util.*;

/**
 * Created by jeppe on 2/7/17.
 */
public class UrlHandler {

    private static final String TAG = "UrlHandler";

    private static UrlHandler ourInstance = new UrlHandler();

    public static UrlHandler getInstance() {
        return ourInstance;
    }

    private UrlHandler() {
    }

    public void parse(final Activity activity, Bundle bundle, final Cb callback) {
        /*
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            Log.d(TAG, key +" : " + value.toString() + " type: " + value.getClass().getSimpleName());
        }
  */
        Object type = bundle.get("url_type");

        if (type != null) {
            if (type.toString().equals(friendRequest)) {
                final Object data = bundle.get("data");
                if (data != null) {
                    IdentityImportDialog.showDialog(activity, null, new IdentityImportDialog.Cb() {
                        @Override
                        public void _import() {
                            try {
                                Identity._import(stringToByteArrayZip((String) data), new Identity.ImportCb() {
                                    @Override
                                    public void cb(String s, byte[] bytes) {
                                    }

                                    @Override
                                    public void err(int i, String s) {
                                    }

                                    @Override
                                    public void end() {
                                    }
                                });
                                callback.ready();
                            } catch (Exception e) {
                                Log.d(TAG, "Error: " + Log.getStackTraceString(e));
                            }

                        }
                    });
                }
            }
        }
    }

    public interface Cb {
        public void ready();
    }
}
