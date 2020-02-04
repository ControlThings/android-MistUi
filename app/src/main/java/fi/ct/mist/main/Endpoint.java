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
package fi.ct.mist.main;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by jeppe on 10/12/2016.
 */

public class Endpoint implements Serializable {

    private boolean online = true;

    private String type;
    private String label;
    private String name;
    private String object;
    //private JSONObject object = null;

    private Boolean booleanValue = null;
    private Integer integerValue = null;
    private Double floatValue = null;
    private String stringValue = null;

    private boolean read = false;
    private boolean write = false;
    private boolean invoke = false;

    public Endpoint() {
    }

    public JSONObject getObject() {
        if (object == null) {
            return null;
        }
        Log.d("json: " , object);

        try {
           return new JSONObject(object);
        } catch (Exception e) {
            Log.d("errr" ,e.getMessage());
            return null;
        }


    }

    public void setObject(JSONObject object) {
        this.object = object.toString();
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public String getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isWrite() {
        return write;
    }

    public void setWrite(boolean write) {
        this.write = write;
    }

    public boolean isInvoke() {
        return invoke;
    }

    public void setInvoke(boolean invoke) {
        this.invoke = invoke;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public Integer getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    public Double getFloatValue() {
        return floatValue;
    }

    public void setFloatValue(Double floatValue) {
        this.floatValue = floatValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }
}
