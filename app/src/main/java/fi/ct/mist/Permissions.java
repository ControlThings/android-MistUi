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
package fi.ct.mist;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.Random;


/**
 * Created by jeppe on 10/24/16.
 */


public class Permissions {

    private static boolean version() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean hasPermission(Activity activity, String permission) {
        if (version()) {
            return (activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
        }
        return true;
    }


    public static int requestPermission(Activity activity, String permission) {
        int REQUEST_CODE = new Random().nextInt(200) + 1;
        ActivityCompat.requestPermissions(activity, new String[]{permission}, REQUEST_CODE);
        return REQUEST_CODE;
    }

}

