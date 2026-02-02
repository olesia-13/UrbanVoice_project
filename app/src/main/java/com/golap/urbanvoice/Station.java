package com.golap.urbanvoice;

import java.util.Objects;


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



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Station station = (Station) o;

        return nameResId == station.nameResId &&
                Double.compare(station.latitude, latitude) == 0 &&
                Double.compare(station.longitude, longitude) == 0 &&
                Objects.equals(audioResKey, station.audioResKey);
    }


    @Override
    public int hashCode() {
        return Objects.hash(nameResId, latitude, longitude, audioResKey);
    }


    @Override
    public String toString() {
        return "Station{" +
                "nameResId=" + nameResId +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", audioResKey='" + audioResKey + '\'' +
                '}';
    }
}
