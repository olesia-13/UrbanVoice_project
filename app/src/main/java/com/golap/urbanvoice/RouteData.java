package com.golap.urbanvoice;

import java.util.List;

public class RouteData {

    private final List<Station> forwardStations;
    private final List<Station> backwardStations;

    private final String polylineEncoded;

    private final int forwardTextResId;
    private final int backwardTextResId;

    public RouteData(List<Station> forwardStations, List<Station> backwardStations,

                     String polylineEncoded,
                     int forwardTextResId, int backwardTextResId) {
        this.forwardStations = forwardStations;
        this.backwardStations = backwardStations;


        this.polylineEncoded = polylineEncoded;

        this.forwardTextResId = forwardTextResId;
        this.backwardTextResId = backwardTextResId;
    }

    public List<Station> getForwardStations() { return forwardStations; }
    public List<Station> getBackwardStations() { return backwardStations; }


    public String getPolylineEncoded() { return polylineEncoded; }


    public int getForwardTextResId() { return forwardTextResId; }
    public int getBackwardTextResId() { return backwardTextResId; }
}
