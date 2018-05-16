package fi.ct.mist.dialogs;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;

import fi.ct.mist.mist.R;

/**
 * Created by jeppe on 12/13/16.
 */

public class IdentityAccepFriendDialog {

    public static void showDialog(final Activity activity, String name, final Cb cb) {
        LayoutInflater layoutInflater = LayoutInflater.from(activity);


        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(String.format(activity.getResources().getString(R.string.identity_accept_friend_dialog_title), name));
        builder.setNeutralButton(R.string.identity_accept_friend_dialog_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d("Dialog", "cancel");
                dialogInterface.dismiss();
            }
        });

        builder.setNegativeButton(R.string.identity_accept_friend_dialog_button_decline, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int i) {
                cb.decline();
                dialogInterface.dismiss();
            }
        });

        builder.setPositiveButton(R.string.identity_accept_friend_dialog_button_accept, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int i) {
                cb.accept();
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

    public interface Cb{
        public void accept();
        public void decline();
    }
}
