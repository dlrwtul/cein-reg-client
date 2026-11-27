#!/usr/bin/env python3
"""Generate registration-client/src/main/resources/images_cein/ (CEIN image theme).

The client looks for an image in images_<theme>/ first and falls back to images/.
Only recoloured icons are written here:
  - MOSIP magenta / pink icons -> CEIN green (white and grey details are kept)
  - white header icons (shown on the white CEIN top bar) -> ink
Run: python3 tools/theme/gen-images.py   (requires Pillow)
"""
import pathlib
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / "registration/registration-client/src/main/resources/images"
OUT = ROOT / "registration/registration-client/src/main/resources/images_cein"
GREEN = (0, 107, 51)
INK = (46, 59, 71)
HEADER_ICONS = ["User.png", "RegCentreLocation.png", "system.png", "hamburger.png", "Home-small.png"]


def pink_to_green(im):
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            s = (r - min(g, b)) / 255.0  # "pinkness" of the pixel
            k = max(0.0, min(1.0, s / 0.72))
            if k == 0:
                continue
            base = (r + g + b) / 3.0 if k < 1 else 255
            nr = round(base * (1 - k) + GREEN[0] * k)
            ng = round(base * (1 - k) + GREEN[1] * k)
            nb = round(base * (1 - k) + GREEN[2] * k)
            px[x, y] = (nr, ng, nb, a)
    return im


def to_ink(im):
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (INK[0], INK[1], INK[2], a)
    return im


def is_pink(im):
    pts = [p for p in im.getdata() if p[3] > 128]
    if not pts:
        return False
    r = sum(p[0] for p in pts) / len(pts)
    g = sum(p[1] for p in pts) / len(pts)
    b = sum(p[2] for p in pts) / len(pts)
    # magenta / pink only: keeps red (errors), orange/yellow (warnings) and skin tones
    return r > 170 and b > g + 20 and r - g > 60


def main():
    OUT.mkdir(exist_ok=True)
    count = 0
    for f in sorted(SRC.iterdir()):
        if f.suffix.lower() != ".png" or f.name.startswith("LoginBackGround") or f.name.startswith("logo"):
            continue
        im = Image.open(f).convert("RGBA")
        if f.name in HEADER_ICONS:
            to_ink(im).save(OUT / f.name)
            count += 1
        elif im.width * im.height <= 512 * 512 and is_pink(im):
            pink_to_green(im).save(OUT / f.name)
            count += 1
    print(f"{count} images written to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
