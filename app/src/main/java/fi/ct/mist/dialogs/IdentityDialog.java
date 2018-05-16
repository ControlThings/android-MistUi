package fi.ct.mist.dialogs;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import fi.ct.mist.mist.R;
import wish.request.Identity;

/**
 * Created by jeppe on 12/13/16.
 */

public class IdentityDialog {


    public static void showDialog(final Activity activity, final onIdentityDialogListener cancelListener) {
        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View view = layoutInflater.inflate(R.layout.custom_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(view);

        TextView title = (TextView) view.findViewById(R.id.custom_dialog_title);
        title.setText(R.string.identity_dialog_title);

        final EditText input = (EditText) view.findViewById(R.id.custom_dialog_input);
        input.setHint(R.string.identity_dialog_subtitle);

        builder.setCancelable(false);

        builder.setNegativeButton(R.string.identity_dialog_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.setPositiveButton(R.string.identity_dialog_button_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int i) {
                Identity.create(input.getText().toString(), new Identity.CreateCb() {
                    @Override
                    public void cb(wish.Identity mistIdentity) {
                        new android.os.Handler().postDelayed(
                                new Runnable() {
                                    public void run() {
                                        cancelListener.onOk();
                                    }
                                },
                                300);

                        // dialogInterface.cancel();
                    }

                    @Override
                    public void err(int i, String s) {

                    }

                    @Override
                    public void end() {

                    }
                });
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            private AlertDialog dialog;

            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positive.setEnabled(false);
            }

            private DialogInterface.OnShowListener init(AlertDialog dialog) {
                this.dialog = dialog;
                return this;
            }


        }.init(dialog));


        input.addTextChangedListener(new TextWatcher() {
            private AlertDialog dialog;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positive.setEnabled(true);

                Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                negative.setTextColor(ContextCompat.getColor(activity, R.color.ctTextColor));
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }

            private TextWatcher init(AlertDialog dialog) {
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

    public interface onIdentityDialogListener extends DialogInterface.OnCancelListener {
        public void onOk();
    }

}