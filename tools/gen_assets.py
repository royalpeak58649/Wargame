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

wjson("lang/ko_kr.json", {
    "entity.lobcorp.one_sin": "단 한 번의 악과 수백 가지의 선행",
    "entity.lobcorp.forsaken_murderer": "버려진 살인마",
    "entity.lobcorp.scorched_girl": "불타버린 소녀",
    "entity.lobcorp.happy_teddy_bear": "행복한 테디베어",
    "entity.lobcorp.fairy_festival": "요정의 축제",
    "entity.lobcorp.punishing_bird": "처벌새",
    "entity.lobcorp.todays_shy_look": "오늘의 수줍은 표정",
    "entity.lobcorp.lady_facing_the_wall": "벽을 보는 여자",
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
    "item.lobcorp.screaming_wedge": "비명 쐐기",
    "item.lobcorp.screaming_wedge_suit": "비명 쐐기 (방어구)",
})
wjson("lang/en_us.json", {
    "entity.lobcorp.one_sin": "One Sin and Hundreds of Good Deeds",
    "entity.lobcorp.forsaken_murderer": "Forsaken Murderer",
    "entity.lobcorp.scorched_girl": "Scorched Girl",
    "entity.lobcorp.happy_teddy_bear": "Happy Teddy Bear",
    "entity.lobcorp.fairy_festival": "Fairy Festival",
    "entity.lobcorp.punishing_bird": "Punishing Bird",
    "entity.lobcorp.todays_shy_look": "Today's Shy Look",
    "entity.lobcorp.lady_facing_the_wall": "The Lady Facing the Wall",
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
    "item.lobcorp.screaming_wedge": "Screaming Wedge",
    "item.lobcorp.screaming_wedge_suit": "Screaming Wedge (Suit)",
})

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

wjson("lang/ko_kr.json", {
    "entity.lobcorp.one_sin": "단 한 번의 악과 수백 가지의 선행",
    "entity.lobcorp.forsaken_murderer": "버려진 살인마",
    "entity.lobcorp.scorched_girl": "불타버린 소녀",
    "entity.lobcorp.happy_teddy_bear": "행복한 테디베어",
    "entity.lobcorp.fairy_festival": "요정의 축제",
    "entity.lobcorp.punishing_bird": "처벌새",
    "entity.lobcorp.todays_shy_look": "오늘의 수줍은 표정",
    "entity.lobcorp.lady_facing_the_wall": "벽을 보는 여자",
    "entity.lobcorp.crumbling_armor": "부서지는 갑옷",
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
    "item.lobcorp.screaming_wedge": "비명 쐐기",
    "item.lobcorp.screaming_wedge_suit": "비명 쐐기 (방어구)",
    "item.lobcorp.daredevil": "만용",
    "item.lobcorp.daredevil_suit": "만용 (방어구)",
})
wjson("lang/en_us.json", {
    "entity.lobcorp.one_sin": "One Sin and Hundreds of Good Deeds",
    "entity.lobcorp.forsaken_murderer": "Forsaken Murderer",
    "entity.lobcorp.scorched_girl": "Scorched Girl",
    "entity.lobcorp.happy_teddy_bear": "Happy Teddy Bear",
    "entity.lobcorp.fairy_festival": "Fairy Festival",
    "entity.lobcorp.punishing_bird": "Punishing Bird",
    "entity.lobcorp.todays_shy_look": "Today's Shy Look",
    "entity.lobcorp.lady_facing_the_wall": "The Lady Facing the Wall",
    "entity.lobcorp.crumbling_armor": "Crumbling Armor",
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
    "item.lobcorp.screaming_wedge": "Screaming Wedge",
    "item.lobcorp.screaming_wedge_suit": "Screaming Wedge (Suit)",
    "item.lobcorp.daredevil": "Daredevil",
    "item.lobcorp.daredevil_suit": "Daredevil (Suit)",
})

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

