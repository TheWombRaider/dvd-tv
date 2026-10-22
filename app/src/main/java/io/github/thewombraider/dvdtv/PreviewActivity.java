package io.github.thewombraider.dvdtv;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

public final class PreviewActivity extends Activity {
    private DvdView dvdView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreviewCapture capture = readCapture();
        dvdView = new DvdView(this, capture.tick, capture.frozen);
        dvdView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(dvdView);
    }

    @SuppressWarnings("deprecation")
    private PreviewCapture readCapture() {
        try {
            Bundle extras = getIntent().getExtras();
            return extras == null ? PreviewCapture.from(null, null, null)
                    : PreviewCapture.from(extras.get(PreviewCapture.EXTRA_TICK),
                            extras.get(PreviewCapture.EXTRA_TIME_MS), extras.get(PreviewCapture.EXTRA_FREEZE));
        } catch (RuntimeException malformedExtras) {
            Log.w("DvdPreview", "Ignoring unreadable preview capture extras");
            return PreviewCapture.from(null, null, null);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        dvdView.setActive(true);
    }

    @Override protected void onPause() {
        dvdView.setActive(false);
        super.onPause();
    }

    @Override protected void onDestroy() {
        dvdView.setActive(false);
        super.onDestroy();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && dismissesPreview(event.getKeyCode())) {
            dvdView.setActive(false);
            finish();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private static boolean dismissesPreview(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_MENU:
                return true;
            default:
                return false;
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            dvdView.setActive(false);
            finish();
            return true;
        }
        return super.onTouchEvent(event);
    }
}
