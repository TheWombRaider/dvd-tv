package io.github.thewombraider.dvdtv;

import static org.junit.Assert.*;

import org.junit.Test;

public class ViewportTest {
    @Test public void fullHdAndFourKUseTheReferencePlane() {
        Viewport fullHd = new Viewport(1920, 1080);
        assertEquals(1, fullHd.scale, 0);
        assertEquals(431, fullHd.x(431), 0);
        assertEquals(388, fullHd.y(388), 0);
        Viewport fourK = new Viewport(3840, 2160);
        assertEquals(2, fourK.scale, 0);
        assertEquals(862, fourK.x(431), 0);
        assertEquals(776, fourK.y(388), 0);
    }

    @Test public void narrowSmallAndUltrawideViewsKeepTheEntirePlaneAndLogoRatio() {
        int[][] sizes = {{1, 1}, {80, 2000}, {2000, 80}, {720, 1280}, {1024, 768}, {2560, 1080}};
        for (int[] size : sizes) {
            Viewport viewport = new Viewport(size[0], size[1]);
            assertTrue(viewport.left >= 0 && viewport.top >= 0);
            assertTrue(viewport.x(1920) <= size[0] + 0.001);
            assertTrue(viewport.y(1080) <= size[1] + 0.001);
            double width = viewport.x(336) - viewport.x(0);
            double height = viewport.y(148) - viewport.y(0);
            assertEquals(336.0 / 148, width / height, 0.001);
        }
    }

    @Test public void resizeDoesNotResetTickOrDirectionAndRemainsReversible() {
        DvdSimulation simulation = new DvdSimulation();
        simulation.seekToTick(600);
        Viewport before = new Viewport(1920, 1080);
        Viewport narrow = new Viewport(80, 2000);
        assertTrue(narrow.x(simulation.x() + 336) <= 80);
        Viewport after = new Viewport(1920, 1080);
        assertEquals(600, simulation.tick());
        assertEquals(before.x(simulation.x()), after.x(simulation.x()), 0);
        assertEquals(before.y(simulation.y()), after.y(simulation.y()), 0);
        assertEquals(1, simulation.directionX());
    }

    @Test public void zeroAndTransientNegativeSizesRemainFinite() {
        for (int[] size : new int[][] {{0, 0}, {0, 1080}, {1920, 0}, {-1, -1}}) {
            Viewport viewport = new Viewport(size[0], size[1]);
            assertEquals(0, viewport.scale, 0);
            assertTrue(Float.isFinite(viewport.x(431)));
            assertTrue(Float.isFinite(viewport.y(388)));
        }
    }
}
