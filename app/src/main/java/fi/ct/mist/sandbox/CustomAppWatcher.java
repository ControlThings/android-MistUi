package fi.ct.mist.sandbox;

import android.os.DropBoxManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import org.bson.BsonBinary;
import org.bson.BsonBinaryWriter;
import org.bson.BsonWriter;
import org.bson.io.BasicOutputBuffer;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mist.api.MistApi;
import mist.api.request.Mist;

import static fi.ct.mist.Util.isServiceRunning;

/**
 * Created by jeppe on 1/9/17.
 */

class CustomAppWatcher {


    private Sandbox sandbox;

    CustomAppWatcher(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    private ArrayMap<byte[], DeathCallback> deathCallbacks = new ArrayMap<>();

    private final class DeathCallback implements IBinder.DeathRecipient {
        private byte[] _id;
        private IBinder _binder;

        DeathCallback(byte[] id, IBinder binder) {
            this._id = id;
            this._binder = binder;
        }

        @Override
        public void binderDied() {
            synchronized (deathCallbacks) {
                _binder.unlinkToDeath(this, 0);
                clientDeath(_id);
            }
        }
    }

    private void clientDeath(byte[] id) {
        /* Set the custom app's login status to false */
        Log.d("Logout", bytesToHex(id));
        logoutFromMist(id);
    }

    boolean login(IBinder binder, byte[] id) {
        synchronized (deathCallbacks) {
            try {
                if (!deathCallbacks.containsKey(id)) {
                    DeathCallback deathCallback = new DeathCallback(id, binder);
                    deathCallbacks.put(id, deathCallback);
                    binder.linkToDeath(deathCallback, 0);
                }
                return true;
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public void logout(IBinder binder) {
        for (Map.Entry<byte[], DeathCallback> entry: deathCallbacks.entrySet()) {
            if (entry.getValue()._binder.equals(binder)) {
                entry.getValue()._binder.unlinkToDeath(entry.getValue(), 0);
                byte[] id = entry.getKey();
                logoutFromMist(id);
            }
        }
    }

    private void logoutFromMist(final byte[] id) {

        sandbox.logout(deathCallbacks.get(id)._binder);
        deathCallbacks.remove(id);

        BasicOutputBuffer buffer = new BasicOutputBuffer();
        BsonWriter writer = new BsonBinaryWriter(buffer);
        writer.writeStartDocument();

        writer.writeString("op", "sandboxed.logout");

        writer.writeStartArray("args");
        writer.writeBinaryData(new BsonBinary(id));
        writer.writeEndArray();

        writer.writeInt32("id", 0);

        writer.writeEndDocument();
        writer.flush();

        MistApi.getInstance().sandboxedRequest(id, buffer.toByteArray(), new MistApi.RequestCb() {
            @Override
            public void response(byte[] bytes) {
                Log.d("Logout", "ack");
            }

            @Override
            public void end() {}

            @Override
            public void err(int code, String msg) {
                super.err(code, msg);
                Log.d("Logout", "err: " + msg);
            }
        });
    }

}
