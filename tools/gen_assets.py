"""LobCorp 모드 텍스처/리소스 생성기"""
import json, os, random
from PIL import Image

R = "/home/claude/lobcorp-mod/src/main/resources/assets/lobcorp"
random.seed(7)


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def jitter(c, amt=10):
    return tuple(max(0, min(255, v + random.randint(-amt, amt))) for v in c[:3]) + (c[3],)


def rect(img, x, y, w, h, color, amt=8):
    for i in range(x, x + w):
        for j in range(y, y + h):
            if 0 <= i < img.width and 0 <= j < img.height:
                img.putpixel((i, j), jitter(color, amt) if amt else color)


def box(img, u, v, w, h, d, color, amt=8, top=None, front=None):
    """마인크래프트 박스 UV 전체를 칠한다. 반환: 각 면의 좌상단 좌표"""
    faces = {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }
    for name, (x, y, fw, fh) in faces.items():
        c = color
        if name == "top" and top: c = top
        if name == "front" and front: c = front
        rect(img, x, y, fw, fh, c, amt)
    return {k: (f[0], f[1]) for k, f in faces.items()}


def px(img, x, y, c):
    img.putpixel((x, y), c)


def save(img, path):
    full = os.path.join(R, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


# ───────────── 단 한 번의 악과 수백 가지의 선행 ─────────────
def one_sin():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    wood = hexc("#b9b3a2")
    box(img, 0, 0, 3, 30, 2, wood, 9)
    box(img, 12, 0, 18, 3, 2, wood, 9)
    # 나뭇결
    for j in range(2, 32, 3):
        px(img, 3, j, hexc("#8f897a"))
    bone = hexc("#e9e3d0")
    f = box(img, 12, 8, 8, 8, 8, bone, 7, top=hexc("#f1ecdc"))
    fx, fy = f["front"]
    black = hexc("#120d0d")
    for (x, y) in [(1, 2), (2, 2), (1, 3), (2, 3), (5, 2), (6, 2), (5, 3), (6, 3), (1, 4), (6, 4)]:
        px(img, fx + x, fy + y, black)
    for (x, y) in [(3, 5), (4, 5)]:
        px(img, fx + x, fy + y, hexc("#2b2320"))
    for x in range(1, 7):
        px(img, fx + x, fy + 7, hexc("#fffaf0") if x % 2 else hexc("#4a3f38"))
    # 관자놀이 그늘
    for side in ("right", "left"):
        sx, sy = f[side]
        for j in range(8):
            px(img, sx + (j % 2) * 7, sy + j, hexc("#cfc7b0"))
    # 턱
    j = box(img, 12, 24, 6, 2, 5, bone, 7)
    jx, jy = j["front"]
    for x in range(6):
        px(img, jx + x, jy, hexc("#fffaf0") if x % 2 == 0 else hexc("#3a302a"))
    # 가시관
    thorn = hexc("#4b3523")
    box(img, 36, 24, 10, 2, 1, thorn, 14)
    box(img, 36, 28, 1, 2, 8, thorn, 14)
    rect(img, 0, 40, 4, 2, hexc("#2f2116"), 6)
    save(img, "textures/entity/one_sin.png")


# ───────────── 버려진 살인마 ─────────────
def murderer():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    skin = hexc("#8b908d")
    dark = hexc("#5d625f")
    h = box(img, 0, 0, 8, 8, 8, skin, 9)
    hx, hy = h["front"]
    # 퀭한 눈, 일자 입
    for (x, y) in [(1, 3), (2, 3), (5, 3), (6, 3)]:
        px(img, hx + x, hy + y, hexc("#232424"))
    for (x, y) in [(1, 2), (2, 2), (5, 2), (6, 2)]:
        px(img, hx + x, hy + y, dark)
    for x in range(2, 6):
        px(img, hx + x, hy + 6, hexc("#3a2c2c"))
    # 정수리 봉합 자국 (뇌 해부)
    tx, ty = h["top"]
    for i in range(8):
        px(img, tx + i, ty + 4, hexc("#6e2a2a"))
        if i % 2 == 0:
            px(img, tx + i, ty + 3, hexc("#4a1c1c"))
            px(img, tx + i, ty + 5, hexc("#4a1c1c"))
    # 몸통: 맨살 + 갈비뼈 + 구속 벨트
    b = box(img, 16, 16, 8, 12, 4, skin, 9)
    bx, by = b["front"]
    for y in (3, 5, 7):
        for x in (1, 2, 5, 6):
            px(img, bx + x, by + y, dark)
    strap = hexc("#4a3a2c")
    for i in range(8):
        px(img, bx + i, by + 9, strap)
        px(img, bx + i, by + 10, strap)
    px(img, bx + 3, by + 9, hexc("#a8a08a"))
    # 팔: 손목 구속구
    for u in (40, 52):
        a = box(img, u, 16, 3, 12, 3, skin, 9)
        for face in ("front", "back", "left", "right"):
            ax, ay = a[face]
            rect(img, ax, ay + 9, 3, 2, hexc("#6b6f73"), 4)
    # 다리: 해진 죄수복 바지
    pants = hexc("#5a5a52")
    for v in (16, 32):
        l = box(img, 0, v, 4, 12, 4, pants, 10)
        lx, ly = l["front"]
        px(img, lx + 1, ly + 8, hexc("#3a3a34"))
        px(img, lx + 2, ly + 4, hexc("#3a3a34"))
    # 쇠머리
    metal = hexc("#8f969d")
    c = box(img, 16, 32, 10, 5, 10, metal, 10, top=hexc("#a4abb2"))
    cx, cy = c["front"]
    for x in (1, 4, 7):
        px(img, cx + x, cy + 2, hexc("#d8dde2"))
    for x in range(10):
        px(img, cx + x, cy + 4, hexc("#5a6066"))
    save(img, "textures/entity/forsaken_murderer.png")


# ───────────── 아이템 아이콘 (16x16) ─────────────
def icon(rows, palette, path):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), hexc(palette[ch]))
    save(img, path)


PEN = {"w": "#ece6d2", "s": "#c9c1a8", "t": "#4b3523", "h": "#9a8a6a", "d": "#6d6046", "k": "#1a1414"}
icon([
    "..........tttt..",
    ".........twwwwt.",
    "........twkwkwwt",
    "........twwwwwwt",
    "........twswswst",
    ".........twwwwt.",
    "..........tsst..",
    ".........hh.....",
    "........hd......",
    ".......hd.......",
    "......hd........",
    ".....hd.........",
    "....hd..........",
    "...hd...........",
    "..dd............",
    "................",
], PEN, "textures/item/penitence.png")

icon([
    "................",
    "...ww......ww...",
    "..wwww....wwww..",
    "..wwwwwwwwwwww..",
    "...wwwwwwwwww...",
    "....wwsssssw....",
    "....wwwtwwww....",
    "....wwttttww....",
    "....wwwtwwww....",
    "....wwwtwwww....",
    "....wwsssssw....",
    "....wwwwwwww....",
    "....wwwwwwww....",
    ".....ssssss.....",
    "................",
    "................",
], PEN, "textures/item/penitence_suit.png")

REG = {"m": "#9aa1a8", "l": "#c9cfd4", "d": "#5c6268", "h": "#3b3028", "b": "#6a5444", "r": "#7a2a2a"}
icon([
    "................",
    "......mmmmmm....",
    ".....mlllllmm...",
    ".....mlmmmmmd...",
    ".....mmmmmmmd...",
    "......ddmdddd...",
    "........hb......",
    ".......hb.......",
    "......hb........",
    ".....hb.........",
    "....hb..........",
    "...rb...........",
    "..hr............",
    "..h.............",
    "................",
    "................",
], REG, "textures/item/regret.png")

icon([
    "................",
    "...dd......dd...",
    "..dddd....dddd..",
    "..dddddddddddd..",
    "...dddddddddd...",
    "....ddmmmmdd....",
    "....dddrrddd....",
    "....ddrrrrdd....",
    "....dddrrddd....",
    "....dddddddd....",
    "....ddmmmmdd....",
    "....dddddddd....",
    "....dddddddd....",
    ".....hhhhhh.....",
    "................",
    "................",
], REG, "textures/item/regret_suit.png")


# ───────────── 방어구(착용) 텍스처 64x32 ─────────────
def armor(base, trim, accent, path):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    box(img, 16, 16, 8, 12, 4, base, 6)       # 몸통
    box(img, 40, 16, 4, 12, 4, base, 6)       # 팔
    box(img, 0, 0, 8, 8, 8, base, 6)          # (투구용, 미사용)
    # 몸통 앞면 장식: 가운데 줄 + 테두리
    fx, fy = 20, 20
    for y in range(12):
        img.putpixel((fx + 3, fy + y), hexc(accent))
        img.putpixel((fx + 4, fy + y), hexc(accent))
    for x in range(8):
        img.putpixel((fx + x, fy), hexc(trim))
        img.putpixel((fx + x, fy + 11), hexc(trim))
    for y in range(12):
        img.putpixel((44, 20 + y), hexc(trim))
    save(img, path)


armor(hexc("#e8e2cf"), "#b9b3a2", "#4b3523", "textures/entity/equipment/humanoid/penitence.png")
armor(hexc("#55595e"), "#2e3135", "#7a2a2a", "textures/entity/equipment/humanoid/regret.png")


# ───────────── JSON 리소스 ─────────────
def wjson(path, obj):
    full = os.path.join(R, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)

# ── 번역: tools/lang_*.json 이 원본. 각 섹션은 새 키만 merge_lang 으로 추가한다 ──
_TOOLS = os.path.dirname(os.path.realpath(__file__))
def merge_lang(ko, en):
    for code, new in (("ko_kr", ko), ("en_us", en)):
        src = os.path.join(_TOOLS, f"lang_{code}.json")
        cur = json.load(open(src, encoding="utf-8")) if os.path.exists(src) else {}
        cur.update(new)
        with open(src, "w", encoding="utf-8") as f:
            json.dump(cur, f, ensure_ascii=False, indent=2)
        wjson(f"lang/{code}.json", cur)



