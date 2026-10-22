package io.github.thewombraider.dvdtv;

import static org.junit.Assert.*;

import org.junit.Test;

public class DvdSimulationTest {
    @Test public void initialFrameMatchesMeasuredFootprint() {
        DvdSimulation simulation = new DvdSimulation();
        assertEquals(431, simulation.x());
        assertEquals(388, simulation.y());
        assertEquals(0xffffffff, simulation.color());
        assertEquals(0, simulation.collisionCount());
    }

    @Test public void reversesBeforeLeavingTheLatticeWithoutClampingToTheWall() {
        DvdSimulation simulation = new DvdSimulation();
        simulation.seekToTick(108);
        assertEquals(928, simulation.y());
        assertEquals(1, simulation.directionY());
        assertEquals(0, simulation.collisionCount());
        assertEquals(1, simulation.contactCount());
        assertEquals(0xff969397, simulation.color());
        simulation.seekToTick(109);
        assertEquals(923, simulation.y());
        assertEquals(-1, simulation.directionY());
        assertEquals(1, simulation.collisionCount());
        assertEquals(0xff969397, simulation.color());
        simulation.seekToTick(230);
        assertEquals(1581, simulation.x());
        assertEquals(2, simulation.contactCount());
        assertEquals(MeasuredPalette.COLORS[2], simulation.color());
        simulation.seekToTick(231);
        assertEquals(1576, simulation.x());
        assertEquals(-1, simulation.directionX());
        simulation.seekToTick(293);
        assertEquals(3, simulation.y());
        assertEquals(3, simulation.contactCount());
        simulation.seekToTick(294);
        assertEquals(8, simulation.y());
    }

    @Test public void colorChangesOnContactPeaksIncludingTheTwoTickBriefState() {
        DvdSimulation simulation = new DvdSimulation();
        for (long peak : new long[] {108, 230, 293, 5286, 5288}) {
            simulation.seekToTick(peak - 1);
            long before = simulation.contactCount();
            int oldColor = simulation.color();
            simulation.seekToTick(peak);
            assertEquals(before + 1, simulation.contactCount());
            assertNotEquals(oldColor, simulation.color());
            int contactColor = simulation.color();
            simulation.seekToTick(peak + 1);
            assertEquals(contactColor, simulation.color());
        }
    }

    @Test public void simultaneousCornerReflectsBothAxesAndChangesColorOnce() {
        DvdSimulation simulation = new DvdSimulation(1581, 928, 1, 1);
        simulation.seekToTick(1);
        assertEquals(1576, simulation.x());
        assertEquals(923, simulation.y());
        assertEquals(-1, simulation.directionX());
        assertEquals(-1, simulation.directionY());
        assertEquals(1, simulation.collisionCount());
        assertEquals(0xff969397, simulation.color());
        simulation = new DvdSimulation(1, 3, -1, -1);
        simulation.seekToTick(1);
        assertEquals(6, simulation.x());
        assertEquals(8, simulation.y());
        assertEquals(1, simulation.collisionCount());
    }

    @Test public void closedFormMatchesAnIndependentPreemptiveStepperIncludingCorners() {
        int[][] starts = {{431, 388, 1, 1}, {1581, 928, 1, 1}, {0, 0, -1, -1}, {1584, 932, 1, 1}, {140, 217, -1, 1}};
        for (int[] start : starts) {
            DvdSimulation simulation = new DvdSimulation(start[0], start[1], start[2], start[3]);
            int x = start[0], y = start[1], dx = start[2], dy = start[3];
            long collisions = 0;
            for (int tick = 1; tick <= 150_000; tick++) {
                boolean hitX = x + dx * 5 < 0 || x + dx * 5 + 336 > 1920;
                boolean hitY = y + dy * 5 < 0 || y + dy * 5 + 148 > 1080;
                if (hitX) dx = -dx;
                if (hitY) dy = -dy;
                if (hitX || hitY) collisions++;
                x += dx * 5;
                y += dy * 5;
                simulation.seekToTick(tick);
                assertEquals(x, simulation.x());
                assertEquals(y, simulation.y());
                assertEquals(dx, simulation.directionX());
                assertEquals(dy, simulation.directionY());
                assertEquals(collisions, simulation.collisionCount());
            }
        }
    }

    @Test(timeout = 1000) public void extremeElapsedTimeIsBoundedAndCanSeekBackwards() {
        DvdSimulation simulation = new DvdSimulation();
        for (long tick : new long[] {2_160_000L, 60L * 60 * 24 * 365 * 100, Long.MAX_VALUE}) {
            simulation.seekToTick(tick);
            assertTrue(simulation.x() >= 0 && simulation.x() + 336 <= 1920);
            assertTrue(simulation.y() >= 0 && simulation.y() + 148 <= 1080);
            assertTrue(simulation.collisionCount() > 0);
            assertEquals(255, simulation.color() >>> 24);
        }
        simulation.seekToTick(300);
        assertEquals(1231, simulation.x());
        assertEquals(38, simulation.y());
        assertEquals(0xffc7f766, simulation.color());
    }

    @Test public void paletteEndsWithMeasuredStateThenUsesDeterministicPastels() {
        assertEquals(78, MeasuredPalette.COLORS.length);
        assertEquals(0xffbaedb9, ColorSequence.at(77));
        int previous = ColorSequence.at(77);
        boolean differsFromRecordedLoop = false;
        for (long collision = 78; collision < 20_000; collision++) {
            int color = ColorSequence.at(collision);
            assertEquals(color, ColorSequence.at(collision));
            assertNotEquals(previous, color);
            assertEquals(255, color >>> 24);
            assertTrue((color >> 16 & 255) >= 100);
            assertTrue((color >> 8 & 255) >= 100);
            assertTrue((color & 255) >= 100);
            differsFromRecordedLoop |= color != MeasuredPalette.COLORS[(int) (collision % 78)];
            previous = color;
        }
        assertTrue(differsFromRecordedLoop);
        assertEquals(ColorSequence.at(Long.MAX_VALUE), ColorSequence.at(Long.MAX_VALUE));
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsNegativeTicks() {
        new DvdSimulation().seekToTick(-1);
    }
}
