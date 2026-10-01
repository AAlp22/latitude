"""Compose the before/after planet comparison image.

before: heights-designed-planet.png (vanilla chain, pre-wiring era)
after:  heights-globe-planet3.png (designed globe chain, sea 214, K=1.5)
"""
import sys
from PIL import Image, ImageDraw

BASE = "run/latdev/atlas/seed_-8461358476146316643/deliver/"
before = Image.open(BASE + "heights-designed-planet.png").convert("RGB")
after = Image.open(BASE + "heights-globe-planet3.png").convert("RGB")

SCALE = 4
PAD = 14
LBL = 22
FOOT = 26

bw, bh = before.size[0] * SCALE, before.size[1] * SCALE
aw, ah = after.size[0] * SCALE, after.size[1] * SCALE
W = PAD * 3 + bw + aw
H = PAD * 2 + LBL + max(bh, ah) + FOOT

canvas = Image.new("RGB", (W, H), (18, 18, 22))
before = before.resize((bw, bh), Image.NEAREST)
after = after.resize((aw, ah), Image.NEAREST)
canvas.paste(before, (PAD, PAD + LBL))
canvas.paste(after, (PAD * 2 + bw, PAD + LBL))

d = ImageDraw.Draw(canvas)
f = d.getfont() if hasattr(d, "getfont") else None
try:
    from PIL import ImageFont
    font = ImageFont.load_default(16)
except Exception:
    font = None

def text(x, y, s, fill=(235, 235, 235)):
    if font:
        d.text((x, y), s, fill=fill, font=font)
    else:
        d.text((x, y), s, fill=fill)

text(PAD, PAD - 2 if LBL > 20 else PAD, "BEFORE  vanilla chain (pre-wiring)" if False else "BEFORE - vanilla chain")
text(PAD * 2 + bw, PAD - 2 if LBL > 20 else PAD, "AFTER - designed continent chain, sea 214")
text(PAD, H - FOOT + 4, "200x200 km planet | 2048 blocks/px | blue = ocean, green/tan/white = land | same seed")
canvas.save(BASE + "planet-before-after.png")
print("saved", BASE + "planet-before-after.png", canvas.size)