for item, parent in [("penitence", "minecraft:item/handheld"), ("regret", "minecraft:item/handheld"),
                     ("penitence_suit", "minecraft:item/generated"), ("regret_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})

for mat in ("penitence", "regret"):
    wjson(f"equipment/{mat}.json", {"layers": {"humanoid": [{"texture": f"lobcorp:{mat}"}]}})


one_sin()
murderer()
print("assets ok")


# ═════════════ 2차: 불타버린 소녀 / 행복한 테디베어 / 요정의 축제 ═════════════
def scorched_girl():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    char = hexc("#1e1a19")
    h = box(img, 0, 0, 6, 6, 6, char, 6)
    hx, hy = h["front"]
    white = hexc("#f2efe6")
    for (x, y) in [(1, 2), (4, 2)]:             # 하얀 눈
        px(img, hx + x, hy + y, white)
    for (x, y) in [(2, 4), (3, 4), (2, 5), (3, 5)]:  # 벌린 입
        px(img, hx + x, hy + y, white)
    # 갈라진 틈 사이 불씨
    for side in ("left", "right", "back", "top"):
        sx, sy = h[side]
        px(img, sx + 2, sy + 3, hexc("#c4471c"))
    box(img, 24, 0, 2, 5, 2, hexc("#151212"), 5)      # 포니테일
    b = box(img, 0, 12, 4, 6, 3, char, 6)
    bx, by = b["front"]
    px(img, bx + 2, by + 2, hexc("#e0661f"))           # 성냥 관통 부위의 불빛
    px(img, bx + 1, by + 3, hexc("#8a2b12"))
    box(img, 16, 12, 2, 6, 2, char, 6)                 # 팔
    box(img, 24, 12, 2, 7, 2, char, 6)                 # 다리
    box(img, 32, 0, 1, 12, 1, hexc("#c9a46a"), 8)      # 성냥개비
    box(img, 36, 0, 2, 2, 2, hexc("#ff7a1a"), 20, top=hexc("#ffd34d"))  # 불붙은 머리
    save(img, "textures/entity/scorched_girl.png")


def teddy():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    fur = hexc("#a06a3c")
    b = box(img, 0, 0, 8, 9, 6, fur, 12)
    bx, by = b["front"]
    # 배의 찢어진 틈 + 삐져나온 솜
    for (x, y) in [(3, 3), (4, 4), (3, 5), (4, 6)]:
        px(img, bx + x, by + y, hexc("#3a2414"))
    for (x, y) in [(5, 4), (5, 5), (2, 4)]:
        px(img, bx + x, by + y, hexc("#f1ede4"))
    # 리본 (목)
    for x in range(8):
        px(img, bx + x, by, hexc("#c43a4a"))
    h = box(img, 28, 0, 8, 7, 6, fur, 12)
    hx, hy = h["front"]
    px(img, hx + 5, hy + 2, hexc("#111111"))           # 남은 눈
    px(img, hx + 2, hy + 4, hexc("#111111"))           # 실에 매달려 처진 눈
    px(img, hx + 2, hy + 3, hexc("#d9d0bf"))           # 실
    for (x, y) in [(5, 0), (6, 0)]:
        px(img, hx + x, hy + y, hexc("#f1ede4"))       # 머리 솜
    box(img, 56, 0, 2, 2, 1, hexc("#7d4f2b"), 8)       # 귀
    s = box(img, 0, 40, 4, 3, 2, hexc("#d8b38a"), 8)   # 주둥이
    sx, sy = s["front"]
    px(img, sx + 1, sy, hexc("#2a1a10")); px(img, sx + 2, sy, hexc("#2a1a10"))
    box(img, 0, 15, 3, 7, 3, fur, 12)                  # 팔
    box(img, 12, 15, 4, 3, 5, fur, 12)                 # 다리
    # 액자: 금색 테두리 + 소녀 사진
    f = box(img, 30, 15, 6, 7, 1, hexc("#b8923a"), 10)
    fx, fy = f["front"]
    rect(img, fx + 1, fy + 1, 4, 5, hexc("#e9dcc4"), 6)
    px(img, fx + 2, fy + 2, hexc("#6b4a2a")); px(img, fx + 3, fy + 2, hexc("#6b4a2a"))
    rect(img, fx + 2, fy + 3, 2, 3, hexc("#d07a8a"), 6)
    save(img, "textures/entity/happy_teddy_bear.png")


def fairy():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    skin = hexc("#5fe0c8")
    box(img, 0, 0, 2, 10, 2, skin, 12)
    h = box(img, 8, 0, 4, 4, 4, hexc("#7cf0da"), 10)
    hx, hy = h["front"]
    for (x, y) in [(1, 1), (2, 1)]:
        px(img, hx + x, hy + y, hexc("#0e3b36"))
    for x in range(4):
        px(img, hx + x, hy + 3, hexc("#0e3b36"))      # 길게 찢어진 입
    box(img, 24, 0, 1, 8, 1, skin, 10)
    box(img, 28, 0, 1, 5, 1, skin, 10)
    box(img, 40, 0, 1, 10, 1, hexc("#3fb8a3"), 10)
    # 날개: 반투명 + 잎맥
    for x in range(16):
        for y in range(16, 26):
            img.putpixel((x, y), (190, 255, 245, 120))
    for y in range(16, 26):
        img.putpixel((4, y), (120, 230, 210, 220)); img.putpixel((11, y), (120, 230, 210, 220))
    box(img, 32, 0, 2, 3, 2, hexc("#8ff7e6"), 10)
    for x in range(32, 38):
        for y in range(8, 11):
            img.putpixel((x, y), (210, 255, 250, 140))
    save(img, "textures/entity/fairy_festival.png")


MF = {"r": "#d8461c", "o": "#ff9a2e", "y": "#ffd34d", "w": "#c9a46a", "d": "#7a5a34", "k": "#1e1a19"}
icon([
    "..........yy....",
    ".........yooy...",
    ".........orro...",
    "..........rr....",
    ".........ww.....",
    "........ww......",
    ".......ww.......",
    "......ww........",
    ".....ww.........",
    "....ww..........",
    "...kk...........",
    "..kk............",
    "..k.............",
    "................",
    "................",
    "................",
], MF, "textures/item/match_flame.png")
icon([
    "................",
    "...kk......kk...",
    "..kkkk....kkkk..",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "....kkrrrrkk....",
    "....kkkookkk....",
    "....kkoyyokk....",
    "....kkkookkk....",
    "....kkkkkkkk....",
    "....kkrrrrkk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....dddddd.....",
    "................",
    "................",
], MF, "textures/item/match_flame_suit.png")
BP = {"b": "#a06a3c", "d": "#6b4426", "p": "#e8c9a4", "r": "#c43a4a", "w": "#f1ede4"}
icon([
    "................",
    "....b..b..b.....",
    "...bbbbbbbbb....",
    "...bbbbbbbbbb...",
    "..bbbppppbbbb...",
    "..bbpppppbbbb...",
    "..bbppppppbbb...",
    "..bbbppppbbbb...",
    "...bbbbbbbbb....",
    "....bbbbbbb.....",
    ".....rrrrr......",
    ".....dbbbd......",
    ".....dbbbd......",
    "......ddd.......",
    "................",
    "................",
], BP, "textures/item/bear_paws.png")
icon([
    "................",
    "...bb......bb...",
    "..bbbb....bbbb..",
    "..bbbbbbbbbbbb..",
    "...bbbbbbbbbb...",
    "....bbrrrrbb....",
    "....bbbrrbbb....",
    "....bbbwbbbb....",
    "....bbbwwbbb....",
    "....bbbbbbbb....",
    "....bbbbbbbb....",
    "....bbbbbbbb....",
    "....bbbbbbbb....",
    ".....dddddd.....",
    "................",
    "................",
], BP, "textures/item/bear_paws_suit.png")
WB = {"g": "#5fe0c8", "l": "#bff8ee", "d": "#2f8f80", "h": "#4a3a2a"}
icon([
    "................",
    "..ll......ll....",
    ".llll....llll...",
    ".lllll..lllll...",
    "..lllllllllll...",
    "...llldgdlll....",
    ".....dggd.......",
    "......gg........",
    ".....hh.........",
    "....hh..........",
    "...hh...........",
    "..hh............",
    ".hh.............",
    "................",
    "................",
    "................",
], WB, "textures/item/wingbeat.png")
icon([
    "................",
    "...gg......gg...",
    "..gggg....gggg..",
    "..gggggggggggg..",
    "...gggggggggg...",
    "....ggllllgg....",
    "....gggllggg....",
    "....ggllllgg....",
    "....gggggggg....",
    "....ggdddddg....",
    "....gggggggg....",
    "....gggggggg....",
    "....gggggggg....",
    ".....dddddd.....",
    "................",
    "................",
], WB, "textures/item/wingbeat_suit.png")

armor(hexc("#2a2422"), "#8a2b12", "#ff7a1a", "textures/entity/equipment/humanoid/match_flame.png")
armor(hexc("#a06a3c"), "#6b4426", "#c43a4a", "textures/entity/equipment/humanoid/bear_paws.png")
armor(hexc("#5fe0c8"), "#2f8f80", "#bff8ee", "textures/entity/equipment/humanoid/wingbeat.png")

for item, parent in [("match_flame", "minecraft:item/handheld"), ("bear_paws", "minecraft:item/handheld"),
                     ("wingbeat", "minecraft:item/handheld"), ("match_flame_suit", "minecraft:item/generated"),
                     ("bear_paws_suit", "minecraft:item/generated"), ("wingbeat_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
for mat in ("match_flame", "bear_paws", "wingbeat"):
    wjson(f"equipment/{mat}.json", {"layers": {"humanoid": [{"texture": f"lobcorp:{mat}"}]}})

scorched_girl()
teddy()
fairy()
print("assets v2 ok")


# ═════════════ 3차: 처벌새 (O-02-56) ═════════════
def punishing_bird():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    white = hexc("#f4f1ea")
    shade = hexc("#d8d3c6")
    red = hexc("#c8102e")
    black = hexc("#141414")
    mouth = hexc("#b0001a")
    teeth = hexc("#fbf7ef")

    # 몸통 (u0 v0, 4x4x5)
    b = box(img, 0, 0, 4, 4, 5, white, 6, top=shade)
    bx, by = b["front"]
    # 가슴의 붉은 표식
    for (x, y) in [(1, 1), (2, 1), (1, 2), (2, 2), (1, 3), (2, 3)]:
        px(img, bx + x, by + y, red)
    px(img, bx + 1, by + 2, hexc("#8a0a20"))
    px(img, bx + 2, by + 2, hexc("#8a0a20"))

    # 머리 (u0 v12, 3x3x3)
    h = box(img, 0, 12, 3, 3, 3, white, 6, top=shade)
    hx, hy = h["front"]
    px(img, hx, hy + 1, black)
    px(img, hx + 2, hy + 1, black)

    # 부리 (u20 v12, 1x1x2)
    bk = box(img, 20, 12, 1, 1, 2, hexc("#e8b23a"), 6)
    bkx, bky = bk["front"]
    px(img, bkx, bky, hexc("#b07a1a"))

    # 날개 (u24 v0, 5x1x4)
    for u in (24, 24):
        w = box(img, u, 0, 5, 1, 4, white, 6, top=shade)
        wx, wy = w["front"]
        for x in range(5):
            px(img, wx + x, wy, shade)

    # 꼬리 (u24 v8, 2x1x3)
    box(img, 24, 8, 2, 1, 3, white, 6, top=shade)

    # 아가리 (u0 v20, 6x5x5)
    m = box(img, 0, 20, 6, 5, 5, mouth, 8)
    mx, my = m["front"]
    for x in range(6):
        px(img, mx + x, my, teeth)
        px(img, mx + x, my + 4, teeth)
    for (x, y) in [(1, 2), (4, 2)]:
        px(img, mx + x, my + y, hexc("#2a0006"))

    # 횃대 (u0 v40, 1x14x1 + 8x1x1)
    box(img, 0, 40, 1, 14, 1, black, 4)
    box(img, 0, 40, 8, 1, 1, black, 4)

    save(img, "textures/entity/punishing_bird.png")


BK = {"w": "#f4f1ea", "s": "#d8d3c6", "r": "#c8102e", "d": "#8a0a20", "y": "#e8b23a", "k": "#141414"}
icon([
    "................",
    "..........ww....",
    ".........wwww...",
    "........wwwwww..",
    ".......wwwwwww..",
    "......wwwwwwww..",
    ".....wwwwwwww...",
    "....wwwwwww.....",
    "...wwwwww.......",
    "..wwwwww........",
    "..wwwww.........",
    "...wwww.........",
    "....rr..........",
    "...rrr..........",
    "..rr............",
    "................",
], BK, "textures/item/beak.png")
icon([
    "................",
    "...ww......ww...",
    "..wwww....wwww..",
    "..wwwwwwwwwwww..",
    "...wwwwwwwwww...",
    "....wwrrrrww....",
    "....wwrrrrww....",
    "....wwrrrrww....",
    "....wwrrrrww....",
    "....wwwwwwww....",
    "....wwssssww....",
    "....wwwwwwww....",
    "....wwwwwwww....",
    ".....ssssss.....",
    "................",
    "................",
], BK, "textures/item/beak_suit.png")

armor(hexc("#f4f1ea"), "#d8d3c6", "#c8102e", "textures/entity/equipment/humanoid/beak.png")

for item, parent in [("beak", "minecraft:item/handheld"), ("beak_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/beak.json", {"layers": {"humanoid": [{"texture": "lobcorp:beak"}]}})


punishing_bird()
print("assets v3 ok")


# ═════════════ 4차: 오늘의 수줍은 표정 (O-01-92) ═════════════
def todays_shy_look():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    robe = hexc("#6b4a30")
    robe_dark = hexc("#4a3220")
    leg = hexc("#a8473f")
    cloth = hexc("#e8dcc0")
    dark = hexc("#2a2018")
    blue = hexc("#5aa8e0")
    red = hexc("#c8102e")

    # 가로대 (u0 v0, 24x1x1) — 모델의 bar
    box(img, 0, 0, 24, 1, 1, hexc("#8a7a5a"), 8)
    # 기둥 (u0 v2, 1x25x1) — 모델의 post_right / post_left
    box(img, 0, 2, 1, 25, 1, hexc("#6b5c42"), 8)

    # 얼굴 판 다섯 개 (u = i*4, v = 8, 4x6x1) — 모델의 face0..face4
    # 0: 활짝 웃는 얼굴
    f = box(img, 0, 8, 4, 6, 1, cloth, 5)
    fx, fy = f["front"]
    px(img, fx + 1, fy + 1, dark); px(img, fx + 2, fy + 1, dark)
    for x in range(4):
        px(img, fx + x, fy + 3, dark)
    px(img, fx, fy + 2, dark); px(img, fx + 3, fy + 2, dark)
    px(img, fx, fy + 4, dark); px(img, fx + 3, fy + 4, dark)
    px(img, fx + 1, fy + 4, dark); px(img, fx + 2, fy + 4, dark)
    # 1: 웃는 얼굴
    f = box(img, 4, 8, 4, 6, 1, cloth, 5)
    fx, fy = f["front"]
    px(img, fx + 1, fy + 1, dark); px(img, fx + 2, fy + 1, dark)
    for x in range(4):
        px(img, fx + x, fy + 3, dark)
    px(img, fx, fy + 2, dark); px(img, fx + 3, fy + 2, dark)
    # 2: 무표정
    f = box(img, 8, 8, 4, 6, 1, cloth, 5)
    fx, fy = f["front"]
    px(img, fx + 1, fy + 1, dark); px(img, fx + 2, fy + 1, dark)
    for x in range(4):
        px(img, fx + x, fy + 3, dark)
    # 3: 슬픈 얼굴 + 눈물
    f = box(img, 12, 8, 4, 6, 1, cloth, 5)
    fx, fy = f["front"]
    px(img, fx + 1, fy + 1, dark); px(img, fx + 2, fy + 1, dark)
    px(img, fx, fy + 4, dark); px(img, fx + 3, fy + 4, dark)
    px(img, fx + 1, fy + 3, dark); px(img, fx + 2, fy + 3, dark)
    px(img, fx + 1, fy + 2, blue); px(img, fx + 2, fy + 2, blue)
    # 4: 화난 얼굴 + 벌린 입
    f = box(img, 16, 8, 4, 6, 1, cloth, 5)
    fx, fy = f["front"]
    px(img, fx + 1, fy + 1, dark); px(img, fx + 2, fy + 1, dark)
    px(img, fx + 1, fy + 2, dark); px(img, fx + 2, fy + 2, dark)
    px(img, fx + 1, fy + 3, red); px(img, fx + 2, fy + 3, red)
    px(img, fx + 1, fy + 4, red); px(img, fx + 2, fy + 4, red)

    # 천 뒤의 형체: 갈색 두루마기 (u0 v16, 4x10x3) — 모델의 figure
    b = box(img, 0, 16, 4, 10, 3, robe, 8)
    bx, by = b["front"]
    for y in range(10):
        px(img, bx + 1, by + y, robe_dark)
        px(img, bx + 2, by + y, robe_dark)
    # 머리 (u20 v16, 4x4x4) — 모델의 head
    h = box(img, 20, 16, 4, 4, 4, robe, 8)
    hx, hy = h["front"]
    px(img, hx + 1, hy + 1, dark); px(img, hx + 2, hy + 1, dark)
    # 팔 (u36 v16, 2x9x2) — 모델의 right_arm / left_arm
    a = box(img, 36, 16, 2, 9, 2, robe, 8)
    ax, ay = a["front"]
    for y in range(9):
        px(img, ax, ay + y, robe_dark)
    # 다리 (u44 v16, 2x8x2) — 모델의 right_leg / left_leg
    box(img, 44, 16, 2, 8, 2, leg, 8)

    save(img, "textures/entity/todays_shy_look.png")


TS = {"c": "#e8dcc0", "d": "#2a2018", "b": "#5aa8e0", "r": "#c8102e", "w": "#6b4a30", "k": "#4a3220"}
icon([
    "................",
    "..cccccccccccc..",
    "..cddcddcddcdd..",
    "..cddcddcddcdd..",
    "..cccccccccccc..",
    "....wwwwwwww....",
    "....wwkkkkww....",
    "....wwkkkkww....",
    "....wwwwwwww....",
    "....wwwwwwww....",
    "....wwwwwwww....",
    "....wwwwwwww....",
    ".....kkkkkk.....",
    "................",
    "................",
    "................",
], TS, "textures/item/todays_expression.png")
icon([
    "................",
    "...cc......cc...",
    "..cccc....cccc..",
    "..cccccccccccc..",
    "...cccccccccc...",
    "....ccddddcc....",
    "....ccddddcc....",
    "....ccddddcc....",
    "....ccddddcc....",
    "....cccccccc....",
    "....ccwwwwcc....",
    "....cccccccc....",
    "....cccccccc....",
    ".....kkkkkk.....",
    "................",
    "................",
], TS, "textures/item/todays_expression_suit.png")

armor(hexc("#6b4a30"), "#4a3220", "#e8dcc0", "textures/entity/equipment/humanoid/todays_expression.png")

for item, parent in [("todays_expression", "minecraft:item/handheld"), ("todays_expression_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/todays_expression.json", {"layers": {"humanoid": [{"texture": "lobcorp:todays_expression"}]}})


todays_shy_look()
print("assets shy ok")


# ═════════════ 5차: 벽을 보는 여자 (F-01-18) ═════════════
def lady_facing_the_wall():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    skin = hexc("#e8e4dc")
    hair = hexc("#0b0b0d")
    strand = hexc("#1d1d22")

    # 몸통 (u0 v0, 6x11x3)
    b = box(img, 0, 0, 6, 11, 3, skin, 5)
    bx, by = b["front"]
    # 낡은 원피스 자락
    for y in range(6, 11):
        for x in range(6):
            px(img, bx + x, by + y, jitter(hexc("#3a3a40"), 6))
    for x in range(6):
        px(img, bx + x, by + 6, hexc("#55555e"))

    # 머리 (u24 v0, 6x7x6)
    h = box(img, 24, 0, 6, 7, 6, skin, 5)
    hx, hy = h["front"]
    # 얼굴은 보이지 않는다 — 앞면은 전부 머리카락
    for y in range(7):
        for x in range(6):
            px(img, hx + x, hy + y, jitter(hair, 6))
    for y in range(7):
        px(img, hx + 1, hy + y, strand)
        px(img, hx + 4, hy + y, strand)

    # 뒤로 늘어진 긴 머리 (u0 v32, 8x22x2)
    hb = box(img, 0, 32, 8, 22, 2, hair, 6)
    hbx, hby = hb["front"]
    for y in range(22):
        if y % 3 == 0:
            for x in range(8):
                px(img, hbx + x, hby + y, strand)
    for x in (1, 4, 6):
        for y in range(22):
            px(img, hbx + x, hby + y, jitter(strand, 4))

    # 옆으로 흘러내리는 머리 (u20 v32, 1x16x7)
    hs = box(img, 20, 32, 1, 16, 7, hair, 6)
    hsx, hsy = hs["front"]
    for y in range(16):
        px(img, hsx, hsy + y, strand)

    # 팔 (u36 v32, 2x11x2)
    a = box(img, 36, 32, 2, 11, 2, skin, 5)
    ax, ay = a["front"]
    for y in range(11):
        px(img, ax, ay + y, hexc("#d8d3c8"))
        px(img, ax + 1, ay + y, hexc("#d8d3c8"))

    # 다리 (u44 v32, 2x12x2)
    l = box(img, 44, 32, 2, 12, 2, hexc("#2a2a30"), 5)
    lx, ly = l["front"]
    for y in range(12):
        px(img, lx, ly + y, hexc("#1a1a1f"))

    save(img, "textures/entity/lady_facing_the_wall.png")


SW = {"p": "#e8e4dc", "s": "#c9c4b8", "d": "#8a857a", "k": "#0b0b0d", "h": "#1d1d22"}
icon([
    "................",
    "..............p.",
    ".............pp.",
    "............pp..",
    "...........pp...",
    "..........pp....",
    ".........pp.....",
    "........pp......",
    ".......pp.......",
    "......pp........",
    ".....pp.........",
    "....pp..........",
    "...pp...........",
    "..pp............",
    ".pp.............",
    "................",
], SW, "textures/item/screaming_wedge.png")
icon([
    "................",
    "...kk......kk...",
    "..kkkk....kkkk..",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "....kkppppkk....",
    "....kkppppkk....",
    "....kkppppkk....",
    "....kkppppkk....",
    "....kkkkkkkk....",
    "....kkppppkk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....hhhhhh.....",
    "................",
    "................",
], SW, "textures/item/screaming_wedge_suit.png")

armor(hexc("#0b0b0d"), "#1d1d22", "#e8e4dc", "textures/entity/equipment/humanoid/screaming_wedge.png")

for item, parent in [("screaming_wedge", "minecraft:item/handheld"),
                     ("screaming_wedge_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/screaming_wedge.json", {"layers": {"humanoid": [{"texture": "lobcorp:screaming_wedge"}]}})


lady_facing_the_wall()
print("assets wall ok")


# ═════════════ 6차: 부서지는 갑옷 (O-05-61) ═════════════
def crumbling_armor():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    lacquer = hexc("#3a0f12")
    black = hexc("#1a1414")
    gold = hexc("#b8923a")
    rust = hexc("#7a4a2a")
    pink = hexc("#e8a0b4")
    eye = hexc("#6ad0ff")

    # 투구 (u0 v0, 8x7x8)
    h = box(img, 0, 0, 8, 7, 8, lacquer, 6, top=black)
    hx, hy = h["front"]
    # 얼굴판: 어두운 철판 + 푸른 눈 두 점
    rect(img, hx + 1, hy + 2, 6, 4, black, 4)
    px(img, hx + 2, hy + 3, eye)
    px(img, hx + 5, hy + 3, eye)
    px(img, hx + 2, hy + 4, hexc("#2a6a8a"))
    px(img, hx + 5, hy + 4, hexc("#2a6a8a"))
    # 금테
    for x in range(8):
        px(img, hx + x, hy, gold)
        px(img, hx + x, hy + 6, gold)
    # 투구깃 (u32 v0, 1x5x1)
    box(img, 32, 0, 1, 5, 1, gold, 6)

    # 흉갑 (u0 v16, 10x10x6)
    c = box(img, 0, 16, 10, 10, 6, lacquer, 6, top=black)
    cx, cy = c["front"]
    # 벚꽃 무늬
    for (x, y) in [(2, 2), (3, 2), (2, 3), (3, 3), (6, 5), (7, 5), (6, 6), (7, 6), (4, 7)]:
        px(img, cx + x, cy + y, pink)
    # 금테 가슴띠
    for x in range(10):
        px(img, cx + x, cy + 4, gold)
    for y in range(10):
        px(img, cx, cy + y, gold)
        px(img, cx + 9, cy + y, gold)

    # 초하부 (u0 v32, 11x5x7)
    s = box(img, 0, 32, 11, 5, 7, lacquer, 6)
    sx, sy = s["front"]
    for y in range(5):
        for x in range(11):
            if (x + y) % 3 == 0:
                px(img, sx + x, sy + y, rust)
    for x in range(11):
        px(img, sx + x, sy, gold)

    # 어깨 판 (u36 v16, 4x2x6) — 녹
    for u in (36, 36):
        sh = box(img, u, 16, 4, 2, 6, lacquer, 6, top=rust)
        shx, shy = sh["front"]
        for x in range(4):
            px(img, shx + x, shy + 1, rust)

    # 왼팔 (u32 v24, 3x10x3)
    a = box(img, 32, 24, 3, 10, 3, lacquer, 6)
    ax, ay = a["front"]
    for y in range(10):
        px(img, ax + 1, ay + y, black)
    for x in range(3):
        px(img, ax + x, ay + 9, gold)

    # 다리 (u44 v24, 4x8x4)
    for u in (44, 44):
        l = box(img, u, 24, 4, 8, 4, black, 6)
        lx, ly = l["front"]
        for x in range(4):
            px(img, lx + x, ly + 7, gold)

    # 바닥에 떨어진 오른쪽 손등가죽 (u48 v0, 3x3x5)
    g = box(img, 48, 0, 3, 3, 5, lacquer, 6, top=rust)
    gx, gy = g["front"]
    for x in range(3):
        px(img, gx + x, gy + 2, gold)

    save(img, "textures/entity/crumbling_armor.png")


DD = {"s": "#c9cfd4", "l": "#eef2f5", "d": "#5c6268", "g": "#b8923a", "h": "#3a2a1a", "r": "#7a2a2a"}
icon([
    "................",
    "..............ll",
    ".............ll.",
    "............ll..",
    "...........ll...",
    "..........ll....",
    ".........ll.....",
    "........ll......",
    ".......ll.......",
    "......ll........",
    ".....ll.........",
    "....ll..........",
    "...gg...........",
    "..gg............",
    "..h.............",
    "................",
], DD, "textures/item/daredevil.png")
icon([
    "................",
    "...dd......dd...",
    "..dddd....dddd..",
    "..dddddddddddd..",
    "...dddddddddd...",
    "....ddggggdd....",
    "....ddggggdd....",
    "....ddggggdd....",
    "....ddggggdd....",
    "....dddddddd....",
    "....ddrrrrdd....",
    "....dddddddd....",
    "....dddddddd....",
    ".....hhhhhh.....",
    "................",
    "................",
], DD, "textures/item/daredevil_suit.png")

armor(hexc("#3a0f12"), "#b8923a", "#e8a0b4", "textures/entity/equipment/humanoid/daredevil.png")

for item, parent in [("daredevil", "minecraft:item/handheld"), ("daredevil_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/daredevil.json", {"layers": {"humanoid": [{"texture": "lobcorp:daredevil"}]}})


crumbling_armor()
print("assets armor ok")


# ═════════════ 7차: 늙은 여인 (O-01-12) ═════════════
def old_lady():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    skin = hexc("#e6e1d8")
    bonnet = hexc("#f2f0ea")
    blouse = hexc("#6e5a7e")
    skirt = hexc("#5a6e8a")
    wood = hexc("#6b4a2a")
    wood_dark = hexc("#4a3220")
    black = hexc("#141212")

    # 의자 좌판 (u0 v0, 8x1x8)
    s = box(img, 0, 0, 8, 1, 8, wood, 8, top=hexc("#7d5a36"))
    sx, sy = s["front"]
    for x in range(8):
        px(img, sx + x, sy, wood_dark)
    # 등받이 (u0 v9, 8x12x1)
    b = box(img, 0, 9, 8, 12, 1, wood, 8)
    bx, by = b["front"]
    for y in range(12):
        px(img, bx + 3, by + y, wood_dark)
        px(img, bx + 4, by + y, wood_dark)
    for x in range(8):
        px(img, bx + x, by + 5, wood_dark)
    # 다리 (u18 v9, 1x7x1)
    box(img, 18, 9, 1, 7, 1, wood_dark, 6)
    box(img, 22, 9, 1, 7, 1, wood_dark, 6)
    # 휘어진 발판 (u26 v9, 1x2x10)
    r = box(img, 26, 9, 1, 2, 10, wood, 8)
    rx, ry = r["front"]
    for y in range(2):
        px(img, rx, ry + y, wood_dark)

    # 머리 (u0 v24, 6x6x6)
    h = box(img, 0, 24, 6, 6, 6, skin, 5)
    hx, hy = h["front"]
    # 꺼진 검은 눈
    px(img, hx + 1, hy + 2, black); px(img, hx + 2, hy + 2, black)
    px(img, hx + 3, hy + 2, black); px(img, hx + 4, hy + 2, black)
    px(img, hx + 1, hy + 3, hexc("#3a3430")); px(img, hx + 4, hy + 3, hexc("#3a3430"))
    # 깊은 주름
    for x in range(6):
        px(img, hx + x, hy + 4, hexc("#cfc8bc"))
    for (x, y) in [(1, 1), (4, 1), (2, 5), (3, 5)]:
        px(img, hx + x, hy + y, hexc("#d6d0c4"))
    # 보닛 (u24 v24, 7x3x7)
    bn = box(img, 24, 24, 7, 3, 7, bonnet, 4, top=hexc("#fbfaf6"))
    bnx, bny = bn["front"]
    for x in range(7):
        px(img, bnx + x, bny + 2, hexc("#dcd8ce"))
    for y in range(3):
        px(img, bnx, bny + y, hexc("#dcd8ce"))
        px(img, bnx + 6, bny + y, hexc("#dcd8ce"))

    # 몸통: 바랜 보랏빛 블라우스 (u0 v36, 6x7x4)
    bo = box(img, 0, 36, 6, 7, 4, blouse, 6)
    box_, boy = bo["front"]
    for y in range(7):
        px(img, box_ + 2, boy + y, hexc("#5a4868"))
        px(img, box_ + 3, boy + y, hexc("#5a4868"))
    for x in range(6):
        px(img, box_ + x, boy + 6, hexc("#4a3a56"))
    # 무릎: 바랜 푸른 치마 (u20 v36, 6x2x5)
    th = box(img, 20, 36, 6, 2, 5, skirt, 6)
    thx, thy = th["front"]
    for x in range(6):
        px(img, thx + x, thy + 1, hexc("#46586e"))
    # 정강이 (u36 v36, 2x6x2)
    sh = box(img, 36, 36, 2, 6, 2, hexc("#3a3a44"), 5)
    shx, shy = sh["front"]
    for y in range(6):
        px(img, shx, shy + y, hexc("#2a2a32"))
    # 팔: 무릎 위에 얹은 손 (u44 v36, 1x6x2)
    a = box(img, 44, 36, 1, 6, 2, blouse, 6)
    ax, ay = a["front"]
    for y in range(6):
        px(img, ax, ay + y, hexc("#5a4868"))
    px(img, ax, ay + 5, skin)

    save(img, "textures/entity/old_lady.png")


SL = {"s": "#8a9bb8", "l": "#c3cfe0", "d": "#4a5a72", "h": "#2a2a32", "k": "#141212"}
icon([
    "................",
    "..............ll",
    ".............ll.",
    "............ll..",
    "...........ll...",
    "..........ll....",
    ".........ll.....",
    "........ll......",
    ".......ll.......",
    "......ll........",
    ".....ll.........",
    "....ll..........",
    "...ss...........",
    "..ss............",
    "..h.............",
    "................",
], SL, "textures/item/solitude.png")
icon([
    "................",
    "...dd......dd...",
    "..dddd....dddd..",
    "..dddddddddddd..",
    "...dddddddddd...",
    "....ddssssdd....",
    "....ddssssdd....",
    "....ddssssdd....",
    "....ddssssdd....",
    "....dddddddd....",
    "....ddlllldd....",
    "....dddddddd....",
    "....dddddddd....",
    ".....hhhhhh.....",
    "................",
    "................",
], SL, "textures/item/solitude_suit.png")

armor(hexc("#4a5a72"), "#2a2a32", "#8a9bb8", "textures/entity/equipment/humanoid/solitude.png")

for item, parent in [("solitude", "minecraft:item/handheld"), ("solitude_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/solitude.json", {"layers": {"humanoid": [{"texture": "lobcorp:solitude"}]}})


old_lady()
print("assets oldlady ok")


# ═════════════ 8차: 거미 봉오리 (T-02-43) ═════════════
def spider_bud():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    body = hexc("#2a1a1e")
    body_dark = hexc("#1a1013")
    leg = hexc("#1b1012")
    eye = hexc("#e01b2e")
    eye_dim = hexc("#8a0f1c")
    silk = hexc("#d8d8d8")
    cocoon = hexc("#e8e4da")
    cocoon_line = hexc("#a8a49a")

    # 배 (u0 v0, 8x7x9)
    a = box(img, 0, 0, 8, 7, 9, body, 6, top=body_dark)
    ax, ay = a["front"]
    # 배의 무늬: 희미한 붉은 점
    for (x, y) in [(2, 2), (5, 2), (3, 4), (4, 4), (2, 5), (5, 5)]:
        px(img, ax + x, ay + y, eye_dim)
    for x in range(8):
        px(img, ax + x, ay + 6, body_dark)

    # 두흉부 (u0 v16, 6x5x6)
    c = box(img, 0, 16, 6, 5, 6, body, 6, top=body_dark)
    cx, cy = c["front"]
    # 눈 여덟 개: 큰 두 개 + 작은 여섯 개
    for (x, y) in [(1, 1), (4, 1)]:
        px(img, cx + x, cy + y, eye)
        px(img, cx + x, cy + y + 1, eye_dim)
    for (x, y) in [(0, 2), (2, 2), (3, 2), (5, 2), (1, 3), (4, 3)]:
        px(img, cx + x, cy + y, eye)
    # 집게
    for x in range(6):
        px(img, cx + x, cy + 4, body_dark)

    # 실 (u34 v0, 1x16x1)
    t = box(img, 34, 0, 1, 16, 1, silk, 6)
    tx, ty = t["front"]
    for y in range(16):
        if y % 3 == 0:
            px(img, tx, ty + y, hexc("#b0b0b0"))

    # 다리 (u40 v0, 1x1x8 / u40 v10, 1x1x8)
    for u, v in ((40, 0), (40, 10)):
        l = box(img, u, v, 1, 1, 8, leg, 5)
        lx, ly = l["front"]
        for x in range(1):
            px(img, lx, ly, hexc("#2a1a1e"))

    # 고치 (u0 v32, 3x5x3)
    co = box(img, 0, 32, 3, 5, 3, cocoon, 4)
    cox, coy = co["front"]
    for y in range(5):
        if y % 2 == 0:
            for x in range(3):
                px(img, cox + x, coy + y, cocoon_line)
    for x in range(3):
        px(img, cox + x, coy, hexc("#f4f1ea"))

    save(img, "textures/entity/spider_bud.png")


RE = {"d": "#2a1a1e", "k": "#1a1013", "r": "#e01b2e", "g": "#8a0f1c", "s": "#c9cfd4", "h": "#3a2a1a"}
icon([
    "................",
    "..............rr",
    ".............rr.",
    "............rr..",
    "...........rr...",
    "..........rr....",
    ".........rr.....",
    "........rr......",
    ".......rr.......",
    "......rr........",
    ".....rr.........",
    "....rr..........",
    "...gg...........",
    "..gg............",
    "..h.............",
    "................",
], RE, "textures/item/red_eyes.png")
icon([
    "................",
    "...dd......dd...",
    "..dddd....dddd..",
    "..dddddddddddd..",
    "...dddddddddd...",
    "....ddrrrrdd....",
    "....ddrrrrdd....",
    "....ddrrrrdd....",
    "....ddrrrrdd....",
    "....dddddddd....",
    "....ddggggdd....",
    "....dddddddd....",
    "....dddddddd....",
    ".....hhhhhh.....",
    "................",
    "................",
], RE, "textures/item/red_eyes_suit.png")

armor(hexc("#2a1a1e"), "#1a1013", "#e01b2e", "textures/entity/equipment/humanoid/red_eyes.png")

for item, parent in [("red_eyes", "minecraft:item/handheld"), ("red_eyes_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/red_eyes.json", {"layers": {"humanoid": [{"texture": "lobcorp:red_eyes"}]}})


spider_bud()
print("assets spider ok")


# ═════════════ 9차: 공허한 꿈 (T-02-99) ═════════════
def void_dream():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    wool = hexc("#ecebf4")
    lilac = hexc("#c9c3e6")
    face = hexc("#8a7fb0")
    horn = hexc("#5a5070")
    comb = hexc("#d2202a")
    beak = hexc("#f0b020")
    black = hexc("#050505")

    # 몸통 (u0 v0, 8x6x10)
    b = box(img, 0, 0, 8, 6, 10, wool, 6, top=hexc("#f4f3fa"))
    bx, by = b["front"]
    # 털 뭉치 음영
    for (x, y) in [(1, 1), (3, 2), (5, 1), (2, 4), (6, 3), (4, 5)]:
        px(img, bx + x, by + y, lilac)
    for x in range(8):
        px(img, bx + x, by + 5, lilac)

    # 머리 (u0 v16, 5x5x5)
    h = box(img, 0, 16, 5, 5, 5, wool, 6, top=hexc("#f4f3fa"))
    hx, hy = h["front"]
    # 감은 눈: 가는 곡선 두 개
    for x in (1, 2):
        px(img, hx + x, hy + 2, face)
    for x in (3, 4):
        px(img, hx + x, hy + 2, face)
    px(img, hx + 1, hy + 3, face)
    px(img, hx + 4, hy + 3, face)
    # 코
    px(img, hx + 2, hy + 4, face)
    px(img, hx + 3, hy + 4, face)

    # 뿔 (u20 v16, 1x2x1)
    hn = box(img, 20, 16, 1, 2, 1, horn, 6)
    hnx, hny = hn["front"]
    px(img, hnx, hny, hexc("#7a7090"))

    # 볏 (u24 v16, 1x4x5) — 수탉 형태에서만 보인다
    cb = box(img, 24, 16, 1, 4, 5, comb, 8)
    cbx, cby = cb["front"]
    for y in range(4):
        px(img, cbx, cby + y, hexc("#a01420"))

    # 부리 (u36 v16, 2x1x2)
    bk = box(img, 36, 16, 2, 1, 2, beak, 6)
    bkx, bky = bk["front"]
    px(img, bkx, bky, hexc("#c08a10"))
    px(img, bkx + 1, bky, hexc("#c08a10"))

    # 뜬 눈 (u44 v16, 2x2x0) — 검은 판
    eo = box(img, 44, 16, 2, 2, 1, black, 0)
    eox, eoy = eo["front"]
    px(img, eox, eoy, hexc("#2a2a3a"))
    px(img, eox + 1, eoy + 1, hexc("#2a2a3a"))

    # 다리 (u20 v20, 1x5x1)
    lg = box(img, 20, 20, 1, 5, 1, hexc("#d8d4e8"), 5)
    lgx, lgy = lg["front"]
    for y in range(5):
        px(img, lgx, lgy + y, hexc("#b8b2cc"))

    # 꼬리 (u24 v20, 1x1x10)
    tl = box(img, 24, 20, 1, 1, 10, wool, 5)
    tlx, tly = tl["front"]
    for x in range(1):
        px(img, tlx, tly, lilac)

    save(img, "textures/entity/void_dream.png")


VD = {"w": "#ecebf4", "l": "#c9c3e6", "f": "#8a7fb0", "h": "#5a5070", "k": "#050505"}
icon([
    "................",
    "................",
    "......wwww......",
    ".....wwwwww.....",
    "....wwffffww....",
    "....wwffffww....",
    "...wwwwwwwwww...",
    "...wwwwwwwwww...",
    "...wwwwwwwwww...",
    "...wwwwwwwwww...",
    "....wwwwwwww....",
    "....ww....ww....",
    "....ww....ww....",
    "................",
    "................",
    "................",
], VD, "textures/item/engulfing_dream.png")
icon([
    "................",
    "...ww......ww...",
    "..wwww....wwww..",
    "..wwwwwwwwwwww..",
    "...wwwwwwwwww...",
    "....wwffffww....",
    "....wwffffww....",
    "....wwffffww....",
    "....wwffffww....",
    "....wwwwwwww....",
    "....wwllllww....",
    "....wwwwwwww....",
    "....wwwwwwww....",
    ".....hhhhhh.....",
    "................",
    "................",
], VD, "textures/item/engulfing_dream_suit.png")

armor(hexc("#ecebf4"), "#c9c3e6", "#8a7fb0", "textures/entity/equipment/humanoid/engulfing_dream.png")

for item, parent in [("engulfing_dream", "minecraft:item/handheld"),
                     ("engulfing_dream_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/engulfing_dream.json", {"layers": {"humanoid": [{"texture": "lobcorp:engulfing_dream"}]}})


void_dream()
print("assets void ok")


# ═════════════ 10차: 미녀와 야수 (F-02-44) ═════════════
def beauty_beast():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    fur = hexc("#6b4a2e")
    fur_dark = hexc("#4a3220")
    patch = hexc("#7a4a8a")
    bone = hexc("#d8cfb8")
    bone_dark = hexc("#b0a68c")
    flower = hexc("#a050d0")
    flower_hi = hexc("#d090f0")
    eye = hexc("#6dff5a")
    eye_dim = hexc("#2a8a24")
    leg = hexc("#3a2a1e")

    # 몸통 (u0 v0, 8x7x10)
    b = box(img, 0, 0, 8, 7, 10, fur, 10, top=fur_dark)
    bx, by = b["front"]
    # 보랏빛 반점
    for (x, y) in [(1, 1), (2, 1), (1, 2), (5, 3), (6, 3), (5, 4), (3, 5)]:
        px(img, bx + x, by + y, patch)
    for x in range(8):
        px(img, bx + x, by + 6, fur_dark)
    # 등 쪽 털 결
    tx, ty = b["top"]
    for y in range(10):
        for x in range(8):
            if (x + y) % 4 == 0:
                px(img, tx + x, ty + y, fur_dark)

    # 머리 (u0 v18, 7x6x6)
    h = box(img, 0, 18, 7, 6, 6, fur, 10, top=fur_dark)
    hx, hy = h["front"]
    # 앞면 가득한 작은 초록 눈
    for (x, y) in [(0, 0), (2, 0), (4, 0), (6, 0),
                   (1, 1), (3, 1), (5, 1),
                   (0, 2), (2, 2), (4, 2), (6, 2),
                   (1, 3), (3, 3), (5, 3),
                   (0, 4), (2, 4), (4, 4), (6, 4),
                   (1, 5), (3, 5), (5, 5)]:
        px(img, hx + x, hy + y, eye)
    # 눈 사이 그늘
    for (x, y) in [(1, 0), (3, 0), (5, 0), (0, 1), (2, 1), (4, 1), (6, 1)]:
        px(img, hx + x, hy + y, eye_dim)
    # 옆면에도 눈 몇 개
    for side in ("right", "left"):
        sx, sy = h[side]
        for y in range(6):
            if y % 2 == 0:
                px(img, sx + 2, sy + y, eye)
                px(img, sx + 4, sy + y, eye)

    # 뿔: 위로 솟다가 바깥으로 휘어진다 (u28 v18, 2x5x2 / u36 v18, 2x5x2)
    for u in (28, 36):
        hn = box(img, u, 18, 2, 5, 2, bone, 6, top=hexc("#efe8d4"))
        hnx, hny = hn["front"]
        for y in range(5):
            px(img, hnx, hny + y, bone_dark)
        for x in range(2):
            px(img, hnx + x, hny + 4, bone_dark)

    # 뿔에 핀 작은 꽃 (u44 v18, 2x2x2)
    fl = box(img, 44, 18, 2, 2, 2, flower, 8, top=flower_hi)
    flx, fly = fl["front"]
    px(img, flx, fly, flower_hi)
    px(img, flx + 1, fly + 1, hexc("#6a2a90"))

    # 다리 (u0 v32, 1x9x1) — 가늘고 긴 곤충 다리
    lg = box(img, 0, 32, 1, 9, 1, leg, 5)
    lgx, lgy = lg["front"]
    for y in range(9):
        if y % 3 == 0:
            px(img, lgx, lgy + y, hexc("#5a4430"))

    save(img, "textures/entity/beauty_and_the_beast.png")


HN = {"b": "#d8cfb8", "d": "#b0a68c", "f": "#a050d0", "h": "#d090f0", "e": "#6dff5a", "k": "#3a2a1e"}
icon([
    "................",
    "..............bb",
    ".............bb.",
    "............bb..",
    "...........bb...",
    "..........bb....",
    ".........bb.....",
    "........bb......",
    ".......bb.......",
    "......bb........",
    ".....bb.........",
    "....bb..........",
    "...dd...........",
    "..dd............",
    "..k.............",
    "................",
], HN, "textures/item/horn.png")
icon([
    "................",
    "...dd......dd...",
    "..dddd....dddd..",
    "..dddddddddddd..",
    "...dddddddddd...",
    "....ddeeeedd....",
    "....ddeeeedd....",
    "....ddeeeedd....",
    "....ddeeeedd....",
    "....dddddddd....",
    "....ddffffdd....",
    "....dddddddd....",
    "....dddddddd....",
    ".....kkkkkk.....",
    "................",
    "................",
], HN, "textures/item/horn_suit.png")

armor(hexc("#6b4a2e"), "#4a3220", "#6dff5a", "textures/entity/equipment/humanoid/horn.png")

for item, parent in [("horn", "minecraft:item/handheld"), ("horn_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/horn.json", {"layers": {"humanoid": [{"texture": "lobcorp:horn"}]}})


beauty_beast()
print("assets beast ok")


# ═════════════ 11차: 고기 등불 (O-04-84) ═════════════
def meat_lantern():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    fur = hexc("#f2efe8")
    fur_shade = hexc("#d8d4c8")
    fur_dark = hexc("#b8b4a8")
    black = hexc("#0a0a0a")
    tooth = hexc("#fbf8f0")
    gum = hexc("#8a2a3a")
    stalk = hexc("#5a7a4a")
    bulb = hexc("#7dff6a")
    bulb_hi = hexc("#d8ffc8")
    flower_pink = hexc("#ff9ad0")
    flower_yellow = hexc("#ffe36a")

    # 큰 털 덩어리 (u0 v0, 14x9x14)
    m = box(img, 0, 0, 14, 9, 14, fur, 6, top=hexc("#fbf9f2"))
    mx, my = m["front"]
    # 털 뭉치 음영
    for (x, y) in [(1, 1), (4, 2), (7, 1), (10, 3), (2, 5), (6, 6), (11, 5), (3, 7), (9, 7)]:
        px(img, mx + x, my + y, fur_shade)
    for x in range(14):
        px(img, mx + x, my + 8, fur_dark)
    # 옆면 털 결
    for side in ("right", "left", "back"):
        sx, sy = m[side]
        for y in range(9):
            for x in range(0, 14, 3):
                px(img, sx + x, sy + y, fur_shade)

    # 위에 얹힌 작은 덩어리 (u0 v24, 10x4x10)
    t = box(img, 0, 24, 10, 4, 10, fur, 6, top=hexc("#fbf9f2"))
    tx, ty = t["front"]
    for x in range(10):
        px(img, tx + x, ty + 3, fur_shade)

    # 작은 검은 눈 (u56 v0, 1x1x1)
    e = box(img, 56, 0, 1, 1, 1, black, 0)
    ex, ey = e["front"]
    px(img, ex, ey, hexc("#2a2a2a"))

    # 아가리 판 (u0 v40, 8x3x1) — 이빨 한 줄
    j = box(img, 0, 40, 8, 3, 1, gum, 6)
    jx, jy = j["front"]
    for x in range(8):
        px(img, jx + x, jy, tooth)
        if x % 2 == 0:
            px(img, jx + x, jy + 1, tooth)
    for x in range(8):
        px(img, jx + x, jy + 2, hexc("#5a1a26"))

    # 줄기 (u40 v40, 1x7x1)
    st = box(img, 40, 40, 1, 7, 1, stalk, 6)
    stx, sty = st["front"]
    for y in range(7):
        px(img, stx, sty + y, hexc("#3a5a2a"))

    # 빛나는 열매 (u44 v40, 3x3x3)
    b = box(img, 44, 40, 3, 3, 3, bulb, 10, top=bulb_hi)
    bx, by = b["front"]
    px(img, bx + 1, by + 1, bulb_hi)
    px(img, bx, by, hexc("#4ac03a"))
    px(img, bx + 2, by + 2, hexc("#4ac03a"))

    # 작은 꽃 두 송이 (u56 v40, 1x1x1 / u58 v40, 1x1x1)
    f1 = box(img, 56, 40, 1, 1, 1, flower_pink, 0)
    f2 = box(img, 58, 40, 1, 1, 1, flower_yellow, 0)

    # 바닥의 흰 털 판 (u0 v48, 30x0.1x30) — 회백색 바탕 + 어두운 반점
    # 64x64 텍스처에는 30x30 영역이 들어가지 않으므로 16x16으로 줄여 그린다 (UV는 0,48 기준)
    hx0, hy0 = 0, 48
    for x in range(16):
        for y in range(16):
            c = fur if (x * 7 + y * 13) % 11 != 0 else fur_shade
            if (x * 3 + y * 5) % 17 == 0:
                c = fur_dark
            px(img, hx0 + x, hy0 + y, c)

    save(img, "textures/entity/meat_lantern.png")


LT = {"k": "#2a2a2a", "d": "#4a4a4a", "g": "#7dff6a", "h": "#d8ffc8", "s": "#8a8a8a", "w": "#f2efe8"}
icon([
    "................",
    "..........gg....",
    ".........ghhg...",
    ".........gggg...",
    "..........ss....",
    "..........ss....",
    "..........ss....",
    "..........ss....",
    "..........ss....",
    ".......kkkkkk...",
    "......kkkkkkkk..",
    ".....kkkkkkkkkk.",
    ".....kkkkkkkkkk.",
    "......kkkkkkkk..",
    ".......kkkkkk...",
    "................",
], LT, "textures/item/lantern.png")
icon([
    "................",
    "...ww......ww...",
    "..wwww....wwww..",
    "..wwwwwwwwwwww..",
    "...wwwwwwwwww...",
    "....wwggggww....",
    "....wwggggww....",
    "....wwggggww....",
    "....wwggggww....",
    "....wwwwwwww....",
    "....wwkkkkww....",
    "....wwwwwwww....",
    "....wwwwwwww....",
    ".....ssssss.....",
    "................",
    "................",
], LT, "textures/item/lantern_suit.png")

armor(hexc("#f2efe8"), "#b8b4a8", "#7dff6a", "textures/entity/equipment/humanoid/lantern.png")

for item, parent in [("lantern", "minecraft:item/handheld"), ("lantern_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/lantern.json", {"layers": {"humanoid": [{"texture": "lobcorp:lantern"}]}})


meat_lantern()
print("assets lantern ok")


# ═════════════ 12차: 우주의 파편 (O-03-60) ═════════════
def fragment_of_universe():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    core = hexc("#0b0b12")
    core_hi = hexc("#1a1a2a")
    core_dark = hexc("#05050a")
    yellow = hexc("#ffe14a")
    red = hexc("#ff4a5a")
    purple = hexc("#b45aff")
    pink = hexc("#ff8ad0")
    pink_dark = hexc("#c05a9a")
    tent = hexc("#141420")
    tent_hi = hexc("#26263a")

    # 검은 구체 (u0 v0, 8x8x8)
    c = box(img, 0, 0, 8, 8, 8, core, 4, top=core_hi)
    cx, cy = c["front"]

    def heart(x0, y0, col, hi):
        # 3x3 하트: 위 두 칸, 아래 한 칸
        px(img, x0, y0, col)
        px(img, x0 + 2, y0, col)
        px(img, x0, y0 + 1, col)
        px(img, x0 + 1, y0 + 1, hi)
        px(img, x0 + 2, y0 + 1, col)
        px(img, x0 + 1, y0 + 2, col)

    # 앞면: 노랑·빨강·보라 하트 반점
    heart(cx + 1, cy + 1, yellow, hexc("#fff6b0"))
    heart(cx + 4, cy + 3, red, hexc("#ffb0b8"))
    heart(cx + 2, cy + 5, purple, hexc("#e0b8ff"))
    # 옆면·뒷면에도 반점
    for side, col, hi in (("right", red, hexc("#ffb0b8")), ("left", yellow, hexc("#fff6b0")),
                          ("back", purple, hexc("#e0b8ff")), ("top", yellow, hexc("#fff6b0"))):
        sx, sy = c[side]
        heart(sx + 2, sy + 2, col, hi)
        px(img, sx + 5, sy + 5, col)
    # 표면의 별가루
    for (x, y) in [(0, 3), (7, 1), (3, 0), (6, 6), (0, 7)]:
        px(img, cx + x, cy + y, core_hi)
    for x in range(8):
        px(img, cx + x, cy + 7, core_dark)

    # 촉수 (u0 v16, 1.5x5x1.5 → 2x5x2 픽셀 근사 / u8 v16, 아래쪽)
    for u in (0, 8):
        t = box(img, u, 16, 2, 5, 2, tent, 5)
        tx, ty = t["front"]
        for y in range(5):
            px(img, tx, ty + y, tent_hi)
        for x in range(2):
            px(img, tx + x, ty + 4, core_dark)

    # 떠 있는 분홍 하트 (u32 v0, 3x3x1 두 개 / u40 v0, 2x2x1)
    h1 = box(img, 32, 0, 3, 3, 1, pink, 6)
    h1x, h1y = h1["front"]
    px(img, h1x + 1, h1y + 1, pink_dark)
    h2 = box(img, 32, 0, 3, 3, 1, pink, 6)
    h2x, h2y = h2["front"]
    px(img, h2x + 1, h2y + 1, pink_dark)
    h3 = box(img, 40, 0, 2, 2, 1, pink, 6)
    h3x, h3y = h3["front"]
    px(img, h3x, h3y, pink_dark)
    px(img, h3x + 1, h3y + 1, pink_dark)

    save(img, "textures/entity/fragment_of_universe.png")


FR = {"k": "#0b0b12", "d": "#05050a", "y": "#ffe14a", "r": "#ff4a5a", "p": "#b45aff",
      "h": "#ff8ad0", "s": "#26263a"}
icon([
    "................",
    "..............ss",
    ".............ss.",
    "............ss..",
    "...........ss...",
    "..........ss....",
    ".........ss.....",
    "........ss......",
    ".......ss.......",
    "......ss........",
    ".....ss.........",
    "....ss..........",
    "...dd...........",
    "..dd............",
    "..d.............",
    "................",
], FR, "textures/item/fragments.png")
icon([
    "................",
    "...kk......kk...",
    "..kkkk....kkkk..",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "....kkyyyykk....",
    "....kkyyyykk....",
    "....kkrrrrkk....",
    "....kkrrrrkk....",
    "....kkkkkkkk....",
    "....kkppppkk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....dddddd.....",
    "................",
    "................",
], FR, "textures/item/fragments_suit.png")

armor(hexc("#0b0b12"), "#26263a", "#ff8ad0", "textures/entity/equipment/humanoid/fragments.png")

for item, parent in [("fragments", "minecraft:item/handheld"), ("fragments_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/fragments.json", {"layers": {"humanoid": [{"texture": "lobcorp:fragments"}]}})


fragment_of_universe()
print("assets fragment ok")


# ═════════════ 13차: 1.76 MHz (T-06-27) ═════════════
def mhz():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    shell = hexc("#3a3a3a")
    shell_hi = hexc("#4a4a4a")
    shell_dark = hexc("#262626")
    grille = hexc("#5a5a5a")
    grille_dark = hexc("#2a2a2a")
    amber = hexc("#e0a030")
    amber_hi = hexc("#ffd070")
    metal = hexc("#8a8a8a")

    # 본체 (u0 v0, 10x8x6)
    b = box(img, 0, 0, 10, 8, 6, shell, 6, top=shell_hi)
    bx, by = b["front"]
    # 스피커 그릴: 가로줄
    for y in range(1, 7):
        for x in range(1, 6):
            px(img, bx + x, by + y, grille if y % 2 == 0 else grille_dark)
    # 그릴 테두리
    for x in range(0, 7):
        px(img, bx + x, by, shell_dark)
        px(img, bx + x, by + 7, shell_dark)
    for y in range(0, 8):
        px(img, bx, by + y, shell_dark)
        px(img, bx + 6, by + y, shell_dark)
    # 다이얼 (u32 v0, 2x2x1)
    d = box(img, 32, 0, 2, 2, 1, amber, 6)
    dx, dy = d["front"]
    px(img, dx, dy, amber_hi)
    px(img, dx + 1, dy + 1, hexc("#a06a10"))
    # 다이얼 눈금
    for x in range(7, 10):
        px(img, bx + x, by + 2, metal)
    # 정전기 잡음 픽셀
    for _ in range(60):
        x = random.randint(0, 9)
        y = random.randint(0, 7)
        c = random.choice([hexc("#d8d8d8"), hexc("#8a8a8a"), hexc("#f0f0f0"), hexc("#5a5a5a")])
        px(img, bx + x, by + y, c)
    # 옆면·뒷면에도 잡음
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for _ in range(20):
            px(img, sx + random.randint(0, 5), sy + random.randint(0, 7),
               random.choice([hexc("#d8d8d8"), hexc("#6a6a6a")]))
    # 안테나 (u32 v4, 1x8x1)
    for u in (32, 36):
        a = box(img, u, 4, 1, 8, 1, metal, 6)
        ax, ay = a["front"]
        for y in range(8):
            px(img, ax, ay + y, hexc("#b0b0b0") if y % 2 == 0 else hexc("#6a6a6a"))
    # 바닥 그림자
    for x in range(10):
        px(img, bx + x, by + 7, shell_dark)

    save(img, "textures/entity/mhz.png")


NZ = {"k": "#3a3a3a", "d": "#262626", "g": "#5a5a5a", "a": "#e0a030", "h": "#ffd070", "s": "#8a8a8a"}
icon([
    "................",
    "..............ss",
    ".............ss.",
    "............ss..",
    "...........ss...",
    "..........ss....",
    ".........ss.....",
    "........ss......",
    ".......ss.......",
    "......ss........",
    ".....ss.........",
    "....ss..........",
    "...dd...........",
    "..dd............",
    "..d.............",
    "................",
], NZ, "textures/item/noise.png")
icon([
    "................",
    "...kk......kk...",
    "..kkkk....kkkk..",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "....kkggggkk....",
    "....kkggggkk....",
    "....kkggggkk....",
    "....kkggggkk....",
    "....kkkkkkkk....",
    "....kkaaaakk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....dddddd.....",
    "................",
    "................",
], NZ, "textures/item/noise_suit.png")

armor(hexc("#3a3a3a"), "#262626", "#e0a030", "textures/entity/equipment/humanoid/noise.png")

for item, parent in [("noise", "minecraft:item/handheld"), ("noise_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/noise.json", {"layers": {"humanoid": [{"texture": "lobcorp:noise"}]}})


mhz()
print("assets mhz ok")


# ═════════════ 14차: 붉은 구두 (O-04-08) ═════════════
def red_shoes():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    wood = hexc("#4a2c1a")
    wood_hi = hexc("#5e3a22")
    wood_dark = hexc("#2e1a0e")
    red = hexc("#c4101a")
    red_dark = hexc("#7a0810")
    red_hi = hexc("#ff5a66")
    white = hexc("#f4eef0")
    sole = hexc("#1a0a0c")

    # 받침대 (u0 v0, 8x2x8)
    p = box(img, 0, 0, 8, 2, 8, wood, 8, top=wood_hi)
    px_, py_ = p["front"]
    for x in range(8):
        px(img, px_ + x, py_ + 1, wood_dark)
    for x in range(8):
        px(img, px_ + x, py_, wood_hi)
    # 받침대 옆면 나뭇결
    for side in ("right", "left", "back"):
        sx, sy = p[side]
        for y in range(2):
            for x in range(0, 8, 3):
                px(img, sx + x, sy + y, wood_dark)

    # 받침대 기둥 (u0 v20, 4x12x4)
    c = box(img, 0, 20, 4, 12, 4, wood, 4, top=wood_hi)
    for side in ("front", "right", "left", "back"):
        cx, cy = c[side]
        for y in range(0, 12, 4):
            for x in range(4):
                px(img, cx + x, cy + y, wood_dark)

    # 구두 몸통 (u0 v10, 3x2x6) — 좌우 공용
    s = box(img, 0, 10, 3, 2, 6, red, 6, top=red_hi)
    sx, sy = s["front"]
    # 광택 하이라이트 가로줄
    for x in range(3):
        px(img, sx + x, sy, white)
    px(img, sx, sy + 1, red_dark)
    px(img, sx + 2, sy + 1, red_dark)
    # 옆면 광택
    for side in ("right", "left"):
        ox, oy = s[side]
        for y in range(2):
            px(img, ox + 2, oy + y, red_hi)
        px(img, ox + 1, oy, white)
    # 밑창
    bx, by = s["bottom"]
    for x in range(3):
        for y in range(6):
            px(img, bx + x, by + y, sole)

    # 굽 (u18 v10, 1x2x1)
    h = box(img, 18, 10, 1, 2, 1, red_dark, 4)
    hx, hy = h["front"]
    px(img, hx, hy, hexc("#4a0408"))

    # 끈 (u22 v10, 3x1x1)
    st = box(img, 22, 10, 3, 1, 1, white, 4)
    stx, sty = st["front"]
    px(img, stx + 1, sty, hexc("#d8ccd0"))

    save(img, "textures/entity/red_shoes.png")


SD = {"r": "#c4101a", "d": "#7a0810", "h": "#ff5a66", "w": "#f4eef0", "k": "#1a0a0c", "b": "#4a2c1a"}
icon([
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......rrrr......",
    ".....rrhhrr.....",
    "....rrrrrrrr....",
    "....rrrrrrrr....",
    "....dddddddd....",
    "....kkkkkkkk....",
    "................",
    "................",
    "................",
    "................",
], SD, "textures/item/sanguine_desire.png")
icon([
    "................",
    "...dd......dd...",
    "..dddd....dddd..",
    "..dddddddddddd..",
    "...dddddddddd...",
    "....ddrrrrdd....",
    "....ddrrrrdd....",
    "....ddrrrrdd....",
    "....ddrrrrdd....",
    "....dddddddd....",
    "....ddhhhhdd....",
    "....dddddddd....",
    "....dddddddd....",
    ".....kkkkkk.....",
    "................",
    "................",
], SD, "textures/item/sanguine_desire_suit.png")

armor(hexc("#c4101a"), "#7a0810", "#f4eef0", "textures/entity/equipment/humanoid/sanguine_desire.png")

for item, parent in [("sanguine_desire", "minecraft:item/handheld"),
                     ("sanguine_desire_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/sanguine_desire.json", {"layers": {"humanoid": [{"texture": "lobcorp:sanguine_desire"}]}})


red_shoes()
print("assets shoes ok")


# ═════════════ 15차: 노래하는 기계 (O-05-30) ═════════════
def singing_machine():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    wood = hexc("#6a3f22")
    wood_hi = hexc("#7d4c2a")
    wood_dark = hexc("#4a2a14")
    gold = hexc("#d4a640")
    gold_dark = hexc("#a07a20")
    drum = hexc("#5a1018")
    drum_hi = hexc("#8a1a26")
    drum_dark = hexc("#2a060a")
    horn = hexc("#b08a3a")
    horn_dark = hexc("#7a5a1a")

    # 본체 (u0 v0, 14x12x10)
    c = box(img, 0, 0, 14, 12, 10, wood, 8, top=wood_hi)
    cx, cy = c["front"]
    # 나뭇결
    for y in range(12):
        for x in range(0, 14, 4):
            px(img, cx + x, cy + y, wood_dark)
    # 금장 테두리 (앞면 위아래 + 세로 기둥)
    for x in range(14):
        px(img, cx + x, cy, gold)
        px(img, cx + x, cy + 11, gold)
    for y in range(12):
        px(img, cx, cy + y, gold)
        px(img, cx + 13, cy + y, gold)
    # 앞면 장식: 금장 음표 문양
    for (x, y) in [(5, 4), (6, 4), (5, 5), (6, 5), (7, 3), (8, 3), (8, 4), (8, 5), (8, 6)]:
        px(img, cx + x, cy + y, gold)
    px(img, cx + 4, cy + 6, gold_dark)
    px(img, cx + 9, cy + 6, gold_dark)
    # 옆면·뒷면 나뭇결
    for side in ("right", "left", "back"):
        sx, sy = c[side]
        for y in range(12):
            for x in range(0, 10, 3):
                px(img, sx + x, sy + y, wood_dark)

    # 금장 테두리 띠 (u0 v24, 14x1x10)
    t = box(img, 0, 24, 14, 1, 10, gold, 6, top=hexc("#e8c060"))
    tx, ty = t["front"]
    for x in range(14):
        px(img, tx + x, ty, gold_dark)

    # 뚜껑 (u0 v36, 14x1x10)
    l = box(img, 0, 36, 14, 1, 10, wood_hi, 6, top=hexc("#8a5a34"))
    lx, ly = l["front"]
    for x in range(14):
        px(img, lx + x, ly, gold)
    for side in ("right", "left", "back"):
        sx, sy = l[side]
        for x in range(0, 10, 3):
            px(img, sx + x, sy, wood_dark)

    # 나팔 스피커 (u40 v0, 4x4x6)
    h = box(img, 40, 0, 4, 4, 6, horn, 8, top=hexc("#d8b060"))
    hx, hy = h["front"]
    # 나팔 입구: 어두운 구멍 + 금테
    rect(img, hx + 1, hy + 1, 2, 2, horn_dark, 4)
    for x in range(4):
        px(img, hx + x, hy, gold)
        px(img, hx + x, hy + 3, gold)
    for y in range(4):
        px(img, hx, hy + y, gold)
        px(img, hx + 3, hy + y, gold)

    # 드럼 (u40 v12, 10x3x3)
    d = box(img, 40, 12, 10, 3, 3, drum, 8, top=drum_hi)
    dx, dy = d["front"]
    # 드럼 가죽면: 밝은 테두리 + 어두운 중심
    for x in range(10):
        px(img, dx + x, dy, drum_hi)
        px(img, dx + x, dy + 2, drum_dark)
    for x in range(2, 8):
        px(img, dx + x, dy + 1, drum_dark)
    # 톱니 이빨
    for x in range(0, 10, 2):
        px(img, dx + x, dy + 1, hexc("#c8c8c8"))

    save(img, "textures/entity/singing_machine.png")


HM = {"w": "#6a3f22", "h": "#7d4c2a", "d": "#4a2a14", "g": "#d4a640", "k": "#a07a20", "r": "#8a1a26"}
icon([
    "................",
    "..............gg",
    ".............gg.",
    "............gg..",
    "...........gg...",
    "..........gg....",
    ".........gg.....",
    "........gg......",
    ".......gg.......",
    "......gg........",
    ".....gg.........",
    "....gg..........",
    "...kk...........",
    "..kk............",
    "..k.............",
    "................",
], HM, "textures/item/harmony.png")
icon([
    "................",
    "...ww......ww...",
    "..wwww....wwww..",
    "..wwwwwwwwwwww..",
    "...wwwwwwwwww...",
    "....wwggggww....",
    "....wwggggww....",
    "....wwggggww....",
    "....wwggggww....",
    "....wwwwwwww....",
    "....wwrrrrww....",
    "....wwwwwwww....",
    "....wwwwwwww....",
    ".....dddddd.....",
    "................",
    "................",
], HM, "textures/item/harmony_suit.png")

armor(hexc("#6a3f22"), "#4a2a14", "#d4a640", "textures/entity/equipment/humanoid/harmony.png")

for item, parent in [("harmony", "minecraft:item/handheld"), ("harmony_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/harmony.json", {"layers": {"humanoid": [{"texture": "lobcorp:harmony"}]}})


singing_machine()
print("assets machine ok")


# ═════════════ 16차: 포큐버스 (O-02-98) ═════════════
def porccubus():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    stem = hexc("#3f8f3a")
    stem_dark = hexc("#2a6a28")
    stem_hi = hexc("#5ab04a")
    spine = hexc("#e8e09a")
    petal = hexc("#c8102e")
    petal_dark = hexc("#8a0a20")
    center = hexc("#f2c400")
    center_hi = hexc("#ffe36a")
    black = hexc("#0a0a0a")

    # 줄기 다섯 마디 (u0 v0 / v6 / v12 / v18 / v24, 각 3x6x3)
    for i in range(5):
        s = box(img, 0, i * 6, 3, 6, 3, stem, 8, top=stem_hi)
        sx, sy = s["front"]
        # 세로 결
        for y in range(6):
            px(img, sx + 1, sy + y, stem_dark)
        # 가시: 노란 점
        for (x, y) in [(0, 1), (2, 2), (0, 4), (2, 5)]:
            px(img, sx + x, sy + y, spine)
        # 옆면 가시
        for side in ("right", "left", "back"):
            ox, oy = s[side]
            for y in range(0, 6, 2):
                px(img, ox + 1, oy + y, spine)
        # 마디 경계
        for x in range(3):
            px(img, sx + x, sy + 5, stem_dark)

    # 꽃 중심 (u24 v0, 4x4x2)
    c = box(img, 24, 0, 4, 4, 2, center, 6, top=center_hi)
    cx, cy = c["front"]
    px(img, cx + 1, cy + 1, center_hi)
    px(img, cx + 2, cy + 2, hexc("#c09a00"))
    # 작고 동그란 검은 눈 두 개
    px(img, cx + 1, cy + 2, black)
    px(img, cx + 2, cy + 2, black)

    # 꽃잎 네 장 (u24 v8, 4x1x4 / 1x4x4)
    for u, v, w, h, d in ((24, 8, 4, 1, 4), (24, 8, 4, 1, 4), (24, 8, 1, 4, 4), (24, 8, 1, 4, 4)):
        p = box(img, u, v, w, h, d, petal, 8, top=hexc("#e02a44"))
        px_, py_ = p["front"]
        for x in range(w):
            for y in range(h):
                if (x + y) % 3 == 0:
                    px(img, px_ + x, py_ + y, petal_dark)

    # 눈 픽셀용 여분 (u40 v0, 1x1x1)
    e = box(img, 40, 0, 1, 1, 1, black, 0)

    save(img, "textures/entity/porccubus.png")


PL = {"g": "#3f8f3a", "d": "#2a6a28", "s": "#e8e09a", "r": "#c8102e", "y": "#f2c400", "k": "#0a0a0a"}
icon([
    "................",
    "..............gg",
    ".............gg.",
    "............gg..",
    "...........gg...",
    "..........gg....",
    ".........gg.....",
    "........gg......",
    ".......gg.......",
    "......gg........",
    ".....gg.........",
    "....gg..........",
    "...dd...........",
    "..dd............",
    "..d.............",
    "................",
], PL, "textures/item/pleasure.png")
icon([
    "................",
    "...gg......gg...",
    "..gggg....gggg..",
    "..gggggggggggg..",
    "...gggggggggg...",
    "....ggrrrrgg....",
    "....ggrrrrgg....",
    "....ggrrrrgg....",
    "....ggrrrrgg....",
    "....gggggggg....",
    "....ggyyyygg....",
    "....gggggggg....",
    "....gggggggg....",
    ".....dddddd.....",
    "................",
    "................",
], PL, "textures/item/pleasure_suit.png")

armor(hexc("#3f8f3a"), "#2a6a28", "#e8e09a", "textures/entity/equipment/humanoid/pleasure.png")

for item, parent in [("pleasure", "minecraft:item/handheld"), ("pleasure_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/pleasure.json", {"layers": {"humanoid": [{"texture": "lobcorp:pleasure"}]}})


porccubus()
print("assets porccubus ok")


# ═════════════ 17차: 라티샤 (O-01-67) + 라티샤의 친구 ═════════════
def laetitia():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    skin = hexc("#f6e2d8")
    skin_shade = hexc("#e0c8bc")
    dress = hexc("#f6a6c8")
    dress_dark = hexc("#d87aa4")
    lace = hexc("#fbf4f8")
    hat = hexc("#202020")
    hat_hi = hexc("#2e2e2e")
    band = hexc("#ff6aa0")
    hair = hexc("#c98a4a")
    hair_dark = hexc("#a06a34")
    black = hexc("#1a1014")
    box_white = hexc("#f4f0f2")
    ribbon = hexc("#d8203a")

    # 다리 (u0 v0, 2x5x2)
    l = box(img, 0, 0, 2, 5, 2, hexc("#fbf4f8"), 4)
    lx, ly = l["front"]
    for y in range(5):
        px(img, lx, ly + y, hexc("#e8dce4"))
    # 신발
    for x in range(2):
        px(img, lx + x, ly + 4, black)

    # 드레스 (u0 v8, 8x7x6)
    d = box(img, 0, 8, 8, 7, 6, dress, 6, top=dress_dark)
    dx, dy = d["front"]
    # 흰 레이스 줄무늬
    for y in (1, 3, 5):
        for x in range(8):
            px(img, dx + x, dy + y, lace)
    for x in range(8):
        px(img, dx + x, dy + 6, dress_dark)
    for side in ("right", "left", "back"):
        sx, sy = d[side]
        for y in (1, 3, 5):
            for x in range(6):
                px(img, sx + x, sy + y, lace)

    # 몸통 (u28 v8, 6x5x4)
    b = box(img, 28, 8, 6, 5, 4, dress, 6)
    bx, by = b["front"]
    for x in range(6):
        px(img, bx + x, by, lace)
    px(img, bx + 2, by + 2, ribbon)
    px(img, bx + 3, by + 2, ribbon)

    # 팔 (u0 v22, 2x5x2)
    a = box(img, 0, 22, 2, 5, 2, skin, 4)
    ax, ay = a["front"]
    for y in range(5):
        px(img, ax, ay + y, skin_shade)
    for x in range(2):
        px(img, ax + x, ay + 4, skin_shade)

    # 선물 상자 (u12 v22, 3x3x3)
    p = box(img, 12, 22, 3, 3, 3, box_white, 4, top=hexc("#fbf8fa"))
    px_, py_ = p["front"]
    for x in range(3):
        px(img, px_ + x, py_ + 1, ribbon)
    for y in range(3):
        px(img, px_ + 1, py_ + y, ribbon)
    tx, ty = p["top"]
    for x in range(3):
        px(img, tx + x, ty + 1, ribbon)
    for y in range(3):
        px(img, tx + 1, ty + y, ribbon)

    # 머리 (u0 v32, 6x6x6)
    h = box(img, 0, 32, 6, 6, 6, skin, 4, top=hair)
    hx, hy = h["front"]
    # 큰 미소
    for x in range(1, 5):
        px(img, hx + x, hy + 4, black)
    px(img, hx, hy + 3, black)
    px(img, hx + 5, hy + 3, black)
    for x in (2, 3):
        px(img, hx + x, hy + 5, hexc("#c86a7a"))
    # 눈: 반짝이는 점
    px(img, hx + 1, hy + 2, black)
    px(img, hx + 4, hy + 2, black)
    px(img, hx + 1, hy + 1, hexc("#5a3a2a"))
    px(img, hx + 4, hy + 1, hexc("#5a3a2a"))
    # 볼터치
    px(img, hx, hy + 3, hexc("#f0a0a8"))
    px(img, hx + 5, hy + 3, hexc("#f0a8b0"))
    # 앞머리
    for x in range(6):
        px(img, hx + x, hy, hair)
    for x in (0, 2, 5):
        px(img, hx + x, hy + 1, hair_dark)
    # 옆머리
    for side in ("right", "left"):
        sx, sy = h[side]
        for y in range(6):
            px(img, sx + 2, sy + y, hair if y < 4 else hair_dark)

    # 모자 챙 (u24 v32, 8x1x8)
    br = box(img, 24, 32, 8, 1, 8, hat, 4, top=hat_hi)
    brx, bry = br["front"]
    for x in range(8):
        px(img, brx + x, bry, hat_hi)
    # 모자 원뿔 (u24 v42, 4x5x4)
    co = box(img, 24, 42, 4, 5, 4, hat, 4, top=hat_hi)
    cox, coy = co["front"]
    for x in range(4):
        px(img, cox + x, coy + 4, band)
    for side in ("right", "left", "back"):
        sx, sy = co[side]
        for x in range(4):
            px(img, sx + x, sy + 4, band)
    # 모자 끝 (u40 v42, 2x2x2)
    tp = box(img, 40, 42, 2, 2, 2, hat, 4, top=hat_hi)
    tpx, tpy = tp["front"]
    px(img, tpx, tpy, band)

    save(img, "textures/entity/laetitia.png")


def laetitia_friend():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    flesh = hexc("#e8b8c8")
    flesh_shade = hexc("#c894a8")
    stitch = hexc("#5a0010")
    mouth = hexc("#5a0010")
    mouth_dark = hexc("#3a0008")
    tooth = hexc("#fbf6f0")
    leg = hexc("#d8a8b8")
    leg_dark = hexc("#a87a8a")
    bow = hexc("#ff6aa0")
    bow_dark = hexc("#c04a78")

    # 몸통 (u0 v0, 8x5x8)
    b = box(img, 0, 0, 8, 5, 8, flesh, 6, top=hexc("#f4ccd8"))
    bx, by = b["front"]
    # 꿰맨 자국
    for x in range(8):
        if x % 2 == 0:
            px(img, bx + x, by + 1, stitch)
    for (x, y) in [(1, 3), (2, 3), (5, 3), (6, 3), (3, 4), (4, 4)]:
        px(img, bx + x, by + y, stitch)
    for x in range(8):
        px(img, bx + x, by + 4, flesh_shade)
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for y in range(5):
            if y % 2 == 0:
                px(img, sx + 2, sy + y, stitch)
                px(img, sx + 5, sy + y, stitch)

    # 아가리 판 (u0 v16, 6x4x1)
    m = box(img, 0, 16, 6, 4, 1, mouth, 6)
    mx, my = m["front"]
    # 이빨: 위아래 한 줄씩
    for x in range(6):
        px(img, mx + x, my, tooth)
        px(img, mx + x, my + 3, tooth)
        if x % 2 == 0:
            px(img, mx + x, my + 1, tooth)
            px(img, mx + x, my + 2, tooth)
    for x in range(6):
        px(img, mx + x, my + 1, mouth_dark if x % 2 == 1 else tooth)
    # 목구멍
    px(img, mx + 2, my + 2, hexc("#1a0004"))
    px(img, mx + 3, my + 2, hexc("#1a0004"))

    # 리본 (u32 v16, 4x2x2)
    bw = box(img, 32, 16, 4, 2, 2, bow, 6, top=hexc("#ff9ac0"))
    bwx, bwy = bw["front"]
    px(img, bwx + 1, bwy, bow_dark)
    px(img, bwx + 2, bwy, bow_dark)
    px(img, bwx + 1, bwy + 1, bow_dark)
    px(img, bwx + 2, bwy + 1, bow_dark)

    # 다리 (u0 v24, 1x6x1 / u4 v24, 1x6x1)
    for u in (0, 4):
        l = box(img, u, 24, 1, 6, 1, leg, 5)
        lx, ly = l["front"]
        for y in range(6):
            px(img, lx, ly + y, leg_dark if y % 2 == 0 else leg)

    save(img, "textures/entity/laetitia_friend.png")


LT2 = {"p": "#f6a6c8", "d": "#d87aa4", "w": "#fbf4f8", "k": "#202020", "r": "#d8203a", "s": "#f6e2d8"}
icon([
    "................",
    "..............pp",
    ".............pp.",
    "............pp..",
    "...........pp...",
    "..........pp....",
    ".........pp.....",
    "........pp......",
    ".......pp.......",
    "......pp........",
    ".....pp.........",
    "....pp..........",
    "...dd...........",
    "..dd............",
    "..d.............",
    "................",
], LT2, "textures/item/laetitia.png")
icon([
    "................",
    "...pp......pp...",
    "..pppp....pppp..",
    "..pppppppppppp..",
    "...pppppppppp...",
    "....ppwwwwpp....",
    "....ppwwwwpp....",
    "....ppwwwwpp....",
    "....ppwwwwpp....",
    "....pppppppp....",
    "....pprrrrpp....",
    "....pppppppp....",
    "....pppppppp....",
    ".....dddddd.....",
    "................",
    "................",
], LT2, "textures/item/laetitia_suit.png")

armor(hexc("#f6a6c8"), "#d87aa4", "#fbf4f8", "textures/entity/equipment/humanoid/laetitia.png")

for item, parent in [("laetitia", "minecraft:item/handheld"), ("laetitia_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/laetitia.json", {"layers": {"humanoid": [{"texture": "lobcorp:laetitia"}]}})


laetitia()
laetitia_friend()
print("assets laetitia ok")


# ═════════════ 18차: 만능 도우미 (T-05-41) ═════════════
def all_around_helper():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    white = hexc("#f4f4f4")
    white_hi = hexc("#fbfbfb")
    seam = hexc("#d8d8d8")
    seam_dark = hexc("#b8b8b8")
    screen = hexc("#101014")
    cyan = hexc("#40e0ff")
    red = hexc("#ff3030")
    steel = hexc("#c8d0d8")
    steel_dark = hexc("#8a949e")
    wheel = hexc("#3a3a3e")
    wheel_hi = hexc("#5a5a60")
    mop_wood = hexc("#a07a4a")
    mop_head = hexc("#e8e4da")
    brush_head = hexc("#c8a050")

    # 몸통 (u0 v0, 10x10x10)
    b = box(img, 0, 0, 10, 10, 10, white, 4, top=white_hi)
    bx, by = b["front"]
    # 이음매 가로줄
    for x in range(10):
        px(img, bx + x, by + 4, seam)
        px(img, bx + x, by + 5, seam_dark)
    # 옆면·뒷면 이음매
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for x in range(10):
            px(img, sx + x, sy + 4, seam)
            px(img, sx + x, sy + 5, seam_dark)
    # 앞면 나사 구멍
    for (x, y) in [(1, 1), (8, 1), (1, 8), (8, 8)]:
        px(img, bx + x, by + y, seam_dark)

    # 웃는 화면 (u40 v0, 6x4x1)
    fh = box(img, 40, 0, 6, 4, 1, screen, 2)
    fhx, fhy = fh["front"]
    # 시안색 미소
    for x in (1, 2, 3, 4):
        px(img, fhx + x, fhy + 2, cyan)
    px(img, fhx, fhy + 1, cyan)
    px(img, fhx + 5, fhy + 1, cyan)
    px(img, fhx + 1, fhy + 1, cyan)
    px(img, fhx + 4, fhy + 1, cyan)
    for x in (1, 4):
        px(img, fhx + x, fhy + 3, cyan)

    # 화난 화면 (u40 v6, 6x4x1)
    fa = box(img, 40, 6, 6, 4, 1, screen, 2)
    fax, fay = fa["front"]
    # 붉은 눈 두 개 + 찡그린 입
    for (x, y) in [(1, 1), (2, 1), (3, 1), (4, 1)]:
        px(img, fax + x, fay + y, red)
    px(img, fax + 1, fay + 2, red)
    px(img, fax + 4, fay + 2, red)
    for x in (2, 3):
        px(img, fax + x, fay + 3, red)

    # 바퀴 (u0 v32, 2x2x2)
    w = box(img, 0, 32, 2, 2, 2, wheel, 4, top=wheel_hi)
    wx, wy = w["front"]
    px(img, wx, wy, wheel_hi)
    px(img, wx + 1, wy + 1, hexc("#1a1a1e"))

    # 청소 팔 (u8 v32, 1x7x1)
    a = box(img, 8, 32, 1, 7, 1, white, 4)
    ax, ay = a["front"]
    for y in range(7):
        px(img, ax, ay + y, seam if y % 2 == 0 else white)
    # 걸레 머리 (u12 v32, 3x2x1)
    mh = box(img, 12, 32, 3, 2, 1, mop_head, 4)
    mhx, mhy = mh["front"]
    for x in range(3):
        px(img, mhx + x, mhy, hexc("#fbf8f2"))
        px(img, mhx + x, mhy + 1, hexc("#c8c4ba"))
    # 걸레 자루
    for y in range(7):
        px(img, ax, ay + y, mop_wood if y > 3 else white)
    # 솔 머리 (u12 v32 재사용)
    bh = box(img, 12, 32, 3, 2, 1, brush_head, 4)
    bhx, bhy = bh["front"]
    for x in range(3):
        px(img, bhx + x, bhy, hexc("#e0b860"))
        px(img, bhx + x, bhy + 1, hexc("#8a6a2a"))

    # 칼날 팔 (u20 v32, 1x5x3)
    bl = box(img, 20, 32, 1, 5, 3, steel, 4, top=hexc("#e8eef4"))
    blx, bly = bl["front"]
    for y in range(5):
        px(img, blx, bly + y, steel_dark)
    # 칼날 끝 (u28 v32, 1x2x3)
    bt = box(img, 28, 32, 1, 2, 3, steel, 4, top=hexc("#e8eef4"))
    btx, bty = bt["front"]
    for y in range(2):
        px(img, btx, bty + y, steel_dark)
    px(img, btx, bty + 1, hexc("#f4f8fc"))

    save(img, "textures/entity/all_around_helper.png")


GR = {"w": "#f4f4f4", "s": "#c8d0d8", "d": "#8a949e", "c": "#40e0ff", "k": "#101014", "r": "#ff3030"}
icon([
    "................",
    "..............ss",
    ".............ss.",
    "............ss..",
    "...........ss...",
    "..........ss....",
    ".........ss.....",
    "........ss......",
    ".......ss.......",
    "......ss........",
    ".....ss.........",
    "....ss..........",
    "...dd...........",
    "..dd............",
    "..d.............",
    "................",
], GR, "textures/item/grinder.png")
icon([
    "................",
    "...ww......ww...",
    "..wwww....wwww..",
    "..wwwwwwwwwwww..",
    "...wwwwwwwwww...",
    "....wwccccww....",
    "....wwccccww....",
    "....wwccccww....",
    "....wwccccww....",
    "....wwwwwwww....",
    "....wwssssww....",
    "....wwwwwwww....",
    "....wwwwwwww....",
    ".....dddddd.....",
    "................",
    "................",
], GR, "textures/item/grinder_suit.png")

armor(hexc("#f4f4f4"), "#d8d8d8", "#40e0ff", "textures/entity/equipment/humanoid/grinder.png")

for item, parent in [("grinder", "minecraft:item/handheld"), ("grinder_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/grinder.json", {"layers": {"humanoid": [{"texture": "lobcorp:grinder"}]}})


all_around_helper()
print("assets helper ok")


# ═════════════ 19차: 거대한 나무 수액 (T-09-80) ═════════════
def tree_sap():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    clay = hexc("#a0603a")
    clay_hi = hexc("#b87448")
    clay_dark = hexc("#7a4428")
    band = hexc("#5e3218")
    amber = hexc("#e09a20")
    amber_hi = hexc("#ffc44a")
    amber_dark = hexc("#a86a10")
    wood = hexc("#8a6a3a")
    wood_dark = hexc("#5e4622")

    # 받침 (u0 v0, 8x2x8)
    b = box(img, 0, 0, 8, 2, 8, clay, 8, top=clay_hi)
    bx, by = b["front"]
    for x in range(8):
        px(img, bx + x, by + 1, clay_dark)
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for y in range(2):
            for x in range(0, 8, 3):
                px(img, sx + x, sy + y, clay_dark)

    # 배 (u0 v10, 10x8x10)
    be = box(img, 0, 10, 10, 8, 10, clay, 8, top=clay_hi)
    bex, bey = be["front"]
    # 점토 띠: 가로 두 줄
    for x in range(10):
        px(img, bex + x, bey + 2, band)
        px(img, bex + x, bey + 6, band)
    # 세로 균열
    for y in range(8):
        if y % 3 != 0:
            px(img, bex + 4, bey + y, clay_dark)
    for side in ("right", "left", "back"):
        sx, sy = be[side]
        for x in range(10):
            px(img, sx + x, sy + 2, band)
            px(img, sx + x, sy + 6, band)
        for y in range(8):
            if y % 3 != 0:
                px(img, sx + 5, sy + y, clay_dark)

    # 목 (u0 v28, 6x3x6)
    n = box(img, 0, 28, 6, 3, 6, clay, 8, top=clay_hi)
    nx, ny = n["front"]
    for x in range(6):
        px(img, nx + x, ny + 2, band)
    for side in ("right", "left", "back"):
        sx, sy = n[side]
        for x in range(6):
            px(img, sx + x, sy + 2, band)

    # 수액 표면 (u0 v38, 5x1x5)
    s = box(img, 0, 38, 5, 1, 5, amber, 6, top=amber_hi)
    sx, sy = s["front"]
    for x in range(5):
        px(img, sx + x, sy, amber_hi if x % 2 == 0 else amber_dark)
    tx, ty = s["top"]
    for x in range(5):
        for y in range(5):
            if (x + y) % 3 == 0:
                px(img, tx + x, ty + y, amber_hi)
            elif (x * 2 + y) % 5 == 0:
                px(img, tx + x, ty + y, amber_dark)

    # 국자 자루 (u24 v38, 1x8x1)
    h = box(img, 24, 38, 1, 8, 1, wood, 6)
    hx, hy = h["front"]
    for y in range(8):
        px(img, hx, hy + y, wood_dark if y % 2 == 0 else wood)

    # 국자 오목한 부분 (u30 v38, 2x1x2)
    bw = box(img, 30, 38, 2, 1, 2, wood, 6, top=hexc("#a88a5a"))
    bwx, bwy = bw["front"]
    px(img, bwx, bwy, wood_dark)
    px(img, bwx + 1, bwy, wood_dark)
    tx2, ty2 = bw["top"]
    for x in range(2):
        for y in range(2):
            px(img, tx2 + x, ty2 + y, amber_dark if (x + y) % 2 == 0 else amber)

    save(img, "textures/entity/tree_sap.png")


tree_sap()
print("assets sap ok")

merge_lang({
    "entity.lobcorp.tree_sap": "거대한 나무 수액",
}, {
    "entity.lobcorp.tree_sap": "Giant Tree Sap",
})


# ═════════════ 20차: 3월 27일의 대피소 (T-09-82) ═════════════
def shelter():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    concrete = hexc("#8a8a8a")
    concrete_hi = hexc("#9a9a9a")
    concrete_dark = hexc("#6a6a6a")
    steel = hexc("#5a6066")
    steel_hi = hexc("#6e757c")
    steel_dark = hexc("#3a4046")
    hazard_y = hexc("#e8c020")
    hazard_k = hexc("#1a1a1a")
    bar = hexc("#8a9096")
    bar_dark = hexc("#5a6066")
    bulb = hexc("#c8a840")
    bulb_hi = hexc("#e8d070")
    bulb_dark = hexc("#8a7020")

    # 바닥 테두리 (u0 v0, 14x1x14)
    f = box(img, 0, 0, 14, 1, 14, concrete, 6, top=concrete_hi)
    fx, fy = f["front"]
    for x in range(14):
        px(img, fx + x, fy, concrete_dark)
    tx, ty = f["top"]
    for x in range(14):
        for y in range(14):
            if (x * 5 + y * 3) % 13 == 0:
                px(img, tx + x, ty + y, concrete_dark)
    for side in ("right", "left", "back"):
        sx, sy = f[side]
        for x in range(14):
            px(img, sx + x, sy, concrete_dark)

    # 둥근 해치 (u0 v16, 10x1x10)
    h = box(img, 0, 16, 10, 1, 10, steel, 6, top=steel_hi)
    hx, hy = h["front"]
    for x in range(10):
        px(img, hx + x, hy, steel_dark)
    # 위험 표시 줄무늬 테두리
    htx, hty = h["top"]
    for x in range(10):
        for y in range(10):
            if x == 0 or y == 0 or x == 9 or y == 9:
                px(img, htx + x, hty + y, hazard_y if (x + y) % 2 == 0 else hazard_k)
            elif (x + y) % 7 == 0:
                px(img, htx + x, hty + y, steel_dark)
    # 가운데 손잡이 자리
    for x in range(4, 6):
        for y in range(4, 6):
            px(img, htx + x, hty + y, steel_dark)
    for side in ("right", "left", "back"):
        sx, sy = h[side]
        for x in range(10):
            px(img, sx + x, sy, steel_dark)

    # 손잡이 막대 (u0 v32, 4x1x1)
    hb = box(img, 0, 32, 4, 1, 1, bar, 4, top=hexc("#a8aeb4"))
    hbx, hby = hb["front"]
    for x in range(4):
        px(img, hbx + x, hby, bar_dark)

    # 등불 기둥 (u0 v40, 1x6x1)
    p = box(img, 0, 40, 1, 6, 1, bar, 4)
    px_, py_ = p["front"]
    for y in range(6):
        px(img, px_, py_ + y, bar_dark if y % 2 == 0 else bar)

    # 희미한 전구 (u8 v40, 2x2x2)
    b = box(img, 8, 40, 2, 2, 2, bulb, 6, top=bulb_hi)
    bx, by = b["front"]
    px(img, bx, by, bulb_hi)
    px(img, bx + 1, by + 1, bulb_dark)
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        px(img, sx, sy, bulb_hi)
        px(img, sx + 1, sy + 1, bulb_dark)

    save(img, "textures/entity/shelter.png")


shelter()
print("assets shelter ok")

merge_lang({
    "entity.lobcorp.shelter": "3월 27일의 대피소",
}, {
    "entity.lobcorp.shelter": "Shelter from the 27th of March",
})


# ═════════════ 21차: 다른 세계의 초상 (O-09-91) ═════════════
def portrait():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    wood = hexc("#6b4a2a")
    wood_hi = hexc("#7d5a36")
    wood_dark = hexc("#4a3220")
    gold = hexc("#c8a040")
    gold_hi = hexc("#e8c060")
    gold_dark = hexc("#8a6a20")
    sky = hexc("#3a2a4a")
    sky_hi = hexc("#4e3a62")
    ground = hexc("#6a4a3a")
    ground_dark = hexc("#4a3228")
    figure = hexc("#e8e2d8")
    figure_shade = hexc("#c8c0b4")
    smear = hexc("#8a7a8a")

    # 이젤 다리 (u0 v0, 1x16x1)
    for u in (0, 4):
        l = box(img, u, 0, 1, 16, 1, wood, 8)
        lx, ly = l["front"]
        for y in range(16):
            px(img, lx, ly + y, wood_dark if y % 4 == 0 else wood)
    # 받침대 (u0 v20, 12x1x3)
    s = box(img, 0, 20, 12, 1, 3, wood, 8, top=wood_hi)
    sx, sy = s["front"]
    for x in range(12):
        px(img, sx + x, sy, wood_dark)

    # 캔버스 (u0 v24, 12x14x1)
    c = box(img, 0, 24, 12, 14, 1, sky, 4)
    cx, cy = c["front"]
    # 하늘: 위쪽은 보랏빛, 아래로 갈수록 흐려진다
    for y in range(14):
        for x in range(12):
            if y < 5:
                px(img, cx + x, cy + y, sky if (x + y) % 5 else sky_hi)
            elif y < 8:
                px(img, cx + x, cy + y, hexc("#5a4258") if (x * 3 + y) % 7 else sky_hi)
            else:
                px(img, cx + x, cy + y, ground if (x + y * 2) % 6 else ground_dark)
    # 지평선
    for x in range(12):
        px(img, cx + x, cy + 8, hexc("#2a1e2a"))
    # 멀리 보이는 봉우리
    for (x, h) in [(1, 2), (2, 3), (3, 2), (8, 2), (9, 3), (10, 2)]:
        for y in range(h):
            px(img, cx + x, cy + 7 - y, hexc("#2e2236"))
    # 창백한 형체: 서 있는 사람
    for y in range(3, 8):
        px(img, cx + 5, cy + y, figure)
        px(img, cx + 6, cy + y, figure_shade)
    px(img, cx + 5, cy + 2, figure)
    px(img, cx + 6, cy + 2, figure_shade)
    # 번진 얼굴
    for (x, y) in [(5, 2), (6, 2), (5, 3), (6, 3), (4, 3), (7, 3), (5, 4), (6, 4)]:
        px(img, cx + x, cy + y, smear)
    # 그림자
    for x in range(4, 8):
        px(img, cx + x, cy + 8, hexc("#1e1620"))

    # 금장 액자 (u40 v24, 13x1x2 / u40 v28, 1x15x2)
    ft = box(img, 40, 24, 13, 1, 2, gold, 6, top=gold_hi)
    ftx, fty = ft["front"]
    for x in range(13):
        px(img, ftx + x, fty, gold_dark)
    fl = box(img, 40, 28, 1, 15, 2, gold, 6)
    flx, fly = fl["front"]
    for y in range(15):
        px(img, flx, fly + y, gold_dark if y % 3 == 0 else gold)

    save(img, "textures/entity/portrait.png")


portrait()
print("assets portrait ok")

merge_lang({
    "entity.lobcorp.portrait": "다른 세계의 초상",
}, {
    "entity.lobcorp.portrait": "Portrait of Another World",
})


# ═════════════ 22차: 미친 연구원의 노트 (T-09-78) ═════════════
def notes():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    wood = hexc("#5a3a22")
    wood_hi = hexc("#6e4a2c")
    wood_dark = hexc("#3a2416")
    page = hexc("#e8e0c8")
    page_hi = hexc("#f4eeda")
    page_dark = hexc("#c8c0a4")
    ink = hexc("#2a2018")
    ink_red = hexc("#8a2a1a")
    stain = hexc("#a89060")

    # 바닥 받침 (u0 v0, 6x1x6)
    b = box(img, 0, 0, 6, 1, 6, wood, 8, top=wood_hi)
    bx, by = b["front"]
    for x in range(6):
        px(img, bx + x, by, wood_dark)
    tx, ty = b["top"]
    for x in range(6):
        for y in range(6):
            if (x * 3 + y * 5) % 7 == 0:
                px(img, tx + x, ty + y, wood_dark)
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for x in range(6):
            px(img, sx + x, sy, wood_dark)

    # 기둥 (u0 v8, 2x10x2)
    p = box(img, 0, 8, 2, 10, 2, wood, 8)
    px_, py_ = p["front"]
    for y in range(10):
        px(img, px_, py_ + y, wood_dark if y % 3 == 0 else wood)
        px(img, px_ + 1, py_ + y, wood_hi if y % 4 == 0 else wood)
    for side in ("right", "left", "back"):
        sx, sy = p[side]
        for y in range(10):
            px(img, sx, sy + y, wood_dark if y % 3 == 0 else wood)

    # 비스듬한 상판 (u0 v22, 8x1x6)
    t = box(img, 0, 22, 8, 1, 6, wood, 8, top=wood_hi)
    tx2, ty2 = t["top"]
    for x in range(8):
        for y in range(6):
            if (x + y * 2) % 5 == 0:
                px(img, tx2 + x, ty2 + y, wood_dark)
    for side in ("right", "left", "back"):
        sx, sy = t[side]
        for x in range(6):
            px(img, sx + x, sy, wood_dark)

    # 펼쳐진 노트 (u0 v32, 7x1x5)
    bk = box(img, 0, 32, 7, 1, 5, page, 4, top=page_hi)
    bkx, bky = bk["front"]
    for x in range(7):
        px(img, bkx + x, bky, page_dark)
    # 책등
    for y in range(5):
        px(img, bkx + 3, bky + y, page_dark)
    # 페이지 위의 휘갈긴 글씨
    btx, bty = bk["top"]
    for y in range(5):
        for x in range(7):
            if x == 3:
                px(img, btx + x, bty + y, page_dark)
                continue
            if (x * 5 + y * 3) % 4 == 0:
                px(img, btx + x, bty + y, ink)
            elif (x * 7 + y) % 9 == 0:
                px(img, btx + x, bty + y, ink_red)
    # 얼룩
    for (x, y) in [(1, 1), (2, 2), (5, 3), (6, 1)]:
        px(img, btx + x, bty + y, stain)
    # 페이지 가장자리
    for x in range(7):
        px(img, btx + x, bty, page_dark)
        px(img, btx + x, bty + 4, page_dark)

    # 표지 (u0 v40, 7x1x5)
    cv = box(img, 0, 40, 7, 1, 5, wood_dark, 6, top=hexc("#4a3020"))
    cvx, cvy = cv["front"]
    for x in range(7):
        px(img, cvx + x, cvy, hexc("#2a1a10"))
    ctx, cty = cv["top"]
    for x in range(7):
        for y in range(5):
            if (x + y) % 6 == 0:
                px(img, ctx + x, cty + y, hexc("#2a1a10"))

    save(img, "textures/entity/notes.png")


notes()
print("assets notes ok")

merge_lang({
    "entity.lobcorp.notes": "미친 연구원의 노트",
}, {
    "entity.lobcorp.notes": "Notes from a Crazed Researcher",
})


# ═════════════ 23차: 은하수의 아이 (O-01-55) ═════════════
def child_galaxy():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    navy = hexc("#141a3a")
    navy_hi = hexc("#1e2650")
    navy_dark = hexc("#0a0e22")
    star_w = hexc("#ffffff")
    star_b = hexc("#9ad0ff")
    skin = hexc("#f0e6dc")
    skin_shade = hexc("#d8c8bc")
    eye = hexc("#2a2a4a")
    pebble = hexc("#c0f0ff")
    pebble_hi = hexc("#f0ffff")
    pebble_dark = hexc("#6aa8c8")

    # 다리 (u0 v0, 2x4x2)
    for u in (0, 4):
        l = box(img, u, 0, 2, 4, 2, navy_dark, 4)
        lx, ly = l["front"]
        for y in range(4):
            px(img, lx, ly + y, hexc("#05081a"))
        for x in range(2):
            px(img, lx + x, ly + 3, hexc("#05081a"))

    # 몸통 (u0 v8, 5x5x3)
    b = box(img, 0, 8, 5, 5, 3, navy, 4)
    bx, by = b["front"]
    for (x, y) in [(1, 1), (3, 2), (2, 4)]:
        px(img, bx + x, by + y, star_b)

    # 망토 (u0 v16, 8x9x5)
    c = box(img, 0, 16, 8, 9, 5, navy, 4, top=navy_hi)
    cx, cy = c["front"]
    # 별가루: 흰 별과 푸른 별
    for (x, y) in [(1, 1), (4, 2), (6, 1), (2, 4), (5, 5), (0, 6), (7, 4), (3, 7), (6, 7)]:
        px(img, cx + x, cy + y, star_w)
    for (x, y) in [(3, 1), (0, 3), (6, 3), (4, 4), (1, 5), (5, 6), (2, 8), (7, 8)]:
        px(img, cx + x, cy + y, star_b)
    for x in range(8):
        px(img, cx + x, cy + 8, navy_dark)
    for side in ("right", "left", "back"):
        sx, sy = c[side]
        for y in range(9):
            for x in range(0, 5, 2):
                if (x + y) % 3 == 0:
                    px(img, sx + x, sy + y, star_b if (x + y) % 2 == 0 else star_w)
        for x in range(5):
            px(img, sx + x, sy + 8, navy_dark)
    tx, ty = c["top"]
    for x in range(8):
        for y in range(5):
            if (x * 3 + y * 5) % 7 == 0:
                px(img, tx + x, ty + y, star_w)

    # 망토 자락 (u26 v16, 9x2x6)
    h = box(img, 26, 16, 9, 2, 6, navy, 4, top=navy_hi)
    hx, hy = h["front"]
    for (x, y) in [(1, 0), (4, 1), (7, 0)]:
        px(img, hx + x, hy + y, star_b)
    for x in range(9):
        px(img, hx + x, hy + 1, navy_dark)

    # 두건 (u0 v30, 7x6x6)
    hd = box(img, 0, 30, 7, 6, 6, navy, 4, top=navy_hi)
    hdx, hdy = hd["front"]
    for (x, y) in [(0, 0), (6, 1), (1, 5), (5, 5)]:
        px(img, hdx + x, hdy + y, star_w)
    for (x, y) in [(3, 0), (0, 3), (6, 3)]:
        px(img, hdx + x, hdy + y, star_b)
    for side in ("right", "left", "back"):
        sx, sy = hd[side]
        for y in range(6):
            for x in range(0, 6, 2):
                if (x + y) % 4 == 0:
                    px(img, sx + x, sy + y, star_b)

    # 두건 속 얼굴 (u26 v30, 4x4x1)
    f = box(img, 26, 30, 4, 4, 1, skin, 4)
    fx, fy = f["front"]
    # 조용한 눈 두 개
    px(img, fx + 1, fy + 1, eye)
    px(img, fx + 2, fy + 1, eye)
    px(img, fx + 1, fy + 2, hexc("#4a4a6a"))
    px(img, fx + 2, fy + 2, hexc("#4a4a6a"))
    # 입
    px(img, fx + 1, fy + 3, skin_shade)
    px(img, fx + 2, fy + 3, skin_shade)
    # 볼
    px(img, fx, fy + 2, hexc("#e8c8c8"))
    px(img, fx + 3, fy + 2, hexc("#e8c8c8"))

    # 팔 (u36 v30, 2x4x2)
    a = box(img, 36, 30, 2, 4, 2, navy, 4)
    ax, ay = a["front"]
    for y in range(4):
        px(img, ax, ay + y, navy_dark)
    for x in range(2):
        px(img, ax + x, ay + 3, skin)

    # 조약돌 (u44 v30, 1x1x1)
    p = box(img, 44, 30, 1, 1, 1, pebble, 0, top=pebble_hi)
    px_, py_ = p["front"]
    px(img, px_, py_, pebble_hi)
    for side in ("right", "left", "back", "bottom"):
        sx, sy = p[side]
        px(img, sx, sy, pebble_dark)

    save(img, "textures/entity/child_galaxy.png")


OG = {"n": "#141a3a", "h": "#1e2650", "d": "#0a0e22", "w": "#ffffff", "b": "#9ad0ff", "p": "#c0f0ff"}
icon([
    "................",
    "..............nn",
    ".............nn.",
    "............nn..",
    "...........nn...",
    "..........nn....",
    ".........nn.....",
    "........nn......",
    ".......nn.......",
    "......nn........",
    ".....nn.........",
    "....nn..........",
    "...dd...........",
    "..dd............",
    "..d.............",
    "................",
], OG, "textures/item/our_galaxy.png")
icon([
    "................",
    "...nn......nn...",
    "..nnnn....nnnn..",
    "..nnnnnnnnnnnn..",
    "...nnnnnnnnnn...",
    "....nnwwbbnn....",
    "....nnwwbbnn....",
    "....nnbbwwnn....",
    "....nnbbwwnn....",
    "....nnnnnnnn....",
    "....nnppppnn....",
    "....nnnnnnnn....",
    "....nnnnnnnn....",
    ".....dddddd.....",
    "................",
    "................",
], OG, "textures/item/our_galaxy_suit.png")

armor(hexc("#141a3a"), "#0a0e22", "#9ad0ff", "textures/entity/equipment/humanoid/our_galaxy.png")

for item, parent in [("our_galaxy", "minecraft:item/handheld"), ("our_galaxy_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/our_galaxy.json", {"layers": {"humanoid": [{"texture": "lobcorp:our_galaxy"}]}})


child_galaxy()
print("assets child ok")

merge_lang({
    "entity.lobcorp.child_galaxy": "은하수의 아이",
}, {
    "entity.lobcorp.child_galaxy": "Child of the Galaxy",
})
