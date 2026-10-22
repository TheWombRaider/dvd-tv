package io.github.thewombraider.dvdtv;

/** Counts only visible, active time, retaining fractions of a simulation tick across pauses. */
final class ActiveClock {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private long activeNanos;
    private long lastNanos;
    private boolean running;

    void resume(long nowNanos) {
        if (!running) {
            lastNanos = nowNanos;
            running = true;
        }
    }

    void pause(long nowNanos) {
        update(nowNanos);
        running = false;
    }

    long ticks(long nowNanos) {
        update(nowNanos);
        // Dividing first avoids overflow and the drift of a rounded 16,666,667 ns period.
        return activeNanos / NANOS_PER_SECOND * DvdSimulation.TICKS_PER_SECOND
                + activeNanos % NANOS_PER_SECOND * DvdSimulation.TICKS_PER_SECOND / NANOS_PER_SECOND;
    }

    private void update(long nowNanos) {
        if (!running) return;
        long delta = nowNanos - lastNanos;
        if (delta < 0) return;
        activeNanos = delta > Long.MAX_VALUE - activeNanos ? Long.MAX_VALUE : activeNanos + delta;
        lastNanos = nowNanos;
    }
}
