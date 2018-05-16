// AppToMist.aidl
package mist.sandbox;

// Declare any non-default types here with import statements
import mist.sandbox.Callback;

interface AppToMist {

    int mistApiRequest(in IBinder binder, in String op, in byte[] data, Callback listener);

    void mistApiCancel(in IBinder binder, in int id);

    void kill(in IBinder binder);

}
