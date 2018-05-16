package fi.ct.mist.dialogs;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import fi.ct.mist.mist.R;


public class CommissioningInterruptedDialog {

    public static final int RETRY = 1;
    public static final int END = 2;

    public static void showDialog(final Activity activity, int message, int hint, final Cb cb) {
        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View view = layoutInflater.inflate(R.layout.information_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(view);

        TextView title = (TextView) view.findViewById(R.id.information_dialog_title);
        title.setText(activity.getResources().getString(R.string.commissioning_interrupted_dialog_title));

        String msg = activity.getResources().getString(message);

        if (hint != 0) {
            msg = msg + "\n\n" + activity.getResources().getString(hint);
        }

        TextView msgText = (TextView) view.findViewById(R.id.information_dialog_message);
        msgText.setText(msg);

        builder.setCancelable(false);

        builder.setPositiveButton(R.string.commissioning_timeout_dialog_button_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int i) {
                cb.onResult(END);
                dialogInterface.dismiss();
            }
        });

        /*
        builder.setNeutralButton(R.string.commissioning_timeout_dialog_button_retry, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                cb.onResult(RETRY);
                dialogInterface.dismiss();
            }
        });
        */

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
        public void onResult(int state);
    }

}
