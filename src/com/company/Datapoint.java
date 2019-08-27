package com.company;

import java.text.DecimalFormat;

public class Datapoint {
    long timestamp;
    double lat;
    double lon;
    float speed;
    float vibr;


    public Datapoint(long timestamp, double lat, double lon, float speed, float vibr) {
        this.timestamp = timestamp;
        this.lat = lat;
        this.lon = lon;
        this.speed = speed;
        this.vibr = vibr;
    }

    public Datapoint(long time_delta, double lat_delta, double lon_delta, float speed_delta, float vibr, Datapoint previous) {
        this.timestamp = previous.getTimestamp() + time_delta;
        this.lat = previous.getLat() + lat_delta;
        this.lon = previous.getLon() + lon_delta;
        this.speed = previous.getSpeed() + speed_delta;
        this.vibr = vibr;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getVibr() {
        return vibr;
    }

    public void setVibr(float vibr) {
        this.vibr = vibr;
    }

    private static DecimalFormat df1 = new DecimalFormat("0.00");
    private static DecimalFormat df2 = new DecimalFormat("0.00000");

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp);
        sb.append('\t');
        sb.append(df2.format(lat));
        sb.append('\t');
        sb.append(df2.format(lon));
        sb.append('\t');
        sb.append(df1.format(speed));
        sb.append('\t');
        sb.append(df1.format(vibr));
        return sb.toString();
    }
}
