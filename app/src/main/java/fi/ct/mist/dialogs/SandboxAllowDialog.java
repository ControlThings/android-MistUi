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
package fi.ct.mist.dialogs;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;

import fi.ct.mist.mist.R;

/**
 * Created by jeppe on 12/13/16.
 */

public class SandboxAllowDialog {

    public static void showDialog(final Activity activity, final String name, final String op, final Cb cb) {

        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(R.string.sandbox_allow_dialog_title);
                if (op.equals("identity.friendRequest")) {
                    builder.setMessage(activity.getResources().getString(R.string.sandbox_allow_dialog_friend_request, name));
                } else {
                    builder.setMessage(op);
                }
                builder.setCancelable(false);
                builder.setNegativeButton(R.string.sandbox_allow_dialog_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d("Dialog", "cancel");
                        dialogInterface.dismiss();

                    }
                });

                builder.setPositiveButton(R.string.sandbox_allow_dialog_button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, int i) {
                        cb.allow();
                        dialogInterface.dismiss();
                    }
                });

                final AlertDialog dialog = builder.create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                dialog.show();        // this will run in the main thread


                activity.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

                    @Override
                    public void onActivityStarted(Activity activity) {}

                    @Override
                    public void onActivityResumed(Activity activity) {}

                    @Override
                    public void onActivityPaused(Activity activity) {
                        dialog.dismiss();
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {}

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

                    @Override
                    public void onActivityDestroyed(Activity activity) {}
                });
            }
        });

    }

    public interface Cb{
        public void allow();
    }
}
