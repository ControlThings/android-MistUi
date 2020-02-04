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
package fi.ct.mist.sandbox;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import org.bson.BSONException;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

import fi.ct.mist.Util;
import fi.ct.mist.dialogs.AboutDialog;
import fi.ct.mist.dialogs.SandboxAllowDialog;
import fi.ct.mist.mist.R;
import fi.ct.mist.sandbox.Loader;
import wish.Peer;
import mist.api.request.Mist;
import mist.api.request.Sandbox;
import mist.api.request.Sandboxed;
import wish.WishApp;

import static utils.Util.*;

public class CustomWebView extends Activity {

    private final String TAG = "customWebView";
    private WebView webView;
    private Peer peer = null;

    private byte[] sandboxId = null;
    private String name = null;
    private String alias = null;
    private File uiPath;

    private String sandboxName;
    private String type;

   // private Loader loader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        Intent intent = getIntent();
        if (intent.hasExtra("peer")) {
            Log.d(TAG, "onCreate ui");
            peer = (Peer) intent.getSerializableExtra("peer");
            type = "ui";
        } else {
            Log.d(TAG, "onCreate app");
            type = "app";
        }

        if (intent.hasExtra("alias")) {
            alias = intent.getStringExtra("alias");
            Log.d(TAG, "onCreate alias: " + alias);
        }
        if (intent.hasExtra("name")) {
            name = intent.getStringExtra("name");
            Log.d(TAG, "onCreate name: " + name);
        }

