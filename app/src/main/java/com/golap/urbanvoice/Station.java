package com.golap.urbanvoice;

public class Station {

    private final String name;
    private final double latitude;
    private final double longitude;
    private final String audioResName;
    private final String textResKey;

    public Station(String name, double latitude, double doubleLongitude, String audioResName, String textResKey) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = doubleLongitude;
        this.audioResName = audioResName;
        this.textResKey = textResKey;
    }

    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAudioResName() { return audioResName; }
    public String getTextResKey() { return textResKey; }

}
