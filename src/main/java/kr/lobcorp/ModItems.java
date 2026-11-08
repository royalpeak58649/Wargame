package kr.lobcorp;

import kr.lobcorp.item.EgoWeaponItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ModItems {
    private ModItems() {}

    public static final ArmorMaterial PENITENCE_MAT = material("penitence", 5);
    public static final ArmorMaterial REGRET_MAT = material("regret", 6);
    public static final ArmorMaterial MATCH_FLAME_MAT = material("match_flame", 6);
    public static final ArmorMaterial BEAR_PAWS_MAT = material("bear_paws", 7);
    public static final ArmorMaterial WINGBEAT_MAT = material("wingbeat", 5);
    public static final ArmorMaterial BEAK_MAT = material("beak", 5);
    public static final ArmorMaterial TODAYS_EXPRESSION_MAT = material("todays_expression", 5);
    public static final ArmorMaterial SCREAMING_WEDGE_MAT = material("screaming_wedge", 5);
    public static final ArmorMaterial DAREDEVIL_MAT = material("daredevil", 6);
    public static final ArmorMaterial SOLITUDE_MAT = material("solitude", 5);
    public static final ArmorMaterial RED_EYES_MAT = material("red_eyes", 6);
    public static final ArmorMaterial ENGULFING_DREAM_MAT = material("engulfing_dream", 5);
    public static final ArmorMaterial HORN_MAT = material("horn", 5);
    public static final ArmorMaterial LANTERN_MAT = material("lantern", 5);
    public static final ArmorMaterial FRAGMENTS_MAT = material("fragments", 5);
    public static final ArmorMaterial NOISE_MAT = material("noise", 5);
    public static final ArmorMaterial SANGUINE_DESIRE_MAT = material("sanguine_desire", 6);
    public static final ArmorMaterial HARMONY_MAT = material("harmony", 6);
    public static final ArmorMaterial PLEASURE_MAT = material("pleasure", 6);
    public static final ArmorMaterial LAETITIA_MAT = material("laetitia", 7);
    public static final ArmorMaterial GRINDER_MAT = material("grinder", 6);
    public static final ArmorMaterial OUR_GALAXY_MAT = material("our_galaxy", 6);
    public static final ArmorMaterial MAGIC_BULLET_MAT = material("magic_bullet", 7);
    public static final ArmorMaterial SYRINX_MAT = material("syrinx", 6);
    public static final ArmorMaterial FROST_SPLINTER_MAT = material("frost_splinter", 6);
    public static final ArmorMaterial LOGGING_MAT = material("logging", 6);
    public static final ArmorMaterial HARVEST_MAT = material("harvest", 6);
    public static final ArmorMaterial CHRISTMAS_MAT = material("christmas", 6);
    public static final ArmorMaterial SOLEMN_LAMENT_MAT = material("solemn_lament", 6);
    public static final ArmorMaterial GAZE_MAT = material("gaze", 6);
    public static final ArmorMaterial LAMP_MAT = material("lamp", 7);
    public static final ArmorMaterial JUSTITIA_MAT = material("justitia", 9);
    public static final ArmorMaterial LOVE_AND_HATE_MAT = material("love_and_hate", 7);
    public static final ArmorMaterial TEARS_SWORD_MAT = material("tears_sword", 7);
    public static final ArmorMaterial GOLD_RUSH_MAT = material("gold_rush", 7);
    public static final ArmorMaterial CRIMSON_SCAR_MAT = material("crimson_scar", 7);
    public static final ArmorMaterial COBALT_SCAR_MAT = material("cobalt_scar", 7);
    public static final ArmorMaterial FAINT_AROMA_MAT = material("faint_aroma", 6);
    public static final ArmorMaterial HORNET_MAT = material("hornet", 7);
    public static final ArmorMaterial BLACK_SWAN_MAT = material("black_swan", 6);
    public static final ArmorMaterial DISCORD_MAT = material("discord", 6);
    public static final ArmorMaterial GREEN_STEM_MAT = material("green_stem", 6);
    public static final ArmorMaterial SPORE_MAT = material("spore", 6);
    public static final ArmorMaterial HYPOCRISY_MAT = material("hypocrisy", 7);
    public static final ArmorMaterial EXUVIAE_MAT = material("exuviae", 7);
    public static final ArmorMaterial ECSTASY_MAT = material("ecstasy", 6);
    public static final ArmorMaterial HEAVEN_MAT = material("heaven", 6);
    public static final ArmorMaterial FEATHER_OF_HONOR_MAT = material("feather_of_honor", 6);
    public static final ArmorMaterial AMITA_MAT = material("amita", 7);
    public static final ArmorMaterial MOONLIGHT_MAT = material("moonlight", 7);
    public static final ArmorMaterial DIFFRACTION_MAT = material("diffraction", 6);

    public static Item PENITENCE;
    public static Item PENITENCE_SUIT;
    public static Item REGRET;
    public static Item REGRET_SUIT;
    public static Item MATCH_FLAME;
    public static Item MATCH_FLAME_SUIT;
    public static Item BEAR_PAWS;
    public static Item BEAR_PAWS_SUIT;
    public static Item WINGBEAT;
    public static Item WINGBEAT_SUIT;
    public static Item BEAK;
    public static Item BEAK_SUIT;
    public static Item TODAYS_EXPRESSION;
    public static Item TODAYS_EXPRESSION_SUIT;
    public static Item SCREAMING_WEDGE;
    public static Item SCREAMING_WEDGE_SUIT;
    public static Item DAREDEVIL;
    public static Item DAREDEVIL_SUIT;
    public static Item SOLITUDE;
    public static Item SOLITUDE_SUIT;
    public static Item RED_EYES;
    public static Item RED_EYES_SUIT;
    public static Item ENGULFING_DREAM;
    public static Item ENGULFING_DREAM_SUIT;
    public static Item HORN;
    public static Item HORN_SUIT;
    public static Item LANTERN;
    public static Item LANTERN_SUIT;
    public static Item FRAGMENTS;
    public static Item FRAGMENTS_SUIT;
    public static Item NOISE;
    public static Item NOISE_SUIT;
    public static Item SANGUINE_DESIRE;
    public static Item SANGUINE_DESIRE_SUIT;
    public static Item HARMONY;
    public static Item HARMONY_SUIT;
    public static Item PLEASURE;
    public static Item PLEASURE_SUIT;
    public static Item LAETITIA;
    public static Item LAETITIA_SUIT;
    public static Item GRINDER;
    public static Item GRINDER_SUIT;
    public static Item OUR_GALAXY;
    public static Item OUR_GALAXY_SUIT;
    public static Item MAGIC_BULLET;
    public static Item MAGIC_BULLET_SUIT;
    public static Item SYRINX;
    public static Item SYRINX_SUIT;
    public static Item FROST_SPLINTER;
    public static Item FROST_SPLINTER_SUIT;
    public static Item LOGGING;
    public static Item LOGGING_SUIT;
    public static Item HARVEST;
    public static Item HARVEST_SUIT;
    public static Item CHRISTMAS;
    public static Item CHRISTMAS_SUIT;
    public static Item SOLEMN_LAMENT;
    public static Item SOLEMN_LAMENT_SUIT;
    public static Item GAZE;
    public static Item GAZE_SUIT;
    public static Item LAMP;
    public static Item LAMP_SUIT;
    public static Item JUSTITIA;
    public static Item JUSTITIA_SUIT;
    public static Item LOVE_AND_HATE;
    public static Item LOVE_AND_HATE_SUIT;
    public static Item TEARS_SWORD;
    public static Item TEARS_SWORD_SUIT;
    public static Item GOLD_RUSH;
    public static Item GOLD_RUSH_SUIT;
    public static Item CRIMSON_SCAR;
    public static Item CRIMSON_SCAR_SUIT;
    public static Item COBALT_SCAR;
    public static Item COBALT_SCAR_SUIT;
    public static Item FAINT_AROMA;
    public static Item FAINT_AROMA_SUIT;
    public static Item HORNET;
    public static Item HORNET_SUIT;
    public static Item BLACK_SWAN;
    public static Item BLACK_SWAN_SUIT;
    public static Item DISCORD;
    public static Item DISCORD_SUIT;
    public static Item GREEN_STEM;
    public static Item GREEN_STEM_SUIT;
    public static Item SPORE;
    public static Item SPORE_SUIT;
    public static Item HYPOCRISY;
    public static Item HYPOCRISY_SUIT;
    public static Item EXUVIAE;
    public static Item EXUVIAE_SUIT;
    public static Item ECSTASY;
    public static Item ECSTASY_SUIT;
    public static Item HEAVEN;
    public static Item HEAVEN_SUIT;
    public static Item FEATHER_OF_HONOR;
    public static Item FEATHER_OF_HONOR_SUIT;
    public static Item AMITA;
    public static Item AMITA_SUIT;
    public static Item MOONLIGHT;
    public static Item MOONLIGHT_SUIT;
    public static Item DIFFRACTION;
    public static Item DIFFRACTION_SUIT;

    private static ArmorMaterial material(String name, int chestDefense) {
        return new ArmorMaterial(
                30,
                Map.of(ArmorType.HELMET, 1, ArmorType.CHESTPLATE, chestDefense, ArmorType.LEGGINGS, 2,
                        ArmorType.BOOTS, 1, ArmorType.BODY, chestDefense),
                10, SoundEvents.ARMOR_EQUIP_LEATHER, 0f, 0f, ItemTags.REPAIRS_LEATHER_ARMOR,
                ResourceKey.create(EquipmentAssets.ROOT_ID, LobCorp.id(name)));
    }

    private static ItemAttributeModifiers weaponStats(String name, double damage, double speed) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(LobCorp.id(name + "_damage"), damage,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(LobCorp.id(name + "_speed"), speed,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, LobCorp.id(name));
        Item item = factory.apply(props.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    private static ItemLore lore(String... lines) {
        return new ItemLore(java.util.Arrays.stream(lines).map(l -> (Component) Component.literal(l)
                .withStyle(s -> s.withColor(0xAAAAAA).withItalic(false))).toList());
    }

    public static void init() {
        PENITENCE = register("penitence", p -> new EgoWeaponItem(p, DamageColor.WHITE),
                new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
                        .attributes(weaponStats("penitence", 5.0, -3.0))
                        .component(DataComponents.LORE, lore("O-03-03 · ZAYIN", "WHITE 피해 둔기", "죄를 짊어진 자의 무게.")));

        PENITENCE_SUIT = register("penitence_suit", Item::new,
                new Item.Properties().humanoidArmor(PENITENCE_MAT, ArmorType.CHESTPLATE)
                        .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                        .component(DataComponents.LORE, lore("O-03-03 · ZAYIN", "WHITE 저항: 작업 실패 시 SP 피해 -1")));

        REGRET = register("regret", p -> new EgoWeaponItem(p, DamageColor.RED),
                new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
                        .attributes(weaponStats("regret", 7.0, -3.2))
                        .component(DataComponents.LORE, lore("T-01-54 · TETH", "RED 피해 둔기", "쇠가 되어 가는 머리로 벽을 두드리던 소리.")));

        REGRET_SUIT = register("regret_suit", Item::new,
                new Item.Properties().humanoidArmor(REGRET_MAT, ArmorType.CHESTPLATE)
                        .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                        .component(DataComponents.LORE, lore("T-01-54 · TETH", "RED 저항: 작업 실패 시 HP 피해 -1")));
        initMore();
    }

    private static Item weapon(String id, DamageColor c, double dmg, double speed, String... lore) {
        return register(id, p -> new EgoWeaponItem(p, c),
                new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
                        .attributes(weaponStats(id, dmg, speed))
                        .component(DataComponents.LORE, lore(lore)));
    }

    private static Item suit(String id, ArmorMaterial mat, String... lore) {
        return register(id, Item::new,
                new Item.Properties().humanoidArmor(mat, ArmorType.CHESTPLATE)
                        .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                        .component(DataComponents.LORE, lore(lore)));
    }

    /** 신규 E.G.O (수치는 임의) */
    public static void initMore() {
        MATCH_FLAME = weapon("match_flame", DamageColor.RED, 8.0, -3.4,
                "F-01-02 · TETH", "RED 피해 대형 무기", "꺼지지 않는 불씨를 쥔 손.");
        MATCH_FLAME_SUIT = suit("match_flame_suit", MATCH_FLAME_MAT,
                "F-01-02 · TETH", "RED 저항: 작업 실패 시 HP 피해 -1");
        BEAR_PAWS = weapon("bear_paws", DamageColor.RED, 4.0, -1.6,
                "T-04-06 · HE", "RED 피해 주먹, 빠른 연타", "보드라운 털 아래 단단한 무언가.");
        BEAR_PAWS_SUIT = suit("bear_paws_suit", BEAR_PAWS_MAT,
                "T-04-06 · HE", "WHITE 저항: 작업 실패 시 SP 피해 -1");
        WINGBEAT = weapon("wingbeat", DamageColor.RED, 4.0, -2.4,
                "F-04-83 · ZAYIN", "RED 피해", "작은 날개들이 한꺼번에 퍼덕이는 소리.");
        WINGBEAT_SUIT = suit("wingbeat_suit", WINGBEAT_MAT,
                "F-04-83 · ZAYIN", "RED 저항: 작업 실패 시 HP 피해 -1");
        BEAK = weapon("beak", DamageColor.RED, 3.0, -1.0,
                "O-02-56 · TETH", "RED 피해, 매우 빠른 연타", "작은 부리가 남긴 자국은 오래 남는다.");
        BEAK_SUIT = suit("beak_suit", BEAK_MAT,
                "O-02-56 · TETH", "RED 저항: 작업 실패 시 HP 피해 -1");
        TODAYS_EXPRESSION = weapon("todays_expression", DamageColor.BLACK, 4.0, -2.4,
                "O-01-92 · TETH", "BLACK 피해", "오늘은 어떤 얼굴을 하고 있을까.");
        TODAYS_EXPRESSION_SUIT = suit("todays_expression_suit", TODAYS_EXPRESSION_MAT,
                "O-01-92 · TETH", "BLACK 저항: 작업 실패 시 HP·SP 피해 -1");
        SCREAMING_WEDGE = weapon("screaming_wedge", DamageColor.WHITE, 5.0, -2.6,
                "F-01-18 · TETH", "WHITE 피해", "벽을 향해 박아 넣은, 아직 식지 않은 비명.");
        SCREAMING_WEDGE_SUIT = suit("screaming_wedge_suit", SCREAMING_WEDGE_MAT,
                "F-01-18 · TETH", "WHITE 저항: 작업 실패 시 SP 피해 -1");
        DAREDEVIL = weapon("daredevil", DamageColor.PALE, 4.0, -2.0,
                "O-05-61 · TETH", "PALE 피해", "갑옷 곁에 놓인 채 녹슨 칼날. 겁 없는 자만이 든다.");
        DAREDEVIL_SUIT = suit("daredevil_suit", DAREDEVIL_MAT,
                "O-05-61 · TETH", "RED 저항: 작업 실패 시 HP 피해 -1");
        SOLITUDE = weapon("solitude", DamageColor.WHITE, 4.0, -2.4,
                "O-01-12 · TETH", "WHITE 피해", "아무도 찾아오지 않는 방의 공기가 칼날에 서려 있다.");
        SOLITUDE_SUIT = suit("solitude_suit", SOLITUDE_MAT,
                "O-01-12 · TETH", "WHITE 저항: 작업 실패 시 SP 피해 -1");
        RED_EYES = weapon("red_eyes", DamageColor.RED, 5.0, -2.2,
                "T-02-43 · TETH", "RED 피해", "어둠 속에서 여덟 개의 붉은 점이 천천히 깜빡인다.");
        RED_EYES_SUIT = suit("red_eyes_suit", RED_EYES_MAT,
                "T-02-43 · TETH", "RED 저항: 작업 실패 시 HP 피해 -1");
        ENGULFING_DREAM = weapon("engulfing_dream", DamageColor.WHITE, 4.0, -2.4,
                "T-02-99 · TETH", "WHITE 피해", "잠결에 삼킨 것들이 아직도 목구멍에서 몸부림친다.");
        ENGULFING_DREAM_SUIT = suit("engulfing_dream_suit", ENGULFING_DREAM_MAT,
                "T-02-99 · TETH", "WHITE 저항: 작업 실패 시 SP 피해 -1");
        HORN = weapon("horn", DamageColor.RED, 4.0, -2.0,
                "F-02-44 · TETH", "RED 피해 창", "창끝에 박힌 수많은 눈이 아직 깜빡이고, 그 사이로 보랏빛 꽃이 피어 있다.");
        HORN_SUIT = suit("horn_suit", HORN_MAT,
                "F-02-44 · TETH", "RED·WHITE 저항: 작업 실패 시 HP·SP 피해 -1");
        LANTERN = weapon("lantern", DamageColor.BLACK, 7.0, -3.2,
                "O-04-84 · TETH", "BLACK 피해 대형 둔기, 느리고 묵직함", "머리통만 한 쇠망치 끝에서 푸른 불빛이 새어 나온다.");
        LANTERN_SUIT = suit("lantern_suit", LANTERN_MAT,
                "O-04-84 · TETH", "RED·WHITE 저항: 작업 실패 시 HP·SP 피해 -1");
        FRAGMENTS = weapon("fragments", DamageColor.BLACK, 4.0, -2.0,
                "O-03-60 · TETH", "BLACK 피해 창, 빠른 찌르기", "밤하늘의 색을 띤 창날 곳곳에 작은 하트가 박혀 있다.");
        FRAGMENTS_SUIT = suit("fragments_suit", FRAGMENTS_MAT,
                "O-03-60 · TETH", "BLACK 저항: 작업 실패 시 HP·SP 피해 -1");
        NOISE = weapon("noise", DamageColor.WHITE, 4.0, -2.4,
                "T-06-27 · TETH", "WHITE 피해 지팡이", "손잡이를 쥘 때마다 지직거리는 잡음이 손바닥을 타고 올라온다.");
        NOISE_SUIT = suit("noise_suit", NOISE_MAT,
                "T-06-27 · TETH", "WHITE·BLACK 저항, RED 취약: 작업 실패 시 SP·HP 피해 -1");
        SANGUINE_DESIRE = weapon("sanguine_desire", DamageColor.RED, 7.0, -3.0,
                "O-04-08 · HE", "RED 피해 대형 도끼, 느리고 묵직함", "춤추고 싶어 하는 붉은 도끼날. 손잡이가 조금씩 따뜻해진다.");
        SANGUINE_DESIRE_SUIT = suit("sanguine_desire_suit", SANGUINE_DESIRE_MAT,
                "O-04-08 · HE", "RED 저항: 작업 실패 시 HP 피해 -1");
        HARMONY = weapon("harmony", DamageColor.WHITE, 9.0, -3.4,
                "O-05-30 · HE", "WHITE 피해 대형 둔기, 매우 느리고 묵직함", "휘두를 때마다 나무 상자 속에서 낮은 화음이 새어 나온다.");
        HARMONY_SUIT = suit("harmony_suit", HARMONY_MAT,
                "O-05-30 · HE", "WHITE 저항: 작업 실패 시 SP 피해 -1");
        PLEASURE = weapon("pleasure", DamageColor.BLACK, 3.0, -1.8,
                "O-02-98 · HE", "BLACK 피해 채찍, 빠른 연타", "가시 돋친 초록 줄기. 휘두를 때마다 손끝이 저릿하다.");
        PLEASURE_SUIT = suit("pleasure_suit", PLEASURE_MAT,
                "O-02-98 · HE", "WHITE·BLACK 저항, RED 취약: 작업 실패 시 SP·HP 피해 -1");
        LAETITIA = weapon("laetitia", DamageColor.BLACK, 4.0, -1.6,
                "O-01-67 · HE", "BLACK 피해 장난감 총, 빠른 연타", "귀여운 장난감 총. 방아쇠를 당기면 이빨이 튀어나온다.");
        LAETITIA_SUIT = suit("laetitia_suit", LAETITIA_MAT,
                "O-01-67 · HE", "RED·WHITE·BLACK 저항: 작업 실패 시 HP·SP 피해 -1");
        GRINDER = weapon("grinder", DamageColor.RED, 4.0, -1.6,
                "T-05-41 · HE", "RED 피해, 빠른 연타", "청소용 팔 사이에서 낮게 돌아가는 칼날. 아직 닦는 소리가 난다.");
        GRINDER_SUIT = suit("grinder_suit", GRINDER_MAT,
                "T-05-41 · HE", "RED 저항, WHITE 취약: 작업 실패 시 HP 피해 -1");
        OUR_GALAXY = weapon("our_galaxy", DamageColor.BLACK, 6.0, -3.0,
                "O-01-55 · HE", "BLACK 피해 대형 둔기", "별가루가 박힌 둔기. 휘두르면 작은 별들이 흩날린다.");
        OUR_GALAXY_SUIT = suit("our_galaxy_suit", OUR_GALAXY_MAT,
                "O-01-55 · HE", "RED·WHITE 저항: 작업 실패 시 HP·SP 피해 -1");
        MAGIC_BULLET = weapon("magic_bullet", DamageColor.BLACK, 6.0, -3.0,
                "F-01-69 · HE", "BLACK 피해 장총, 느리고 묵직함", "한 번도 빗나간 적 없는 총신. 방아쇠는 아직 따뜻하다.");
        MAGIC_BULLET_SUIT = suit("magic_bullet_suit", MAGIC_BULLET_MAT,
                "F-01-69 · HE", "RED·WHITE·BLACK 저항: 작업 실패 시 HP·SP 피해 -1");
        SYRINX = weapon("syrinx", DamageColor.WHITE, 3.0, -2.0,
                "O-01-15 · HE", "WHITE 피해, 빠른 연타", "뼈로 깎은 피리. 불면 갓난아이의 울음 같은 소리가 새어 나온다.");
        SYRINX_SUIT = suit("syrinx_suit", SYRINX_MAT,
                "O-01-15 · HE", "WHITE·BLACK 저항, RED 취약: 작업 실패 시 SP·HP 피해 -1");
        FROST_SPLINTER = weapon("frost_splinter", DamageColor.WHITE, 5.0, -2.0,
                "F-01-37 · HE", "WHITE 피해 창", "끝을 만질 때마다 손끝이 하얗게 언다. 차가운 창날에는 서리가 내려앉아 있다.");
        FROST_SPLINTER_SUIT = suit("frost_splinter_suit", FROST_SPLINTER_MAT,
                "F-01-37 · HE", "WHITE·BLACK 저항, RED 취약: 작업 실패 시 SP·HP 피해 -1");
        LOGGING = weapon("logging", DamageColor.RED, 8.0, -3.2,
                "F-05-32 · HE", "RED 피해 대형 도끼, 느리고 묵직함", "주석으로 된 도끼날. 자루에는 아직 나무 진이 마르지 않았다.");
        LOGGING_SUIT = suit("logging_suit", LOGGING_MAT,
                "F-05-32 · HE", "RED 저항, WHITE 취약: 작업 실패 시 HP 피해 -1");
        HARVEST = weapon("harvest", DamageColor.BLACK, 5.0, -2.2,
                "F-01-87 · HE", "BLACK 피해 낫창", "볏단을 베어 온 낫. 날에 밀짚 한 오라기가 붙어 있다.");
        HARVEST_SUIT = suit("harvest_suit", HARVEST_MAT,
                "F-01-87 · HE", "RED·WHITE 저항, BLACK 취약: 작업 실패 시 HP·SP 피해 -1");
        CHRISTMAS = weapon("christmas", DamageColor.WHITE, 6.0, -3.0,
                "F-02-49 · HE", "WHITE 피해 대형 둔기, 느리고 묵직함", "종소리가 울릴 때마다 손잡이가 조금씩 차가워진다.");
        CHRISTMAS_SUIT = suit("christmas_suit", CHRISTMAS_MAT,
                "F-02-49 · HE", "RED·WHITE 저항, BLACK 취약: 작업 실패 시 HP·SP 피해 -1");
        SOLEMN_LAMENT = weapon("solemn_lament", DamageColor.WHITE, 2.5, -1.0,
                "T-01-68 · HE", "WHITE 피해 쌍권총, 매우 빠른 연사", "검은 총신과 흰 총신 한 쌍. 방아쇠를 당길 때마다 나비가 한 마리씩 흩날린다.");
        SOLEMN_LAMENT_SUIT = suit("solemn_lament_suit", SOLEMN_LAMENT_MAT,
                "T-01-68 · HE", "WHITE·BLACK 저항, RED 취약: 작업 실패 시 SP·HP 피해 -1");
        GAZE = weapon("gaze", DamageColor.RED, 3.0, -1.6,
                "O-05-76 · HE", "RED 피해, 빠른 연타", "자물쇠에 가려진 열쇠 구멍. 들여다보면 안쪽에서 무언가가 되돌아본다.");
        GAZE_SUIT = suit("gaze_suit", GAZE_MAT,
                "O-05-76 · HE", "WHITE 저항: 작업 실패 시 SP 피해 -1");
        LAMP = weapon("lamp", DamageColor.BLACK, 9.0, -3.2,
                "O-02-40 · WAW", "BLACK 피해 대형 둔기, 느리고 묵직함", "수십 개의 눈이 박힌 등불. 휘두를 때마다 노란 빛이 흔들린다.");
        LAMP_SUIT = suit("lamp_suit", LAMP_MAT,
                "O-02-40 · WAW", "RED·WHITE·BLACK 저항: 작업 실패 시 HP·SP 피해 -1");
        JUSTITIA = weapon("justitia", DamageColor.PALE, 6.0, -2.4,
                "O-02-62 · WAW", "PALE 피해 대형 둔기", "금빛 저울이 매달린 둔기. 휘두를 때마다 접시가 조용히 흔들린다.");
        JUSTITIA_SUIT = suit("justitia_suit", JUSTITIA_MAT,
                "O-02-62 · WAW", "모든 피해 저항: 작업 실패 시 HP·SP 피해 -1");
        LOVE_AND_HATE = weapon("love_and_hate", DamageColor.BLACK, 5.0, -2.0,
                "O-01-04 · WAW", "BLACK 피해 지팡이", "별이 달린 분홍 지팡이. 휘두를 때마다 반짝이는 가루가 흩날린다.");
        LOVE_AND_HATE_SUIT = suit("love_and_hate_suit", LOVE_AND_HATE_MAT,
                "O-01-04 · WAW", "RED·WHITE·BLACK 저항: 작업 실패 시 HP·SP 피해 -1");
        TEARS_SWORD = weapon("tears_sword", DamageColor.WHITE, 5.0, -1.8,
                "O-01-73 · WAW", "WHITE 피해 검, 빠른 연타", "눈물을 식혀 벼려 낸 날. 아직 마르지 않았다.");
        TEARS_SWORD_SUIT = suit("tears_sword_suit", TEARS_SWORD_MAT,
                "O-01-73 · WAW", "모든 피해 저항: 작업 실패 시 HP·SP 피해 -1");
        GOLD_RUSH = weapon("gold_rush", DamageColor.RED, 4.0, -1.4,
                "O-01-64 · WAW", "RED 피해 황금 건틀릿, 빠른 연타", "손등을 덮은 황금 장갑. 주먹을 쥘 때마다 보석이 작게 울린다.");
        GOLD_RUSH_SUIT = suit("gold_rush_suit", GOLD_RUSH_MAT,
                "O-01-64 · WAW", "RED 저항: 작업 실패 시 HP 피해 -1");
        CRIMSON_SCAR = weapon("crimson_scar", DamageColor.RED, 5.0, -1.6,
                "F-01-57 · WAW", "RED 피해 대형 도끼, 빠른 연타", "붉은 두건 아래에서 아직 식지 않은 피가 뚝뚝 떨어진다.");
        CRIMSON_SCAR_SUIT = suit("crimson_scar_suit", CRIMSON_SCAR_MAT,
                "F-01-57 · WAW", "모든 피해 저항: 작업 실패 시 HP·SP 피해 -1");
        COBALT_SCAR = weapon("cobalt_scar", DamageColor.RED, 5.0, -1.6,
                "F-02-58 · WAW", "RED 피해 발톱, 빠른 연타", "푸른 강철로 벼려 낸 발톱. 쥐고 있으면 손끝이 짐승처럼 저릿하다.");
        COBALT_SCAR_SUIT = suit("cobalt_scar_suit", COBALT_SCAR_MAT,
                "F-02-58 · WAW", "RED 저항, WHITE·BLACK 저항: 작업 실패 시 HP·SP 피해 -1");
        FAINT_AROMA = weapon("faint_aroma", DamageColor.WHITE, 5.0, -2.4,
                "T-04-53 · WAW", "WHITE 피해 활, 빠른 연사", "꽃잎으로 감은 활시위. 당길 때마다 달콤한 냄새가 손끝에 남는다.");
        FAINT_AROMA_SUIT = suit("faint_aroma_suit", FAINT_AROMA_MAT,
                "T-04-53 · WAW", "WHITE·BLACK 저항: 작업 실패 시 SP·HP 피해 -1");
        HORNET = weapon("hornet", DamageColor.RED, 3.0, -1.0,
                "T-04-50 · WAW", "RED 피해 소총, 매우 빠른 연사", "날개가 달린 총신. 방아쇠를 당길 때마다 낮게 윙윙거린다.");
        HORNET_SUIT = suit("hornet_suit", HORNET_MAT,
                "T-04-50 · WAW", "RED·WHITE·BLACK 저항: 작업 실패 시 HP·SP 피해 -1");
        BLACK_SWAN = weapon("black_swan", DamageColor.BLACK, 4.0, -1.8,
                "F-02-70 · WAW", "BLACK 피해 양산, 빠른 연타", "검은 깃털로 덮인 양산. 펼치면 우산살 사이로 호숫가의 물빛이 비친다.");
        BLACK_SWAN_SUIT = suit("black_swan_suit", BLACK_SWAN_MAT,
                "F-02-70 · WAW", "RED 저항, WHITE 취약: 작업 실패 시 HP 피해 -1");
        DISCORD = weapon("discord", DamageColor.BLACK, 4.0, -1.6,
                "O-05-102 · WAW", "BLACK 피해 쌍어, 빠른 연타", "검은 비늘로 벼려 낸 한 쌍의 물고기. 쥐면 손바닥에서 차가운 물이 흐른다.");
        DISCORD_SUIT = suit("discord_suit", DISCORD_MAT,
                "O-05-102 · WAW", "WHITE·BLACK 저항, RED·PALE 취약: 작업 실패 시 SP·HP 피해 -1");
        GREEN_STEM = weapon("green_stem", DamageColor.BLACK, 5.0, -2.2,
                "F-04-42 · WAW", "BLACK 피해 창, 느린 찌르기", "사과에서 뽑아낸 초록 덩굴을 곧게 다듬은 창. 끝에서 아직 단내가 난다.");
        GREEN_STEM_SUIT = suit("green_stem_suit", GREEN_STEM_MAT,
                "F-04-42 · WAW", "WHITE·BLACK 저항, RED 취약: 작업 실패 시 SP·HP 피해 -1");
        SPORE = weapon("spore", DamageColor.WHITE, 4.0, -2.0,
                "O-04-66 · WAW", "WHITE 피해 창, 빠른 찌르기", "버섯 갓에서 뽑아낸 창끝에서 푸른 포자가 떨어진다.");
        SPORE_SUIT = suit("spore_suit", SPORE_MAT,
                "O-04-66 · WAW", "WHITE 저항: 작업 실패 시 SP 피해 -1");
        HYPOCRISY = weapon("hypocrisy", DamageColor.RED, 5.0, -2.4,
                "D-04-108 · WAW", "RED 피해 석궁, 긴 사거리", "창백한 나무의 가지로 깎아 만든 석궁. 시위를 당길 때마다 하얀 꽃잎이 떨어진다.");
        HYPOCRISY_SUIT = suit("hypocrisy_suit", HYPOCRISY_MAT,
                "D-04-108 · WAW", "RED·WHITE 저항: 작업 실패 시 HP·SP 피해 -1");
        EXUVIAE = weapon("exuviae", DamageColor.RED, 9.0, -3.2,
                "O-02-74 · WAW", "RED 피해 대포, 매우 느림", "둥지에서 벗겨낸 살점 껍질을 그대로 두른 대포. 쏠 때마다 창백한 뱀 한 마리가 함께 튀어나간다.");
        EXUVIAE_SUIT = suit("exuviae_suit", EXUVIAE_MAT,
                "O-02-74 · WAW", "RED 저항: 작업 실패 시 HP 피해 -1");
        ECSTASY = weapon("ecstasy", DamageColor.WHITE, 4.0, -1.6,
                "T-02-71 · WAW", "WHITE 피해 단검, 매우 빠름", "꿈꾸는 조류의 비늘을 벼려 만든 단검. 휘두를 때마다 무지개빛 물보라가 흩어진다.");
        ECSTASY_SUIT = suit("ecstasy_suit", ECSTASY_MAT,
                "T-02-71 · WAW", "WHITE 저항: 작업 실패 시 SP 피해 -1");
        HEAVEN = weapon("heaven", DamageColor.RED, 5.0, -2.0,
                "O-04-72 · WAW", "RED 피해 창, 빠른 찌르기", "핏발 선 눈이 박힌 창백한 가지를 깎아 만든 창. 찌를 때마다 뿌리 같은 가시가 바닥을 긁는다.");
        HEAVEN_SUIT = suit("heaven_suit", HEAVEN_MAT,
                "O-04-72 · WAW", "WHITE·BLACK 저항, RED 취약: 작업 실패 시 SP·HP 피해 -1");
        FEATHER_OF_HONOR = weapon("feather_of_honor", DamageColor.WHITE, 3.0, -1.0,
                "O-02-101 · WAW", "WHITE 피해 깃털, 매우 빠른 연타", "불새의 깃털을 그대로 벼려 만든 무기. 휘두를 때마다 타다 남은 불티가 흩날린다.");
        FEATHER_OF_HONOR_SUIT = suit("feather_of_honor_suit", FEATHER_OF_HONOR_MAT,
                "O-02-101 · WAW", "RED·WHITE 저항: 작업 실패 시 HP·SP 피해 -1");
        AMITA = weapon("amita", DamageColor.RED, 4.0, -1.4,
                "D-01-110 · WAW", "RED 피해, 빠른 연타", "구름 낀 수도승의 긴 팔을 닮은 붉은 무기. 휘두를 때마다 뿌연 구름이 발밑에 고인다.");
        AMITA_SUIT = suit("amita_suit", AMITA_MAT,
                "D-01-110 · WAW", "RED·BLACK 저항, WHITE 취약: 작업 실패 시 HP 피해 -1");
        MOONLIGHT = weapon("moonlight", DamageColor.WHITE, 4.5, -1.6,
                "D-01-105 · WAW", "WHITE 피해, 빠른 연타", "달의 울음의 건반을 벼려 만든 창백한 무기. 휘두를 때마다 젖은 화음이 흘러나온다.");
        MOONLIGHT_SUIT = suit("moonlight_suit", MOONLIGHT_MAT,
                "D-01-105 · WAW", "WHITE·BLACK 저항: 작업 실패 시 SP·HP 피해 -1");
        DIFFRACTION = weapon("diffraction", DamageColor.WHITE, 6.0, -2.4,
                "O-03-88 · WAW", "WHITE 피해, 보통 속도", "굴절된 공간의 파편을 그대로 벼려 만든 무기. 휘두를 때마다 허공이 잠깐 어긋난다.");
        DIFFRACTION_SUIT = suit("diffraction_suit", DIFFRACTION_MAT,
                "O-03-88 · WAW", "WHITE 저항: 작업 실패 시 SP 피해 -1");
    }

    public static List<Item> all() {
        return List.of(PENITENCE, PENITENCE_SUIT, REGRET, REGRET_SUIT, MATCH_FLAME, MATCH_FLAME_SUIT,
                BEAR_PAWS, BEAR_PAWS_SUIT, WINGBEAT, WINGBEAT_SUIT, BEAK, BEAK_SUIT,
                TODAYS_EXPRESSION, TODAYS_EXPRESSION_SUIT, SCREAMING_WEDGE, SCREAMING_WEDGE_SUIT,
                DAREDEVIL, DAREDEVIL_SUIT, SOLITUDE, SOLITUDE_SUIT, RED_EYES, RED_EYES_SUIT,
                ENGULFING_DREAM, ENGULFING_DREAM_SUIT, HORN, HORN_SUIT, LANTERN, LANTERN_SUIT,
                FRAGMENTS, FRAGMENTS_SUIT, NOISE, NOISE_SUIT, SANGUINE_DESIRE, SANGUINE_DESIRE_SUIT,
                HARMONY, HARMONY_SUIT, PLEASURE, PLEASURE_SUIT, LAETITIA, LAETITIA_SUIT,
                GRINDER, GRINDER_SUIT, OUR_GALAXY, OUR_GALAXY_SUIT, MAGIC_BULLET, MAGIC_BULLET_SUIT,
                SYRINX, SYRINX_SUIT, FROST_SPLINTER, FROST_SPLINTER_SUIT, LOGGING, LOGGING_SUIT,
                HARVEST, HARVEST_SUIT, CHRISTMAS, CHRISTMAS_SUIT, SOLEMN_LAMENT, SOLEMN_LAMENT_SUIT,
                GAZE, GAZE_SUIT, LAMP, LAMP_SUIT, JUSTITIA, JUSTITIA_SUIT,
                LOVE_AND_HATE, LOVE_AND_HATE_SUIT, TEARS_SWORD, TEARS_SWORD_SUIT,
                GOLD_RUSH, GOLD_RUSH_SUIT, CRIMSON_SCAR, CRIMSON_SCAR_SUIT,
                COBALT_SCAR, COBALT_SCAR_SUIT, FAINT_AROMA, FAINT_AROMA_SUIT,
                HORNET, HORNET_SUIT, BLACK_SWAN, BLACK_SWAN_SUIT,
                DISCORD, DISCORD_SUIT, GREEN_STEM, GREEN_STEM_SUIT, SPORE, SPORE_SUIT, HYPOCRISY, HYPOCRISY_SUIT, EXUVIAE, EXUVIAE_SUIT, ECSTASY, ECSTASY_SUIT, HEAVEN, HEAVEN_SUIT, FEATHER_OF_HONOR, FEATHER_OF_HONOR_SUIT, AMITA, AMITA_SUIT, MOONLIGHT, MOONLIGHT_SUIT, DIFFRACTION, DIFFRACTION_SUIT);
    }
}
