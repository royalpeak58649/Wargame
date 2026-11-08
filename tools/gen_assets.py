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


# ═════════════ 24차: 마탄의 사수 (F-01-69) ═════════════
def freischutz():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    coat = hexc("#1e1e24")
    coat_hi = hexc("#2a2a32")
    coat_dark = hexc("#121216")
    skin = hexc("#d8d2c4")
    skin_shade = hexc("#b8b2a4")
    hat = hexc("#141418")
    hat_hi = hexc("#22222a")
    band = hexc("#3a2a1a")
    eye = hexc("#ff2a2a")
    eye_dim = hexc("#8a0a0a")
    wood = hexc("#4a3220")
    wood_hi = hexc("#5e4028")
    steel = hexc("#8a9096")
    steel_dark = hexc("#5a6066")

    # 다리 (u0 v0, 2x12x2)
    for u in (0, 8):
        l = box(img, u, 0, 2, 12, 2, coat_dark, 4)
        lx, ly = l["front"]
        for y in range(12):
            px(img, lx, ly + y, hexc("#0a0a0e"))
        for x in range(2):
            px(img, lx + x, ly + 11, hexc("#0a0a0e"))

    # 몸통: 긴 코트 (u0 v16, 6x10x3)
    b = box(img, 0, 16, 6, 10, 3, coat, 4)
    bx, by = b["front"]
    # 코트 여밈 단추
    for y in (2, 4, 6, 8):
        px(img, bx + 2, by + y, hexc("#8a7a5a"))
    for y in range(10):
        px(img, bx + 3, by + y, coat_dark)
    for x in range(6):
        px(img, bx + x, by + 9, coat_dark)
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for y in range(10):
            if y % 3 == 0:
                px(img, sx + 1, sy + y, coat_hi)

    # 코트 자락 (u20 v16, 8x6x4)
    c = box(img, 20, 16, 8, 6, 4, coat, 4, top=coat_hi)
    cx, cy = c["front"]
    for y in range(6):
        px(img, cx + 3, cy + y, coat_dark)
        px(img, cx + 4, cy + y, coat_dark)
    for x in range(8):
        px(img, cx + x, cy + 5, coat_dark)
    for side in ("right", "left", "back"):
        sx, sy = c[side]
        for y in range(6):
            if y % 2 == 0:
                px(img, sx + 2, sy + y, coat_hi)

    # 머리 (u0 v32, 5x6x5)
    h = box(img, 0, 32, 5, 6, 5, skin, 4, top=hat)
    hx, hy = h["front"]
    # 한쪽 눈만 붉게 빛난다
    px(img, hx + 1, hy + 2, eye)
    px(img, hx + 1, hy + 3, eye_dim)
    # 다른 쪽 눈은 그림자에 묻혀 있다
    px(img, hx + 3, hy + 2, hexc("#3a3430"))
    # 마른 볼과 입
    px(img, hx, hy + 3, skin_shade)
    px(img, hx + 4, hy + 3, skin_shade)
    for x in (1, 2, 3):
        px(img, hx + x, hy + 5, hexc("#6a5a50"))
    # 턱수염 자국
    for x in range(5):
        px(img, hx + x, hy + 4, skin_shade if x % 2 == 0 else skin)

    # 챙모자 (u24 v32, 10x1x10 + u24 v44, 5x3x5)
    ht = box(img, 24, 32, 10, 1, 10, hat, 4, top=hat_hi)
    htx, hty = ht["front"]
    for x in range(10):
        px(img, htx + x, hty, hat_hi)
    htx2, hty2 = ht["top"]
    for x in range(10):
        for y in range(10):
            if (x + y) % 7 == 0:
                px(img, htx2 + x, hty2 + y, hat_hi)
    cr = box(img, 24, 44, 5, 3, 5, hat, 4, top=hat_hi)
    crx, cry = cr["front"]
    for x in range(5):
        px(img, crx + x, cry + 2, band)
    for side in ("right", "left", "back"):
        sx, sy = cr[side]
        for x in range(5):
            px(img, sx + x, sy + 2, band)

    # 팔 (u44 v0, 2x10x2 / u52 v0, 2x10x2)
    for u in (44, 52):
        a = box(img, u, 0, 2, 10, 2, coat, 4)
        ax, ay = a["front"]
        for y in range(10):
            px(img, ax, ay + y, coat_dark)
        for x in range(2):
            px(img, ax + x, ay + 9, skin)

    # 장총 (u0 v48, 1x16x1) — 64x64 텍스처의 아래쪽 경계에 걸치므로 1x15로 줄여 그린다
    m = box(img, 0, 48, 1, 15, 1, wood, 6)
    mx, my = m["front"]
    for y in range(15):
        px(img, mx, my + y, wood_hi if y % 4 == 0 else wood)
    # 총열
    for y in range(0, 9):
        px(img, mx, my + y, steel if y % 3 == 0 else steel_dark)
    # 개머리판
    for y in range(11, 15):
        px(img, mx, my + y, hexc("#2e1e12"))
    # 방아쇠울
    px(img, mx, my + 10, hexc("#c8a040"))

    save(img, "textures/entity/freischutz.png")


MB = {"k": "#1e1e24", "d": "#121216", "s": "#8a9096", "w": "#4a3220", "r": "#ff2a2a", "g": "#c8a040"}
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
    "....ww..........",
    "...ww...........",
    "..gg............",
    "..g.............",
    "................",
], MB, "textures/item/magic_bullet.png")
icon([
    "................",
    "...kk......kk...",
    "..kkkk....kkkk..",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "....kkrrrrkk....",
    "....kkrrrrkk....",
    "....kkrrrrkk....",
    "....kkrrrrkk....",
    "....kkkkkkkk....",
    "....kksssskk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....dddddd.....",
    "................",
    "................",
], MB, "textures/item/magic_bullet_suit.png")

armor(hexc("#1e1e24"), "#121216", "#ff2a2a", "textures/entity/equipment/humanoid/magic_bullet.png")

for item, parent in [("magic_bullet", "minecraft:item/handheld"), ("magic_bullet_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/magic_bullet.json", {"layers": {"humanoid": [{"texture": "lobcorp:magic_bullet"}]}})


freischutz()
print("assets freischutz ok")

merge_lang({
    "entity.lobcorp.freischutz": "마탄의 사수",
    "item.lobcorp.magic_bullet": "마탄",
    "item.lobcorp.magic_bullet_suit": "마탄의 코트",
}, {
    "entity.lobcorp.freischutz": "Der Freischütz",
    "item.lobcorp.magic_bullet": "Magic Bullet",
    "item.lobcorp.magic_bullet_suit": "Magic Bullet Coat",
})


# ═════════════ 25차: 이름 없는 태아 (O-01-15) ═════════════
def nameless_fetus():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    flesh = hexc("#d8b8b0")
    flesh_hi = hexc("#e8ccc4")
    flesh_dark = hexc("#b89890")
    vein = hexc("#8a5a6a")
    vein_dark = hexc("#6a3a4a")
    mouth = hexc("#401018")
    mouth_dark = hexc("#2a0810")
    tooth = hexc("#f4eee4")
    gum = hexc("#7a2030")
    eye = hexc("#2a1a20")
    tear = hexc("#9ad0e8")

    # 몸통 (u0 v0, 16x10x12)
    b = box(img, 0, 0, 16, 10, 12, flesh, 6, top=flesh_hi)
    bx, by = b["front"]
    # 살의 주름
    for y in range(10):
        for x in range(16):
            if (x * 3 + y * 7) % 23 == 0:
                px(img, bx + x, by + y, flesh_dark)
    # 표면 아래 혈관
    for (x, y) in [(1, 1), (2, 1), (3, 2), (4, 2), (12, 1), (13, 2), (14, 2), (15, 3),
                   (1, 7), (2, 8), (13, 7), (14, 8), (7, 1), (8, 1)]:
        px(img, bx + x, by + y, vein)
    for (x, y) in [(2, 2), (13, 3), (2, 9), (13, 8)]:
        px(img, bx + x, by + y, vein_dark)
    for x in range(16):
        px(img, bx + x, by + 9, flesh_dark)
    # 옆면·뒷면·윗면에도 주름과 혈관
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for y in range(10):
            for x in range(12):
                if (x * 5 + y * 3) % 19 == 0:
                    px(img, sx + x, sy + y, flesh_dark)
                elif (x * 7 + y) % 31 == 0:
                    px(img, sx + x, sy + y, vein)
        for x in range(12):
            px(img, sx + x, sy + 9, flesh_dark)
    tx, ty = b["top"]
    for x in range(16):
        for y in range(12):
            if (x * 3 + y * 5) % 17 == 0:
                px(img, tx + x, ty + y, flesh_dark)
            elif (x + y * 2) % 29 == 0:
                px(img, tx + x, ty + y, vein)

    # 입 판 (u0 v24, 10x6x1) — 어두운 아가리 + 작은 이빨
    m = box(img, 0, 24, 10, 6, 1, mouth, 6)
    mx, my = m["front"]
    for x in range(10):
        px(img, mx + x, my, gum)
        px(img, mx + x, my + 5, gum)
    for x in range(10):
        if x % 2 == 0:
            px(img, mx + x, my + 1, tooth)
        if x % 2 == 1:
            px(img, mx + x, my + 4, tooth)
    for x in range(2, 8):
        px(img, mx + x, my + 3, mouth_dark)
    for (x, y) in [(3, 2), (6, 2)]:
        px(img, mx + x, my + y, hexc("#1a0408"))

    # 머리 (u24 v24, 8x4x8)
    h = box(img, 24, 24, 8, 4, 8, flesh, 6, top=flesh_hi)
    hx, hy = h["front"]
    for x in range(8):
        px(img, hx + x, hy + 3, flesh_dark)
    for (x, y) in [(1, 1), (6, 1), (3, 2), (4, 2)]:
        px(img, hx + x, hy + y, flesh_dark)
    for side in ("right", "left", "back"):
        sx, sy = h[side]
        for x in range(8):
            px(img, sx + x, sy + 3, flesh_dark)

    # 눈 (u56 v24, 2x1x1 두 개) — 감긴 눈 / 젖은 눈
    e = box(img, 56, 24, 2, 1, 1, eye, 0)
    ex, ey = e["front"]
    px(img, ex, ey, hexc("#4a3038"))
    px(img, ex + 1, ey, hexc("#4a3038"))

    # 눈물 (u60 v24, 1x1x1)
    t = box(img, 60, 24, 1, 1, 1, tear, 0)
    ttx, tty = t["front"]
    px(img, ttx, tty, hexc("#d8f0fa"))

    # 팔 (u0 v40, 2x3x2)
    a = box(img, 0, 40, 2, 3, 2, flesh, 6)
    ax, ay = a["front"]
    for y in range(3):
        px(img, ax, ay + y, flesh_dark)
    for x in range(2):
        px(img, ax + x, ay + 2, flesh_dark)
    # 다리 (u8 v40, 2x3x2)
    l = box(img, 8, 40, 2, 3, 2, flesh, 6)
    lx, ly = l["front"]
    for y in range(3):
        px(img, lx, ly + y, flesh_dark)
    for x in range(2):
        px(img, lx + x, ly + 2, flesh_dark)

    save(img, "textures/entity/nameless_fetus.png")


SY = {"w": "#e8e0d0", "s": "#c8c0b0", "d": "#8a8070", "k": "#2a1a20", "r": "#8a2a3a", "h": "#3a2a1a"}
icon([
    "................",
    "..............ww",
    ".............ww.",
    "............ww..",
    "...........ww...",
    "..........ww....",
    ".........ww.....",
    "........ww......",
    ".......ww.......",
    "......ww........",
    ".....ww.........",
    "....ww..........",
    "...dd...........",
    "..dd............",
    "..d.............",
    "................",
], SY, "textures/item/syrinx.png")
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
    "....wwkkkkww....",
    "....wwwwwwww....",
    "....wwwwwwww....",
    ".....dddddd.....",
    "................",
    "................",
], SY, "textures/item/syrinx_suit.png")

armor(hexc("#d8b8b0"), "#b89890", "#8a5a6a", "textures/entity/equipment/humanoid/syrinx.png")

for item, parent in [("syrinx", "minecraft:item/handheld"), ("syrinx_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/syrinx.json", {"layers": {"humanoid": [{"texture": "lobcorp:syrinx"}]}})


nameless_fetus()
print("assets fetus ok")


# ═════════════ 26차: 눈의 여왕 (F-01-37) ═════════════
def snow_queen():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    ice = hexc("#bfe8ff")
    ice_hi = hexc("#e8f8ff")
    ice_dark = hexc("#7ab8d8")
    gown = hexc("#e8f6ff")
    gown_shade = hexc("#c0dcee")
    gown_blue = hexc("#5a9ad0")
    skin = hexc("#fbfdff")
    skin_shade = hexc("#dce8f0")
    lip = hexc("#c8a0b8")
    hair = hexc("#dff0fa")
    hair_dark = hexc("#a8c8dc")
    crown = hexc("#d8f4ff")
    crown_dark = hexc("#8ac8e8")

    # 왕좌 좌판 (u0 v0, 10x2x8)
    s = box(img, 0, 0, 10, 2, 8, ice, 6, top=ice_hi)
    sx, sy = s["front"]
    for x in range(10):
        px(img, sx + x, sy + 1, ice_dark)
    for side in ("right", "left", "back"):
        ox, oy = s[side]
        for y in range(2):
            for x in range(0, 8, 3):
                px(img, ox + x, oy + y, ice_dark)

    # 왕좌 등받이 (u0 v10, 10x14x2)
    b = box(img, 0, 10, 10, 14, 2, ice, 6, top=ice_hi)
    bx, by = b["front"]
    # 얼음 결정 무늬
    for y in range(14):
        for x in range(10):
            if (x * 3 + y * 5) % 11 == 0:
                px(img, bx + x, by + y, ice_hi)
            elif (x * 7 + y) % 13 == 0:
                px(img, bx + x, by + y, ice_dark)
    for x in range(10):
        px(img, bx + x, by + 13, ice_dark)
    # 등받이 꼭대기의 고드름
    for x in (1, 4, 7):
        px(img, bx + x, by, ice_hi)
        px(img, bx + x, by + 1, ice_hi)

    # 팔걸이 (u24 v10, 1x3x8)
    for u in (24, 28):
        a = box(img, u, 10, 1, 3, 8, ice, 6, top=ice_hi)
        ax, ay = a["front"]
        for y in range(3):
            px(img, ax, ay + y, ice_dark if y == 2 else ice)

    # 드레스 (u0 v26, 8x10x6)
    g = box(img, 0, 26, 8, 10, 6, gown, 4, top=hexc("#f4fbff"))
    gx, gy = g["front"]
    # 푸른 무늬: 세로 줄기와 눈꽃
    for y in range(10):
        px(img, gx + 3, gy + y, gown_blue)
        px(img, gx + 4, gy + y, gown_blue)
    for (x, y) in [(1, 2), (2, 2), (1, 3), (6, 4), (5, 4), (6, 5), (2, 7), (5, 7)]:
        px(img, gx + x, gy + y, gown_blue)
    for x in range(8):
        px(img, gx + x, gy + 9, gown_shade)
    for side in ("right", "left", "back"):
        ox, oy = g[side]
        for y in range(10):
            for x in range(0, 6, 2):
                if (x + y) % 4 == 0:
                    px(img, ox + x, oy + y, gown_shade)
        for x in range(6):
            px(img, ox + x, oy + 9, gown_shade)

    # 몸통 (u28 v26, 6x6x3)
    bo = box(img, 28, 26, 6, 6, 3, gown, 4)
    box_, boy = bo["front"]
    for y in range(6):
        px(img, box_ + 2, boy + y, gown_blue)
        px(img, box_ + 3, boy + y, gown_blue)
    for x in range(6):
        px(img, box_ + x, boy, gown_shade)

    # 팔 (u46 v26, 2x8x2)
    a = box(img, 46, 26, 2, 8, 2, gown, 4)
    ax, ay = a["front"]
    for y in range(8):
        px(img, ax, ay + y, gown_shade)
    for x in range(2):
        px(img, ax + x, ay + 7, skin)

    # 머리 (u0 v42, 5x5x5)
    h = box(img, 0, 42, 5, 5, 5, skin, 3, top=hair)
    hx, hy = h["front"]
    # 창백한 입술
    px(img, hx + 1, hy + 4, lip)
    px(img, hx + 2, hy + 4, lip)
    px(img, hx + 3, hy + 4, lip)
    # 차가운 눈
    px(img, hx + 1, hy + 2, hexc("#3a7aa8"))
    px(img, hx + 3, hy + 2, hexc("#3a7aa8"))
    px(img, hx + 1, hy + 1, hair_dark)
    px(img, hx + 3, hy + 1, hair_dark)
    # 앞머리
    for x in range(5):
        px(img, hx + x, hy, hair)
    for x in (0, 2, 4):
        px(img, hx + x, hy + 1, hair_dark)
    # 옆머리
    for side in ("right", "left"):
        ox, oy = h[side]
        for y in range(5):
            px(img, ox + 2, oy + y, hair if y < 3 else hair_dark)

    # 고드름 왕관 (u20 v42, 1x3x1 다섯 개)
    c = box(img, 20, 42, 1, 3, 1, crown, 4, top=ice_hi)
    cx, cy = c["front"]
    for y in range(3):
        px(img, cx, cy + y, crown_dark if y == 2 else crown)
    # 왕관 띠 (u28 v42, 5x1x5)
    cb = box(img, 28, 42, 5, 1, 5, crown, 4, top=ice_hi)
    cbx, cby = cb["front"]
    for x in range(5):
        px(img, cbx + x, cby, crown_dark)

    save(img, "textures/entity/snow_queen.png")


FS = {"i": "#bfe8ff", "h": "#e8f8ff", "d": "#7ab8d8", "w": "#e8f6ff", "b": "#5a9ad0", "k": "#3a7aa8"}
icon([
    "................",
    "..............hh",
    ".............hh.",
    "............hh..",
    "...........hh...",
    "..........hh....",
    ".........hh.....",
    "........hh......",
    ".......hh.......",
    "......hh........",
    ".....hh.........",
    "....ii..........",
    "...dd...........",
    "..dd............",
    "..d.............",
    "................",
], FS, "textures/item/frost_splinter.png")
icon([
    "................",
    "...ii......ii...",
    "..iiii....iiii..",
    "..iiiiiiiiiiii..",
    "...iiiiiiiiii...",
    "....iiwwwwii....",
    "....iiwwwwii....",
    "....iiwwwwii....",
    "....iiwwwwii....",
    "....iiiiiiii....",
    "....iibbbbi.....",
    "....iiiiiiii....",
    "....iiiiiiii....",
    ".....dddddd.....",
    "................",
    "................",
], FS, "textures/item/frost_splinter_suit.png")

armor(hexc("#bfe8ff"), "#7ab8d8", "#e8f8ff", "textures/entity/equipment/humanoid/frost_splinter.png")

for item, parent in [("frost_splinter", "minecraft:item/handheld"),
                     ("frost_splinter_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/frost_splinter.json", {"layers": {"humanoid": [{"texture": "lobcorp:frost_splinter"}]}})


snow_queen()
print("assets snowqueen ok")

merge_lang({
    "entity.lobcorp.snow_queen": "눈의 여왕",
    "item.lobcorp.frost_splinter": "서리 조각",
    "item.lobcorp.frost_splinter_suit": "서리 조각의 옷",
}, {
    "entity.lobcorp.snow_queen": "The Snow Queen",
    "item.lobcorp.frost_splinter": "Frost Splinter",
    "item.lobcorp.frost_splinter_suit": "Frost Splinter Garb",
})

merge_lang({
    "entity.lobcorp.nameless_fetus": "이름 없는 태아",
    "item.lobcorp.syrinx": "시링크스",
    "item.lobcorp.syrinx_suit": "시링크스의 옷",
}, {
    "entity.lobcorp.nameless_fetus": "Nameless Fetus",
    "item.lobcorp.syrinx": "Syrinx",
    "item.lobcorp.syrinx_suit": "Syrinx Garb",
})


# ═════════════ 27차: 따뜻한 마음의 나무꾼 (F-05-32) ═════════════
def woodsman():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    tin = hexc("#a8acae")
    tin_hi = hexc("#c0c4c6")
    tin_dark = hexc("#7a7e82")
    rust = hexc("#8a5a3a")
    rust_dark = hexc("#5e3a22")
    hollow = hexc("#141212")
    hollow_hi = hexc("#2a2422")
    steel = hexc("#7a7e82")
    steel_hi = hexc("#9aa0a4")
    steel_dark = hexc("#4a4e52")
    wood = hexc("#6b4a2a")
    wood_dark = hexc("#4a3220")
    eye = hexc("#e8a030")

    # 몸통 (u0 v0, 8x9x5)
    b = box(img, 0, 0, 8, 9, 5, tin, 6, top=tin_hi)
    bx, by = b["front"]
    # 녹슨 얼룩
    for (x, y) in [(0, 1), (1, 1), (0, 2), (6, 4), (7, 4), (6, 5), (2, 7), (3, 7), (7, 0)]:
        px(img, bx + x, by + y, rust)
    for (x, y) in [(1, 2), (6, 6), (2, 8)]:
        px(img, bx + x, by + y, rust_dark)
    # 가슴의 빈자리: 어두운 구멍 + 안쪽 그늘
    rect(img, bx + 2, by + 2, 3, 3, hollow, 2)
    px(img, bx + 2, by + 2, hollow_hi)
    px(img, bx + 4, by + 4, hollow_hi)
    # 이음매
    for x in range(8):
        px(img, bx + x, by + 8, tin_dark)
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for y in range(9):
            if y % 3 == 0:
                px(img, sx + 1, sy + y, tin_dark)
        for (x, y) in [(0, 2), (3, 6)]:
            px(img, sx + x, sy + y, rust)
        for x in range(5):
            px(img, sx + x, sy + 8, tin_dark)

    # 머리 (u0 v16, 6x7x6)
    h = box(img, 0, 16, 6, 7, 6, tin, 6, top=tin_hi)
    hx, hy = h["front"]
    # 깔때기 모자 그늘
    for x in range(6):
        px(img, hx + x, hy, tin_dark)
    # 눈: 작고 노란 점
    px(img, hx + 1, hy + 2, eye)
    px(img, hx + 4, hy + 2, eye)
    px(img, hx + 1, hy + 3, hexc("#8a5a10"))
    px(img, hx + 4, hy + 3, hexc("#8a5a10"))
    # 입: 가로로 난 틈
    for x in range(2, 4):
        px(img, hx + x, hy + 5, hollow)
    # 볼의 녹
    px(img, hx, hy + 4, rust)
    px(img, hx + 5, hy + 4, rust)
    for side in ("right", "left", "back"):
        sx, sy = h[side]
        for y in range(7):
            if y % 3 == 0:
                px(img, sx + 2, sy + y, tin_dark)
        px(img, sx + 1, sy + 4, rust)

    # 깔때기 챙 (u28 v16, 4x1x4)
    br = box(img, 28, 16, 4, 1, 4, tin, 6, top=tin_hi)
    brx, bry = br["front"]
    for x in range(4):
        px(img, brx + x, bry, tin_dark)
    # 깔때기 꼭대기 (u28 v22, 2x3x2)
    tp = box(img, 28, 22, 2, 3, 2, tin, 6, top=tin_hi)
    tpx, tpy = tp["front"]
    for y in range(3):
        px(img, tpx, tpy + y, tin_dark)
    for x in range(2):
        px(img, tpx + x, tpy, tin_hi)

    # 팔 (u24 v0, 3x9x3)
    a = box(img, 24, 0, 3, 9, 3, tin, 6, top=tin_hi)
    ax, ay = a["front"]
    for y in range(9):
        px(img, ax + 1, ay + y, tin_dark if y % 3 == 0 else tin)
    for (x, y) in [(0, 2), (2, 5)]:
        px(img, ax + x, ay + y, rust)
    for x in range(3):
        px(img, ax + x, ay + 8, tin_dark)

    # 다리 (u36 v0, 3x10x3)
    l = box(img, 36, 0, 3, 10, 3, tin, 6, top=tin_hi)
    lx, ly = l["front"]
    for y in range(10):
        px(img, lx + 1, ly + y, tin_dark if y % 4 == 0 else tin)
    for (x, y) in [(0, 3), (2, 7)]:
        px(img, lx + x, ly + y, rust)
    for x in range(3):
        px(img, lx + x, ly + 9, hexc("#3a3a3e"))

    # 도끼 자루 (u0 v32, 1x16x1)
    hd = box(img, 0, 32, 1, 16, 1, wood, 6)
    hdx, hdy = hd["front"]
    for y in range(16):
        px(img, hdx, hdy + y, wood_dark if y % 4 == 0 else wood)

    # 도끼날 (u8 v32, 5x4x1)
    bl = box(img, 8, 32, 5, 4, 1, steel, 6, top=steel_hi)
    blx, bly = bl["front"]
    # 날: 왼쪽으로 갈수록 밝은 날카로운 선
    for y in range(4):
        px(img, blx, bly + y, hexc("#e8eef2"))
        px(img, blx + 1, bly + y, steel_hi)
        px(img, blx + 4, bly + y, steel_dark)
    for x in range(5):
        px(img, blx + x, bly + 3, steel_dark)
    # 날의 녹
    px(img, blx + 3, bly + 1, rust)
    px(img, blx + 2, bly + 2, rust_dark)

    save(img, "textures/entity/woodsman.png")


LG = {"t": "#a8acae", "h": "#c0c4c6", "d": "#7a7e82", "r": "#8a5a3a", "k": "#141212", "w": "#6b4a2a"}
icon([
    "................",
    "..............tt",
    ".............tt.",
    "............tt..",
    "...........tt...",
    "..........tt....",
    ".........tt.....",
    "........tt......",
    ".......tt.......",
    "......tt........",
    ".....tt.........",
    "....ww..........",
    "...ww...........",
    "..rr............",
    "..r.............",
    "................",
], LG, "textures/item/logging.png")
icon([
    "................",
    "...tt......tt...",
    "..tttt....tttt..",
    "..tttttttttttt..",
    "...tttttttttt...",
    "....ttkkkktt....",
    "....ttkkkktt....",
    "....ttkkkktt....",
    "....ttkkkktt....",
    "....tttttttt....",
    "....ttrrrrtt....",
    "....tttttttt....",
    "....tttttttt....",
    ".....dddddd.....",
    "................",
    "................",
], LG, "textures/item/logging_suit.png")

armor(hexc("#a8acae"), "#7a7e82", "#8a5a3a", "textures/entity/equipment/humanoid/logging.png")

for item, parent in [("logging", "minecraft:item/handheld"), ("logging_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/logging.json", {"layers": {"humanoid": [{"texture": "lobcorp:logging"}]}})


woodsman()
print("assets woodsman ok")

merge_lang({
    "entity.lobcorp.woodsman": "따뜻한 마음의 나무꾼",
    "item.lobcorp.logging": "벌목",
    "item.lobcorp.logging_suit": "벌목의 옷",
}, {
    "entity.lobcorp.woodsman": "Warm-Hearted Woodsman",
    "item.lobcorp.logging": "Logging",
    "item.lobcorp.logging_suit": "Logging Garb",
})


# ═════════════ 28차: 지혜를 찾는 허수아비 (F-01-87) ═════════════
def scarecrow():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    wood = hexc("#6b4a2a")
    wood_hi = hexc("#7d5a36")
    wood_dark = hexc("#4a3220")
    straw = hexc("#d8b860")
    straw_hi = hexc("#e8cc80")
    straw_dark = hexc("#a88a3a")
    shirt = hexc("#6a5a8a")
    shirt_dark = hexc("#4a3e64")
    patch = hexc("#8a7aa8")
    sack = hexc("#c8b088")
    sack_hi = hexc("#d8c4a0")
    sack_dark = hexc("#a08a64")
    stitch = hexc("#3a2a1a")
    button = hexc("#1a1414")
    hat = hexc("#4a3a2a")
    hat_dark = hexc("#2e2418")

    # 기둥 (u0 v0, 2x24x2)
    p = box(img, 0, 0, 2, 24, 2, wood, 8, top=wood_hi)
    px_, py_ = p["front"]
    for y in range(24):
        px(img, px_, py_ + y, wood_dark if y % 5 == 0 else wood)
        px(img, px_ + 1, py_ + y, wood_hi if y % 7 == 0 else wood)
    for side in ("right", "left", "back"):
        sx, sy = p[side]
        for y in range(24):
            px(img, sx, sy + y, wood_dark if y % 5 == 0 else wood)

    # 몸통: 짚 + 기운 셔츠 (u0 v26, 6x8x4)
    b = box(img, 0, 26, 6, 8, 4, straw, 8, top=straw_hi)
    bx, by = b["front"]
    # 셔츠 자락
    for y in range(2, 8):
        for x in range(6):
            px(img, bx + x, by + y, shirt if (x + y) % 7 else shirt_dark)
    # 짚이 삐져나온 어깨와 밑단
    for x in range(6):
        px(img, bx + x, by, straw_hi if x % 2 == 0 else straw)
        px(img, bx + x, by + 7, straw_dark if x % 2 == 0 else straw)
    # 꿰맨 헝겊 조각
    for (x, y) in [(1, 3), (2, 3), (1, 4), (2, 4), (4, 5), (5, 5), (4, 6)]:
        px(img, bx + x, by + y, patch)
    for (x, y) in [(1, 3), (2, 4), (4, 5), (5, 6)]:
        px(img, bx + x, by + y, stitch)
    # 세로 짚 결
    for y in range(8):
        if y % 3 == 0:
            px(img, bx + 3, by + y, straw_dark)
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for y in range(2, 8):
            for x in range(4):
                px(img, sx + x, sy + y, shirt if (x * 3 + y) % 5 else shirt_dark)
        for x in range(4):
            px(img, sx + x, sy, straw_hi if x % 2 == 0 else straw)
            px(img, sx + x, sy + 7, straw_dark)

    # 부대 머리 (u20 v26, 6x7x6)
    h = box(img, 20, 26, 6, 7, 6, sack, 6, top=sack_hi)
    hx, hy = h["front"]
    # 단추 눈
    for (x, y) in [(1, 2), (4, 2)]:
        px(img, hx + x, hy + y, button)
        px(img, hx + x, hy + y + 1, hexc("#3a3430"))
    # 꿰맨 X자 입
    for i in range(3):
        px(img, hx + 1 + i, hy + 4 + i, stitch)
        px(img, hx + 3 - i, hy + 4 + i, stitch)
    # 부대의 주름과 꿰맨 자국
    for x in range(6):
        px(img, hx + x, hy + 6, sack_dark)
    for (x, y) in [(0, 1), (5, 3), (2, 0)]:
        px(img, hx + x, hy + y, sack_dark)
    for side in ("right", "left", "back"):
        sx, sy = h[side]
        for y in range(7):
            if y % 3 == 0:
                px(img, sx + 2, sy + y, sack_dark)
        for x in range(6):
            px(img, sx + x, sy + 6, sack_dark)

    # 찢어진 모자 챙 (u44 v26, 8x1x8)
    br = box(img, 44, 26, 8, 1, 8, hat, 6, top=hexc("#5a4834"))
    brx, bry = br["front"]
    for x in range(8):
        px(img, brx + x, bry, hat_dark if x % 3 == 0 else hat)
    # 챙의 찢긴 틈
    px(img, brx + 2, bry, (0, 0, 0, 0))
    px(img, brx + 6, bry, (0, 0, 0, 0))
    # 모자 꼭대기 (u44 v36, 4x3x4)
    cr = box(img, 44, 36, 4, 3, 4, hat, 6, top=hexc("#5a4834"))
    crx, cry = cr["front"]
    for x in range(4):
        px(img, crx + x, cry + 2, hat_dark)
    px(img, crx + 1, cry, (0, 0, 0, 0))

    # 팔: 짚 (u0 v40, 2x8x2 / u8 v40, 2x8x2)
    for u in (0, 8):
        a = box(img, u, 40, 2, 8, 2, straw, 8, top=straw_hi)
        ax, ay = a["front"]
        for y in range(8):
            px(img, ax, ay + y, straw_dark if y % 3 == 0 else straw)
            px(img, ax + 1, ay + y, straw_hi if y % 4 == 0 else straw)
        for x in range(2):
            px(img, ax + x, ay + 7, straw_dark)

    # 다리: 짚 (u16 v40, 2x10x2 / u24 v40, 2x10x2)
    for u in (16, 24):
        l = box(img, u, 40, 2, 10, 2, straw, 8, top=straw_hi)
        lx, ly = l["front"]
        for y in range(10):
            px(img, lx, ly + y, straw_dark if y % 4 == 0 else straw)
            px(img, lx + 1, ly + y, straw_hi if y % 5 == 0 else straw)
        for x in range(2):
            px(img, lx + x, ly + 9, straw_dark)

    save(img, "textures/entity/scarecrow.png")


HV = {"s": "#d8b860", "h": "#e8cc80", "d": "#a88a3a", "k": "#3a2a1a", "w": "#6b4a2a", "p": "#6a5a8a"}
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
    "....ww..........",
    "...ww...........",
    "..dd............",
    "..d.............",
    "................",
], HV, "textures/item/harvest.png")
icon([
    "................",
    "...pp......pp...",
    "..pppp....pppp..",
    "..pppppppppppp..",
    "...pppppppppp...",
    "....ppsssspp....",
    "....ppsssspp....",
    "....ppsssspp....",
    "....ppsssspp....",
    "....pppppppp....",
    "....pphhhhpp....",
    "....pppppppp....",
    "....pppppppp....",
    ".....kkkkkk.....",
    "................",
    "................",
], HV, "textures/item/harvest_suit.png")

armor(hexc("#6a5a8a"), "#4a3e64", "#d8b860", "textures/entity/equipment/humanoid/harvest.png")

for item, parent in [("harvest", "minecraft:item/handheld"), ("harvest_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/harvest.json", {"layers": {"humanoid": [{"texture": "lobcorp:harvest"}]}})


scarecrow()
print("assets scarecrow ok")


# ═════════════ 29차: 썰매 탄 루돌타 (F-02-49) ═════════════
def rudolta():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    fur = hexc("#6a4a32")
    fur_hi = hexc("#7d5a3e")
    fur_dark = hexc("#4a3220")
    stain = hexc("#7a1a1a")
    stain_dark = hexc("#4a0a0a")
    hoof = hexc("#2a1e16")
    antler = hexc("#c8b088")
    antler_dark = hexc("#8a7450")
    nose = hexc("#ff2020")
    nose_hi = hexc("#ff8080")
    light_y = hexc("#ffe040")
    light_g = hexc("#40ff60")
    light_r = hexc("#ff4040")
    sleigh = hexc("#a01818")
    sleigh_hi = hexc("#c02a2a")
    sleigh_dark = hexc("#6a0a0a")
    gold = hexc("#d4a640")
    gold_dark = hexc("#8a6a20")
    sack = hexc("#8a7a5a")
    sack_hi = hexc("#a89878")
    sack_dark = hexc("#5e5238")

    # 몸통 (u0 v0, 6x6x12)
    b = box(img, 0, 0, 6, 6, 12, fur, 8, top=fur_hi)
    bx, by = b["front"]
    # 피 얼룩
    for (x, y) in [(1, 1), (2, 1), (1, 2), (4, 3), (5, 3), (4, 4), (0, 4), (3, 5)]:
        px(img, bx + x, by + y, stain)
    for (x, y) in [(2, 2), (5, 4), (0, 5)]:
        px(img, bx + x, by + y, stain_dark)
    for x in range(6):
        px(img, bx + x, by + 5, fur_dark)
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for y in range(6):
            for x in range(12):
                if (x * 3 + y * 5) % 17 == 0:
                    px(img, sx + x, sy + y, stain)
                elif (x + y) % 9 == 0:
                    px(img, sx + x, sy + y, fur_dark)
        for x in range(12):
            px(img, sx + x, sy + 5, fur_dark)
    tx, ty = b["top"]
    for x in range(6):
        for y in range(12):
            if (x * 5 + y * 3) % 13 == 0:
                px(img, tx + x, ty + y, stain)
            elif (x + y) % 7 == 0:
                px(img, tx + x, ty + y, fur_dark)

    # 목 (u0 v18, 4x8x4)
    n = box(img, 0, 18, 4, 8, 4, fur, 8, top=fur_hi)
    nx, ny = n["front"]
    for y in range(8):
        px(img, nx + 1, ny + y, fur_dark if y % 3 == 0 else fur)
    for (x, y) in [(0, 2), (3, 5)]:
        px(img, nx + x, ny + y, stain)

    # 머리 (u16 v18, 4x4x7)
    h = box(img, 16, 18, 4, 4, 7, fur, 8, top=fur_hi)
    hx, hy = h["front"]
    # 눈: 작고 어두운 점
    px(img, hx + 1, hy + 1, hexc("#141010"))
    px(img, hx + 2, hy + 1, hexc("#141010"))
    # 주둥이 끝의 그늘
    for x in range(4):
        px(img, hx + x, hy + 3, fur_dark)
    # 피 묻은 턱
    px(img, hx + 1, hy + 2, stain)
    px(img, hx + 2, hy + 2, stain_dark)
    for side in ("right", "left", "back"):
        sx, sy = h[side]
        for y in range(4):
            for x in range(7):
                if (x * 3 + y) % 11 == 0:
                    px(img, sx + x, sy + y, stain)
                elif (x + y) % 5 == 0:
                    px(img, sx + x, sy + y, fur_dark)

    # 붉게 빛나는 코 (u38 v18, 2x2x2)
    ns = box(img, 38, 18, 2, 2, 2, nose, 4, top=nose_hi)
    nsx, nsy = ns["front"]
    px(img, nsx, nsy, nose_hi)
    px(img, nsx + 1, nsy + 1, hexc("#a00000"))

    # 귀 (u46 v18, 1x2x2)
    e = box(img, 46, 18, 1, 2, 2, fur, 8)
    ex, ey = e["front"]
    px(img, ex, ey, fur_dark)
    px(img, ex, ey + 1, stain)

    # 뿔 막대 (u52 v18, 1x6x1)
    a = box(img, 52, 18, 1, 6, 1, antler, 6, top=hexc("#e0c8a0"))
    ax, ay = a["front"]
    for y in range(6):
        px(img, ax, ay + y, antler_dark if y % 2 == 0 else antler)

    # 뿔에 매달린 전구 (u56 v18, 1x1x1) — 노랑 / 초록 / 빨강
    for u, col in ((56, light_y), (58, light_g), (60, light_r)):
        l = box(img, u, 18, 1, 1, 1, col, 0)
        lx, ly = l["front"]
        px(img, lx, ly, col)

    # 다리 (u0 v32, 2x8x2)
    l = box(img, 0, 32, 2, 8, 2, fur, 8, top=fur_hi)
    lx, ly = l["front"]
    for y in range(8):
        px(img, lx, ly + y, fur_dark if y % 3 == 0 else fur)
    for x in range(2):
        px(img, lx + x, ly + 7, hoof)
    for (x, y) in [(1, 3), (0, 5)]:
        px(img, lx + x, ly + y, stain)

    # 썰매 몸체 (u0 v44, 6x4x8)
    s = box(img, 0, 44, 6, 4, 8, sleigh, 6, top=sleigh_hi)
    sx, sy = s["front"]
    for x in range(6):
        px(img, sx + x, sy + 3, sleigh_dark)
    # 금장 장식
    for x in range(6):
        px(img, sx + x, sy, gold)
    for (x, y) in [(1, 1), (4, 1), (2, 2), (3, 2)]:
        px(img, sx + x, sy + y, gold_dark)
    for side in ("right", "left", "back"):
        ox, oy = s[side]
        for x in range(8):
            px(img, ox + x, oy, gold)
        for x in range(8):
            px(img, ox + x, oy + 3, sleigh_dark)

    # 썰매 날 (u28 v44, 1x1x9)
    r = box(img, 28, 44, 1, 1, 9, gold, 4, top=hexc("#e8c060"))
    rx, ry = r["front"]
    px(img, rx, ry, gold_dark)

    # 자루 (u40 v44, 5x5x5)
    sk = box(img, 40, 44, 5, 5, 5, sack, 6, top=sack_hi)
    skx, sky = sk["front"]
    for y in range(5):
        px(img, skx + 2, sky + y, sack_dark)
    for x in range(5):
        px(img, skx + x, sky + 4, sack_dark)
    # 자루를 묶은 끈
    for x in range(5):
        px(img, skx + x, sky, hexc("#3a2a1a"))
    for side in ("right", "left", "back"):
        ox, oy = sk[side]
        for y in range(5):
            if y % 2 == 0:
                px(img, ox + 2, oy + y, sack_dark)

    save(img, "textures/entity/rudolta.png")


CH = {"r": "#a01818", "h": "#c02a2a", "d": "#6a0a0a", "g": "#d4a640", "w": "#f4f0e8", "k": "#2a1e16"}
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
    "....rr..........",
    "...rr...........",
    "..dd............",
    "..d.............",
    "................",
], CH, "textures/item/christmas.png")
icon([
    "................",
    "...rr......rr...",
    "..rrrr....rrrr..",
    "..rrrrrrrrrrrr..",
    "...rrrrrrrrrr...",
    "....rrwwwwrr....",
    "....rrwwwwrr....",
    "....rrwwwwrr....",
    "....rrwwwwrr....",
    "....rrrrrrrr....",
    "....rrggggrr....",
    "....rrrrrrrr....",
    "....rrrrrrrr....",
    ".....dddddd.....",
    "................",
    "................",
], CH, "textures/item/christmas_suit.png")

