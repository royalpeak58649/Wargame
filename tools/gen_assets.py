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


for item, parent in [("penitence", "minecraft:item/handheld"), ("regret", "minecraft:item/handheld"),
                     ("penitence_suit", "minecraft:item/generated"), ("regret_suit", "minecraft:item/generated")]:
    wjson(f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"lobcorp:item/{item}"}})
    wjson(f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"lobcorp:item/{item}"}})

for mat in ("penitence", "regret"):
    wjson(f"equipment/{mat}.json", {"layers": {"humanoid": [{"texture": f"lobcorp:{mat}"}]}})

wjson("lang/ko_kr.json", {
    "entity.lobcorp.one_sin": "단 한 번의 악과 수백 가지의 선행",
    "entity.lobcorp.forsaken_murderer": "버려진 살인마",
    "item.lobcorp.penitence": "참회",
    "item.lobcorp.penitence_suit": "참회 (방어구)",
    "item.lobcorp.regret": "후회",
    "item.lobcorp.regret_suit": "후회 (방어구)",
})
wjson("lang/en_us.json", {
    "entity.lobcorp.one_sin": "One Sin and Hundreds of Good Deeds",
    "entity.lobcorp.forsaken_murderer": "Forsaken Murderer",
    "item.lobcorp.penitence": "Penitence",
    "item.lobcorp.penitence_suit": "Penitence (Suit)",
    "item.lobcorp.regret": "Regret",
    "item.lobcorp.regret_suit": "Regret (Suit)",
})

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

wjson("lang/ko_kr.json", {
    "entity.lobcorp.one_sin": "단 한 번의 악과 수백 가지의 선행",
    "entity.lobcorp.forsaken_murderer": "버려진 살인마",
    "entity.lobcorp.scorched_girl": "불타버린 소녀",
    "entity.lobcorp.happy_teddy_bear": "행복한 테디베어",
    "entity.lobcorp.fairy_festival": "요정의 축제",
    "item.lobcorp.penitence": "참회",
    "item.lobcorp.penitence_suit": "참회 (방어구)",
    "item.lobcorp.regret": "후회",
    "item.lobcorp.regret_suit": "후회 (방어구)",
    "item.lobcorp.match_flame": "4번째 성냥불",
    "item.lobcorp.match_flame_suit": "4번째 성냥불 (방어구)",
    "item.lobcorp.bear_paws": "곰 발바닥",
    "item.lobcorp.bear_paws_suit": "곰 발바닥 (방어구)",
    "item.lobcorp.wingbeat": "날갯짓",
    "item.lobcorp.wingbeat_suit": "날갯짓 (방어구)",
})
wjson("lang/en_us.json", {
    "entity.lobcorp.one_sin": "One Sin and Hundreds of Good Deeds",
    "entity.lobcorp.forsaken_murderer": "Forsaken Murderer",
    "entity.lobcorp.scorched_girl": "Scorched Girl",
    "entity.lobcorp.happy_teddy_bear": "Happy Teddy Bear",
    "entity.lobcorp.fairy_festival": "Fairy Festival",
    "item.lobcorp.penitence": "Penitence",
    "item.lobcorp.penitence_suit": "Penitence (Suit)",
    "item.lobcorp.regret": "Regret",
    "item.lobcorp.regret_suit": "Regret (Suit)",
    "item.lobcorp.match_flame": "Fourth Match Flame",
    "item.lobcorp.match_flame_suit": "Fourth Match Flame (Suit)",
    "item.lobcorp.bear_paws": "Bear Paws",
    "item.lobcorp.bear_paws_suit": "Bear Paws (Suit)",
    "item.lobcorp.wingbeat": "Wingbeat",
    "item.lobcorp.wingbeat_suit": "Wingbeat (Suit)",
})
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

