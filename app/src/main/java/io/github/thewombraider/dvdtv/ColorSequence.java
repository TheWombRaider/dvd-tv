package io.github.thewombraider.dvdtv;

final class ColorSequence {
    private ColorSequence() {}

    static int at(long collisionCount) {
        if (collisionCount < 0) {
            throw new IllegalArgumentException("Negative collision count");
        }
        if (collisionCount < MeasuredPalette.COLORS.length) {
            return MeasuredPalette.COLORS[(int) collisionCount];
        }

        long index = collisionCount - MeasuredPalette.COLORS.length + 1;
        long bits = index + 0x9e3779b97f4a7c15L;
        bits = (bits ^ (bits >>> 30)) * 0xbf58476d1ce4e5b9L;
        bits = (bits ^ (bits >>> 27)) * 0x94d049bb133111ebL;
        bits ^= bits >>> 31;
        // A changing red channel prevents adjacent repeats without replaying earlier colors.
        int red = 100 + (int) ((index % 156) * 73 % 156);
        int green = 100 + (int) Long.remainderUnsigned(bits, 156);
        int blue = 100 + (int) Long.remainderUnsigned(bits >>> 32, 156);
        int color = 0xff000000 | red << 16 | green << 8 | blue;
        if (index == 1 && color == MeasuredPalette.COLORS[MeasuredPalette.COLORS.length - 1]) {
            color ^= 1;
        }
        return color;
    }
}
