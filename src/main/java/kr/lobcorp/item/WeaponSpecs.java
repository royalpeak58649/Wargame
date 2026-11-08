package kr.lobcorp.item;

import java.util.HashMap;
import java.util.Map;

/** 아이템 id별 E.G.O 무기 분류·사거리·쿨다운·피해. */
public final class WeaponSpecs {
    public record Spec(WeaponKind kind, double range, int cooldown, float damage) {
    }

    private static final Spec MELEE = new Spec(WeaponKind.BLADE, 0, 0, 0f);
    private static final Map<String, Spec> SPECS = new HashMap<>();

    static {
        // ── 원거리 ──
        put("laetitia", WeaponKind.GUN, 24, 10, 5f);
        put("solemn_lament", WeaponKind.GUN, 24, 10, 5f);
        put("hornet", WeaponKind.GUN, 32, 10, 5f);
        put("magic_bullet", WeaponKind.PIERCE, 64, 40, 12f);
        put("faint_aroma", WeaponKind.BOW, 32, 14, 6f);
        put("hypocrisy", WeaponKind.BOW, 40, 28, 10f);
        put("exuviae", WeaponKind.CANNON, 24, 60, 14f);
        put("feather_of_honor", WeaponKind.THROWN, 16, 10, 3f);
        put("beak", WeaponKind.GUN, 20, 10, 3f);
        put("solitude", WeaponKind.GUN, 20, 12, 3f);
        put("todays_expression", WeaponKind.GUN, 20, 14, 4f);
        put("ecstasy", WeaponKind.GUN, 20, 10, 4f);
        put("love_and_hate", WeaponKind.GUN, 48, 16, 7f);
        put("screaming_wedge", WeaponKind.BOW, 32, 24, 8f);
        put("syrinx", WeaponKind.PIERCE, 20, 20, 4f);
        put("harmony", WeaponKind.PIERCE, 24, 30, 8f);
        put("match_flame", WeaponKind.CANNON, 24, 60, 12f);

        // ── 창 ──
        for (String id : new String[]{"horn", "fragments", "frost_splinter", "harvest", "green_stem", "spore", "heaven"}) {
            put(id, WeaponKind.SPEAR, 0, 0, 0f);
        }

        // ── 중량 ──
        for (String id : new String[]{"penitence", "regret", "lantern", "sanguine_desire",
                "our_galaxy", "logging", "christmas", "lamp", "justitia", "mimicry"}) {
            put(id, WeaponKind.HEAVY, 0, 0, 0f);
        }

        // ── 속도 ──
        for (String id : new String[]{"bear_paws", "grinder", "gaze", "gold_rush", "crimson_scar",
                "cobalt_scar", "amita", "moonlight", "discord", "tears_sword", "pleasure", "da_capo"}) {
            put(id, WeaponKind.FAST, 0, 0, 0f);
        }
    }

    private WeaponSpecs() {
    }

    private static void put(String id, WeaponKind kind, double range, int cooldown, float damage) {
        SPECS.put(id, new Spec(kind, range, cooldown, damage));
    }

    public static Spec of(String itemId) {
        if (itemId == null) return MELEE;
        return SPECS.getOrDefault(itemId, MELEE);
    }
}
