package io.github.thewombraider.dvdtv;

import static org.junit.Assert.*;

import org.junit.Test;

public class PreviewCaptureTest {
    @Test public void normalPreviewStartsAtZeroAndRuns() {
        PreviewCapture capture = PreviewCapture.from(null, null, null);
        assertEquals(0, capture.tick);
        assertFalse(capture.frozen);
    }

    @Test public void tickHasPriorityAndFreezeIsExplicit() {
        PreviewCapture capture = PreviewCapture.from(777L, 5000L, true);
        assertEquals(777, capture.tick);
        assertTrue(capture.frozen);
        assertFalse(PreviewCapture.from(0L, null, "true").frozen);
    }

    @Test public void timestampUsesFloorAndAvoidsRoundedTickDrift() {
        assertEquals(300, PreviewCapture.from(null, 5000L, true).tick);
        assertEquals(0, PreviewCapture.from(null, 16, true).tick);
        assertEquals(1, PreviewCapture.from(null, 17, true).tick);
        assertEquals(3, PreviewCapture.from(null, 50, true).tick);
    }

    @Test public void negativeAndHugeExtrasAreClampedBeforeArithmetic() {
        assertEquals(0, PreviewCapture.from(Long.MIN_VALUE, null, true).tick);
        assertEquals(0, PreviewCapture.from(null, Long.MIN_VALUE, true).tick);
        assertEquals(2_160_000, PreviewCapture.from(Long.MAX_VALUE, null, true).tick);
        assertEquals(2_160_000, PreviewCapture.from(null, Long.MAX_VALUE, true).tick);
    }

    @Test public void malformedTypesFallBackWithoutParsingOrOverflow() {
        assertEquals(60, PreviewCapture.from("777", 1000, false).tick);
        assertEquals(0, PreviewCapture.from(Double.NaN, Double.POSITIVE_INFINITY, new Object()).tick);
        assertEquals(0, PreviewCapture.from(1.5, "5000", null).tick);
    }
}
