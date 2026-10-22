"""Fit the supplied DVD mask into Android TV's 320x180 banner and a square icon."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "app/src/main/res/drawable-nodpi"


def main():
    original = Image.open(RESOURCES / "dvd_logo.png").convert("RGBA")
    for name, dimensions, inset in (("tv_banner", (320, 180), 35), ("app_icon", (96, 96), 10)):
        image = Image.new("RGB", dimensions, (0, 0, 0))
        logo = original.copy()
        logo.thumbnail((dimensions[0] - 2 * inset, dimensions[1] - 2 * inset), Image.Resampling.LANCZOS)
        image.paste(logo, ((dimensions[0] - logo.width) // 2, (dimensions[1] - logo.height) // 2), logo)
        image.save(RESOURCES / f"{name}.png", optimize=True)


if __name__ == "__main__":
    main()
