package io.github.thewombraider.dvdtv;

import static org.junit.Assert.*;

import org.junit.Test;

public class ActiveClockTest {
    @Test public void rationalSixtyHzBoundaryDoesNotRoundTheTickDuration() {
        ActiveClock clock = new ActiveClock();
        clock.resume(0);
        assertEquals(0, clock.ticks(16_666_666));
        assertEquals(1, clock.ticks(16_666_667));
        assertEquals(2, clock.ticks(49_999_999));
        assertEquals(3, clock.ticks(50_000_000));
        assertEquals(60, clock.ticks(1_000_000_000L));
    }

    @Test public void pausePreservesPartialTicksAndExcludesHiddenTime() {
        ActiveClock clock = new ActiveClock();
        clock.resume(0);
        clock.pause(10_000_000);
        assertEquals(0, clock.ticks(10_000_000_000L));
        clock.resume(10_000_000_000L);
        assertEquals(1, clock.ticks(10_007_000_000L));
        clock.pause(10_007_000_000L);
        clock.pause(20_000_000_000L);
        clock.resume(20_000_000_000L);
        clock.resume(20_001_000_000L);
        assertEquals(60, clock.ticks(20_983_000_000L));
    }

    @Test public void staleFramesDoNotRollBackTimeOrChangeTheAnchor() {
        ActiveClock clock = new ActiveClock();
        clock.resume(100_000_000);
        assertEquals(6, clock.ticks(200_000_000));
        assertEquals(6, clock.ticks(150_000_000));
        assertEquals(12, clock.ticks(300_000_000));
    }

    @Test public void hugeGapsAndNanotimeWrapDoNotOverflow() {
        ActiveClock clock = new ActiveClock();
        clock.resume(0);
        assertEquals(553_402_322_211L, clock.ticks(Long.MAX_VALUE));
        clock.pause(Long.MAX_VALUE);
        clock.resume(0);
        assertEquals(553_402_322_211L, clock.ticks(1_000_000_000L));
        ActiveClock wrapping = new ActiveClock();
        wrapping.resume(Long.MAX_VALUE - 100_000_000L);
        assertEquals(12, wrapping.ticks(Long.MIN_VALUE + 100_000_000L - 1));
    }

    @Test public void displayRefreshAndDroppedFramesDoNotChangeSimulation() {
        for (int refresh : new int[] {24, 30, 50, 60, 90, 120, 144}) {
            ActiveClock clock = new ActiveClock();
            clock.resume(0);
            DvdSimulation simulation = new DvdSimulation();
            for (int frame = 1; frame <= refresh * 5; frame++) {
                simulation.seekToTick(clock.ticks(frame * 1_000_000_000L / refresh));
            }
            assertEquals(300, simulation.tick());
            assertEquals(1231, simulation.x());
            assertEquals(38, simulation.y());
            assertEquals(3, simulation.collisionCount());
        }
        ActiveClock skipped = new ActiveClock();
        skipped.resume(0);
        assertEquals(300, skipped.ticks(5_000_000_000L));
    }
}
