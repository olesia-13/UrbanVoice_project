package com.golap.urbanvoice;

import java.util.List;

public class RouteData {

    private final List<Station> forwardStations;
    private final List<Station> backwardStations;

    // !!! ПОВЕРТАЄМО ОДНЕ ПОЛЕ, ОСКІЛЬКИ ЛІНІЯ ОДНАКОВА !!!
    private final String polylineEncoded;

    private final int forwardTextResId;
    private final int backwardTextResId;

    public RouteData(List<Station> forwardStations, List<Station> backwardStations,
                     // !!! ТУТ ТІЛЬКИ ОДИН АРГУМЕНТ ДЛЯ ПОЛІЛІНІЇ !!!
                     String polylineEncoded,
                     int forwardTextResId, int backwardTextResId) {
        this.forwardStations = forwardStations;
        this.backwardStations = backwardStations;

        // Ініціалізуємо лише одне поле
        this.polylineEncoded = polylineEncoded;

        this.forwardTextResId = forwardTextResId;
        this.backwardTextResId = backwardTextResId;
    }

    public List<Station> getForwardStations() { return forwardStations; }
    public List<Station> getBackwardStations() { return backwardStations; }

    // !!! ОСНОВНИЙ МЕТОД, ЯКИЙ БУДЕ ВИКЛИКАТИ RouteMap.java !!!
    public String getPolylineEncoded() { return polylineEncoded; }

    // Методи для тексту залишаються
    public int getForwardTextResId() { return forwardTextResId; }
    public int getBackwardTextResId() { return backwardTextResId; }
}
