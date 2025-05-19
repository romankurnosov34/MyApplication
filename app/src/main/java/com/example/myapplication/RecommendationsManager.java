package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RecommendationsManager {
    private static RecommendationsManager instance;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MusicPlayerPrefs";
    private static final String KEY_HISTORY = "play_history";

    private RecommendationsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized RecommendationsManager getInstance(Context context) {
        if (instance == null) {
            instance = new RecommendationsManager(context);
        }
        return instance;
    }

    public void addToHistory(String songId) {
        Set<String> history = sharedPreferences.getStringSet(KEY_HISTORY, new HashSet<>());
        history.add(songId);
        sharedPreferences.edit().putStringSet(KEY_HISTORY, history).apply();
    }

    public ArrayList<Song> getRecommendations(ArrayList<Song> allSongs) {
        // Простая реализация рекомендаций - возвращаем случайные песни
        // В реальном приложении здесь должна быть более сложная логика
        // основанная на истории прослушиваний и предпочтениях пользователя

        ArrayList<Song> recommendations = new ArrayList<>(allSongs);
        Collections.shuffle(recommendations);
        return new ArrayList<>(recommendations.subList(0, Math.min(5, recommendations.size())));
    }
}