armor(hexc("#a01818"), "#6a0a0a", "#d4a640", "textures/entity/equipment/humanoid/christmas.png")

for item, parent in [("christmas", "minecraft:item/handheld"), ("christmas_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/christmas.json", {"layers": {"humanoid": [{"texture": "lobcorp:christmas"}]}})


rudolta()
print("assets rudolta ok")

merge_lang({
    "entity.lobcorp.rudolta": "썰매 탄 루돌타",
    "item.lobcorp.christmas": "크리스마스",
    "item.lobcorp.christmas_suit": "크리스마스의 옷",
}, {
    "entity.lobcorp.rudolta": "Rudolta of the Sleigh",
    "item.lobcorp.christmas": "Christmas",
    "item.lobcorp.christmas_suit": "Christmas Garb",
})

merge_lang({
    "entity.lobcorp.scarecrow": "지혜를 찾는 허수아비",
    "item.lobcorp.harvest": "수확",
    "item.lobcorp.harvest_suit": "수확의 옷",
}, {
    "entity.lobcorp.scarecrow": "Scarecrow Searching for Wisdom",
    "item.lobcorp.harvest": "Harvest",
    "item.lobcorp.harvest_suit": "Harvest Garb",
})


# ═════════════ 30차: 죽은 나비들의 장례식 (T-01-68) ═════════════
def funeral():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    suit = hexc("#141418")
    suit_hi = hexc("#1e1e24")
    suit_dark = hexc("#0a0a0e")
    shirt = hexc("#f0f0f0")
    shirt_shade = hexc("#d0d0d0")
    tie = hexc("#2a2a30")
    wood = hexc("#3a2418")
    wood_hi = hexc("#4a3020")
    wood_dark = hexc("#24160e")
    cross = hexc("#e8e0d0")
    cross_dark = hexc("#b8b0a0")
    gun = hexc("#1a1a1e")
    gun_hi = hexc("#3a3a42")
    gun_steel = hexc("#8a9096")
    wing = hexc("#f4f4f4")
    wing_shade = hexc("#d8d8d8")
    wing_dark = hexc("#a8a8a8")

    # 몸통: 검은 상복 (u0 v0, 8x12x4)
    b = box(img, 0, 0, 8, 12, 4, suit, 4, top=suit_hi)
    bx, by = b["front"]
    # 흰 셔츠 깃과 단추
    for y in range(0, 8):
        px(img, bx + 3, by + y, shirt)
        px(img, bx + 4, by + y, shirt_shade)
    for y in (2, 4, 6):
        px(img, bx + 3, by + y, hexc("#8a8a90"))
    # 넥타이
    for y in range(1, 7):
        px(img, bx + 4, by + y, tie)
    px(img, bx + 3, by + 1, tie)
    px(img, bx + 5, by + 1, tie)
    # 옷깃
    for y in range(0, 3):
        px(img, bx + 2, by + y, suit_hi)
        px(img, bx + 5, by + y, suit_hi)
    # 밑단
    for x in range(8):
        px(img, bx + x, by + 11, suit_dark)
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for y in range(12):
            if y % 4 == 0:
                px(img, sx + 1, sy + y, suit_hi)
        for x in range(4):
            px(img, sx + x, sy + 11, suit_dark)

    # 관 머리 (u0 v16, 6x9x4)
    h = box(img, 0, 16, 6, 9, 4, wood, 6, top=wood_hi)
    hx, hy = h["front"]
    # 나뭇결
    for y in range(9):
        for x in range(6):
            if (x * 3 + y * 5) % 11 == 0:
                px(img, hx + x, hy + y, wood_dark)
    # 창백한 십자
    for y in range(2, 7):
        px(img, hx + 2, hy + y, cross)
        px(img, hx + 3, hy + y, cross_dark)
    for x in range(1, 5):
        px(img, hx + x, hy + 3, cross)
        px(img, hx + x, hy + 4, cross_dark)
    # 관 테두리
    for x in range(6):
        px(img, hx + x, hy, wood_hi)
        px(img, hx + x, hy + 8, wood_dark)
    for y in range(9):
        px(img, hx, hy + y, wood_hi)
        px(img, hx + 5, hy + y, wood_dark)
    for side in ("right", "left", "back"):
        sx, sy = h[side]
        for y in range(9):
            for x in range(4):
                if (x + y) % 5 == 0:
                    px(img, sx + x, sy + y, wood_dark)
        for x in range(4):
            px(img, sx + x, sy + 8, wood_dark)

    # 팔 (u32 v0, 2x12x2)
    for u in (32, 36):
        a = box(img, u, 0, 2, 12, 2, suit, 4)
        ax, ay = a["front"]
        for y in range(12):
            px(img, ax, ay + y, suit_dark if y % 4 == 0 else suit)
        for x in range(2):
            px(img, ax + x, ay + 11, suit_dark)

    # 총 (u40 v0, 1x2x4)
    for u in (40, 44):
        g = box(img, u, 0, 1, 2, 4, gun, 4, top=gun_hi)
        gx, gy = g["front"]
        px(img, gx, gy, gun_hi)
        px(img, gx, gy + 1, gun_steel)
        for side in ("right", "left", "back"):
            sx, sy = g[side]
            for y in range(2):
                px(img, sx, sy + y, gun_hi if y == 0 else gun)

    # 다리 (u0 v30, 2x12x2)
    for u in (0, 4):
        l = box(img, u, 30, 2, 12, 2, suit, 4)
        lx, ly = l["front"]
        for y in range(12):
            px(img, lx, ly + y, suit_dark if y % 5 == 0 else suit)
        for x in range(2):
            px(img, lx + x, ly + 11, hexc("#050508"))

    # 흰 나비 (u48 v0, 2x1x2)
    bf = box(img, 48, 0, 2, 1, 2, wing, 4, top=hexc("#ffffff"))
    bfx, bfy = bf["front"]
    px(img, bfx, bfy, wing_shade)
    px(img, bfx + 1, bfy, wing_shade)
    for side in ("right", "left", "back"):
        sx, sy = bf[side]
        px(img, sx, sy, wing_dark)
        px(img, sx + 1, sy, wing_shade)

    save(img, "textures/entity/funeral.png")


SL = {"k": "#141418", "h": "#1e1e24", "d": "#0a0a0e", "w": "#f0f0f0", "s": "#8a9096", "g": "#3a3a42"}
icon([
    "................",
    "..............kk",
    ".............kk.",
    "............kk..",
    "...........kk...",
    "..........kk....",
    ".........kk.....",
    "........kk......",
    ".......kk.......",
    "......kk........",
    ".....kk.........",
    "....ww..........",
    "...ww...........",
    "..ss............",
    "..s.............",
    "................",
], SL, "textures/item/solemn_lament.png")
icon([
    "................",
    "...kk......kk...",
    "..kkkk....kkkk..",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "....kkwwwwkk....",
    "....kkwwwwkk....",
    "....kkwwwwkk....",
    "....kkwwwwkk....",
    "....kkkkkkkk....",
    "....kksssskk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....dddddd.....",
    "................",
    "................",
], SL, "textures/item/solemn_lament_suit.png")

armor(hexc("#141418"), "#0a0a0e", "#f0f0f0", "textures/entity/equipment/humanoid/solemn_lament.png")

for item, parent in [("solemn_lament", "minecraft:item/handheld"),
                     ("solemn_lament_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/solemn_lament.json", {"layers": {"humanoid": [{"texture": "lobcorp:solemn_lament"}]}})


funeral()
print("assets funeral ok")

merge_lang({
    "entity.lobcorp.funeral": "죽은 나비들의 장례식",
    "item.lobcorp.solemn_lament": "엄숙한 애도",
    "item.lobcorp.solemn_lament_suit": "엄숙한 애도의 상복",
}, {
    "entity.lobcorp.funeral": "The Funeral of the Dead Butterflies",
    "item.lobcorp.solemn_lament": "Solemn Lament",
    "item.lobcorp.solemn_lament_suit": "Solemn Lament Suit",
})


# ═════════════ 31차: 샤덴프로이데 (O-05-76) ═════════════
def schadenfreude():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    iron = hexc("#3a3a40")
    iron_hi = hexc("#4a4a52")
    iron_dark = hexc("#26262c")
    chain = hexc("#8a8a90")
    chain_hi = hexc("#b0b0b8")
    chain_dark = hexc("#5a5a62")
    lock = hexc("#c8a040")
    lock_hi = hexc("#e8c060")
    lock_dark = hexc("#8a6a20")
    keyhole = hexc("#0a0a0c")
    pupil = hexc("#ffd040")
    pupil_hi = hexc("#fff0a0")
    blade = hexc("#c8c8d0")
    blade_hi = hexc("#eef0f4")
    blade_dark = hexc("#7a7a84")
    tooth = hexc("#9a9aa4")

    # 몸통 (u0 v0, 10x12x6)
    b = box(img, 0, 0, 10, 12, 6, iron, 6, top=iron_hi)
    bx, by = b["front"]
    # 철판 이음매
    for x in range(10):
        px(img, bx + x, by + 3, iron_dark)
        px(img, bx + x, by + 8, iron_dark)
    for y in range(12):
        px(img, bx, by + y, iron_dark)
        px(img, bx + 9, by + y, iron_dark)
    # 리벳
    for (x, y) in [(1, 1), (8, 1), (1, 6), (8, 6), (1, 10), (8, 10)]:
        px(img, bx + x, by + y, iron_hi)
        px(img, bx + x, by + y + 1, iron_dark)
    # 사슬: 대각선으로 감긴 고리
    for i in range(10):
        y = 1 + i
        if y >= 12:
            break
        px(img, bx + i, by + y, chain if i % 2 == 0 else chain_dark)
        if i % 2 == 0:
            px(img, bx + i, by + y + 1, chain_hi)
    # 매달린 자물쇠 두 개
    for (x, y) in [(2, 5), (6, 9)]:
        rect(img, bx + x, by + y, 2, 2, lock, 4)
        px(img, bx + x, by + y, lock_hi)
        px(img, bx + x + 1, by + y + 1, lock_dark)
        px(img, bx + x, by + y - 1, chain_dark)
    # 옆면·뒷면·윗면에도 사슬과 자물쇠
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for y in range(12):
            for x in range(6):
                if (x * 3 + y * 5) % 11 == 0:
                    px(img, sx + x, sy + y, chain)
                elif (x + y) % 7 == 0:
                    px(img, sx + x, sy + y, iron_dark)
        for x in range(6):
            px(img, sx + x, sy + 11, iron_dark)
        rect(img, sx + 2, sy + 4, 2, 2, lock, 4)
    tx, ty = b["top"]
    for x in range(10):
        for y in range(6):
            if (x + y * 2) % 5 == 0:
                px(img, tx + x, ty + y, chain)
            elif (x * 3 + y) % 9 == 0:
                px(img, tx + x, ty + y, iron_dark)

    # 가슴의 열쇠 구멍 눈 판 (u32 v0, 4x6x1)
    c = box(img, 32, 0, 4, 6, 1, iron_hi, 4)
    cx, cy = c["front"]
    # 열쇠 구멍: 위는 둥근 구멍, 아래는 쐐기
    for (x, y) in [(1, 1), (2, 1), (1, 2), (2, 2)]:
        px(img, cx + x, cy + y, keyhole)
    for y in range(3, 6):
        px(img, cx + 1, cy + y, keyhole)
        px(img, cx + 2, cy + y, keyhole)
    px(img, cx + 1, cy + 5, keyhole)
    # 테두리
    for x in range(4):
        px(img, cx + x, cy, iron_dark)
        px(img, cx + x, cy + 5, iron_dark)
    for y in range(6):
        px(img, cx, cy + y, iron_dark)
        px(img, cx + 3, cy + y, iron_dark)

    # 눈동자 (u42 v0, 2x2x1) — 바라볼 때만 보인다
    p = box(img, 42, 0, 2, 2, 1, pupil, 4)
    px_, py_ = p["front"]
    px(img, px_, py_, pupil_hi)
    px(img, px_ + 1, py_ + 1, hexc("#c09a10"))

    # 사슬에 감긴 머리 (u0 v20, 5x4x5)
    h = box(img, 0, 20, 5, 4, 5, iron, 6, top=iron_hi)
    hx, hy = h["front"]
    # 사슬 띠
    for x in range(5):
        px(img, hx + x, hy + 1, chain if x % 2 == 0 else chain_dark)
        px(img, hx + x, hy + 2, chain_dark if x % 2 == 0 else chain)
    # 틈으로 보이는 어두운 눈
    px(img, hx + 1, hy + 2, hexc("#0a0a0c"))
    px(img, hx + 3, hy + 2, hexc("#0a0a0c"))
    for x in range(5):
        px(img, hx + x, hy + 3, iron_dark)
    for side in ("right", "left", "back"):
        sx, sy = h[side]
        for y in range(4):
            for x in range(5):
                if (x + y) % 3 == 0:
                    px(img, sx + x, sy + y, chain)
                elif (x * 2 + y) % 5 == 0:
                    px(img, sx + x, sy + y, iron_dark)

    # 팔 (u20 v20, 3x11x3)
    a = box(img, 20, 20, 3, 11, 3, iron, 6, top=iron_hi)
    ax, ay = a["front"]
    for y in range(11):
        px(img, ax + 1, ay + y, iron_dark if y % 4 == 0 else iron)
    for (x, y) in [(0, 2), (2, 6)]:
        px(img, ax + x, ay + y, chain)
    for x in range(3):
        px(img, ax + x, ay + 10, iron_dark)

    # 둥근 톱날 (u32 v20, 6x6x1)
    bl = box(img, 32, 20, 6, 6, 1, blade, 4, top=blade_hi)
    blx, bly = bl["front"]
    # 톱니: 테두리를 따라
    for x in range(6):
        px(img, blx + x, bly, tooth)
        px(img, blx + x, bly + 5, tooth)
    for y in range(6):
        px(img, blx, bly + y, tooth)
        px(img, blx + 5, bly + y, tooth)
    # 날: 중심에서 뻗은 선
    for i in range(6):
        px(img, blx + i, bly + i, blade_hi)
        px(img, blx + 5 - i, bly + i, blade_dark)
    for (x, y) in [(2, 2), (3, 3)]:
        px(img, blx + x, bly + y, blade_dark)
    # 축
    px(img, blx + 2, bly + 2, hexc("#3a3a40"))
    px(img, blx + 3, bly + 3, hexc("#3a3a40"))

    # 다리 (u0 v32, 3x10x3 / u12 v32, 3x10x3)
    for u in (0, 12):
        l = box(img, u, 32, 3, 10, 3, iron, 6, top=iron_hi)
        lx, ly = l["front"]
        for y in range(10):
            px(img, lx + 1, ly + y, iron_dark if y % 5 == 0 else iron)
        for (x, y) in [(0, 3), (2, 7)]:
            px(img, lx + x, ly + y, chain)
        for x in range(3):
            px(img, lx + x, ly + 9, hexc("#1a1a1e"))

    save(img, "textures/entity/schadenfreude.png")


GZ = {"k": "#3a3a40", "h": "#4a4a52", "d": "#26262c", "c": "#8a8a90", "y": "#ffd040", "s": "#c8c8d0"}
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
    "....kk..........",
    "...kk...........",
    "..dd............",
    "..d.............",
    "................",
], GZ, "textures/item/gaze.png")
icon([
    "................",
    "...kk......kk...",
    "..kkkk....kkkk..",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "....kkcccckk....",
    "....kkcccckk....",
    "....kkcccckk....",
    "....kkcccckk....",
    "....kkkkkkkk....",
    "....kkyyyykk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....dddddd.....",
    "................",
    "................",
], GZ, "textures/item/gaze_suit.png")

armor(hexc("#3a3a40"), "#26262c", "#8a8a90", "textures/entity/equipment/humanoid/gaze.png")

for item, parent in [("gaze", "minecraft:item/handheld"), ("gaze_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/gaze.json", {"layers": {"humanoid": [{"texture": "lobcorp:gaze"}]}})


schadenfreude()
print("assets schadenfreude ok")

merge_lang({
    "entity.lobcorp.schadenfreude": "샤덴프로이데",
    "item.lobcorp.gaze": "응시",
    "item.lobcorp.gaze_suit": "응시의 옷",
}, {
    "entity.lobcorp.schadenfreude": "Schadenfreude",
    "item.lobcorp.gaze": "Gaze",
    "item.lobcorp.gaze_suit": "Gaze Garb",
})


# ═════════════ 32차: 큰 새 (O-02-40) ═════════════
def big_bird():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    black = hexc("#101014")
    black_hi = hexc("#1a1a20")
    black_dark = hexc("#08080a")
    eye = hexc("#ffd040")
    eye_dim = hexc("#c09a20")
    brass = hexc("#b08840")
    brass_hi = hexc("#d0a860")
    brass_dark = hexc("#7a5a20")
    core = hexc("#ffcc55")
    core_hi = hexc("#fff0b0")
    beak = hexc("#2a2a30")
    beak_hi = hexc("#4a4a52")

    # 몸통 (u0 v0, 16x16x14)
    b = box(img, 0, 0, 16, 16, 14, black, 4, top=black_hi)
    bx, by = b["front"]
    # 온몸의 노란 눈: 앞면
    for (x, y) in [(1, 1), (4, 2), (7, 1), (10, 3), (13, 1),
                   (2, 4), (5, 5), (8, 4), (11, 5), (14, 4),
                   (0, 7), (3, 8), (6, 7), (9, 8), (12, 7), (15, 8),
                   (1, 10), (4, 11), (7, 10), (10, 11), (13, 10),
                   (2, 13), (5, 14), (8, 13), (11, 14), (14, 13)]:
        px(img, bx + x, by + y, eye)
    # 눈의 그늘
    for (x, y) in [(1, 2), (7, 2), (13, 2), (3, 9), (9, 9), (15, 9), (5, 15), (11, 15)]:
        px(img, bx + x, by + y, eye_dim)
    # 옆면·뒷면·윗면에도 눈
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for y in range(16):
            for x in range(14):
                if (x * 3 + y * 5) % 7 == 0:
                    px(img, sx + x, sy + y, eye)
                elif (x * 5 + y * 3) % 11 == 0:
                    px(img, sx + x, sy + y, eye_dim)
                elif (x + y) % 9 == 0:
                    px(img, sx + x, sy + y, black_hi)
        for x in range(14):
            px(img, sx + x, sy + 15, black_dark)
    tx, ty = b["top"]
    for x in range(16):
        for y in range(14):
            if (x * 7 + y * 3) % 13 == 0:
                px(img, tx + x, ty + y, eye)
            elif (x + y * 2) % 17 == 0:
                px(img, tx + x, ty + y, black_hi)

    # 다리 (u0 v30, 3x6x3)
    l = box(img, 0, 30, 3, 6, 3, black, 4, top=black_hi)
    lx, ly = l["front"]
    for y in range(6):
        px(img, lx + 1, ly + y, black_dark if y % 2 == 0 else black)
    for x in range(3):
        px(img, lx + x, ly + 5, hexc("#2a2a30"))
    for side in ("right", "left", "back"):
        sx, sy = l[side]
        for y in range(6):
            if y % 3 == 0:
                px(img, sx + 1, sy + y, black_dark)

    # 목 (u0 v40, 4x14x4)
    n = box(img, 0, 40, 4, 14, 4, black, 4, top=black_hi)
    nx, ny = n["front"]
    for y in range(14):
        if y % 3 == 0:
            px(img, nx + 1, ny + y, eye)
        elif y % 5 == 0:
            px(img, nx + 2, ny + y, eye_dim)
    for side in ("right", "left", "back"):
        sx, sy = n[side]
        for y in range(14):
            if y % 4 == 0:
                px(img, sx + 1, sy + y, eye)
            elif y % 7 == 0:
                px(img, sx + 2, sy + y, black_dark)

    # 머리 (u16 v40, 6x6x8)
    h = box(img, 16, 40, 6, 6, 8, black, 4, top=black_hi)
    hx, hy = h["front"]
    # 머리 앞면의 큰 눈 두 개
    for (x, y) in [(1, 1), (2, 1), (1, 2), (2, 2)]:
        px(img, hx + x, hy + y, eye)
    for (x, y) in [(3, 1), (4, 1), (3, 2), (4, 2)]:
        px(img, hx + x, hy + y, eye)
    px(img, hx + 1, hy + 2, eye_dim)
    px(img, hx + 4, hy + 2, eye_dim)
    for (x, y) in [(0, 4), (5, 4), (2, 5), (3, 5)]:
        px(img, hx + x, hy + y, eye_dim)
    for side in ("right", "left", "back"):
        sx, sy = h[side]
        for y in range(6):
            for x in range(8):
                if (x * 3 + y * 5) % 9 == 0:
                    px(img, sx + x, sy + y, eye)
                elif (x + y) % 7 == 0:
                    px(img, sx + x, sy + y, black_hi)

    # 새장 같은 부리: 위판 (u44 v40, 4x1x6)
    bt = box(img, 44, 40, 4, 1, 6, beak, 4, top=beak_hi)
    btx, bty = bt["front"]
    for x in range(4):
        px(img, btx + x, bty, beak_hi)
    # 창살
    for x in (1, 2):
        px(img, btx + x, bty, hexc("#8a8a92"))
    # 아래판 (u44 v48, 4x1x6)
    bb = box(img, 44, 48, 4, 1, 6, beak, 4, top=beak_hi)
    bbx, bby = bb["front"]
    for x in range(4):
        px(img, bbx + x, bby, hexc("#8a8a92") if x % 2 == 0 else beak_hi)

    # 등불 (u48 v0, 3x4x3)
    lp = box(img, 48, 0, 3, 4, 3, brass, 6, top=brass_hi)
    lpx, lpy = lp["front"]
    # 유리창: 안쪽에서 새어 나오는 빛
    for y in range(1, 3):
        for x in range(1, 2):
            px(img, lpx + x, lpy + y, core)
    px(img, lpx + 1, lpy + 1, core_hi)
    px(img, lpx + 1, lpy + 2, hexc("#e0a030"))
    for x in range(3):
        px(img, lpx + x, lpy, brass_hi)
        px(img, lpx + x, lpy + 3, brass_dark)
    for side in ("right", "left", "back"):
        sx, sy = lp[side]
        for y in range(4):
            px(img, sx + 1, sy + y, core if 0 < y < 3 else brass_dark)
        px(img, sx + 1, sy + 1, core_hi)
    # 손잡이 고리
    for x in range(3):
        px(img, lpx + x, lpy - 1, brass_dark)

    save(img, "textures/entity/big_bird.png")


big_bird()
print("assets bigbird ok")

LP = {"k": "#101014", "h": "#1a1a20", "d": "#08080a", "y": "#ffd040", "b": "#b08840", "c": "#ffcc55"}
icon([
    "................",
    "..............kk",
    ".............kk.",
    "............kk..",
    "...........kk...",
    "..........kk....",
    ".........kk.....",
    "........kk......",
    ".......kk.......",
    "......kk........",
    ".....kk.........",
    "....bb..........",
    "...bb...........",
    "..cc............",
    "..c.............",
    "................",
], LP, "textures/item/lamp.png")
icon([
    "................",
    "...kk......kk...",
    "..kkkk....kkkk..",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "....kkyyyykk....",
    "....kkyyyykk....",
    "....kkyyyykk....",
    "....kkyyyykk....",
    "....kkkkkkkk....",
    "....kkbbbbkk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....dddddd.....",
    "................",
    "................",
], LP, "textures/item/lamp_suit.png")

armor(hexc("#101014"), "#08080a", "#ffd040", "textures/entity/equipment/humanoid/lamp.png")

for item, parent in [("lamp", "minecraft:item/handheld"), ("lamp_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/lamp.json", {"layers": {"humanoid": [{"texture": "lobcorp:lamp"}]}})

merge_lang({
    "entity.lobcorp.big_bird": "큰 새",
    "item.lobcorp.lamp": "램프",
    "item.lobcorp.lamp_suit": "램프의 옷",
}, {
    "entity.lobcorp.big_bird": "Big Bird",
    "item.lobcorp.lamp": "Lamp",
    "item.lobcorp.lamp_suit": "Lamp Garb",
})


# ═════════════ 33차: 심판 새 (O-02-62) ═════════════
def judgement_bird():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    robe = hexc("#2a2a30")
    robe_hi = hexc("#3a3a42")
    robe_dark = hexc("#141418")
    leg = hexc("#1a1a1e")
    leg_hi = hexc("#2e2e34")
    sack = hexc("#d8d0b8")
    sack_hi = hexc("#e8e0cc")
    sack_dark = hexc("#b0a890")
    wrap = hexc("#8a8270")
    wrap_dark = hexc("#5e5848")
    beak = hexc("#c8b088")
    beak_dark = hexc("#8a7450")
    gold = hexc("#d8b040")
    gold_hi = hexc("#f0d070")
    gold_dark = hexc("#8a6a20")

    # 다리 (u0 v0, 1x18x1 / u4 v0, 1x18x1)
    for u in (0, 4):
        l = box(img, u, 0, 1, 18, 1, leg, 4)
        lx, ly = l["front"]
        for y in range(18):
            px(img, lx, ly + y, leg_hi if y % 4 == 0 else leg)
        px(img, lx, ly + 17, hexc("#3a3a40"))

    # 사제복 (u0 v20, 6x14x4)
    b = box(img, 0, 20, 6, 14, 4, robe, 4, top=robe_hi)
    bx, by = b["front"]
    # 옷깃과 여밈
    for y in range(14):
        px(img, bx + 2, by + y, robe_dark)
        px(img, bx + 3, by + y, robe_dark)
    for y in (2, 5, 8, 11):
        px(img, bx + 2, by + y, gold_dark)
    for x in range(6):
        px(img, bx + x, by + 13, robe_dark)
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for y in range(14):
            if y % 4 == 0:
                px(img, sx + 1, sy + y, robe_hi)
        for x in range(4):
            px(img, sx + x, sy + 13, robe_dark)

    # 팔 (u20 v20, 1x14x1 / u24 v20, 1x14x1)
    for u in (20, 24):
        a = box(img, u, 20, 1, 14, 1, robe, 4)
        ax, ay = a["front"]
        for y in range(14):
            px(img, ax, ay + y, robe_dark if y % 5 == 0 else robe)
        px(img, ax, ay + 13, hexc("#3a3a40"))

    # 목 (u28 v20, 2x6x2)
    n = box(img, 28, 20, 2, 6, 2, robe, 4, top=robe_hi)
    nx, ny = n["front"]
    for y in range(6):
        px(img, nx, ny + y, robe_dark)

    # 자루 머리 (u0 v40, 5x6x5)
    h = box(img, 0, 40, 5, 6, 5, sack, 4, top=sack_hi)
    hx, hy = h["front"]
    # 붕대 감은 자국: 비스듬한 띠
    for y in range(6):
        for x in range(5):
            if (x + y) % 3 == 0:
                px(img, hx + x, hy + y, wrap)
            elif (x + y) % 7 == 0:
                px(img, hx + x, hy + y, wrap_dark)
    for x in range(5):
        px(img, hx + x, hy + 5, sack_dark)
    for side in ("right", "left", "back"):
        sx, sy = h[side]
        for y in range(6):
            for x in range(5):
                if (x * 2 + y) % 4 == 0:
                    px(img, sx + x, sy + y, wrap)
                elif (x + y) % 6 == 0:
                    px(img, sx + x, sy + y, wrap_dark)
        for x in range(5):
            px(img, sx + x, sy + 5, sack_dark)
    tx, ty = h["top"]
    for x in range(5):
        for y in range(5):
            if (x + y) % 3 == 0:
                px(img, tx + x, ty + y, wrap)

    # 긴 부리 (u20 v40, 1x1x6)
    bk = box(img, 20, 40, 1, 1, 6, beak, 4, top=beak_dark)
    bkx, bky = bk["front"]
    px(img, bkx, bky, beak_dark)
    for side in ("right", "left", "back"):
        sx, sy = bk[side]
        for x in range(6):
            px(img, sx + x, sy, beak_dark if x % 2 == 0 else beak)

    # 저울대 (u36 v40, 8x1x1)
    sc = box(img, 36, 40, 8, 1, 1, gold, 4, top=gold_hi)
    scx, scy = sc["front"]
    for x in range(8):
        px(img, scx + x, scy, gold_hi if x % 2 == 0 else gold_dark)
    # 저울 접시 (u36 v44, 3x1x3 / u48 v44, 3x1x3)
    for u in (36, 48):
        p = box(img, u, 44, 3, 1, 3, gold, 4, top=gold_hi)
        px_, py_ = p["front"]
        for x in range(3):
            px(img, px_ + x, py_, gold_dark)
        ptx, pty = p["top"]
        for x in range(3):
            for y in range(3):
                if (x + y) % 2 == 0:
                    px(img, ptx + x, pty + y, gold_hi)

    save(img, "textures/entity/judgement_bird.png")


JT = {"g": "#d8b040", "h": "#f0d070", "d": "#8a6a20", "k": "#2a2a30", "s": "#d8d0b8", "w": "#8a8270"}
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
], JT, "textures/item/justitia.png")
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
    "....kkhhhhkk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....dddddd.....",
    "................",
    "................",
], JT, "textures/item/justitia_suit.png")

armor(hexc("#2a2a30"), "#141418", "#d8b040", "textures/entity/equipment/humanoid/justitia.png")

