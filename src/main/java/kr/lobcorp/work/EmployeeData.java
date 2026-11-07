package kr.lobcorp.work;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 플레이어(직원)마다 붙는 영구 데이터. 사망해도 유지된다. */
public class EmployeeData {
    public static final int BASE_STAT = 15;

    public int fortitude = BASE_STAT;   // 용기 → 최대 HP
    public int prudence = BASE_STAT;    // 신중 → 최대 SP
    public int temperance = BASE_STAT;  // 자제 → 작업 성공률/속도
    public int justice = BASE_STAT;     // 정의 → 이동 속도
    public int sp = -1;                 // -1 = 아직 초기화 안 됨
    public int panicTicks = 0;
    public int regenTicks = 0;
    /** 요정의 축제 축복 남은 틱 (저장 안 함) */
    public int fairyTicks = 0;
    /** 공허한 꿈의 빛무리에 닿아 잠든 남은 틱 (저장 안 함) */
    public int sleepTicks = 0;
    /** 홀림(강제 조종) 남은 틱 (저장 안 함) */
    public int possessTicks = 0;
    /** 홀린 직원이 동료에게 맞은 횟수 */
    public int possessHits = 0;
    /** 홀림 원인 표시 (예: 붉은 구두) */
    public String possessLabel = "";
    /** 홀림 중 동료를 쓰러뜨리면 회복하는가 */
    public boolean possessHeal = false;
    /** 라티샤의 선물을 지니고 있는가 (저장 안 함) */
    public boolean laetitiaHeart = false;
    /** 거대한 나무 수액을 마셔 몸속에서 터질 때까지 남은 틱 (저장 안 함) */
    public int sapTicks = 0;
    /** 은하수의 아이가 건넨 우정의 조약돌을 지니고 있는가 (저장 안 함) */
    public boolean pebble = false;
    /** 눈의 여왕의 저주: 0 없음, 1 심장의 얼음 조각, 2 얼어붙음 (저장 안 함) */
    public int frost = 0;
    /** 절망의 기사의 가호를 받았는가 (저장 안 함) */
    public boolean blessed = false;
    /** 얼어붙은 자리 (저장 안 함) */
    public net.minecraft.world.phys.Vec3 frozenAt = null;
    public Set<String> gifts = new HashSet<>();
    /** 마지막으로 작업한 환상체 id (저장 안 함) */
    public String lastWorked = "";

