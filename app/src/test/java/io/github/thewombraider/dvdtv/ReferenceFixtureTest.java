package io.github.thewombraider.dvdtv;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ReferenceFixtureTest {
    @Test public void independentHeldOutFramesMatchGeometryAndMeasuredColors() throws Exception {
        DvdSimulation simulation = new DvdSimulation();
        int count = 0;
        for (String[] sample : samples()) {
            if (!sample[1].equals("held-out")) continue;
            int tick = Integer.parseInt(sample[0]);
            simulation.seekToTick(tick);
            int[] actual = {simulation.x(), simulation.y(), simulation.x() + 336, simulation.y() + 148};
            for (int edge = 0; edge < actual.length; edge++) {
                assertEquals("Reference frame " + tick + ", edge " + edge,
                        Integer.parseInt(sample[edge + 2]), actual[edge], 1);
            }
            int color = simulation.color();
            for (int channel = 0; channel < 3; channel++) {
                assertEquals("Reference frame " + tick + ", RGB channel " + channel,
                        Integer.parseInt(sample[channel + 6]), color >>> (16 - channel * 8) & 255, 3);
            }
            count++;
        }
        assertTrue("Held-out samples must be supplied independently", count >= 18);
        System.out.println("Independent held-out reference: " + count + " frames, <=1px geometry, <=3 RGB levels");
    }

    private static List<String[]> samples() throws Exception {
        InputStream stream = ReferenceFixtureTest.class.getResourceAsStream("/reference-samples.tsv");
        assertNotNull("Missing coordinator reference measurements", stream);
        List<String[]> samples = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null;) samples.add(line.split("\t"));
        }
        return samples;
    }
}
