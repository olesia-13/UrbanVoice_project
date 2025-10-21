package com.golap.urbanvoice;

public class Route
{
    private final String type;       // "Тролейбус", "Електричка", "Автобус", "Трамвай"
    private final String number;     // "№111", "№38", "№1", etc.
    private final String description; // "ст. м. Площа українських героїв - пл. Дарницька"
    private final String searchName;  // Об'єднана назва для пошуку, наприклад: "Тролейбус 30"
    private final String key;        // Унікальний ключ для карти: "T30_A_B" або "T30_B_A"
    private final int iconResId;     // ID іконки транспорту (Drawable)

    public Route(String type, String number, String description, String key, int iconResId) {
        this.type = type;
        this.number = number;
        this.description = description;
        this.key = key;
        this.iconResId = iconResId;
        this.searchName = type + " " + number.replace("№", "");
    }


    public String getType() { return type; }
    public String getNumber() { return number; }
    public String getDescription() { return description; }
    public String getSearchName() { return searchName; }
    public String getKey() { return key; }
    public int getIconResId() { return iconResId; }

    public String getDisplayText() {
        return type + " " + number;
    }
}