wjson("lang/ko_kr.json", {
    "entity.lobcorp.one_sin": "단 한 번의 악과 수백 가지의 선행",
    "entity.lobcorp.forsaken_murderer": "버려진 살인마",
    "entity.lobcorp.scorched_girl": "불타버린 소녀",
    "entity.lobcorp.happy_teddy_bear": "행복한 테디베어",
    "entity.lobcorp.fairy_festival": "요정의 축제",
    "entity.lobcorp.punishing_bird": "처벌새",
    "entity.lobcorp.todays_shy_look": "오늘의 수줍은 표정",
    "entity.lobcorp.lady_facing_the_wall": "벽을 보는 여자",
    "entity.lobcorp.crumbling_armor": "부서지는 갑옷",
    "entity.lobcorp.old_lady": "늙은 여인",
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
    "item.lobcorp.screaming_wedge": "비명 쐐기",
    "item.lobcorp.screaming_wedge_suit": "비명 쐐기 (방어구)",
    "item.lobcorp.daredevil": "만용",
    "item.lobcorp.daredevil_suit": "만용 (방어구)",
    "item.lobcorp.solitude": "고독",
    "item.lobcorp.solitude_suit": "고독 (방어구)",
})
wjson("lang/en_us.json", {
    "entity.lobcorp.one_sin": "One Sin and Hundreds of Good Deeds",
    "entity.lobcorp.forsaken_murderer": "Forsaken Murderer",
    "entity.lobcorp.scorched_girl": "Scorched Girl",
    "entity.lobcorp.happy_teddy_bear": "Happy Teddy Bear",
    "entity.lobcorp.fairy_festival": "Fairy Festival",
    "entity.lobcorp.punishing_bird": "Punishing Bird",
    "entity.lobcorp.todays_shy_look": "Today's Shy Look",
    "entity.lobcorp.lady_facing_the_wall": "The Lady Facing the Wall",
    "entity.lobcorp.crumbling_armor": "Crumbling Armor",
    "entity.lobcorp.old_lady": "Old Lady",
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
    "item.lobcorp.screaming_wedge": "Screaming Wedge",
    "item.lobcorp.screaming_wedge_suit": "Screaming Wedge (Suit)",
    "item.lobcorp.daredevil": "Daredevil",
    "item.lobcorp.daredevil_suit": "Daredevil (Suit)",
    "item.lobcorp.solitude": "Solitude",
    "item.lobcorp.solitude_suit": "Solitude (Suit)",
})

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

wjson("lang/ko_kr.json", {
    "entity.lobcorp.one_sin": "단 한 번의 악과 수백 가지의 선행",
    "entity.lobcorp.forsaken_murderer": "버려진 살인마",
    "entity.lobcorp.scorched_girl": "불타버린 소녀",
    "entity.lobcorp.happy_teddy_bear": "행복한 테디베어",
    "entity.lobcorp.fairy_festival": "요정의 축제",
    "entity.lobcorp.punishing_bird": "처벌새",
    "entity.lobcorp.todays_shy_look": "오늘의 수줍은 표정",
    "entity.lobcorp.lady_facing_the_wall": "벽을 보는 여자",
    "entity.lobcorp.crumbling_armor": "부서지는 갑옷",
    "entity.lobcorp.old_lady": "늙은 여인",
    "entity.lobcorp.spider_bud": "거미 봉오리",
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
    "item.lobcorp.screaming_wedge": "비명 쐐기",
    "item.lobcorp.screaming_wedge_suit": "비명 쐐기 (방어구)",
    "item.lobcorp.daredevil": "만용",
    "item.lobcorp.daredevil_suit": "만용 (방어구)",
    "item.lobcorp.solitude": "고독",
    "item.lobcorp.solitude_suit": "고독 (방어구)",
    "item.lobcorp.red_eyes": "붉은 눈",
    "item.lobcorp.red_eyes_suit": "붉은 눈 (방어구)",
})
wjson("lang/en_us.json", {
    "entity.lobcorp.one_sin": "One Sin and Hundreds of Good Deeds",
    "entity.lobcorp.forsaken_murderer": "Forsaken Murderer",
    "entity.lobcorp.scorched_girl": "Scorched Girl",
    "entity.lobcorp.happy_teddy_bear": "Happy Teddy Bear",
    "entity.lobcorp.fairy_festival": "Fairy Festival",
    "entity.lobcorp.punishing_bird": "Punishing Bird",
    "entity.lobcorp.todays_shy_look": "Today's Shy Look",
    "entity.lobcorp.lady_facing_the_wall": "The Lady Facing the Wall",
    "entity.lobcorp.crumbling_armor": "Crumbling Armor",
    "entity.lobcorp.old_lady": "Old Lady",
    "entity.lobcorp.spider_bud": "Spider Bud",
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
    "item.lobcorp.screaming_wedge": "Screaming Wedge",
    "item.lobcorp.screaming_wedge_suit": "Screaming Wedge (Suit)",
    "item.lobcorp.daredevil": "Daredevil",
    "item.lobcorp.daredevil_suit": "Daredevil (Suit)",
    "item.lobcorp.solitude": "Solitude",
    "item.lobcorp.solitude_suit": "Solitude (Suit)",
    "item.lobcorp.red_eyes": "Red Eyes",
    "item.lobcorp.red_eyes_suit": "Red Eyes (Suit)",
})

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

