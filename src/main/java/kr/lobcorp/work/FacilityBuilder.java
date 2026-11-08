package kr.lobcorp.work;

import kr.lobcorp.ModEntities;
import kr.lobcorp.entity.AbnormalityEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 로보토미 코퍼레이션 시설 전체를 짓는다.
 *
 * 층(위→아래): 상층(중앙 본부 위) 4개 부서 / 중층 3개 / 하층 3개. 층 간격 14블록.
 * 부서 = 중앙 홀(15x15) + 북·남 복도(폭 3) + 복도 동쪽에 늘어선 격리실(RoomBuilder, 문이 복도를 향함).
 * 같은 층 부서끼리는 동서 통로로, 층과 층은 계단실로 잇는다(상→중: 서쪽, 중→하: 동쪽).
 */
public final class FacilityBuilder {
    private FacilityBuilder() {}

    public static final int FLOOR_GAP = 14;
    public static final int DEPT_SPACING = 48;
    private static final int UNIT_SPACING = 10;

    public record Unit(String id, EntityType<? extends AbnormalityEntity> type, boolean corner) {}

    record Dept(String name, String sephirah, DyeColor color, List<Unit> north, List<Unit> south) {}

    private static Unit u(String id, EntityType<? extends AbnormalityEntity> t) { return new Unit(id, t, false); }
    private static Unit c(String id, EntityType<? extends AbnormalityEntity> t) { return new Unit(id, t, true); }

    /** 층별 부서 배치. 위층일수록 낮은 등급(원작의 진행 순서와 비슷하게). */
    static List<List<Dept>> layout() {
        List<Dept> upper = List.of(
                new Dept("중앙제어팀", "말쿠트", DyeColor.YELLOW,
                        List.of(u("one_sin", ModEntities.ONE_SIN), u("fairy_festival", ModEntities.FAIRY_FESTIVAL)),
                        List.of(c("forsaken_murderer", ModEntities.FORSAKEN_MURDERER), c("scorched_girl", ModEntities.SCORCHED_GIRL))),
                new Dept("정보팀", "예소드", DyeColor.PURPLE,
                        List.of(u("punishing_bird", ModEntities.PUNISHING_BIRD), u("todays_shy_look", ModEntities.TODAYS_SHY_LOOK)),
                        List.of(c("lady_facing_the_wall", ModEntities.LADY_FACING_THE_WALL), u("crumbling_armor", ModEntities.CRUMBLING_ARMOR))),
                new Dept("안전팀", "네짜흐", DyeColor.LIME,
                        List.of(u("old_lady", ModEntities.OLD_LADY), u("spider_bud", ModEntities.SPIDER_BUD)),
                        List.of(u("void_dream", ModEntities.VOID_DREAM), u("mhz", ModEntities.MHZ))),
                new Dept("교육팀", "호드", DyeColor.ORANGE,
                        List.of(u("beauty_and_the_beast", ModEntities.BEAUTY_BEAST), u("meat_lantern", ModEntities.MEAT_LANTERN)),
                        List.of(u("fragment_of_universe", ModEntities.FRAGMENT), u("happy_teddy_bear", ModEntities.HAPPY_TEDDY))));
        List<Dept> middle = List.of(
                new Dept("중앙본부", "티페리트", DyeColor.WHITE,
                        List.of(u("red_shoes", ModEntities.RED_SHOES), u("singing_machine", ModEntities.SINGING_MACHINE),
                                u("porccubus", ModEntities.PORCCUBUS), u("laetitia", ModEntities.LAETITIA)),
                        List.of(u("all_around_helper", ModEntities.ALL_AROUND_HELPER), u("child_galaxy", ModEntities.CHILD_GALAXY),
                                u("freischutz", ModEntities.FREISCHUTZ), u("nameless_fetus", ModEntities.NAMELESS_FETUS))),
                new Dept("징계팀", "게부라", DyeColor.RED,
                        List.of(u("snow_queen", ModEntities.SNOW_QUEEN), u("woodsman", ModEntities.WOODSMAN), u("scarecrow", ModEntities.SCARECROW)),
                        List.of(u("rudolta", ModEntities.RUDOLTA), u("funeral", ModEntities.FUNERAL), u("schadenfreude", ModEntities.SCHADENFREUDE))),
                new Dept("복지팀", "헤세드", DyeColor.LIGHT_BLUE,
                        List.of(u("tree_sap", ModEntities.TREE_SAP), u("shelter", ModEntities.SHELTER)),
                        List.of(u("portrait", ModEntities.PORTRAIT), u("notes", ModEntities.NOTES))));
        List<Dept> lower = List.of(
                new Dept("추출팀", "비나", DyeColor.BROWN,
                        List.of(u("big_bird", ModEntities.BIG_BIRD), u("judgement_bird", ModEntities.JUDGEMENT_BIRD), u("dreaming_current", ModEntities.DREAMING_CURRENT), u("firebird", ModEntities.FIREBIRD)),
                        List.of(u("queen_of_hatred", ModEntities.QUEEN_HATRED), u("knight_of_despair", ModEntities.KNIGHT_DESPAIR), u("burrowing_heaven", ModEntities.BURROWING_HEAVEN))),
                new Dept("기록팀", "호크마", DyeColor.GRAY,
                        List.of(u("king_of_greed", ModEntities.KING_GREED), u("little_red", ModEntities.LITTLE_RED), u("clouded_monk", ModEntities.CLOUDED_MONK)),
                        List.of(u("big_bad_wolf", ModEntities.BIG_BAD_WOLF), u("alriune", ModEntities.ALRIUNE))),
                new Dept("설계팀", "케테르", DyeColor.BLACK,
                        List.of(u("queen_bee", ModEntities.QUEEN_BEE), u("yin", ModEntities.YIN),
                                u("snow_white_apple", ModEntities.SNOW_WHITE_APPLE), u("parasite_tree", ModEntities.PARASITE_TREE)),
                        List.of(u("black_swan", ModEntities.BLACK_SWAN), u("yang", ModEntities.YANG), u("little_prince", ModEntities.LITTLE_PRINCE), u("naked_nest", ModEntities.NAKED_NEST))));
        return List.of(upper, middle, lower);
    }

