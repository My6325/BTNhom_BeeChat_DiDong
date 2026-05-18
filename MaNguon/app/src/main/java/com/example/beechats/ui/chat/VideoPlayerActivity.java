package com.example.beechats.ui.chat;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.beechats.R;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URL = "extra_video_url";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        VideoView videoView = findViewById(R.id.videoView);
        String videoUrl = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            Toast.makeText(this, "Không có link video", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(Uri.parse(videoUrl.trim()));
        videoView.setKeepScreenOn(true);
        videoView.setZOrderOnTop(false);

        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            videoView.setVisibility(View.VISIBLE);
            videoView.start();
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(this, "Không phát được video", Toast.LENGTH_SHORT).show();
            return true;
        });

        videoView.requestFocus();
    }
}
