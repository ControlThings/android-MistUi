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
import android.support.v4.content.ContextCompat;
import android.support.v4.content.IntentCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.bson.BsonArray;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonWriter;
import org.bson.RawBsonDocument;
import org.bson.io.BasicOutputBuffer;

import fi.ct.mist.mist.R;
import wish.Identity;
import wish.Peer;
import mist.node.request.Control;
import mist.api.request.Directory;

/**
 * Created by jeppe on 12/13/16.
 */

public class DirectoryPublishDialog {




    public static void showDialog(final Activity activity, final byte[] uid, String alias, final onDirectoryPublishDialogListener  cancelListener) {

        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View view = layoutInflater.inflate(R.layout.directory_publish_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(view);

        TextView title = (TextView) view.findViewById(R.id.directory_publish_dialog_title);
        title.setText(activity.getResources().getString(R.string.directory_publish_dialog_title) + " " + alias);

        final EditText typeInput = (EditText) view.findViewById(R.id.directory_publish_type_input);
        typeInput.setHint(R.string.directory_publish_dialog_type_input);

        final EditText descriptionInput = (EditText) view.findViewById(R.id.directory_publish_description_input);
        descriptionInput.setHint(R.string.directory_publish_dialog_description_input);

        builder.setCancelable(false);

        builder.setNegativeButton(R.string.directory_publish_dialog_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
            }
        });

        builder.setPositiveButton(R.string.directory_publish_dialog_button_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int i) {
                BsonDocument document = null;

                if (!typeInput.getText().toString().matches("")) {
                    BasicOutputBuffer buffer = new BasicOutputBuffer();
                    BsonWriter writer = new BsonBinaryWriter(buffer);
                    writer.writeStartDocument();
                    writer.writeString("@type", typeInput.getText().toString());

                    if (!descriptionInput.getText().toString().matches("")) {
                        writer.writeString("description", descriptionInput.getText().toString());
                    }

                    writer.writeEndDocument();
                    writer.flush();

                    document = new RawBsonDocument(buffer.toByteArray());
                }
                if (uid != null) {
                    Directory.publishIdentity(uid, document, new Directory.PublishIdentityCb() {
                        @Override
                        public void cb(boolean b) {

                            if (b) {
                                cancelListener.onOk();
                            } else {
                                cancelListener.onErr();
                            }
                        }

                        @Override
                        public void err(int i, String s) {
                            cancelListener.onErr();
                        }

                        @Override
                        public void end() {}
                    });
                }
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            private AlertDialog dialog;

            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positive.setEnabled(true);
            }

            private DialogInterface.OnShowListener init(AlertDialog dialog) {
                this.dialog = dialog;
                return this;
            }

        }.init(dialog));

        dialog.setOnCancelListener(cancelListener);
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

    public interface onDirectoryPublishDialogListener extends DialogInterface.OnCancelListener {
        public void onOk();
        public void onErr();
    }

}