    public record Result(BlockPos spawn, int units, int topY, List<FacilityData.Slot> slots) {}

    /** top = 상층 바닥 위 첫 칸(서 있는 높이). 하층이 월드 바닥 아래로 내려가면 전체를 위로 올린다. */
    public static Result build(ServerLevel level, BlockPos requested, boolean fill) {
        List<FacilityData.Slot> slots = new ArrayList<>();
        int topY = requested.getY();
        int lowest = topY - 2 * FLOOR_GAP - 2;
        if (lowest < level.getMinY() + 1) topY += (level.getMinY() + 1) - lowest;
        int ox = requested.getX(), oz = requested.getZ();

        List<List<Dept>> floors = layout();
        int units = 0;
        for (int f = 0; f < floors.size(); f++) {
            int fy = topY - f * FLOOR_GAP;
            List<Dept> depts = floors.get(f);
            for (int i = 0; i < depts.size(); i++) {
                int cx = ox + i * DEPT_SPACING;
                boolean westDoor = i > 0 || f == 0 || f == 1;         // 상층·중층 첫 부서는 서쪽 계단실과 연결
                boolean eastDoor = i < depts.size() - 1 || f == 1 || f == 2; // 중층·하층 마지막 부서는 동쪽 계단실과 연결
                units += department(level, cx, fy, oz, depts.get(i), f, westDoor, eastDoor, fill, slots);
                if (i < depts.size() - 1) corridorX(level, cx + 8, cx + DEPT_SPACING - 8, fy, oz, depts.get(i).color());
            }
        }
        // 상층 → 중층: 첫 부서 서쪽, 중층 → 하층: 마지막 부서 동쪽
        stairwell(level, ox, topY, oz, -1);
        stairwell(level, ox + (floors.get(1).size() - 1) * DEPT_SPACING, topY - FLOOR_GAP, oz, +1);
        return new Result(new BlockPos(ox + 3, topY, oz + 3), units, topY, slots);
    }

    // ───────────────────────── 부서 ─────────────────────────