for item, parent in [("justitia", "minecraft:item/handheld"), ("justitia_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/justitia.json", {"layers": {"humanoid": [{"texture": "lobcorp:justitia"}]}})


judgement_bird()
print("assets judgement ok")


# ═════════════ 34차: 증오의 여왕 (O-01-04) ═════════════
def queen_of_hatred():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    dress = hexc("#ff8ac8")
    dress_hi = hexc("#ffb0dc")
    dress_dark = hexc("#d86aa4")
    white = hexc("#fbf6fa")
    white_shade = hexc("#e0d8e4")
    skin = hexc("#fbe4d8")
    skin_shade = hexc("#e0c4b8")
    hair = hexc("#ffe080")
    hair_dark = hexc("#d8b850")
    bow = hexc("#ff5aa8")
    bow_dark = hexc("#c03a78")
    star = hexc("#ffe040")
    star_hi = hexc("#fff6b0")
    stick = hexc("#f0e0f0")
    eye = hexc("#3a2a4a")

    # 다리: 흰 부츠 (u0 v0, 2x8x2)
    l = box(img, 0, 0, 2, 8, 2, white, 4)
    lx, ly = l["front"]
    for y in range(8):
        px(img, lx, ly + y, white_shade if y % 3 == 0 else white)
    for x in range(2):
        px(img, lx + x, ly + 7, hexc("#c8b8cc"))

    # 퍼프 드레스 (u0 v10, 8x6x6)
    d = box(img, 0, 10, 8, 6, 6, dress, 6, top=dress_hi)
    dx, dy = d["front"]
    # 흰 레이스 단
    for x in range(8):
        px(img, dx + x, dy + 5, white)
    for y in range(6):
        px(img, dx + 3, dy + y, dress_dark)
        px(img, dx + 4, dy + y, dress_dark)
    for (x, y) in [(1, 1), (6, 2), (2, 3), (5, 4)]:
        px(img, dx + x, dy + y, dress_hi)
    for side in ("right", "left", "back"):
        sx, sy = d[side]
        for x in range(6):
            px(img, sx + x, sy + 5, white)
        for y in range(6):
            if y % 2 == 0:
                px(img, sx + 2, sy + y, dress_dark)

    # 몸통 (u28 v10, 5x5x3)
    b = box(img, 28, 10, 5, 5, 3, dress, 6)
    bx, by = b["front"]
    for x in range(5):
        px(img, bx + x, by, white)
    px(img, bx + 2, by + 2, bow)
    px(img, bx + 2, by + 3, bow_dark)

    # 팔 (u0 v22, 2x6x2)
    a = box(img, 0, 22, 2, 6, 2, skin, 4)
    ax, ay = a["front"]
    for y in range(6):
        px(img, ax, ay + y, skin_shade if y % 3 == 0 else skin)
    for x in range(2):
        px(img, ax + x, ay + 5, skin_shade)

    # 머리 (u0 v32, 6x6x6)
    h = box(img, 0, 32, 6, 6, 6, skin, 4, top=hair)
    hx, hy = h["front"]
    # 반짝이는 눈
    px(img, hx + 1, hy + 2, eye)
    px(img, hx + 4, hy + 2, eye)
    px(img, hx + 1, hy + 1, hair_dark)
    px(img, hx + 4, hy + 1, hair_dark)
    # 웃는 입
    for x in (2, 3):
        px(img, hx + x, hy + 4, hexc("#c86a7a"))
    px(img, hx + 1, hy + 3, hexc("#c86a7a"))
    px(img, hx + 4, hy + 3, hexc("#c86a7a"))
    # 볼터치
    px(img, hx, hy + 3, hexc("#f0a0a8"))
    px(img, hx + 5, hy + 3, hexc("#f0a8b0"))
    # 앞머리
    for x in range(6):
        px(img, hx + x, hy, hair)
    for x in (0, 2, 5):
        px(img, hx + x, hy + 1, hair_dark)
    for side in ("right", "left"):
        sx, sy = h[side]
        for y in range(6):
            px(img, sx + 2, sy + y, hair if y < 4 else hair_dark)

    # 트윈테일 (u24 v32, 2x8x2)
    t = box(img, 24, 32, 2, 8, 2, hair, 6, top=hexc("#fff0b0"))
    tx, ty = t["front"]
    for y in range(8):
        px(img, tx, ty + y, hair_dark if y % 3 == 0 else hair)
        px(img, tx + 1, ty + y, hair if y % 4 == 0 else hair_dark)
    for x in range(2):
        px(img, tx + x, ty + 7, hair_dark)

    # 큰 리본 (u32 v32, 8x3x2)
    bw = box(img, 32, 32, 8, 3, 2, bow, 6, top=hexc("#ff9ac8"))
    bwx, bwy = bw["front"]
    for x in range(8):
        px(img, bwx + x, bwy + 1, bow_dark if x % 2 == 0 else bow)
    px(img, bwx + 3, bwy, bow_dark)
    px(img, bwx + 4, bwy, bow_dark)
    for x in range(8):
        px(img, bwx + x, bwy + 2, bow_dark)

    # 지팡이 자루 (u44 v0, 1x6x1)
    st = box(img, 44, 0, 1, 6, 1, stick, 4)
    stx, sty = st["front"]
    for y in range(6):
        px(img, stx, sty + y, hexc("#d8c8dc") if y % 2 == 0 else stick)

    # 별 (u48 v0, 3x3x1)
    sr = box(img, 48, 0, 3, 3, 1, star, 4, top=star_hi)
    srx, sry = sr["front"]
    px(img, srx + 1, sry, star_hi)
    px(img, srx, sry + 1, star_hi)
    px(img, srx + 2, sry + 1, star_hi)
    px(img, srx + 1, sry + 2, star_hi)
    px(img, srx + 1, sry + 1, hexc("#e0a020"))

    # ── 뱀 ──
    snake = hexc("#1a0a20")
    snake_hi = hexc("#2a1030")
    snake_dark = hexc("#0a040e")
    crack = hexc("#c030c0")
    crack_hi = hexc("#ff70ff")
    eye_m = hexc("#ff30d0")
    eye_hi = hexc("#ffb0f0")
    fang = hexc("#f4f0f4")
    maw = hexc("#3a0a2a")

    # 몸통 마디 (u0 v44, 6x6x6)
    sg = box(img, 0, 44, 6, 6, 6, snake, 4, top=snake_hi)
    sgx, sgy = sg["front"]
    # 마젠타 균열
    for (x, y) in [(1, 1), (2, 1), (2, 2), (4, 3), (5, 3), (5, 4), (0, 4), (3, 5)]:
        px(img, sgx + x, sgy + y, crack)
    for (x, y) in [(2, 2), (5, 4)]:
        px(img, sgx + x, sgy + y, crack_hi)
    for x in range(6):
        px(img, sgx + x, sgy + 5, snake_dark)
    for side in ("right", "left", "back"):
        sx, sy = sg[side]
        for y in range(6):
            for x in range(6):
                if (x * 3 + y * 5) % 7 == 0:
                    px(img, sx + x, sy + y, crack)
                elif (x + y) % 5 == 0:
                    px(img, sx + x, sy + y, snake_hi)
        for x in range(6):
            px(img, sx + x, sy + 5, snake_dark)
    tx, ty = sg["top"]
    for x in range(6):
        for y in range(6):
            if (x * 5 + y * 3) % 9 == 0:
                px(img, tx + x, ty + y, crack)
            elif (x + y) % 4 == 0:
                px(img, tx + x, ty + y, snake_hi)

    # 뱀 머리 (u24 v44, 8x7x8)
    sh = box(img, 24, 44, 8, 7, 8, snake, 4, top=snake_hi)
    shx, shy = sh["front"]
    # 빛나는 마젠타 눈
    for (x, y) in [(1, 2), (2, 2), (1, 3), (2, 3)]:
        px(img, shx + x, shy + y, eye_m)
    for (x, y) in [(5, 2), (6, 2), (5, 3), (6, 3)]:
        px(img, shx + x, shy + y, eye_m)
    px(img, shx + 1, shy + 2, eye_hi)
    px(img, shx + 6, shy + 2, eye_hi)
    # 머리 균열
    for (x, y) in [(3, 1), (4, 1), (3, 5), (4, 5), (0, 4), (7, 4)]:
        px(img, shx + x, shy + y, crack)
    for x in range(8):
        px(img, shx + x, shy + 6, snake_dark)
    for side in ("right", "left", "back"):
        sx, sy = sh[side]
        for y in range(7):
            for x in range(8):
                if (x * 3 + y) % 8 == 0:
                    px(img, sx + x, sy + y, crack)
                elif (x + y) % 6 == 0:
                    px(img, sx + x, sy + y, snake_hi)
        for x in range(8):
            px(img, sx + x, sy + 6, snake_dark)
    tx, ty = sh["top"]
    for x in range(8):
        for y in range(8):
            if (x + y * 2) % 7 == 0:
                px(img, tx + x, ty + y, crack)
            elif (x * 3 + y) % 11 == 0:
                px(img, tx + x, ty + y, snake_hi)

    # 아가리 (u24 v52, 6x2x8) — 64x64 텍스처의 아래 경계에 걸치지 않도록 v를 올린다
    j = box(img, 24, 52, 6, 2, 8, maw, 4)
    jx, jy = j["front"]
    for x in range(6):
        px(img, jx + x, jy, fang if x % 2 == 0 else maw)
        px(img, jx + x, jy + 1, hexc("#1a0410"))
    for side in ("right", "left", "back"):
        sx, sy = j[side]
        for x in range(8):
            px(img, sx + x, sy, fang if x % 3 == 0 else maw)
            px(img, sx + x, sy + 1, hexc("#1a0410"))

    save(img, "textures/entity/queen_of_hatred.png")


queen_of_hatred()
print("assets queen ok")

LH = {"p": "#ff8ac8", "h": "#ffb0dc", "d": "#d86aa4", "w": "#fbf6fa", "y": "#ffe040", "s": "#f0e0f0"}
icon([
    "................",
    "..............yy",
    ".............yy.",
    "............yy..",
    "...........yy...",
    "..........yy....",
    ".........yy.....",
    "........ss......",
    ".......ss.......",
    "......ss........",
    ".....ss.........",
    "....ss..........",
    "...dd...........",
    "..dd............",
    "..d.............",
    "................",
], LH, "textures/item/love_and_hate.png")
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
    "....ppyyyypp....",
    "....pppppppp....",
    "....pppppppp....",
    ".....dddddd.....",
    "................",
    "................",
], LH, "textures/item/love_and_hate_suit.png")

armor(hexc("#ff8ac8"), "#d86aa4", "#fbf6fa", "textures/entity/equipment/humanoid/love_and_hate.png")

for item, parent in [("love_and_hate", "minecraft:item/handheld"),
                     ("love_and_hate_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/love_and_hate.json", {"layers": {"humanoid": [{"texture": "lobcorp:love_and_hate"}]}})

merge_lang({
    "entity.lobcorp.queen_of_hatred": "증오의 여왕",
    "item.lobcorp.love_and_hate": "사랑과 증오의 이름으로",
    "item.lobcorp.love_and_hate_suit": "사랑과 증오의 드레스",
}, {
    "entity.lobcorp.queen_of_hatred": "Queen of Hatred",
    "item.lobcorp.love_and_hate": "In the Name of Love and Hate",
    "item.lobcorp.love_and_hate_suit": "Love and Hate Dress",
})

merge_lang({
    "entity.lobcorp.judgement_bird": "심판 새",
    "item.lobcorp.justitia": "유스티티아",
    "item.lobcorp.justitia_suit": "유스티티아의 옷",
}, {
    "entity.lobcorp.judgement_bird": "Judgement Bird",
    "item.lobcorp.justitia": "Justitia",
    "item.lobcorp.justitia_suit": "Justitia Garb",
})


# ═════════════ 35차: 절망의 기사 (O-01-73) ═════════════
def knight_of_despair():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    armor_dark = hexc("#2a3050")
    armor_dark_hi = hexc("#3a4268")
    armor_dark_dark = hexc("#181c34")
    silver = hexc("#8aa0c8")
    silver_hi = hexc("#b0c4e4")
    silver_dark = hexc("#5a6e94")
    skin = hexc("#f0ece8")
    skin_shade = hexc("#d4ccc8")
    hair_blue = hexc("#4a70d0")
    hair_mid = hexc("#2e3e88")
    hair_black = hexc("#0e0e1a")
    tear = hexc("#9ad0ff")
    cape = hexc("#1a1a2a")
    cape_hi = hexc("#2a2a40")
    cape_dark = hexc("#0a0a12")
    blade = hexc("#d0d8ff")
    blade_hi = hexc("#f0f4ff")
    blade_dark = hexc("#8a94c0")
    hilt = hexc("#c8a040")

    # 다리: 어두운 갑옷 (u0 v0, 2x10x2 / u8 v0, 2x10x2)
    for u in (0, 8):
        l = box(img, u, 0, 2, 10, 2, armor_dark, 6, top=armor_dark_hi)
        lx, ly = l["front"]
        for y in range(10):
            px(img, lx, ly + y, armor_dark_dark if y % 4 == 0 else armor_dark)
            px(img, lx + 1, ly + y, armor_dark_hi if y % 5 == 0 else armor_dark)
        for x in range(2):
            px(img, lx + x, ly + 9, hexc("#0e1020"))

    # 치마 (u0 v12, 7x5x5)
    s = box(img, 0, 12, 7, 5, 5, armor_dark, 6, top=armor_dark_hi)
    sx, sy = s["front"]
    for y in range(5):
        px(img, sx + 3, sy + y, armor_dark_dark)
    for x in range(7):
        px(img, sx + x, sy + 4, armor_dark_dark)
    for side in ("right", "left", "back"):
        ox, oy = s[side]
        for y in range(5):
            if y % 2 == 0:
                px(img, ox + 2, oy + y, armor_dark_hi)
        for x in range(5):
            px(img, ox + x, oy + 4, armor_dark_dark)

    # 몸통: 은빛 푸른 갑옷 (u24 v0, 6x7x3)
    b = box(img, 24, 0, 6, 7, 3, silver, 6, top=silver_hi)
    bx, by = b["front"]
    # 가슴의 푸른 보석
    px(img, bx + 2, by + 2, hair_blue)
    px(img, bx + 3, by + 2, hair_blue)
    px(img, bx + 2, by + 3, hair_mid)
    px(img, bx + 3, by + 3, hair_mid)
    # 갑옷 이음매
    for x in range(6):
        px(img, bx + x, by + 5, silver_dark)
    for y in range(7):
        px(img, bx, by + y, silver_dark)
        px(img, bx + 5, by + y, silver_dark)
    for side in ("right", "left", "back"):
        ox, oy = b[side]
        for y in range(7):
            if y % 3 == 0:
                px(img, ox + 1, oy + y, silver_dark)
        for x in range(3):
            px(img, ox + x, oy + 6, silver_dark)

    # 팔 (u42 v0, 2x8x2 / u50 v0, 2x8x2)
    for u in (42, 50):
        a = box(img, u, 0, 2, 8, 2, silver, 6, top=silver_hi)
        ax, ay = a["front"]
        for y in range(8):
            px(img, ax, ay + y, silver_dark if y % 3 == 0 else silver)
        for x in range(2):
            px(img, ax + x, ay + 7, skin)

    # 머리: 창백한 얼굴 (u0 v24, 6x6x6)
    h = box(img, 0, 24, 6, 6, 6, skin, 3, top=hair_blue)
    hx, hy = h["front"]
    # 슬픈 눈
    px(img, hx + 1, hy + 2, hexc("#2a3050"))
    px(img, hx + 4, hy + 2, hexc("#2a3050"))
    px(img, hx + 1, hy + 1, skin_shade)
    px(img, hx + 4, hy + 1, skin_shade)
    # 눈물 자국
    px(img, hx + 1, hy + 3, tear)
    px(img, hx + 1, hy + 4, tear)
    px(img, hx + 4, hy + 3, hexc("#6aa0d0"))
    # 입
    px(img, hx + 2, hy + 5, hexc("#a08a90"))
    px(img, hx + 3, hy + 5, hexc("#a08a90"))
    # 앞머리
    for x in range(6):
        px(img, hx + x, hy, hair_blue)
    for x in (0, 2, 5):
        px(img, hx + x, hy + 1, hair_mid)
    for side in ("right", "left"):
        ox, oy = h[side]
        for y in range(6):
            px(img, ox + 2, oy + y, hair_blue if y < 3 else hair_mid)

    # 긴 머리카락: 위는 푸르고 아래로 갈수록 검어진다 (u0 v36, 6x12x2)
    hr = box(img, 0, 36, 6, 12, 2, hair_blue, 6, top=hexc("#6a90e8"))
    hrx, hry = hr["front"]
    for y in range(12):
        if y < 4:
            c = hair_blue
        elif y < 8:
            c = hair_mid
        else:
            c = hair_black
        for x in range(6):
            px(img, hrx + x, hry + y, c)
    for y in range(12):
        px(img, hrx + 1, hry + y, hair_mid if y < 8 else hair_black)
        px(img, hrx + 4, hry + y, hair_black if y > 5 else hair_mid)
    for side in ("right", "left", "back"):
        ox, oy = hr[side]
        for y in range(12):
            c = hair_blue if y < 4 else hair_mid if y < 8 else hair_black
            for x in range(2):
                px(img, ox + x, oy + y, c)

    # 찢어진 망토 (u16 v36, 8x14x1)
    c = box(img, 16, 36, 8, 14, 1, cape, 4, top=cape_hi)
    cx, cy = c["front"]
    for y in range(14):
        for x in range(8):
            if (x * 3 + y * 5) % 13 == 0:
                px(img, cx + x, cy + y, cape_hi)
            elif (x + y) % 9 == 0:
                px(img, cx + x, cy + y, cape_dark)
    # 밑단의 찢긴 틈
    for x in (1, 4, 6):
        px(img, cx + x, cy + 13, (0, 0, 0, 0))
        px(img, cx + x, cy + 12, cape_dark)
    for x in range(8):
        px(img, cx + x, cy, cape_hi)

    # 떠 있는 검 (u34 v36, 1x8x1)
    sw = box(img, 34, 36, 1, 8, 1, blade, 4, top=blade_hi)
    swx, swy = sw["front"]
    for y in range(8):
        px(img, swx, swy + y, blade_hi if y < 5 else blade_dark)
    px(img, swx, swy + 6, hilt)
    px(img, swx, swy + 7, hexc("#8a6a20"))

    save(img, "textures/entity/knight_of_despair.png")


knight_of_despair()
print("assets knight ok")

TS2 = {"s": "#d0d8ff", "h": "#f0f4ff", "d": "#8a94c0", "g": "#c8a040", "k": "#2a3050", "b": "#4a70d0"}
icon([
    "................",
    "..............hh",
    ".............hh.",
    "............hh..",
    "...........hh...",
    "..........hh....",
    ".........hh.....",
    "........hh......",
    ".......hh.......",
    "......hh........",
    ".....gg.........",
    "....gg..........",
    "...kk...........",
    "..kk............",
    "..k.............",
    "................",
], TS2, "textures/item/tears_sword.png")
icon([
    "................",
    "...kk......kk...",
    "..kkkk....kkkk..",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "....kksssskk....",
    "....kksssskk....",
    "....kksssskk....",
    "....kksssskk....",
    "....kkkkkkkk....",
    "....kkbbbbkk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....dddddd.....",
    "................",
    "................",
], TS2, "textures/item/tears_sword_suit.png")

armor(hexc("#8aa0c8"), "#5a6e94", "#4a70d0", "textures/entity/equipment/humanoid/tears_sword.png")

for item, parent in [("tears_sword", "minecraft:item/handheld"), ("tears_sword_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/tears_sword.json", {"layers": {"humanoid": [{"texture": "lobcorp:tears_sword"}]}})

merge_lang({
    "entity.lobcorp.knight_of_despair": "절망의 기사",
    "item.lobcorp.tears_sword": "눈물로 벼려낸 검",
    "item.lobcorp.tears_sword_suit": "눈물로 벼려낸 검의 갑주",
}, {
    "entity.lobcorp.knight_of_despair": "The Knight of Despair",
    "item.lobcorp.tears_sword": "The Sword Sharpened with Tears",
    "item.lobcorp.tears_sword_suit": "Sword Sharpened with Tears Armor",
})


# ═════════════ 36차: 탐욕의 왕 (O-01-64) ═════════════
def king_of_greed():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    gold = hexc("#e8b830")
    gold_hi = hexc("#f8d868")
    gold_dark = hexc("#b08a18")
    jewel = hexc("#d02040")
    jewel_hi = hexc("#ff6a80")
    skin = hexc("#7a4a30")
    skin_shade = hexc("#5e3620")
    hair = hexc("#f4f0e8")
    hair_shade = hexc("#d0c8bc")
    crown = hexc("#f8d868")
    crown_dark = hexc("#b08a18")
    mouth = hexc("#100808")
    mouth_hi = hexc("#2a1414")
    tooth = hexc("#fbf6ea")
    tooth_dark = hexc("#c8c0b0")

    # ── 마법소녀 ──
    # 다리 (u0 v0, 2x8x2 / u8 v0, 2x8x2)
    for u in (0, 8):
        l = box(img, u, 0, 2, 8, 2, gold, 6, top=gold_hi)
        lx, ly = l["front"]
        for y in range(8):
            px(img, lx, ly + y, gold_dark if y % 3 == 0 else gold)
        for x in range(2):
            px(img, lx + x, ly + 7, hexc("#8a6a10"))

    # 드레스 (u0 v10, 8x7x6)
    d = box(img, 0, 10, 8, 7, 6, gold, 6, top=gold_hi)
    dx, dy = d["front"]
    # 붉은 보석 세 개
    for (x, y) in [(1, 1), (6, 1)]:
        px(img, dx + x, dy + y, jewel)
        px(img, dx + x, dy + y + 1, jewel_hi)
    for (x, y) in [(3, 4), (4, 4), (3, 5), (4, 5)]:
        px(img, dx + x, dy + y, jewel)
    px(img, dx + 3, dy + 4, jewel_hi)
    # 드레스 주름
    for y in range(7):
        px(img, dx + 2, dy + y, gold_dark)
        px(img, dx + 5, dy + y, gold_dark)
    for x in range(8):
        px(img, dx + x, dy + 6, gold_dark)
    for side in ("right", "left", "back"):
        sx, sy = d[side]
        for y in range(7):
            if y % 2 == 0:
                px(img, sx + 2, sy + y, gold_dark)
        for x in range(6):
            px(img, sx + x, sy + 6, gold_dark)
        px(img, sx + 3, sy + 2, jewel)

    # 몸통 (u28 v10, 5x5x3)
    b = box(img, 28, 10, 5, 5, 3, gold, 6)
    bx, by = b["front"]
    for x in range(5):
        px(img, bx + x, by, gold_hi)
    px(img, bx + 2, by + 2, jewel)
    px(img, bx + 2, by + 3, jewel_hi)

    # 팔 (u0 v24, 2x6x2)
    a = box(img, 0, 24, 2, 6, 2, skin, 4)
    ax, ay = a["front"]
    for y in range(6):
        px(img, ax, ay + y, skin_shade if y % 3 == 0 else skin)
    # 팔찌
    for x in range(2):
        px(img, ax + x, ay + 4, gold)
        px(img, ax + x, ay + 5, gold_dark)

    # 머리 (u0 v32, 6x6x6)
    h = box(img, 0, 32, 6, 6, 6, skin, 4, top=hair)
    hx, hy = h["front"]
    # 눈
    px(img, hx + 1, hy + 2, hexc("#1a0e08"))
    px(img, hx + 4, hy + 2, hexc("#1a0e08"))
    px(img, hx + 1, hy + 1, skin_shade)
    px(img, hx + 4, hy + 1, skin_shade)
    # 입
    px(img, hx + 2, hy + 4, hexc("#3a1a14"))
    px(img, hx + 3, hy + 4, hexc("#3a1a14"))
    # 앞머리
    for x in range(6):
        px(img, hx + x, hy, hair)
    for x in (0, 2, 5):
        px(img, hx + x, hy + 1, hair_shade)
    for side in ("right", "left"):
        sx, sy = h[side]
        for y in range(6):
            px(img, sx + 2, sy + y, hair if y < 4 else hair_shade)

    # 긴 흰 머리 (u0 v44, 6x10x2)
    hr = box(img, 0, 44, 6, 10, 2, hair, 4, top=hexc("#ffffff"))
    hrx, hry = hr["front"]
    for y in range(10):
        px(img, hrx + 1, hry + y, hair_shade if y % 3 == 0 else hair)
        px(img, hrx + 4, hry + y, hair_shade if y % 4 == 0 else hair)
    for x in range(6):
        px(img, hrx + x, hry + 9, hair_shade)
    for side in ("right", "left", "back"):
        sx, sy = hr[side]
        for y in range(10):
            if y % 3 == 0:
                px(img, sx + 1, sy + y, hair_shade)

    # 작은 왕관 (u24 v32, 4x2x4)
    cr = box(img, 24, 32, 4, 2, 4, crown, 4, top=hexc("#fff0b0"))
    crx, cry = cr["front"]
    for x in range(4):
        px(img, crx + x, cry + 1, crown_dark)
    px(img, crx + 1, cry, jewel)
    px(img, crx + 2, cry, jewel_hi)
    ctx, cty = cr["top"]
    for x in range(4):
        for y in range(4):
            if (x + y) % 3 == 0:
                px(img, ctx + x, cty + y, crown_dark)

    # ── 탈출: 거대한 황금빛 아가리 ──
    # 구체 (u0 v0, 16x16x16) — 64x64 텍스처에 꼭 맞는 크기 (2*d + 2*w = 64)
    o = box(img, 0, 0, 16, 16, 16, gold, 6, top=gold_hi)
    ox, oy = o["front"]
    # 어두운 띠
    for y in range(16):
        if y % 4 == 0:
            for x in range(16):
                px(img, ox + x, oy + y, gold_dark)
    for (x, y) in [(2, 2), (13, 3), (4, 13), (11, 12), (7, 6)]:
        px(img, ox + x, oy + y, gold_hi)
    for side in ("right", "left", "back"):
        sx, sy = o[side]
        for y in range(16):
            if y % 4 == 0:
                for x in range(16):
                    px(img, sx + x, sy + y, gold_dark)
        for (x, y) in [(3, 4), (10, 11)]:
            px(img, sx + x, sy + y, gold_hi)
    tx, ty = o["top"]
    for x in range(16):
        for y in range(16):
            if (x + y) % 7 == 0:
                px(img, tx + x, ty + y, gold_dark)
            elif (x * 3 + y) % 11 == 0:
                px(img, tx + x, ty + y, gold_hi)

    # 아가리 판 (u0 v40, 16x9x1) — 64x64 텍스처의 아래 경계에 걸치므로 16x9로 줄여 그린다
    m = box(img, 0, 40, 16, 9, 1, mouth, 4)
    mx, my = m["front"]
    # 톱니 같은 이빨: 위아래 한 줄씩
    for x in range(16):
        if x % 2 == 0:
            px(img, mx + x, my, tooth)
            px(img, mx + x, my + 1, tooth_dark)
        if x % 2 == 1:
            px(img, mx + x, my + 8, tooth)
            px(img, mx + x, my + 7, tooth_dark)
    # 목구멍의 그늘
    for y in range(3, 6):
        for x in range(4, 12):
            if (x + y) % 3 == 0:
                px(img, mx + x, my + y, mouth_hi)
    for x in range(16):
        px(img, mx + x, my + 4, hexc("#1a0a0a"))

    save(img, "textures/entity/king_of_greed.png")


king_of_greed()
print("assets king ok")

GR2 = {"g": "#e8b830", "h": "#f8d868", "d": "#b08a18", "r": "#d02040", "k": "#7a4a30", "w": "#f4f0e8"}
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
], GR2, "textures/item/gold_rush.png")
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
    "....gghhhhgg....",
    "....gggggggg....",
    "....gggggggg....",
    ".....dddddd.....",
    "................",
    "................",
], GR2, "textures/item/gold_rush_suit.png")

armor(hexc("#e8b830"), "#b08a18", "#d02040", "textures/entity/equipment/humanoid/gold_rush.png")

for item, parent in [("gold_rush", "minecraft:item/handheld"), ("gold_rush_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/gold_rush.json", {"layers": {"humanoid": [{"texture": "lobcorp:gold_rush"}]}})

merge_lang({
    "entity.lobcorp.king_of_greed": "탐욕의 왕",
    "item.lobcorp.gold_rush": "골드러시",
    "item.lobcorp.gold_rush_suit": "골드러시의 드레스",
}, {
    "entity.lobcorp.king_of_greed": "The King of Greed",
    "item.lobcorp.gold_rush": "Gold Rush",
    "item.lobcorp.gold_rush_suit": "Gold Rush Dress",
})


# ═════════════ 37차: 빨간 두건 용병 (F-01-57) ═════════════
def little_red():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    cloth = hexc("#3a2a2a")
    cloth_hi = hexc("#4a3636")
    cloth_dark = hexc("#241818")
    boot = hexc("#2a1a1a")
    boot_hi = hexc("#3a2626")
    skin = hexc("#f0e2d8")
    skin_shade = hexc("#d4c0b4")
    scar = hexc("#a04050")
    eye = hexc("#2a1a1a")
    hood = hexc("#b01020")
    hood_hi = hexc("#d02030")
    hood_dark = hexc("#7a0814")
    blade = hexc("#c0c0c8")
    blade_hi = hexc("#eef0f4")
    blade_dark = hexc("#7a7a84")
    blood = hexc("#8a0a14")
    grip = hexc("#4a3220")

    # 다리: 어두운 부츠와 바지 (u0 v0, 2x10x2 / u8 v0, 2x10x2)
    for u in (0, 8):
        l = box(img, u, 0, 2, 10, 2, boot, 4, top=boot_hi)
        lx, ly = l["front"]
        for y in range(10):
            px(img, lx, ly + y, boot_hi if y % 4 == 0 else boot)
            px(img, lx + 1, ly + y, boot if y % 3 == 0 else boot_hi)
        for x in range(2):
            px(img, lx + x, ly + 9, hexc("#141010"))

    # 몸통: 가죽 조끼 (u0 v12, 6x8x3)
    b = box(img, 0, 12, 6, 8, 3, cloth, 4, top=cloth_hi)
    bx, by = b["front"]
    # 벨트와 버클
    for x in range(6):
        px(img, bx + x, by + 6, hexc("#1a1010"))
        px(img, bx + x, by + 7, hexc("#1a1010"))
    px(img, bx + 2, by + 6, hexc("#c8a040"))
    px(img, bx + 3, by + 6, hexc("#c8a040"))
    # 조끼 여밈
    for y in range(8):
        px(img, bx + 2, by + y, cloth_dark)
        px(img, bx + 3, by + y, cloth_dark)
    # 피 묻은 자국
    for (x, y) in [(1, 2), (4, 3), (5, 4), (0, 5)]:
        px(img, bx + x, by + y, blood)
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for y in range(8):
            if y % 3 == 0:
                px(img, sx + 1, sy + y, cloth_hi)
        for x in range(3):
            px(img, sx + x, sy + 6, hexc("#1a1010"))
        px(img, sx + 1, sy + 2, blood)

    # 팔 (u24 v0, 2x9x2 / u32 v0, 2x9x2)
    for u in (24, 32):
        a = box(img, u, 0, 2, 9, 2, cloth, 4, top=cloth_hi)
        ax, ay = a["front"]
        for y in range(9):
            px(img, ax, ay + y, cloth_dark if y % 3 == 0 else cloth)
        for x in range(2):
            px(img, ax + x, ay + 8, skin)
        px(img, ax, ay + 4, blood)

    # 머리: 창백한 얼굴 + 흉터 (u0 v24, 6x6x6)
    h = box(img, 0, 24, 6, 6, 6, skin, 3, top=hood)
    hx, hy = h["front"]
    # 눈
    px(img, hx + 1, hy + 2, eye)
    px(img, hx + 4, hy + 2, eye)
    px(img, hx + 1, hy + 1, skin_shade)
    px(img, hx + 4, hy + 1, skin_shade)
    # 흉터: 왼쪽 뺨을 가로지른다
    for (x, y) in [(0, 2), (1, 3), (1, 4), (2, 4)]:
        px(img, hx + x, hy + y, scar)
    # 입
    px(img, hx + 2, hy + 5, hexc("#8a5a5a"))
    px(img, hx + 3, hy + 5, hexc("#8a5a5a"))
    # 두건 그늘
    for x in range(6):
        px(img, hx + x, hy, hood_dark)
    for side in ("right", "left", "back"):
        sx, sy = h[side]
        for y in range(6):
            px(img, sx + 2, sy + y, hood_dark if y < 2 else skin_shade)

    # 두건 (u0 v36, 7x7x7)
    hd = box(img, 0, 36, 7, 7, 7, hood, 6, top=hood_hi)
    hdx, hdy = hd["front"]
    # 두건 앞면: 얼굴이 드러나는 어두운 구멍
    rect(img, hdx + 1, hdy + 2, 5, 4, hood_dark, 4)
    for x in range(7):
        px(img, hdx + x, hdy, hood_hi)
        px(img, hdx + x, hdy + 6, hood_dark)
    for side in ("right", "left", "back"):
        sx, sy = hd[side]
        for y in range(7):
            for x in range(7):
                if (x * 3 + y * 5) % 11 == 0:
                    px(img, sx + x, sy + y, hood_hi)
                elif (x + y) % 7 == 0:
                    px(img, sx + x, sy + y, hood_dark)
        for x in range(7):
            px(img, sx + x, sy + 6, hood_dark)
    tx, ty = hd["top"]
    for x in range(7):
        for y in range(7):
            if (x + y) % 5 == 0:
                px(img, tx + x, ty + y, hood_hi)

    # 망토 (u28 v36, 8x14x1)
    c = box(img, 28, 36, 8, 14, 1, hood, 6, top=hood_hi)
    cx, cy = c["front"]
    for y in range(14):
        for x in range(8):
            if (x * 5 + y * 3) % 13 == 0:
                px(img, cx + x, cy + y, hood_hi)
            elif (x + y) % 9 == 0:
                px(img, cx + x, cy + y, hood_dark)
    # 밑단의 찢긴 틈
    for x in (2, 5):
        px(img, cx + x, cy + 13, (0, 0, 0, 0))
        px(img, cx + x, cy + 12, hood_dark)
    for x in range(8):
        px(img, cx + x, cy, hood_hi)
    # 망토에 밴 피
    for (x, y) in [(1, 4), (2, 5), (6, 8), (5, 9)]:
        px(img, cx + x, cy + y, blood)

    # 도끼날 (u40 v0, 1x6x3)
    bl = box(img, 40, 0, 1, 6, 3, blade, 4, top=blade_hi)
    blx, bly = bl["front"]
    for y in range(6):
        px(img, blx, bly + y, blade_hi if y < 4 else blade_dark)
    for side in ("right", "left", "back"):
        sx, sy = bl[side]
        for y in range(6):
            px(img, sx + 1, sy + y, blade_dark if y % 2 == 0 else blade)
        px(img, sx + 1, sy + 4, blood)
    # 자루
    for y in range(6):
        px(img, blx, bly + y, grip if y > 3 else blade_hi)

    save(img, "textures/entity/little_red.png")


little_red()
print("assets littlered ok")

CS = {"r": "#b01020", "h": "#d02030", "d": "#7a0814", "s": "#c0c0c8", "l": "#eef0f4", "k": "#2a1a1a"}
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
    "....rr..........",
    "...rr...........",
    "..dd............",
    "..d.............",
    "................",
], CS, "textures/item/crimson_scar.png")
icon([
    "................",
    "...rr......rr...",
    "..rrrr....rrrr..",
    "..rrrrrrrrrrrr..",
    "...rrrrrrrrrr...",
    "....rrssssrr....",
    "....rrssssrr....",
    "....rrssssrr....",
    "....rrssssrr....",
    "....rrrrrrrr....",
    "....rrllllrr....",
    "....rrrrrrrr....",
    "....rrrrrrrr....",
    ".....dddddd.....",
    "................",
    "................",
], CS, "textures/item/crimson_scar_suit.png")

armor(hexc("#b01020"), "#7a0814", "#c0c0c8", "textures/entity/equipment/humanoid/crimson_scar.png")

