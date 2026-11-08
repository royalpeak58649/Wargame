package kr.lobcorp;

import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.BreachingAbnormality;
import kr.lobcorp.ordeal.OrdealEntity;
import kr.lobcorp.ordeal.OrdealTier;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * 로보토미 코퍼레이션식 등급(ZAYIN~ALEPH)과 등급 차이 배율.
 * 공격자 등급이 수비자 등급보다 높으면 피해가 커지고, 낮으면 줄어든다.
 */
public final class Grades {
    private Grades() {}

    /** 등급 이름 (인덱스 = 등급) */
    public static final String[] NAMES = {"", "ZAYIN", "TETH", "HE", "WAW", "ALEPH"};

    /** 위험 등급 문자열 → 등급 숫자. 모르는 값은 0 */
    public static int of(String risk) {
        if (risk == null) return 0;
        return switch (risk) {
            case "ZAYIN" -> 1;
            case "TETH" -> 2;
            case "HE" -> 3;
            case "WAW" -> 4;
            case "ALEPH" -> 5;
            default -> 0;
        };
    }

    /** 등급 차이에 따른 피해 배율. 둘 중 하나라도 0이면 1 */
    public static float mult(int attacker, int defender) {
        if (attacker == 0 || defender == 0) return 1f;
        int diff = attacker - defender;
        if (diff <= -4) return 0.4f;
        if (diff == -3) return 0.6f;
        if (diff == -2) return 0.7f;
        if (diff == -1) return 0.8f;
        if (diff <= 1) return 1.0f;
        if (diff == 2) return 1.2f;
        if (diff == 3) return 1.5f;
        return 2.0f;
    }

    /** 아이템 레지스트리 경로 → 등급. 첫 사용 시 AbnoProfile에서 만든다 */
    private static Map<String, Integer> ITEM_GRADE;

    private static Map<String, Integer> itemGrade() {
        Map<String, Integer> m = ITEM_GRADE;
        if (m != null) return m;
        m = new HashMap<>();
        for (AbnoProfile p : AbnoProfile.BY_ID.values()) {
            int g = of(p.risk());
            putItem(m, p.weapon(), g);
            putItem(m, p.suit(), g);
        }
        m.put("twilight", 5);
        m.put("twilight_suit", 5);
        ITEM_GRADE = m;
        return m;
    }

    private static void putItem(Map<String, Integer> m, java.util.function.Supplier<Item> sup, int grade) {
        try {
            Item item = sup != null ? sup.get() : null;
            if (item == null) return;
            m.put(BuiltInRegistries.ITEM.getKey(item).getPath(), grade);
        } catch (Exception ignored) {
            // 공급자가 실패해도 나머지 등급표는 유지한다
        }
    }

    /** 아이템 경로로 등급을 찾는다. 없으면 0 */
    public static int ofPath(String path) {
        if (path == null) return 0;
        Integer g = itemGrade().get(path);
        return g != null ? g : 0;
    }

    /** 아이템의 등급. 없으면 0 */
    public static int ofItem(Item item) {
        if (item == null) return 0;
        return ofPath(BuiltInRegistries.ITEM.getKey(item).getPath());
    }

    /** 착용한 슈트(가슴)의 등급. E.G.O 슈트가 없으면 표준 제복 = ZAYIN(1) */
    public static int suitGrade(ServerPlayer p) {
        int g = ofItem(p.getItemBySlot(EquipmentSlot.CHEST).getItem());
        return g > 0 ? g : 1;
    }

    /** 엔티티의 등급. 해당 없으면 0 */
    public static int ofEntity(Entity e) {
        if (e == null) return 0;
        if (e instanceof AbnormalityEntity a) return of(a.profile().risk());
        if (e instanceof OrdealEntity o) {
            OrdealTier t = o.ordealType().tier;
            return switch (t) {
                case DAWN -> 2;
                case NOON -> 3;
                case DUSK -> 4;
            };
        }
        if (e instanceof kr.lobcorp.entity.CensoredMinionEntity
                || e instanceof kr.lobcorp.entity.MeltingSlimeEntity
                || e instanceof kr.lobcorp.entity.ApostleEntity
                || e instanceof kr.lobcorp.entity.ArmySoldierEntity
                || e instanceof kr.lobcorp.apocalypse.ApocalypseBirdEntity
                || e instanceof kr.lobcorp.apocalypse.ApocalypseEggEntity) return 5;
        if (e instanceof kr.lobcorp.entity.WorkerBeeEntity
                || e instanceof kr.lobcorp.entity.NakedWormEntity) return 4;
        if (e instanceof kr.lobcorp.entity.LittlePrinceMinionEntity
                || e instanceof kr.lobcorp.entity.LaetitiaFriendEntity) return 3;
        return 0;
    }

    /**
     * 지금 플레이어를 위협하는 대상의 등급.
     * 작업 중이면 그 환상체, 아니면 8블록 안에서 가장 가까운 적대 개체.
     */
    public static int threatGradeNear(ServerPlayer p) {
        WorkSession s = WorkManager.session(p);
        if (s != null && s.abno != null) return of(s.abno.profile().risk());
        int best = 0;
        double bd = Double.MAX_VALUE;
        for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(8.0))) {
            if (!e.isAlive()) continue;
            int g = ofEntity(e);
            if (g <= 0) continue;
            if (e instanceof BreachingAbnormality b) {
                if (!b.isBreached()) continue;
            } else if (e instanceof AbnormalityEntity) {
                continue; // 격리 중인 환상체는 위협이 아니다
            }
            double d = p.distanceToSqr(e);
            if (d > 64.0 || d >= bd) continue;
            bd = d;
            best = g;
        }
        return best;
    }
}
