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

    public static final AbnoProfile CRUMBLING_ARMOR = new AbnoProfile(
            "crumbling_armor", "부서지는 갑옷", "O-05-61", "TETH", 0x5599FF,
            new int[]{60, 45, 35, 55}, 12, 9, 5,
            DamageColor.RED, 2, 4,
            0,
            () -> ModItems.DAREDEVIL, 30,
            () -> ModItems.DAREDEVIL_SUIT, 30,
            "courage", "용기", "이동 속도 +10% (애착 작업 시 사망)", 0,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "용기 I등급(30 미만) 직원은 작업을 시작하자마자 목이 베인다.",
                    "억압 작업을 마치면 기프트 「용기」를 준다.",
                    "「용기」를 가진 직원이 어떤 환상체든 애착 작업을 하면 죽는다."
            ));

    public static final AbnoProfile OLD_LADY = new AbnoProfile(
            "old_lady", "늙은 여인", "O-01-12", "TETH", 0x5599FF,
            new int[]{30, 50, 65, 30}, 14, 10, 6,
            DamageColor.WHITE, 1, 3,
            4,
            () -> ModItems.SOLITUDE, 25,
            () -> ModItems.SOLITUDE_SUIT, 30,
            "solitude", "고독", "최대 SP +3", 5,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "다른 환상체의 작업이 끝날 때마다 클리포트 카운터가 1 감소한다.",
                    "자신의 작업 결과가 좋음이나 보통이면 카운터가 1 회복된다 (최대 4).",
                    "카운터가 0이 되면 격리실이 고독으로 가득 찬다. 작업 성공률 -20%, 실패 피해 2배, 작업 시작 시 SP가 깎인다."
            ));

    public static final AbnoProfile SPIDER_BUD = new AbnoProfile(
            "spider_bud", "거미 봉오리", "T-02-43", "TETH", 0x5599FF,
            new int[]{60, 20, 50, 40}, 14, 15, 7,
            DamageColor.RED, 1, 3,
            0,
            () -> ModItems.RED_EYES, 30,
            () -> ModItems.RED_EYES_SUIT, 30,
            "red_eyes", "붉은 눈", "작업 성공률 +3%", 5,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "통찰 작업을 하거나, 신중 I등급(30 미만)인 직원이 작업을 마치면 고치가 된다.",
                    "고치 하나당 이 환상체의 작업이 끝날 때마다 PE-BOX가 1씩 더 쌓인다.",
                    "작업 결과는 나쁨 또는 보통뿐이다. 좋음은 존재하지 않는다."
            ));

    public static final AbnoProfile VOID_DREAM = new AbnoProfile(
            "void_dream", "공허한 꿈", "T-02-99", "TETH", 0x5599FF,
            new int[]{40, 50, 60, 30}, 12, 9, 5,
            DamageColor.WHITE, 1, 2,
            2,
            () -> ModItems.ENGULFING_DREAM, 30,
            () -> ModItems.ENGULFING_DREAM_SUIT, 30,
            "engulfing_dream", "집어삼키는 꿈", "최대 SP +4", 8,
            new float[]{1.0f, 1.0f, 1.2f, 1.5f},
            List.of(
                    "작업 결과가 나쁨이면 클리포트 카운터가 1 감소한다.",
                    "탈출하면 낮게 떠다니며, 몸에서 흩뿌린 빛무리에 닿은 직원은 잠들어 버린다.",
                    "탈출한 꿈을 때리면 공허한 꿈-1로 변이한다. 수탉의 울음은 부서 전체의 정신력을 깎고, 잠든 직원을 패닉에 빠뜨린다."
            ));

    public static final AbnoProfile BEAUTY_BEAST = new AbnoProfile(
            "beauty_and_the_beast", "미녀와 야수", "F-02-44", "TETH", 0x5599FF,
            new int[]{40, 50, 30, 65}, 12, 9, 5,
            DamageColor.WHITE, 2, 4,
            0,
            () -> ModItems.HORN, 30,
            () -> ModItems.HORN_SUIT, 30,
            "horn", "뿔", "최대 HP +2, 최대 SP +2", 5,
            new float[]{1.0f, 1.0f, 1.0f, 1.0f},
            List.of(
                    "억압 작업은 가장 좋은 결과를 내지만, 야수를 상처 입힌다.",
                    "다른 종류의 작업은 야수를 달랜다. 상처가 아물면 다시 억압을 시도할 수 있다.",
                    "상처 입은 야수에게 또 억압 작업을 마치면, 저주가 작업자에게 옮겨 붙는다."
            ));

    public static final AbnoProfile MEAT_LANTERN = new AbnoProfile(
            "meat_lantern", "고기 등불", "O-04-84", "TETH", 0x5599FF,
            new int[]{45, 60, 45, 30}, 14, 11, 7,
            DamageColor.WHITE, 1, 3,
            1,
            () -> ModItems.LANTERN, 25,
            () -> ModItems.LANTERN_SUIT, 25,
            "lantern", "등불", "최대 HP +5", 5,
            new float[]{1.5f, 0.8f, 1.0f, 2.0f},
            List.of(
                    "작업 결과가 나쁨이면 클리포트 카운터가 1 감소한다.",
                    "작업이 지나치게 빨리 끝나거나 도중에 포기해도 카운터가 1 감소한다.",
                    "탈출하면 시설 어딘가의 바닥 밑에 숨는다. 빛나는 꽃 주변의 흰 털을 절대 밟지 마십시오."
            ));

    public static final AbnoProfile FRAGMENT = new AbnoProfile(
            "fragment_of_universe", "우주의 파편", "O-03-60", "TETH", 0x5599FF,
            new int[]{30, 40, 60, 50}, 12, 8, 4,
            DamageColor.BLACK, 1, 3,
            2,
            () -> ModItems.FRAGMENTS, 20,
            () -> ModItems.FRAGMENTS_SUIT, 25,
            "fragments", "어딘가의 파편", "작업 성공률 +2%, 작업 속도 +2%", 5,
            new float[]{1.0f, 1.5f, 1.0f, 2.0f},
            List.of(
                    "작업 결과가 보통이면 50%, 나쁨이면 70% 확률로 클리포트 카운터가 1 감소한다.",
                    "이 환상체와 작업하는 도중 직원이 패닉에 빠지면 클리포트 카운터가 1 감소한다.",
                    "탈출하면 느릿하게 배회하며 촉수로 직원을 찌른다. 때때로 멈춰 서서 노래를 부르는데, 그 소리를 들은 직원은 정신이 무너진다."
            ));

    public static final AbnoProfile MHZ = new AbnoProfile(
            "mhz", "1.76 MHz", "T-06-27", "TETH", 0x5599FF,
            new int[]{40, 40, 20, 55}, 12, 10, 6,
            DamageColor.WHITE, 2, 4,
            4,
            () -> ModItems.NOISE, 30,
            () -> ModItems.NOISE_SUIT, 30,
            "noise", "잡음", "이동 속도 +3%", 5,
            new float[]{1.0f, 1.0f, 1.0f, 1.0f},
            List.of(
                    "작업 결과가 좋음이면 클리포트 카운터가 1 회복된다. PE-BOX를 12개 모두 모으면 2 회복된다.",
                    "작업 결과가 보통이면 40%, 나쁨이면 80% 확률로 카운터가 1 감소한다.",
                    "카운터가 0이 되면 격리실에서 잡음이 새어 나와 시설로 번진다. 구역 안의 직원은 정신력을 잃고, 카운터를 다시 채워야 멈춘다."
            ));

    public static final AbnoProfile RED_SHOES = new AbnoProfile(
            "red_shoes", "붉은 구두", "O-04-08", "HE", 0xFFAA00,
            new int[]{45, 40, 50, 30}, 16, 11, 7,
            DamageColor.RED, 3, 5,
            0,
            () -> ModItems.SANGUINE_DESIRE, 40,
            () -> ModItems.SANGUINE_DESIRE_SUIT, 40,
            "sanguine_desire", "선혈의 욕망", "최대 HP +3", 4,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "작은 받침대 위에 놓인, 윤이 번들번들 흐르는 붉은 구두 한 켤레. 얼핏 보면 아주 깨끗하고 곱다.",
                    "자제가 낮은 직원은 애착 작업을 쉽게 성공한다. 그러나 그 쉬움 자체가 함정이다.",
                    "작업 결과가 보통이면 30%, 나쁨이면 60% 확률로 자제가 낮은 직원을 부른다. 불려간 직원은 구두를 신고 동료를 덮친다. 세 번 때리면 정신을 차린다."
            ));

    public static final AbnoProfile SINGING_MACHINE = new AbnoProfile(
            "singing_machine", "노래하는 기계", "O-05-30", "HE", 0xFFAA00,
            new int[]{55, 50, 0, 40}, 18, 15, 8,
            DamageColor.WHITE, 4, 6,
            1,
            () -> ModItems.HARMONY, 40,
            () -> ModItems.HARMONY_SUIT, 40,
            "harmony", "화음", "최대 HP +8, 최대 SP -4", 4,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "작업 결과가 나쁨이면 클리포트 카운터가 1 감소한다.",
                    "카운터가 0인 상태에서 작업을 시작하거나 마치면, 직원이 기계 속으로 뛰어들어 갈려 버린다.",
                    "자제 III 미만이거나 용기 IV 이상인 직원도 작업을 마치면 기계 속으로 빨려 든다. 누군가 갈릴 때마다 아름다운 연주가 흘러나와, 다른 직원 한 명이 음악에 홀린다."
            ));

    public static final AbnoProfile PORCCUBUS = new AbnoProfile(
            "porccubus", "포큐버스", "O-02-98", "HE", 0xFFAA00,
            new int[]{60, 40, 50, 30}, 18, 15, 9,
            DamageColor.BLACK, 1, 5,
            2,
            () -> ModItems.PLEASURE, 45,
            () -> ModItems.PLEASURE_SUIT, 30,
            "pleasure", "쾌락", "최대 SP +10, 성공률 -6%, 작업 속도 -6%", 4,
            new float[]{1.0f, 0.5f, 1.0f, 1.5f},
            List.of(
                    "작업 결과가 좋음이면 클리포트 카운터가 1 회복된다 (최대 2). 나쁨이면 1 감소한다.",
                    "자제 IV 미만인 직원이 좋음 결과를 내면, 그 쾌락을 견디지 못하고 격리실에서 죽는다.",
                    "탈출하면 시설 어딘가에 숨어 있다가 다가오는 직원을 덮친다. 가시에 찔린 직원은 자제가 낮을수록 더 큰 정신 피해를 입고, 패닉에 빠지면 머리가 터진다."
            ));

    public static final AbnoProfile LAETITIA = new AbnoProfile(
            "laetitia", "라티샤", "O-01-67", "HE", 0xFFAA00,
            new int[]{40, 40, 60, 0}, 16, 11, 7,
            DamageColor.BLACK, 2, 4,
            0,
            () -> ModItems.LAETITIA, 40,
            () -> ModItems.LAETITIA_SUIT, 45,
            "laetitia", "라티샤", "최대 SP +4", 4,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "작업 결과가 보통이면, 라티샤가 작업자에게 하트 모양의 작은 선물을 건넨다.",
                    "선물을 지닌 채 다른 환상체와 작업을 시작하면 선물에 금이 간다. 그 작업이 절반을 넘는 순간 배가 터져 무언가 기어 나온다.",
                    "금이 간 선물을 안고 있는 작업을 절반 전에 포기하면, 선물은 멀쩡한 채로 남는다."
            ));

    public static final AbnoProfile HELPER = new AbnoProfile(
            "all_around_helper", "만능 도우미", "T-05-41", "HE", 0xFFAA00,
            new int[]{50, 0, 35, 50}, 16, 10, 6,
            DamageColor.RED, 3, 5,
            2,
            () -> ModItems.GRINDER, 50,
            () -> ModItems.GRINDER_SUIT, 35,
            "grinder", "그라인더 Mk4", "작업 성공률 +4%, 작업 속도 +4%", 4,
            new float[]{0.5f, 1.0f, 2.0f, 2.0f},
            List.of(
                    "작업 결과가 보통이면 50%, 나쁨이면 70% 확률로 클리포트 카운터가 1 감소한다.",
                    "카운터가 0이 되면 격리실을 탈출해 제자리에서 칼날을 펼친 뒤, 직선으로 돌진해 지나가는 길의 모두를 벤다.",
                    "돌진이 끝나면 동력이 떨어져 한동안 멈춰 선다. 그때가 공격할 기회다."
            ));

    public static final AbnoProfile CHILD_GALAXY = new AbnoProfile(
            "child_galaxy", "은하수의 아이", "O-01-55", "HE", 0xFFAA00,
            new int[]{45, 45, 45, 45}, 16, 17, 9,
            DamageColor.BLACK, 2, 3,
            5,
            () -> ModItems.OUR_GALAXY, 45,
            () -> ModItems.OUR_GALAXY_SUIT, 30,
            "our_galaxy", "우리의 은하", "작업 성공률 +3%, 작업 속도 +3%", 4,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "작업을 마칠 때마다 클리포트 카운터가 1 오르고, 작업자는 우정의 조약돌을 받는다 (한 사람당 하나).",
                    "조약돌은 지닌 이의 HP와 SP를 조금씩 회복시킨다. 카운터가 높을수록 더 많이 회복된다.",
                    "조약돌을 지닌 채 다른 환상체를 작업하면 카운터가 1 내려가고, 조약돌을 지닌 이가 죽으면 4 내려간다. 0이 되면 조약돌을 지닌 모두가 함께 무너진다."
            ));

    public static final AbnoProfile FREISCHUTZ = new AbnoProfile(
            "freischutz", "마탄의 사수", "F-01-69", "HE", 0xFFAA00,
            new int[]{40, 50, 30, 0}, 18, 12, 7,
            DamageColor.BLACK, 3, 4,
            3,
            () -> ModItems.MAGIC_BULLET, 50,
            () -> ModItems.MAGIC_BULLET_SUIT, 45,
            "magic_bullet", "마탄", "최대 HP -5, 최대 SP -5, 이동 속도 +10%", 4,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "정의가 낮은 직원이 작업을 마치거나, 결과가 좋음이 아니면 클리포트 카운터가 1 감소한다.",
                    "카운터가 0이 되면 시설 어딘가로 마탄이 날아간다. 그 뒤 카운터는 3으로 돌아온다.",
                    "애착 작업은 의뢰로 바뀐다. 시설 에너지 10%를 바치면 마탄 의뢰서를 받는다. 우클릭하면 바라보는 방향으로 탄환이 벽을 뚫고 날아간다.",
                    "억압 작업은 정의 III 이상인 직원만 제대로 해낼 수 있다."
            ));

    public static final AbnoProfile FETUS = new AbnoProfile(
            "nameless_fetus", "이름 없는 태아", "O-01-15", "HE", 0xFFAA00,
            new int[]{40, 20, 20, 20}, 18, 14, 7,
            DamageColor.RED, 4, 6,
            1,
            () -> ModItems.SYRINX, 40,
            () -> ModItems.SYRINX_SUIT, 40,
            "syrinx", "시링크스", "최대 HP -2, 최대 SP -2, 이동 속도 +6%", 4,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "거대한 살덩어리가 잠들어 있다. 숨을 쉴 때마다 표면 아래로 무언가가 꿈틀거린다.",
                    "작업 결과가 보통이면 30%, 나쁨이면 90% 확률로 클리포트 카운터가 1 감소한다.",
                    "카운터가 0이 되면 태아가 울기 시작한다. 울음소리는 24블록 안의 정신을 갉아먹고, 가까운 환상체들의 격리까지 무너뜨린다. 누군가 먹이가 되어야 그친다."
            ));

    public static final AbnoProfile SNOW_QUEEN = new AbnoProfile(
            "snow_queen", "눈의 여왕", "F-01-37", "HE", 0xFFAA00,
            new int[]{30, 50, 40, 0}, 18, 14, 8,
            DamageColor.WHITE, 4, 5,
            0,
            () -> ModItems.FROST_SPLINTER, 35,
            () -> ModItems.FROST_SPLINTER_SUIT, 35,
            "frost_splinter", "서리 조각", "최대 HP +6, 최대 SP +6", 4,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "작업 결과가 보통이면 50%, 나쁨이면 반드시 저주가 한 단계 진행된다.",
                    "한 번 저주받은 직원의 심장에는 얼음 조각이 박힌다. 두 번째에는 그 자리에서 얼어붙어 움직일 수 없다.",
                    "얼어붙은 직원은 하루가 끝날 때까지 구출되지 않으면 죽는다. 얼어붙은 이가 있는 동안 누군가 그녀와 작업을 시작하면, 그 직원은 대신 결투를 벌인다."
            ));

    public static final AbnoProfile WOODSMAN = new AbnoProfile(
            "woodsman", "따뜻한 마음의 나무꾼", "F-05-32", "HE", 0xFFAA00,
            new int[]{45, 45, 50, 45}, 18, 15, 9,
            DamageColor.WHITE, 3, 5,
            1,
            () -> ModItems.LOGGING, 40,
            () -> ModItems.LOGGING_SUIT, 30,
            "logging", "벌목", "최대 HP +2, 작업 성공률 +2%, 작업 속도 +2%", 4,
            new float[]{1.5f, 0.8f, 0.8f, 1.0f},
            List.of(
                    "주석으로 된 몸을 가진 나무꾼. 가슴에는 심장이 들어갈 자리가 비어 있다.",
                    "작업 결과가 나쁨이거나, 자제 II등급(30 이상)인 직원이 작업을 마치면 클리포트 카운터가 1 감소한다.",
                    "카운터가 0이 되어도 스스로 탈출하지 않는다. 다음에 격리실에 들어온 직원을 베어 버린 뒤에야 문을 연다."
            ));

    public static final AbnoProfile TREE_SAP = new AbnoProfile(
            "tree_sap", "거대한 나무 수액", "T-09-80", "HE", 0xFFAA00,
            new int[]{0, 0, 0, 0}, 0, 0, 0,
            DamageColor.WHITE, 0, 0,
            0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            "", "", "", 0,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "도구형 환상체. 작업도, PE-BOX도, E.G.O도 없다.",
                    "국자로 수액을 떠서 마시면 HP가 모두 회복되고 한동안 재생된다.",
                    "마실 때마다 항아리가 터질 확률이 15%씩 오른다 (최대 60%). 터지면 20초 뒤 마신 직원이 터지고, 주변 8블록의 직원이 정신 피해를 입는다."
            ));

    public static final AbnoProfile NOTES = new AbnoProfile(
            "notes", "미친 연구원의 노트", "T-09-78", "HE", 0xFFAA00,
            new int[]{0, 0, 0, 0}, 0, 0, 0,
            DamageColor.WHITE, 0, 0,
            0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            "", "", "", 0,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "도구형 환상체. 작업도, PE-BOX도, E.G.O도 없다.",
                    "노트를 들고 있는 동안 작업 성공률 +20, 작업 속도가 빨라진다. 30초가 지나고 한 번이라도 작업을 마쳐야 안전하게 내려놓을 수 있다.",
                    "그 전에 반납하거나, 든 채로 HP·SP 피해를 60 이상 받으면 머리가 터진다. 주변 5블록의 직원도 함께 휩쓸린다."
            ));

    public static final AbnoProfile PORTRAIT = new AbnoProfile(
            "portrait", "다른 세계의 초상", "O-09-91", "HE", 0xFFAA00,
            new int[]{0, 0, 0, 0}, 0, 0, 0,
            DamageColor.WHITE, 0, 0,
            0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            "", "", "", 0,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "도구형 환상체. 작업도, PE-BOX도, E.G.O도 없다.",
                    "사용한 직원은 「새겨진 자」가 되고, 접속 중인 다른 직원 한 명이 「대신하는 자」가 된다. 새겨진 자가 받을 HP·SP 피해는 대신하는 자에게 1.5배로 옮겨 간다.",
                    "옮겨 간 피해가 100을 넘으면 2배, 200을 넘으면 2.5배가 된다. 한 번의 피해가 최대 HP·SP를 넘으면 새겨진 자가 그대로 죽는다. 누군가 새로 사용하면 이전 새겨진 자는 죽고, 하루가 끝나면 초상화는 제자리로 돌아간다."
            ));

    public static final AbnoProfile SHELTER = new AbnoProfile(
            "shelter", "3월 27일의 대피소", "T-09-82", "HE", 0xFFAA00,
            new int[]{0, 0, 0, 0}, 0, 0, 0,
            DamageColor.WHITE, 0, 0,
            0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            "", "", "", 0,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "도구형 환상체. 작업도, PE-BOX도, E.G.O도 없다.",
                    "해치를 열고 들어가면 한 번에 한 사람만 머무를 수 있다. 안에 있는 동안 어떤 피해도 받지 않고, 5초마다 HP와 SP가 회복된다.",
                    "30초를 넘겨 머물면 시설 곳곳의 격리가 무너지기 시작한다. 다시 사용하거나 3블록 밖으로 나가면 나온다."
            ));

    public static final AbnoProfile SCARECROW = new AbnoProfile(
            "scarecrow", "지혜를 찾는 허수아비", "F-01-87", "HE", 0xFFAA00,
            new int[]{45, 50, 45, 45}, 18, 15, 9,
            DamageColor.WHITE, 2, 6,
            1,
            () -> ModItems.HARVEST, 35,
            () -> ModItems.HARVEST_SUIT, 35,
            "harvest", "수확", "최대 SP +4", 4,
            new float[]{0.8f, 0.5f, 1.2f, 2.0f},
            List.of(
                    "작업 결과가 나쁨이면 클리포트 카운터가 1 감소한다.",
                    "신중 II등급(30 이상)인 직원이 작업을 마치면 카운터가 1 감소한다.",
                    "카운터가 0이 되면 탈출해 마지막으로 본 직원을 쫓는다. 쓰러뜨린 직원의 머릿속에서 지혜를 꺼내 가슴에 채워 넣는다."
            ));

    public static final AbnoProfile FUNERAL = new AbnoProfile(
            "funeral", "죽은 나비들의 장례식", "T-01-68", "HE", 0xFFAA00,
            new int[]{50, 50, 0, 0}, 16, 11, 7,
            DamageColor.WHITE, 4, 6,
            2,
            () -> ModItems.SOLEMN_LAMENT, 50,
            () -> ModItems.SOLEMN_LAMENT_SUIT, 45,
            "solemn_lament", "엄숙한 애도", "최대 HP·SP +1, 작업 성공률·속도 +1", 4,
            new float[]{0.5f, 1.5f, 1.0f, 2.0f},
            List.of(
                    "검은 상복을 입은 남자. 머리통은 관이고, 그 주위를 흰 나비들이 맴돈다. 두 손에는 검은 총이 쥐여 있다.",
                    "정의 II등급(45 미만)인 직원이 작업을 마치면 클리포트 카운터가 1 감소한다. 용기 IV등급(65 이상)인 직원도 마찬가지다.",
                    "작업 결과가 나쁨이면 80% 확률로 카운터가 1 더 감소한다. 카운터가 0이 되면 관이 열리고, 나비 떼가 시설로 흩어진다."
            ));

    public static final AbnoProfile RUDOLTA = new AbnoProfile(
            "rudolta", "썰매 탄 루돌타", "F-02-49", "HE", 0xFFAA00,
            new int[]{20, 50, 40, 0}, 18, 12, 6,
            DamageColor.WHITE, 3, 4,
            2,
            () -> ModItems.CHRISTMAS, 35,
            () -> ModItems.CHRISTMAS_SUIT, 40,
            "christmas", "크리스마스", "최대 HP -4, 최대 SP +8", 4,
            new float[]{1.5f, 0.5f, 1.0f, 2.0f},
            List.of(
                    "작업 결과가 보통이면 50%, 나쁨이면 70% 확률로 클리포트 카운터가 1 감소한다.",
                    "카운터가 0이 되면 썰매를 끌고 시설을 느릿하게 배회한다. 누구도 쫓지 않지만, 지나간 자리에는 눈이 쌓인다.",
                    "3초마다 몸에서 피를 쏟아낸다. 10블록 안의 직원은 정신력을 잃는다. 멀리서 제압하십시오."
            ));

    public static final AbnoProfile SCHADENFREUDE = new AbnoProfile(
            "schadenfreude", "샤덴프로이데", "O-05-76", "HE", 0xFFAA00,
            new int[]{0, 30, 40, 40}, 18, 11, 6,
            DamageColor.RED, 3, 6,
            2,
            () -> ModItems.GAZE, 40,
            () -> ModItems.GAZE_SUIT, 35,
            "gaze", "응시", "최대 HP +4", 4,
            new float[]{1.0f, 1.0f, 1.0f, 1.5f},
            List.of(
                    "누군가 자신을 바라보는 동안, 5초마다 클리포트 카운터가 1 감소한다. 작업자는 끝까지 눈을 돌려야 한다.",
                    "탈출하면 누군가 바라볼 때만 움직인다. 아무도 보지 않으면 완전히 멈춰 선다.",
                    "멈춰 세우려면 눈을 돌리고, 싸우려면 다시 바라봐야 한다. 그 사이에 톱날이 돌기 시작한다."
            ));

    public static final AbnoProfile ALRIUNE = new AbnoProfile(
            "alriune", "알리우네", "T-04-53", "WAW", 0xAA55FF,
            new int[]{45, 45, 50, 35}, 22, 16, 8,
            DamageColor.WHITE, 4, 6,
            1,
            () -> ModItems.FAINT_AROMA, 60,
            () -> ModItems.FAINT_AROMA_SUIT, 50,
            "faint_aroma", "희미한 향기", "최대 SP +4, 작업 성공률 +2%, 작업 속도 +2%", 3,
            new float[]{1.2f, 0.0f, 0.5f, 1.5f},
            List.of(
                    "창백한 분홍 꽃잎과 가느다란 가지로 빚은 키 큰 여인. 꽃으로 지은 드레스를 입고, 얼굴에는 아무것도 없다.",
                    "작업 결과가 좋음이거나 나쁨이면 70% 확률로 클리포트 카운터가 1 감소한다.",
                    "탈출하면 걷지 않고 시설 곳곳으로 순간이동한다. 6블록 안에 직원이 머무는 동안 꽃잎이 5초마다 하나씩 피어나고, 세 번째 꽃잎이 필 때 8블록 안의 모든 직원이 향기에 잠긴다. 정신력이 바닥난 직원은 패닉 대신 그대로 쓰러진다."
            ));

    public static final AbnoProfile BIG_BIRD = new AbnoProfile(
            "big_bird", "큰 새", "O-02-40", "WAW", 0xAA55FF,
            new int[]{45, 35, 40, 25}, 20, 14, 8,
            DamageColor.BLACK, 2, 6,
            5,
            () -> ModItems.LAMP, 70,
            () -> ModItems.LAMP_SUIT, 60,
            "lamp", "램프", "최대 HP +3, 작업 성공률 +3%, 작업 속도 +3%", 3,
            new float[]{0.8f, 1.2f, 0.5f, 1.5f},
            List.of(
                    "온몸이 노란 눈으로 뒤덮인 거대한 검은 새. 긴 목 끝의 부리는 새장처럼 갈라져 있고, 발톱에는 낡은 등불이 매달려 있다.",
                    "작업 결과가 좋음이면 클리포트 카운터가 1 회복된다 (최대 5). 나쁨이면 1 감소한다.",
                    "직원이 다섯 명 죽을 때마다 카운터가 1 감소한다. 다른 환상체 세 개가 동시에 탈출하면 스스로 격리실을 나온다."
            ));

    public static final AbnoProfile BIG_BAD_WOLF = new AbnoProfile(
            "big_bad_wolf", "크고 나빠질 늑대", "F-02-58", "WAW", 0xAA55FF,
            new int[]{45, 30, 45, 0}, 22, 18, 10,
            DamageColor.RED, 4, 8,
            2,
            () -> ModItems.COBALT_SCAR, 70,
            () -> ModItems.COBALT_SCAR_SUIT, 60,
            "cobalt_scar", "흑청빛 상처", "최대 HP +4, 이동 속도 +2%", 3,
            new float[]{1.0f, 0.7f, 0.7f, 1.0f},
            List.of(
                    "두 발로 선 거대한 회색 늑대. 목덜미의 털은 유난히 복슬복슬하고, 입에는 양털이 한 움큼 물려 있다.",
                    "작업 결과가 나쁨이면 클리포트 카운터가 1 감소하고, 작업자는 늑대에게 통째로 삼켜진다.",
                    "빨간 두건 용병과 작업한 직원이 이 늑대 앞에 서면, 늑대는 그 냄새를 기억하고 삼켜 버린다. 용병이 격리실 밖에서 늑대의 곁에 다가가면 카운터가 곧바로 0이 된다."
            ));

    public static final AbnoProfile LITTLE_RED = new AbnoProfile(
            "little_red", "빨간 두건 용병", "F-01-57", "WAW", 0xAA55FF,
            new int[]{0, 45, 0, 30}, 20, 17, 9,
            DamageColor.RED, 4, 6,
            3,
            () -> ModItems.CRIMSON_SCAR, 70,
            () -> ModItems.CRIMSON_SCAR_SUIT, 60,
            "crimson_scar", "진홍빛 상처", "최대 HP +2, 이동 속도 +2%", 3,
            new float[]{0.9f, 0.6f, 0.8f, 1.2f},
            List.of(
                    "다른 환상체가 탈출할 때마다 클리포트 카운터가 1 감소한다. 처벌새는 세지 않는다.",
                    "애착 작업은 의뢰로 바뀐다. 시설 에너지를 바치면 그녀가 가장 가까운 탈출 환상체를 쫓아가 베어 넘긴다. 고용된 동안 카운터는 줄지 않는다.",
                    "탈출하면 빠르게 달려들어 도끼를 휘두르고, 가까운 직원을 향해 권총을 쏜다. 늑대의 냄새를 맡으면 이성을 잃는다."
            ));

    public static final AbnoProfile KING_GREED = new AbnoProfile(
            "king_of_greed", "탐욕의 왕", "O-01-64", "WAW", 0xAA55FF,
            new int[]{25, 0, 0, 0}, 22, 16, 8,
            DamageColor.RED, 5, 7,
            1,
            () -> ModItems.GOLD_RUSH, 100,
            () -> ModItems.GOLD_RUSH_SUIT, 60,
            "gold_rush", "골드러시", "최대 HP +6", 3,
            new float[]{0.0f, 0.5f, 1.2f, 1.5f},
            List.of(
                    "황금 드레스를 입은 마법소녀. 온몸을 장식한 보석들이 조명을 받아 눈부시게 빛난다.",
                    "작업 결과가 보통이면 50%, 나쁨이면 70% 확률로 클리포트 카운터가 1 감소한다.",
                    "카운터가 0이 되면 거대한 황금빛 아가리가 되어 시설 어딘가에 나타난다. 앞으로만 느릿하게 나아가며, 부딪힌 벽은 문이 되어 다른 곳으로 이어진다. 정면에 선 직원은 통째로 삼켜진다. 뒤나 옆에서 때리십시오."
            ));

    public static final AbnoProfile KNIGHT_DESPAIR = new AbnoProfile(
            "knight_of_despair", "절망의 기사", "O-01-73", "WAW", 0xAA55FF,
            new int[]{0, 45, 50, 40}, 22, 14, 8,
            DamageColor.WHITE, 4, 6,
            0,
            () -> ModItems.TEARS_SWORD, 70,
            () -> ModItems.TEARS_SWORD_SUIT, 60,
            "tears_sword", "눈물로 벼려낸 검", "최대 SP +2, 이동 속도 +4%", 3,
            new float[]{1.2f, 1.0f, 0.8f, 0.5f},
            List.of(
                    "푸른 머리카락이 검게 물들어 가는 여성 기사. 찢어진 망토 뒤로 수많은 검이 떠 있고, 그녀는 조용히 운다.",
                    "매일 처음으로 좋음 결과를 낸 직원에게 가호를 내린다. 가호는 하루에 한 사람만 받을 수 있다.",
                    "가호를 받은 직원은 더 이상 작업을 할 수 없고, 오직 싸울 수만 있다. 그 직원이 쓰러지거나 패닉에 빠지면 기사가 격리실을 나온다."
            ));

    public static final AbnoProfile QUEEN_HATRED = new AbnoProfile(
            "queen_of_hatred", "증오의 여왕", "O-01-04", "WAW", 0xAA55FF,
            new int[]{30, 45, 50, 20}, 22, 16, 10,
            DamageColor.BLACK, 3, 5,
            2,
            () -> ModItems.LOVE_AND_HATE, 70,
            () -> ModItems.LOVE_AND_HATE_SUIT, 60,
            "love_and_hate", "사랑과 증오의 이름으로", "작업 성공률 +2%, 작업 속도 +2%, 이동 속도 +4%", 3,
            new float[]{0.7f, 1.2f, 0.3f, 1.5f},
            List.of(
                    "분홍빛 드레스를 입은 마법소녀. 별 지팡이를 들고 웃고 있지만, 그 웃음은 오래가지 않는다.",
                    "작업 결과가 나쁨이면 클리포트 카운터가 1 감소한다. 카운터가 1이 되면 그녀는 히스테리에 빠져 작업 성공률이 20% 떨어진다.",
                    "히스테리 상태에서 작업을 마쳤을 때 PE-BOX가 16개 이상이면 카운터가 1 회복되고, 그렇지 않으면 1 감소한다. 0이 되면 거대한 검은 뱀이 되어 시설을 옮겨 다니며 레이저를 쏜다."
            ));

    public static final AbnoProfile JUDGEMENT_BIRD = new AbnoProfile(
            "judgement_bird", "심판 새", "O-02-62", "WAW", 0xAA55FF,
            new int[]{20, 20, 20, 0}, 24, 13, 7,
            DamageColor.PALE, 5, 7,
            2,
            () -> ModItems.JUSTITIA, 150,
            () -> ModItems.JUSTITIA_SUIT, 120,
            "justitia", "유스티티아", "이동 속도 +3%", 2,
            new float[]{0.8f, 0.8f, 0.8f, 2.0f},
            List.of(
                    "길고 마른 다리 위에 검은 사제복을 걸친 새. 머리는 붕대로 감싼 자루이고, 그 앞으로 긴 부리가 뻗어 있다.",
                    "작업 결과가 보통이면 50%, 나쁨이면 70% 확률로 클리포트 카운터가 1 감소한다.",
                    "탈출하면 저울을 들어 올린 뒤 8블록 안의 모든 직원에게 심판을 내린다. 방호복은 아무 소용이 없다. 저울이 빛나는 2초 동안 자리를 벗어나십시오."
            ));

    public static final AbnoProfile QUEEN_BEE = new AbnoProfile(
            "queen_bee", "여왕벌", "T-04-50", "WAW", 0xAA55FF,
            new int[]{40, 45, 45, 35}, 22, 17, 10,
            DamageColor.RED, 4, 6,
            1,
            () -> ModItems.HORNET, 60,
            () -> ModItems.HORNET_SUIT, 50,
            "hornet", "말벌", "최대 HP +2, 최대 SP +4", 3,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "밀랍과 살로 빚은 거대한 벌집 위에 앉아, 부푼 배를 느리게 움직이는 여왕벌. 더듬이는 왕관처럼 솟아 있다.",
                    "작업 결과가 보통이면 50%, 나쁨이면 70% 확률로 클리포트 카운터가 1 감소한다.",
                    "카운터가 0이 되면 3초 뒤 격리실에서 포자가 퍼져 나간다. 32블록 안의 직원 셋이 감염되어 20초 동안 피부를 뚫고 자라난다. 감염된 채 죽은 직원의 몸에서는 일벌이 기어 나온다."
            ));

    public static final AbnoProfile BLACK_SWAN = new AbnoProfile(
            "black_swan", "흑조의 꿈", "F-02-70", "WAW", 0xAA55FF,
            new int[]{35, 40, 45, 35}, 24, 17, 9,
            DamageColor.WHITE, 5, 6,
            5,
            () -> ModItems.BLACK_SWAN, 60,
            () -> ModItems.BLACK_SWAN_SUIT, 50,
            "black_swan", "흑조", "최대 HP -4, 최대 SP -4, 작업 성공률 +10%, 작업 속도 +10%", 3,
            new float[]{1.0f, 0.8f, 1.5f, 1.0f},
            List.of(
                    "검은 깃털 드레스를 입은 여인이 검은 양산을 들고 조용한 호수 위에 서 있다. 그녀를 둘러싼 여섯 마리의 백조는 저주받은 오라비들이다.",
                    "작업 결과가 보통이거나 나쁘면, 그리고 하루에 직원 다섯이 죽거나 다섯이 패닉에 빠지거나 환상체 셋이 탈출할 때마다 백조 한 마리가 검게 물든다.",
                    "여섯 마리가 모두 검게 물들면 여동생이 모습을 드러낸다. 탈출한 그녀의 양산은 정면의 공격을 되돌려 보내고, 지나가는 격리실의 카운터를 갉아먹는다. 생명이 절반 아래로 떨어지면 날개가 돋아나며 비명을 지른다."
            ));

    public static final AbnoProfile YIN = new AbnoProfile(
            "yin", "음", "O-05-102", "WAW", 0xAA55FF,
            new int[]{35, 45, 25, 35}, 20, 16, 9,
            DamageColor.BLACK, 4, 6,
            2,
            () -> ModItems.DISCORD, 60,
            () -> ModItems.DISCORD_SUIT, 50,
            "discord", "불화", "최대 HP -4, 최대 SP -4, 이동 속도 +10%", 3,
            new float[]{0.5f, 1.5f, 0.0f, 1.0f},
            List.of(
                    "검은 비단 같은 비늘을 지닌 잉어. 머리 한가운데에 눈알 하나가 박혀 있고, 그 눈은 언제나 짝을 찾고 있다.",
                    "통찰 작업이 가장 좋고, 본능과 억압이 그다음, 애착이 가장 나쁘다. 작업 결과가 나쁘면 클리포트 카운터가 1 감소한다.",
                    "카운터가 0이 되면 탈출한다. 곁을 지나는 직원에게 어둠의 파동을 뿜고, 잠시 바닥을 표시한 뒤 그 자리를 검게 터뜨린다. 생명이 다하면 작은 검은 조각이 되어 20초 뒤 다시 일어난다."
            ));

    public static final AbnoProfile YANG = new AbnoProfile(
            "yang", "양", "O-07-103", "WAW", 0xAA55FF,
            new int[]{0, 0, 0, 0}, 0, 0, 0,
            DamageColor.WHITE, 0, 0,
            0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            "", "", "", 0,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "도구형 환상체. 작업도, PE-BOX도, E.G.O도 없다.",
                    "사용한 직원은 흰 잉어를 몸에 두른다. 착용자는 1초마다 SP가 1씩 회복되고 WHITE 작업 피해가 줄어든다. 다시 사용하면 내려놓는다.",
                    "착용 시간이 30초씩 쌓일 때마다 음의 클리포트 카운터가 1 내려간다. 격리 중인 음에게 4블록 안으로 다가가면 둘이 만나 용이 지나가고, 착용자는 그 자리에서 죽는다."
            ));

    public static final AbnoProfile SNOW_WHITE_APPLE = new AbnoProfile(
            "snow_white_apple", "백설공주의 사과", "F-04-42", "WAW", 0xAA55FF,
            new int[]{25, 35, 25, 45}, 20, 14, 8,
            DamageColor.BLACK, 3, 5,
            1,
            () -> ModItems.GREEN_STEM, 60,
            () -> ModItems.GREEN_STEM_SUIT, 50,
            "green_stem", "초록 줄기", "최대 SP +6", 2,
            new float[]{0.5f, 1.0f, 0.0f, 1.5f},
            List.of(
                    "한 입 베어 문 거대한 붉은 사과. 벌레 먹은 자리에서 초록 덩굴이 뻗어 나와 바닥을 기어 다닌다.",
                    "억압 작업이 가장 좋고 통찰이 그다음, 본능과 애착이 가장 나쁘다. 작업 결과가 보통이면 50%, 나쁨이면 70% 확률로 클리포트 카운터가 1 감소한다.",
                    "카운터가 0이 되면 탈출해 같은 층의 먼 자리로 순간이동하고, 주변으로 덩굴을 퍼뜨린다. 덩굴 위의 직원은 느려지고, 4초마다 검은 피해를 입는다. 덩굴이 다 자라면 다시 자리를 옮긴다."
            ));

    public static final AbnoProfile LITTLE_PRINCE = new AbnoProfile(
            "little_prince", "어린 왕자", "O-04-66", "WAW", 0xAA55FF,
            new int[]{20, 30, 40, 40}, 24, 16, 9,
            DamageColor.BLACK, 3, 4,
            2,
            () -> ModItems.SPORE, 50,
            () -> ModItems.SPORE_SUIT, 50,
            "spore", "포자", "최대 SP +5, 작업 성공률 +2%", 3,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "푸른 회색 버섯이 망토를 쓴 아이의 모양으로 뭉쳐 있다. 갓이 흔들릴 때마다 포자가 사방으로 흩날린다.",
                    "통찰 작업이 가장 좋고, 억압과 애착이 그다음, 본능이 가장 나쁘다. 통찰이 아닌 작업을 세 번 연달아 마치거나 결과가 나쁘면 클리포트 카운터가 1 감소한다.",
                    "카운터가 0이 되면 포자가 가장 많이 쌓인 직원들을 격리실로 끌어들인다. 가장 먼저 닿은 직원은 그 자리에서 죽고 어린 왕자-1이 태어난다."
            ));

    public static final AbnoProfile PARASITE_TREE = new AbnoProfile(
            "parasite_tree", "기생 나무", "D-04-108", "WAW", 0xAA55FF,
            new int[]{40, 35, 45, 20}, 24, 15, 8,
            DamageColor.WHITE, 5, 6,
            1,
            () -> ModItems.HYPOCRISY, 60,
            () -> ModItems.HYPOCRISY_SUIT, 60,
            "hypocrisy", "위선", "최대 HP +3, 최대 SP +3", 3,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "창백한 나무. 가지마다 하얀 꽃봉오리가 달려 있고, 봉오리가 모두 열리면 열매 대신 해골이 맺힌다.",
                    "애착 작업이 가장 좋고 본능이 그다음, 억압이 가장 나쁘다. 억압 작업을 하거나 다른 환상체의 작업을 다섯 번 마치면 클리포트 카운터가 1 감소한다.",
                    "작업한 직원은 축복을 받아 5초마다 SP가 회복된다. 봉오리가 다 열리면 축복받은 직원이 패닉에 빠지고, 잠시 뒤 몸이 터져 묘목이 자라난다."
            ));

    public static final AbnoProfile NAKED_NEST = new AbnoProfile(
            "naked_nest", "벌거벗은 둥지", "O-02-74", "WAW", 0xAA55FF,
            new int[]{45, 20, 30, 40}, 22, 15, 9,
            DamageColor.RED, 5, 7,
            0,
            () -> ModItems.EXUVIAE, 60,
            () -> ModItems.EXUVIAE_SUIT, 50,
            "exuviae", "탈피", "최대 HP +5, 최대 SP +2", 3,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "축축한 살점이 겹겹이 뭉쳐 둥지를 이룬다. 군데군데 뚫린 구멍마다 창백한 뱀이 고개를 내밀고 꿈틀거린다.",
                    "본능 작업이 가장 좋고 억압과 애착이 그다음, 통찰이 가장 나쁘다. 탈출하지도, 클리포트 카운터도 없다. 대신 작업 중 잃은 HP가 많을수록 작업자를 감염시킨다.",
                    "감염된 직원은 12초 동안 몸에서 검은 물이 새고, 12초가 지나면 느려지며, 48초가 지나면 몸이 찢겨 뱀이 기어 나온다. 감염자는 5초마다 30% 확률로 4블록 안의 다른 직원에게 뱀을 옮긴다."
            ));

    public static final AbnoProfile DREAMING_CURRENT = new AbnoProfile(
            "dreaming_current", "꿈꾸는 조류", "T-02-71", "WAW", 0xAA55FF,
            new int[]{45, 20, 40, 30}, 20, 11, 6,
            DamageColor.WHITE, 3, 6,
            2,
            () -> ModItems.ECSTASY, 60,
            () -> ModItems.ECSTASY_SUIT, 40,
            "ecstasy", "황홀", "최대 SP +6", 3,
            new float[]{1.5f, 0.5f, 1.0f, 2.0f},
            List.of(
                    "몸이 비쳐 보이는 길고 긴 무지개빛 물고기. 입을 크게 벌린 채 꿈속을 헤엄치듯 떠다니고, 지나간 자리에는 물빛이 남는다.",
                    "본능 작업이 가장 좋고 애착이 그다음, 억압과 통찰이 가장 나쁘다. 절제가 I 등급인 직원이 작업하면, 그리고 작업 중 패닉에 빠지면 클리포트 카운터가 1 감소한다.",
                    "카운터가 0이 되면 탈출해 같은 층에 무지개빛 물웅덩이 세 개를 만든다. 물웅덩이 위의 직원은 느려지고 계속 SP가 깎인다. 물고기는 웅덩이 사이를 오가며 몸을 웅크렸다가 곧게 돌진하는데, 돌진 중에는 어떤 피해도 통하지 않는다."
            ));

    public static final AbnoProfile BURROWING_HEAVEN = new AbnoProfile(
            "burrowing_heaven", "땅을 파는 천국", "O-04-72", "WAW", 0xAA55FF,
            new int[]{20, 30, 45, 40}, 24, 21, 13,
            DamageColor.BLACK, 4, 5,
            3,
            () -> ModItems.HEAVEN, 40,
            () -> ModItems.HEAVEN_SUIT, 50,
            "heaven", "천국", "최대 HP +4, 작업 성공률 +2%", 3,
            new float[]{0.0f, 1.2f, 0.5f, 1.5f},
            List.of(
                    "창백하게 긴 몸통 위에 핏발 선 눈이 수없이 달린 나무. 뿌리가 바닥을 파고들어 몸을 지탱한다.",
                    "작업 중에는 반드시 이 환상체를 바라보고 있어야 한다. 시선을 떼면 클리포트 카운터가 내려가고, 작업 결과가 좋음이면 카운터가 2, 보통이면 1 오른다.",
                    "카운터가 0이 되면 탈출해 아무도 보지 않는 순간 땅속으로 사라졌다가 직원 곁에서 솟아오른다. 떠난 자리와 도착한 자리 주변의 직원이 찢긴다."
            ));

    public static final AbnoProfile FIREBIRD = new AbnoProfile(
            "firebird", "불새", "O-02-101", "WAW", 0xAA55FF,
            new int[]{45, 20, 40, 30}, 24, 18, 11,
            DamageColor.RED, 3, 4,
            3,
            () -> ModItems.FEATHER_OF_HONOR, 60,
            () -> ModItems.FEATHER_OF_HONOR_SUIT, 50,
            "feather_of_honor", "명예의 깃털", "최대 SP +2, 이동 속도 +4%", 3,
            new float[]{0.8f, 0.4f, 1.2f, 2.0f},
            List.of(
                    "불꽃으로 이루어진 거대한 새. 깃털 하나하나가 타오르며, 날갯짓마다 격리실이 주황빛으로 물든다.",
                    "카운터는 거꾸로 움직인다. 작업 결과가 좋음이면 카운터가 1 내려가고, 나쁨이면 1 오른다. 카운터가 낮을수록 작업 실패 피해가 커지지만, 카운터가 1이거나 HP가 위태로운 직원은 불꽃으로 되살아난다.",
                    "카운터가 0이 되면 탈출해 주변을 태우고, 공격당하면 불길을 두르고 곧장 돌진한다. 제압하면 명예의 깃털을 하나 남긴다."
            ));

    public static final AbnoProfile CLOUDED_MONK = new AbnoProfile(
            "clouded_monk", "구름 낀 수도승", "D-01-110", "WAW", 0xAA55FF,
            new int[]{15, 45, 40, 35}, 22, 15, 7,
            DamageColor.WHITE, 4, 6,
            3,
            () -> ModItems.AMITA, 70,
            () -> ModItems.AMITA_SUIT, 50,
            "amita", "아미타", "최대 HP +6, 최대 SP -4", 3,
            new float[]{1.2f, 0.8f, 0.8f, 1.5f},
            List.of(
                    "회색 구름에 온몸을 감싼 수도승. 얼굴은 구름 속에 가려 보이지 않고, 구름 밖으로 드러난 두 팔만이 유난히 길게 늘어져 있다.",
                    "통찰 작업이 가장 좋고 애착과 억압이 그다음, 본능이 가장 나쁘다. 통찰 작업을 마치면 작업자의 SP가 회복되지만, 보통이면 30% 확률로, 나쁨이면 반드시 클리포트 카운터가 1 내려간다. 직원이 세 명 죽을 때마다 카운터가 1 내려가며, 탈출한 용(Yin)이 격리실 근처에 있으면 카운터가 곧바로 0이 된다.",
                    "카운터가 0이 되면 탈출해 직원을 쫓는다. 체력이 70% 아래로 떨어지면 구름을 모은 뒤 정면으로 곧장 돌진하며, 돌진에 닿은 직원은 큰 피해를 입고 수도승은 상처를 메운다."
            ));

    public static final AbnoProfile IL_PIANTO = new AbnoProfile(
            "il_pianto", "달의 울음", "D-01-105", "WAW", 0xAA55FF,
            new int[]{30, 45, 35, 25}, 20, 13, 7,
            DamageColor.WHITE, 4, 6,
            3,
            () -> ModItems.MOONLIGHT, 70,
            () -> ModItems.MOONLIGHT_SUIT, 60,
            "moonlight", "달빛", "최대 HP +1, 최대 SP +1, 작업 성공률 +1%, 이동 속도 +1%", 3,
            new float[]{1.2f, 0.0f, 1.0f, 2.0f},
            List.of(
                    "뚜껑이 우는 초승달 얼굴로 된 창백한 그랜드 피아노. 탈출하면 뚜껑이 달머리로 솟아오른 키 큰 형상이 된다.",
                    "통찰 작업이 가장 좋고 애착과 억압이 그다음, 본능이 가장 나쁘다. 통찰 작업을 마치면 연주가 시작되어 모든 직원이 잠시 빨라지지만, 세 번째 연주에서는 연주자가 피아노에 갇히고 카운터가 0이 되어 곧바로 탈출한다. 보통이면 30% 확률로, 나쁨이면 반드시 카운터가 1 내려간다.",
                    "탈출하면 직원을 쫓으며, 갇힌 연주자는 계속 느려지고 피해를 입는다. 주기적으로 몸을 웅크렸다가 정면으로 돌진한다. 연주가 끝나면 연주자의 체력을 절반으로 깎고 격리실로 돌아간다."
            ));

    public static final AbnoProfile BACKWARD_CLOCK = new AbnoProfile(
            "backward_clock", "거꾸로 가는 시계", "D-09-104", "WAW", 0xAA55FF,
            new int[]{0, 0, 0, 0}, 0, 0, 0,
            DamageColor.WHITE, 0, 0,
            0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            "", "", "", 0,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "도구형 환상체. 작업도, PE-BOX도, E.G.O도 없다. 낡은 괘종시계 위에 등불 네 개가 나란히 걸려 있고, 시곗바늘은 거꾸로 돈다.",
                    "가장 높은 스탯이 III 등급(45) 이상인 직원이 사용할 때마다 등불이 하나씩 켜진다. 네 개가 모두 켜진 뒤에는 IV 등급 이상 스탯을 세 개 가진 직원만이 시계를 돌릴 수 있다.",
                    "시계를 돌리면 탈출한 환상체가 격리실로 돌아가고 하수인들이 사라지며 모든 클리포트 카운터가 복구된다. 대신 시계를 돌린 직원은 그 자리에서 죽고, 시계는 그날 하루 멈춘다. 감당하지 못한 직원이 돌리면 주변 직원이 죽거나 패닉에 빠진다."
            ));

    public static final AbnoProfile DIMENSION_REFRACTION = new AbnoProfile(
            "dimension_refraction", "차원 굴절 변이체", "O-03-88", "WAW", 0xAA55FF,
            new int[]{35, 45, 30, 25}, 22, 17, 9,
            DamageColor.WHITE, 4, 7,
            2,
            () -> ModItems.DIFFRACTION, 50,
            () -> ModItems.DIFFRACTION_SUIT, 50,
            "diffraction", "회절", "최대 SP +6", 3,
            new float[]{0.0f, 1.5f, 0.8f, 1.0f},
            List.of(
                    "텅 빈 중심 주위로 굴절된 파편들이 떠다니는, 겨우 눈에 보이는 공간의 일그러짐. 파편마다 비치는 풍경이 조금씩 어긋나 있다.",
                    "작업이 40초를 넘기면 클리포트 카운터가 1 내려간다. 작업 결과가 나쁨이면 카운터가 1 내려간다.",
                    "카운터가 0이 되면 탈출해 모습을 감춘 채 직원 곁을 느리게 떠돈다. 닿은 직원은 보이지 않는 파편에 갈려 연속으로 피해를 입는다. 희미한 빛만이 그 위치를 알려준다."
            ));

    public static final AbnoProfile FLESH_IDOL = new AbnoProfile(
            "flesh_idol", "살점 우상", "T-09-79", "WAW", 0xAA55FF,
            new int[]{0, 0, 0, 0}, 0, 0, 0,
            DamageColor.WHITE, 0, 0,
            0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            "", "", "", 0,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "날고기와 뼈를 아무렇게나 쌓아 올린 조잡한 우상. 앞에는 작은 제단이 놓여 있고, 살점은 아직 축축하다.",
                    "누군가 제단 앞에 무릎을 꿇고 기도하는 동안, 시설의 모든 직원이 주기적으로 체력과 정신력을 회복한다.",
                    "그 값을 치르는 것은 기도하는 자 자신이다. 기도가 길어질수록 우상은 굶주려, 이따금 기도하는 자의 최대 체력 일부를 살점으로 삼킨다."
            ));

    public static final AbnoProfile EXPRESS_TRAIN = new AbnoProfile(
            "express_train", "지옥행 급행열차", "T-09-86", "WAW", 0xAA55FF,
            new int[]{0, 0, 0, 0}, 0, 0, 0,
            DamageColor.WHITE, 0, 0,
            0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            () -> net.minecraft.world.item.Items.AIR, 0,
            "", "", "", 0,
            new float[]{1f, 1f, 1f, 1f},
            List.of(
                    "지붕에 등불 네 개가 달린 작은 매표소. 앞에는 선로가 놓여 있고, 선로 끝은 어둠 속으로 사라진다.",
                    "표를 사면 켜진 등불의 수만큼 값이 매겨진다. 등불이 적을 때는 표를 팔지 않고, 많을수록 회복의 대가가 커진다.",
                    "등불 네 개가 모두 켜진 채로 시간이 흐르면 지옥행 급행열차가 선로를 따라 시설을 가로지른다. 선로 위에 서 있던 직원은 짓눌린다."
            ));

    public static final Map<String, AbnoProfile> BY_ID = Map.ofEntries(
            Map.entry(EXPRESS_TRAIN.id, EXPRESS_TRAIN),
            Map.entry(FLESH_IDOL.id, FLESH_IDOL),
            Map.entry(DIMENSION_REFRACTION.id, DIMENSION_REFRACTION),
            Map.entry(BACKWARD_CLOCK.id, BACKWARD_CLOCK),
            Map.entry(IL_PIANTO.id, IL_PIANTO),
            Map.entry(CLOUDED_MONK.id, CLOUDED_MONK),
            Map.entry(FIREBIRD.id, FIREBIRD),
            Map.entry(BURROWING_HEAVEN.id, BURROWING_HEAVEN),
            Map.entry(DREAMING_CURRENT.id, DREAMING_CURRENT),
            Map.entry(NAKED_NEST.id, NAKED_NEST),
            Map.entry(PARASITE_TREE.id, PARASITE_TREE),
            Map.entry(LITTLE_PRINCE.id, LITTLE_PRINCE),
            Map.entry(YIN.id, YIN),
            Map.entry(YANG.id, YANG),
            Map.entry(SNOW_WHITE_APPLE.id, SNOW_WHITE_APPLE),
            Map.entry(BLACK_SWAN.id, BLACK_SWAN),
            Map.entry(QUEEN_BEE.id, QUEEN_BEE),
            Map.entry(ALRIUNE.id, ALRIUNE),
            Map.entry(BIG_BIRD.id, BIG_BIRD),
            Map.entry(JUDGEMENT_BIRD.id, JUDGEMENT_BIRD),
            Map.entry(QUEEN_HATRED.id, QUEEN_HATRED),
            Map.entry(KNIGHT_DESPAIR.id, KNIGHT_DESPAIR),
            Map.entry(KING_GREED.id, KING_GREED),
            Map.entry(LITTLE_RED.id, LITTLE_RED),
            Map.entry(BIG_BAD_WOLF.id, BIG_BAD_WOLF),
            Map.entry(ONE_SIN.id, ONE_SIN),
            Map.entry(FORSAKEN_MURDERER.id, FORSAKEN_MURDERER),
            Map.entry(SCORCHED_GIRL.id, SCORCHED_GIRL),
            Map.entry(HAPPY_TEDDY.id, HAPPY_TEDDY),
            Map.entry(FAIRY_FESTIVAL.id, FAIRY_FESTIVAL),
            Map.entry(PUNISHING_BIRD.id, PUNISHING_BIRD),
            Map.entry(TODAYS_SHY_LOOK.id, TODAYS_SHY_LOOK),
            Map.entry(LADY_FACING_THE_WALL.id, LADY_FACING_THE_WALL),
            Map.entry(CRUMBLING_ARMOR.id, CRUMBLING_ARMOR),
            Map.entry(OLD_LADY.id, OLD_LADY),
            Map.entry(SPIDER_BUD.id, SPIDER_BUD),
            Map.entry(VOID_DREAM.id, VOID_DREAM),
            Map.entry(BEAUTY_BEAST.id, BEAUTY_BEAST),
            Map.entry(MEAT_LANTERN.id, MEAT_LANTERN),
            Map.entry(FRAGMENT.id, FRAGMENT),
            Map.entry(MHZ.id, MHZ),
            Map.entry(RED_SHOES.id, RED_SHOES),
            Map.entry(SINGING_MACHINE.id, SINGING_MACHINE),
            Map.entry(PORCCUBUS.id, PORCCUBUS),
            Map.entry(LAETITIA.id, LAETITIA),
            Map.entry(HELPER.id, HELPER),
            Map.entry(CHILD_GALAXY.id, CHILD_GALAXY),
            Map.entry(FREISCHUTZ.id, FREISCHUTZ),
            Map.entry(FETUS.id, FETUS),
            Map.entry(SNOW_QUEEN.id, SNOW_QUEEN),
            Map.entry(WOODSMAN.id, WOODSMAN),
            Map.entry(TREE_SAP.id, TREE_SAP),
            Map.entry(SHELTER.id, SHELTER),
            Map.entry(PORTRAIT.id, PORTRAIT),
            Map.entry(NOTES.id, NOTES),
            Map.entry(SCARECROW.id, SCARECROW),
            Map.entry(RUDOLTA.id, RUDOLTA),
            Map.entry(FUNERAL.id, FUNERAL),
            Map.entry(SCHADENFREUDE.id, SCHADENFREUDE)
    );

    public boolean canBreach() {
        return qliphothMax > 0;
    }

    /** 도구형 환상체: 작업도 PE-BOX도 없다 */
    public boolean isTool() {
        return maxBox == 0;
    }

    public float resistTo(DamageColor c) {
        return resist[c.ordinal()];
    }
}
