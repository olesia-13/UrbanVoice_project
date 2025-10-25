package com.golap.urbanvoice;

import java.util.Objects;

/**
 * Клас для представлення аудіостанції з географічними координатами.
 * Він є незмінним (immutable) завдяки використанню final полів та
 * містить реалізацію standard Object methods (equals, hashCode, toString).
 */
public class Station {

    private final int nameResId;
    private final double latitude;
    private final double longitude;
    private final String audioResKey;

    /**
     * Конструктор для створення нової станції.
     * @param nameResId ID ресурсу назви (якщо це Android-додаток, це може бути R.string.some_name)
     * @param latitude Географічна широта станції.
     * @param longitude Географічна довгота станції.
     * @param audioResKey Ключ або посилання на аудіоресурс.
     */
    public Station(int nameResId, double latitude, double longitude, String audioResKey) {
        this.nameResId = nameResId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.audioResKey = audioResKey;
    }

    // --- Getter methods ---
    public int getNameResId() { return nameResId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAudioResKey() { return audioResKey; }

    // --- Standard Object method overrides ---

    /**
     * Перевизначення методу equals для коректного порівняння двох об'єктів Station.
     * Два об'єкти вважаються рівними, якщо всі їхні поля (включаючи координати) збігаються.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Station station = (Station) o;
        // Використовуємо Double.compare для безпечного порівняння double значень
        return nameResId == station.nameResId &&
                Double.compare(station.latitude, latitude) == 0 &&
                Double.compare(station.longitude, longitude) == 0 &&
                Objects.equals(audioResKey, station.audioResKey);
    }

    /**
     * Перевизначення методу hashCode.
     * Хеш-коди повинні збігатися для рівних об'єктів (згідно з контрактом equals/hashCode).
     */
    @Override
    public int hashCode() {
        return Objects.hash(nameResId, latitude, longitude, audioResKey);
    }

    /**
     * Перевизначення методу toString для зручного друку та налагодження.
     */
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
