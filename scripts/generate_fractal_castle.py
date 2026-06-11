from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


WIDTH = 1600
HEIGHT = 900


@dataclass(frozen=True)
class Palette:
    top: str
    front: str
    side: str
    roof: str


PALETTES = [
    Palette("#f5fbff", "#b8ccda", "#7892aa", "#ffd15b"),
    Palette("#65eee5", "#159ab5", "#0b5873", "#ffb74f"),
    Palette("#ff766f", "#cf3155", "#7d2346", "#ffe15d"),
    Palette("#b990ff", "#5c4ed0", "#2d2f82", "#ffbd64"),
]


class Svg:
    def __init__(self) -> None:
        self.parts: list[str] = []

    def add(self, markup: str) -> None:
        self.parts.append(markup)

    def render(self) -> str:
        return "\n".join(self.parts)


def fmt(points: list[tuple[float, float]]) -> str:
    return " ".join(f"{x:.2f},{y:.2f}" for x, y in points)


def polygon(svg: Svg, points: list[tuple[float, float]], fill: str, opacity: float = 1.0, extra: str = "") -> None:
    svg.add(f'<polygon points="{fmt(points)}" fill="{fill}" opacity="{opacity:.3f}" {extra}/>')


def path(svg: Svg, d: str, stroke: str, width: float, opacity: float = 1.0, fill: str = "none") -> None:
    svg.add(
        f'<path d="{d}" fill="{fill}" stroke="{stroke}" '
        f'stroke-width="{width:.2f}" stroke-linecap="round" opacity="{opacity:.3f}" filter="url(#softGlow)"/>'
    )


def ellipse(svg: Svg, cx: float, cy: float, rx: float, ry: float, fill: str, opacity: float = 1.0, extra: str = "") -> None:
    svg.add(
        f'<ellipse cx="{cx:.2f}" cy="{cy:.2f}" rx="{rx:.2f}" ry="{ry:.2f}" '
        f'fill="{fill}" opacity="{opacity:.3f}" {extra}/>'
    )


def rect(svg: Svg, x: float, y: float, w: float, h: float, fill: str, opacity: float = 1.0, rx: float = 0, extra: str = "") -> None:
    svg.add(
        f'<rect x="{x:.2f}" y="{y:.2f}" width="{w:.2f}" height="{h:.2f}" rx="{rx:.2f}" '
        f'fill="{fill}" opacity="{opacity:.3f}" {extra}/>'
    )


def block(svg: Svg, x: float, y: float, w: float, h: float, dx: float, dy: float, pal: Palette, opacity: float = 1.0) -> None:
    polygon(svg, [(x, y), (x + dx, y - dy), (x + w + dx, y - dy), (x + w, y)], pal.top, opacity)
    polygon(svg, [(x, y), (x + w, y), (x + w, y + h), (x, y + h)], pal.front, opacity)
    polygon(svg, [(x + w, y), (x + w + dx, y - dy), (x + w + dx, y + h - dy), (x + w, y + h)], pal.side, opacity)


def studs(svg: Svg, x: float, y: float, w: float, dx: float, dy: float, count: int, scale: float, opacity: float) -> None:
    if count <= 1:
        positions = [0.5]
    else:
        positions = [(i + 1) / (count + 1) for i in range(count)]
    for t in positions:
        sx = x + w * t + dx * 0.46
        sy = y - dy * 0.55
        ellipse(svg, sx, sy, 13 * scale, 6.2 * scale, "#ffffff", 0.28 * opacity)
        ellipse(svg, sx, sy - 1.6 * scale, 8 * scale, 3.8 * scale, "#ffffff", 0.34 * opacity)


def roof(svg: Svg, cx: float, base_y: float, w: float, h: float, pal: Palette, opacity: float) -> None:
    half = w / 2
    polygon(svg, [(cx - half, base_y), (cx, base_y - h), (cx + half, base_y), (cx, base_y + h * 0.28)], pal.roof, opacity)
    polygon(svg, [(cx - half, base_y), (cx, base_y + h * 0.28), (cx, base_y + h * 0.5), (cx - half, base_y + h * 0.18)], "#a95b22", opacity * 0.92)
    polygon(svg, [(cx, base_y + h * 0.28), (cx + half, base_y), (cx + half, base_y + h * 0.18), (cx, base_y + h * 0.5)], "#75411a", opacity * 0.9)


