package kr.lobcorp.item;

import kr.lobcorp.DamageColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;

/** E.G.O 슈트별 피해 내성 배율 (원작 기준). 순서는 RED, WHITE, BLACK, PALE */
public final class SuitResist {
    private SuitResist() {}

    /** 배율을 다시 적용할 때 재귀를 막는 가드 */
    public static boolean bypass = false;

    /** E.G.O 슈트 없음 = 표준 제복 */
    public static final float[] DEFAULT = {1.0f, 1.0f, 1.5f, 2.0f};

    private static final java.util.Map<String, float[]> TABLE = new java.util.HashMap<>();

    static {
        put("penitence_suit", .9f, .8f, .9f, 2.0f);
        put("regret_suit", .7f, 1.2f, .8f, 2.0f);
        put("match_flame_suit", .6f, 1.0f, 1.2f, 2.0f);
        put("bear_paws_suit", .8f, 1.0f, 1.0f, 1.5f);
        put("wingbeat_suit", .8f, .8f, 1.0f, 2.0f);
        put("beak_suit", .7f, .8f, 1.2f, 2.0f);
        put("todays_expression_suit", .7f, .6f, 1.5f, 2.0f);
        put("screaming_wedge_suit", 1.2f, .6f, 1.0f, 2.0f);
        put("daredevil_suit", .6f, .9f, .9f, 2.0f);
        put("solitude_suit", 1.5f, .8f, .8f, 2.0f);
        put("red_eyes_suit", .8f, .8f, .8f, 2.0f);
        put("engulfing_dream_suit", 1.2f, .8f, .7f, 2.0f);
        put("horn_suit", .8f, .8f, 1.5f, 2.0f);
        put("lantern_suit", .8f, .7f, 1.2f, 2.0f);
        put("fragments_suit", 1.0f, 1.2f, .6f, 2.0f);
        put("noise_suit", 1.2f, .7f, .6f, 2.0f);
        put("sanguine_desire_suit", .5f, 1.2f, .8f, 1.5f);
        put("harmony_suit", .8f, .6f, 1.0f, 2.0f);
        put("pleasure_suit", 1.2f, .8f, .8f, 1.5f);
        put("laetitia_suit", .7f, .7f, .7f, 1.5f);
        put("grinder_suit", .6f, 1.3f, .9f, 1.5f);
        put("our_galaxy_suit", .8f, .8f, 1.2f, 1.5f);
        put("magic_bullet_suit", .7f, .7f, .7f, 1.5f);
        put("syrinx_suit", 1.2f, .5f, .8f, 1.5f);
        put("frost_splinter_suit", 1.3f, .6f, .8f, 1.5f);
        put("logging_suit", .8f, 1.2f, .8f, 1.5f);
        put("harvest_suit", .6f, .8f, 1.3f, 1.5f);
        put("christmas_suit", .8f, .6f, 1.3f, 1.5f);
        put("solemn_lament_suit", 1.2f, .8f, .5f, 1.5f);
        put("gaze_suit", 1.0f, .8f, 1.0f, 1.5f);
        put("lamp_suit", .8f, .7f, .4f, 1.5f);
        put("justitia_suit", .5f, .5f, .5f, .5f);
        put("love_and_hate_suit", .7f, .8f, .4f, 2.0f);
        put("tears_sword_suit", .8f, .8f, .8f, .8f);
        put("gold_rush_suit", .4f, .7f, .8f, 2.0f);
        put("crimson_scar_suit", .6f, .6f, .6f, 1.5f);
        put("cobalt_scar_suit", .4f, .8f, .7f, 2.0f);
        put("faint_aroma_suit", 1.2f, .6f, .8f, 1.5f);
        put("hornet_suit", .7f, .7f, .7f, 1.5f);
        put("black_swan_suit", .6f, 1.2f, .8f, 1.5f);
        put("discord_suit", 1.2f, .8f, .6f, 1.5f);
        put("green_stem_suit", .8f, 1.2f, .6f, 1.5f);
        put("spore_suit", .8f, .6f, 1.2f, 1.5f);
        put("hypocrisy_suit", .7f, .5f, 1.3f, 1.5f);
        put("exuviae_suit", .6f, .8f, 1.2f, 1.5f);
        put("ecstasy_suit", .8f, .8f, .8f, 1.5f);
        put("heaven_suit", 1.2f, .8f, .6f, 1.5f);
        put("feather_of_honor_suit", .6f, .6f, 1.3f, 2.0f);
        put("amita_suit", .5f, 1.3f, .7f, 1.5f);
        put("moonlight_suit", .8f, .4f, .7f, 2.0f);
        put("diffraction_suit", 1.0f, .6f, 1.0f, 1.5f);
        put("mimicry_suit", .2f, .5f, .5f, 1.0f);
        put("da_capo_suit", .5f, .2f, .5f, 1.5f);
        put("smile_suit", .5f, .5f, .2f, 1.0f);
        put("censored_suit", .5f, .5f, .3f, 1.0f);
        put("sound_of_a_star_suit", .4f, .4f, .4f, 1.0f);
        put("adoration_suit", .3f, .6f, .3f, 1.0f);
        put("pink_suit", .5f, .3f, .4f, 1.0f);
        put("paradise_lost_suit", .5f, .5f, .5f, .3f);
        put("twilight_suit", .3f, .3f, .3f, .5f);
    }

