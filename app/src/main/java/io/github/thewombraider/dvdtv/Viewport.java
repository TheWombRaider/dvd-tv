package io.github.thewombraider.dvdtv;

/** Fits the full reference plane inside the view without changing the mask or its motion lattice. */
final class Viewport {
    final double scale;
    final double left;
    final double top;

    Viewport(int width, int height) {
        int safeWidth = Math.max(0, width);
        int safeHeight = Math.max(0, height);
        scale = Math.min((double) safeWidth / DvdSimulation.WIDTH, (double) safeHeight / DvdSimulation.HEIGHT);
        left = (safeWidth - DvdSimulation.WIDTH * scale) / 2;
        top = (safeHeight - DvdSimulation.HEIGHT * scale) / 2;
    }

    float x(int logicalX) { return (float) (left + logicalX * scale); }
    float y(int logicalY) { return (float) (top + logicalY * scale); }
}
