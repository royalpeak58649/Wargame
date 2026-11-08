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
        put("solemn_lament", WeaponKind.GUN, 24, 5, 3f);
        put("hornet", WeaponKind.GUN, 32, 4, 3f);
        put("magic_bullet", WeaponKind.PIERCE, 64, 40, 12f);
        put("faint_aroma", WeaponKind.BOW, 32, 14, 6f);
        put("hypocrisy", WeaponKind.BOW, 40, 28, 10f);
        put("exuviae", WeaponKind.CANNON, 24, 60, 14f);
        put("feather_of_honor", WeaponKind.THROWN, 16, 10, 3f);

        // ── 창 ──
        for (String id : new String[]{"horn", "fragments", "frost_splinter", "harvest", "green_stem", "spore", "heaven"}) {
            put(id, WeaponKind.SPEAR, 0, 0, 0f);
        }

        // ── 중량 ──
        for (String id : new String[]{"penitence", "regret", "match_flame", "lantern", "sanguine_desire",
                "harmony", "our_galaxy", "logging", "christmas", "lamp", "justitia"}) {
            put(id, WeaponKind.HEAVY, 0, 0, 0f);
        }

        // ── 속도 ──
        for (String id : new String[]{"bear_paws", "beak", "grinder", "gaze", "gold_rush", "crimson_scar",
                "cobalt_scar", "ecstasy", "amita", "moonlight", "discord", "tears_sword", "syrinx", "pleasure"}) {
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
