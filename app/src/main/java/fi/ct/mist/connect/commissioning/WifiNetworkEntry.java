package fi.ct.mist.connect.commissioning;

/**
 * Created by jeppe on 3/10/17.
 */


class WifiNetworkEntry {
    private String ssid;
    private int strength;

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }
}
