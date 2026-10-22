package io.github.thewombraider.dvdtv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.view.Choreographer;
import android.view.View;

final class DvdView extends View implements Choreographer.FrameCallback {
    private final DvdSimulation simulation = new DvdSimulation();
    private final Bitmap logo;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private final Choreographer choreographer = Choreographer.getInstance();
    private final FrameLoop loop;
    private Viewport viewport = new Viewport(0, 0);
    private boolean active;
    private boolean attached;
    private boolean aggregatedVisible;
    private int lastColor;

    DvdView(Context context, long initialTick, boolean frozen) {
        super(context);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        logo = BitmapFactory.decodeResource(getResources(), R.drawable.dvd_logo, options);
        if (logo == null) throw new IllegalStateException("DVD alpha mask could not be decoded");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        loop = new FrameLoop(simulation, new FrameLoop.Scheduler() {
            @Override public void postFrame() { choreographer.postFrameCallback(DvdView.this); }
            @Override public void cancelFrame() { choreographer.removeFrameCallback(DvdView.this); }
        }, this::invalidate, initialTick, frozen);
    }

    void setActive(boolean active) {
        this.active = active;
        updateState();
    }

    private void updateState() {
        if (loop == null) return; // View can dispatch visibility changes during construction.
        boolean visible = aggregatedVisible && getWindowVisibility() == VISIBLE;
        loop.setState(active, attached, visible, System.nanoTime());
        setKeepScreenOn(active && attached && visible);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        aggregatedVisible = isShown();
        updateState();
    }

    @Override protected void onDetachedFromWindow() {
        attached = false;
        updateState();
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updateState();
    }

    @Override public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        aggregatedVisible = isVisible;
        updateState();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        viewport = new Viewport(width, height);
    }

    @Override public void doFrame(long frameTimeNanos) { loop.onFrame(frameTimeNanos); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);
        if (viewport.scale <= 0) return;
        int color = simulation.color();
        if (color != lastColor) {
            paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            lastColor = color;
        }
        destination.set(viewport.x(simulation.x()), viewport.y(simulation.y()),
                viewport.x(simulation.x() + DvdSimulation.LOGO_WIDTH),
                viewport.y(simulation.y() + DvdSimulation.LOGO_HEIGHT));
        canvas.drawBitmap(logo, null, destination, paint);
    }
}