wjson("lang/ko_kr.json", {
    "entity.lobcorp.one_sin": "단 한 번의 악과 수백 가지의 선행",
    "entity.lobcorp.forsaken_murderer": "버려진 살인마",
    "entity.lobcorp.scorched_girl": "불타버린 소녀",
    "entity.lobcorp.happy_teddy_bear": "행복한 테디베어",
    "entity.lobcorp.fairy_festival": "요정의 축제",
    "entity.lobcorp.punishing_bird": "처벌새",
    "entity.lobcorp.todays_shy_look": "오늘의 수줍은 표정",
    "entity.lobcorp.lady_facing_the_wall": "벽을 보는 여자",
    "entity.lobcorp.crumbling_armor": "부서지는 갑옷",
    "entity.lobcorp.old_lady": "늙은 여인",
    "entity.lobcorp.spider_bud": "거미 봉오리",
    "entity.lobcorp.void_dream": "공허한 꿈",
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
    "item.lobcorp.screaming_wedge": "비명 쐐기",
    "item.lobcorp.screaming_wedge_suit": "비명 쐐기 (방어구)",
    "item.lobcorp.daredevil": "만용",
    "item.lobcorp.daredevil_suit": "만용 (방어구)",
    "item.lobcorp.solitude": "고독",
    "item.lobcorp.solitude_suit": "고독 (방어구)",
    "item.lobcorp.red_eyes": "붉은 눈",
    "item.lobcorp.red_eyes_suit": "붉은 눈 (방어구)",
    "item.lobcorp.engulfing_dream": "집어삼키는 꿈",
    "item.lobcorp.engulfing_dream_suit": "집어삼키는 꿈 (방어구)",
})
wjson("lang/en_us.json", {
    "entity.lobcorp.one_sin": "One Sin and Hundreds of Good Deeds",
    "entity.lobcorp.forsaken_murderer": "Forsaken Murderer",
    "entity.lobcorp.scorched_girl": "Scorched Girl",
    "entity.lobcorp.happy_teddy_bear": "Happy Teddy Bear",
    "entity.lobcorp.fairy_festival": "Fairy Festival",
    "entity.lobcorp.punishing_bird": "Punishing Bird",
    "entity.lobcorp.todays_shy_look": "Today's Shy Look",
    "entity.lobcorp.lady_facing_the_wall": "The Lady Facing the Wall",
    "entity.lobcorp.crumbling_armor": "Crumbling Armor",
    "entity.lobcorp.old_lady": "Old Lady",
    "entity.lobcorp.spider_bud": "Spider Bud",
    "entity.lobcorp.void_dream": "Void Dream",
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
    "item.lobcorp.screaming_wedge": "Screaming Wedge",
    "item.lobcorp.screaming_wedge_suit": "Screaming Wedge (Suit)",
    "item.lobcorp.daredevil": "Daredevil",
    "item.lobcorp.daredevil_suit": "Daredevil (Suit)",
    "item.lobcorp.solitude": "Solitude",
    "item.lobcorp.solitude_suit": "Solitude (Suit)",
    "item.lobcorp.red_eyes": "Red Eyes",
    "item.lobcorp.red_eyes_suit": "Red Eyes (Suit)",
    "item.lobcorp.engulfing_dream": "Engulfing Dream",
    "item.lobcorp.engulfing_dream_suit": "Engulfing Dream (Suit)",
})

void_dream()
print("assets void ok")