    private static void put(String id, float red, float white, float black, float pale) {
        TABLE.put(id, new float[]{red, white, black, pale});
    }

    public static float[] of(String itemId) {
        float[] v = TABLE.get(itemId);
        return v != null ? v : DEFAULT;
    }

    public static float[] of(ServerPlayer p) {
        String id = BuiltInRegistries.ITEM.getKey(p.getItemBySlot(EquipmentSlot.CHEST).getItem()).getPath();
        return of(id);
    }

    /** 슈트 내성 × 등급 차이 배율 (작업 피해·SP 피해·PALE·RED 재적용 모두 이 경로를 쓴다) */
    public static float mult(ServerPlayer p, DamageColor c) {
        return baseMult(p, c) * kr.lobcorp.Grades.mult(kr.lobcorp.Grades.threatGradeNear(p), kr.lobcorp.Grades.suitGrade(p));
    }

    /**
     * 플레이어가 아닌 개체(관리직 등)가 입는 작업 피해 배율.
     * 가슴 슈트의 내성표 × 등급 차이 배율(공격자 등급 vs 슈트 등급).
     */
    public static float itemMult(net.minecraft.world.item.ItemStack chest, DamageColor c, int attackerGrade) {
        String id = chest == null || chest.isEmpty()
                ? ""
                : BuiltInRegistries.ITEM.getKey(chest.getItem()).getPath();
        float[] v = of(id);
        float base = switch (c) {
            case RED -> v[0];
            case WHITE -> v[1];
            case BLACK -> v[2];
            case PALE -> v[3];
        };
        int gradeOfChest = chest == null || chest.isEmpty() ? 0 : kr.lobcorp.Grades.ofItem(chest.getItem());
        if (gradeOfChest <= 0) gradeOfChest = 1;
        return base * kr.lobcorp.Grades.mult(attackerGrade, gradeOfChest);
    }

    /**
     * 지금 플레이어를 위협하는 개체의 피해 속성.
     * 환상체면 그 프로필의 작업 피해 속성, 그 밖에는 fallback.
     */
    public static DamageColor threatColor(ServerPlayer p, DamageColor fallback) {
        net.minecraft.world.entity.Entity e = kr.lobcorp.Grades.threatNear(p);
        if (e instanceof kr.lobcorp.entity.AbnormalityEntity a) return a.profile().workDamage();
        return fallback;
    }

    /** 등급 배율을 뺀 순수 슈트 내성 */
    public static float baseMult(ServerPlayer p, DamageColor c) {
        float[] v = of(p);
        return switch (c) {
            case RED -> v[0];
            case WHITE -> v[1];
            case BLACK -> v[2];
            case PALE -> v[3];
        };
    }

    public static String loreLine(String itemId) {
        float[] v = of(itemId);
        String line = "내성  RED " + f(v[0]) + " · WHITE " + f(v[1]) + " · BLACK " + f(v[2]) + " · PALE " + f(v[3]);
        int grade = kr.lobcorp.Grades.ofPath(itemId);
        if (grade > 0) line += "  ·  등급 " + kr.lobcorp.Grades.NAMES[grade];
        return line;
    }

    private static String f(float v) {
        return String.format("%.1f", v);
    }

    /** 내성을 이미 반영한 피해를 그대로 적용한다 (재귀 방지) */
    public static void hurtRaw(ServerPlayer p, float amount) {
        if (amount <= 0) return;
        bypass = true;
        try {
            p.hurtServer(p.level(), p.level().damageSources().magic(), amount);
        } finally {
            bypass = false;
        }
    }
}
