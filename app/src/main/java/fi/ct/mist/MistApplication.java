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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;


import fi.ct.mist.mist.BuildConfig;
import fi.ct.mist.mist.R;
import wish.Errors;

import static fi.ct.mist.settings.SettingsFragment.RPC_ERROR_KEY;
import static fi.ct.mist.settings.SettingsFragment.SETTINGS_KEY;

@ReportsCrashes(
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "http://foremost.cto.fi:5984/acra-mist/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "Mist",
        formUriBasicAuthPassword = "f76i23",

        mode = ReportingInteractionMode.DIALOG,
        // resToastText = R.string.crash_toast_text, // optional, displayed as soon as the crash occurs, before collecting data which can take a few seconds
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_info, //optional. default is a warning sign
        resDialogTitle = R.string.app_name, // optional. default is your application name
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. When defined, adds a user text field input with this text resource as a label
        //resDialogEmailPrompt = R.string.crash_user_email_label, // optional. When defined, adds a user email text entry with this text resource as label. The email address will be populated from SharedPreferences and will be provided as an ACRA field if configured.
        resDialogOkToast = R.string.crash_dialog_ok_toast, // optional. displays a Toast message when the user accepts to send a report.
        resDialogTheme = R.style.AppTheme_Dialog,
        logcatArguments = {"-t", "200", "-v", "time"}

)

public class MistApplication extends Application {

    @Override
    protected void attachBaseContext(final Context base) {
        super.attachBaseContext(base);


        ACRA.init(this);


        String appVersion = BuildConfig.GitVersion + " " + (BuildConfig.GitClean.equals("-d") ? "-d" : "");
        String mistVersion = mist.api.ui.BuildConfig.GitVersion + " " + (mist.api.ui.BuildConfig.GitClean.equals("-d") ? "-d" : "");

        ACRA.getErrorReporter().putCustomData(base.getResources().getString(R.string.about_dialog_app_version), appVersion);
        ACRA.getErrorReporter().putCustomData(base.getResources().getString(R.string.about_dialog_mist_version), mistVersion);

        SharedPreferences preferences = base.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE);
        if (!preferences.contains(RPC_ERROR_KEY)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(RPC_ERROR_KEY, true);
            editor.commit();
        }

        mist.node.Errors.listen(new mist.node.Errors.ListenCb() {
            @Override
            public void cb(int code, String msg) {
                Log.d("mist RPC error"," code: " + code + " msg: " + msg);
                if (code == 104) {  // Endpoint not found or permission denied
                    return;
                }

                Toast.makeText(base, "code: " + code, Toast.LENGTH_SHORT).show();
                Toast.makeText(base, "msg: " + msg, Toast.LENGTH_LONG).show();

                SharedPreferences preferences = base.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE);
                if (preferences.getBoolean(RPC_ERROR_KEY, true)) {
                    sendRpcError(code, msg);
                }
            }
        });

        Errors.listen(new Errors.ListenCb() {
            @Override
            public void cb(int code, String msg) {
                Log.d("wish RPC error", " code: " + code + " msg: " + msg);

                if (code == 63) {
                    Toast.makeText(base, "Error: try again (wish core)", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(base, "code: " + code, Toast.LENGTH_SHORT).show();
                    Toast.makeText(base, "msg: " + msg, Toast.LENGTH_LONG).show();
                }
                SharedPreferences preferences = base.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE);
                if (preferences.getBoolean(RPC_ERROR_KEY, true)) {
                    sendRpcError(code, msg);
                }
            }
        });
    }

    private void sendRpcError(int code, String msg) {
        ACRA.getErrorReporter().putCustomData("RPC", " code: " + code + " msg: " + msg);
        ACRA.getErrorReporter().handleSilentException(null);
        ACRA.getErrorReporter().removeCustomData("RPC");
    }

}
