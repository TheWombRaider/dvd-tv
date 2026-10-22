package io.github.thewombraider.dvdtv;

/** Preview-only intent contract. Numeric extras are whole milliseconds or absolute reference ticks. */
final class PreviewCapture {
    static final String EXTRA_TICK = "capture_tick";
    static final String EXTRA_TIME_MS = "capture_time_ms";
    static final String EXTRA_FREEZE = "capture_freeze";
    static final long MAX_TIME_MS = 10 * 60 * 60 * 1000L;
    static final long MAX_TICK = MAX_TIME_MS * DvdSimulation.TICKS_PER_SECOND / 1000;

    final long tick;
    final boolean frozen;

    private PreviewCapture(long tick, boolean frozen) {
        this.tick = tick;
        this.frozen = frozen;
    }

    static PreviewCapture from(Object tick, Object timeMs, Object freeze) {
        Long explicitTick = integer(tick);
        Long timestamp = integer(timeMs);
        long value = explicitTick != null ? clamp(explicitTick, MAX_TICK)
                : timestamp != null ? clamp(timestamp, MAX_TIME_MS) * DvdSimulation.TICKS_PER_SECOND / 1000
                : 0;
        return new PreviewCapture(value, Boolean.TRUE.equals(freeze));
    }

    private static Long integer(Object value) {
        return value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte
                ? ((Number) value).longValue() : null;
    }

    private static long clamp(long value, long maximum) {
        return Math.max(0, Math.min(maximum, value));
    }
}
