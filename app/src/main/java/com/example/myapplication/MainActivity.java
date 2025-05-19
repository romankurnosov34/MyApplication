package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SongAdapter.OnItemClickListener {

    private static final int REQUEST_PERMISSION_CODE = 1;
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private ArrayList<Song> songList = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private int currentSongIndex = -1;
    private boolean isPlaying = false;
    private SeekBar seekBar;
    private Handler handler = new Handler();
    private Runnable updateSeekBar;
    private TextView currentTimeTextView, totalTimeTextView;
    private ImageButton playPauseButton, prevButton, nextButton, shuffleButton, repeatButton;
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    private EditText searchEditText;
    private Button createPlaylistButton, showPlaylistsButton, loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация UI элементов
        initViews();

        // Проверка и запрос разрешений
        checkPermissions();

        // Настройка RecyclerView
        setupRecyclerView();

        // Загрузка песен
        loadSongs();

        // Настройка MediaPlayer
        setupMediaPlayer();

        // Настройка слушателей
        setupListeners();

        // Настройка поиска
        setupSearch();

        // Проверка авторизации
        checkAuth();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        seekBar = findViewById(R.id.seekBar);
        currentTimeTextView = findViewById(R.id.currentTimeTextView);
        totalTimeTextView = findViewById(R.id.totalTimeTextView);
        playPauseButton = findViewById(R.id.playPauseButton);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        shuffleButton = findViewById(R.id.shuffleButton);
        repeatButton = findViewById(R.id.repeatButton);
        searchEditText = findViewById(R.id.searchEditText);
        createPlaylistButton = findViewById(R.id.createPlaylistButton); // Исправлено здесь
        showPlaylistsButton = findViewById(R.id.showPlaylistsButton);
        loginButton = findViewById(R.id.loginButton);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_CODE);
        }
    }

    private void setupRecyclerView() {
        songAdapter = new SongAdapter(songList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(songAdapter);
    }

    private void loadSongs() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        Cursor cursor = getContentResolver().query(uri, projection, selection, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Song song = new Song(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3)
                );
                songList.add(song);
            }
            cursor.close();
        }

        songAdapter.notifyDataSetChanged();
    }

    private void setupMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> {
            if (isRepeat) {
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            } else {
                playNextSong();
            }
        });
    }

    private void setupListeners() {
        playPauseButton.setOnClickListener(v -> {
            if (currentSongIndex != -1) {
                if (isPlaying) {
                    pauseSong();
                } else {
                    playSong(currentSongIndex);
                }
            }
        });

        prevButton.setOnClickListener(v -> playPrevSong());

        nextButton.setOnClickListener(v -> playNextSong());

        shuffleButton.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            shuffleButton.setColorFilter(isShuffle ?
                    ContextCompat.getColor(this, R.color.colorAccent) :
                    ContextCompat.getColor(this, android.R.color.darker_gray));
        });

        repeatButton.setOnClickListener(v -> {
            isRepeat = !isRepeat;
            repeatButton.setColorFilter(isRepeat ?
                    ContextCompat.getColor(this, R.color.colorAccent) :
                    ContextCompat.getColor(this, android.R.color.darker_gray));
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                handler.postDelayed(updateSeekBar, 100);
            }
        });

        createPlaylistButton.setOnClickListener(v -> {
            // Реализация создания плейлиста
            showCreatePlaylistDialog();
        });

        showPlaylistsButton.setOnClickListener(v -> {
            // Переход к активности плейлистов
            Intent intent = new Intent(MainActivity.this, PlaylistsActivity.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(v -> {
            if (AuthManager.getInstance(this).isLoggedIn()) {
                AuthManager.getInstance(this).logout();
                loginButton.setText("Войти");
                Toast.makeText(this, "Вы вышли из системы", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSongs(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkAuth() {
        if (AuthManager.getInstance(this).isLoggedIn()) {
            loginButton.setText("Выйти");
        } else {
            loginButton.setText("Войти");
        }
    }

    private void filterSongs(String query) {
        ArrayList<Song> filteredList = new ArrayList<>();
        for (Song song : songList) {
            if (song.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    song.getArtist().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(song);
            }
        }
        songAdapter.filterList(filteredList);
    }

    private void showCreatePlaylistDialog() {
        // Реализация диалога создания плейлиста
        // Можно использовать AlertDialog или создать кастомный диалог
    }

    @Override
    public void onItemClick(int position) {
        playSong(position);
    }

    private void playSong(int position) {
        if (position >= 0 && position < songList.size()) {
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(songList.get(position).getPath());
                mediaPlayer.prepare();
                mediaPlayer.start();

                currentSongIndex = position;
                isPlaying = true;
                playPauseButton.setImageResource(R.drawable.ic_pause);

                updateSeekBar();

                // Обновление UI с информацией о текущей песне
                updateNowPlayingInfo();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void pauseSong() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            playPauseButton.setImageResource(R.drawable.ic_play);
            handler.removeCallbacks(updateSeekBar);
        }
    }

    private void playNextSong() {
        if (songList.size() > 0) {
            if (isShuffle) {
                Random random = new Random();
                int newIndex;
                do {
                    newIndex = random.nextInt(songList.size());
                } while (newIndex == currentSongIndex && songList.size() > 1);
                playSong(newIndex);
            } else {
                playSong((currentSongIndex + 1) % songList.size());
            }
        }
    }

    private void playPrevSong() {
        if (songList.size() > 0) {
            if (mediaPlayer.getCurrentPosition() > 3000) {
                mediaPlayer.seekTo(0);
            } else {
                if (isShuffle) {
                    Random random = new Random();
                    int newIndex;
                    do {
                        newIndex = random.nextInt(songList.size());
                    } while (newIndex == currentSongIndex && songList.size() > 1);
                    playSong(newIndex);
                } else {
                    playSong((currentSongIndex - 1 + songList.size()) % songList.size());
                }
            }
        }
    }

    private void updateNowPlayingInfo() {
        if (currentSongIndex != -1) {
            Song currentSong = songList.get(currentSongIndex);
            // Можно обновлять TextView с названием и исполнителем
        }
    }

    private void updateSeekBar() {
        seekBar.setMax(mediaPlayer.getDuration());
        totalTimeTextView.setText(formatTime(mediaPlayer.getDuration()));

        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    currentTimeTextView.setText(formatTime(currentPosition));
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.postDelayed(updateSeekBar, 0);
    }

    private String formatTime(long milliseconds) {
        int seconds = (int) (milliseconds / 1000) % 60;
        int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateSeekBar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAuth();
    }
}