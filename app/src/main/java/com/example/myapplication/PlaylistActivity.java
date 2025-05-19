package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

public class PlaylistsActivity extends AppCompatActivity implements PlaylistAdapter.OnPlaylistClickListener {

    private RecyclerView recyclerView;
    private PlaylistAdapter playlistAdapter;
    private ArrayList<Playlist> playlists = new ArrayList<>();
    private Button createPlaylistButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlists);

        recyclerView = findViewById(R.id.playlistsRecyclerView);
        createPlaylistButton = findViewById(R.id.createNewPlaylistButton);

        setupRecyclerView();
        loadPlaylists();

        createPlaylistButton.setOnClickListener(v -> {
            // Показать диалог создания плейлиста
            showCreatePlaylistDialog();
        });
    }

    private void setupRecyclerView() {
        playlistAdapter = new PlaylistAdapter(playlists, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(playlistAdapter);
    }

    private void loadPlaylists() {
        // Здесь должна быть загрузка плейлистов из SharedPreferences или базы данных
        // Временные данные для примера
        Playlist favorites = new Playlist("Избранное");
        playlists.add(favorites);

        Playlist workout = new Playlist("Тренировка");
        playlists.add(workout);

        playlistAdapter.notifyDataSetChanged();
    }

    private void showCreatePlaylistDialog() {
        // Реализация диалога создания плейлиста
    }

    @Override
    public void onPlaylistClick(int position) {
        Intent intent = new Intent(this, PlaylistDetailsActivity.class);
        intent.putExtra("playlist_name", playlists.get(position).getName());
        startActivity(intent);
    }
}