wjson("lang/ko_kr.json", {
    "entity.lobcorp.one_sin": "단 한 번의 악과 수백 가지의 선행",
    "entity.lobcorp.forsaken_murderer": "버려진 살인마",
    "entity.lobcorp.scorched_girl": "불타버린 소녀",
    "entity.lobcorp.happy_teddy_bear": "행복한 테디베어",
    "entity.lobcorp.fairy_festival": "요정의 축제",
    "entity.lobcorp.punishing_bird": "처벌새",
    "item.lobcorp.penitence": "참회",
    "item.lobcorp.penitence_suit": "참회 (방어구)",
    "item.lobcorp.regret": "후회",
    "item.lobcorp.regret_suit": "후회 (방어구)",
    "item.lobcorp.match_flame": "4번째 성냥불",
    "item.lobcorp.match_flame_suit": "4번째 성냥불 (방어구)",
    "item.lobcorp.bear_paws": "곰 발바닥",
    "item.lobcorp.bear_paws_suit": "곰 발바닥 (방어구)",
    "item.lobcorp.wingbeat": "날갯짓",
    "item.lobcorp.wingbeat_suit": "날갯짓 (방어구)",
    "item.lobcorp.beak": "부리",
    "item.lobcorp.beak_suit": "부리 (방어구)",
})
wjson("lang/en_us.json", {
    "entity.lobcorp.one_sin": "One Sin and Hundreds of Good Deeds",
    "entity.lobcorp.forsaken_murderer": "Forsaken Murderer",
    "entity.lobcorp.scorched_girl": "Scorched Girl",
    "entity.lobcorp.happy_teddy_bear": "Happy Teddy Bear",
    "entity.lobcorp.fairy_festival": "Fairy Festival",
    "entity.lobcorp.punishing_bird": "Punishing Bird",
    "item.lobcorp.penitence": "Penitence",
    "item.lobcorp.penitence_suit": "Penitence (Suit)",
    "item.lobcorp.regret": "Regret",
    "item.lobcorp.regret_suit": "Regret (Suit)",
    "item.lobcorp.match_flame": "Fourth Match Flame",
    "item.lobcorp.match_flame_suit": "Fourth Match Flame (Suit)",
    "item.lobcorp.bear_paws": "Bear Paws",
    "item.lobcorp.bear_paws_suit": "Bear Paws (Suit)",
    "item.lobcorp.wingbeat": "Wingbeat",
    "item.lobcorp.wingbeat_suit": "Wingbeat (Suit)",
    "item.lobcorp.beak": "Beak",
    "item.lobcorp.beak_suit": "Beak (Suit)",
})

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

    # 밧줄 (u0 v0, 14x1x1)
    box(img, 0, 0, 14, 1, 1, hexc("#8a7a5a"), 8)

    # 얼굴 판 다섯 개 (u = i*4, v = 8, 3x4x1)
    # 0: 활짝 웃는 얼굴
    f = box(img, 0, 8, 3, 4, 1, cloth, 5)
    fx, fy = f["front"]
    for x in range(3):
        px(img, fx + x, fy + 2, dark)
    px(img, fx, fy + 1, dark); px(img, fx + 2, fy + 1, dark)
    px(img, fx, fy + 3, dark); px(img, fx + 2, fy + 3, dark)
    px(img, fx + 1, fy + 3, dark)
    # 1: 웃는 얼굴
    f = box(img, 4, 8, 3, 4, 1, cloth, 5)
    fx, fy = f["front"]
    px(img, fx, fy + 1, dark); px(img, fx + 2, fy + 1, dark)
    for x in range(3):
        px(img, fx + x, fy + 2, dark)
    # 2: 무표정
    f = box(img, 8, 8, 3, 4, 1, cloth, 5)
    fx, fy = f["front"]
    px(img, fx, fy + 1, dark); px(img, fx + 2, fy + 1, dark)
    for x in range(3):
        px(img, fx + x, fy + 2, dark)
    # 3: 슬픈 얼굴 + 눈물
    f = box(img, 12, 8, 3, 4, 1, cloth, 5)
    fx, fy = f["front"]
    px(img, fx, fy + 1, dark); px(img, fx + 2, fy + 1, dark)
    px(img, fx, fy + 3, dark); px(img, fx + 2, fy + 3, dark)
    px(img, fx + 1, fy + 2, dark)
    px(img, fx, fy + 2, blue); px(img, fx + 2, fy + 2, blue)
    # 4: 화난 얼굴 + 벌린 입
    f = box(img, 16, 8, 3, 4, 1, cloth, 5)
    fx, fy = f["front"]
    px(img, fx, fy + 1, dark); px(img, fx + 2, fy + 1, dark)
    px(img, fx + 1, fy + 1, dark)
    px(img, fx + 1, fy + 2, red); px(img, fx + 1, fy + 3, red)

    # 천 뒤의 형체: 갈색 두루마기 (u0 v16, 4x8x3)
    b = box(img, 0, 16, 4, 8, 3, robe, 8)
    bx, by = b["front"]
    for y in range(8):
        px(img, bx + 1, by + y, robe_dark)
        px(img, bx + 2, by + y, robe_dark)
    # 머리 (u20 v16, 3x3x3)
    h = box(img, 20, 16, 3, 3, 3, robe, 8)
    hx, hy = h["front"]
    px(img, hx, hy + 1, dark); px(img, hx + 2, hy + 1, dark)
    # 다리 (u32 v16, 1x5x1)
    box(img, 32, 16, 1, 5, 1, leg, 8)

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

