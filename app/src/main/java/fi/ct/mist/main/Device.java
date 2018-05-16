package fi.ct.mist.main;

import org.json.JSONObject;

import java.io.Serializable;

import wish.Peer;

/**
 * Created by jeppe on 12/7/16.
 */

public class Device implements Serializable {

    private String name;
    private boolean online;
    private Peer peer;
    private String alias;
    private boolean privkey;
    private String ui;
    private transient JSONObject model;
    private boolean config;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isPrivkey() {
        return privkey;
    }

    public void setPrivkey(boolean privkey) {
        this.privkey = privkey;
    }

    public String getUi() {
        return ui;
    }

    public void setUi(String ui) {
        this.ui = ui;
    }

    public Peer getPeer() {
        return peer;
    }

    public void setPeer(Peer peer) {
        this.peer = peer;
    }

    public JSONObject getModel() {
        return model;
    }

    public void setModel(JSONObject model) {
        this.model = model;
    }

    public boolean isConfig() {
        return config;
    }

    public void setConfig(boolean config) {
        this.config = config;
    }
}
