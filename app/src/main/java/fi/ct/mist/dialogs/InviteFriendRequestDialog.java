package fi.ct.mist.dialogs;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.List;

import fi.ct.mist.connect.commissioning.ListFriendWifisFragment;
import fi.ct.mist.mist.R;

/**
 * Created by jeppe on 12/13/16.
 */

public class InviteFriendRequestDialog {


    public static void showDialog(final Activity activity, String fromAlias, String toAlias, String invAlias, final Cb cb) {
        LayoutInflater layoutInflater = LayoutInflater.from(activity);

        final boolean onSamePeer = fromAlias.equals(invAlias);

        final boolean[] boxes = new boolean[2];

        List<String> listItems = new ArrayList<>();
        listItems.add(activity.getResources().getString(R.string.invite_friend_request_dialog_checkbox1, fromAlias));

        if (!onSamePeer) {
            listItems.add(activity.getResources().getString(R.string.invite_friend_request_dialog_checkbox2, invAlias));

        }

        CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(String.format(activity.getResources().getString(R.string.invite_friend_request_dialog_title), fromAlias));


        builder.setMultiChoiceItems(items, null, null);


        builder.setNeutralButton(R.string.invite_friend_request_dialog_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
            }
        });

        builder.setNegativeButton(R.string.invite_friend_request_dialog_button_decline, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int i) {
                cb.decline();
                dialogInterface.dismiss();
            }
        });

        builder.setPositiveButton(R.string.invite_friend_request_dialog_button_accept, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int i) {
                cb.accept(boxes[0], boxes[1]);
                dialogInterface.dismiss();
            }
        });

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            private AlertDialog dialog;

            @Override
            public void onShow(DialogInterface dialogInterface) {

                if (!onSamePeer) {
                    dialog.getListView().getChildAt(1).setEnabled(false);
                }

                dialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        AppCompatCheckedTextView checkBox1 = (AppCompatCheckedTextView) parent.getChildAt(0);

                        if (!onSamePeer) {
                            AppCompatCheckedTextView checkBox2 = (AppCompatCheckedTextView) parent.getChildAt(1);
                            if (!checkBox1.isChecked()) {
                                checkBox2.setChecked(false);
                                checkBox2.setEnabled(false);
                            } else {
                                checkBox2.setEnabled(true);
                            }
                            boxes[1] = checkBox2.isChecked();
                        } else {
                            boxes[1] = false;
                        }

                        boxes[0] = checkBox1.isChecked();

                    }
                });
            }

            private DialogInterface.OnShowListener init(AlertDialog dialog) {
                this.dialog = dialog;
                return this;
            }
        }.init(dialog));
        dialog.show();

        activity.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
                dialog.dismiss();
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }


    public interface Cb {
        public void accept(boolean frendrequest, boolean invite);

        public void decline();
    }

}
