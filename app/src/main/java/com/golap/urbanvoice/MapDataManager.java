package com.golap.urbanvoice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapDataManager {

    private static final Map<String, RouteData> ALL_ROUTES_DATA;

    public static final float GEOFENCE_RADIUS_METERS = 50f;
    public static final String ROUTE_TRAM_1_KEY = "R001";
    public static final String ROUTE_TROLLEYBUS_111_KEY = "R111";
    public static final String ROUTE_TROLLEYBUS_38_KEY = "R038";
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

        // -----------------------------------------------------------------
        // 🚨 МАРШРУТ 2: Тролейбус №111 (R111)
        // Пл. Українських Героїв (1) <-> Пл. Дарницька (23)
        // -----------------------------------------------------------------

        // Координати R111 (23 станції)
        final double R111_LAT_1 = 50.439600; final double R111_LON_1 = 30.516380; // Ст. м. “Площа Українських Героїв”
        final double R111_LAT_2 = 50.440994; final double R111_LON_2 = 30.511385; // Університет
        final double R111_LAT_3 = 50.441457; final double R111_LON_3 = 30.511390; // Володимирська вулиця
        final double R111_LAT_4 = 50.443261; final double R111_LON_4 = 30.520735; // Центральний Універмаг
        final double R111_LAT_5 = 50.447115; final double R111_LON_5 = 30.522072; // ст. м. Хрещатик
        final double R111_LAT_6 = 50.449543; final double R111_LON_6 = 30.523213; // Майдан Незалежності
        final double R111_LAT_7 = 50.450927; final double R111_LON_7 = 30.525458; // Алея Героїв Небесної Сотні
        final double R111_LAT_8 = 50.459535; final double R111_LON_8 = 30.523943; // Ст. м. Поштова площа
        final double R111_LAT_9 = 50.470203; final double R111_LON_9 = 30.518838; // Набережно-Хрещатицька
        final double R111_LAT_10 = 50.476486; final double R111_LON_10 = 30.575350; // вул. 20-та Садова
        final double R111_LAT_11 = 50.468751; final double R111_LON_11 = 30.582053; // Вул. 5-та Садова
        final double R111_LAT_12 = 50.467421; final double R111_LON_12 = 30.583257; // Вул. 3-я Садова
        final double R111_LAT_13 = 50.462329; final double R111_LON_13 = 30.587029; // Житловий масив Микільська Слобідка
        final double R111_LAT_14 = 50.462730; final double R111_LON_14 = 30.590176; // Сільгосптехніка
        final double R111_LAT_15 = 50.458940; final double R111_LON_15 = 30.590582; // Вул. Митрополита Андрея Шептицького
        final double R111_LAT_16 = 50.455740; final double R111_LON_16 = 30.593273; // вул. Микільсько-Слобідська
        final double R111_LAT_17 = 50.451483; final double R111_LON_17 = 30.598516; // ст. м. Лівобережна
        final double R111_LAT_18 = 50.447818; final double R111_LON_18 = 30.603175; // вул. Ованеса Туманяна
        final double R111_LAT_19 = 50.444533; final double R111_LON_19 = 30.608248; // вул. Євгена Сверстюка
        final double R111_LAT_20 = 50.440040; final double R111_LON_20 = 30.610552; // станція Русанівка
        final double R111_LAT_21 = 50.439560; final double R111_LON_21 = 30.615757; // вул. Тампере
        final double R111_LAT_22 = 50.440376; final double R111_LON_22 = 30.619584; // ж/к “Комфорт-Таун”
        final double R111_LAT_23 = 50.441474; final double R111_LON_23 = 30.623657; // пл. Дарницька


        // --- 1. Станції "Вперед" (1 -> 23) ---
        List<Station> forwardStationsR111 = new ArrayList<>();

        // FWD_01: Площа Українських Героїв
        forwardStationsR111.add(new Station(R.string.station_r111_1, R111_LAT_1, R111_LON_1, "r111_fwd_01"));
        // FWD_02: Університет
        forwardStationsR111.add(new Station(R.string.station_r111_2, R111_LAT_2, R111_LON_2, "r111_fwd_02"));
        // FWD_03: Володимирська вулиця
        forwardStationsR111.add(new Station(R.string.station_r111_3, R111_LAT_3, R111_LON_3, "r111_fwd_03"));
        // FWD_04: Центральний Універмаг
        forwardStationsR111.add(new Station(R.string.station_r111_4, R111_LAT_4, R111_LON_4, "r111_fwd_04"));
        // FWD_05: ст. м. Хрещатик
        forwardStationsR111.add(new Station(R.string.station_r111_5, R111_LAT_5, R111_LON_5, "r111_fwd_05"));
        // FWD_06: Майдан Незалежності
        forwardStationsR111.add(new Station(R.string.station_r111_6, R111_LAT_6, R111_LON_6, "r111_fwd_06"));
        // FWD_07: Алея Героїв Небесної Сотні
        forwardStationsR111.add(new Station(R.string.station_r111_7, R111_LAT_7, R111_LON_7, "r111_fwd_07"));
        // FWD_08: Ст. м. Поштова площа
        forwardStationsR111.add(new Station(R.string.station_r111_8, R111_LAT_8, R111_LON_8, "r111_fwd_08"));
        // FWD_09: Набережно-Хрещатицька
        forwardStationsR111.add(new Station(R.string.station_r111_9, R111_LAT_9, R111_LON_9, "r111_fwd_09"));
        // FWD_10: вул. 20-та Садова
        forwardStationsR111.add(new Station(R.string.station_r111_10, R111_LAT_10, R111_LON_10, "r111_fwd_10"));
        // FWD_11: Вул. 5-та Садова
        forwardStationsR111.add(new Station(R.string.station_r111_11, R111_LAT_11, R111_LON_11, "r111_fwd_11"));
        // FWD_12: Вул. 3-я Садова
        forwardStationsR111.add(new Station(R.string.station_r111_12, R111_LAT_12, R111_LON_12, "r111_fwd_12"));
        // FWD_13: Житловий масив Микільська Слобідка
        forwardStationsR111.add(new Station(R.string.station_r111_13, R111_LAT_13, R111_LON_13, "r111_fwd_13"));
        // FWD_14: Сільгосптехніка
        forwardStationsR111.add(new Station(R.string.station_r111_14, R111_LAT_14, R111_LON_14, "r111_fwd_14"));
        // FWD_15: Вул. Митрополита Андрея Шептицького
        forwardStationsR111.add(new Station(R.string.station_r111_15, R111_LAT_15, R111_LON_15, "r111_fwd_15"));
        // FWD_16: вул. Микільсько-Слобідська
        forwardStationsR111.add(new Station(R.string.station_r111_16, R111_LAT_16, R111_LON_16, "r111_fwd_16"));
        // FWD_17: ст. м. Лівобережна
        forwardStationsR111.add(new Station(R.string.station_r111_17, R111_LAT_17, R111_LON_17, "r111_fwd_17"));
        // FWD_18: вул. Ованеса Туманяна
        forwardStationsR111.add(new Station(R.string.station_r111_18, R111_LAT_18, R111_LON_18, "r111_fwd_18"));
        // FWD_19: вул. Євгена Сверстюка
        forwardStationsR111.add(new Station(R.string.station_r111_19, R111_LAT_19, R111_LON_19, "r111_fwd_19"));
        // FWD_20: станція Русанівка
        forwardStationsR111.add(new Station(R.string.station_r111_20, R111_LAT_20, R111_LON_20, "r111_fwd_20"));
        // FWD_21: вул. Тампере
        forwardStationsR111.add(new Station(R.string.station_r111_21, R111_LAT_21, R111_LON_21, "r111_fwd_21"));
        // FWD_22: ж/к “Комфорт-Таун”
        forwardStationsR111.add(new Station(R.string.station_r111_22, R111_LAT_22, R111_LON_22, "r111_fwd_22"));
        // FWD_23: пл. Дарницька (Кінцева)
        forwardStationsR111.add(new Station(R.string.station_r111_23, R111_LAT_23, R111_LON_23, "r111_fwd_23"));


        // --- 2. Станції "Назад" (23 -> 1) ---
        List<Station> backwardStationsR111 = new ArrayList<>();

        // BWD_01: пл. Дарницька (Початок) - Використовуємо координати FWD_23
        backwardStationsR111.add(new Station(R.string.station_r111_23, R111_LAT_23, R111_LON_23, "r111_bwd_01"));
        // BWD_02: ж/к “Комфорт-Таун” - Використовуємо координати FWD_22
        backwardStationsR111.add(new Station(R.string.station_r111_22, R111_LAT_22, R111_LON_22, "r111_bwd_02"));
        // BWD_03: вул. Тампере - Використовуємо координати FWD_21
        backwardStationsR111.add(new Station(R.string.station_r111_21, R111_LAT_21, R111_LON_21, "r111_bwd_03"));
        // BWD_04: станція Русанівка - Використовуємо координати FWD_20
        backwardStationsR111.add(new Station(R.string.station_r111_20, R111_LAT_20, R111_LON_20, "r111_bwd_04"));
        // BWD_05: вул. Євгена Сверстюка - Використовуємо координати FWD_19
        backwardStationsR111.add(new Station(R.string.station_r111_19, R111_LAT_19, R111_LON_19, "r111_bwd_05"));
        // BWD_06: вул. Ованеса Туманяна - Використовуємо координати FWD_18
        backwardStationsR111.add(new Station(R.string.station_r111_18, R111_LAT_18, R111_LON_18, "r111_bwd_06"));
        // BWD_07: ст. м. Лівобережна - Використовуємо координати FWD_17
        backwardStationsR111.add(new Station(R.string.station_r111_17, R111_LAT_17, R111_LON_17, "r111_bwd_07"));
        // BWD_08: вул. Микільсько-Слобідська - Використовуємо координати FWD_16
        backwardStationsR111.add(new Station(R.string.station_r111_16, R111_LAT_16, R111_LON_16, "r111_bwd_08"));
        // BWD_09: Вул. Митрополита Андрея Шептицького - Використовуємо координати FWD_15
        backwardStationsR111.add(new Station(R.string.station_r111_15, R111_LAT_15, R111_LON_15, "r111_bwd_09"));
        // BWD_10: Сільгосптехніка - Використовуємо координати FWD_14
        backwardStationsR111.add(new Station(R.string.station_r111_14, R111_LAT_14, R111_LON_14, "r111_bwd_10"));
        // BWD_11: Житловий масив Микільська Слобідка - Використовуємо координати FWD_13
        backwardStationsR111.add(new Station(R.string.station_r111_13, R111_LAT_13, R111_LON_13, "r111_bwd_11"));
        // BWD_12: Вул. 3-я Садова - Використовуємо координати FWD_12
        backwardStationsR111.add(new Station(R.string.station_r111_12, R111_LAT_12, R111_LON_12, "r111_bwd_12"));
        // BWD_13: Вул. 5-та Садова - Використовуємо координати FWD_11
        backwardStationsR111.add(new Station(R.string.station_r111_11, R111_LAT_11, R111_LON_11, "r111_bwd_13"));
        // BWD_14: вул. 20-та Садова - Використовуємо координати FWD_10
        backwardStationsR111.add(new Station(R.string.station_r111_10, R111_LAT_10, R111_LON_10, "r111_bwd_14"));
        // BWD_15: Набережно-Хрещатицька - Використовуємо координати FWD_09
        backwardStationsR111.add(new Station(R.string.station_r111_9, R111_LAT_9, R111_LON_9, "r111_bwd_15"));
        // BWD_16: Ст. м. Поштова площа - Використовуємо координати FWD_08
        backwardStationsR111.add(new Station(R.string.station_r111_8, R111_LAT_8, R111_LON_8, "r111_bwd_16"));
        // BWD_17: Алея Героїв Небесної Сотні - Використовуємо координати FWD_07
        backwardStationsR111.add(new Station(R.string.station_r111_7, R111_LAT_7, R111_LON_7, "r111_bwd_17"));
        // BWD_18: Майдан Незалежності - Використовуємо координати FWD_06
        backwardStationsR111.add(new Station(R.string.station_r111_6, R111_LAT_6, R111_LON_6, "r111_bwd_18"));
        // BWD_19: ст. м. Хрещатик - Використовуємо координати FWD_05
        backwardStationsR111.add(new Station(R.string.station_r111_5, R111_LAT_5, R111_LON_5, "r111_bwd_19"));
        // BWD_20: Центральний Універмаг - Використовуємо координати FWD_04
        backwardStationsR111.add(new Station(R.string.station_r111_4, R111_LAT_4, R111_LON_4, "r111_bwd_20"));
        // BWD_21: Володимирська вулиця - Використовуємо координати FWD_03
        backwardStationsR111.add(new Station(R.string.station_r111_3, R111_LAT_3, R111_LON_3, "r111_bwd_21"));
        // BWD_22: Університет - Використовуємо координати FWD_02
        backwardStationsR111.add(new Station(R.string.station_r111_2, R111_LAT_2, R111_LON_2, "r111_bwd_22"));
        // BWD_23: Ст. м. “Площа Українських Героїв” (Кінцева) - Використовуємо координати FWD_01
        backwardStationsR111.add(new Station(R.string.station_r111_1, R111_LAT_1, R111_LON_1, "r111_bwd_23"));


        // --- 3. Збереження об'єкта RouteData (R111) ---
        // ЗАМІНІТЬ 'polylineEncodedTram1' на закодовану полілінію для TROLLEYBUS_111
        String polylineEncodedTrolleybus111 = "onzrHkfgyDuGd^}A?gJky@aWkGeNcFuGaMyt@nHsaAz^if@e`Jjo@{h@hGqFx^qVoAuRtVoA~RyOrYy_@zUc\\pSu^`[kM~Aq_@cD{VyEoX";

        ALL_ROUTES_DATA.put(ROUTE_TROLLEYBUS_111_KEY, new RouteData(
                forwardStationsR111,
                backwardStationsR111,
                polylineEncodedTrolleybus111,
                R.string.text_r111_forward,
                R.string.text_r111_backward
        ));

        // =================================================================
        // 🚨 МАРШРУТ 3: Тролейбус №38 (R038)
        // National Museum of Ukrainian History in World War II (1) <-> Vydubychi Metro (21)
        // =================================================================

        // Координати R038 (21 станція)
        final double R038_LAT_1 = 50.432605; final double R038_LON_1 = 30.556219; // 1. National Museum of Ukrainian History in World War II
        final double R038_LAT_2 = 50.434985; final double R038_LON_2 = 30.554430; // 2. Kyiv-Pechersk Lavra
        final double R038_LAT_3 = 50.438174; final double R038_LON_3 = 30.553835; // 3. Holodomor Victims Memorial
        final double R038_LAT_4 = 50.439616; final double R038_LON_4 = 30.551053; // 4. Park of Eternal Glory
        final double R038_LAT_5 = 50.441096; final double R038_LON_5 = 30.549513; // 5. Glory Square
        final double R038_LAT_6 = 50.443051; final double R038_LON_6 = 30.547045; // 6. Arsenalna Metro
        final double R038_LAT_7 = 50.440102; final double R038_LON_7 = 30.544874; // 7. Arsenal Factory
        final double R038_LAT_8 = 50.437101; final double R038_LON_8 = 30.544935; // 8. Pechersk District
        final double R038_LAT_9 = 50.431888; final double R038_LON_9 = 30.544236; // 9. Kopylenko Street
        final double R038_LAT_10 = 50.430354; final double R038_LON_10 = 30.542871; // 10. General Almazov Street
        final double R038_LAT_11 = 50.428683; final double R038_LON_11 = 30.539814; // 11. Arsenalna Street
        final double R038_LAT_12 = 50.428184; final double R038_LON_12 = 30.539541; // 12. Lesya Ukrainka Square
        final double R038_LAT_13 = 50.424676; final double R038_LON_13 = 30.542087; // 13. Round Tower
        final double R038_LAT_14 = 50.423273; final double R038_LON_14 = 30.543657; // 14. John McCain Street
        final double R038_LAT_15 = 50.419110; final double R038_LON_15 = 30.548375; // 15. Pechersk Bridge
        final double R038_LAT_16 = 50.417078; final double R038_LON_16 = 30.548817; // 16. Nimanska Street
        final double R038_LAT_17 = 50.412499; final double R038_LON_17 = 30.548927; // 17. Optics Shop
        final double R038_LAT_18 = 50.409914; final double R038_LON_18 = 30.550198; // 18. Boychuk Academy of Arts
        final double R038_LAT_19 = 50.406706; final double R038_LON_19 = 30.552223; // 19. Dormitory No. 4.
        final double R038_LAT_20 = 50.404031; final double R038_LON_20 = 30.551844; // 20. Zaliznychne Highway
        final double R038_LAT_21 = 50.402066; final double R038_LON_21 = 30.559669; // 21. Vydubychi Metro


        // --- 1. Станції "Вперед" (1 -> 21) ---
        List<Station> forwardStationsR038 = new ArrayList<>();

        // FWD_01: National Museum of Ukrainian History in World War II (Початок)
        forwardStationsR038.add(new Station(R.string.station_r038_1, R038_LAT_1, R038_LON_1, "r038_fwd_01"));
        // FWD_02: Kyiv-Pechersk Lavra
        forwardStationsR038.add(new Station(R.string.station_r038_2, R038_LAT_2, R038_LON_2, "r038_fwd_02"));
        // FWD_03: Holodomor Victims Memorial
        forwardStationsR038.add(new Station(R.string.station_r038_3, R038_LAT_3, R038_LON_3, "r038_fwd_03"));
        // FWD_04: Park of Eternal Glory
        forwardStationsR038.add(new Station(R.string.station_r038_4, R038_LAT_4, R038_LON_4, "r038_fwd_04"));
        // FWD_05: Glory Square
        forwardStationsR038.add(new Station(R.string.station_r038_5, R038_LAT_5, R038_LON_5, "r038_fwd_05"));
        // FWD_06: Arsenalna Metro
        forwardStationsR038.add(new Station(R.string.station_r038_6, R038_LAT_6, R038_LON_6, "r038_fwd_06"));
        // FWD_07: Arsenal Factory
        forwardStationsR038.add(new Station(R.string.station_r038_7, R038_LAT_7, R038_LON_7, "r038_fwd_07"));
        // FWD_08: Pechersk District
        forwardStationsR038.add(new Station(R.string.station_r038_8, R038_LAT_8, R038_LON_8, "r038_fwd_08"));
        // FWD_09: Kopylenko Street
        forwardStationsR038.add(new Station(R.string.station_r038_9, R038_LAT_9, R038_LON_9, "r038_fwd_09"));
        // FWD_10: General Almazov Street
        forwardStationsR038.add(new Station(R.string.station_r038_10, R038_LAT_10, R038_LON_10, "r038_fwd_10"));
        // FWD_11: Arsenalna Street
        forwardStationsR038.add(new Station(R.string.station_r038_11, R038_LAT_11, R038_LON_11, "r038_fwd_11"));
        // FWD_12: Lesya Ukrainka Square
        forwardStationsR038.add(new Station(R.string.station_r038_12, R038_LAT_12, R038_LON_12, "r038_fwd_12"));
        // FWD_13: Round Tower
        forwardStationsR038.add(new Station(R.string.station_r038_13, R038_LAT_13, R038_LON_13, "r038_fwd_13"));
        // FWD_14: John McCain Street
        forwardStationsR038.add(new Station(R.string.station_r038_14, R038_LAT_14, R038_LON_14, "r038_fwd_14"));
        // FWD_15: Pechersk Bridge
        forwardStationsR038.add(new Station(R.string.station_r038_15, R038_LAT_15, R038_LON_15, "r038_fwd_15"));
        // FWD_16: Nimanska Street
        forwardStationsR038.add(new Station(R.string.station_r038_16, R038_LAT_16, R038_LON_16, "r038_fwd_16"));
        // FWD_17: Optics Shop
        forwardStationsR038.add(new Station(R.string.station_r038_17, R038_LAT_17, R038_LON_17, "r038_fwd_17"));
        // FWD_18: Boychuk Academy of Arts
        forwardStationsR038.add(new Station(R.string.station_r038_18, R038_LAT_18, R038_LON_18, "r038_fwd_18"));
        // FWD_19: Dormitory No. 4.
        forwardStationsR038.add(new Station(R.string.station_r038_19, R038_LAT_19, R038_LON_19, "r038_fwd_19"));
        // FWD_20: Zaliznychne Highway
        forwardStationsR038.add(new Station(R.string.station_r038_20, R038_LAT_20, R038_LON_20, "r038_fwd_20"));
        // FWD_21: Vydubychi Metro (Кінцева)
        forwardStationsR038.add(new Station(R.string.station_r038_21, R038_LAT_21, R038_LON_21, "r038_fwd_21"));


        // --- 2. Станції "Назад" (21 -> 1) ---
        List<Station> backwardStationsR038 = new ArrayList<>();

        // BWD_01: Vydubychi Metro (Початок) - Використовуємо координати FWD_21
        backwardStationsR038.add(new Station(R.string.station_r038_21, R038_LAT_21, R038_LON_21, "r038_bwd_01"));
        // BWD_02: Zaliznychne Highway - Використовуємо координати FWD_20
        backwardStationsR038.add(new Station(R.string.station_r038_20, R038_LAT_20, R038_LON_20, "r038_bwd_02"));
        // BWD_03: Dormitory No. 4. - Використовуємо координати FWD_19
        backwardStationsR038.add(new Station(R.string.station_r038_19, R038_LAT_19, R038_LON_19, "r038_bwd_03"));
        // BWD_04: Boychuk Academy of Arts - Використовуємо координати FWD_18
        backwardStationsR038.add(new Station(R.string.station_r038_18, R038_LAT_18, R038_LON_18, "r038_bwd_04"));
        // BWD_05: Optics Shop - Використовуємо координати FWD_17
        backwardStationsR038.add(new Station(R.string.station_r038_17, R038_LAT_17, R038_LON_17, "r038_bwd_05"));
        // BWD_06: Nimanska Street - Використовуємо координати FWD_16
        backwardStationsR038.add(new Station(R.string.station_r038_16, R038_LAT_16, R038_LON_16, "r038_bwd_06"));
        // BWD_07: Pechersk Bridge - Використовуємо координати FWD_15
        backwardStationsR038.add(new Station(R.string.station_r038_15, R038_LAT_15, R038_LON_15, "r038_bwd_07"));
        // BWD_08: John McCain Street - Використовуємо координати FWD_14
        backwardStationsR038.add(new Station(R.string.station_r038_14, R038_LAT_14, R038_LON_14, "r038_bwd_08"));
        // BWD_09: Round Tower - Використовуємо координати FWD_13
        backwardStationsR038.add(new Station(R.string.station_r038_13, R038_LAT_13, R038_LON_13, "r038_bwd_09"));
        // BWD_10: Lesya Ukrainka Square - Використовуємо координати FWD_12
        backwardStationsR038.add(new Station(R.string.station_r038_12, R038_LAT_12, R038_LON_12, "r038_bwd_10"));
        // BWD_11: Arsenalna Street - Використовуємо координати FWD_11
        backwardStationsR038.add(new Station(R.string.station_r038_11, R038_LAT_11, R038_LON_11, "r038_bwd_11"));
        // BWD_12: General Almazov Street - Використовуємо координати FWD_10
        backwardStationsR038.add(new Station(R.string.station_r038_10, R038_LAT_10, R038_LON_10, "r038_bwd_12"));
        // BWD_13: Kopylenko Street - Використовуємо координати FWD_09
        backwardStationsR038.add(new Station(R.string.station_r038_9, R038_LAT_9, R038_LON_9, "r038_bwd_13"));
        // BWD_14: Pechersk District - Використовуємо координати FWD_08
        backwardStationsR038.add(new Station(R.string.station_r038_8, R038_LAT_8, R038_LON_8, "r038_bwd_14"));
        // BWD_15: Arsenal Factory - Використовуємо координати FWD_07
        backwardStationsR038.add(new Station(R.string.station_r038_7, R038_LAT_7, R038_LON_7, "r038_bwd_15"));
        // BWD_16: Arsenalna Metro - Використовуємо координати FWD_06
        backwardStationsR038.add(new Station(R.string.station_r038_6, R038_LAT_6, R038_LON_6, "r038_bwd_16"));
        // BWD_17: Glory Square - Використовуємо координати FWD_05
        backwardStationsR038.add(new Station(R.string.station_r038_5, R038_LAT_5, R038_LON_5, "r038_bwd_17"));
        // BWD_18: Park of Eternal Glory - Використовуємо координати FWD_04
        backwardStationsR038.add(new Station(R.string.station_r038_4, R038_LAT_4, R038_LON_4, "r038_bwd_18"));
        // BWD_19: Holodomor Victims Memorial - Використовуємо координати FWD_03
        backwardStationsR038.add(new Station(R.string.station_r038_3, R038_LAT_3, R038_LON_3, "r038_bwd_19"));
        // BWD_20: Kyiv-Pechersk Lavra - Використовуємо координати FWD_02
        backwardStationsR038.add(new Station(R.string.station_r038_2, R038_LAT_2, R038_LON_2, "r038_bwd_20"));
        // BWD_21: National Museum of Ukrainian History in World War II (Кінцева) - Використовуємо координати FWD_01
        backwardStationsR038.add(new Station(R.string.station_r038_1, R038_LAT_1, R038_LON_1, "r038_bwd_21"));


        // --- 3. Збереження об'єкта RouteData (R038) ---
        // ЗАМІНІТЬ 'key' на закодовану полілінію для TROLLEYBUS_38
        String polylineEncodedTrolleybus38 = "ybyrHk_oyDyMdJ}RvBiQ~YeKjNlQrLvQKp_@hCrHpGlIbR~WgMxGyH~Xo\\tKwAr[UdO}F~RsKvOjAfK}o@";

        ALL_ROUTES_DATA.put(ROUTE_TROLLEYBUS_38_KEY, new RouteData(
                forwardStationsR038,
                backwardStationsR038,
                polylineEncodedTrolleybus38,
                R.string.text_r038_forward,
                R.string.text_r038_backward
        ));





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
