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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;

import fi.ct.mist.Versions;
import fi.ct.mist.mist.BuildConfig;
import fi.ct.mist.mist.R;

/**
 * Created by jeppe on 12/13/16.
 */

public class AboutDialog {



    public static void showDialog(final Activity activity) {
        LayoutInflater layoutInflater = LayoutInflater.from(activity);


        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.about_dialog_title);

        String appVersion =  BuildConfig.GitVersion + " " + (BuildConfig.GitClean.equals("-d")?"-d":"");
        String mistVersion = mist.api.ui.BuildConfig.GitVersion + " " + (mist.api.ui.BuildConfig.GitClean.equals("-d")?"-d":"");

        String mistC99Version = Versions.getInstance().getMistC99();
        String wishC99Version = Versions.getInstance().getWishC99();

        builder.setMessage(
                activity.getResources().getString(R.string.about_dialog_app_version) + " " + appVersion + "\n\n"
                + activity.getResources().getString(R.string.about_dialog_mist_version) + " " + mistVersion + "\n"
                + activity.getResources().getString(R.string.about_dialog_mistC99_version) + " " + mistC99Version + "\n"
                + activity.getResources().getString(R.string.about_dialog_wishC99_version) + " " + wishC99Version);
        builder.setCancelable(false);
        builder.setNegativeButton(R.string.about_dialog_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

       final AlertDialog dialog = builder.create();
        dialog.show();


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

    public interface Cb {
        public void delete();
    }
}
