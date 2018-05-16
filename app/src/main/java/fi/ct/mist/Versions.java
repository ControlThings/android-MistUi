package fi.ct.mist;

import android.util.Log;

import org.acra.ACRA;

import fi.ct.mist.mist.R;
import mist.api.request.Mist;
import wish.request.Wish;

/**
 * Created by jeppe on 2/27/17.
 */
public class Versions {
    private static Versions ourInstance = new Versions();

    public static Versions getInstance() {
        return ourInstance;
    }

    private Versions() {
    }

    private String mistC99 = null;
    private String wishC99 = null;

    public void setVersions() {
        Wish.version(new Wish.VersionCb() {
            @Override
            public void cb(String wishV) {
                wishC99 = wishV;
                ACRA.getErrorReporter().putCustomData("wish-c99 version:", wishV);

            }

            @Override
            public void err(int i, String s) {}

            @Override
            public void end() {}
        });

        Mist.version(new Mist.VersionCb() {
            @Override
            public void cb(String mistV) {
                mistC99 = mistV;
                ACRA.getErrorReporter().putCustomData("mist-c99 version:", mistV);
            }

            @Override
            public void err(int i, String s) {}

            @Override
            public void end() {}
        });
    }


    public String getWishC99() {
        return wishC99;
    }

    public String getMistC99() {
        return mistC99;
    }
}
