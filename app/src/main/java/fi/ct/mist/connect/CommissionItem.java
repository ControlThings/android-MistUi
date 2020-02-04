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
package fi.ct.mist.connect;

import java.io.Serializable;

/**
 * Created by jeppe on 3/10/17.
 */

public class CommissionItem implements Serializable {

    private String name;

    public static final int TYPE_HEADER = 1;
    public static final int TYPE_WLD = 2;
    public static final int TYPE_WIFI = 3;

    public int getType() {
        return 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public class Header extends CommissionItem {
        String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        @Override
        public int getType() {
            return TYPE_HEADER;
        }
    }


    public class WldConnection extends CommissionItem {

        private byte[] ruid;
        private byte[] rhid;

        public byte[] getRuid() {
            return ruid;
        }

        public void setRuid(byte[] ruid) {
            this.ruid = ruid;
        }

        public byte[] getRhid() {
            return rhid;
        }

        public void setRhid(byte[] rhid) {
            this.rhid = rhid;
        }

        @Override
        public int getType() {
            return TYPE_WLD;
        }
    }

    public class WifiConnection extends CommissionItem {

        private String ssid;
        private String security;
        private int level;

        public String getSsid() {
            return ssid;
        }

        public void setSsid(String ssid) {
            this.ssid = ssid;
        }

        public String getSecurity() {
            return security;
        }

        public void setSecurity(String security) {
            this.security = security;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        @Override
        public int getType() {
            return TYPE_WIFI;
        }
    }
}