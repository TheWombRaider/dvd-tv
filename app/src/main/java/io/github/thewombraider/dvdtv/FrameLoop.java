package io.github.thewombraider.dvdtv;

/** Owns the single pending frame callback, independent of Activity and DreamService lifecycles. */
final class FrameLoop {
    interface Scheduler {
        void postFrame();
        void cancelFrame();
    }

    private final DvdSimulation simulation;
    private final Scheduler scheduler;
    private final Runnable redraw;
    private final ActiveClock clock = new ActiveClock();
    private final long initialTick;
    private final boolean frozen;
    private boolean running;
    private boolean pending;

    FrameLoop(DvdSimulation simulation, Scheduler scheduler, Runnable redraw, long initialTick, boolean frozen) {
        this.simulation = simulation;
        this.scheduler = scheduler;
        this.redraw = redraw;
        this.initialTick = initialTick;
        this.frozen = frozen;
        simulation.seekToTick(initialTick);
    }

    void setState(boolean active, boolean attached, boolean visible, long nowNanos) {
        boolean shouldRun = active && attached && visible && !frozen;
        if (shouldRun == running) return;
        running = shouldRun;
        if (running) {
            clock.resume(nowNanos);
            post();
        } else {
            clock.pause(nowNanos);
            update(nowNanos);
            if (pending) scheduler.cancelFrame();
            pending = false;
        }
    }

    void onFrame(long nowNanos) {
        if (!pending || !running) return;
        pending = false;
        update(nowNanos);
        redraw.run();
        if (running) post();
    }

    private void update(long nowNanos) {
        long elapsedTicks = clock.ticks(nowNanos);
        simulation.seekToTick(elapsedTicks > Long.MAX_VALUE - initialTick ? Long.MAX_VALUE : initialTick + elapsedTicks);
    }

    private void post() {
        if (!pending) {
            pending = true;
            scheduler.postFrame();
        }
    }
}