    private static int department(ServerLevel level, int cx, int fy, int oz, Dept d, int floor, boolean westDoor, boolean eastDoor,
                                  boolean fill, List<FacilityData.Slot> slots) {
        BlockState wall = Blocks.CONCRETE.white().defaultBlockState();
        BlockState band = Blocks.CONCRETE.pick(d.color()).defaultBlockState();
        BlockState glass = Blocks.STAINED_GLASS.pick(d.color()).defaultBlockState();
        // 중앙 홀: x ±7, z ±7, 바닥 fy-1, 천장 fy+6
        box(level, cx - 7, fy - 1, oz - 7, cx + 7, fy + 6, oz + 7, wall, true);
        for (int x = cx - 6; x <= cx + 6; x++)
            for (int z = oz - 6; z <= oz + 6; z++) {
                boolean ring = Math.abs(x - cx) == 5 || Math.abs(z - oz) == 5;
                set(level, x, fy - 1, z, ring ? band : Blocks.POLISHED_ANDESITE.defaultBlockState());
                set(level, x, fy + 6, z, Blocks.SMOOTH_QUARTZ.defaultBlockState());
            }
        for (int a = -7; a <= 7; a++) {          // 벽 띠
            set(level, cx + a, fy + 4, oz - 7, band); set(level, cx + a, fy + 4, oz + 7, band);
            set(level, cx - 7, fy + 4, oz + a, band); set(level, cx + 7, fy + 4, oz + a, band);
        }
        for (int sx : new int[]{-4, 0, 4}) for (int sz : new int[]{-4, 0, 4})
            if (sx != 0 || sz != 0) set(level, cx + sx, fy + 6, oz + sz, Blocks.SEA_LANTERN.defaultBlockState());
        // 가운데 기둥: 부서 색 유리 + 조명
        for (int y = fy; y <= fy + 4; y++) set(level, cx, y, oz, glass);
        set(level, cx, fy + 5, oz, Blocks.SEA_LANTERN.defaultBlockState());
        String floorName = floor == 0 ? "상층" : floor == 1 ? "중층" : "하층";
        sign(level, cx, fy + 1, oz - 1, Direction.NORTH, d.name(), d.sephirah(), floorName, "");
        sign(level, cx, fy + 1, oz + 1, Direction.SOUTH, d.name(), d.sephirah(), floorName, "");
        sign(level, cx - 1, fy + 1, oz, Direction.WEST, d.name(), d.sephirah(), floorName, "");
        sign(level, cx + 1, fy + 1, oz, Direction.EAST, d.name(), d.sephirah(), floorName, "");

        // 출입구 (폭 3, 높이 3)
        opening(level, cx - 1, fy, oz - 7, cx + 1, fy + 2, oz - 7);   // 북 복도
        opening(level, cx - 1, fy, oz + 7, cx + 1, fy + 2, oz + 7);   // 남 복도
        if (westDoor) opening(level, cx - 7, fy, oz - 1, cx - 7, fy + 2, oz + 1);
        if (eastDoor) opening(level, cx + 7, fy, oz - 1, cx + 7, fy + 2, oz + 1);

        int n = 0;
        n += hallway(level, cx, fy, oz, d, d.north(), -1, fill, slots);
        n += hallway(level, cx, fy, oz, d, d.south(), +1, fill, slots);
        return n;
    }

