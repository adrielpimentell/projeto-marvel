package com.example.marvel.ui.splash;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.marvel.MainActivity;
import com.example.marvel.data.auth.Session;
import com.example.marvel.ui.auth.LoginActivity;
import com.example.marvel.R;
import com.example.marvel.game.GameBalance;

import java.io.IOException;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable watchdog = this::openHome;

    private TextureView videoView;
    private View curtain;
    private MediaPlayer player;
    private Surface surface;
    private int videoWidth;
    private int videoHeight;

    private boolean leaving;
    private boolean launched;
    private boolean stopped;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            launchHome();
            return;
        }
        setContentView(R.layout.activity_splash);
        enterFullScreen();

        videoView = findViewById(R.id.splash_video);
        curtain = findViewById(R.id.splash_curtain);
        videoView.setSurfaceTextureListener(this);
        findViewById(R.id.splash_root).setOnClickListener(v -> openHome());
        handler.postDelayed(watchdog, GameBalance.SPLASH_MAX_MS);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!stopped) return;
        stopped = false;
        if (leaving) launchHome();
        else openHome();
    }

    @Override
    protected void onStop() {
        stopped = true;
        handler.removeCallbacks(watchdog);
        if (player != null && player.isPlaying()) player.pause();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(watchdog);
        if (curtain != null) curtain.animate().cancel();
        releasePlayer();
        super.onDestroy();
    }

    private void enterFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat bars =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        bars.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        bars.hide(WindowInsetsCompat.Type.systemBars());
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture texture, int width, int height) {
        if (leaving || player != null) return;
        player = new MediaPlayer();
        surface = new Surface(texture);
        try {
            player.setDataSource(this, Uri.parse(
                    "android.resource://" + getPackageName() + "/" + R.raw.splash_intro));
        } catch (IOException | RuntimeException e) {
            openHome();
            return;
        }
        player.setSurface(surface);
        player.setLooping(false);
        player.setVolume(0f, 0f);
        player.setOnVideoSizeChangedListener((mp, w, h) -> {
            videoWidth = w;
            videoHeight = h;
            fitToWidth();
        });
        player.setOnPreparedListener(mp -> {
            if (!leaving && !stopped) mp.start();
        });
        player.setOnCompletionListener(mp -> openHome());
        player.setOnErrorListener((mp, what, extra) -> {
            openHome();
            return true;
        });
        player.prepareAsync();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture texture, int width, int height) {
        fitToWidth();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture texture) {
        releasePlayer();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture texture) {
    }

    private void fitToWidth() {
        int viewWidth = videoView.getWidth();
        int viewHeight = videoView.getHeight();
        if (videoWidth == 0 || videoHeight == 0 || viewWidth == 0 || viewHeight == 0) return;
        Matrix matrix = new Matrix();
        matrix.setScale(1f, heightScale(viewWidth, viewHeight, videoWidth, videoHeight),
                viewWidth / 2f, viewHeight / 2f);
        videoView.setTransform(matrix);
    }

    static float heightScale(int viewWidth, int viewHeight, int videoWidth, int videoHeight) {
        float shownHeight = viewWidth * (float) videoHeight / videoWidth;
        return shownHeight / viewHeight;
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
    }

    private void openHome() {
        if (leaving) return;
        leaving = true;
        handler.removeCallbacks(watchdog);
        if (curtain == null) {
            launchHome();
            return;
        }
        curtain.animate()
                .alpha(1f)
                .setDuration(getResources().getInteger(R.integer.splash_fade_ms))
                .withEndAction(this::launchHome)
                .start();
    }

    private void launchHome() {
        if (launched || stopped) return;
        launched = true;
        releasePlayer();
        ActivityOptions fade = ActivityOptions.makeCustomAnimation(this,
                R.anim.splash_fade_in, R.anim.splash_fade_out);
        Class<?> next = Session.isSignedIn() ? MainActivity.class : LoginActivity.class;
        startActivity(new Intent(this, next), fade.toBundle());
        finish();
    }
}