wjson("lang/ko_kr.json", {
    "entity.lobcorp.one_sin": "단 한 번의 악과 수백 가지의 선행",
    "entity.lobcorp.forsaken_murderer": "버려진 살인마",
    "entity.lobcorp.scorched_girl": "불타버린 소녀",
    "entity.lobcorp.happy_teddy_bear": "행복한 테디베어",
    "entity.lobcorp.fairy_festival": "요정의 축제",
    "entity.lobcorp.punishing_bird": "처벌새",
    "entity.lobcorp.todays_shy_look": "오늘의 수줍은 표정",
    "item.lobcorp.penitence": "참회",
    "item.lobcorp.penitence_suit": "참회 (방어구)",
    "item.lobcorp.regret": "후회",
    "item.lobcorp.regret_suit": "후회 (방어구)",
    "item.lobcorp.match_flame": "4번째 성냥불",
    "item.lobcorp.match_flame_suit": "4번째 성냥불 (방어구)",
    "item.lobcorp.bear_paws": "곰 발바닥",
    "item.lobcorp.bear_paws_suit": "곰 발바닥 (방어구)",
    "item.lobcorp.wingbeat": "날갯짓",
    "item.lobcorp.wingbeat_suit": "날갯짓 (방어구)",
    "item.lobcorp.beak": "부리",
    "item.lobcorp.beak_suit": "부리 (방어구)",
    "item.lobcorp.todays_expression": "오늘의 표정",
    "item.lobcorp.todays_expression_suit": "오늘의 표정 (방어구)",
})
wjson("lang/en_us.json", {
    "entity.lobcorp.one_sin": "One Sin and Hundreds of Good Deeds",
    "entity.lobcorp.forsaken_murderer": "Forsaken Murderer",
    "entity.lobcorp.scorched_girl": "Scorched Girl",
    "entity.lobcorp.happy_teddy_bear": "Happy Teddy Bear",
    "entity.lobcorp.fairy_festival": "Fairy Festival",
    "entity.lobcorp.punishing_bird": "Punishing Bird",
    "entity.lobcorp.todays_shy_look": "Today's Shy Look",
    "item.lobcorp.penitence": "Penitence",
    "item.lobcorp.penitence_suit": "Penitence (Suit)",
    "item.lobcorp.regret": "Regret",
    "item.lobcorp.regret_suit": "Regret (Suit)",
    "item.lobcorp.match_flame": "Fourth Match Flame",
    "item.lobcorp.match_flame_suit": "Fourth Match Flame (Suit)",
    "item.lobcorp.bear_paws": "Bear Paws",
    "item.lobcorp.bear_paws_suit": "Bear Paws (Suit)",
    "item.lobcorp.wingbeat": "Wingbeat",
    "item.lobcorp.wingbeat_suit": "Wingbeat (Suit)",
    "item.lobcorp.beak": "Beak",
    "item.lobcorp.beak_suit": "Beak (Suit)",
    "item.lobcorp.todays_expression": "Today's Expression",
    "item.lobcorp.todays_expression_suit": "Today's Expression (Suit)",
})

todays_shy_look()
print("assets shy ok")