    /** dir = -1 북쪽(z 감소), +1 남쪽. 복도 내부 x cx-1..cx+1, 격리실은 복도 동쪽(문이 서쪽). */
    private static int hallway(ServerLevel level, int cx, int fy, int oz, Dept d, List<Unit> units, int dir,
                               boolean fill, List<FacilityData.Slot> slots) {
        int len = 1 + units.size() * UNIT_SPACING;               // 중앙 홀 벽 바깥부터 끝벽까지
        int zStart = oz + dir * 8, zEnd = oz + dir * (7 + len);
        int z0 = Math.min(zStart, zEnd), z1 = Math.max(zStart, zEnd);
        BlockState band = Blocks.CONCRETE.pick(d.color()).defaultBlockState();
        box(level, cx - 2, fy - 1, z0, cx + 2, fy + 4, z1, Blocks.CONCRETE.lightGray().defaultBlockState(), true);
        for (int z = z0; z <= z1; z++) {
            for (int x = cx - 1; x <= cx + 1; x++) set(level, x, fy - 1, z, Blocks.POLISHED_ANDESITE.defaultBlockState());
            set(level, cx - 2, fy + 3, z, band);                  // 서쪽 벽 색 띠
            if (Math.floorMod(z - oz, 5) == 0) set(level, cx, fy + 4, z, Blocks.SEA_LANTERN.defaultBlockState());
        }
        // 끝벽은 막혀 있고, 중앙 홀 쪽 끝(zStart)은 열려 있어야 한다
        for (int x = cx - 1; x <= cx + 1; x++) for (int y = fy; y <= fy + 3; y++) set(level, x, y, zStart, Blocks.AIR.defaultBlockState());
        int placed = 0;
        for (int k = 0; k < units.size(); k++) {
            Unit unit = units.get(k);
            int uz = oz + dir * (13 + k * UNIT_SPACING);
            BlockPos origin = new BlockPos(cx, fy, uz);
            RoomBuilder.shell(level, origin);
            if (!fill) {
                slots.add(new FacilityData.Slot(origin, d.name(), ""));
                labelSlot(level, origin, null, d.name());
                continue;
            }
            AbnormalityEntity a = RoomBuilder.spawn(level, origin, unit.type(), unit.corner());
            if (a == null) continue;
            placed++;
            slots.add(new FacilityData.Slot(origin, d.name(), unit.id()));
            labelSlot(level, origin, a.profile(), d.name());
        }
        return placed;
    }

    /** 같은 층 부서 사이 동서 통로 (내부 z ±1). */
    private static void corridorX(ServerLevel level, int x0, int x1, int fy, int oz, DyeColor color) {
        box(level, x0, fy - 1, oz - 2, x1, fy + 4, oz + 2, Blocks.CONCRETE.lightGray().defaultBlockState(), true);
        for (int x = x0; x <= x1; x++) {
            for (int z = oz - 1; z <= oz + 1; z++) set(level, x, fy - 1, z, Blocks.POLISHED_ANDESITE.defaultBlockState());
            set(level, x, fy - 1, oz, Blocks.CONCRETE.pick(color).defaultBlockState());   // 가운데 색 줄
            if (Math.floorMod(x, 6) == 0) set(level, x, fy + 4, oz, Blocks.SEA_LANTERN.defaultBlockState());
        }
        // 양 끝은 홀 출입구와 맞닿도록 열어 둔다
        for (int z = oz - 1; z <= oz + 1; z++) for (int y = fy; y <= fy + 3; y++) {
            set(level, x0, y, z, Blocks.AIR.defaultBlockState());
            set(level, x1, y, z, Blocks.AIR.defaultBlockState());
        }
    }