        setContentView(R.layout.custom_web_view);
        webView = (WebView) findViewById(R.id.custom_webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.addJavascriptInterface(new WebViewInterface(), "android");

        webView.setWebChromeClient(new WebChromeClient(){
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());

            }
        } );

        if (intent.hasExtra("url")){
            String url = intent.getStringExtra("url");
            Log.d(TAG, "onCreate url");
            new DownloadUiTask().execute(url, type);
        } else {
            if (intent.hasExtra("sandboxName") && intent.hasExtra("sandboxPath") && intent.hasExtra("sandboxId")) {
                sandboxName = intent.getStringExtra("sandboxName");
                uiPath =  new File(intent.getStringExtra("sandboxPath"));
                sandboxId = intent.getByteArrayExtra("sandboxId");

                Log.d(TAG,"path" + intent.getStringExtra("sandboxPath"));
                Log.d(TAG, " name" + sandboxName);

                loginToSandbox();
            } else {
                finish();
            }
        }


        // webView.getSettings().setDomStorageEnabled(true);

    }

    @Nullable
    @Override
    public View onCreateView(String name, final Context context, AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    };

    private byte[] getHash(){
        byte[] hash = new byte[20];

        ByteBuffer buffer = ByteBuffer.allocate(128);
        buffer.put(peer.getLuid());
        buffer.put(peer.getRuid());
        buffer.put(peer.getRhid());
        buffer.put(peer.getRsid());
        buffer.array();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            hash = md.digest(buffer.array());
        }catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "SHA-1 algorithm is not available...");
            finish();
        }
        return ByteBuffer.allocate(32).put(hash).array();
    }

    private void setVariables(Res res) {

        uiPath = res.getUrl();

        if (type.equals("app")) {
            sandboxId = res.getHash();
            try {
                JSONObject obj = loadJSON();
                sandboxName = obj.getJSONObject("mist").getString("name");
            } catch (JSONException e) {
                Log.d(TAG, "error parsing package.json " + e.getMessage());
                sandboxName = res.getHash().toString().substring(0, 8);
            }
        } else {
            sandboxId = getHash();
            sandboxName = name + " (" + alias + ")";
        }
       loginToSandbox();
    }

    private void loginToSandbox() {
        Sandbox.create(sandboxId, sandboxName, new Sandbox.CreateCb() {
            @Override
            public void cb(boolean b) {
                Log.d(TAG, "login res" + b);


                if (b) {
                    addPeerToSandbox();
                } else {
                    finish();
                }
            }

            @Override
            public void err(int i, String s){
                Log.d(TAG, "Sandbox create error:" + s);

                finish();
            }

            @Override
            public void end() {}
        });
    }

    private void addPeerToSandbox() {

        if (peer == null) {
            loadUi();
            return;
        }
        Sandbox.listPeers(sandboxId, new Sandbox.ListPeersCb() {
            @Override
            public void cb(List<Peer> arrayList) {
                if (arrayList.size() == 0) {
                    Sandbox.addPeer(sandboxId, peer, new Sandbox.AddPeerCb() {
                        @Override
                        public void cb() {
                            loadUi();
                        }

                        @Override
                        public void err(int i, String s) {
                            finish();
                        }

                        @Override
                        public void end() {}
                    });
                } else if(arrayList.get(0).equals(peer)){
                    loadUi();
                } else {
                    finish();
                }
            }
            @Override
            public void err(int i, String s) {
                finish();
            }
            @Override
            public void end() {}
        });
    }

    private void loadUi() {
        String path = uiPath.toString() + "/package/src/application.html";
        webView.loadUrl("file:///" + path);
    }

    public class WebViewInterface {

        @JavascriptInterface
        public void send(String bsonBase64) throws JSONException, UnsupportedEncodingException {

            byte[] args = Base64.decode(bsonBase64, Base64.DEFAULT);

            WishApp.getInstance().bsonConsolePrettyPrinter("bson from ui", args);

            Sandboxed.request(sandboxId, args, new Sandboxed.SandboxedCb() {
                @Override
                public void cb(byte[] bson) {
                   // Log.d(TAG, "sandboxID: " + sandboxId );
                    WishApp.getInstance().bsonConsolePrettyPrinter("bson to ui", bson);
                    callbackToUi(bson);
                }
            });
        }
    }

    public void callbackToUi(byte[] bson) {
        final String base64String = Base64.encodeToString(bson, Base64.DEFAULT);
        if (webView == null) {
            Log.d(TAG, "Trying to sending data to closed webview: ");
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("javascript: android.receive(\"" + base64String + "\")");
            }
        });

    }

    @Override
    protected void onStart() {
        Log.d(TAG, "Start.");
        super.onStart();
        //final Activity activity = this;

       // loader = new Loader(this);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Stop.");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Destory.");
        super.onDestroy();
      //  loader.close();
        if (webView != null) {
            ViewGroup parent = (ViewGroup) webView.getParent();
            parent.removeView(webView);

            /*
            webView.clearHistory();
            webView.clearCache(true);
            webView.loadUrl("about:blank");
            webView.pauseTimers();
            webView = null;
            */

        }
        if (sandboxId != null) {
            Sandbox.logout(sandboxId, new Sandbox.LogoutCb() {
                @Override
                public void cb(boolean b) {
                    sandboxId = null;
                }

                @Override
                public void err(int i, String s) {}

                @Override
                public void end() {}
            });
        }
    }


    private class DownloadUiTask extends AsyncTask<String, Void, Res> {

        public DownloadUiTask() {}

        protected Res doInBackground(String... urls) {
            String tgzUrl = urls[0];
            String type = urls[1];
            File tmp = new File(getCacheDir(), "tmp.tgz");
            try {
                URLConnection urlConnection = new URL(tgzUrl).openConnection();
                HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;
                int responseCode = httpConnection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String etag = httpConnection.getHeaderField("ETag");
                    etag = etag.replace("\"", "");
                    httpConnection.disconnect();
                    if (etag == null) {
                        return null;
                    }

                    File dir = new File(getFilesDir(), "/"+type+"/" + etag);

                    if (!dir.isDirectory()) {

                        dir.mkdir();

                        InputStream in = new java.net.URL(tgzUrl).openStream();
                        OutputStream out = new FileOutputStream(tmp);

                        byte[] buffer = new byte[4 * 1024]; // or other buffer size
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        out.flush();
                        out.close();
                        in.close();
                        Util.unTar(Util.unGzip(tmp, getCacheDir()), dir);
                    }
                    Res res = new Res();
                    res.setHash(ByteBuffer.allocate(32).put(ByteBuffer.wrap(etag.getBytes())).array());
                    res.setUrl(dir);

                    //Log.d(TAG, "doInBackground returning");
                    return res;
                } else {
                    return null;
                }
            }
            catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Res res) {
            if (res == null){
                Log.d(TAG, "Error loading ui.");
                onDestroy();
            } else {
                try {
                    Log.d(TAG, "onPostExecute");
                    setVariables(res);
                } catch (Exception e) {
                    Log.d(TAG, "tar.gz error: " + e.getMessage());
                }
            }
        }
    }

    private class Res {

        private byte[] hash;
        private File url;

        public byte[] getHash() {
            return hash;
        }

        public void setHash(byte[] hash) {
            this.hash = hash;
        }

        public File getUrl() {
            return url;
        }

        public void setUrl(File url) {
            this.url = url;
        }
    }

    private JSONObject loadJSON() throws JSONException{
        String json = null;
        try {

            InputStream is = new FileInputStream(uiPath.toString() + "/package/package.json");//getAssets().open("file_name.json");

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return new JSONObject(json);

    }
}