    public static final Codec<EmployeeData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("fortitude").forGetter(d -> d.fortitude),
            Codec.INT.fieldOf("prudence").forGetter(d -> d.prudence),
            Codec.INT.fieldOf("temperance").forGetter(d -> d.temperance),
            Codec.INT.fieldOf("justice").forGetter(d -> d.justice),
            Codec.INT.fieldOf("sp").forGetter(d -> d.sp),
            Codec.INT.optionalFieldOf("panic", 0).forGetter(d -> d.panicTicks),
            Codec.STRING.listOf().optionalFieldOf("gifts", List.of()).forGetter(d -> new ArrayList<>(d.gifts))
    ).apply(i, (f, p, t, j, sp, panic, gifts) -> {
        EmployeeData d = new EmployeeData();
        d.fortitude = f;
        d.prudence = p;
        d.temperance = t;
        d.justice = j;
        d.sp = sp;
        d.panicTicks = panic;
        d.gifts = new HashSet<>(gifts);
        return d;
    }));

    public boolean hasGift(String id) {
        return gifts.contains(id);
    }

    /** 기프트 효과: {최대 HP, 최대 SP, 작업 성공률 %, 이동 속도 %} — 수치는 임의 */
    public static final java.util.Map<String, int[]> GIFT_EFFECTS = java.util.Map.ofEntries(
            java.util.Map.entry("penitence", new int[]{0, 2, 3, 0}),
            java.util.Map.entry("regret", new int[]{2, 0, 0, 0}),
            java.util.Map.entry("match_flame", new int[]{3, 0, 0, 0}),
            java.util.Map.entry("bear_paws", new int[]{0, 3, 0, 0}),
            java.util.Map.entry("wingbeat", new int[]{0, 0, 3, 0}),
            java.util.Map.entry("beak", new int[]{0, 0, 2, 5}),
            java.util.Map.entry("todays_expression", new int[]{0, -2, 4, 0}),
            java.util.Map.entry("screaming_wedge", new int[]{1, 2, 0, 0}),
            java.util.Map.entry("courage", new int[]{0, 0, 0, 10}),
            java.util.Map.entry("solitude", new int[]{0, 3, 0, 0}),
            java.util.Map.entry("red_eyes", new int[]{0, 0, 3, 0}),
            java.util.Map.entry("engulfing_dream", new int[]{0, 4, 0, 0}),
            java.util.Map.entry("horn", new int[]{2, 2, 0, 0}),
            java.util.Map.entry("lantern", new int[]{5, 0, 0, 0}),
            java.util.Map.entry("fragments", new int[]{0, 0, 2, 2}),
            java.util.Map.entry("noise", new int[]{0, 0, 0, 3}),
            java.util.Map.entry("sanguine_desire", new int[]{3, 0, 0, 0}),
            java.util.Map.entry("harmony", new int[]{8, -4, 0, 0}),
            java.util.Map.entry("pleasure", new int[]{0, 10, -6, -6}),
            java.util.Map.entry("laetitia", new int[]{0, 4, 0, 0}),
            java.util.Map.entry("grinder", new int[]{0, 0, 4, 4}),
            java.util.Map.entry("our_galaxy", new int[]{0, 0, 3, 3}),
            java.util.Map.entry("magic_bullet", new int[]{-5, -5, 0, 10}),
            java.util.Map.entry("syrinx", new int[]{-2, -2, 0, 6}),
            java.util.Map.entry("frost_splinter", new int[]{6, 6, 0, 0}),
            java.util.Map.entry("logging", new int[]{2, 0, 2, 2}),
            java.util.Map.entry("harvest", new int[]{0, 4, 0, 0}),
            java.util.Map.entry("christmas", new int[]{-4, 8, 0, 0}),
            java.util.Map.entry("solemn_lament", new int[]{1, 1, 1, 1}),
            java.util.Map.entry("gaze", new int[]{4, 0, 0, 0}),
            java.util.Map.entry("lamp", new int[]{3, 0, 3, 3}),
            java.util.Map.entry("justitia", new int[]{0, 0, 0, 3}),
            java.util.Map.entry("love_and_hate", new int[]{0, 0, 2, 4}),
            java.util.Map.entry("tears_sword", new int[]{0, 2, 0, 4}),
            java.util.Map.entry("gold_rush", new int[]{6, 0, 0, 0}),
            java.util.Map.entry("shelter", new int[]{0, 0, 0, 0}),
            java.util.Map.entry("portrait", new int[]{0, 0, 0, 0}),
            java.util.Map.entry("notes", new int[]{0, 0, 0, 0})
    );

    private int giftSum(int idx) {
        int sum = 0;
        for (String g : gifts) {
            int[] e = GIFT_EFFECTS.get(g);
            if (e != null) sum += e[idx];
        }
        return sum;
    }

    public int maxSp() {
        return Math.max(1, prudence + giftSum(1));
    }

    public int maxHp() {
        return fortitude + 5 + giftSum(0);
    }

    public int giftChanceBonus() {
        return giftSum(2);
    }

    public int giftSpeedBonus() {
        return giftSum(3);
    }

    public boolean isPanicking() {
        return panicTicks > 0;
    }

    public boolean isPossessed() {
        return possessTicks > 0;
    }

    public void addStat(WorkType type, int amount) {
        switch (type) {
            case INSTINCT -> fortitude += amount;
            case INSIGHT -> prudence += amount;
            case ATTACHMENT -> temperance += amount;
            case REPRESSION -> justice += amount;
        }
    }

    /** 원작 등급표(I: 1~29, II: 30~44, III: 45~64, IV: 65~84, V: 85~) 근사 */
    public static String grade(int v) {
        if (v >= 85) return "V";
        if (v >= 65) return "IV";
        if (v >= 45) return "III";
        if (v >= 30) return "II";
        return "I";
    }
}
