"""Terrain metrics for latdev height renders (heights.png from BiomePreviewExporter).

Usage: python terrain_metrics.py <heights.png> [step_blocks]

Classification: the render colors heights by the live sea level (blue = below sea,
green/tan/white = land). Water test: blue channel clearly dominating red+green.

Reports: land %, contiguous land runs per row (blocks), connected land components
(4-conn after 2x downsample-by-max) with area + max extent, and top component sizes.
"""
import sys
from PIL import Image
from collections import deque

def is_water(r, g, b):
    return b - max(r, g) > 20

def metrics(path, step):
    im = Image.open(path).convert("RGB")
    w, h = im.size
    px = im.load()
    total = w * h
    water = 0
    for y in range(h):
        for x in range(w):
            r, g, b = px[x, y]
            if is_water(r, g, b):
                water += 1
    land = total - water
    print(f"{path}: {w}x{h}  land {100.0*land/total:.1f}%  water {100.0*water/total:.1f}%")

    # land runs per row (in px, converted to blocks)
    runs = []
    for y in range(h):
        run = 0
        for x in range(w):
            if not is_water(*px[x, y]):
                run += 1
            else:
                if run:
                    runs.append(run)
                run = 0
        if run:
            runs.append(run)
    runs.sort()
    if runs:
        def pct(p):
            return runs[min(len(runs) - 1, int(len(runs) * p))]
        print(f"  land runs: n={len(runs)} median={pct(.5)}px({pct(.5)*step//1000}k) "
              f"p90={pct(.9)}px({pct(.9)*step//1000}k) p99={pct(.99)}px({pct(.99)*step//1000}k) "
              f"max={runs[-1]}px({runs[-1]*step} blocks)")

    # components at 2x downsample (max-pool: a cell is land if any of its 2x2 is land)
    dw, dh = (w + 1) // 2, (h + 1) // 2
    grid = [[False] * dw for _ in range(dh)]
    for y in range(dh):
        for x in range(dw):
            land_here = False
            for dy in (0, 1):
                for dx in (0, 1):
                    sx, sy = x * 2 + dx, y * 2 + dy
                    if sx < w and sy < h and not is_water(*px[sx, sy]):
                        land_here = True
            grid[y][x] = land_here
    seen = [[False] * dw for _ in range(dh)]
    comps = []
    for y in range(dh):
        for x in range(dw):
            if grid[y][x] and not seen[y][x]:
                q = deque([(x, y)])
                seen[y][x] = True
                n = 0
                minx = maxx = x
                miny = maxy = y
                while q:
                    cx, cy = q.popleft()
                    n += 1
                    minx = min(minx, cx); maxx = max(maxx, cx)
                    miny = min(miny, cy); maxy = max(maxy, cy)
                    for nx, ny in ((cx+1,cy),(cx-1,cy),(cx,cy+1),(cx,cy-1)):
                        if 0 <= nx < dw and 0 <= ny < dh and grid[ny][nx] and not seen[ny][nx]:
                            seen[ny][nx] = True
                            q.append((nx, ny))
                ext_px = max(maxx - minx, maxy - miny) + 1
                ext_km = ext_px * 2 * step / 1000.0
                area_km2 = n * (2 * step / 1000.0) ** 2
                comps.append((area_km2, ext_km))
    comps.sort(reverse=True)
    big = sum(1 for _, e in comps if e >= 10.0)
    med = sum(1 for _, e in comps if 3.0 <= e < 10.0)
    small = sum(1 for _, e in comps if e < 3.0)
    print(f"  components: total={len(comps)} BIG(>=10km)={big} medium={med} small={small}")
    top = [round(a) for a, _ in comps[:10]]
    print(f"  top sizes km^2 approx: {top}")

if __name__ == "__main__":
    metrics(sys.argv[1], int(sys.argv[2]) if len(sys.argv) > 2 else 512)
