// MistToApp.aidl
package mist.sandbox;

// Declare any non-default types here with import statements

interface Callback {
    void ack(in byte[] data);
    void sig(in byte[] data);
    void err(int code, String msg);
}
