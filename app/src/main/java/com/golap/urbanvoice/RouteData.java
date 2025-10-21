package com.golap.urbanvoice;

import java.util.List;

public class RouteData {

    private final List<Station> forwardStations;
    private final List<Station> backwardStations;
    private final String polylineEncoded;

    public RouteData(List<Station> forwardStations, List<Station> backwardStations, String polylineEncoded) {
        this.forwardStations = forwardStations;
        this.backwardStations = backwardStations;
        this.polylineEncoded = polylineEncoded;
    }

    public List<Station> getForwardStations() { return forwardStations; }
    public List<Station> getBackwardStations() { return backwardStations; }
    public String getPolylineEncoded() { return polylineEncoded; }

}
