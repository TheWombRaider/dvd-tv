package io.github.thewombraider.dvdtv;

import static org.junit.Assert.*;

import org.junit.Test;

public class FrameLoopTest {
    private static final class Scheduler implements FrameLoop.Scheduler {
        int posts;
        int cancels;
        int outstanding;

        @Override public void postFrame() {
            assertEquals("Duplicate callback", 0, outstanding);
            outstanding++;
            posts++;
        }

        @Override public void cancelFrame() {
            assertEquals(1, outstanding);
            outstanding--;
            cancels++;
        }

        void deliver(FrameLoop loop, long time) {
            assertEquals(1, outstanding);
            outstanding--;
            loop.onFrame(time);
        }
    }

    @Test public void inactiveDetachedAndHiddenStatesNeverSchedule() {
        Scheduler scheduler = new Scheduler();
        FrameLoop loop = new FrameLoop(new DvdSimulation(), scheduler, () -> fail("Inactive redraw"), 0, false);
        loop.setState(false, true, true, 0);
        loop.setState(true, false, true, 0);
        loop.setState(true, true, false, 0);
        loop.onFrame(1_000_000_000L);
        assertEquals(0, scheduler.posts);
        assertEquals(0, scheduler.cancels);
    }

    @Test public void everyExitGateCancelsAndStaleCallbacksCannotReschedule() {
        boolean[][] exits = {{false, true, true}, {true, false, true}, {true, true, false}};
        for (boolean[] exit : exits) {
            DvdSimulation simulation = new DvdSimulation();
            Scheduler scheduler = new Scheduler();
            int[] redraws = {0};
            FrameLoop loop = new FrameLoop(simulation, scheduler, () -> redraws[0]++, 0, false);
            loop.setState(true, true, true, 0);
            loop.setState(true, true, true, 0);
            assertEquals(1, scheduler.posts);
            scheduler.deliver(loop, 1_000_000_000L);
            assertEquals(60, simulation.tick());
            loop.setState(exit[0], exit[1], exit[2], 1_010_000_000L);
            loop.setState(exit[0], exit[1], exit[2], 2_000_000_000L);
            loop.onFrame(3_000_000_000L);
            assertEquals(0, scheduler.outstanding);
            assertEquals(1, scheduler.cancels);
            assertEquals(2, scheduler.posts);
            assertEquals(1, redraws[0]);
        }
    }

    @Test public void resumeKeepsFractionalTickAndSkipsHiddenTravel() {
        DvdSimulation simulation = new DvdSimulation();
        Scheduler scheduler = new Scheduler();
        FrameLoop loop = new FrameLoop(simulation, scheduler, () -> {}, 300, false);
        loop.setState(true, true, true, 0);
        loop.setState(false, true, true, 10_000_000L);
        assertEquals(300, simulation.tick());
        loop.setState(true, true, true, 100_000_000_000L);
        scheduler.deliver(loop, 100_007_000_000L);
        assertEquals(301, simulation.tick());
        loop.setState(false, false, false, 100_007_000_000L);
        assertEquals(0, scheduler.outstanding);
    }

    @Test public void frozenCaptureSeeksWithoutPostingCallbacks() {
        DvdSimulation simulation = new DvdSimulation();
        Scheduler scheduler = new Scheduler();
        FrameLoop loop = new FrameLoop(simulation, scheduler, () -> fail("Frozen redraw loop"), 600, true);
        loop.setState(true, true, true, 0);
        loop.onFrame(Long.MAX_VALUE);
        loop.setState(false, false, false, Long.MAX_VALUE);
        assertEquals(600, simulation.tick());
        assertEquals(271, simulation.x());
        assertEquals(318, simulation.y());
        assertEquals(0, scheduler.posts);
    }

    @Test public void stoppingDuringRedrawDoesNotRepost() {
        Scheduler scheduler = new Scheduler();
        FrameLoop[] reference = new FrameLoop[1];
        reference[0] = new FrameLoop(new DvdSimulation(), scheduler,
                () -> reference[0].setState(false, false, false, 1_000_000_000L), 0, false);
        reference[0].setState(true, true, true, 0);
        scheduler.deliver(reference[0], 1_000_000_000L);
        assertEquals(0, scheduler.outstanding);
        assertEquals(1, scheduler.posts);
    }
}