for item, parent in [("crimson_scar", "minecraft:item/handheld"), ("crimson_scar_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/crimson_scar.json", {"layers": {"humanoid": [{"texture": "lobcorp:crimson_scar"}]}})

merge_lang({
    "entity.lobcorp.little_red": "빨간 두건 용병",
    "item.lobcorp.crimson_scar": "진홍빛 상처",
    "item.lobcorp.crimson_scar_suit": "진홍빛 상처의 옷",
}, {
    "entity.lobcorp.little_red": "Little Red Riding Hooded Mercenary",
    "item.lobcorp.crimson_scar": "Crimson Scar",
    "item.lobcorp.crimson_scar_suit": "Crimson Scar Garb",
})


# ═════════════ 38차: 크고 나빠질 늑대 (F-02-58) ═════════════
def big_bad_wolf():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    fur = hexc("#7a7a80")
    fur_hi = hexc("#8e8e96")
    fur_dark = hexc("#5a5a60")
    chest = hexc("#c8c8c8")
    chest_shade = hexc("#a8a8a8")
    nose = hexc("#1a1a1e")
    eye = hexc("#e8d060")
    eye_dim = hexc("#a89030")
    claw = hexc("#e8e4d8")
    fleece = hexc("#f4f0e6")
    fleece_shade = hexc("#d8d2c4")
    tongue = hexc("#c05a6a")

    # 몸통 (u0 v0, 10x10x6)
    b = box(img, 0, 0, 10, 10, 6, fur, 8, top=fur_hi)
    bx, by = b["front"]
    # 가슴의 밝은 털
    for y in range(2, 9):
        for x in range(2, 8):
            px(img, bx + x, by + y, chest if (x + y) % 5 else chest_shade)
    # 털 결
    for y in range(10):
        for x in range(10):
            if (x * 3 + y * 5) % 13 == 0:
                px(img, bx + x, by + y, fur_dark)
    for x in range(10):
        px(img, bx + x, by + 9, fur_dark)
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for y in range(10):
            for x in range(6):
                if (x * 5 + y * 3) % 11 == 0:
                    px(img, sx + x, sy + y, fur_dark)
                elif (x + y) % 7 == 0:
                    px(img, sx + x, sy + y, fur_hi)
        for x in range(6):
            px(img, sx + x, sy + 9, fur_dark)
    tx, ty = b["top"]
    for x in range(10):
        for y in range(6):
            if (x + y * 2) % 9 == 0:
                px(img, tx + x, ty + y, fur_dark)
            elif (x * 3 + y) % 13 == 0:
                px(img, tx + x, ty + y, fur_hi)

    # 가슴 덧판 (u32 v0, 8x7x2)
    c = box(img, 32, 0, 8, 7, 2, chest, 6, top=hexc("#dcdcdc"))
    cx, cy = c["front"]
    for y in range(7):
        for x in range(8):
            if (x * 3 + y) % 7 == 0:
                px(img, cx + x, cy + y, chest_shade)
    for x in range(8):
        px(img, cx + x, cy + 6, chest_shade)

    # 머리 (u0 v16, 6x6x7)
    h = box(img, 0, 16, 6, 6, 7, fur, 8, top=fur_hi)
    hx, hy = h["front"]
    # 슬픈 노란 눈: 눈꼬리가 처진다
    for (x, y) in [(1, 2), (2, 2), (1, 3)]:
        px(img, hx + x, hy + y, eye)
    for (x, y) in [(3, 2), (4, 2), (4, 3)]:
        px(img, hx + x, hy + y, eye)
    px(img, hx + 1, hy + 3, eye_dim)
    px(img, hx + 4, hy + 3, eye_dim)
    px(img, hx + 1, hy + 1, fur_dark)
    px(img, hx + 4, hy + 1, fur_dark)
    for x in range(6):
        px(img, hx + x, hy + 5, fur_dark)
    for side in ("right", "left", "back"):
        sx, sy = h[side]
        for y in range(6):
            for x in range(7):
                if (x * 3 + y * 5) % 9 == 0:
                    px(img, sx + x, sy + y, fur_dark)
                elif (x + y) % 6 == 0:
                    px(img, sx + x, sy + y, fur_hi)
        for x in range(7):
            px(img, sx + x, sy + 5, fur_dark)

    # 주둥이 (u26 v16, 4x3x4)
    s = box(img, 26, 16, 4, 3, 4, fur, 8, top=fur_hi)
    sx, sy = s["front"]
    # 코
    px(img, sx + 1, sy, nose)
    px(img, sx + 2, sy, nose)
    # 벌린 아가리와 혀
    for x in range(4):
        px(img, sx + x, sy + 2, hexc("#3a1a20"))
    px(img, sx + 1, sy + 2, tongue)
    px(img, sx + 2, sy + 2, tongue)
    # 이빨
    px(img, sx, sy + 1, claw)
    px(img, sx + 3, sy + 1, claw)
    for side in ("right", "left", "back"):
        ox, oy = s[side]
        for y in range(3):
            for x in range(4):
                if (x + y) % 4 == 0:
                    px(img, ox + x, oy + y, fur_dark)

    # 귀 (u42 v16, 2x3x1)
    e = box(img, 42, 16, 2, 3, 1, fur, 8, top=fur_hi)
    ex, ey = e["front"]
    for y in range(3):
        px(img, ex, ey + y, fur_dark)
        px(img, ex + 1, ey + y, fur_hi if y == 0 else fur)

    # 입에 물린 양털 (u48 v16, 4x3x2)
    f = box(img, 48, 16, 4, 3, 2, fleece, 4, top=hexc("#fbf8f0"))
    fx, fy = f["front"]
    for y in range(3):
        for x in range(4):
            if (x + y) % 3 == 0:
                px(img, fx + x, fy + y, fleece_shade)
    for x in range(4):
        px(img, fx + x, fy + 2, fleece_shade)
    for side in ("right", "left", "back"):
        ox, oy = f[side]
        for y in range(3):
            for x in range(2):
                if (x + y) % 2 == 0:
                    px(img, ox + x, oy + y, fleece_shade)

    # 팔 (u0 v30, 3x11x3)
    a = box(img, 0, 30, 3, 11, 3, fur, 8, top=fur_hi)
    ax, ay = a["front"]
    for y in range(11):
        px(img, ax + 1, ay + y, fur_dark if y % 4 == 0 else fur)
    for (x, y) in [(0, 3), (2, 7)]:
        px(img, ax + x, ay + y, fur_hi)
    for x in range(3):
        px(img, ax + x, ay + 10, fur_dark)

    # 발톱 (u12 v30, 4x1x4)
    cl = box(img, 12, 30, 4, 1, 4, claw, 4, top=hexc("#fbf8f0"))
    clx, cly = cl["front"]
    for x in range(4):
        px(img, clx + x, cly, hexc("#c8c4b8") if x % 2 == 0 else claw)

    # 다리 (u24 v30, 3x10x3)
    l = box(img, 24, 30, 3, 10, 3, fur, 8, top=fur_hi)
    lx, ly = l["front"]
    for y in range(10):
        px(img, lx + 1, ly + y, fur_dark if y % 5 == 0 else fur)
    for x in range(3):
        px(img, lx + x, ly + 9, fur_dark)

    # 발 (u36 v30, 4x1x5)
    ft = box(img, 36, 30, 4, 1, 5, fur_dark, 6, top=fur)
    ftx, fty = ft["front"]
    for x in range(4):
        px(img, ftx + x, fty, claw if x % 2 == 0 else fur_dark)

    # 꼬리 (u48 v30, 2x6x2)
    t = box(img, 48, 30, 2, 6, 2, fur, 8, top=fur_hi)
    tx2, ty2 = t["front"]
    for y in range(6):
        px(img, tx2, ty2 + y, fur_dark if y % 2 == 0 else fur)
        px(img, tx2 + 1, ty2 + y, fur_hi if y % 3 == 0 else fur)
    for x in range(2):
        px(img, tx2 + x, ty2 + 5, fur_dark)

    save(img, "textures/entity/big_bad_wolf.png")


big_bad_wolf()
print("assets wolf ok")

CS2 = {"g": "#7a7a80", "h": "#8e8e96", "d": "#5a5a60", "c": "#c8c8c8", "k": "#1a1a1e", "y": "#e8d060"}
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
], CS2, "textures/item/cobalt_scar.png")
icon([
    "................",
    "...gg......gg...",
    "..gggg....gggg..",
    "..gggggggggggg..",
    "...gggggggggg...",
    "....ggccccgg....",
    "....ggccccgg....",
    "....ggccccgg....",
    "....ggccccgg....",
    "....gggggggg....",
    "....ggyyyygg....",
    "....gggggggg....",
    "....gggggggg....",
    ".....dddddd.....",
    "................",
    "................",
], CS2, "textures/item/cobalt_scar_suit.png")

armor(hexc("#7a7a80"), "#5a5a60", "#c8c8c8", "textures/entity/equipment/humanoid/cobalt_scar.png")