    /**
     * 위층(fy) 부서 홀에서 아래층(fy-14) 같은 위치 부서 홀로 내려가는 계단실.
     * side -1: 홀 서쪽, +1: 홀 동쪽. 위층 통로는 바깥 레인(홀에서 먼 쪽), 아래층 통로는 안쪽 레인을 쓴다.
     */
    private static void stairwell(ServerLevel level, int cx, int fy, int oz, int side) {
        BlockState shell = Blocks.CONCRETE.lightGray().defaultBlockState();
        BlockState floor = Blocks.POLISHED_ANDESITE.defaultBlockState();
        int lowY = fy - FLOOR_GAP;
        int outer = cx + side * 15;   // 바깥 레인 중심 x
        int inner = cx + side * 11;   // 안쪽 레인 중심 x
        // ① 위층: 홀 옆문 → 바깥 레인까지 동서 통로
        hall(level, Math.min(cx + side * 8, outer), fy, oz - 1, Math.max(cx + side * 8, outer), oz + 1, shell, floor);
        // ② 위층: 바깥 레인을 따라 남쪽으로
        hall(level, outer - 1, fy, oz - 1, outer + 1, oz + 20, shell, floor);
        // ④ 아래층 착지: 두 레인을 잇는 넓은 칸
        hall(level, Math.min(outer, inner) - 1, lowY, oz + 35, Math.max(outer, inner) + 1, oz + 37, shell, floor);
        // ⑤ 아래층: 안쪽 레인을 따라 북쪽으로 돌아와
        hall(level, inner - 1, lowY, oz - 1, inner + 1, oz + 37, shell, floor);
        // ⑥ 아래층: 홀 옆문까지
        hall(level, Math.min(cx + side * 8, inner), lowY, oz - 1, Math.max(cx + side * 8, inner), oz + 1, shell, floor);
        // ③ 계단 (착지·아래 통로를 먼저 지은 뒤에 깔아야 착지 벽이 마지막 계단을 덮지 않는다): z = oz+21 .. oz+34, 한 칸마다 1블록씩 내려감
        for (int k = 0; k < FLOOR_GAP; k++) {
            int z = oz + 21 + k, y = fy - 1 - k;
            for (int x = outer - 2; x <= outer + 2; x++) for (int yy = y - 1; yy <= y + 5; yy++) {
                boolean wall = x == outer - 2 || x == outer + 2 || yy == y - 1 || yy == y + 5;
                set(level, x, yy, z, wall ? shell : Blocks.AIR.defaultBlockState());
            }
            BlockState step = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            for (int x = outer - 1; x <= outer + 1; x++) set(level, x, y, z, step);
            if (k % 4 == 0) set(level, outer, y + 5, z, Blocks.SEA_LANTERN.defaultBlockState());
        }
        // 이음매 뚫기: 위층 ①↔②, 계단 시작, 아래층 ④↔⑤↔⑥
        clear(level, Math.min(cx + side * 8, outer), fy, oz - 1, Math.max(cx + side * 8, outer), fy + 3, oz + 1);
        clear(level, outer - 1, fy, oz - 1, outer + 1, fy + 3, oz + 21);
        clear(level, Math.min(outer, inner) - 1, lowY, oz + 35, Math.max(outer, inner) + 1, lowY + 3, oz + 37);
        clear(level, inner - 1, lowY, oz - 1, inner + 1, lowY + 3, oz + 37);
        clear(level, Math.min(cx + side * 8, inner), lowY, oz - 1, Math.max(cx + side * 8, inner), lowY + 3, oz + 1);
        // 홀 옆문은 통로 벽에 덮이므로 다시 연다
        clear(level, cx + side * 7, fy, oz - 1, cx + side * 7, fy + 2, oz + 1);
        clear(level, cx + side * 7, lowY, oz - 1, cx + side * 7, lowY + 2, oz + 1);
        sign(level, cx + side * 9, fy + 2, oz - 1, Direction.SOUTH, "▼ 계단", "아래층으로", "", "");
        sign(level, cx + side * 9, lowY + 2, oz - 1, Direction.SOUTH, "▲ 계단", "위층으로", "", "");
    }

