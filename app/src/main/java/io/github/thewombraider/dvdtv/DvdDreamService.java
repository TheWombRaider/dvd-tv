package io.github.thewombraider.dvdtv;

import android.service.dreams.DreamService;

public final class DvdDreamService extends DreamService {
    private DvdView dvdView;

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        // Capture extras are deliberately confined to PreviewActivity.
        dvdView = new DvdView(this, 0, false);
        setContentView(dvdView);
    }

    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        setScreenBright(true);
        if (dvdView != null) dvdView.setActive(true);
    }

    @Override public void onDreamingStopped() {
        stopAnimation();
        super.onDreamingStopped();
    }

    @Override public void onDetachedFromWindow() {
        stopAnimation();
        dvdView = null;
        super.onDetachedFromWindow();
    }

    @Override public void onDestroy() {
        stopAnimation();
        super.onDestroy();
    }

    private void stopAnimation() {
        if (dvdView != null) dvdView.setActive(false);
        setScreenBright(false);
    }
}
