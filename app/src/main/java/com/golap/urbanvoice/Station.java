package com.golap.urbanvoice;

public class Station {

    private final int nameResId;
    private final double latitude;
    private final double longitude;
    private final String audioResKey;


    public Station(int nameResId, double latitude, double longitude, String audioResKey) {
        this.nameResId = nameResId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.audioResKey = audioResKey;

    }

    public int getNameResId() { return nameResId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAudioResKey() { return audioResKey; }

}
