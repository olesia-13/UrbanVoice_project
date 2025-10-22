package com.golap.urbanvoice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapDataManager {

    private static final Map<String, RouteData> ALL_ROUTES_DATA;

    public static final float GEOFENCE_RADIUS_METERS = 50f;
    public static final String ROUTE_TRAM_1_KEY = "R001";
    public static final String DIRECTION_FORWARD = "FORWARD";
    public static final String DIRECTION_BACKWARD = "BACKWARD";

    static {
        ALL_ROUTES_DATA = new HashMap<>();

        // -----------------------------------------------------------------
        // 🚨 МАРШРУТ: Трамвай №1 (R001)
        // Михайлівська Борщагівка (1) <-> Старовокзальна (16)
        // -----------------------------------------------------------------

        // Зберігаємо координати для повторного використання у зворотному напрямку
        final double LAT_1 = 50.4083436923589; final double LON_1 = 30.406895340303663; // Михайлівська Б.
        final double LAT_2 = 50.40933925862721; final double LON_2 = 30.400403911953777; // Кікабідзе
        final double LAT_3 = 50.41394384986361; final double LON_3 = 30.395447710840326; // Махова
        final double LAT_4 = 50.4172099530599;  final double LON_4 = 30.389047483118638; // Зодчих
        final double LAT_5 = 50.42048970430078; final double LON_5 = 30.382230911954466; // Руденка
        final double LAT_6 = 50.42595898727306; final double LON_6 = 30.384549787091505; // Доманицького
        final double LAT_7 = 50.429785993091876; final double LON_7 = 30.38628508126995; // Гната Юри
        final double LAT_8 = 50.43315475028982; final double LON_8 = 30.399456483119643; // Івана Дзюби
        final double LAT_9 = 50.43610181208145; final double LON_9 = 30.4107998254486; // Вацлава Гавела
        final double LAT_10 = 50.438504103271654; final double LON_10 = 30.420159837092314; // Шалімова
        final double LAT_11 = 50.44116963568798; final double LON_11 = 30.430886335488776; // НАУ
        final double LAT_12 = 50.444126318176785; final double LON_12 = 30.442475284969795; // Індустріальна
        final double LAT_13 = 50.44651734689533; final double LON_13 = 30.455071251615205; // Олекси Тихого
        final double LAT_14 = 50.44717274141664; final double LON_14 = 30.467162599724603; // Політехнічна
        final double LAT_15 = 50.44637261428129; final double LON_15 = 30.48816543709288; // Площа Галицька
        final double LAT_16 = 50.4442329372453;  final double LON_16 = 30.489967988668752; // Старовокзальна


        // --- 1. Станції "Вперед" (1 -> 16) ---
        List<Station> forwardStations = new ArrayList<>();

        // FWD_01: Михайлівська Борщагівка
        forwardStations.add(new Station(R.string.station_r001_1, LAT_1, LON_1, "r001_fwd_01"));
        // FWD_02: Вулиця Вахтанга Кікабідзе
        forwardStations.add(new Station(R.string.station_r001_2, LAT_2, LON_2, "r001_fwd_02"));
        // FWD_03: Вулиця Олександра Махова
        forwardStations.add(new Station(R.string.station_r001_3, LAT_3, LON_3, "r001_fwd_03"));
        // FWD_04: Вулиця Зодчих
        forwardStations.add(new Station(R.string.station_r001_4, LAT_4, LON_4, "r001_fwd_04"));
        // FWD_05: Бульвар Миколи Руденка
        forwardStations.add(new Station(R.string.station_r001_5, LAT_5, LON_5, "r001_fwd_05"));
        // FWD_06: Вулиця Василя Доманицького
        forwardStations.add(new Station(R.string.station_r001_6, LAT_6, LON_6, "r001_fwd_06"));
        // FWD_07: Гната Юри
        forwardStations.add(new Station(R.string.station_r001_7, LAT_7, LON_7, "r001_fwd_07"));
        // FWD_08: Івана Дзюби
        forwardStations.add(new Station(R.string.station_r001_8, LAT_8, LON_8, "r001_fwd_08"));
        // FWD_09: Вацлава Гавела
        forwardStations.add(new Station(R.string.station_r001_9, LAT_9, LON_9, "r001_fwd_09"));
        // FWD_10: Академіка Шалімова
        forwardStations.add(new Station(R.string.station_r001_10, LAT_10, LON_10, "r001_fwd_10"));
        // FWD_11: НАУ
        forwardStations.add(new Station(R.string.station_r001_11, LAT_11, LON_11, "r001_fwd_11"));
        // FWD_12: Індустріальна
        forwardStations.add(new Station(R.string.station_r001_12, LAT_12, LON_12, "r001_fwd_12"));
        // FWD_13: Олекси Тихого
        forwardStations.add(new Station(R.string.station_r001_13, LAT_13, LON_13, "r001_fwd_13"));
        // FWD_14: Політехнічна
        forwardStations.add(new Station(R.string.station_r001_14, LAT_14, LON_14, "r001_fwd_14"));
        // FWD_15: Площа Галицька
        forwardStations.add(new Station(R.string.station_r001_15, LAT_15, LON_15, "r001_fwd_15"));
        // FWD_16: Старовокзальна (Кінцева)
        forwardStations.add(new Station(R.string.station_r001_16, LAT_16, LON_16, "r001_fwd_16"));


        // --- 2. Станції "Назад" (16 -> 1) ---
        List<Station> backwardStations = new ArrayList<>();

        // !!! КООРДИНАТИ ТА ПОРЯДОК ВИПРАВЛЕНО ДЛЯ ДЗЕРКАЛЬНОГО ВІДОБРАЖЕННЯ !!!

        // BWD_01: Старовокзальна (Початок) - Використовуємо координати FWD_16
        backwardStations.add(new Station(R.string.station_r001_16, LAT_16, LON_16, "r001_bwd_01"));
        // BWD_02: Площа Галицька - Використовуємо координати FWD_15
        backwardStations.add(new Station(R.string.station_r001_15, LAT_15, LON_15, "r001_bwd_02"));
        // BWD_03: Політехнічна - Використовуємо координати FWD_14
        backwardStations.add(new Station(R.string.station_r001_14, LAT_14, LON_14, "r001_bwd_03"));
        // BWD_04: Олекси Тихого - Використовуємо координати FWD_13
        backwardStations.add(new Station(R.string.station_r001_13, LAT_13, LON_13, "r001_bwd_04"));
        // BWD_05: Індустріальна - Використовуємо координати FWD_12
        backwardStations.add(new Station(R.string.station_r001_12, LAT_12, LON_12, "r001_bwd_05"));
        // BWD_06: НАУ - Використовуємо координати FWD_11
        backwardStations.add(new Station(R.string.station_r001_11, LAT_11, LON_11, "r001_bwd_06"));
        // BWD_07: Академіка Шалімова - Використовуємо координати FWD_10
        backwardStations.add(new Station(R.string.station_r001_10, LAT_10, LON_10, "r001_bwd_07"));
        // BWD_08: Вацлава Гавела - Використовуємо координати FWD_09
        backwardStations.add(new Station(R.string.station_r001_9, LAT_9, LON_9, "r001_bwd_08"));
        // BWD_09: Івана Дзюби - Використовуємо координати FWD_08
        backwardStations.add(new Station(R.string.station_r001_8, LAT_8, LON_8, "r001_bwd_09"));
        // BWD_10: Гната Юри - Використовуємо координати FWD_07
        backwardStations.add(new Station(R.string.station_r001_7, LAT_7, LON_7, "r001_bwd_10"));
        // BWD_11: Вулиця Василя Доманицького - Використовуємо координати FWD_06
        backwardStations.add(new Station(R.string.station_r001_6, LAT_6, LON_6, "r001_bwd_11"));
        // BWD_12: Бульвар Миколи Руденка - Використовуємо координати FWD_05
        backwardStations.add(new Station(R.string.station_r001_5, LAT_5, LON_5, "r001_bwd_12"));
        // BWD_13: Вулиця Зодчих - Використовуємо координати FWD_04
        backwardStations.add(new Station(R.string.station_r001_4, LAT_4, LON_4, "r001_bwd_13"));
        // BWD_14: Вулиця Олександра Махова - Використовуємо координати FWD_03
        backwardStations.add(new Station(R.string.station_r001_3, LAT_3, LON_3, "r001_bwd_14"));
        // BWD_15: Вулиця Вахтанга Кікабідзе - Використовуємо координати FWD_02
        backwardStations.add(new Station(R.string.station_r001_2, LAT_2, LON_2, "r001_bwd_15"));
        // BWD_16: Михайлівська Борщагівка (Кінцева) - Використовуємо координати FWD_01
        backwardStations.add(new Station(R.string.station_r001_1, LAT_1, LON_1, "r001_bwd_16"));


        // --- 3. Збереження об'єкта RouteData ---
        // Використовується ваш приклад закодованої полілінії
        String polylineEncodedTram1 = "cktrHczqxDgErg@w[|]mS~f@oSri@ea@oM}V{I_TiqAmQ{eA_Noy@uOabAm`@cvCaCqjA~CibCjLgJ";

        ALL_ROUTES_DATA.put(ROUTE_TRAM_1_KEY, new RouteData(
                forwardStations,
                backwardStations,
                polylineEncodedTram1,
                R.string.text_r001_forward,
                R.string.text_r001_backward
        ));

        // ДОДАЙТЕ СЮДИ ІНШІ МАРШРУТИ (R038, R111, тощо)
    }

    /**
     * Повертає повний об'єкт RouteData для заданого ключа маршруту.
     */
    public static RouteData getRouteData(String routeKey) {
        return ALL_ROUTES_DATA.get(routeKey);
    }

    /**
     * Повертає список станцій для заданого маршруту та напрямку.
     * @param routeKey Ключ маршруту (напр., "R001")
     * @param direction Напрямок ("FORWARD" або "BACKWARD")
     * @return Список об'єктів Station у правильному порядку.
     */
    public static List<Station> getStationsForDirection(String routeKey, String direction) {
        RouteData data = getRouteData(routeKey);
        if (data == null) {
            return new ArrayList<>();
        }

        if (direction.equals(DIRECTION_FORWARD)) {
            return data.getForwardStations();
        } else if (direction.equals(DIRECTION_BACKWARD)) {
            return data.getBackwardStations();
        }
        return new ArrayList<>();
    }

    /**
     * Повертає ID текстового ресурсу (повний текст гіда) для заданого маршруту та напрямку.
     */
    public static int getTextResIdForDirection(String routeKey, String direction) {
        RouteData data = getRouteData(routeKey);
        if (data == null) {
            return 0; // 0 - це безпечний ID, якщо ресурс не знайдено
        }

        if (direction.equals(DIRECTION_FORWARD)) {
            return data.getForwardTextResId();
        } else if (direction.equals(DIRECTION_BACKWARD)) {
            return data.getBackwardTextResId();
        }
        return 0;
    }

}
