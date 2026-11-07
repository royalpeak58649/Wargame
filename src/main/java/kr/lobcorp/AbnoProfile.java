package kr.lobcorp;

import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 환상체 관리 정보. 수치 조정은 여기서.
 * chances 순서: 본능, 통찰, 애착, 억압 (기본 성공률 %)
 * resist 순서: RED, WHITE, BLACK, PALE (받는 피해 배율, 탈출형만 의미 있음)
 */
public record AbnoProfile(
        String id, String name, String code, String risk, int riskColor,
        int[] chances, int maxBox, int goodMin, int normalMin,
        DamageColor workDamage, int dmgMin, int dmgMax,
        int qliphothMax,
        Supplier<Item> weapon, int weaponCost,
        Supplier<Item> suit, int suitCost,
        String giftId, String giftName, String giftDesc, int giftChance,
        float[] resist,
        List<String> notes
) {
    public static final AbnoProfile ONE_SIN = new AbnoProfile(
            "one_sin", "단 한 번의 악과 수백 가지의 선행", "O-03-03", "ZAYIN", 0x55FF55,
            new int[]{40, 60, 70, 40}, 10, 8, 5,
            DamageColor.WHITE, 1, 2,
            0,
            () -> ModItems.PENITENCE, 15,
            () -> ModItems.PENITENCE_SUIT, 20,
            "penitence", "참회", "최대 SP +2, 작업 성공률 +3%", 5,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "특수 능력 「고해」: 작업 결과가 좋음이면 작업자의 SP를 모두 회복시킨다.",
                    "PE-BOX를 최대치로 생산하면 부서 내 모든 직원의 SP가 회복된다.",
                    "격리실을 탈출하지 않는다."
            ));

    public static final AbnoProfile FORSAKEN_MURDERER = new AbnoProfile(
            "forsaken_murderer", "버려진 살인마", "T-01-54", "TETH", 0x5599FF,
            new int[]{60, 40, 50, 30}, 12, 9, 5,
            DamageColor.RED, 1, 2,
            1,
            () -> ModItems.REGRET, 25,
            () -> ModItems.REGRET_SUIT, 30,
            "regret", "후회", "최대 HP +2", 5,
            new float[]{1.0f, 1.2f, 1.2f, 1.5f},
            List.of(
                    "작업 결과가 나쁨이면 클리포트 카운터가 1 감소한다.",
                    "클리포트 카운터가 0이 되면 격리실을 탈출해 직원을 공격한다.",
                    "E.G.O가 아닌 무기로는 피해가 절반만 들어간다."
            ));


    public static final AbnoProfile SCORCHED_GIRL = new AbnoProfile(
            "scorched_girl", "불타버린 소녀", "F-01-02", "TETH", 0x5599FF,
            new int[]{50, 40, 30, 60}, 12, 9, 5,
            DamageColor.RED, 1, 3,
            2,
            () -> ModItems.MATCH_FLAME, 30,
            () -> ModItems.MATCH_FLAME_SUIT, 30,
            "match_flame", "4번째 성냥불", "최대 HP +3", 5,
            new float[]{1.0f, 0.8f, 1.2f, 1.5f},
            List.of(
                    "작업 결과가 나쁨이면 클리포트 카운터가 1 감소한다.",
                    "작업 결과가 보통이면 50% 확률로 클리포트 카운터가 1 감소한다.",
                    "탈출하면 가장 가까운 직원에게 달려가 자폭한 뒤 격리실로 돌아온다."
            ));

    public static final AbnoProfile HAPPY_TEDDY = new AbnoProfile(
            "happy_teddy_bear", "행복한 테디베어", "T-04-06", "HE", 0xFFCC33,
            new int[]{40, 50, 60, 30}, 14, 11, 6,
            DamageColor.WHITE, 1, 2,
            0,
            () -> ModItems.BEAR_PAWS, 30,
            () -> ModItems.BEAR_PAWS_SUIT, 35,
            "bear_paws", "곰 발바닥", "최대 SP +3", 5,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "같은 직원이 연달아 두 번 작업하면, 두 번째에 그 직원을 꽉 끌어안는다.",
                    "끌어안긴 직원은 살아남지 못한다. 작업자를 번갈아 보내야 한다.",
                    "격리실을 탈출하지 않는다."
            ));

    public static final AbnoProfile FAIRY_FESTIVAL = new AbnoProfile(
            "fairy_festival", "요정의 축제", "F-04-83", "ZAYIN", 0x55FF55,
            new int[]{50, 60, 70, 40}, 10, 8, 5,
            DamageColor.RED, 1, 2,
            0,
            () -> ModItems.WINGBEAT, 15,
            () -> ModItems.WINGBEAT_SUIT, 20,
            "wingbeat", "날갯짓", "작업 성공률 +3%", 5,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "작업을 마친 직원은 요정들의 축복을 받아 60초 동안 HP와 SP가 회복된다.",
                    "축복을 받은 채 다른 환상체를 작업하면, 요정들이 그 직원을 먹어 치운다.",
                    "격리실을 탈출하지 않는다."
            ));

    public static final AbnoProfile PUNISHING_BIRD = new AbnoProfile(
            "punishing_bird", "처벌새", "O-02-56", "TETH", 0x5599FF,
            new int[]{50, 40, 60, 30}, 12, 9, 5,
            DamageColor.RED, 1, 3,
            3,
            () -> ModItems.BEAK, 30,
            () -> ModItems.BEAK_SUIT, 35,
            "beak", "부리", "이동 속도 +5%, 작업 성공률 +2%", 5,
            new float[]{1.0f, 1.2f, 1.0f, 1.5f},
            List.of(
                    "다른 환상체에 작업이 시작될 때마다 50% 확률로 클리포트 카운터가 1 감소한다.",
                    "누군가 패닉에 빠지면 클리포트 카운터가 1 감소한다.",
                    "탈출한 새를 때리면 격노해 거대한 아가리로 물어뜯는다. 90초가 지나면 스스로 돌아온다."
            ));

    public static final AbnoProfile TODAYS_SHY_LOOK = new AbnoProfile(
            "todays_shy_look", "오늘의 수줍은 표정", "O-01-92", "TETH", 0x5599FF,
            new int[]{50, 50, 50, 50}, 12, 9, 5,
            DamageColor.BLACK, 2, 3,
            0,
            () -> ModItems.TODAYS_EXPRESSION, 25,
            () -> ModItems.TODAYS_EXPRESSION_SUIT, 30,
            "todays_expression", "오늘의 표정", "최대 SP -2, 작업 성공률 +4%", 5,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "다섯 개의 얼굴이 천에 걸려 있고, 그중 하나 뒤에 수줍은 무언가가 숨어 있다.",
                    "얼굴이 바뀔 때마다 작업 성공률과 실패 피해가 달라진다. 활짝 웃는 얼굴이 가장 좋다.",
                    "격리실을 탈출하지 않는다."
            ));

    public static final AbnoProfile LADY_FACING_THE_WALL = new AbnoProfile(
            "lady_facing_the_wall", "벽을 보는 여자", "F-01-18", "TETH", 0x5599FF,
            new int[]{50, 30, 60, 40}, 14, 10, 6,
            DamageColor.WHITE, 2, 3,
            2,
            () -> ModItems.SCREAMING_WEDGE, 30,
            () -> ModItems.SCREAMING_WEDGE_SUIT, 30,
            "screaming_wedge", "비명 쐐기", "최대 SP +2, 최대 HP +1", 5,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "애착 작업을 시작할 때마다 클리포트 카운터가 1 감소한다.",
                    "작업 결과가 보통이면 50%, 나쁨이면 75% 확률로 카운터가 1 더 감소한다.",
                    "카운터가 0이 되면 부서 전체에 흐느낌이 퍼져 정신력을 깎아먹는다. 자제가 낮은 직원은 뒤를 돌아보고 만다."
            ));

    public static final Map<String, AbnoProfile> BY_ID = Map.ofEntries(
            Map.entry(ONE_SIN.id, ONE_SIN),
            Map.entry(FORSAKEN_MURDERER.id, FORSAKEN_MURDERER),
            Map.entry(SCORCHED_GIRL.id, SCORCHED_GIRL),
            Map.entry(HAPPY_TEDDY.id, HAPPY_TEDDY),
            Map.entry(FAIRY_FESTIVAL.id, FAIRY_FESTIVAL),
            Map.entry(PUNISHING_BIRD.id, PUNISHING_BIRD),
            Map.entry(TODAYS_SHY_LOOK.id, TODAYS_SHY_LOOK),
            Map.entry(LADY_FACING_THE_WALL.id, LADY_FACING_THE_WALL)
    );

    public boolean canBreach() {
        return qliphothMax > 0;
    }

    public float resistTo(DamageColor c) {
        return resist[c.ordinal()];
    }
}