def gear(svg: Svg, cx: float, cy: float, r: float, opacity: float) -> None:
    svg.add(f'<g opacity="{opacity:.3f}" filter="url(#softGlow)">')
    for i in range(8):
        angle = i * 45
        rect(svg, cx - r * 0.09, cy - r * 1.42, r * 0.18, r * 0.5, "#ffdf62", 1.0, r * 0.04, f'transform="rotate({angle:.1f} {cx:.2f} {cy:.2f})"')
    ellipse(svg, cx, cy, r, r, "#ffdf62", 1.0)
    ellipse(svg, cx, cy, r * 0.42, r * 0.42, "#142844", 1.0)
    svg.add("</g>")


def castle(svg: Svg, cx: float, baseline: float, scale: float, depth: int, branch: int = 0) -> None:
    if scale < 0.075 or depth < 0:
        return

    pal = PALETTES[(depth + branch) % len(PALETTES)]
    opacity = min(1.0, 0.45 + 0.16 * depth)
    dx = 34 * scale
    dy = 24 * scale

    base_w = 470 * scale
    base_h = 64 * scale
    base_x = cx - base_w / 2
    base_y = baseline - base_h
    block(svg, base_x, base_y, base_w, base_h, dx, dy, PALETTES[(branch + 1) % len(PALETTES)], opacity)
    studs(svg, base_x, base_y, base_w, dx, dy, 5, scale, opacity)

    wall_w = 365 * scale
    wall_h = 82 * scale
    wall_x = cx - wall_w / 2
    wall_y = base_y - wall_h
    block(svg, wall_x, wall_y, wall_w, wall_h, dx * 0.92, dy * 0.92, pal, opacity)
    studs(svg, wall_x, wall_y, wall_w, dx, dy, 4, scale, opacity)

    tower_w = 86 * scale
    tower_h = 170 * scale
    tower_y = wall_y - tower_h + 24 * scale
    left_x = cx - 185 * scale
    right_x = cx + 99 * scale
    tower_pal = PALETTES[(branch + 3) % len(PALETTES)]
    block(svg, left_x, tower_y, tower_w, tower_h, dx * 0.78, dy * 0.78, tower_pal, opacity)
    block(svg, right_x, tower_y, tower_w, tower_h, dx * 0.78, dy * 0.78, tower_pal, opacity)
    roof(svg, left_x + tower_w / 2, tower_y - 5 * scale, 104 * scale, 128 * scale, pal, opacity)
    roof(svg, right_x + tower_w / 2, tower_y - 5 * scale, 104 * scale, 128 * scale, pal, opacity)

    keep_w = 155 * scale
    keep_h = 205 * scale
    keep_x = cx - keep_w / 2
    keep_y = wall_y - keep_h + 42 * scale
    keep_pal = PALETTES[(branch + 2) % len(PALETTES)]
    block(svg, keep_x, keep_y, keep_w, keep_h, dx, dy, keep_pal, opacity)
    roof(svg, cx, keep_y - 8 * scale, 136 * scale, 154 * scale, tower_pal, opacity)

    gate_w = 82 * scale
    gate_h = 104 * scale
    gate_x = cx - gate_w / 2
    gate_y = wall_y + wall_h - gate_h
    rect(svg, gate_x, gate_y, gate_w, gate_h, "#142844", opacity * 0.96, 34 * scale)
    ellipse(svg, cx, gate_y + gate_h * 0.34, 18 * scale, 18 * scale, "#3affe6", opacity, 'filter="url(#softGlow)"')
    ellipse(svg, cx, gate_y + gate_h * 0.34, 7 * scale, 7 * scale, "#fff9b7", opacity)
    gear(svg, cx - 45 * scale, wall_y + 40 * scale, 18 * scale, opacity)
    gear(svg, cx + 45 * scale, wall_y + 40 * scale, 18 * scale, opacity)

    # Light paths encode composition: foundation -> action engine -> watchtowers.
    path(svg, f"M{cx - 195 * scale:.2f} {baseline - 48 * scale:.2f} C{cx - 120 * scale:.2f} {baseline - 116 * scale:.2f} {cx - 42 * scale:.2f} {wall_y + 42 * scale:.2f} {cx:.2f} {gate_y + gate_h * 0.34:.2f}", "url(#lightPath)", 5.2 * scale, 0.58 * opacity)
    path(svg, f"M{cx + 195 * scale:.2f} {baseline - 48 * scale:.2f} C{cx + 120 * scale:.2f} {baseline - 116 * scale:.2f} {cx + 42 * scale:.2f} {wall_y + 42 * scale:.2f} {cx:.2f} {gate_y + gate_h * 0.34:.2f}", "url(#lightPath)", 5.2 * scale, 0.58 * opacity)
    path(svg, f"M{left_x + tower_w / 2:.2f} {tower_y + 92 * scale:.2f} C{cx - 85 * scale:.2f} {keep_y + 115 * scale:.2f} {cx - 42 * scale:.2f} {keep_y + 96 * scale:.2f} {cx:.2f} {gate_y + gate_h * 0.34:.2f}", "url(#lightPath)", 4.2 * scale, 0.52 * opacity)
    path(svg, f"M{right_x + tower_w / 2:.2f} {tower_y + 92 * scale:.2f} C{cx + 85 * scale:.2f} {keep_y + 115 * scale:.2f} {cx + 42 * scale:.2f} {keep_y + 96 * scale:.2f} {cx:.2f} {gate_y + gate_h * 0.34:.2f}", "url(#lightPath)", 4.2 * scale, 0.52 * opacity)

    if depth == 0:
        return

    child_scale = scale * 0.335
    castle(svg, left_x + tower_w / 2, tower_y - 78 * scale, child_scale, depth - 1, branch + 1)
    castle(svg, cx, keep_y - 98 * scale, child_scale * 1.12, depth - 1, branch + 2)
    castle(svg, right_x + tower_w / 2, tower_y - 78 * scale, child_scale, depth - 1, branch + 3)

    # Smaller rooms on the base repeat the same castle silhouette.
    if depth >= 2:
        castle(svg, cx - 110 * scale, baseline - 38 * scale, scale * 0.19, depth - 2, branch + 4)
        castle(svg, cx + 110 * scale, baseline - 38 * scale, scale * 0.19, depth - 2, branch + 5)


