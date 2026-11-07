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
    }

    public static List<Item> all() {
        return List.of(PENITENCE, PENITENCE_SUIT, REGRET, REGRET_SUIT, MATCH_FLAME, MATCH_FLAME_SUIT,
                BEAR_PAWS, BEAR_PAWS_SUIT, WINGBEAT, WINGBEAT_SUIT, BEAK, BEAK_SUIT,
                TODAYS_EXPRESSION, TODAYS_EXPRESSION_SUIT, SCREAMING_WEDGE, SCREAMING_WEDGE_SUIT,
                DAREDEVIL, DAREDEVIL_SUIT, SOLITUDE, SOLITUDE_SUIT, RED_EYES, RED_EYES_SUIT,
                ENGULFING_DREAM, ENGULFING_DREAM_SUIT, HORN, HORN_SUIT, LANTERN, LANTERN_SUIT,
                FRAGMENTS, FRAGMENTS_SUIT, NOISE, NOISE_SUIT, SANGUINE_DESIRE, SANGUINE_DESIRE_SUIT);
    }
}
