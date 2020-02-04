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
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import fi.ct.mist.mist.BuildConfig;
import fi.ct.mist.mist.R;

import static android.R.attr.width;
import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

/**
 * Created by jeppe on 12/13/16.
 */

public class QrDialog {

    public final static int WIDTH=500;

    public static void showDialog(final Activity activity, String qrText) {
        LayoutInflater layoutInflater = LayoutInflater.from(activity);

        View view = activity.getLayoutInflater().inflate(R.layout.qr_dialog, null);

        ImageView imageView = (ImageView) view.findViewById(R.id.qr_image);
        try {
            Bitmap bitmap = encodeAsBitmap(qrText);
            imageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        final Dialog settingsDialog = new Dialog(activity);
        settingsDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        settingsDialog.setContentView(view);
        settingsDialog.show();


        activity.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(Activity activity) {}

            @Override
            public void onActivityResumed(Activity activity) {}

            @Override
            public void onActivityPaused(Activity activity) {
                settingsDialog.dismiss();
            }

            @Override
            public void onActivityStopped(Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        });

    }

   private static Bitmap encodeAsBitmap(String str) throws WriterException {
    BitMatrix result;
    try {
        result = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, WIDTH, WIDTH, null);
    } catch (IllegalArgumentException iae) {
        // Unsupported format
        return null;
    }
    int w = result.getWidth();
    int h = result.getHeight();
    int[] pixels = new int[w * h];
    for (int y = 0; y < h; y++) {
        int offset = y * w;
        for (int x = 0; x < w; x++) {
            pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
        }
    }
    Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    bitmap.setPixels(pixels, 0, WIDTH, 0, 0, w, h);
    return bitmap;
}

}