def floating_fractals(svg: Svg) -> None:
    # Five miniature castle seeds orbit the main structure.
    seeds = [
        (400, 290, 0.26, 2, 1),
        (588, 238, 0.22, 2, 2),
        (800, 188, 0.29, 2, 3),
        (1012, 238, 0.22, 2, 0),
        (1200, 292, 0.26, 2, 1),
    ]
    for cx, y, scale, depth, branch in seeds:
        castle(svg, cx, y + 110 * scale, scale, depth, branch)


def background(svg: Svg) -> None:
    svg.add(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {WIDTH} {HEIGHT}">')
    svg.add(
        """
<defs>
  <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="#0f225a"/>
    <stop offset="0.36" stop-color="#315fa8"/>
    <stop offset="0.68" stop-color="#f5b66d"/>
    <stop offset="1" stop-color="#fff6de"/>
  </linearGradient>
  <radialGradient id="sun" cx="76%" cy="23%" r="38%">
    <stop offset="0" stop-color="#fff7b0" stop-opacity="1"/>
    <stop offset="0.34" stop-color="#ffd76b" stop-opacity="0.72"/>
    <stop offset="1" stop-color="#ffd76b" stop-opacity="0"/>
  </radialGradient>
  <linearGradient id="ground" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="#edf8ff"/>
    <stop offset="1" stop-color="#c5d8e5"/>
  </linearGradient>
  <linearGradient id="lightPath" x1="0" y1="0" x2="1" y2="0">
    <stop offset="0" stop-color="#38ffe7" stop-opacity="0"/>
    <stop offset="0.28" stop-color="#38ffe7" stop-opacity="0.95"/>
    <stop offset="0.62" stop-color="#fff06a" stop-opacity="0.95"/>
    <stop offset="1" stop-color="#fff06a" stop-opacity="0"/>
  </linearGradient>
  <filter id="shadow" x="-35%" y="-35%" width="170%" height="170%">
    <feDropShadow dx="0" dy="16" stdDeviation="18" flood-color="#061b32" flood-opacity="0.33"/>
  </filter>
  <filter id="softGlow" x="-80%" y="-80%" width="260%" height="260%">
    <feGaussianBlur stdDeviation="5" result="blur"/>
    <feColorMatrix in="blur" type="matrix" values="0.2 0 0 0 0.02  0 1 0 0 0.9  0 0 1 0 0.82  0 0 0 0.82 0" result="colored"/>
    <feMerge>
      <feMergeNode in="colored"/>
      <feMergeNode in="SourceGraphic"/>
    </feMerge>
  </filter>
</defs>
"""
    )
    rect(svg, 0, 0, WIDTH, HEIGHT, "url(#bg)")
    rect(svg, 0, 0, WIDTH, HEIGHT, "url(#sun)")
    svg.add(
        """
<g opacity="0.34" fill="none" stroke-linecap="round">
  <path d="M-60 232C109 82 306 77 412 171C526 272 391 380 222 316C81 263 91 138 288 69" stroke="#fff3a7" stroke-width="15"/>
  <path d="M140 222C289 135 488 165 542 286C601 414 430 447 301 360C192 287 228 190 390 150" stroke="#3ef2e1" stroke-width="10"/>
  <path d="M864 89C1071 -18 1244 32 1281 158C1315 278 1161 335 1058 239C973 160 1045 68 1201 62" stroke="#fff6b8" stroke-width="15"/>
  <path d="M1009 184C1194 82 1367 171 1374 316C1381 461 1191 436 1114 334C1042 239 1114 161 1270 164" stroke="#ff95d2" stroke-width="9"/>
</g>
<g opacity="0.5">
  <ellipse cx="260" cy="152" rx="78" ry="30" fill="#ffffff"/>
  <ellipse cx="326" cy="145" rx="88" ry="38" fill="#ffffff"/>
  <ellipse cx="392" cy="158" rx="70" ry="28" fill="#ffffff"/>
  <ellipse cx="1120" cy="143" rx="88" ry="37" fill="#ffffff"/>
  <ellipse cx="1193" cy="135" rx="103" ry="43" fill="#ffffff"/>
  <ellipse cx="1288" cy="154" rx="81" ry="32" fill="#ffffff"/>
</g>
<g opacity="0.43">
  <polygon points="116,313 163,262 211,318 158,356" fill="#fff7b7"/>
  <polygon points="1336,99 1393,52 1450,110 1394,163" fill="#52eadc"/>
  <polygon points="1456,344 1507,302 1548,353 1494,389" fill="#ffa7cf"/>
  <circle cx="525" cy="205" r="7" fill="#fff7b7"/>
  <circle cx="1320" cy="510" r="6" fill="#52eadc"/>
  <circle cx="936" cy="142" r="7" fill="#52eadc"/>
</g>
<path d="M0 594C214 549 425 557 604 600C794 646 966 656 1151 610C1320 568 1470 558 1600 580V900H0Z" fill="url(#ground)"/>
<ellipse cx="800" cy="776" rx="655" ry="116" fill="#35536b" opacity="0.16"/>
"""
    )


def main() -> None:
    out = Path("images/fractal-composable-fantasy-castle.svg")
    out.parent.mkdir(parents=True, exist_ok=True)

    svg = Svg()
    background(svg)
    svg.add('<g filter="url(#shadow)">')
    castle(svg, 800, 790, 1.0, 3, 0)
    svg.add("</g>")
    floating_fractals(svg)
    svg.add("</svg>")

    out.write_text(svg.render(), encoding="utf-8")


if __name__ == "__main__":
    main()
