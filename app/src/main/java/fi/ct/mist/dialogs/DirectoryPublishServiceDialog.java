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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.bson.BSONException;
import org.bson.BsonArray;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonWriter;
import org.bson.RawBsonDocument;
import org.bson.io.BasicOutputBuffer;
import org.json.JSONException;
import org.json.JSONObject;

import fi.ct.mist.mist.R;
import wish.Peer;
import mist.node.request.Control;
import mist.api.request.Directory;


/**
 * Created by jeppe on 12/13/16.
 */

public class DirectoryPublishServiceDialog {

    private static final String TAG = "DirectoryPublishDialog";


    public static void showDialog(final Activity activity, final Peer peer, String alias, final onDirectoryPublishDialogListener cancelListener) {

        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View view = layoutInflater.inflate(R.layout.directory_publish_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(view);

        TextView title = (TextView) view.findViewById(R.id.directory_publish_dialog_title);
        title.setText(activity.getResources().getString(R.string.directory_publish_dialog_title) + " " + alias);

        final EditText typeInput = (EditText) view.findViewById(R.id.directory_publish_type_input);
        typeInput.setVisibility(View.INVISIBLE);

        final EditText descriptionInput = (EditText) view.findViewById(R.id.directory_publish_description_input);
        descriptionInput.setVisibility(View.INVISIBLE);

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


                if (peer != null) {
                    Control.model(peer, new Control.ModelCb() {
                        @Override
                        public void cb(byte[] dataBson) {

                            JSONObject jsonObject;
                            try {
                                BsonDocument bsonDocument = new RawBsonDocument(dataBson);
                                jsonObject = new JSONObject(bsonDocument.toJson());
                            } catch (JSONException e) {
                                Log.d(TAG, "Json parsing error: " + e);
                                return;
                            }

                            if (jsonObject.has("directory")) {
                                getDocument(peer, cancelListener, new GetDocument() {
                                    @Override
                                    public void cb(BsonDocument document) {
                                        publish(peer, document, cancelListener);
                                    }
                                });
                            } else {
                                publish(peer, null, cancelListener);
                            }
                        }

                        @Override
                        public void err(int i, String s) {
                            cancelListener.onErr();
                        }

                        @Override
                        public void end() {
                        }
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

    private static void getDocument(final Peer peer, final onDirectoryPublishDialogListener cancelListener, final GetDocument callback) {
        Control.invoke(peer, "directory", new Control.InvokeCb() {
            @Override
            public void cbArray(BsonArray bsonArray) {
                Log.d("aa", "a " + bsonArray.size() + " :_ " + bsonArray.get(0).asString().getValue());
                if (bsonArray.size() == 1) {
                    Control.invoke(peer, "directory", bsonArray.get(0).asString().getValue(), new Control.InvokeCb() {
                        @Override
                        public void cbDocument(BsonDocument bsonDocument) {
                            callback.cb(bsonDocument);
                        }

                        @Override
                        public void err(int i, String s) {
                            cancelListener.onErr();
                        }
                    });
                }
            }
            @Override
            public void cbDocument(BsonDocument bsonDocument) {
            }

            @Override
            public void err(int i, String s) {
                cancelListener.onErr();
            }

            @Override
            public void end() {
            }
        });


    }

    private static void publish(Peer peer, BsonDocument document, final onDirectoryPublishDialogListener cancelListener) {
        Directory.publishService(peer, document, new Directory.PublishServiceCb() {
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
            public void end() {
            }
        });
    }

    private interface GetDocument {
        public void cb(BsonDocument document);
    }

    public interface onDirectoryPublishDialogListener extends DialogInterface.OnCancelListener {
        public void onOk();

        public void onErr();
    }

}
