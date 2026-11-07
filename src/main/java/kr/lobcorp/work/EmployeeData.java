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
    public Set<String> gifts = new HashSet<>();

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
    public static final java.util.Map<String, int[]> GIFT_EFFECTS = java.util.Map.of(
            "penitence", new int[]{0, 2, 3, 0},
            "regret", new int[]{2, 0, 0, 0},
            "match_flame", new int[]{3, 0, 0, 0},
            "bear_paws", new int[]{0, 3, 0, 0},
            "wingbeat", new int[]{0, 0, 3, 0},
            "beak", new int[]{0, 0, 2, 5},
            "todays_expression", new int[]{0, -2, 4, 0},
            "screaming_wedge", new int[]{1, 2, 0, 0}
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
        return prudence + giftSum(1);
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
