package io.github.thewombraider.dvdtv;

/** The reference's integer lattice, sampled by absolute 60 Hz tick. */
public final class DvdSimulation {
    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;
    public static final int LOGO_WIDTH = 336;
    public static final int LOGO_HEIGHT = 148;
    public static final int STEP = 5;
    public static final int TICKS_PER_SECOND = 60;

    private final Axis horizontal;
    private final Axis vertical;
    private final long firstCorner;
    private final long cornerPeriod;
    private long tick;

    public DvdSimulation() {
        this(431, 388, 1, 1);
    }

    DvdSimulation(int x, int y, int directionX, int directionY) {
        horizontal = new Axis(x, WIDTH - LOGO_WIDTH, directionX);
        vertical = new Axis(y, HEIGHT - LOGO_HEIGHT, directionY);
        long intersection = -1;
        long period = (long) horizontal.span * vertical.span / gcd(horizontal.span, vertical.span);
        for (long candidate = horizontal.firstBounce;
                candidate < horizontal.firstBounce + period; candidate += horizontal.span) {
            if (candidate >= vertical.firstBounce
                    && (candidate - vertical.firstBounce) % vertical.span == 0) {
                intersection = candidate;
                break;
            }
        }
        firstCorner = intersection;
        cornerPeriod = period;
    }

    public void seekToTick(long tick) {
        if (tick < 0) {
            throw new IllegalArgumentException("Negative tick");
        }
        this.tick = tick;
    }

    public long tick() { return tick; }
    public int x() { return horizontal.position(tick); }
    public int y() { return vertical.position(tick); }
    public int directionX() { return horizontal.direction(tick); }
    public int directionY() { return vertical.direction(tick); }

    public long collisionCount() {
        long corners = firstCorner < 0 ? 0 : events(tick, firstCorner, cornerPeriod);
        return events(tick, horizontal.firstBounce, horizontal.span)
                + events(tick, vertical.firstBounce, vertical.span) - corners;
    }

    public long contactCount() {
        // The recording changes tint on arrival at a peak, before the next tick reverses motion.
        long corners = firstCorner < 0 ? 0 : events(tick, firstCorner - 1, cornerPeriod);
        return events(tick, horizontal.firstBounce - 1, horizontal.span)
                + events(tick, vertical.firstBounce - 1, vertical.span) - corners;
    }

    public int color() { return ColorSequence.at(contactCount()); }

    private static long events(long tick, long first, long period) {
        return tick < first ? 0 : 1 + (tick - first) / period;
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int next = a % b;
            a = b;
            b = next;
        }
        return a;
    }

    private static final class Axis {
        final int minimum;
        final int span;
        final int initialPhase;
        final int initialDirection;
        final int firstBounce;

        Axis(int initial, int maximum, int direction) {
            if (initial < 0 || initial > maximum || (direction != -1 && direction != 1)) {
                throw new IllegalArgumentException("Invalid initial axis state");
            }
            minimum = initial % STEP;
            span = (maximum - minimum) / STEP;
            int index = (initial - minimum) / STEP;
            initialDirection = direction;
            initialPhase = direction > 0 ? index : 2 * span - index;
            firstBounce = (direction > 0 ? span - index : index) + 1;
        }

        int phase(long tick) { return (int) ((initialPhase + tick % (2L * span)) % (2L * span)); }

        int position(long tick) {
            int phase = phase(tick);
            return minimum + STEP * (phase <= span ? phase : 2 * span - phase);
        }

        int direction(long tick) {
            if (tick == 0) return initialDirection;
            int phase = phase(tick);
            return phase > 0 && phase <= span ? 1 : -1;
        }
    }
}
