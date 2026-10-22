"""Draw the pipes-tv-style README banner/card from the app's mask and a real capture.

Usage: python tools/make_readme_art.py FONT_DIRECTORY TV_SCREENSHOT APK
Fonts: Fredoka.ttf and Outfit.ttf from google/fonts (SIL Open Font License).
"""
from pathlib import Path
import argparse

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
MINT = (207, 255, 207)
WHITE = (247, 248, 250)
GRAY = (150, 156, 168)


def font(folder, family, size, weight):
    result = ImageFont.truetype(str(folder / f"{family}.ttf"), size)
    axes = result.get_variation_axes()
    values = [axis["default"] for axis in axes]
    for index, axis in enumerate(axes):
        if axis["name"] == b"Weight":
            values[index] = weight
    result.set_variation_by_axes(values)
    return result


def background(width, height):
    image = Image.new("RGB", (width, height))
    draw = ImageDraw.Draw(image)
    for y in range(height):
        t = y / max(1, height - 1)
        draw.line((0, y, width, y), fill=tuple(round(a + (b - a) * t) for a, b in zip((24, 27, 34), (16, 18, 23))))
    return image


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("fonts", type=Path)
    parser.add_argument("capture", type=Path)
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    out = ROOT / "docs"
    out.mkdir(exist_ok=True)
    logo = Image.open(ROOT / "app/src/main/res/drawable-nodpi/dvd_logo.png").convert("RGBA")
    banner = background(1600, 440)
    draw = ImageDraw.Draw(banner)
    draw.rounded_rectangle((105, 70, 415, 380), 48, fill=(0, 0, 0), outline=(70, 76, 90), width=2)
    logo.thumbnail((258, 220), Image.Resampling.LANCZOS)
    tint = Image.new("RGBA", logo.size, MINT + (255,))
    tint.putalpha(logo.getchannel("A"))
    banner.paste(tint, (260 - logo.width // 2, 225 - logo.height // 2), tint)
    title = font(args.fonts, "Fredoka", 125, 600)
    draw.text((490, 83), "DVD", font=title, fill=MINT)
    draw.text((490 + draw.textlength("DVD ", font=title), 83), "Video", font=title, fill=WHITE)
    draw.text((494, 257), "The bouncing DVD logo, on your TV.", font=font(args.fonts, "Outfit", 46, 600), fill=WHITE)
    draw.text((494, 328), "Measured motion. Native Android. No network.", font=font(args.fonts, "Outfit", 29, 400), fill=GRAY)
    banner.save(out / "banner.png", optimize=True)

    capture = Image.open(args.capture).convert("RGB")
    picture = capture.resize((1420, 799), Image.Resampling.LANCZOS)
    card = background(1600, 1140)
    draw = ImageDraw.Draw(card)
    stats = [("60", "REFERENCE FPS"), ("300", "PX / S EACH AXIS"), ("18", "HELD-OUT FRAMES"), (f"{args.apk.stat().st_size / 1024:.0f} KB", "SIDELOAD APP")]
    for index, (value, label) in enumerate(stats):
        cx = index * 400 + 200
        face = font(args.fonts, "Fredoka", 82, 600)
        draw.text((cx - draw.textlength(value, font=face) / 2, 42), value, font=face, fill=MINT)
        face = font(args.fonts, "Outfit", 23, 500)
        draw.text((cx - draw.textlength(label, font=face) / 2, 157), label, font=face, fill=GRAY)
    card.paste(picture, (90, 250))
    draw.rounded_rectangle((89, 249, 1510, 1049), 10, outline=(52, 57, 68), width=2)
    card.save(out / "card.png", optimize=True)
    capture.resize((1280, 720), Image.Resampling.LANCZOS).save(out / "preview.jpg", quality=92, optimize=True)
    print("Wrote docs/banner.png, docs/card.png and docs/preview.jpg")


if __name__ == "__main__":
    main()