    /** 바닥(y-1)·벽·천장(y+4)을 갖춘 직육면체 통로, 내부 x0..x1 / z0..z1. 이음매는 호출 쪽에서 clear로 뚫는다. */
    private static void hall(ServerLevel level, int x0, int y, int z0, int x1, int z1, BlockState shell, BlockState floor) {
        for (int x = x0 - 1; x <= x1 + 1; x++)
            for (int z = z0 - 1; z <= z1 + 1; z++)
                for (int yy = y - 1; yy <= y + 4; yy++) {
                    boolean edge = x < x0 || x > x1 || z < z0 || z > z1 || yy == y + 4;
                    set(level, x, yy, z, yy == y - 1 ? (edge ? shell : floor) : edge ? shell : Blocks.AIR.defaultBlockState());
                }
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++)
            if (Math.floorMod(x + z, 6) == 0) set(level, x, y + 4, z, Blocks.SEA_LANTERN.defaultBlockState());
    }

    /** 격리실 문 맞은편(복도 서쪽 벽)의 표찰. profile == null 이면 빈 격리실. */
    public static void labelSlot(ServerLevel level, BlockPos origin, kr.lobcorp.AbnoProfile p, String dept) {
        int x = origin.getX() - 1, y = origin.getY() + 1, z = origin.getZ();
        if (p == null) sign(level, x, y, z, Direction.EAST, "빈 격리실", dept, "", "");
        else sign(level, x, y, z, Direction.EAST, p.code(), p.name(), p.risk(), dept);
    }

    /** 모든 부서 배치를 한 줄로 편 목록 (환상체 id → 엔티티 종류·구석 배치). */
    public static java.util.Map<String, Unit> catalog() {
        java.util.Map<String, Unit> m = new java.util.LinkedHashMap<>();
        for (List<Dept> floor : layout()) for (Dept d : floor) {
            for (Unit x : d.north()) m.put(x.id(), x);
            for (Unit x : d.south()) m.put(x.id(), x);
        }
        return m;
    }

    // ───────────────────────── 도우미 ─────────────────────────

    /** hollow=true면 껍데기만 채우고 속은 공기. */
    private static void box(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, BlockState shell, boolean hollow) {
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++) {
                    boolean edge = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
                    set(level, x, y, z, edge || !hollow ? shell : Blocks.AIR.defaultBlockState());
                }
    }

    private static void opening(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1) {
        clear(level, x0, y0, z0, x1, y1, z1);
    }

    private static void clear(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++)
            set(level, x, y, z, Blocks.AIR.defaultBlockState());
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState s) {
        level.setBlock(new BlockPos(x, y, z), s, 2);
    }

    /** facing = 표지판 글씨가 바라보는 방향. 표지판은 그 반대쪽 블록에 붙는다. */
    private static void sign(ServerLevel level, int x, int y, int z, Direction facing, String l1, String l2, String l3, String l4) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos.relative(facing.getOpposite())).isAir()) return;
        level.setBlock(pos, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), 2);
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            List<Component> lines = new ArrayList<>();
            for (String s : new String[]{l1, l2, l3, l4}) lines.add(Component.literal(s));
            SignText text = new SignText(lines, lines, DyeColor.WHITE, true);
            sign.setText(text, SignTextSlot.FRONT);
            sign.setChanged();
            level.sendBlockUpdated(pos, sign.getBlockState(), sign.getBlockState(), 3);
        }
    }
    /** 이미 지어진 시설의 부서 표지판에서 세피라 이름을 현재 배치에 맞게 고친다. 고친 표지판 수를 돌려준다. */
    public static int fixSephirahSigns(ServerLevel level, List<FacilityData.Slot> slots) {
        if (slots.isEmpty()) return 0;
        java.util.Map<String, String> seph = new java.util.HashMap<>();
        for (List<Dept> floor : layout()) for (Dept d : floor) seph.put(d.name(), d.sephirah());
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (FacilityData.Slot sl : slots) {
            minX = Math.min(minX, sl.origin.getX()); maxX = Math.max(maxX, sl.origin.getX());
            minZ = Math.min(minZ, sl.origin.getZ()); maxZ = Math.max(maxZ, sl.origin.getZ());
        }
        int fixed = 0;
        for (int cx = (minX - 48) >> 4; cx <= (maxX + 48) >> 4; cx++) {
            for (int cz = (minZ - 48) >> 4; cz <= (maxZ + 48) >> 4; cz++) {
                var chunk = level.getChunk(cx, cz);
                for (var be : new ArrayList<>(chunk.getBlockEntities().values())) {
                    if (!(be instanceof SignBlockEntity sign)) continue;
                    SignText t = sign.getText(SignTextSlot.FRONT);
                    List<Component> lines = new ArrayList<>(t.getMessages(false));
                    String want = seph.get(lines.get(0).getString());
                    if (want == null || lines.get(1).getString().equals(want)) continue;
                    lines.set(1, Component.literal(want));
                    sign.setText(new SignText(lines, lines, DyeColor.WHITE, true), SignTextSlot.FRONT);
                    sign.setChanged();
                    level.sendBlockUpdated(sign.getBlockPos(), sign.getBlockState(), sign.getBlockState(), 3);
                    fixed++;
                }
            }
        }
        return fixed;
    }
}
