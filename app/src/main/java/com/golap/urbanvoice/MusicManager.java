package com.golap.urbanvoice;

import android.content.Context;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MusicManager {

    public static final String GENRE_MELODY = "Melody";
    public static final String GENRE_CLASSICAL = "Classical";
    public static final String GENRE_NONE = "None";

    // Списки доступних музичних треків для кожного жанру
    private static final List<String> MELODY_TRACKS = Arrays.asList(
            "melody_1", "melody_2", "melody_3", "melody_4", "melody_5"
    );

    private static final List<String> CLASSICAL_TRACKS = Arrays.asList(
            "classic_1", "classic_2"
    );

    private static final Random random = new Random();

    /**
     * Отримує випадковий аудіо ресурс для вказаного жанру
     */
    public static int getRandomAudioResId(Context context, String genre) {
        List<String> tracks;

        if (GENRE_MELODY.equals(genre)) {
            tracks = MELODY_TRACKS;
        } else if (GENRE_CLASSICAL.equals(genre)) {
            tracks = CLASSICAL_TRACKS;
        } else {
            return 0; // No music
        }

        if (tracks.isEmpty()) return 0;

        // Спробуємо знайти існуючий трек
        for (int i = 0; i < tracks.size(); i++) {
            String trackName = tracks.get(random.nextInt(tracks.size()));
            int resId = context.getResources().getIdentifier(trackName, "raw", context.getPackageName());
            if (resId != 0) {
                return resId;
            }
        }

        return 0; // Жоден трек не знайдено
    }

    /**
     * Отримує конкретний аудіо ресурс за ключем (для аудіо гідів)
     */
    public static int getSpecificAudioResId(Context context, String audioKey) {
        return context.getResources().getIdentifier(audioKey, "raw", context.getPackageName());
    }

    /**
     * Перевіряє, чи існує аудіо файл для вказаного ключа
     */
    public static boolean doesAudioExist(Context context, String audioKey) {
        int resId = context.getResources().getIdentifier(audioKey, "raw", context.getPackageName());
        return resId != 0;
    }
}