for item, parent in [("cobalt_scar", "minecraft:item/handheld"), ("cobalt_scar_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/cobalt_scar.json", {"layers": {"humanoid": [{"texture": "lobcorp:cobalt_scar"}]}})

merge_lang({
    "entity.lobcorp.big_bad_wolf": "크고 나빠질 늑대",
    "item.lobcorp.cobalt_scar": "흑청빛 상처",
    "item.lobcorp.cobalt_scar_suit": "흑청빛 상처의 옷",
}, {
    "entity.lobcorp.big_bad_wolf": "Big and Will be Bad Wolf",
    "item.lobcorp.cobalt_scar": "Cobalt Scar",
    "item.lobcorp.cobalt_scar_suit": "Cobalt Scar Garb",
})


# ═════════════ 39차: 알리우네 (T-04-53) ═════════════
def alriune():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    petal = hexc("#f4c0d0")
    petal_hi = hexc("#fbd8e4")
    petal_dark = hexc("#d898b0")
    petal_deep = hexc("#b87090")
    branch = hexc("#6a4a3a")
    branch_hi = hexc("#7d5a46")
    branch_dark = hexc("#4a3226")
    skin = hexc("#f6e8e4")
    skin_shade = hexc("#e0ccc8")
    crown = hexc("#f8d0dc")
    crown_dark = hexc("#c88aa4")

    # 드레스 (u0 v0, 8x16x6)
    d = box(img, 0, 0, 8, 16, 6, petal, 6, top=petal_hi)
    dx, dy = d["front"]
    # 꽃잎 무늬: 겹겹이 흩어진 분홍 조각
    for (x, y) in [(1, 1), (2, 1), (1, 2), (5, 3), (6, 3), (5, 4),
                   (2, 6), (3, 6), (2, 7), (6, 8), (5, 9), (1, 10),
                   (3, 12), (4, 12), (3, 13), (6, 14), (0, 14)]:
        px(img, dx + x, dy + y, petal_dark)
    for (x, y) in [(2, 2), (6, 4), (3, 7), (5, 10), (4, 13)]:
        px(img, dx + x, dy + y, petal_deep)
    # 드레스 자락의 꽃잎 결
    for x in range(8):
        px(img, dx + x, dy + 15, petal_dark)
    for side in ("right", "left", "back"):
        sx, sy = d[side]
        for y in range(16):
            for x in range(6):
                if (x * 3 + y * 5) % 11 == 0:
                    px(img, sx + x, sy + y, petal_dark)
                elif (x + y) % 13 == 0:
                    px(img, sx + x, sy + y, petal_deep)
        for x in range(6):
            px(img, sx + x, sy + 15, petal_dark)
    tx, ty = d["top"]
    for x in range(8):
        for y in range(6):
            if (x + y) % 5 == 0:
                px(img, tx + x, ty + y, petal_dark)

    # 몸통 (u28 v0, 5x6x3)
    b = box(img, 28, 0, 5, 6, 3, petal, 6, top=petal_hi)
    bx, by = b["front"]
    for y in range(6):
        px(img, bx + 2, by + y, petal_dark)
    for (x, y) in [(1, 2), (3, 4)]:
        px(img, bx + x, by + y, petal_deep)
    for x in range(5):
        px(img, bx + x, by + 5, petal_dark)

    # 팔: 가느다란 가지 (u44 v0, 1x10x1)
    a = box(img, 44, 0, 1, 10, 1, branch, 6, top=branch_hi)
    ax, ay = a["front"]
    for y in range(10):
        px(img, ax, ay + y, branch_dark if y % 3 == 0 else branch)
    # 가지 끝의 작은 꽃
    px(img, ax, ay + 9, petal_hi)
    px(img, ax, ay + 8, petal)

    # 머리: 얼굴이 없다 (u0 v24, 5x6x5)
    h = box(img, 0, 24, 5, 6, 5, skin, 3, top=crown)
    hx, hy = h["front"]
    # 얼굴 대신 옅은 그늘만
    for y in range(2, 5):
        for x in range(1, 4):
            if (x + y) % 3 == 0:
                px(img, hx + x, hy + y, skin_shade)
    for x in range(5):
        px(img, hx + x, hy + 5, skin_shade)
    for side in ("right", "left", "back"):
        sx, sy = h[side]
        for y in range(6):
            if y % 3 == 0:
                px(img, sx + 2, sy + y, skin_shade)

    # 꽃잎 왕관 (u20 v24, 6x2x6)
    c = box(img, 20, 24, 6, 2, 6, crown, 4, top=hexc("#fde8f0"))
    cx, cy = c["front"]
    for x in range(6):
        px(img, cx + x, cy + 1, crown_dark)
    for (x, y) in [(1, 0), (4, 0)]:
        px(img, cx + x, cy + y, petal_deep)
    ctx, cty = c["top"]
    for x in range(6):
        for y in range(6):
            if (x + y) % 4 == 0:
                px(img, ctx + x, cty + y, crown_dark)

    # 등 뒤의 큰 꽃잎 세 장 (u0 v36, 6x8x1 / u16 v36 / u32 v36)
    for u in (0, 16, 32):
        p = box(img, u, 36, 6, 8, 1, petal, 6, top=petal_hi)
        px_, py_ = p["front"]
        # 꽃잎의 잎맥
        for y in range(8):
            px(img, px_ + 2, py_ + y, petal_dark)
            px(img, px_ + 3, py_ + y, petal_dark)
        for (x, y) in [(1, 2), (4, 2), (1, 5), (4, 5), (0, 7), (5, 7)]:
            px(img, px_ + x, py_ + y, petal_deep)
        for x in range(6):
            px(img, px_ + x, py_ + 7, petal_dark)

    save(img, "textures/entity/alriune.png")


FA = {"p": "#f4c0d0", "h": "#fbd8e4", "d": "#d898b0", "b": "#6a4a3a", "k": "#b87090", "s": "#f6e8e4"}
icon([
    "................",
    "..............pp",
    ".............pp.",
    "............pp..",
    "...........pp...",
    "..........pp....",
    ".........pp.....",
    "........bb......",
    ".......bb.......",
    "......bb........",
    ".....bb.........",
    "....bb..........",
    "...dd...........",
    "..dd............",
    "..d.............",
    "................",
], FA, "textures/item/faint_aroma.png")
icon([
    "................",
    "...pp......pp...",
    "..pppp....pppp..",
    "..pppppppppppp..",
    "...pppppppppp...",
    "....pphhhhpp....",
    "....pphhhhpp....",
    "....pphhhhpp....",
    "....pphhhhpp....",
    "....pppppppp....",
    "....ppkkkkpp....",
    "....pppppppp....",
    "....pppppppp....",
    ".....dddddd.....",
    "................",
    "................",
], FA, "textures/item/faint_aroma_suit.png")

armor(hexc("#f4c0d0"), "#d898b0", "#6a4a3a", "textures/entity/equipment/humanoid/faint_aroma.png")

for item, parent in [("faint_aroma", "minecraft:item/handheld"), ("faint_aroma_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/faint_aroma.json", {"layers": {"humanoid": [{"texture": "lobcorp:faint_aroma"}]}})

alriune()
print("assets alriune ok")

merge_lang({
    "entity.lobcorp.alriune": "알리우네",
    "item.lobcorp.faint_aroma": "희미한 향기",
    "item.lobcorp.faint_aroma_suit": "희미한 향기의 옷",
}, {
    "entity.lobcorp.alriune": "Alriune",
    "item.lobcorp.faint_aroma": "Faint Aroma",
    "item.lobcorp.faint_aroma_suit": "Faint Aroma Garb",
})


# ═════════════ 40차: 여왕벌 (T-04-50) + 일벌 ═════════════
def queen_bee():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    wax = hexc("#d8a840")
    wax_hi = hexc("#e8c060")
    wax_dark = hexc("#a87a20")
    flesh = hexc("#7a1a20")
    flesh_dark = hexc("#4a0a10")
    stripe_dark = hexc("#2a1a0a")
    stripe_light = hexc("#e0a020")
    stripe_hi = hexc("#f0c050")
    thorax = hexc("#3a2a14")
    thorax_hi = hexc("#4e3a1e")
    eye = hexc("#e01b2e")
    eye_hi = hexc("#ff7080")
    antenna = hexc("#1a1208")
    wing = hexc("#e8f0ff")
    wing_shade = hexc("#c0d0e8")

    # 벌집 받침 (u0 v0, 16x6x16)
    h = box(img, 0, 0, 16, 6, 16, wax, 6, top=wax_hi)
    hx, hy = h["front"]
    # 벌집무늬: 육각형 격자
    for y in range(6):
        for x in range(16):
            if (x + (y // 2) * 2) % 4 == 0 and y % 2 == 0:
                px(img, hx + x, hy + y, wax_dark)
            elif (x + (y // 2) * 2) % 4 == 2 and y % 2 == 1:
                px(img, hx + x, hy + y, wax_dark)
    # 살점 반점
    for (x, y) in [(2, 1), (3, 1), (2, 2), (11, 3), (12, 3), (11, 4), (6, 4), (7, 4)]:
        px(img, hx + x, hy + y, flesh)
    for (x, y) in [(3, 2), (12, 4), (7, 5)]:
        px(img, hx + x, hy + y, flesh_dark)
    for x in range(16):
        px(img, hx + x, hy + 5, wax_dark)
    for side in ("right", "left", "back"):
        sx, sy = h[side]
        for y in range(6):
            for x in range(16):
                if (x + y) % 4 == 0:
                    px(img, sx + x, sy + y, wax_dark)
                elif (x * 3 + y * 5) % 17 == 0:
                    px(img, sx + x, sy + y, flesh)
        for x in range(16):
            px(img, sx + x, sy + 5, wax_dark)
    tx, ty = h["top"]
    for x in range(16):
        for y in range(16):
            if (x + y) % 4 == 0:
                px(img, tx + x, ty + y, wax_dark)
            elif (x * 5 + y * 3) % 23 == 0:
                px(img, tx + x, ty + y, flesh)
            elif (x * 3 + y) % 11 == 0:
                px(img, tx + x, ty + y, wax_hi)

    # 부푼 배 (u0 v24, 10x8x12)
    a = box(img, 0, 24, 10, 8, 12, stripe_light, 6, top=stripe_hi)
    ax, ay = a["front"]
    # 가로 줄무늬
    for y in range(8):
        if y % 3 == 0:
            for x in range(10):
                px(img, ax + x, ay + y, stripe_dark)
        elif y % 3 == 1:
            for x in range(10):
                px(img, ax + x, ay + y, stripe_hi if x % 2 == 0 else stripe_light)
    for x in range(10):
        px(img, ax + x, ay + 7, stripe_dark)
    for side in ("right", "left", "back"):
        sx, sy = a[side]
        for y in range(8):
            for x in range(12):
                if y % 3 == 0:
                    px(img, sx + x, sy + y, stripe_dark)
                elif y % 3 == 1:
                    px(img, sx + x, sy + y, stripe_hi if x % 2 == 0 else stripe_light)
        for x in range(12):
            px(img, sx + x, sy + 7, stripe_dark)
    tx, ty = a["top"]
    for x in range(10):
        for y in range(12):
            if y % 3 == 0:
                px(img, tx + x, ty + y, stripe_dark)
            elif y % 3 == 1:
                px(img, tx + x, ty + y, stripe_hi)

    # 가슴 (u0 v44, 6x6x6)
    t = box(img, 0, 44, 6, 6, 6, thorax, 6, top=thorax_hi)
    tx2, ty2 = t["front"]
    for y in range(6):
        for x in range(6):
            if (x * 3 + y * 5) % 7 == 0:
                px(img, tx2 + x, ty2 + y, stripe_light)
    for x in range(6):
        px(img, tx2 + x, ty2 + 5, stripe_dark)
    for side in ("right", "left", "back"):
        sx, sy = t[side]
        for y in range(6):
            for x in range(6):
                if (x + y) % 5 == 0:
                    px(img, sx + x, sy + y, stripe_light)
        for x in range(6):
            px(img, sx + x, sy + 5, stripe_dark)

    # 머리 (u32 v24, 5x5x5)
    hd = box(img, 32, 24, 5, 5, 5, thorax, 6, top=thorax_hi)
    hdx, hdy = hd["front"]
    # 큰 붉은 눈
    for (x, y) in [(0, 1), (1, 1), (0, 2), (1, 2), (0, 3), (1, 3)]:
        px(img, hdx + x, hdy + y, eye)
    for (x, y) in [(3, 1), (4, 1), (3, 2), (4, 2), (3, 3), (4, 3)]:
        px(img, hdx + x, hdy + y, eye)
    px(img, hdx, hdy + 1, eye_hi)
    px(img, hdx + 4, hdy + 1, eye_hi)
    # 입
    for x in (2, 3):
        px(img, hdx + x, hdy + 4, stripe_dark)
    for side in ("right", "left", "back"):
        sx, sy = hd[side]
        for y in range(5):
            for x in range(5):
                if (x + y) % 4 == 0:
                    px(img, sx + x, sy + y, stripe_light)
        for x in range(5):
            px(img, sx + x, sy + 4, stripe_dark)

    # 더듬이 (u48 v24, 1x5x1)
    an = box(img, 48, 24, 1, 5, 1, antenna, 4)
    anx, any_ = an["front"]
    for y in range(5):
        px(img, anx, any_ + y, hexc("#3a2a14") if y % 2 == 0 else antenna)

    # 날개 (u0 v48, 8x1x12) — 64x64 텍스처의 아래 경계에 걸치지 않도록 v를 올린다
    w = box(img, 0, 48, 8, 1, 12, wing, 4, top=hexc("#f8fbff"))
    wx, wy = w["front"]
    for x in range(8):
        px(img, wx + x, wy, wing_shade)
    wtx, wty = w["top"]
    for x in range(8):
        for y in range(12):
            if x == 0 or y == 0 or x == 7 or y == 11:
                px(img, wtx + x, wty + y, wing_shade)
            elif (x + y) % 5 == 0:
                px(img, wtx + x, wty + y, wing_shade)

    save(img, "textures/entity/queen_bee.png")


def worker_bee():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    chitin = hexc("#3a2a14")
    chitin_hi = hexc("#4e3a1e")
    chitin_dark = hexc("#1a1208")
    stripe_dark = hexc("#2a1a0a")
    stripe_light = hexc("#e0a020")
    stripe_hi = hexc("#f0c050")
    skin = hexc("#c8b8a0")
    skin_shade = hexc("#a89880")
    eye = hexc("#e01b2e")
    eye_hi = hexc("#ff7080")
    sting = hexc("#e8e4d8")
    wing = hexc("#e8f0ff")
    wing_shade = hexc("#c0d0e8")
    blood = hexc("#7a1a20")

    # 가슴 (u0 v0, 6x6x4)
    t = box(img, 0, 0, 6, 6, 4, chitin, 6, top=chitin_hi)
    tx, ty = t["front"]
    for y in range(6):
        for x in range(6):
            if (x * 3 + y * 5) % 7 == 0:
                px(img, tx + x, ty + y, stripe_light)
    for (x, y) in [(1, 2), (4, 3)]:
        px(img, tx + x, ty + y, blood)
    for x in range(6):
        px(img, tx + x, ty + 5, chitin_dark)
    for side in ("right", "left", "back"):
        sx, sy = t[side]
        for y in range(6):
            for x in range(4):
                if (x + y) % 5 == 0:
                    px(img, sx + x, sy + y, stripe_light)
        for x in range(4):
            px(img, sx + x, sy + 5, chitin_dark)

    # 줄무늬 배 (u0 v12, 6x6x8)
    a = box(img, 0, 12, 6, 6, 8, stripe_light, 6, top=stripe_hi)
    ax, ay = a["front"]
    for y in range(6):
        if y % 2 == 0:
            for x in range(6):
                px(img, ax + x, ay + y, stripe_dark)
        else:
            for x in range(6):
                px(img, ax + x, ay + y, stripe_hi if x % 2 == 0 else stripe_light)
    for x in range(6):
        px(img, ax + x, ay + 5, stripe_dark)
    for side in ("right", "left", "back"):
        sx, sy = a[side]
        for y in range(6):
            for x in range(8):
                if y % 2 == 0:
                    px(img, sx + x, sy + y, stripe_dark)
                else:
                    px(img, sx + x, sy + y, stripe_hi if x % 2 == 0 else stripe_light)
        for x in range(8):
            px(img, sx + x, sy + 5, stripe_dark)
    tx2, ty2 = a["top"]
    for x in range(6):
        for y in range(8):
            if y % 2 == 0:
                px(img, tx2 + x, ty2 + y, stripe_dark)
            else:
                px(img, tx2 + x, ty2 + y, stripe_hi)

    # 벌 머리 (u28 v0, 5x5x5)
    h = box(img, 28, 0, 5, 5, 5, chitin, 6, top=chitin_hi)
    hx, hy = h["front"]
    # 눈
    for (x, y) in [(0, 1), (1, 1), (0, 2), (1, 2)]:
        px(img, hx + x, hy + y, eye)
    for (x, y) in [(3, 1), (4, 1), (3, 2), (4, 2)]:
        px(img, hx + x, hy + y, eye)
    px(img, hx, hy + 1, eye_hi)
    px(img, hx + 4, hy + 1, eye_hi)
    # 사람의 이빨이 남은 입
    for x in range(1, 4):
        px(img, hx + x, hy + 4, skin if x % 2 == 0 else skin_shade)
    for side in ("right", "left", "back"):
        sx, sy = h[side]
        for y in range(5):
            for x in range(5):
                if (x + y) % 4 == 0:
                    px(img, sx + x, sy + y, stripe_light)
        for x in range(5):
            px(img, sx + x, sy + 4, chitin_dark)

    # 더듬이 (u44 v0, 1x3x1)
    an = box(img, 44, 0, 1, 3, 1, chitin_dark, 4)
    anx, any_ = an["front"]
    for y in range(3):
        px(img, anx, any_ + y, chitin if y % 2 == 0 else chitin_dark)

    # 팔 (u0 v28, 2x8x2)
    for u in (0, 8):
        ar = box(img, u, 28, 2, 8, 2, chitin, 6, top=chitin_hi)
        arx, ary = ar["front"]
        for y in range(8):
            px(img, arx, ary + y, chitin_dark if y % 3 == 0 else chitin)
            px(img, arx + 1, ary + y, chitin_hi if y % 4 == 0 else chitin)
        # 끝의 침
        for x in range(2):
            px(img, arx + x, ary + 7, sting)

    # 다리 (u12 v28, 2x8x2)
    for u in (12, 20):
        l = box(img, u, 28, 2, 8, 2, chitin, 6, top=chitin_hi)
        lx, ly = l["front"]
        for y in range(8):
            px(img, lx, ly + y, chitin_dark if y % 4 == 0 else chitin)
            px(img, lx + 1, ly + y, chitin_hi if y % 5 == 0 else chitin)
        for x in range(2):
            px(img, lx + x, ly + 7, chitin_dark)

    # 작은 날개 (u24 v28, 5x1x6)
    w = box(img, 24, 28, 5, 1, 6, wing, 4, top=hexc("#f8fbff"))
    wx, wy = w["front"]
    for x in range(5):
        px(img, wx + x, wy, wing_shade)
    wtx, wty = w["top"]
    for x in range(5):
        for y in range(6):
            if x == 0 or y == 0 or x == 4 or y == 5:
                px(img, wtx + x, wty + y, wing_shade)

    save(img, "textures/entity/worker_bee.png")


HN = {"y": "#e0a020", "h": "#f0c050", "d": "#2a1a0a", "r": "#e01b2e", "k": "#3a2a14", "w": "#e8f0ff"}
icon([
    "................",
    "..............yy",
    ".............yy.",
    "............yy..",
    "...........yy...",
    "..........yy....",
    ".........yy.....",
    "........yy......",
    ".......yy.......",
    "......yy........",
    ".....yy.........",
    "....yy..........",
    "...dd...........",
    "..dd............",
    "..d.............",
    "................",
], HN, "textures/item/hornet.png")
icon([
    "................",
    "...yy......yy...",
    "..yyyy....yyyy..",
    "..yyyyyyyyyyyy..",
    "...yyyyyyyyyy...",
    "....yyrrrryy....",
    "....yyrrrryy....",
    "....yyrrrryy....",
    "....yyrrrryy....",
    "....yyyyyyyy....",
    "....yyhhhh yy...".replace(" ", ""),
    "....yyyyyyyy....",
    "....yyyyyyyy....",
    ".....dddddd.....",
    "................",
    "................",
], HN, "textures/item/hornet_suit.png")

armor(hexc("#e0a020"), "#2a1a0a", "#e01b2e", "textures/entity/equipment/humanoid/hornet.png")

for item, parent in [("hornet", "minecraft:item/handheld"), ("hornet_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/hornet.json", {"layers": {"humanoid": [{"texture": "lobcorp:hornet"}]}})

queen_bee()
worker_bee()
print("assets queenbee ok")

merge_lang({
    "entity.lobcorp.queen_bee": "여왕벌",
    "entity.lobcorp.worker_bee": "일벌",
    "item.lobcorp.hornet": "말벌",
    "item.lobcorp.hornet_suit": "말벌의 옷",
}, {
    "entity.lobcorp.queen_bee": "Queen Bee",
    "entity.lobcorp.worker_bee": "Worker Bee",
    "item.lobcorp.hornet": "Hornet",
    "item.lobcorp.hornet_suit": "Hornet Garb",
})


# ═════════════ 41차: 흑조의 꿈 (F-02-70) ═════════════
def black_swan():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    water = hexc("#1a2a3a")
    water_hi = hexc("#24384c")
    water_dark = hexc("#0e1a26")
    white = hexc("#f4f4f4")
    white_shade = hexc("#d8d8d8")
    black = hexc("#101010")
    black_hi = hexc("#242424")
    beak = hexc("#e8a030")
    feather = hexc("#141418")
    feather_hi = hexc("#22222a")
    feather_dark = hexc("#0a0a0e")
    skin = hexc("#f0e8e4")
    skin_shade = hexc("#d4c8c4")
    eye = hexc("#2a1a24")
    crown = hexc("#0e0e12")
    crown_hi = hexc("#2a2a34")
    stick = hexc("#1a1a20")
    canopy = hexc("#141418")
    canopy_hi = hexc("#2a2a34")
    canopy_dark = hexc("#08080a")
    wing = hexc("#0e0e14")
    wing_hi = hexc("#2a2a3a")

    # 호수 판 (u0 v0, 20x1x20) — 64x64 텍스처에 맞도록 16x1x16으로 줄여 그린다
    lk = box(img, 0, 0, 16, 1, 16, water, 6, top=water_hi)
    lkx, lky = lk["front"]
    for x in range(16):
        px(img, lkx + x, lky, water_dark)
    ltx, lty = lk["top"]
    for x in range(16):
        for y in range(16):
            if (x * 3 + y * 5) % 17 == 0:
                px(img, ltx + x, lty + y, water_hi)
            elif (x + y * 2) % 23 == 0:
                px(img, ltx + x, lty + y, water_dark)
    for side in ("right", "left", "back"):
        sx, sy = lk[side]
        for x in range(16):
            px(img, sx + x, sy, water_dark)

    # 백조 몸통 (u0 v24, 3x2x4)
    sw = box(img, 0, 24, 3, 2, 4, white, 4, top=hexc("#ffffff"))
    swx, swy = sw["front"]
    for x in range(3):
        px(img, swx + x, swy + 1, white_shade)
    # 목 (u14 v24, 1x3x1)
    nk = box(img, 14, 24, 1, 3, 1, white, 4)
    nkx, nky = nk["front"]
    for y in range(3):
        px(img, nkx, nky + y, white_shade if y % 2 == 0 else white)
    # 머리 (u18 v24, 2x1x2)
    hd = box(img, 18, 24, 2, 1, 2, white, 4, top=hexc("#ffffff"))
    hdx, hdy = hd["front"]
    px(img, hdx, hdy, white_shade)
    # 부리 (u26 v24, 1x1x1)
    bk = box(img, 26, 24, 1, 1, 1, beak, 0)
    bkx, bky = bk["front"]
    px(img, bkx, bky, hexc("#b07a1a"))

    # 검게 물든 백조용 어두운 판 (u28 v24, 3x2x4 / u28 v32, 1x3x1 / u28 v38, 2x1x2)
    bs = box(img, 28, 24, 3, 2, 4, black, 4, top=black_hi)
    bsx, bsy = bs["front"]
    for x in range(3):
        px(img, bsx + x, bsy + 1, black_hi)
    bn = box(img, 28, 32, 1, 3, 1, black, 4)
    bnx, bny = bn["front"]
    for y in range(3):
        px(img, bnx, bny + y, black_hi if y % 2 == 0 else black)
    bh = box(img, 28, 38, 2, 1, 2, black, 4, top=black_hi)
    bhx, bhy = bh["front"]
    px(img, bhx, bhy, black_hi)

    # 드레스: 검은 깃털 (u0 v32, 8x10x8)
    g = box(img, 0, 32, 8, 10, 8, feather, 4, top=feather_hi)
    gx, gy = g["front"]
    # 깃털 결: 아래로 흐르는 가는 선
    for y in range(10):
        for x in range(8):
            if (x * 3 + y * 5) % 11 == 0:
                px(img, gx + x, gy + y, feather_hi)
            elif (x + y) % 7 == 0:
                px(img, gx + x, gy + y, feather_dark)
    for x in range(8):
        px(img, gx + x, gy + 9, feather_dark)
    for side in ("right", "left", "back"):
        sx, sy = g[side]
        for y in range(10):
            for x in range(8):
                if (x * 5 + y * 3) % 13 == 0:
                    px(img, sx + x, sy + y, feather_hi)
                elif (x + y) % 9 == 0:
                    px(img, sx + x, sy + y, feather_dark)
        for x in range(8):
            px(img, sx + x, sy + 9, feather_dark)
    tx, ty = g["top"]
    for x in range(8):
        for y in range(8):
            if (x + y) % 5 == 0:
                px(img, tx + x, ty + y, feather_hi)

    # 몸통 (u32 v32, 5x6x3)
    b = box(img, 32, 32, 5, 6, 3, feather, 4, top=feather_hi)
    bx, by = b["front"]
    for y in range(6):
        px(img, bx + 2, by + y, feather_dark)
    for x in range(5):
        px(img, bx + x, by + 5, feather_dark)

    # 팔 (u48 v32, 2x7x2)
    a = box(img, 48, 32, 2, 7, 2, feather, 4, top=feather_hi)
    ax, ay = a["front"]
    for y in range(7):
        px(img, ax, ay + y, feather_dark if y % 3 == 0 else feather)
    for x in range(2):
        px(img, ax + x, ay + 6, skin)

    # 머리: 창백한 얼굴 (u0 v50, 5x5x5)
    h = box(img, 0, 50, 5, 5, 5, skin, 3, top=crown)
    hx, hy = h["front"]
    # 눈
    px(img, hx + 1, hy + 2, eye)
    px(img, hx + 3, hy + 2, eye)
    px(img, hx + 1, hy + 1, skin_shade)
    px(img, hx + 3, hy + 1, skin_shade)
    # 입
    px(img, hx + 2, hy + 4, hexc("#a07a80"))
    # 앞머리
    for x in range(5):
        px(img, hx + x, hy, crown)
    for x in (0, 2, 4):
        px(img, hx + x, hy + 1, crown_hi)
    for side in ("right", "left"):
        sx, sy = h[side]
        for y in range(5):
            px(img, sx + 2, sy + y, crown if y < 3 else crown_hi)

    # 깃털 왕관 (u20 v50, 6x2x6)
    cr = box(img, 20, 50, 6, 2, 6, crown, 4, top=crown_hi)
    crx, cry = cr["front"]
    for x in range(6):
        px(img, crx + x, cry + 1, crown_hi)
    for (x, y) in [(1, 0), (4, 0)]:
        px(img, crx + x, cry + y, hexc("#3a3a48"))
    ctx, cty = cr["top"]
    for x in range(6):
        for y in range(6):
            if (x + y) % 4 == 0:
                px(img, ctx + x, cty + y, crown_hi)

    # 양산 자루 (u40 v50, 1x10x1)
    st = box(img, 40, 50, 1, 10, 1, stick, 4)
    stx, sty = st["front"]
    for y in range(10):
        px(img, stx, sty + y, hexc("#3a3a44") if y % 3 == 0 else stick)

    # 양산 우산 (u0 v0, 10x2x10) — 호수 판은 16x1x16이라 u0 v0의 16x16 영역만 쓴다.
    # 우산은 그 아래 v16부터 시작해 겹치지 않게 한다.
    cp = box(img, 0, 16, 10, 2, 10, canopy, 4, top=canopy_hi)
    cpx, cpy = cp["front"]
    for x in range(10):
        px(img, cpx + x, cpy + 1, canopy_dark)
    ctx2, cty2 = cp["top"]
    # 우산살: 중심에서 뻗은 검은 선
    for x in range(10):
        for y in range(10):
            if x == 5 or y == 5 or abs(x - 5) == abs(y - 5):
                px(img, ctx2 + x, cty2 + y, canopy_dark)
            elif (x + y) % 7 == 0:
                px(img, ctx2 + x, cty2 + y, canopy_hi)
    for side in ("right", "left", "back"):
        sx, sy = cp[side]
        for x in range(10):
            px(img, sx + x, sy, canopy_hi if x % 3 == 0 else canopy)
            px(img, sx + x, sy + 1, canopy_dark)

    # 검은 날개 (u0 v30, 10x12x1) — 변이 시 펼친다
    w = box(img, 0, 30, 10, 12, 1, wing, 4, top=wing_hi)
    wx, wy = w["front"]
    for y in range(12):
        for x in range(10):
            if (x * 3 + y * 5) % 9 == 0:
                px(img, wx + x, wy + y, wing_hi)
            elif (x + y) % 6 == 0:
                px(img, wx + x, wy + y, hexc("#05050a"))
    for x in range(10):
        px(img, wx + x, wy + 11, hexc("#05050a"))

    save(img, "textures/entity/black_swan.png")


BS = {"k": "#141418", "h": "#22222a", "d": "#0a0a0e", "w": "#f4f4f4", "s": "#1a1a20", "p": "#f0e8e4"}
icon([
    "................",
    "..............kk",
    ".............kk.",
    "............kk..",
    "...........kk...",
    "..........kk....",
    ".........kk.....",
    "........kk......",
    ".......kk.......",
    "......kk........",
    ".....kk.........",
    "....kk..........",
    "...dd...........",
    "..dd............",
    "..d.............",
    "................",
], BS, "textures/item/black_swan.png")
icon([
    "................",
    "...kk......kk...",
    "..kkkk....kkkk..",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "....kkwwwwkk....",
    "....kkwwwwkk....",
    "....kkwwwwkk....",
    "....kkwwwwkk....",
    "....kkkkkkkk....",
    "....kkhhhhkk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....dddddd.....",
    "................",
    "................",
], BS, "textures/item/black_swan_suit.png")

armor(hexc("#141418"), "#0a0a0e", "#f4f4f4", "textures/entity/equipment/humanoid/black_swan.png")

for item, parent in [("black_swan", "minecraft:item/handheld"), ("black_swan_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/black_swan.json", {"layers": {"humanoid": [{"texture": "lobcorp:black_swan"}]}})

black_swan()
print("assets swan ok")

merge_lang({
    "entity.lobcorp.black_swan": "흑조의 꿈",
    "item.lobcorp.black_swan": "흑조",
    "item.lobcorp.black_swan_suit": "흑조의 드레스",
}, {
    "entity.lobcorp.black_swan": "Dream of a Black Swan",
    "item.lobcorp.black_swan": "Black Swan",
    "item.lobcorp.black_swan_suit": "Black Swan Dress",
})


# ───────────── 음 (O-05-102) ─────────────
def yin():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    scale = hexc("#0c0c12")
    scale_hi = hexc("#1e1e2c")
    scale_dark = hexc("#05050a")
    fin = hexc("#141420")
    fin_hi = hexc("#26263a")
    eye_white = hexc("#e8e8f0")
    eye_iris = hexc("#8a2a2a")
    pendant = hexc("#101018")
    pendant_hi = hexc("#2a2a3a")

    # 몸통 (u0 v0, 6x6x14)
    b = box(img, 0, 0, 6, 6, 14, scale, 5, top=scale_hi)
    bx, by = b["front"]
    for y in range(6):
        for x in range(6):
            if (x * 3 + y * 5) % 7 == 0:
                px(img, bx + x, by + y, scale_hi)
            elif (x + y) % 5 == 0:
                px(img, bx + x, by + y, scale_dark)
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for y in range(6):
            for x in range(14):
                if (x * 5 + y * 3) % 9 == 0:
                    px(img, sx + x, sy + y, scale_hi)
                elif (x + y) % 7 == 0:
                    px(img, sx + x, sy + y, scale_dark)

    # 꼬리지느러미 (u0 v20, 8x8x1)
    t = box(img, 0, 20, 8, 8, 1, fin, 5, top=fin_hi)
    tx, ty = t["front"]
    for y in range(8):
        for x in range(8):
            if abs(x - 3.5) + abs(y - 3.5) > 5:
                px(img, tx + x, ty + y, (0, 0, 0, 0))
            elif (x + y) % 4 == 0:
                px(img, tx + x, ty + y, fin_hi)

    # 등·배지느러미 (u20 v20, 1x4x8)
    f = box(img, 20, 20, 1, 4, 8, fin, 5, top=fin_hi)
    fx, fy = f["front"]
    for y in range(4):
        px(img, fx, fy + y, fin_hi if y % 2 == 0 else fin)

    # 하나뿐인 눈 (u40 v0, 3x3x1)
    e = box(img, 40, 0, 3, 3, 1, eye_white, 0)
    ex, ey = e["front"]
    px(img, ex + 1, ey + 1, eye_iris)
    px(img, ex + 1, ey, eye_white)

    # 조각: 태극의 검은 반쪽 (u40 v8, 4x4x1)
    p = box(img, 40, 8, 4, 4, 1, pendant, 4, top=pendant_hi)
    pfx, pfy = p["front"]
    for y in range(4):
        for x in range(4):
            if (x - 1.5) ** 2 + (y - 1.5) ** 2 > 4:
                px(img, pfx + x, pfy + y, (0, 0, 0, 0))
            elif x + y < 3:
                px(img, pfx + x, pfy + y, pendant_hi)

    save(img, "textures/entity/yin.png")


YD = {"k": "#0c0c12", "h": "#1e1e2c", "d": "#05050a", "r": "#8a2a2a", "w": "#e8e8f0"}
icon([
    "................",
    "................",
    "......kkkk......",
    ".....kkkkkk.....",
    "....kkkkkkkk....",
    "...kkkkkkkkkk...",
    "...kkkkkkkkkk...",
    "...kkkkkkkkkk...",
    "...kkkkkkkkkk...",
    "....kkkkkkkk....",
    ".....kkkkkk.....",
    "......kkkk......",
    ".......kk.......",
    "................",
    "................",
    "................",
], YD, "textures/item/discord.png")
icon([
    "................",
    "...kk......kk...",
    "..kkkk....kkkk..",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "....kkhhhhkk....",
    "....kkhhhhkk....",
    "....kkhhhhkk....",
    "....kkhhhhkk....",
    "....kkkkkkkk....",
    "....kkddddkk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....dddddd.....",
    "................",
    "................",
], YD, "textures/item/discord_suit.png")

armor(hexc("#0c0c12"), "#05050a", "#e8e8f0", "textures/entity/equipment/humanoid/discord.png")

for item, parent in [("discord", "minecraft:item/handheld"), ("discord_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/discord.json", {"layers": {"humanoid": [{"texture": "lobcorp:discord"}]}})

yin()
print("assets yin ok")

merge_lang({
    "entity.lobcorp.yin": "음",
    "item.lobcorp.discord": "불화",
    "item.lobcorp.discord_suit": "불화의 옷",
}, {
    "entity.lobcorp.yin": "Yin",
    "item.lobcorp.discord": "Discord",
    "item.lobcorp.discord_suit": "Discord Suit",
})


# ───────────── 양 (O-07-103) ─────────────
def yang():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    scale = hexc("#f2f2f6")
    scale_hi = hexc("#ffffff")
    scale_dark = hexc("#d0d0da")
    fin = hexc("#e4e4ec")
    fin_hi = hexc("#ffffff")
    eye_white = hexc("#f8f8ff")
    eye_iris = hexc("#2a4a8a")
    pendant = hexc("#f4f4f8")
    pendant_hi = hexc("#ffffff")

    # 몸통 (u0 v0, 6x6x14)
    b = box(img, 0, 0, 6, 6, 14, scale, 4, top=scale_hi)
    bx, by = b["front"]
    for y in range(6):
        for x in range(6):
            if (x * 3 + y * 5) % 7 == 0:
                px(img, bx + x, by + y, scale_dark)
    for side in ("right", "left", "back"):
        sx, sy = b[side]
        for y in range(6):
            for x in range(14):
                if (x * 5 + y * 3) % 9 == 0:
                    px(img, sx + x, sy + y, scale_dark)

    # 꼬리지느러미 (u0 v20, 8x8x1)
    t = box(img, 0, 20, 8, 8, 1, fin, 4, top=fin_hi)
    tx, ty = t["front"]
    for y in range(8):
        for x in range(8):
            if abs(x - 3.5) + abs(y - 3.5) > 5:
                px(img, tx + x, ty + y, (0, 0, 0, 0))
            elif (x + y) % 4 == 0:
                px(img, tx + x, ty + y, scale_dark)

    # 등·배지느러미 (u20 v20, 1x4x8)
    f = box(img, 20, 20, 1, 4, 8, fin, 4, top=fin_hi)
    fx, fy = f["front"]
    for y in range(4):
        px(img, fx, fy + y, scale_dark if y % 2 == 0 else fin)

    # 하나뿐인 눈 (u40 v0, 3x3x1)
    e = box(img, 40, 0, 3, 3, 1, eye_white, 0)
    ex, ey = e["front"]
    px(img, ex + 1, ey + 1, eye_iris)
    px(img, ex + 1, ey, eye_white)

    # 조각: 태극의 흰 반쪽 (u40 v8, 4x4x1)
    p = box(img, 40, 8, 4, 4, 1, pendant, 3, top=pendant_hi)
    pfx, pfy = p["front"]
    for y in range(4):
        for x in range(4):
            if (x - 1.5) ** 2 + (y - 1.5) ** 2 > 4:
                px(img, pfx + x, pfy + y, (0, 0, 0, 0))
            elif x + y > 4:
                px(img, pfx + x, pfy + y, scale_dark)

    save(img, "textures/entity/yang.png")


yang()
print("assets yang ok")

merge_lang({
    "entity.lobcorp.yang": "양",
}, {
    "entity.lobcorp.yang": "Yang",
})


# ───────────── 백설공주의 사과 (F-04-42) ─────────────
def snow_white_apple():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    red = hexc("#b81c28")
    red_hi = hexc("#e03440")
    red_dark = hexc("#7a0e18")
    flesh = hexc("#f4e8c8")
    flesh_dark = hexc("#d8c8a0")
    stem = hexc("#5a3a1a")
    leaf = hexc("#3a8a2a")
    leaf_hi = hexc("#5ab83a")
    vine = hexc("#2e6a22")
    vine_hi = hexc("#4a9a34")

    # 사과 (u0 v0, 14x14x14)
    a = box(img, 0, 0, 14, 14, 14, red, 6, top=red_hi)
    ax, ay = a["front"]
    for y in range(14):
        for x in range(14):
            if (x * 3 + y * 5) % 11 == 0:
                px(img, ax + x, ay + y, red_hi)
            elif (x + y * 2) % 13 == 0:
                px(img, ax + x, ay + y, red_dark)
    for side in ("right", "left", "back"):
        sx, sy = a[side]
        for y in range(14):
            for x in range(14):
                if (x * 5 + y * 3) % 12 == 0:
                    px(img, sx + x, sy + y, red_hi)
                elif (x + y) % 9 == 0:
                    px(img, sx + x, sy + y, red_dark)

    # 한 입 베어 문 자리 (u0 v32, 6x6x2)
    bt = box(img, 0, 32, 6, 6, 2, flesh, 4, top=flesh_dark)
    btx, bty = bt["front"]
    for y in range(6):
        for x in range(6):
            if (x - 2.5) ** 2 + (y - 2.5) ** 2 > 8:
                px(img, btx + x, bty + y, red_dark)
            elif (x + y) % 3 == 0:
                px(img, btx + x, bty + y, flesh_dark)

    # 꼭지 (u32 v32, 1x4x1)
    st = box(img, 32, 32, 1, 4, 1, stem, 4)
    stx, sty = st["front"]
    for y in range(4):
        px(img, stx, sty + y, hexc("#3a2410") if y % 2 == 0 else stem)

    # 잎 (u40 v32, 5x1x4)
    lf = box(img, 40, 32, 5, 1, 4, leaf, 5, top=leaf_hi)
    lfx, lfy = lf["front"]
    for x in range(5):
        px(img, lfx + x, lfy, leaf_hi if x % 2 == 0 else leaf)

    # 바닥으로 뻗은 덩굴 (u0 v48, 10x1x1)
    v = box(img, 0, 48, 10, 1, 1, vine, 5, top=vine_hi)
    vx, vy = v["front"]
    for x in range(10):
        px(img, vx + x, vy, vine_hi if x % 3 == 0 else vine)

    save(img, "textures/entity/snow_white_apple.png")


GS = {"g": "#2e6a22", "h": "#4a9a34", "r": "#b81c28", "d": "#7a0e18", "f": "#f4e8c8"}
icon([
    "................",
    ".......gg.......",
    "......ghhg......",
    ".......gg.......",
    "......gggg......",
    ".....gggggg.....",
    "....gggggggg....",
    "....gggggggg....",
    "....gggggggg....",
    "....gggggggg....",
    ".....gggggg.....",
    "......gggg......",
    ".......gg.......",
    "................",
    "................",
    "................",
], GS, "textures/item/green_stem.png")
icon([
    "................",
    "...gg......gg...",
    "..gggg....gggg..",
    "..gggggggggggg..",
    "...gggggggggg...",
    "....gghhhhgg....",
    "....gghhhhgg....",
    "....gghhhhgg....",
    "....gghhhhgg....",
    "....gggggggg....",
    "....ggddddgg....",
    "....gggggggg....",
    "....gggggggg....",
    ".....dddddd.....",
    "................",
    "................",
], GS, "textures/item/green_stem_suit.png")

armor(hexc("#2e6a22"), "#1a3a12", "#b81c28", "textures/entity/equipment/humanoid/green_stem.png")

for item, parent in [("green_stem", "minecraft:item/handheld"), ("green_stem_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/green_stem.json", {"layers": {"humanoid": [{"texture": "lobcorp:green_stem"}]}})

snow_white_apple()
print("assets apple ok")

merge_lang({
    "entity.lobcorp.snow_white_apple": "백설공주의 사과",
    "item.lobcorp.green_stem": "초록 줄기",
    "item.lobcorp.green_stem_suit": "초록 줄기의 옷",
}, {
    "entity.lobcorp.snow_white_apple": "Snow White's Apple",
    "item.lobcorp.green_stem": "Green Stem",
    "item.lobcorp.green_stem_suit": "Green Stem Suit",
})


# ───────────── 어린 왕자 ─────────────
def little_prince():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    cloak = hexc("#5a7a9a")
    cloak_hi = hexc("#7a9ab8")
    cloak_dark = hexc("#3a5470")
    cloak_trim = hexc("#9ec2e0")
    stem = hexc("#e8e2d0")
    stem_shade = hexc("#c8c0aa")
    cap = hexc("#e8f2fa")
    cap_hi = hexc("#f6fbff")
    gill = hexc("#b8c8d8")
    gill_dark = hexc("#8a9cb0")

    # 망토 상체 (u0 v0, 6x5x4)
    t = box(img, 0, 0, 6, 5, 4, cloak, 6, top=cloak_hi)
    tx, ty = t["front"]
    for y in range(5):
        px(img, tx + 2, ty + y, cloak_trim)
        px(img, tx + 3, ty + y, cloak_trim)
    for x in range(6):
        px(img, tx + x, ty + 4, cloak_dark)

    # 망토 허리 (u0 v12, 8x5x6)
    w = box(img, 0, 12, 8, 5, 6, cloak, 6, top=cloak_hi)
    wx, wy = w["front"]
    for y in range(5):
        px(img, wx + 3, wy + y, cloak_trim)
        px(img, wx + 4, wy + y, cloak_trim)
    for x in range(8):
        px(img, wx + x, wy + 4, cloak_dark)
    for side in ("right", "left", "back"):
        sx, sy = w[side]
        for x in range(6):
            px(img, sx + x, sy + 4, cloak_dark)

    # 망토 밑단 (u0 v26, 10x5x8)
    h = box(img, 0, 26, 10, 5, 8, cloak, 6, top=cloak_hi)
    hx, hy = h["front"]
    for y in range(5):
        px(img, hx + 4, hy + y, cloak_trim)
        px(img, hx + 5, hy + y, cloak_trim)
    for x in range(10):
        px(img, hx + x, hy + 4, cloak_dark)
    for side in ("right", "left", "back"):
        sx, sy = h[side]
        for x in range(8):
            px(img, sx + x, sy + 4, cloak_dark)

    # 목(버섯 대) (u32 v0, 3x3x3)
    s = box(img, 32, 0, 3, 3, 3, stem, 5, top=stem_shade)
    sx, sy = s["front"]
    for y in range(3):
        px(img, sx + 1, sy + y, stem_shade)

    # 버섯 갓 (u0 v40, 14x3x14)
    c = box(img, 0, 40, 14, 3, 14, cap, 5, top=cap_hi)
    cx, cy = c["front"]
    for x in range(14):
        px(img, cx + x, cy + 2, gill)
    for side in ("right", "left", "back"):
        sx, sy = c[side]
        for x in range(14):
            px(img, sx + x, sy + 2, gill)
    for (dx, dy) in [(3, 0), (9, 1), (6, 2)]:
        px(img, cx + dx, cy + dy, cap_hi)

    # 갓 주름 (u32 v12, 10x1x10)
    g = box(img, 32, 12, 10, 1, 10, gill, 5, top=gill_dark)
    gx, gy = g["front"]
    for x in range(10):
        if x % 2 == 0:
            px(img, gx + x, gy, gill_dark)

    # 갓 정수리 (u32 v24, 8x2x8)
    k = box(img, 32, 24, 8, 2, 8, cap, 5, top=cap_hi)
    kx, ky = k["front"]
    for x in range(8):
        px(img, kx + x, ky + 1, gill)

    save(img, "textures/entity/little_prince.png")


def little_prince_minion():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    skin = hexc("#7a8a9a")
    skin_hi = hexc("#94a4b4")
    skin_dark = hexc("#4e5a68")
    cloth = hexc("#5a7a9a")
    cloth_dark = hexc("#3a5470")
    mush = hexc("#e8f2fa")
    mush_hi = hexc("#f6fbff")
    mush_gill = hexc("#b8c8d8")
    eye = hexc("#1e2a36")

    # 몸통 (u0 v0, 6x8x4)
    b = box(img, 0, 0, 6, 8, 4, cloth, 6, top=skin_hi)
    bx, by = b["front"]
    for y in range(8):
        px(img, bx + 2, by + y, cloth_dark)
        px(img, bx + 3, by + y, cloth_dark)
    for x in range(6):
        px(img, bx + x, by + 7, cloth_dark)

    # 머리 (u0 v12, 6x6x6)
    h = box(img, 0, 12, 6, 6, 6, skin, 6, top=skin_hi)
    hx, hy = h["front"]
    for (ex, ey) in [(1, 2), (4, 2)]:
        px(img, hx + ex, hy + ey, eye)
    for x in range(6):
        px(img, hx + x, hy + 5, skin_dark)

    # 머리 위 버섯 갓 (u24 v12, 4x2x4)
    m = box(img, 24, 12, 4, 2, 4, mush, 5, top=mush_hi)
    mx, my = m["front"]
    for x in range(4):
        px(img, mx + x, my + 1, mush_gill)
    # 머리 위 버섯 대 (u24 v18, 2x1x2)
    box(img, 24, 18, 2, 1, 2, mush_gill, 4)

    # 등 버섯 1 (u24 v20, 4x2x3)
    m2 = box(img, 24, 20, 4, 2, 3, mush, 5, top=mush_hi)
    m2x, m2y = m2["front"]
    for x in range(4):
        px(img, m2x + x, m2y + 1, mush_gill)
    # 등 버섯 2 (u24 v25, 2x2x2)
    box(img, 24, 25, 2, 2, 2, mush, 5, top=mush_hi)

    # 긴 팔 (u0 v24, 2x10x2)
    a = box(img, 0, 24, 2, 10, 2, skin, 6, top=skin_hi)
    ax, ay = a["front"]
    for y in range(10):
        px(img, ax, ay + y, skin_dark)
    for x in range(2):
        px(img, ax + x, ay + 9, skin_dark)

    # 다리 (u12 v24, 2x8x2)
    l = box(img, 12, 24, 2, 8, 2, skin, 6, top=skin_hi)
    lx, ly = l["front"]
    for y in range(8):
        px(img, lx, ly + y, skin_dark)
    for x in range(2):
        px(img, lx + x, ly + 7, skin_dark)

    save(img, "textures/entity/little_prince_minion.png")


SP = {"k": "#1e2a36", "b": "#5a7a9a", "l": "#9ec2e0", "w": "#e8f2fa"}
icon([
    "................",
    "............ww..",
    "...........wwl..",
    "..........wwl...",
    ".........wwl....",
    "........wwl.....",
    ".......wwl......",
    "......wwl.......",
    ".....wwl........",
    "....wwl.........",
    "...wwl..........",
    "..bwl...........",
    ".bbk............",
    ".bk.............",
    "................",
    "................",
], SP, "textures/item/spore.png")
icon([
    "................",
    "...bb......bb...",
    "..bbbb....bbbb..",
    "..bbbbbbbbbbbb..",
    "...bbbbbbbbbb...",
    "....bbllllbb....",
    "....bbllllbb....",
    "....bbllllbb....",
    "....bbllllbb....",
    "....bbbbbbbb....",
    "....bbkkkkbb....",
    "....bbbbbbbb....",
    "....bbbbbbbb....",
    ".....kkkkkk.....",
    "................",
    "................",
], SP, "textures/item/spore_suit.png")

armor(hexc("#5a7a9a"), "#1e2a36", "#9ec2e0", "textures/entity/equipment/humanoid/spore.png")

for item, parent in [("spore", "minecraft:item/handheld"), ("spore_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/spore.json", {"layers": {"humanoid": [{"texture": "lobcorp:spore"}]}})

little_prince()
little_prince_minion()
print("assets little prince ok")

merge_lang({
    "entity.lobcorp.little_prince": "어린 왕자",
    "entity.lobcorp.little_prince_minion": "어린 왕자-1",
    "item.lobcorp.spore": "포자",
    "item.lobcorp.spore_suit": "포자의 망토",
}, {
    "entity.lobcorp.little_prince": "The Little Prince",
    "entity.lobcorp.little_prince_minion": "Little Prince-1",
    "item.lobcorp.spore": "Spore",
    "item.lobcorp.spore_suit": "Spore Cloak",
})


# ───────────── 기생 나무 / 묘목 ─────────────
def parasite_tree():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    bark = hexc("#c8c0b0")
    bark_hi = hexc("#e0d8c8")
    bark_dark = hexc("#9a9080")
    bulb = hexc("#f2efe6")
    bulb_hi = hexc("#ffffff")
    bulb_dark = hexc("#d8d2c4")
    bone = hexc("#e8e2d0")
    bone_hi = hexc("#f6f2e6")
    hole = hexc("#2a2420")

    # 밑동 (u0 v0, 8x16x8)
    t = box(img, 0, 0, 8, 16, 8, bark, 7, top=bark_hi)
    tx, ty = t["front"]
    for y in range(16):
        px(img, tx + 2, ty + y, bark_dark)
        px(img, tx + 5, ty + y, bark_dark)
    for x in range(8):
        px(img, tx + x, ty + 15, bark_dark)

    # 상체 (u0 v24, 6x14x6)
    u = box(img, 0, 24, 6, 14, 6, bark, 7, top=bark_hi)
    ux, uy = u["front"]
    for y in range(14):
        px(img, ux + 1, uy + y, bark_dark)
        px(img, ux + 4, uy + y, bark_dark)
    for x in range(6):
        px(img, ux + x, uy + 13, bark_dark)

    # 가지: 오른쪽 (u24 v0, 10x2x2)
    b = box(img, 24, 0, 10, 2, 2, bark, 7, top=bark_hi)
    bx, by = b["front"]
    for x in range(10):
        px(img, bx + x, by + 1, bark_dark)
    # 가지: 왼쪽 (u24 v4, 10x2x2)
    b2 = box(img, 24, 4, 10, 2, 2, bark, 7, top=bark_hi)
    b2x, b2y = b2["front"]
    for x in range(10):
        px(img, b2x + x, b2y + 1, bark_dark)
    # 가지: 뒤쪽 (u24 v8, 2x2x8)
    box(img, 24, 8, 2, 2, 8, bark, 7, top=bark_hi)

    # 봉오리 (u24 v20, 3x3x3)
    for i in range(5):
        p = box(img, 24, 20, 3, 3, 3, bulb, 5, top=bulb_hi)
        px_, py_ = p["front"]
        px(img, px_ + 1, py_ + 1, bulb_hi)
        for x in range(3):
            px(img, px_ + x, py_ + 2, bulb_dark)

    # 해골 (u40 v20, 4x4x4)
    for i in range(5):
        s = box(img, 40, 20, 4, 4, 4, bone, 5, top=bone_hi)
        sx, sy = s["front"]
        for x in range(4):
            px(img, sx + x, sy + 3, bone_hi)
        # 눈구멍 (u40 v30, 1x1x1 두 개)
        e = box(img, 40, 30, 1, 1, 1, hole, 0)
        ex, ey = e["front"]
        px(img, ex, ey, hole)
        e2 = box(img, 42, 30, 1, 1, 1, hole, 0)
        e2x, e2y = e2["front"]
        px(img, e2x, e2y, hole)

    save(img, "textures/entity/parasite_tree.png")


def sapling():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    flesh = hexc("#8a7a68")
    flesh_hi = hexc("#a89880")
    flesh_dark = hexc("#5e5248")
    bark = hexc("#6a4a30")
    bark_hi = hexc("#8a6440")
    bark_dark = hexc("#4a3220")
    leaf = hexc("#6a8a4a")
    leaf_hi = hexc("#8aa864")
    leaf_dark = hexc("#3e5a2a")
    root = hexc("#d8d2c4")

    # 웅크린 몸 (u0 v0, 8x6x6)
    b = box(img, 0, 0, 8, 6, 6, flesh, 7, top=flesh_hi)
    bx, by = b["front"]
    for y in range(6):
        px(img, bx + 3, by + y, flesh_dark)
        px(img, bx + 4, by + y, flesh_dark)
    for x in range(8):
        px(img, bx + x, by + 5, flesh_dark)

    # 굽은 등 (u0 v12, 6x5x6)
    h = box(img, 0, 12, 6, 5, 6, flesh, 7, top=flesh_hi)
    hx, hy = h["front"]
    for x in range(6):
        px(img, hx + x, hy + 4, flesh_dark)

    # 뿌리 (u28 v0 / v6 / v10)
    box(img, 28, 0, 2, 1, 5, root, 5)
    box(img, 28, 6, 5, 1, 2, root, 5)
    box(img, 28, 10, 5, 1, 2, root, 5)

    # 줄기 (u0 v24, 3x12x3)
    t = box(img, 0, 24, 3, 12, 3, bark, 7, top=bark_hi)
    tx, ty = t["front"]
    for y in range(12):
        px(img, tx + 1, ty + y, bark_dark)

    # 잎: 정수리 (u12 v24, 6x3x6)
    c = box(img, 12, 24, 6, 3, 6, leaf, 7, top=leaf_hi)
    cx, cy = c["front"]
    for x in range(6):
        px(img, cx + x, cy + 2, leaf_dark)
    # 잎: 오른쪽 (u36 v24, 5x2x4)
    r = box(img, 36, 24, 5, 2, 4, leaf, 7, top=leaf_hi)
    rx, ry = r["front"]
    for x in range(5):
        px(img, rx + x, ry + 1, leaf_dark)
    # 잎: 왼쪽 (u36 v30, 5x2x4)
    l = box(img, 36, 30, 5, 2, 4, leaf, 7, top=leaf_hi)
    lx, ly = l["front"]
    for x in range(5):
        px(img, lx + x, ly + 1, leaf_dark)
    # 잎: 뒤쪽 (u36 v36, 4x2x5)
    box(img, 36, 36, 4, 2, 5, leaf, 7, top=leaf_hi)

    save(img, "textures/entity/sapling.png")


HY = {"k": "#2a2420", "b": "#8a7a68", "w": "#f2efe6", "g": "#6a8a4a"}
# 석궁: 가로로 뻗은 활과 세로 몸통, 하얀 볼트
icon([
    "................",
    "................",
    "..bb........bb..",
    "..bbb......bbb..",
    "..bbbb....bbbb..",
    "..bbbbbbbbbbbb..",
    "..bbbbbbbbbbbb..",
    "......bbbb......",
    "......bbbb......",
    "......bbbb......",
    "......bbbb......",
    "......bbbb......",
    "......bbbb......",
    "......bbbb......",
    "......kkkk......",
    "................",
], HY, "textures/item/hypocrisy.png")
# 외투: 어깨에서 퍼지는 코트, 가슴에 하얀 꽃
icon([
    "................",
    "................",
    ".....bbbbbb.....",
    "....bbbbbbbb....",
    "...bbbbbbbbbb...",
    "..bbbbbbbbbbbb..",
    "..bbbbbbbbbbbb..",
    "..bbbbbbbbbbbb..",
    "..bbbbbbbbbbbb..",
    "..bbbbbbbbbbbb..",
    "..bbbbbbbbbbbb..",
    "..bbbbbbbbbbbb..",
    "..bbbbbbbbbbbb..",
    "..bbbbbbbbbbbb..",
    "..kkkkkkkkkkkk..",
    "................",
], HY, "textures/item/hypocrisy_suit.png")

armor(hexc("#d8d2c4"), "#2a2420", "#6a8a4a", "textures/entity/equipment/humanoid/hypocrisy.png")

for item, parent in [("hypocrisy", "minecraft:item/handheld"), ("hypocrisy_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/hypocrisy.json", {"layers": {"humanoid": [{"texture": "lobcorp:hypocrisy"}]}})

parasite_tree()
sapling()
print("assets parasite tree ok")

merge_lang({
    "entity.lobcorp.parasite_tree": "기생 나무",
    "entity.lobcorp.sapling": "묘목",
    "item.lobcorp.hypocrisy": "위선",
    "item.lobcorp.hypocrisy_suit": "위선의 외투",
}, {
    "entity.lobcorp.parasite_tree": "Parasite Tree",
    "entity.lobcorp.sapling": "Sapling",
    "item.lobcorp.hypocrisy": "Hypocrisy",
    "item.lobcorp.hypocrisy_suit": "Hypocrisy Coat",
})


# ───────────── 벌거벗은 둥지 / 탈피 ─────────────
def naked_nest():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    flesh = hexc("#a83a44")
    flesh_hi = hexc("#c85a60")
    flesh_dark = hexc("#6e2028")
    hole = hexc("#2a1418")
    pale = hexc("#e0a0a0")
    pale_hi = hexc("#f2e6e0")

    # 본체 (u0 v0, 20x8x20)
    m = box(img, 0, 0, 20, 8, 20, flesh, 8, top=flesh_hi)
    mx, my = m["front"]
    for y in range(8):
        px(img, mx + 6, my + y, flesh_dark)
        px(img, mx + 13, my + y, flesh_dark)
    for x in range(20):
        px(img, mx + x, my + 7, flesh_dark)

    # 위 혹 (u0 v30, 14x6x14)
    l = box(img, 0, 30, 14, 6, 14, flesh, 8, top=flesh_hi)
    lx, ly = l["front"]
    for x in range(14):
        px(img, lx + x, ly + 5, flesh_dark)
    for y in range(6):
        px(img, lx + 4, ly + y, flesh_dark)

    # 옆 혹 (u0 v52, 10x4x10)
    l2 = box(img, 0, 52, 10, 4, 10, flesh, 8, top=flesh_hi)
    l2x, l2y = l2["front"]
    for x in range(10):
        px(img, l2x + x, l2y + 3, flesh_dark)

    # 뒤 혹 (u44 v0, 8x3x8)
    l3 = box(img, 44, 0, 8, 3, 8, flesh, 8, top=flesh_hi)
    l3x, l3y = l3["front"]
    for x in range(8):
        px(img, l3x + x, l3y + 2, flesh_dark)

    # 구멍 (u44 v14, 4x1x4) — 어둡게
    for i in range(4):
        h = box(img, 44, 14, 4, 1, 4, hole, 3)
        hx, hy = h["front"]
        for x in range(4):
            px(img, hx + x, hy, hole)

    # 뱀 (u44 v20, 2x14x2) — 창백한 살
    for i in range(5):
        s = box(img, 44, 20, 2, 14, 2, pale, 6, top=pale_hi)
        sx, sy = s["front"]
        for y in range(14):
            px(img, sx, sy + y, pale_hi)
        for x in range(2):
            px(img, sx + x, sy, hole)

    save(img, "textures/entity/naked_nest.png")


def naked_worm():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    skin = hexc("#f2e6e0")
    skin_hi = hexc("#ffffff")
    skin_dark = hexc("#d0b0ac")
    eye = hexc("#2a1418")

    # 머리 (u0 v0, 4x4x4)
    h = box(img, 0, 0, 4, 4, 4, skin, 5, top=skin_hi)
    hx, hy = h["front"]
    for (ex, ey) in [(0, 1), (3, 1)]:
        px(img, hx + ex, hy + ey, eye)
    for x in range(4):
        px(img, hx + x, hy + 3, skin_dark)

    # 마디 1 (u0 v10, 4x4x5)
    s1 = box(img, 0, 10, 4, 4, 5, skin, 5, top=skin_hi)
    s1x, s1y = s1["front"]
    for x in range(4):
        px(img, s1x + x, s1y + 3, skin_dark)
    # 마디 2 (u0 v20, 3x3x5)
    s2 = box(img, 0, 20, 3, 3, 5, skin, 5, top=skin_hi)
    s2x, s2y = s2["front"]
    for x in range(3):
        px(img, s2x + x, s2y + 2, skin_dark)
    # 마디 3 (u0 v30, 2x2x5)
    s3 = box(img, 0, 30, 2, 2, 5, skin, 5, top=skin_hi)
    s3x, s3y = s3["front"]
    for x in range(2):
        px(img, s3x + x, s3y + 1, skin_dark)

    save(img, "textures/entity/naked_worm.png")


EX = {"k": "#2a1418", "r": "#a83a44", "p": "#e0a0a0", "w": "#f2e6e0"}
# 탈피: 오른쪽 위를 향한 두툼한 대포 포신, 살빛 붕대를 감았다
icon([
    "................",
    "............rr..",
    "...........rrr..",
    "..........rrrr..",
    ".........rrrr...",
    "........rrrr....",
    ".......rrrr.....",
    "......rrrr......",
    ".....rrrr.......",
    "....rrrr........",
    "...rrrr.........",
    "..rrrr..........",
    "..ppp...........",
    "..pp............",
    "..k.............",
    "................",
], EX, "textures/item/exuviae.png")
# 외투: 살덩이로 만든 코트
icon([
    "................",
    "...rr......rr...",
    "..rrrr....rrrr..",
    "..rrrrrrrrrrrr..",
    "...rrrrrrrrrr...",
    "....rrpppprr....",
    "....rrpwwprr....",
    "....rrpwwprr....",
    "....rrpppprr....",
    "....rrrrrrrr....",
    "....rrrrrrrr....",
    "....rrrrrrrr....",
    "....rrrrrrrr....",
    ".....kkkkkk.....",
    "................",
    "................",
], EX, "textures/item/exuviae_suit.png")

armor(hexc("#a83a44"), "#2a1418", "#e0a0a0", "textures/entity/equipment/humanoid/exuviae.png")

for item, parent in [("exuviae", "minecraft:item/handheld"), ("exuviae_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/exuviae.json", {"layers": {"humanoid": [{"texture": "lobcorp:exuviae"}]}})

naked_nest()
naked_worm()
print("assets naked nest ok")

merge_lang({"entity.lobcorp.naked_nest": "벌거벗은 둥지", "entity.lobcorp.naked_worm": "벌거벗은 둥지의 뱀", "item.lobcorp.exuviae": "탈피", "item.lobcorp.exuviae_suit": "탈피의 외투"},
           {"entity.lobcorp.naked_nest": "The Naked Nest", "entity.lobcorp.naked_worm": "Naked Nest Serpent", "item.lobcorp.exuviae": "Exuviae", "item.lobcorp.exuviae_suit": "Exuviae Coat"})


# ───────────── 꿈꾸는 조류 / 황홀 ─────────────
def dreaming_current():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    red = hexc("#e05a6a")
    orange = hexc("#e08a4a")
    yellow = hexc("#f2d65a")
    green = hexc("#6ad08a")
    blue = hexc("#5a9ae0")
    violet = hexc("#a06ae0")
    mouth = hexc("#2a1a2a")
    eye = hexc("#1a1a2a")

    # 위턱 (u0 v0, 6x3x7) — 붉은 줄무늬
    h = box(img, 0, 0, 6, 3, 7, red, 6, top=orange)
    hx, hy = h["front"]
    for x in range(6):
        px(img, hx + x, hy + 2, mouth)
    for (ex, ey) in [(1, 0), (4, 0)]:
        px(img, hx + ex, hy + ey, eye)

    # 아래턱 (u0 v10, 6x2x7) — 입 안쪽은 어둡게
    j = box(img, 0, 10, 6, 2, 7, orange, 6, top=mouth)
    jx, jy = j["front"]
    for x in range(6):
        px(img, jx + x, jy, mouth)

    # 몸통 마디 3개 (u0 v20 / u0 v32 / u0 v44, 각 6x5x7) — 무지개 줄무늬
    for (u, v, c, hi) in [(0, 20, yellow, green), (0, 32, green, blue), (0, 44, blue, violet)]:
        s = box(img, u, v, 6, 5, 7, c, 6, top=hi)
        sx, sy = s["front"]
        for y in range(5):
            px(img, sx, sy + y, hi)
            px(img, sx + 5, sy + y, hi)

    # 꼬리지느러미 (u26 v0, 1x6x6) — 보랏빛
    t = box(img, 26, 0, 1, 6, 6, violet, 6, top=red)
    tx, ty = t["front"]
    for y in range(6):
        px(img, tx, ty + y, red)

    # 옆지느러미 (u26 v14 / u26 v24, 1x3x5) — 청록
    box(img, 26, 14, 1, 3, 5, blue, 6, top=green)
    box(img, 26, 24, 1, 3, 5, blue, 6, top=green)

    save(img, "textures/entity/dreaming_current.png")


EC = {"k": "#1a1a2a", "r": "#e05a6a", "y": "#f2d65a", "g": "#6ad08a", "b": "#5a9ae0", "v": "#a06ae0", "w": "#f4f4fa"}
# 황홀: 무지개빛 리본 채찍이 대각선으로 휜다
icon([
    "................",
    "...........rr...",
    "..........rrr...",
    ".........rrr....",
    "........yyy.....",
    ".......yyy......",
    "......ggg.......",
    ".....ggg........",
    "....bbb.........",
    "...bbb..........",
    "..vvv...........",
    "..vv............",
    ".kk.............",
    "................",
    "................",
    "................",
], EC, "textures/item/ecstasy.png")
# 황홀의 옷: 무지개 테두리를 두른 코트
icon([
    "................",
    "...bb......bb...",
    "..bbbb....bbbb..",
    "..bbbbbbbbbbbb..",
    "...bbbbbbbbbb...",
    "....bbwwwwbb....",
    "....bbwyywbb....",
    "....bbwyywbb....",
    "....bbwwwwbb....",
    "....bbbbbbbb....",
    "....bbbbbbbb....",
    "....bbbbbbbb....",
    "....bbbbbbbb....",
    ".....vvvvvv.....",
    "................",
    "................",
], EC, "textures/item/ecstasy_suit.png")

armor(hexc("#5a9ae0"), "#1a1a2a", "#f2d65a", "textures/entity/equipment/humanoid/ecstasy.png")

for item, parent in [("ecstasy", "minecraft:item/handheld"), ("ecstasy_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/ecstasy.json", {"layers": {"humanoid": [{"texture": "lobcorp:ecstasy"}]}})

dreaming_current()
print("assets dreaming current ok")

merge_lang({"entity.lobcorp.dreaming_current": "꿈꾸는 조류", "item.lobcorp.ecstasy": "황홀", "item.lobcorp.ecstasy_suit": "황홀의 옷"},
           {"entity.lobcorp.dreaming_current": "The Dreaming Current", "item.lobcorp.ecstasy": "Ecstasy", "item.lobcorp.ecstasy_suit": "Ecstasy Suit"})


# ───────────── 땅을 파는 천국 ─────────────
def burrowing_heaven():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    bark = hexc("#efeae0")
    bark_hi = hexc("#f7f3ea")
    bark_lo = hexc("#c9c2b4")
    root = hexc("#3a2a24")
    root_hi = hexc("#5a4438")
    knot = hexc("#8a847a")
    eye_w = hexc("#f4f0e6")
    vein = hexc("#b0202a")
    pupil = hexc("#1a1414")

    # 몸통 마디 3개 (u0 v0 / u0 v16 / u0 v32, 각 4x10x4) — 창백한 나무껍질
    for (u, v) in [(0, 0), (0, 16), (0, 32)]:
        t = box(img, u, v, 4, 10, 4, bark, 6, top=bark_hi)
        tx, ty = t["front"]
        for y in range(10):
            px(img, tx, ty + y, bark_lo)
            px(img, tx + 3, ty + y, bark_lo)
        for y in range(1, 10, 3):
            px(img, tx + 1, ty + y, bark_lo)

    # 뿌리 네 개 (u16 v0 / u16 v8 / u16 v16 / u16 v24, 각 3x2x8) — 어두운 흙빛
    for (u, v) in [(16, 0), (16, 8), (16, 16), (16, 24)]:
        r = box(img, u, v, 3, 2, 8, root, 6, top=root_hi)
        rx, ry = r["front"]
        px(img, rx + 1, ry, root_hi)

    # 눈 덩어리 중심 매듭 (u40 v0, 4x4x4)
    k = box(img, 40, 0, 4, 4, 4, knot, 6, top=bark_lo)
    kx, ky = k["front"]
    px(img, kx + 1, ky + 1, pupil)
    px(img, kx + 2, ky + 2, pupil)

    # 눈 여덟 개 (3x3x3) — 흰자에 핏발, 앞면 가운데 검은 동공
    for (u, v) in [(0, 48), (16, 48), (32, 48), (48, 48), (0, 56), (16, 56), (32, 56), (48, 56)]:
        e = box(img, u, v, 3, 3, 3, eye_w, 4, top=eye_w)
        ex, ey = e["front"]
        px(img, ex, ey, vein)
        px(img, ex + 2, ey, vein)
        px(img, ex, ey + 2, vein)
        px(img, ex + 2, ey + 2, vein)
        px(img, ex + 1, ey + 1, pupil)

    save(img, "textures/entity/burrowing_heaven.png")


HV = {"k": "#1a1414", "r": "#b0202a", "w": "#efeae0", "g": "#8a847a"}
# 천국: 창백한 창이 대각선으로 뻗고 날 밑동에 작은 눈이 달린다
icon([
    "................",
    "............ww..",
    "...........www..",
    "..........wwk...",
    ".........www....",
    "........www.....",
    ".......www......",
    "......www.......",
    ".....www........",
    "....www.........",
    "...www..........",
    "..wwr...........",
    ".wrrw...........",
    ".rkw............",
    "..r.............",
    "................",
], HV, "textures/item/heaven.png")
# 천국의 옷: 눈 문양이 달린 창백한 코트
icon([
    "................",
    "...ww......ww...",
    "..wwww....wwww..",
    "..wwwwwwwwwwww..",
    "...wwwwwwwwww...",
    "....wwwwwwww....",
    "....wwrrrrww....",
    "....wwrkkrww....",
    "....wwrrrrww....",
    "....wwwwwwww....",
    "....wwwwwwww....",
    "....wwwwwwww....",
    "....wwwwwwww....",
    ".....gggggg.....",
    "................",
    "................",
], HV, "textures/item/heaven_suit.png")

armor(hexc("#efeae0"), "#1a1414", "#b0202a", "textures/entity/equipment/humanoid/heaven.png")

for item, parent in [("heaven", "minecraft:item/handheld"), ("heaven_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/heaven.json", {"layers": {"humanoid": [{"texture": "lobcorp:heaven"}]}})

burrowing_heaven()
print("assets burrowing heaven ok")

merge_lang({"entity.lobcorp.burrowing_heaven": "땅을 파는 천국", "item.lobcorp.heaven": "천국", "item.lobcorp.heaven_suit": "천국의 옷"},
           {"entity.lobcorp.burrowing_heaven": "The Burrowing Heaven", "item.lobcorp.heaven": "Heaven", "item.lobcorp.heaven_suit": "Heaven Suit"})


# ───────────── 불새 ─────────────
def firebird():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    dark = hexc("#3a0a06")
    red = hexc("#d0301a")
    orange = hexc("#f08020")
    yellow = hexc("#ffd040")
    white = hexc("#fff4c0")

    # 몸통 (u0 v0, 6x6x8) — 진홍빛, 가슴에 밝은 노란 핵
    b = box(img, 0, 0, 6, 6, 8, red, 7, top=orange)
    bx, by = b["front"]
    for y in range(2, 5):
        for x in range(2, 4):
            px(img, bx + x, by + y, yellow)
    px(img, bx + 2, by + 3, white)
    px(img, bx + 3, by + 3, white)

    # 목 (u0 v16, 3x4x3) 과 머리 (u0 v24, 4x4x4)
    box(img, 0, 16, 3, 4, 3, orange, 6, top=yellow)
    h = box(img, 0, 24, 4, 4, 4, red, 6, top=orange)
    hx, hy = h["front"]
    px(img, hx + 1, hy + 1, dark)
    px(img, hx + 2, hy + 1, dark)
    # 부리 (u16 v24, 2x1x3) — 금빛
    box(img, 16, 24, 2, 1, 3, yellow, 4, top=white)

    # 왼쪽 날개 세 장 (u0 v32 / u0 v40 / u0 v48)
    for (u, v, w, hh, d, c) in [(0, 32, 10, 1, 6, red), (0, 40, 10, 1, 6, orange), (0, 48, 8, 1, 4, yellow)]:
        box(img, u, v, w, hh, d, c, 6, top=c)
    # 오른쪽 날개 세 장 (u24 v32 / u24 v40 / u24 v48)
    for (u, v, w, hh, d, c) in [(24, 32, 10, 1, 6, red), (24, 40, 10, 1, 6, orange), (24, 48, 8, 1, 4, yellow)]:
        box(img, u, v, w, hh, d, c, 6, top=c)

    # 꼬리 깃 세 장 (u32 v0 / u32 v8 / u32 v16, 각 4x1x8) — 진홍 → 주황 → 노랑
    for (u, v, c) in [(32, 0, red), (32, 8, orange), (32, 16, yellow)]:
        t = box(img, u, v, 4, 1, 8, c, 6, top=c)
        tx, ty = t["front"]
        px(img, tx + 1, ty, white)
        px(img, tx + 2, ty, white)

    save(img, "textures/entity/firebird.png")


FB = {"k": "#3a0a06", "r": "#d0301a", "o": "#f08020", "y": "#ffd040", "w": "#fff4c0"}
# 불새: 대각선으로 뻗은 길고 타오르는 깃털
icon([
    "................",
    "............ww..",
    "...........wyy..",
    "..........wyy...",
    ".........wyy....",
    "........wyo.....",
    ".......wyo......",
    "......wyo.......",
    ".....wyo........",
    "....wyo.........",
    "...wyo..........",
    "..wro...........",
    ".wro............",
    ".ro.............",
    "rk..............",
    "................",
], FB, "textures/item/feather_of_honor.png")
# 명예의 깃털 옷: 깃털 장식이 달린 코트
icon([
    "................",
    "...rr......rr...",
    "..rrrr....rrrr..",
    "..rrrrrrrrrrrr..",
    "...rrrrrrrrrr...",
    "....rrwwwwrr....",
    "....rrwyywrr....",
    "....rrwyywrr....",
    "....rrwwwwrr....",
    "....rrrrrrrr....",
    "....rroooorr....",
    "....rrrrrrrr....",
    "....rrrrrrrr....",
    ".....kkkkkk.....",
    "................",
    "................",
], FB, "textures/item/feather_of_honor_suit.png")

armor(hexc("#d0301a"), "#3a0a06", "#ffd040", "textures/entity/equipment/humanoid/feather_of_honor.png")

for item, parent in [("feather_of_honor", "minecraft:item/handheld"), ("feather_of_honor_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/feather_of_honor.json", {"layers": {"humanoid": [{"texture": "lobcorp:feather_of_honor"}]}})

firebird()
print("assets firebird ok")

merge_lang({"entity.lobcorp.firebird": "불새", "item.lobcorp.feather_of_honor": "명예의 깃털", "item.lobcorp.feather_of_honor_suit": "명예의 깃털 옷"},
           {"entity.lobcorp.firebird": "The Firebird", "item.lobcorp.feather_of_honor": "Feather of Honor", "item.lobcorp.feather_of_honor_suit": "Feather of Honor Suit"})


# ───────────── 구름 낀 수도승 ─────────────
def clouded_monk():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    robe = hexc("#6a5a48")
    robe_dark = hexc("#4a3e32")
    hood = hexc("#1e1a16")
    cloud = hexc("#b8b4ac")
    cloud_hi = hexc("#eeece6")
    hand = hexc("#d8d4c8")

    # 몸통 (u0 v0, 8x10x5) — 회갈색 승복
    box(img, 0, 0, 8, 10, 5, robe, 7, top=robe_dark)
    # 머리 (u0 v16, 6x6x6) — 후드 안쪽은 어둡다
    box(img, 0, 16, 6, 6, 6, robe, 7, top=robe_dark, front=hood)
    # 긴 팔 (u0 v32, 2x12x2) — 끝은 창백한 손
    a = box(img, 0, 32, 2, 12, 2, robe, 7)
    ax, ay = a["front"]
    for y in range(9, 12):
        for x in range(2):
            px(img, ax + x, ay + y, hand)
    # 짧은 다리 (u8 v32, 2x5x2)
    box(img, 8, 32, 2, 5, 2, robe_dark, 7)
    # 구름 덩어리 네 개 (u16 v32 / u24 v32 / u32 v32 / u40 v32)
    box(img, 16, 32, 4, 4, 4, cloud, 6, top=cloud_hi)
    box(img, 24, 32, 4, 4, 4, cloud, 6, top=cloud_hi)
    box(img, 32, 32, 3, 3, 3, cloud, 6, top=cloud_hi)
    box(img, 40, 32, 3, 3, 3, cloud, 6, top=cloud_hi)

    save(img, "textures/entity/clouded_monk.png")


AM = {"k": "#1e1a16", "b": "#6a5a48", "g": "#b8b4ac", "w": "#eeece6", "o": "#d0a040"}
# 아미타: 금 고리가 달린 수도승의 지팡이
icon([
    "................",
    "......oooo......",
    ".....o....o.....",
    "....o......o....",
    "....o......o....",
    ".....o....o.....",
    "......oooo......",
    ".......bb.......",
    ".......bb.......",
    "......bbb.......",
    "......bb........",
    ".....bbb........",
    ".....bb.........",
    "....bbb.........",
    "....kk..........",
    "................",
], AM, "textures/item/amita.png")
# 아미타의 승복: 후드가 달린 승복 실루엣
icon([
    "................",
    "......bbbb......",
    ".....bbbbbb.....",
    "....bbbbbbbb....",
    "...bbbbbbbbbb...",
    "...bbkkkkkkbb...",
    "...bbkwwwwkbb...",
    "...bbkkkkkkbb...",
    "...bbbbbbbbbb...",
    "...bbbbbbbbbb...",
    "...bbbbbbbbbb...",
    "...bbbbbbbbbb...",
    "...bbbbbbbbbb...",
    "...bbbbbbbbbb...",
    "....kkkkkkkk....",
    "................",
], AM, "textures/item/amita_suit.png")

armor(hexc("#6a5a48"), "#1e1a16", "#d0a040", "textures/entity/equipment/humanoid/amita.png")

for item, parent in [("amita", "minecraft:item/handheld"), ("amita_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/amita.json", {"layers": {"humanoid": [{"texture": "lobcorp:amita"}]}})

clouded_monk()
print("assets clouded monk ok")

merge_lang({"entity.lobcorp.clouded_monk": "구름 낀 수도승", "item.lobcorp.amita": "아미타", "item.lobcorp.amita_suit": "아미타의 승복"},
           {"entity.lobcorp.clouded_monk": "Clouded Monk", "item.lobcorp.amita": "Amita", "item.lobcorp.amita_suit": "Amita Robe"})


# ───────────── 달의 울음 ─────────────
def il_pianto():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    ivory = hexc("#f2f0e8")
    ivory_dark = hexc("#cfcabb")
    key_white = hexc("#fbfaf4")
    key_black = hexc("#14141c")
    moon = hexc("#f0e0a0")
    moon_dark = hexc("#c8b070")
    tear = hexc("#5a6a9a")
    coat = hexc("#14141c")
    coat_hi = hexc("#2a2a38")

    # 피아노 몸통 (u0 v0, 12x5x10)
    box(img, 0, 0, 12, 5, 10, ivory, 6, top=ivory_dark)
    # 뚜껑 (u0 v16, 12x1x10)
    box(img, 0, 16, 12, 1, 10, ivory, 6, top=hexc("#fbfaf4"))
    # 다리 (u0 v28, 1x6x1)
    box(img, 0, 28, 1, 6, 1, ivory_dark, 5)
    # 건반 (u0 v36, 10x1x2) — 흑백 건반 줄
    k = box(img, 0, 36, 10, 1, 2, key_white, 4)
    kx, ky = k["front"]
    for x in range(0, 10, 2):
        px(img, kx + x, ky, key_black)
    # 초승달 얼굴 (u0 v40, 6x6x1)
    m = box(img, 0, 40, 6, 6, 1, moon, 5, top=moon_dark)
    mx, my = m["front"]
    for y in range(6):
        px(img, mx + 5, my + y, moon_dark)
    # 눈 (u8 v40, 1x1x1)
    box(img, 8, 40, 1, 1, 1, key_black, 0)
    # 눈물 자국 (u16 v40, 1x3x1)
    box(img, 16, 40, 1, 3, 1, tear, 4)

    # 형체의 긴 코트 (u24 v0, 6x14x4)
    box(img, 24, 0, 6, 14, 4, coat, 5, top=coat_hi)
    # 큰 초승달 머리 (u24 v20, 8x8x2)
    h = box(img, 24, 20, 8, 8, 2, moon, 5, top=moon_dark)
    hx, hy = h["front"]
    for y in range(8):
        px(img, hx + 7, hy + y, moon_dark)
        px(img, hx + 6, hy + y, moon_dark)
    for x in range(2, 5):
        px(img, hx + x, hy + 3, key_black)
    for y in range(4, 8):
        px(img, hx + 3, hy + y, tear)
    # 긴 팔 (u24 v32, 2x12x2)
    box(img, 24, 32, 2, 12, 2, coat, 5, top=coat_hi)

    save(img, "textures/entity/il_pianto.png")


ML = {"k": "#14141c", "w": "#f2f0e8", "y": "#f0e0a0", "b": "#5a6a9a", "g": "#9a9aa8"}
# 달빛: 가느다란 초승달 칼날
icon([
    "................",
    "..........yyy...",
    ".........yywy...",
    "........yywwy...",
    ".......yywwy....",
    "......yywwy.....",
    ".....yywwy......",
    ".....yywy.......",
    ".....yywy.......",
    ".....yywwy......",
    "......yywwy.....",
    ".......yywwy....",
    "........yywy....",
    ".........yyy....",
    "..........k.....",
    "................",
], ML, "textures/item/moonlight.png")
# 달빛의 연미복: 검은 연미복 실루엣
icon([
    "................",
    "......kkkk......",
    ".....kkkkkk.....",
    "....kkkkkkkk....",
    "...kkkkkkkkkk...",
    "...kkwwwwwwkk...",
    "...kkwyyyywkk...",
    "...kkwwwwwwkk...",
    "...kkkkkkkkkk...",
    "...kkkkkkkkkk...",
    "...kkkkkkkkkk...",
    "...kkkkkkkkkk...",
    "...kkk....kkk...",
    "...kkk....kkk...",
    "...kk......kk...",
    "................",
], ML, "textures/item/moonlight_suit.png")

armor(hexc("#14141c"), "#5a6a9a", "#f0e0a0", "textures/entity/equipment/humanoid/moonlight.png")

for item, parent in [("moonlight", "minecraft:item/handheld"), ("moonlight_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/moonlight.json", {"layers": {"humanoid": [{"texture": "lobcorp:moonlight"}]}})

il_pianto()
print("assets il pianto ok")

merge_lang({"entity.lobcorp.il_pianto": "달의 울음", "item.lobcorp.moonlight": "달빛", "item.lobcorp.moonlight_suit": "달빛의 연미복"},
           {"entity.lobcorp.il_pianto": "Il Pianto della Luna", "item.lobcorp.moonlight": "Moonlight", "item.lobcorp.moonlight_suit": "Moonlight Tailcoat"})


def backward_clock():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    wood = hexc("#4a2c18")
    wood_dark = hexc("#2e1a0e")
    wood_hi = hexc("#6a4024")
    brass = hexc("#c8a040")
    brass_dark = hexc("#8a6a20")
    cream = hexc("#f2e8c8")
    ink = hexc("#14141c")
    lamp_off = hexc("#3a3a44")
    lamp_on = hexc("#ffe860")

    # 밑동 (u0 v0, 10x4x6)
    box(img, 0, 0, 10, 4, 6, wood, 6, top=wood_hi)
    # 긴 몸통 (u0 v12, 8x18x5)
    box(img, 0, 12, 8, 18, 5, wood, 6, top=wood_hi)
    # 머리 (u0 v36, 10x8x6)
    box(img, 0, 36, 10, 8, 6, wood, 6, top=wood_hi)
    # 문자판 (u28 v0, 8x8x1) — 크림색 바탕에 검은 점 눈금
    f = box(img, 28, 0, 8, 8, 1, cream, 4, top=brass)
    fx, fy = f["front"]
    for i in range(8):
        px(img, fx + i, fy, brass_dark)
        px(img, fx + i, fy + 7, brass_dark)
        px(img, fx, fy + i, brass_dark)
        px(img, fx + 7, fy + i, brass_dark)
    for (dx, dy) in [(3, 1), (6, 3), (4, 6), (1, 4)]:
        px(img, fx + dx, fy + dy, ink)
    # 시침 (u28 v12, 1x3x1) / 분침 (u34 v12, 1x4x1)
    box(img, 28, 12, 1, 3, 1, ink, 0)
    box(img, 34, 12, 1, 4, 1, ink, 0)
    # 진자 (u28 v20, 2x8x1)
    box(img, 28, 20, 2, 8, 1, brass, 5, top=brass_dark)
    # 등불: 꺼진 것 (u40 v0, 2x2x2) / 켜진 것 (u40 v8, 2.5x2.5x2.5)
    box(img, 40, 0, 2, 2, 2, lamp_off, 4)
    box(img, 40, 8, 3, 3, 3, lamp_on, 6, top=hexc("#fff6b0"))

    save(img, "textures/entity/backward_clock.png")


backward_clock()
print("assets backward clock ok")

merge_lang({"entity.lobcorp.backward_clock": "거꾸로 가는 시계"},
           {"entity.lobcorp.backward_clock": "Backward Clock"})


# ───────────── 차원 굴절 변이체 ─────────────
def dimension_refraction():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    cyan = hexc("#7ad8f0")
    violet = hexc("#b08af0")
    white = hexc("#f4faff")
    core = hexc("#ffd8f0")

    # 중심의 작은 핵 (u0 v0, 2x2x2) — 분홍빛 흰색
    c = box(img, 0, 0, 2, 2, 2, core, 4, top=white)
    cx, cy = c["front"]
    px(img, cx, cy, white)
    px(img, cx + 1, cy + 1, white)

    # 얇은 수정 파편들: 1x6x1 (u0 v8) / 2x5x1 (u8 v8) / 1x4x2 (u16 v8)
    for (u, v, w, h, d, col) in [(0, 8, 1, 6, 1, cyan), (8, 8, 2, 5, 1, violet), (16, 8, 1, 4, 2, cyan)]:
        f = box(img, u, v, w, h, d, col, 6, top=white)
        fx, fy = f["front"]
        # 밝은 흰 테두리와 유리 같은 세로 하이라이트
        for y in range(h):
            px(img, fx, fy + y, white)
            px(img, fx + w - 1, fy + y, white)
        for x in range(w):
            px(img, fx + x, fy, white)
            px(img, fx + x, fy + h - 1, white)
        if w > 1:
            for y in range(1, h - 1):
                px(img, fx + 1, fy + y, violet if col == cyan else cyan)

    save(img, "textures/entity/dimension_refraction.png")


DF = {"k": "#20203a", "c": "#7ad8f0", "v": "#b08af0", "w": "#f4faff"}
# 회절: 대각선으로 뻗은 수정 칼날
icon([
    "................",
    "............ww..",
    "...........wcc..",
    "..........wcc...",
    ".........wcc....",
    "........wcc.....",
    ".......wcc......",
    "......wcc.......",
    ".....wcc........",
    "....wcc.........",
    "...wcc..........",
    "..wcc...........",
    "..wv............",
    ".wvk............",
    ".kk.............",
    "................",
], DF, "textures/item/diffraction.png")
# 회절의 옷: 프리즘 하이라이트가 흐르는 코트
icon([
    "................",
    "...cc......cc...",
    "..cccc....cccc..",
    "..cccccccccccc..",
    "...cccccccccc...",
    "....cccccccc....",
    "....ccwwwwcc....",
    "....ccwvvwcc....",
    "....ccwwwwcc....",
    "....cccccccc....",
    "....cccccccc....",
    "....cccccccc....",
    "....cccccccc....",
    ".....kkkkkk.....",
    "................",
    "................",
], DF, "textures/item/diffraction_suit.png")

armor(hexc("#7ad8f0"), "#20203a", "#b08af0", "textures/entity/equipment/humanoid/diffraction.png")

for item, parent in [("diffraction", "minecraft:item/handheld"), ("diffraction_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
wjson("equipment/diffraction.json", {"layers": {"humanoid": [{"texture": "lobcorp:diffraction"}]}})

dimension_refraction()
print("assets dimension refraction ok")

merge_lang({"entity.lobcorp.dimension_refraction": "차원 굴절 변이체", "item.lobcorp.diffraction": "회절", "item.lobcorp.diffraction_suit": "회절의 옷"},
           {"entity.lobcorp.dimension_refraction": "Dimensional Refraction Variant", "item.lobcorp.diffraction": "Diffraction", "item.lobcorp.diffraction_suit": "Diffraction Suit"})


# ───────────── 살점 우상 ─────────────
def flesh_idol():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    stone = hexc("#8a8a86")
    stone_dark = hexc("#5e5e5a")
    stone_hi = hexc("#a8a8a2")
    flesh = hexc("#c0504e")
    flesh_dark = hexc("#7a2a2c")
    flesh_hi = hexc("#e08078")
    bone = hexc("#efe6cc")
    bone_dark = hexc("#b8ac8c")

    # 돌 제단 (u0 v0, 12x4x8)
    f = box(img, 0, 0, 12, 4, 8, stone, 6, top=stone_hi, front=stone_dark)
    fx, fy = f["front"]
    for x in range(0, 12, 3):
        px(img, fx + x, fy + 1, stone_dark)
        px(img, fx + x, fy + 2, stone_dark)

    # 살점 덩어리 세 개: 8x5x6 (u0 v16) / 6x4x5 (u0 v28) / 4x3x4 (u0 v38)
    for (u, v, w, h, d) in [(0, 16, 8, 5, 6), (0, 28, 6, 4, 5), (0, 38, 4, 3, 4)]:
        f = box(img, u, v, w, h, d, flesh, 8, top=flesh_hi)
        fx, fy = f["front"]
        # 핏줄: 어두운 세로/가로 선
        for y in range(1, h - 1, 2):
            px(img, fx + (y % w), fy + y, flesh_dark)
        for x in range(1, w - 1, 2):
            px(img, fx + x, fy + (x % h), flesh_dark)
        px(img, fx + w - 1, fy, flesh_hi)

    # 머리 덩어리 (u0 v46, 4x4x4) — 어두운 눈 두 개
    f = box(img, 0, 46, 4, 4, 4, flesh, 8, top=flesh_hi)
    fx, fy = f["front"]
    px(img, fx + 1, fy + 1, flesh_dark)
    px(img, fx + 2, fy + 1, flesh_dark)
    px(img, fx + 1, fy + 2, flesh_dark)
    px(img, fx + 2, fy + 2, flesh_dark)

    # 치켜든 팔 토막 두 개 (u16 v46 / u24 v46, 2x4x2)
    box(img, 16, 46, 2, 4, 2, flesh, 8, top=flesh_hi)
    box(img, 24, 46, 2, 4, 2, flesh, 8, top=flesh_hi)

    # 뼈 가시 세 개 (u32 v46 / u36 v46 / u40 v46, 1x3x1)
    for u in (32, 36, 40):
        box(img, u, 46, 1, 3, 1, bone, 4, top=bone_dark)

    save(img, "textures/entity/flesh_idol.png")


flesh_idol()
print("assets flesh idol ok")

merge_lang({"entity.lobcorp.flesh_idol": "살점 우상"},
           {"entity.lobcorp.flesh_idol": "Flesh Idol"})


# ───────────── 지옥행 급행열차 ─────────────
def express_train():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    wood = hexc("#6b3a2a")
    wood_dark = hexc("#4a2418")
    wood_hi = hexc("#8a4f38")
    brass = hexc("#c9a227")
    brass_dark = hexc("#8a6d14")
    roof_c = hexc("#1e1e22")
    roof_hi = hexc("#33333a")
    steel = hexc("#8c8c94")
    steel_dark = hexc("#5a5a62")
    lamp_off = hexc("#3a2a20")
    lamp_on = hexc("#ffb020")
    lamp_on_hi = hexc("#ffe066")

    # 매표소 몸통 10x12x8 (u0 v0)
    f = box(img, 0, 0, 10, 12, 8, wood, 8, top=wood_hi, front=wood_dark)
    fx, fy = f["front"]
    for y in range(1, 12, 3):
        for x in range(10):
            px(img, fx + x, fy + y, wood_dark)

    # 지붕 판 12x2x10 (u0 v22)
    box(img, 0, 22, 12, 2, 10, roof_c, 5, top=roof_hi)

    # 매표 창 6x4x1 (u0 v36) — 놋쇠 창틀
    f = box(img, 0, 36, 6, 4, 1, brass, 6, top=brass_dark, front=brass_dark)
    fx, fy = f["front"]
    for x in range(1, 5):
        for y in range(1, 3):
            px(img, fx + x, fy + y, hexc("#2a1a12"))

    # 레일 두 줄 2x1x14 (u0 v44 / u0 v60)
    box(img, 0, 44, 2, 1, 14, steel, 6, top=steel_dark)
    box(img, 0, 60, 2, 1, 14, steel, 6, top=steel_dark)

    # 지붕 위 등불 네 개 (u32 v0 / u32 v8)
    for i in range(4):
        box(img, 32, 0, 2, 2, 2, lamp_off, 6, top=hexc("#4a3a2c"))
        box(img, 32, 8, 2, 2, 2, lamp_on, 6, top=lamp_on_hi, front=lamp_on_hi)

    save(img, "textures/entity/express_train.png")


express_train()
print("assets express train ok")

merge_lang({"entity.lobcorp.express_train": "지옥행 급행열차"},
           {"entity.lobcorp.express_train": "Express Train to Hell"})


# ═════════════ 마탄 의뢰서 (아이템) ═════════════
MB = {"k": "#1a1410", "b": "#6a5030", "y": "#e8d8a0", "r": "#a02020"}
icon([
    "................",
    "..kkkkkkkkkk....",
    ".kyyyyyyyyyyk...",
    ".kybbbbbbbbbk...",
    ".kybyyyyyyybk...",
    ".kybybbbbbybk...",
    ".kybyyyyyyybk...",
    ".kybbbbbbbbbk...",
    ".kyyyyyyyyyyk...",
    "..kkkkkkkkkk....",
    ".....krrk.......",
    ".....krrk.......",
    "......kk........",
    "................",
    "................",
    "................",
], MB, "textures/item/magic_bullet_order.png")
wjson("items/magic_bullet_order.json", {"model": {"type": "minecraft:model", "model": "lobcorp:item/magic_bullet_order"}})
wjson("models/item/magic_bullet_order.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "lobcorp:item/magic_bullet_order"}})
print("assets magic bullet order ok")

merge_lang({"item.lobcorp.magic_bullet_order": "마탄 의뢰서"},
           {"item.lobcorp.magic_bullet_order": "Magic Bullet Order"})


# ═════════════ 여명 의례 ( ordeal dawn ) ═════════════
def amber_dawn():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    amber = hexc("#d98a1e")
    amber_hi = hexc("#f2b64a")
    ring = hexc("#8a4a10")
    mouth = hexc("#5a1010")

    # 머리 (u0 v0, 4x4x4) + 입 테두리 (u0 v10, 2x2x1)
    h = box(img, 0, 0, 4, 4, 4, amber, 6, top=amber_hi)
    hx, hy = h["front"]
    for x in range(4):
        px(img, hx + x, hy + 3, ring)
    m = box(img, 0, 10, 2, 2, 1, ring, 6)
    mx, my = m["front"]
    for x in range(2):
        for y in range(2):
            px(img, mx + x, my + y, mouth)

    # 마디 1 (u0 v14, 4x4x4)
    s1 = box(img, 0, 14, 4, 4, 4, amber, 6, top=amber_hi)
    s1x, s1y = s1["front"]
    for x in range(4):
        px(img, s1x + x, s1y + 3, ring)
    # 마디 2 (u0 v24, 3x3x3)
    s2 = box(img, 0, 24, 3, 3, 3, amber, 6, top=amber_hi)
    s2x, s2y = s2["front"]
    for x in range(3):
        px(img, s2x + x, s2y + 2, ring)
    # 마디 3 (u0 v32, 3x3x3)
    s3 = box(img, 0, 32, 3, 3, 3, amber, 6, top=amber_hi)
    s3x, s3y = s3["front"]
    for x in range(3):
        px(img, s3x + x, s3y + 2, ring)

    save(img, "textures/entity/amber_dawn.png")


def green_dawn():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    metal = hexc("#6f8a72")
    metal_hi = hexc("#9ab89c")
    metal_dark = hexc("#3f5244")
    visor = hexc("#5cff8a")
    steel = hexc("#c8d2d8")

    # 몸통 (u0 v0, 6x8x4)
    box(img, 0, 0, 6, 8, 4, metal, 6, top=metal_hi)
    # 머리 (u0 v12, 6x6x6) + 바이저 (u24 v12, 4x1x1)
    h = box(img, 0, 12, 6, 6, 6, metal, 6, top=metal_hi)
    hx, hy = h["front"]
    for x in range(6):
        px(img, hx + x, hy + 5, metal_dark)
    v = box(img, 24, 12, 4, 1, 1, visor, 4)
    vx, vy = v["front"]
    for x in range(4):
        px(img, vx + x, vy, visor)
    # 오른팔 (u0 v24, 2x8x2) + 칼날 (u12 v24, 2x4x1)
    box(img, 0, 24, 2, 8, 2, metal, 6, top=metal_hi)
    box(img, 12, 24, 2, 4, 1, steel, 5, top=hexc("#eef4f8"))
    # 왼팔 (u0 v24, 2x8x2)
    box(img, 0, 24, 2, 8, 2, metal, 6, top=metal_hi)
    # 다리 (u20 v24, 2x8x2)
    box(img, 20, 24, 2, 8, 2, metal_dark, 6, top=metal)

    save(img, "textures/entity/green_dawn.png")


def crimson_dawn():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    white = hexc("#f2ece4")
    red = hexc("#b81c28")
    black = hexc("#241418")
    nose = hexc("#e03040")

    # 몸통 (u0 v0, 6x6x4): 진홍/검정 줄무늬
    b = box(img, 0, 0, 6, 6, 4, red, 6, top=black)
    bx, by = b["front"]
    for y in range(6):
        if y % 2 == 1:
            for x in range(6):
                px(img, bx + x, by + y, black)
    # 머리 (u0 v10, 8x8x8) + 코 (u32 v10, 2x2x1)
    h = box(img, 0, 10, 8, 8, 8, white, 5, top=white)
    hx, hy = h["front"]
    for (ex, ey) in [(1, 2), (6, 2)]:
        px(img, hx + ex, hy + ey, black)
    for x in range(8):
        px(img, hx + x, hy + 7, red)
    n = box(img, 32, 10, 2, 2, 1, nose, 5)
    nx, ny = n["front"]
    for x in range(2):
        for y in range(2):
            px(img, nx + x, ny + y, nose)
    # 고깔모자 1단 (u32 v14, 6x2x6)
    c1 = box(img, 32, 14, 6, 2, 6, red, 6, top=black)
    c1x, c1y = c1["front"]
    for x in range(6):
        px(img, c1x + x, c1y + 1, black)
    # 고깔모자 2단 (u32 v22, 2x3x2)
    box(img, 32, 22, 2, 3, 2, red, 6, top=black)
    # 팔 (u0 v26, 2x6x2)
    box(img, 0, 26, 2, 6, 2, red, 6, top=black)
    box(img, 0, 26, 2, 6, 2, red, 6, top=black)
    # 다리 (u12 v26, 2x8x2)
    box(img, 12, 26, 2, 8, 2, black, 6, top=red)
    box(img, 12, 26, 2, 8, 2, black, 6, top=red)

    save(img, "textures/entity/crimson_dawn.png")


def violet_dawn():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    skin = hexc("#4a1e6e")
    skin_hi = hexc("#7a3aa8")
    spot = hexc("#a06ad0")
    stem_c = hexc("#2a1030")
    root_c = hexc("#c8a8e0")

    # 몸통 3겹 (u0 v0 8x8x8 / u0 v16 6x6x6 / u0 v28 4x4x4)
    b = box(img, 0, 0, 8, 8, 8, skin, 6, top=skin_hi)
    bx, by = b["front"]
    for (sx, sy) in [(1, 2), (5, 1), (3, 5), (6, 6)]:
        px(img, bx + sx, by + sy, spot)
    b2 = box(img, 0, 16, 6, 6, 6, skin, 6, top=skin_hi)
    b2x, b2y = b2["front"]
    for (sx, sy) in [(1, 1), (4, 3)]:
        px(img, b2x + sx, b2y + sy, spot)
    box(img, 0, 28, 4, 4, 4, skin, 6, top=skin_hi)
    # 꼭지 (u24 v16, 2x3x2)
    box(img, 24, 16, 2, 3, 2, stem_c, 5, top=hexc("#3a1a40"))
    # 뿌리 네 가닥 (u24 v22, 1x6x1)
    for i in range(4):
        box(img, 24, 22, 1, 6, 1, root_c, 5)

    save(img, "textures/entity/violet_dawn.png")


amber_dawn()
green_dawn()
crimson_dawn()
violet_dawn()
print("assets ordeal dawn ok")

merge_lang({"entity.lobcorp.amber_dawn": "호박색 여명", "entity.lobcorp.green_dawn": "녹색 여명", "entity.lobcorp.crimson_dawn": "진홍색 여명", "entity.lobcorp.violet_dawn": "보라색 여명"},
           {"entity.lobcorp.amber_dawn": "Dawn of Amber", "entity.lobcorp.green_dawn": "Dawn of Green", "entity.lobcorp.crimson_dawn": "Dawn of Crimson", "entity.lobcorp.violet_dawn": "Dawn of Violet"})


def green_noon():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    plate = hexc("#4a5a4c")
    plate_hi = hexc("#6f8a72")
    plate_dark = hexc("#2c3a2e")
    eye = hexc("#5cff8a")
    steel = hexc("#c8d2d8")
    steel_hi = hexc("#eef4f8")

    # 몸통 (u0 v0, 10x10x6)
    b = box(img, 0, 0, 10, 10, 6, plate, 6, top=plate_hi)
    bx, by = b["front"]
    for y in range(10):
        if y % 3 == 2:
            for x in range(10):
                px(img, bx + x, by + y, plate_dark)
    # 머리 (u0 v16, 6x5x6) + 눈 (u24 v16, 4x1x1)
    h = box(img, 0, 16, 6, 5, 6, plate, 6, top=plate_hi)
    hx, hy = h["front"]
    for x in range(6):
        px(img, hx + x, hy + 4, plate_dark)
    e = box(img, 24, 16, 4, 1, 1, eye, 4)
    ex, ey = e["front"]
    for x in range(4):
        px(img, ex + x, ey, eye)
    # 오른팔 (u0 v28, 4x8x4) + 총열 (u16 v28, 2x6x2)
    box(img, 0, 28, 4, 8, 4, plate, 6, top=plate_hi)
    box(img, 16, 28, 2, 6, 2, steel, 5, top=steel_hi)
    # 왼팔 (u0 v28, 4x8x4) + 팔 끝 판 (u28 v28, 8x8x1)
    box(img, 0, 28, 4, 8, 4, plate, 6, top=plate_hi)
    box(img, 28, 28, 8, 8, 1, plate_dark, 6, top=plate)
    # 회전 톱 (u46 v28, 10x10x1)
    s = box(img, 46, 28, 10, 10, 1, steel, 5, top=steel_hi)
    sx, sy = s["front"]
    for i in range(10):
        px(img, sx + i, sy, steel_hi)
        px(img, sx + i, sy + 9, steel_hi)
        px(img, sx, sy + i, steel_hi)
        px(img, sx + 9, sy + i, steel_hi)
    # 다리 (u0 v40, 4x8x4)
    box(img, 0, 40, 4, 8, 4, plate_dark, 6, top=plate)

    save(img, "textures/entity/green_noon.png")


def crimson_noon():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    coat = hexc("#b81c28")
    coat_dark = hexc("#6e0e18")
    gold = hexc("#e8c04a")
    white = hexc("#f2ece4")
    black = hexc("#241418")
    nose = hexc("#e03040")

    # 연미복 몸통 (u0 v0, 8x12x4) + 금단추
    b = box(img, 0, 0, 8, 12, 4, coat, 6, top=coat_dark)
    bx, by = b["front"]
    for y in range(1, 12, 3):
        px(img, bx + 3, by + y, gold)
        px(img, bx + 4, by + y, gold)
    # 머리 (u0 v16, 8x8x8) + 코 (u32 v16, 2x2x1)
    h = box(img, 0, 16, 8, 8, 8, white, 5, top=white)
    hx, hy = h["front"]
    for (ex, ey) in [(1, 2), (6, 2)]:
        px(img, hx + ex, hy + ey, black)
    for x in range(8):
        px(img, hx + x, hy + 7, coat)
    n = box(img, 32, 16, 2, 2, 1, nose, 5)
    nx, ny = n["front"]
    for x in range(2):
        for y in range(2):
            px(img, nx + x, ny + y, nose)
    # 실크햇 챙 (u32 v20, 8x2x8) + 통 (u32 v30, 6x8x6)
    c1 = box(img, 32, 20, 8, 2, 8, black, 5, top=black)
    c1x, c1y = c1["front"]
    for x in range(8):
        px(img, c1x + x, c1y + 1, coat)
    c2 = box(img, 32, 30, 6, 8, 6, black, 5, top=black)
    c2x, c2y = c2["front"]
    for x in range(6):
        px(img, c2x + x, c2y + 6, gold)
    # 긴 팔 (u0 v32, 2x12x2)
    box(img, 0, 32, 2, 12, 2, coat, 6, top=coat_dark)
    box(img, 0, 32, 2, 12, 2, coat, 6, top=coat_dark)
    # 다리 (u8 v32, 2x8x2)
    box(img, 8, 32, 2, 8, 2, black, 6, top=coat)
    box(img, 8, 32, 2, 8, 2, black, 6, top=coat)

    save(img, "textures/entity/crimson_noon.png")


def violet_noon():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    stone = hexc("#241a30")
    stone_hi = hexc("#3a2a4a")
    rune = hexc("#a06ad0")
    tent = hexc("#1a1020")

    # 석판 아래 (u0 v0, 8x12x4) + 룬
    b = box(img, 0, 0, 8, 12, 4, stone, 5, top=stone_hi)
    bx, by = b["front"]
    for (rx, ry) in [(2, 2), (5, 4), (3, 7), (6, 9)]:
        px(img, bx + rx, by + ry, rune)
        px(img, bx + rx, by + ry + 1, rune)
    # 석판 위 (u0 v16, 8x12x4) + 룬
    u = box(img, 0, 16, 8, 12, 4, stone, 5, top=stone_hi)
    ux, uy = u["front"]
    for (rx, ry) in [(3, 1), (5, 3), (2, 6), (4, 9)]:
        px(img, ux + rx, uy + ry, rune)
        px(img, ux + rx + 1, uy + ry, rune)
    # 촉수 네 가닥 (u24 v0, 2x8x2)
    for i in range(4):
        box(img, 24, 0, 2, 8, 2, tent, 5, top=stone_hi)

    save(img, "textures/entity/violet_noon.png")


def indigo_noon():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    cloak = hexc("#2a2a6e")
    cloak_hi = hexc("#4444a0")
    cloak_dark = hexc("#16163a")
    face = hexc("#c8c0b8")
    shadow = hexc("#3a3a4a")
    steel = hexc("#c8d2d8")

    # 망토 몸통 (u0 v0, 8x12x6)
    b = box(img, 0, 0, 8, 12, 6, cloak, 6, top=cloak_hi)
    bx, by = b["front"]
    for y in range(12):
        if y % 4 == 3:
            for x in range(8):
                px(img, bx + x, by + y, cloak_dark)
    # 후드 (u0 v18, 8x7x8) + 그늘진 얼굴 (u32 v18, 6x4x1)
    h = box(img, 0, 18, 8, 7, 8, cloak, 6, top=cloak_hi)
    hx, hy = h["front"]
    for x in range(8):
        px(img, hx + x, hy + 6, cloak_dark)
    f = box(img, 32, 18, 6, 4, 1, face, 5)
    fx, fy = f["front"]
    for x in range(6):
        px(img, fx + x, fy, shadow)
    for (ex, ey) in [(1, 2), (4, 2)]:
        px(img, fx + ex, fy + ey, shadow)
    # 긴 팔 (u0 v34, 2x12x2) + 칼날 (u8 v34, 2x4x1)
    box(img, 0, 34, 2, 12, 2, cloak, 6, top=cloak_hi)
    box(img, 8, 34, 2, 4, 1, steel, 5, top=hexc("#eef4f8"))
    box(img, 0, 34, 2, 12, 2, cloak, 6, top=cloak_hi)
    box(img, 8, 34, 2, 4, 1, steel, 5, top=hexc("#eef4f8"))
    # 다리 (u16 v34, 2x6x2)
    box(img, 16, 34, 2, 6, 2, cloak_dark, 6, top=cloak)
    box(img, 16, 34, 2, 6, 2, cloak_dark, 6, top=cloak)

    save(img, "textures/entity/indigo_noon.png")


green_noon()
crimson_noon()
violet_noon()
indigo_noon()
print("assets ordeal noon ok")

merge_lang({"entity.lobcorp.green_noon": "녹색 정오", "entity.lobcorp.crimson_noon": "진홍색 정오", "entity.lobcorp.violet_noon": "보라색 정오", "entity.lobcorp.indigo_noon": "쪽빛 정오"},
           {"entity.lobcorp.green_noon": "Noon of Green", "entity.lobcorp.crimson_noon": "Noon of Crimson", "entity.lobcorp.violet_noon": "Noon of Violet", "entity.lobcorp.indigo_noon": "Noon of Indigo"})


def amber_dusk():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    chitin = hexc("#c07a1e")
    chitin_hi = hexc("#e8a83c")
    chitin_dark = hexc("#6e3c0c")
    tooth = hexc("#f4ecd8")
    maw = hexc("#3a1a08")

    # 머리 마디 (u0 v0, 20x14x12) + 등 가시 (u0 v26, 4x4x4)
    h = box(img, 0, 0, 20, 14, 12, chitin, 7, top=chitin_hi)
    hx, hy = h["front"]
    for y in range(14):
        if y % 5 == 4:
            for x in range(20):
                px(img, hx + x, hy + y, chitin_dark)
    for x in range(0, 20, 4):
        px(img, hx + x, hy + 1, chitin_hi)
    box(img, 0, 26, 4, 4, 4, chitin_dark, 6, top=chitin_hi)
    # 아가리 (u64 v0, 18x4x10) + 이빨 (u64 v14, 16x2x1)
    j = box(img, 64, 0, 18, 4, 10, maw, 6, top=chitin_dark)
    jx, jy = j["front"]
    for x in range(18):
        px(img, jx + x, jy, tooth if x % 2 == 0 else maw)
    t = box(img, 64, 14, 16, 2, 1, tooth, 4)
    tx, ty = t["front"]
    for x in range(16):
        px(img, tx + x, ty + 1, chitin_dark)
    # 몸통 마디 3개 (u0 v34 / u0 v60 / u0 v86, 20x14x12) + 가시
    for (u, v) in ((0, 34), (0, 60), (0, 86)):
        s = box(img, u, v, 20, 14, 12, chitin, 7, top=chitin_hi)
        sx, sy = s["front"]
        for y in range(14):
            if y % 5 == 4:
                for x in range(20):
                    px(img, sx + x, sy + y, chitin_dark)
        for x in range(2, 20, 6):
            px(img, sx + x, sy + 2, chitin_hi)
            px(img, sx + x, sy + 3, chitin_hi)
    box(img, 0, 60, 4, 4, 4, chitin_dark, 6, top=chitin_hi)
    box(img, 0, 86, 4, 4, 4, chitin_dark, 6, top=chitin_hi)
    box(img, 0, 112, 4, 4, 4, chitin_dark, 6, top=chitin_hi)

    save(img, "textures/entity/amber_dusk.png")


def green_dusk():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    metal = hexc("#5a6a5c")
    metal_hi = hexc("#7e9080")
    metal_dark = hexc("#2e3a30")
    rivet = hexc("#a8b8a8")
    glow = hexc("#5cff8a")

    # 받침 (u0 v0, 24x8x24)
    b = box(img, 0, 0, 24, 8, 24, metal, 6, top=metal_hi)
    bx, by = b["front"]
    for x in range(0, 24, 4):
        px(img, bx + x, by + 1, rivet)
        px(img, bx + x, by + 6, rivet)
    # 탑 몸통 (u0 v34, 16x24x16) + 녹색 창
    t = box(img, 0, 34, 16, 24, 16, metal, 6, top=metal_hi)
    tx, ty = t["front"]
    for (wx, wy) in ((3, 4), (9, 4), (3, 13), (9, 13)):
        for x in range(4):
            for y in range(5):
                px(img, tx + wx + x, ty + wy + y, glow if (x + y) % 3 else metal_dark)
    for x in range(0, 16, 4):
        px(img, tx + x, ty + 22, rivet)
    # 굴뚝 (u0 v76, 6x10x6)
    c = box(img, 0, 76, 6, 10, 6, metal_dark, 6, top=metal)
    cx, cy = c["front"]
    for x in range(6):
        px(img, cx + x, cy, metal_hi)
    # 기계 팔 (u64 v34, 4x16x4)
    a = box(img, 64, 34, 4, 16, 4, metal, 6, top=metal_hi)
    ax, ay = a["front"]
    for y in range(0, 16, 4):
        px(img, ax + 1, ay + y, rivet)
        px(img, ax + 2, ay + y, rivet)

    save(img, "textures/entity/green_dusk.png")


def crimson_dusk():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    cloth = hexc("#b81c28")
    cloth_dark = hexc("#6e0e18")
    white = hexc("#f2ece4")
    face = hexc("#f6f0e8")
    black = hexc("#241418")
    nose = hexc("#e03040")

    # 둥근 배 몸통 (u0 v0, 20x20x16) + 다이아몬드 무늬
    b = box(img, 0, 0, 20, 20, 16, cloth, 6, top=cloth_dark)
    bx, by = b["front"]
    for (dx, dy) in ((3, 3), (10, 3), (16, 3), (6, 9), (13, 9), (3, 15), (10, 15), (16, 15)):
        for i in range(3):
            px(img, bx + dx - i, by + dy, white)
            px(img, bx + dx + i, by + dy, white)
            px(img, bx + dx, by + dy - i, white)
            px(img, bx + dx, by + dy + i, white)
    # 큰 머리 (u0 v40, 16x16x16) + 코 (u0 v76, 4x4x4)
    h = box(img, 0, 40, 16, 16, 16, face, 5, top=face)
    hx, hy = h["front"]
    for (ex, ey) in ((3, 4), (11, 4)):
        for x in range(2):
            for y in range(2):
                px(img, hx + ex + x, hy + ey + y, black)
    for x in range(2, 14):
        px(img, hx + x, hy + 12, cloth)
    for x in range(3, 13, 2):
        px(img, hx + x, hy + 13, cloth_dark)
    n = box(img, 0, 76, 4, 4, 4, nose, 5, top=hexc("#f06070"))
    nx, ny = n["front"]
    for x in range(4):
        for y in range(4):
            px(img, nx + x, ny + y, nose)
    # 뾰족한 모자 (u64 v0, 14x10x14) + 꼭대기 (u64 v28, 6x8x6)
    c1 = box(img, 64, 0, 14, 10, 14, cloth, 6, top=cloth_dark)
    c1x, c1y = c1["front"]
    for x in range(14):
        px(img, c1x + x, c1y + 9, white)
    for (dx, dy) in ((4, 4), (9, 4)):
        for i in range(2):
            px(img, c1x + dx - i, c1y + dy, white)
            px(img, c1x + dx + i, c1y + dy, white)
            px(img, c1x + dx, c1y + dy - i, white)
            px(img, c1x + dx, c1y + dy + i, white)
    c2 = box(img, 64, 28, 6, 8, 6, cloth, 6, top=white)
    c2x, c2y = c2["front"]
    for x in range(6):
        px(img, c2x + x, c2y + 7, white)
    # 긴 팔 (u64 v44, 6x20x6) + 소매 커프스
    a = box(img, 64, 44, 6, 20, 6, cloth, 6, top=cloth_dark)
    ax, ay = a["front"]
    for x in range(6):
        px(img, ax + x, ay + 17, white)
        px(img, ax + x, ay + 18, white)
    # 다리 (u64 v72, 6x12x6)
    l = box(img, 64, 72, 6, 12, 6, cloth_dark, 6, top=cloth)
    lx, ly = l["front"]
    for x in range(6):
        px(img, lx + x, ly + 10, black)
        px(img, lx + x, ly + 11, black)

    save(img, "textures/entity/crimson_dusk.png")


amber_dusk()
green_dusk()
crimson_dusk()
print("assets ordeal dusk ok")

merge_lang({"entity.lobcorp.amber_dusk": "호박색 황혼", "entity.lobcorp.green_dusk": "녹색 황혼", "entity.lobcorp.crimson_dusk": "진홍색 황혼"},
           {"entity.lobcorp.amber_dusk": "Amber Dusk", "entity.lobcorp.green_dusk": "Green Dusk", "entity.lobcorp.crimson_dusk": "Crimson Dusk"})


# ═════════════ ALEPH: 아무것도 없는 / 침묵의 오케스트라 / 웃는 시체 산 ═════════════
def nothing_there():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    flesh = hexc("#a01c1c")
    flesh_dark = hexc("#6a0e0e")
    skin = hexc("#e0b8a0")
    black = hexc("#1a0808")
    white = hexc("#f4ece0")

    # 몸통 (u0 v0, 10x14x6)
    b = box(img, 0, 0, 10, 14, 6, flesh, 10, top=flesh_dark)
    bx, by = b["front"]
    for y in range(2, 12, 3):                       # 갈비뼈 같은 살점 주름
        for x in range(1, 9):
            px(img, bx + x, by + y, flesh_dark)
    for (x, y) in [(2, 4), (7, 7), (4, 10)]:        # 벗겨진 피부 조각
        rect(img, bx + x, by + y, 2, 2, skin, 6)

    # 머리 (u0 v20, 8x8x8)
    h = box(img, 0, 20, 8, 8, 8, flesh, 10, top=flesh_dark)
    hx, hy = h["front"]
    for (x, y) in [(1, 2), (2, 2), (5, 2), (6, 2), (1, 3), (6, 3)]:
        px(img, hx + x, hy + y, black)
    for x in range(1, 7):
        px(img, hx + x, hy + 5, black)
        px(img, hx + x, hy + 6, white if x % 2 else black)
    rect(img, hx + 3, hy + 1, 2, 1, skin, 4)

    # 아가리 (u32 v20, 6x4x2)
    m = box(img, 32, 20, 6, 4, 2, black, 4)
    mx, my = m["front"]
    for x in range(6):
        px(img, mx + x, my, white)
        px(img, mx + x, my + 3, white)

    # 긴 팔 (u0 v36 / u16 v36, 4x18x4)
    for u in (0, 16):
        a = box(img, u, 36, 4, 18, 4, flesh, 10, top=flesh_dark)
        ax, ay = a["front"]
        for y in range(3, 16, 4):
            for x in range(4):
                px(img, ax + x, ay + y, flesh_dark)
        rect(img, ax + 1, ay + 8, 2, 3, skin, 6)

    # 팔 칼날 (u32 v36, 2x14x2)
    bl = box(img, 32, 36, 2, 14, 2, hexc("#c8c0b4"), 6, top=white)
    blx, bly = bl["front"]
    for y in range(14):
        px(img, blx, bly + y, white)
        px(img, blx + 1, bly + y, hexc("#8a8078"))

    # 어깨 가시 (u40 v36, 8x4x4)
    s = box(img, 40, 36, 8, 4, 4, flesh_dark, 8, top=hexc("#d8d0c0"))
    sx, sy = s["front"]
    for x in range(0, 8, 2):
        px(img, sx + x, sy, white)

    # 다리 (u0 v60 / u12 v60, 4x10x4)
    for u in (0, 12):
        l = box(img, u, 60, 4, 10, 4, flesh, 10, top=flesh_dark)
        lx, ly = l["front"]
        for y in range(2, 9, 3):
            for x in range(4):
                px(img, lx + x, ly + y, flesh_dark)

    # 알 (u0 v76, 16x16x16)
    e = box(img, 0, 76, 16, 16, 16, flesh, 12, top=hexc("#c03030"))
    ex, ey = e["front"]
    for (x, y) in [(3, 4), (10, 6), (6, 11)]:
        rect(img, ex + x, ey + y, 3, 3, skin, 8)
    for x in range(2, 14, 4):
        for y in range(2, 14, 5):
            px(img, ex + x, ey + y, flesh_dark)

    save(img, "textures/entity/nothing_there.png")


def silent_orchestra():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    coat = hexc("#14141a")
    coat_hi = hexc("#2a2a36")
    white = hexc("#f2f0ea")
    silver = hexc("#c8ccd4")
    black = hexc("#0a0a0e")

    # 단상 (u0 v0, 12x2x12)
    p = box(img, 0, 0, 12, 2, 12, hexc("#3a2a1e"), 8, top=hexc("#5a4430"))
    px_, py_ = p["front"]
    for x in range(12):
        px(img, px_ + x, py_ + 1, hexc("#241a12"))

    # 연미복 몸통 (u0 v16, 8x12x4)
    b = box(img, 0, 16, 8, 12, 4, coat, 6, top=coat_hi)
    bx, by = b["front"]
    for y in range(12):
        px(img, bx + 3, by + y, white)
        px(img, bx + 4, by + y, white)
    for x in range(8):
        px(img, bx + x, by + 11, coat_hi)

    # 하얀 머리 (u0 v32, 6x6x6)
    h = box(img, 0, 32, 6, 6, 6, white, 5, top=hexc("#ffffff"))
    hx, hy = h["front"]
    for (x, y) in [(1, 2), (4, 2)]:
        px(img, hx + x, hy + y, black)
    for x in range(2, 4):
        px(img, hx + x, hy + 4, hexc("#8a8a92"))

    # 팔 (u24 v16, 2x10x2)
    for u in (24, 28):
        a = box(img, u, 16, 2, 10, 2, coat, 6, top=coat_hi)
        ax, ay = a["front"]
        for x in range(2):
            px(img, ax + x, ay + 9, white)

    # 지휘봉 (u32 v16, 1x8x1)
    bt = box(img, 32, 16, 1, 8, 1, silver, 4, top=hexc("#ffffff"))
    btx, bty = bt["front"]
    for y in range(8):
        px(img, btx, bty + y, silver)

    # 악사 구체 (u0 v44, 4x4x4)
    o = box(img, 0, 44, 4, 4, 4, hexc("#2a2a36"), 6, top=hexc("#4a4a5a"))
    ox, oy = o["front"]
    for (x, y) in [(1, 1), (2, 2)]:
        px(img, ox + x, oy + y, white)

    save(img, "textures/entity/silent_orchestra.png")


def smiling_mountain():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    flesh = hexc("#b09090")
    flesh_dark = hexc("#7a5c5c")
    pale = hexc("#e8dcd4")
    black = hexc("#241a1a")
    red = hexc("#8a2a2a")

    def face(fx, fy, w, h):
        for x in range(1, w - 1, 2):
            px(img, fx + x, fy + 1, black)
        for x in range(1, w - 1):
            px(img, fx + x, fy + h - 2, black)
        px(img, fx + 1, fy + h - 3, red)
        px(img, fx + w - 2, fy + h - 3, red)

    # 1단계 무더기 (u0 v0, 24x14x20)
    b = box(img, 0, 0, 24, 14, 20, flesh, 10, top=hexc("#c8a8a8"))
    bx, by = b["front"]
    for (x, y) in [(2, 2), (9, 4), (16, 2), (5, 9), (13, 9), (19, 7)]:
        rect(img, bx + x, by + y, 4, 4, pale, 6)
        face(bx + x, by + y, 4, 4)
    for y in range(0, 14, 4):
        for x in range(0, 24, 6):
            px(img, bx + x, by + y, flesh_dark)

    # 머리 네 개 (u0 v36, 4x4x4)
    for i in range(4):
        h = box(img, 0, 36, 4, 4, 4, pale, 6, top=hexc("#f4ece4"))
        hx, hy = h["front"]
        face(hx, hy, 4, 4)

    # 2단계 무더기 (u0 v48, 16x10x14)
    m = box(img, 0, 48, 16, 10, 14, flesh, 10, top=hexc("#c8a8a8"))
    mx, my = m["front"]
    for (x, y) in [(1, 1), (6, 3), (11, 1), (4, 6), (9, 6)]:
        rect(img, mx + x, my + y, 4, 4, pale, 6)
        face(mx + x, my + y, 4, 4)

    # 3단계 얼굴 무더기 (u0 v76, 12x10x12)
    t = box(img, 0, 76, 12, 10, 12, flesh_dark, 10, top=hexc("#a88888"))
    tx, ty = t["front"]
    rect(img, tx + 2, ty + 2, 8, 6, pale, 6)
    for x in range(3, 9):
        px(img, tx + x, ty + 4, black)
    for x in range(2, 10):
        px(img, tx + x, ty + 6, black)
        px(img, tx + x, ty + 7, pale if x % 2 else black)
    for (x, y) in [(0, 0), (9, 0), (0, 8), (9, 8)]:
        rect(img, tx + x, ty + y, 3, 2, pale, 6)

    save(img, "textures/entity/smiling_mountain.png")


nothing_there()
silent_orchestra()
smiling_mountain()

# ── E.G.O: 흉내 / 다 카포 / 미소 ──
MC = {"k": "#2a0606", "r": "#b81c1c", "c": "#e04040", "w": "#f0d8d0", "s": "#8a1010"}
icon([
    "................",
    ".............kk.",
    "............krr.",
    "...........krr..",
    "..........krr...",
    ".........krr....",
    "........krr.....",
    ".......krr......",
    "......krr.......",
    ".....krr........",
    "....krr.........",
    "...krr..........",
    "..krc...........",
    ".krc............",
    ".kc.............",
    "................",
], MC, "textures/item/mimicry.png")
icon([
    "................",
    "...rr......rr...",
    "..rrrr....rrrr..",
    "..rrrrrrrrrrrr..",
    "...rrrrrrrrrr...",
    "....rrwwwwrr....",
    "....rrwccwrr....",
    "....rrwccwrr....",
    "....rrwwwwrr....",
    "....rrrrrrrr....",
    "....rrkkkkrr....",
    "....rrrrrrrr....",
    "....rrrrrrrr....",
    ".....kkkkkk.....",
    "................",
    "................",
], MC, "textures/item/mimicry_suit.png")

DC = {"k": "#0e0e14", "d": "#22222e", "w": "#f2f0ea", "s": "#c8ccd4", "g": "#8a8a96"}
icon([
    "................",
    ".............ss.",
    "............sw..",
    "...........sw...",
    "..........sw....",
    ".........sw.....",
    "........sw......",
    ".......sw.......",
    "......sw........",
    ".....sw.........",
    "....sw..........",
    "...sw...........",
    "..sw............",
    "..w.............",
    "................",
    "................",
], DC, "textures/item/da_capo.png")
icon([
    "................",
    "...kk......kk...",
    "..kkkk....kkkk..",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "....kkwwwwkk....",
    "....kkwddwkk....",
    "....kkwddwkk....",
    "....kkwwwwkk....",
    "....kkkkkkkk....",
    "....kkddddkk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....wwwwww.....",
    "................",
    "................",
], DC, "textures/item/da_capo_suit.png")

SM = {"k": "#2a2a2a", "g": "#8a8a8a", "l": "#b8b8b8", "p": "#e8dcd4", "d": "#5a5a5a"}
icon([
    "................",
    "......gggg......",
    ".....gllllg.....",
    "....glpppplg....",
    "....glpkkplg....",
    "....glpppplg....",
    ".....gllllg.....",
    "......gggg......",
    ".......gg.......",
    ".......gg.......",
    ".......gg.......",
    ".......gg.......",
    ".......gg.......",
    "......kkkk......",
    "................",
    "................",
], SM, "textures/item/smile.png")
icon([
    "................",
    "...dd......dd...",
    "..dddd....dddd..",
    "..dddddddddddd..",
    "...dddddddddd...",
    "....ddppppdd....",
    "....ddpkkpdd....",
    "....ddppppdd....",
    "....dddddddd....",
    "....ddppppdd....",
    "....ddpkkpdd....",
    "....dddddddd....",
    "....dddddddd....",
    ".....kkkkkk.....",
    "................",
    "................",
], SM, "textures/item/smile_suit.png")

armor(hexc("#b81c1c"), "#2a0606", "#f0d8d0", "textures/entity/equipment/humanoid/mimicry.png")
armor(hexc("#14141a"), "#f2f0ea", "#c8ccd4", "textures/entity/equipment/humanoid/da_capo.png")
armor(hexc("#4a4a4a"), "#2a2a2a", "#e8dcd4", "textures/entity/equipment/humanoid/smile.png")

for item, parent in [("mimicry", "minecraft:item/handheld"), ("mimicry_suit", "minecraft:item/generated"),
                     ("da_capo", "minecraft:item/handheld"), ("da_capo_suit", "minecraft:item/generated"),
                     ("smile", "minecraft:item/handheld"), ("smile_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
for mat in ("mimicry", "da_capo", "smile"):
    wjson(f"equipment/{mat}.json", {"layers": {"humanoid": [{"texture": f"lobcorp:{mat}"}]}})

merge_lang({"entity.lobcorp.nothing_there": "아무것도 없는", "entity.lobcorp.silent_orchestra": "침묵의 오케스트라",
            "entity.lobcorp.smiling_mountain": "웃는 시체 산",
            "item.lobcorp.mimicry": "흉내", "item.lobcorp.mimicry_suit": "흉내 옷",
            "item.lobcorp.da_capo": "다 카포", "item.lobcorp.da_capo_suit": "다 카포 옷",
            "item.lobcorp.smile": "미소", "item.lobcorp.smile_suit": "미소 옷"},
           {"entity.lobcorp.nothing_there": "Nothing There", "entity.lobcorp.silent_orchestra": "The Silent Orchestra",
            "entity.lobcorp.smiling_mountain": "Mountain of Smiling Bodies",
            "item.lobcorp.mimicry": "Mimicry", "item.lobcorp.mimicry_suit": "Mimicry Suit",
            "item.lobcorp.da_capo": "Da Capo", "item.lobcorp.da_capo_suit": "Da Capo Suit",
            "item.lobcorp.smile": "Smile", "item.lobcorp.smile_suit": "Smile Suit"})
print("assets aleph 1 ok")


# ═════════════ ALEPH 2: 검열됨 / 푸른 별 / 녹아내리는 사랑 ═════════════
def censored():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    black = hexc("#050505")
    bar = hexc("#f4f4f4")
    red = hexc("#c01010")
    red_dark = hexc("#6a0606")

    def glitch(fx, fy, w, h):
        for _ in range(max(2, w // 3)):
            y = fy + random.randint(0, h - 1)
            x = fx + random.randint(0, max(0, w - 3))
            rect(img, x, y, random.randint(1, 3), 1, red, 6)
        for _ in range(2):
            y = fy + random.randint(0, h - 1)
            rect(img, fx, y, w, 1, red_dark, 4)

    def censor_bar(fx, fy, w, h):
        rect(img, fx, fy + h // 2 - 1, w, 2, bar, 2)
        for x in range(fx, fx + w, 3):
            px(img, x, fy + h // 2 - 1, hexc("#c8c8c8"))

    # 몸통 (u0 v0, 16x24x12)
    b = box(img, 0, 0, 16, 24, 12, black, 3)
    bx, by = b["front"]
    glitch(bx, by, 16, 24)
    censor_bar(bx, by, 16, 24)

    # 머리 (u0 v36, 10x10x10)
    h = box(img, 0, 36, 10, 10, 10, black, 3)
    hx, hy = h["front"]
    glitch(hx, hy, 10, 10)
    censor_bar(hx, hy, 10, 10)

    # 굵은 팔 (u0 v56 / u16 v56, 6x20x6)
    for u in (0, 16):
        a = box(img, u, 56, 6, 20, 6, black, 3)
        ax, ay = a["front"]
        glitch(ax, ay, 6, 20)
        censor_bar(ax, ay, 6, 20)

    # 촉수 (u32 v56 / u40 v56, 4x16x4)
    for u in (32, 40):
        t = box(img, u, 56, 4, 16, 4, black, 3)
        tx, ty = t["front"]
        glitch(tx, ty, 4, 16)

    # 다리 (u0 v84 / u12 v84, 6x16x6)
    for u in (0, 12):
        l = box(img, u, 84, 6, 16, 6, black, 3)
        lx, ly = l["front"]
        glitch(lx, ly, 6, 16)
        censor_bar(lx, ly, 6, 16)

    save(img, "textures/entity/censored.png")


def censored_minion():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    pale = hexc("#d8d0c8")
    pale_dark = hexc("#a89c94")
    black = hexc("#050505")
    bar = hexc("#f4f4f4")
    red = hexc("#c01010")

    def glitch(fx, fy, w, h):
        for _ in range(max(1, w // 4)):
            y = fy + random.randint(0, h - 1)
            x = fx + random.randint(0, max(0, w - 2))
            rect(img, x, y, random.randint(1, 2), 1, red, 6)

    # 몸통 (u0 v0, 8x12x4)
    b = box(img, 0, 0, 8, 12, 4, pale, 6, top=pale_dark)
    bx, by = b["front"]
    for y in range(2, 11, 3):
        for x in range(1, 7):
            px(img, bx + x, by + y, pale_dark)
    glitch(bx, by, 8, 12)

    # 머리 (u0 v16, 8x8x8) — 검은 막대가 덮는다
    h = box(img, 0, 16, 8, 8, 8, pale, 6, top=pale_dark)
    hx, hy = h["front"]
    rect(img, hx, hy + 2, 8, 4, black, 2)
    rect(img, hx, hy + 3, 8, 2, bar, 2)
    glitch(hx, hy, 8, 8)

    # 팔 (u0 v32 / u8 v32, 3x10x3)
    for u in (0, 8):
        a = box(img, u, 32, 3, 10, 3, pale, 6, top=pale_dark)
        ax, ay = a["front"]
        glitch(ax, ay, 3, 10)

    # 다리 (u0 v48 / u8 v48, 3x12x3)
    for u in (0, 8):
        l = box(img, u, 48, 3, 12, 3, pale, 6, top=pale_dark)
        lx, ly = l["front"]
        glitch(lx, ly, 3, 12)

    save(img, "textures/entity/censored_minion.png")


def blue_star():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    deep = hexc("#0a1a5a")
    deep_dark = hexc("#050c30")
    cyan = hexc("#40e0ff")
    white = hexc("#e8fbff")

    # 핵 (u0 v0, 12x12x12)
    c = box(img, 0, 0, 12, 12, 12, deep, 8, top=hexc("#122a7a"))
    cx, cy = c["front"]
    for (x, y) in [(5, 5), (6, 5), (5, 6), (6, 6)]:
        px(img, cx + x, cy + y, white)
    for (x, y) in [(4, 4), (7, 4), (4, 7), (7, 7)]:
        px(img, cx + x, cy + y, cyan)
    for i in range(12):
        px(img, cx + i, cy + 1, cyan)
        px(img, cx + i, cy + 10, cyan)
    for i in range(12):
        px(img, cx + 1, cy + i, hexc("#2a6ad0"))
        px(img, cx + 10, cy + i, hexc("#2a6ad0"))

    # 가시 네 개 (u0 v24 / u8 v24 / u16 v24 / u24 v24, 4x4x4)
    for u in (0, 8, 16, 24):
        s = box(img, u, 24, 4, 4, 4, deep_dark, 6, top=cyan)
        sx, sy = s["front"]
        px(img, sx + 1, sy + 1, white)
        px(img, sx + 2, sy + 2, cyan)

    save(img, "textures/entity/blue_star.png")


def melting_love():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    pink = hexc("#f08ab0")
    pink_dark = hexc("#c05a80")
    pink_deep = hexc("#8a3050")
    hair = hexc("#e06a98")
    black = hexc("#2a1018")
    white = hexc("#fff0f4")

    # 흘러내리는 치마 (u0 v0, 16x10x16)
    b = box(img, 0, 0, 16, 10, 16, pink, 8, top=hexc("#f8a8c4"))
    bx, by = b["front"]
    for x in range(0, 16, 3):
        rect(img, bx + x, by + 4, 2, 6, pink_dark, 6)
    for x in range(1, 16, 4):
        px(img, bx + x, by + 9, pink_deep)

    # 몸통 (u0 v32, 8x10x6)
    t = box(img, 0, 32, 8, 10, 6, pink, 8, top=hexc("#f8a8c4"))
    tx, ty = t["front"]
    for y in range(0, 10, 3):
        px(img, tx + 3, ty + y, pink_dark)
        px(img, tx + 4, ty + y, pink_dark)

    # 머리 (u0 v52, 8x8x8)
    h = box(img, 0, 52, 8, 8, 8, pink, 8, top=hexc("#f8a8c4"))
    hx, hy = h["front"]
    for (x, y) in [(2, 3), (5, 3)]:
        px(img, hx + x, hy + y, black)
        px(img, hx + x, hy + y + 1, black)
    for x in range(3, 5):
        px(img, hx + x, hy + 5, pink_deep)
    px(img, hx + 1, hy + 4, white)
    px(img, hx + 6, hy + 4, white)

    # 머리카락 (u0 v72, 10x10x10)
    hr = box(img, 0, 72, 10, 10, 10, hair, 8, top=hexc("#f08ab0"))
    hrx, hry = hr["front"]
    for x in range(0, 10, 2):
        rect(img, hrx + x, hry + 6, 1, 4, pink_deep, 4)

    # 팔 (u0 v96 / u8 v96, 3x10x3)
    for u in (0, 8):
        a = box(img, u, 96, 3, 10, 3, pink, 8, top=hexc("#f8a8c4"))
        ax, ay = a["front"]
        for y in range(6, 10):
            px(img, ax + 1, ay + y, pink_dark)

    # 방울 (u0 v112 / u6 v112 / u12 v112, 3x6x3)
    for u in (0, 6, 12):
        d = box(img, u, 112, 3, 6, 3, pink_dark, 6, top=pink)
        dx, dy = d["front"]
        px(img, dx + 1, dy + 5, pink_deep)

    save(img, "textures/entity/melting_love.png")


def melting_slime():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    pink = hexc("#f08ab0")
    pink_dark = hexc("#c05a80")
    pink_deep = hexc("#8a3050")
    black = hexc("#2a1018")
    white = hexc("#fff0f4")

    # 몸통 (u0 v0, 12x10x12)
    b = box(img, 0, 0, 12, 10, 12, pink, 8, top=hexc("#f8a8c4"))
    bx, by = b["front"]
    for x in range(0, 12, 4):
        rect(img, bx + x, by + 6, 2, 4, pink_dark, 6)
    for x in range(2, 12, 5):
        px(img, bx + x, by + 9, pink_deep)

    # 눈 (u0 v24 / u4 v24, 2x2x2)
    for u in (0, 4):
        e = box(img, u, 24, 2, 2, 2, white, 2)
        ex, ey = e["front"]
        px(img, ex, ey, black)
        px(img, ex + 1, ey + 1, black)

    # 하트 왕관 (u0 v32, 6x4x4)
    c = box(img, 0, 32, 6, 4, 4, pink_deep, 6, top=hexc("#ff5a8a"))
    cx, cy = c["front"]
    for (x, y) in [(1, 1), (4, 1), (2, 2), (3, 2)]:
        px(img, cx + x, cy + y, hexc("#ff9ac0"))

    save(img, "textures/entity/melting_slime.png")


censored()
censored_minion()
blue_star()
melting_love()
melting_slime()

# ── E.G.O: 검열됨 / 별의 소리 / 흠모 ──
CE = {"k": "#050505", "r": "#c01010", "d": "#6a0606", "w": "#f4f4f4", "g": "#3a3a3a"}
icon([
    "................",
    ".............kk.",
    "............krk.",
    "...........krrk.",
    "..........krrk..",
    ".........krrk...",
    "........krrk....",
    ".......krrk.....",
    "......krrk......",
    ".....krrk.......",
    "....krrk........",
    "...krrk.........",
    "..krdk..........",
    ".krdk...........",
    ".kk.............",
    "................",
], CE, "textures/item/censored.png")
icon([
    "................",
    "...kk......kk...",
    "..kkkk....kkkk..",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "....kkwwwwkk....",
    "....kkwrrwkk....",
    "....kkwrrwkk....",
    "....kkwwwwkk....",
    "....kkkkkkkk....",
    "....kkrrrrkk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....gggggg.....",
    "................",
    "................",
], CE, "textures/item/censored_suit.png")

BS = {"k": "#050c30", "b": "#0a1a5a", "c": "#40e0ff", "w": "#e8fbff", "d": "#2a6ad0"}
icon([
    "................",
    ".......cc.......",
    "......cwwc......",
    "......cwwc......",
    "..cc..cwwc..cc..",
    ".cwwccwwwwccwwc.",
    "..cc..cwwc..cc..",
    "......cwwc......",
    "......cwwc......",
    ".......cc.......",
    ".......bb.......",
    ".......bb.......",
    ".......bb.......",
    "......kkkk......",
    "................",
    "................",
], BS, "textures/item/sound_of_a_star.png")
icon([
    "................",
    "...bb......bb...",
    "..bbbb....bbbb..",
    "..bbbbbbbbbbbb..",
    "...bbbbbbbbbb...",
    "....bbccccbb....",
    "....bbcwccbb....",
    "....bbccccbb....",
    "....bbbbbbbb....",
    "....bbccccbb....",
    "....bbcwccbb....",
    "....bbbbbbbb....",
    "....bbbbbbbb....",
    ".....kkkkkk.....",
    "................",
    "................",
], BS, "textures/item/sound_of_a_star_suit.png")

AD = {"k": "#8a3050", "p": "#f08ab0", "d": "#c05a80", "w": "#fff0f4", "s": "#ff9ac0"}
icon([
    "................",
    "................",
    "..pppppppppppp..",
    "..pwwwwwwwwwwp..",
    "..pppppppppppp..",
    "..dddddddddddd..",
    "......dddd......",
    "......dddd......",
    "......dddd......",
    "......dddd......",
    "......dddd......",
    "......dddd......",
    "......kkkk......",
    "................",
    "................",
    "................",
], AD, "textures/item/adoration.png")
icon([
    "................",
    "...pp......pp...",
    "..pppp....pppp..",
    "..pppppppppppp..",
    "...pppppppppp...",
    "....ppwwwwpp....",
    "....ppwddwpp....",
    "....ppwwwwpp....",
    "....pppppppp....",
    "....ppwwwwpp....",
    "....ppwddwpp....",
    "....pppppppp....",
    "....pppppppp....",
    ".....kkkkkk.....",
    "................",
    "................",
], AD, "textures/item/adoration_suit.png")

armor(hexc("#050505"), "#c01010", "#f4f4f4", "textures/entity/equipment/humanoid/censored.png")
armor(hexc("#0a1a5a"), "#40e0ff", "#e8fbff", "textures/entity/equipment/humanoid/sound_of_a_star.png")
armor(hexc("#f08ab0"), "#8a3050", "#fff0f4", "textures/entity/equipment/humanoid/adoration.png")

for item, parent in [("censored", "minecraft:item/handheld"), ("censored_suit", "minecraft:item/generated"),
                     ("sound_of_a_star", "minecraft:item/handheld"), ("sound_of_a_star_suit", "minecraft:item/generated"),
                     ("adoration", "minecraft:item/handheld"), ("adoration_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
for mat in ("censored", "sound_of_a_star", "adoration"):
    wjson(f"equipment/{mat}.json", {"layers": {"humanoid": [{"texture": f"lobcorp:{mat}"}]}})

merge_lang({"entity.lobcorp.censored": "███████", "entity.lobcorp.censored_minion": "███████의 하수인",
            "entity.lobcorp.blue_star": "푸른 별", "entity.lobcorp.melting_love": "녹아내리는 사랑",
            "entity.lobcorp.melting_slime": "점액 덩어리",
            "item.lobcorp.censored": "███████", "item.lobcorp.censored_suit": "███████ 옷",
            "item.lobcorp.sound_of_a_star": "별의 소리", "item.lobcorp.sound_of_a_star_suit": "별의 소리 옷",
            "item.lobcorp.adoration": "흠모", "item.lobcorp.adoration_suit": "흠모 옷"},
           {"entity.lobcorp.censored": "CENSORED", "entity.lobcorp.censored_minion": "CENSORED Minion",
            "entity.lobcorp.blue_star": "Blue Star", "entity.lobcorp.melting_love": "Melting Love",
            "entity.lobcorp.melting_slime": "Slime Pawn",
            "item.lobcorp.censored": "CENSORED", "item.lobcorp.censored_suit": "CENSORED Suit",
            "item.lobcorp.sound_of_a_star": "Sound of a Star", "item.lobcorp.sound_of_a_star_suit": "Sound of a Star Suit",
            "item.lobcorp.adoration": "Adoration", "item.lobcorp.adoration_suit": "Adoration Suit"})
print("assets aleph 2 ok")


# ═════════════ ALEPH 3: 검은 군단 / 백야 / 종말새 ═════════════
def army_in_black():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    pink = hexc("#f08ab0")
    pink_dark = hexc("#c05a80")
    pink_deep = hexc("#8a3050")
    white = hexc("#f4f0f4")
    black = hexc("#1a1418")

    # 다리 (u0 v0 / u8 v0, 2x4x2)
    for u in (0, 8):
        box(img, u, 0, 2, 4, 2, black, 4, top=hexc("#2a2228"))

    # 몸통 (u0 v8, 4x6x3) — 가슴의 분홍 하트
    b = box(img, 0, 8, 4, 6, 3, white, 6, top=pink)
    bx, by = b["front"]
    for (x, y) in [(1, 1), (2, 1), (1, 2), (2, 2)]:
        px(img, bx + x, by + y, pink)
    px(img, bx + 1, by + 3, pink_deep)
    px(img, bx + 2, by + 3, pink_deep)

    # 머리 (u0 v20, 4x4x4)
    h = box(img, 0, 20, 4, 4, 4, white, 6, top=hexc("#e8e0e8"))
    hx, hy = h["front"]
    px(img, hx + 1, hy + 1, black)
    px(img, hx + 2, hy + 1, black)
    px(img, hx + 1, hy + 2, pink_dark)
    px(img, hx + 2, hy + 2, pink_dark)

    # 높은 모자 (u0 v32, 4x4x4)
    box(img, 0, 32, 4, 4, 4, black, 4, top=pink_dark)

    save(img, "textures/entity/army_in_black.png")


def army_soldier():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    black = hexc("#141018")
    black_light = hexc("#241c28")
    pink = hexc("#ff5a8a")
    pink_light = hexc("#ff9ac0")
    pink_deep = hexc("#8a3050")
    skin = hexc("#e8d8d0")

    # 몸통 (u0 v0, 8x12x4) — 가슴의 커다란 하트
    b = box(img, 0, 0, 8, 12, 4, black, 4, top=black_light)
    bx, by = b["front"]
    for x in range(8):
        px(img, bx + x, by, hexc("#3a2a34"))
    for (x, y) in [(2, 3), (3, 3), (4, 3), (5, 3), (2, 4), (3, 4), (4, 4), (5, 4),
                   (3, 5), (4, 5), (3, 6), (4, 6)]:
        px(img, bx + x, by + y, pink)
    for (x, y) in [(3, 3), (4, 3), (3, 4), (4, 4)]:
        px(img, bx + x, by + y, pink_light)
    px(img, bx + 3, by + 7, pink_deep)
    px(img, bx + 4, by + 7, pink_deep)

    # 머리 (u0 v16, 8x8x8)
    h = box(img, 0, 16, 8, 8, 8, skin, 6, top=hexc("#d8c8c0"))
    hx, hy = h["front"]
    for x in range(8):
        px(img, hx + x, hy, black)
        px(img, hx + x, hy + 1, black)
    px(img, hx + 2, hy + 3, black)
    px(img, hx + 5, hy + 3, black)
    px(img, hx + 3, hy + 5, pink_deep)
    px(img, hx + 4, hy + 5, pink_deep)

    # 높은 모자 (u0 v32, 6x8x6)
    box(img, 0, 32, 6, 8, 6, black, 4, top=black_light)

    # 팔 (u0 v48 / u8 v48, 3x12x3)
    for u in (0, 8):
        a = box(img, u, 48, 3, 12, 3, black, 4, top=black_light)
        ax, ay = a["front"]
        for y in range(9, 12):
            px(img, ax + 1, ay + y, skin)

    # 다리 (u0 v64 / u8 v64, 3x12x3)
    for u in (0, 8):
        l = box(img, u, 64, 3, 12, 3, black, 4, top=black_light)
        lx, ly = l["front"]
        for y in range(10, 12):
            px(img, lx + 1, ly + y, hexc("#0a080c"))

    save(img, "textures/entity/army_soldier.png")


def whitenight():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    white = hexc("#f6f2e8")
    white_dark = hexc("#d8d0c0")
    gold = hexc("#e8c860")
    gold_dark = hexc("#a88a30")
    red = hexc("#c02020")

    # 몸통 (u0 v0, 8x16x6)
    b = box(img, 0, 0, 8, 16, 6, white, 4, top=white_dark)
    bx, by = b["front"]
    for y in range(0, 16, 4):
        for x in range(8):
            px(img, bx + x, by + y, gold)
    for x in range(3, 5):
        for y in range(2, 14):
            px(img, bx + x, by + y, gold_dark)
    px(img, bx + 3, by + 6, red)
    px(img, bx + 4, by + 6, red)

    # 머리 (u0 v24, 8x8x8)
    h = box(img, 0, 24, 8, 8, 8, white, 4, top=white_dark)
    hx, hy = h["front"]
    for (x, y) in [(2, 3), (5, 3)]:
        px(img, hx + x, hy + y, gold)
        px(img, hx + x, hy + y + 1, gold_dark)
    for x in range(3, 5):
        px(img, hx + x, hy + 6, red)

    # 고리 (u0 v40, 14x1x14)
    box(img, 0, 40, 14, 1, 14, gold, 4, top=hexc("#fff0a0"))

    # 날개 (u0 v56 / u32 v56, 2x20x16) — 흰 깃털
    for u in (0, 32):
        w = box(img, u, 56, 2, 20, 16, white, 4, top=white_dark)
        wx, wy = w["front"]
        for y in range(0, 20, 3):
            for x in range(2):
                px(img, wx + x, wy + y, white_dark)
        for y in range(0, 20, 5):
            px(img, wx, wy + y, gold)
            px(img, wx + 1, wy + y, gold)

    save(img, "textures/entity/whitenight.png")


def apostle():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    white = hexc("#f2eee2")
    white_dark = hexc("#d0c8b8")
    gold = hexc("#e0c060")
    gold_dark = hexc("#a08030")
    skin = hexc("#e8d8c8")
    wood = hexc("#6a4a2a")

    # 두루마기 몸통 (u0 v0, 8x16x6)
    b = box(img, 0, 0, 8, 16, 6, white, 4, top=white_dark)
    bx, by = b["front"]
    for x in range(8):
        px(img, bx + x, by + 15, gold)
        px(img, bx + x, by + 14, gold_dark)
    for y in range(0, 16, 5):
        for x in range(8):
            px(img, bx + x, by + y, white_dark)
    for y in range(2, 14):
        px(img, bx + 3, by + y, gold)
        px(img, bx + 4, by + y, gold)

    # 머리 (u0 v24, 8x8x8)
    h = box(img, 0, 24, 8, 8, 8, skin, 6, top=hexc("#d8c8b8"))
    hx, hy = h["front"]
    for x in range(8):
        px(img, hx + x, hy, white)
        px(img, hx + x, hy + 1, white_dark)
    px(img, hx + 2, hy + 3, hexc("#3a2a20"))
    px(img, hx + 5, hy + 3, hexc("#3a2a20"))
    px(img, hx + 3, hy + 5, hexc("#a08070"))
    px(img, hx + 4, hy + 5, hexc("#a08070"))

    # 작은 고리 (u0 v40, 8x1x8)
    box(img, 0, 40, 8, 1, 8, gold, 4, top=hexc("#fff0a0"))

    # 팔 (u0 v52 / u8 v52, 3x12x3)
    for u in (0, 8):
        a = box(img, u, 52, 3, 12, 3, white, 4, top=white_dark)
        ax, ay = a["front"]
        for y in range(9, 12):
            px(img, ax + 1, ay + y, skin)
        for y in range(0, 9, 4):
            px(img, ax + 1, ay + y, gold)

    # 긴 창 (u0 v68, 1x24x1)
    box(img, 0, 68, 1, 24, 1, wood, 4, top=gold)

    # 다리 (u0 v96 / u8 v96, 3x12x3)
    for u in (0, 8):
        l = box(img, u, 96, 3, 12, 3, white, 4, top=white_dark)
        lx, ly = l["front"]
        for y in range(10, 12):
            px(img, lx + 1, ly + y, hexc("#4a3a2a"))

    save(img, "textures/entity/apostle.png")


def apocalypse_bird():
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    dark = hexc("#0e0c12")
    dark_light = hexc("#1e1a24")
    yellow = hexc("#f0d040")
    yellow_dark = hexc("#a08010")
    gold = hexc("#e0b840")
    red = hexc("#b02020")

    # 몸통 (u0 v0, 24x20x20)
    b = box(img, 0, 0, 24, 20, 20, dark, 4, top=dark_light)
    bx, by = b["front"]
    for y in range(0, 20, 4):
        for x in range(0, 24, 6):
            px(img, bx + x, by + y, dark_light)

    # 목 (u96 v0, 6x14x6)
    box(img, 96, 0, 6, 14, 6, dark, 4, top=dark_light)

    # 머리 (u128 v0, 10x10x14)
    h = box(img, 128, 0, 10, 10, 14, dark, 4, top=dark_light)
    hx, hy = h["front"]
    for (x, y) in [(2, 3), (6, 3)]:
        px(img, hx + x, hy + y, yellow)
        px(img, hx + x + 1, hy + y, yellow)
        px(img, hx + x, hy + y + 1, yellow_dark)
        px(img, hx + x + 1, hy + y + 1, yellow_dark)

    # 부리 (u184 v0, 4x3x6)
    box(img, 184, 0, 4, 3, 6, red, 6, top=hexc("#d04040"))

    # 날개 (u0 v48 / u64 v48, 4x28x24)
    for u in (0, 64):
        w = box(img, u, 48, 4, 28, 24, dark, 4, top=dark_light)
        wx, wy = w["front"]
        for y in range(0, 28, 4):
            for x in range(4):
                px(img, wx + x, wy + y, hexc("#2a2430"))
        for y in range(2, 28, 8):
            px(img, wx + 1, wy + y, yellow_dark)

    # 꼬리 (u96 v24, 4x4x20)
    box(img, 96, 24, 4, 4, 20, dark, 4, top=dark_light)

    # 몸통의 눈 (u208 v0 / u216 v0 / u224 v0, 2x2x1)
    for k in range(3):
        e = box(img, 208 + k * 8, 0, 2, 2, 1, yellow, 4)
        ex, ey = e["front"]
        px(img, ex, ey, hexc("#1a1408"))
        px(img, ex + 1, ey + 1, yellow_dark)

    # 황금 저울 (u208 v8, 1x12x1 + u216 v8, 6x1x1)
    box(img, 208, 8, 1, 12, 1, gold, 4, top=hexc("#fff0a0"))
    box(img, 216, 8, 6, 1, 1, gold, 4, top=hexc("#fff0a0"))

    save(img, "textures/entity/apocalypse_bird.png")


def apocalypse_egg():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    shell = hexc("#2a2430")
    shell_light = hexc("#3e3648")
    yellow = hexc("#f0d040")
    yellow_dark = hexc("#a08010")
    red = hexc("#b02020")

    # 알 (u0 v0, 12x16x12)
    e = box(img, 0, 0, 12, 16, 12, shell, 4, top=shell_light)
    ex, ey = e["front"]
    for y in range(0, 16, 4):
        for x in range(0, 12, 3):
            px(img, ex + x, ey + y, shell_light)
    for x in range(2, 10, 3):
        px(img, ex + x, ey + 8, yellow_dark)

    # 부리 (u0 v32, 4x3x4)
    box(img, 0, 32, 4, 3, 4, red, 6, top=hexc("#d04040"))

    # 팔 (u0 v44 / u8 v44, 2x12x2)
    for u in (0, 8):
        a = box(img, u, 44, 2, 12, 2, shell, 4, top=shell_light)
        ax, ay = a["front"]
        for y in range(0, 12, 3):
            px(img, ax, ay + y, yellow_dark)

    # 눈 (u0 v60 / u4 v60 / u8 v60 / u12 v60, 2x2x1)
    for u in (0, 4, 8, 12):
        ey2 = box(img, u, 60, 2, 2, 1, yellow, 4)
        exx, eyy = ey2["front"]
        px(img, exx, eyy, hexc("#1a1408"))
        px(img, exx + 1, eyy + 1, yellow_dark)

    save(img, "textures/entity/apocalypse_egg.png")


army_in_black()
army_soldier()
whitenight()
apostle()
apocalypse_bird()
apocalypse_egg()

# ── E.G.O: 분홍 / 실낙원 / 황혼 ──
PK = {"k": "#1a1418", "p": "#f08ab0", "d": "#c05a80", "w": "#fff0f4", "s": "#ff9ac0"}
icon([
    "................",
    ".............kk.",
    "............kpk.",
    "...........kppk.",
    "..........kppk..",
    ".........kppk...",
    "........kppk....",
    ".......kppk.....",
    "......kppk......",
    ".....kppk.......",
    "....kppk........",
    "...kppk.........",
    "..kpdk..........",
    ".kpdk...........",
    ".kk.............",
    "................",
], PK, "textures/item/pink.png")
icon([
    "................",
    "...kk......kk...",
    "..kkkk....kkkk..",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "....kkppppkk....",
    "....kkpsspkk....",
    "....kkppppkk....",
    "....kkkkkkkk....",
    "....kkppppkk....",
    "....kkpsspkk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....pppppp.....",
    "................",
    "................",
], PK, "textures/item/pink_suit.png")

PL = {"k": "#3a3020", "w": "#f6f2e8", "g": "#e0c060", "r": "#c02020", "d": "#d0c8b8"}
icon([
    "................",
    "......wwww......",
    ".....wwwwww.....",
    "....wwrrrrww....",
    "....wwrrrrww....",
    ".....wwwwww.....",
    "......wwww......",
    ".......gg.......",
    ".......gg.......",
    ".......gg.......",
    ".......gg.......",
    ".......gg.......",
    ".......gg.......",
    "......kkkk......",
    "................",
    "................",
], PL, "textures/item/paradise_lost.png")
icon([
    "................",
    "...ww......ww...",
    "..wwww....wwww..",
    "..wwwwwwwwwwww..",
    "...wwwwwwwwww...",
    "....wwggggww....",
    "....wwgrrgww....",
    "....wwggggww....",
    "....wwwwwwww....",
    "....wwggggww....",
    "....wwgrrgww....",
    "....wwwwwwww....",
    "....wwwwwwww....",
    ".....gggggg.....",
    "................",
    "................",
], PL, "textures/item/paradise_lost_suit.png")

TW = {"k": "#0e0c12", "g": "#e0b840", "r": "#b02020", "b": "#3060c0", "d": "#2a2430"}
icon([
    "................",
    ".............kk.",
    "............kgk.",
    "...........kgrk.",
    "..........kgbk..",
    ".........kgrk...",
    "........kgbk....",
    ".......kgrk.....",
    "......kgbk......",
    ".....kgrk.......",
    "....kgbk........",
    "...kgrk.........",
    "..kgdk..........",
    ".kgdk...........",
    ".kk.............",
    "................",
], TW, "textures/item/twilight.png")
icon([
    "................",
    "...kk......kk...",
    "..kkkk....kkkk..",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "....kkggggkk....",
    "....kkgrrgkk....",
    "....kkggggkk....",
    "....kkkkkkkk....",
    "....kkggggkk....",
    "....kkgbbgkk....",
    "....kkkkkkkk....",
    "....kkkkkkkk....",
    ".....gggggg.....",
    "................",
    "................",
], TW, "textures/item/twilight_suit.png")

armor(hexc("#f08ab0"), "#1a1418", "#fff0f4", "textures/entity/equipment/humanoid/pink.png")
armor(hexc("#f6f2e8"), "#e0c060", "#c02020", "textures/entity/equipment/humanoid/paradise_lost.png")
armor(hexc("#0e0c12"), "#e0b840", "#b02020", "textures/entity/equipment/humanoid/twilight.png")

for item, parent in [("pink", "minecraft:item/handheld"), ("pink_suit", "minecraft:item/generated"),
                     ("paradise_lost", "minecraft:item/handheld"), ("paradise_lost_suit", "minecraft:item/generated"),
                     ("twilight", "minecraft:item/handheld"), ("twilight_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})
for mat in ("pink", "paradise_lost", "twilight"):
    wjson(f"equipment/{mat}.json", {"layers": {"humanoid": [{"texture": f"lobcorp:{mat}"}]}})

merge_lang({"entity.lobcorp.army_in_black": "검은 군단", "entity.lobcorp.army_soldier": "검은 군인",
            "entity.lobcorp.whitenight": "백야", "entity.lobcorp.apostle": "사도",
            "entity.lobcorp.apocalypse_bird": "종말새", "entity.lobcorp.apocalypse_egg": "종말새의 알",
            "item.lobcorp.pink": "분홍", "item.lobcorp.pink_suit": "분홍 옷",
            "item.lobcorp.paradise_lost": "실낙원", "item.lobcorp.paradise_lost_suit": "실낙원 옷",
            "item.lobcorp.twilight": "황혼", "item.lobcorp.twilight_suit": "황혼 옷"},
           {"entity.lobcorp.army_in_black": "Army in Black", "entity.lobcorp.army_soldier": "Black Soldier",
            "entity.lobcorp.whitenight": "WhiteNight", "entity.lobcorp.apostle": "Apostle",
            "entity.lobcorp.apocalypse_bird": "Apocalypse Bird", "entity.lobcorp.apocalypse_egg": "Apocalypse Egg",
            "item.lobcorp.pink": "Pink", "item.lobcorp.pink_suit": "Pink Suit",
            "item.lobcorp.paradise_lost": "Paradise Lost", "item.lobcorp.paradise_lost_suit": "Paradise Lost Suit",
            "item.lobcorp.twilight": "Twilight", "item.lobcorp.twilight_suit": "Twilight Suit"})
print("assets aleph 3 ok")


# ═════════════ 사무직 (Clerk) ═════════════
def clerks():
    """플레이어 스킨 레이아웃(64x64) 사무직 4종."""
    suits = ["#1e2a44", "#2b2b30", "#1d3326", "#4a1c26"]  # 네이비 / 차콜 / 진녹 / 버건디
    hairs = ["#141018", "#4a2c18", "#2a1a10", "#c8a050"]  # 검정 / 갈색 / 진갈색 / 금발
    skin = hexc("#e8b48c")
    shirt = hexc("#f2f2ee")
    tie = hexc("#8c1c24")
    trouser = hexc("#22222a")
    shoe = hexc("#101014")
    for n in range(4):
        img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        suit = hexc(suits[n])
        hair = hexc(hairs[n])

        # 머리 (u0 v0, 8x8x8)
        f = box(img, 0, 0, 8, 8, 8, skin, 4)
        # 얼굴: 눈 / 입
        fx, fy = f["front"]
        px(img, fx + 1, fy + 3, hexc("#20202a"))
        px(img, fx + 2, fy + 3, hexc("#20202a"))
        px(img, fx + 5, fy + 3, hexc("#20202a"))
        px(img, fx + 6, fy + 3, hexc("#20202a"))
        rect(img, fx + 3, fy + 5, 2, 1, hexc("#a05a50"), 0)
        # 머리카락: 윗면 + 앞/뒤/옆 상단
        tx, ty = f["top"]
        rect(img, tx, ty, 8, 8, hair, 6)
        rect(img, fx, fy, 8, 2, hair, 6)
        bx, by = f["back"]
        rect(img, bx, by, 8, 4, hair, 6)
        rx, ry = f["right"]
        rect(img, rx, ry, 8, 2, hair, 6)
        lx, ly = f["left"]
        rect(img, lx, ly, 8, 2, hair, 6)

        # 몸통 (u16 v16, 8x12x4) — 흰 셔츠 + 넥타이 + 재킷
        b = box(img, 16, 16, 8, 12, 4, shirt, 3)
        bx, by = b["front"]
        rect(img, bx + 3, by, 2, 8, tie, 4)          # 넥타이
        rect(img, bx, by, 2, 12, suit, 5)            # 재킷 앞 왼쪽
        rect(img, bx + 6, by, 2, 12, suit, 5)        # 재킷 앞 오른쪽
        rect(img, bx, by + 10, 8, 2, suit, 5)        # 재킷 아래
        for face in ("back", "right", "left"):
            x, y = b[face]
            rect(img, x, y, 8 if face == "back" else 4, 12, suit, 5)
        tx, ty = b["top"]
        rect(img, tx, ty, 8, 4, suit, 5)
        tx, ty = b["bottom"]
        rect(img, tx, ty, 8, 4, suit, 5)

        # 팔 (오른팔 u40 v16 / 왼팔 u32 v48, 4x12x4) — 재킷 소매 + 손
        for u, v in ((40, 16), (32, 48)):
            a = box(img, u, v, 4, 12, 4, suit, 5)
            for face in ("front", "back", "right", "left"):
                x, y = a[face]
                rect(img, x, y + 10, 4, 2, skin, 4)

        # 다리 (오른다리 u0 v16 / 왼다리 u16 v48, 4x12x4) — 바지 + 검은 구두
        for u, v in ((0, 16), (16, 48)):
            l = box(img, u, v, 4, 12, 4, trouser, 4)
            for face in ("front", "back", "right", "left"):
                x, y = l[face]
                rect(img, x, y + 10, 4, 2, shoe, 3)
            tx, ty = l["bottom"]
            rect(img, tx, ty, 4, 4, shoe, 3)

        save(img, f"textures/entity/clerk_{n}.png")


clerks()
merge_lang({"entity.lobcorp.clerk": "사무직"},
           {"entity.lobcorp.clerk": "Clerk"})
print("assets clerks ok")
