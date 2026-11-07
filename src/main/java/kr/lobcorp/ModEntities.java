package kr.lobcorp;

import kr.lobcorp.entity.AllAroundHelperEntity;
import kr.lobcorp.entity.BeautyBeastEntity;
import kr.lobcorp.entity.CrumblingArmorEntity;
import kr.lobcorp.entity.FairyFestivalEntity;
import kr.lobcorp.entity.ForsakenMurdererEntity;
import kr.lobcorp.entity.FragmentEntity;
import kr.lobcorp.entity.HappyTeddyBearEntity;
import kr.lobcorp.entity.LadyFacingTheWallEntity;
import kr.lobcorp.entity.LaetitiaEntity;
import kr.lobcorp.entity.LaetitiaFriendEntity;
import kr.lobcorp.entity.MeatLanternEntity;
import kr.lobcorp.entity.MhzEntity;
import kr.lobcorp.entity.ScorchedGirlEntity;
import kr.lobcorp.entity.SingingMachineEntity;
import kr.lobcorp.entity.SpiderBudEntity;
import kr.lobcorp.entity.OldLadyEntity;
import kr.lobcorp.entity.OneSinEntity;
import kr.lobcorp.entity.PorccubusEntity;
import kr.lobcorp.entity.PunishingBirdEntity;
import kr.lobcorp.entity.RedShoesEntity;
import kr.lobcorp.entity.TodaysShyLookEntity;
import kr.lobcorp.entity.TreeSapEntity;
import kr.lobcorp.entity.VoidDreamEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    private ModEntities() {}

    public static EntityType<OneSinEntity> ONE_SIN;
    public static EntityType<ForsakenMurdererEntity> FORSAKEN_MURDERER;
    public static EntityType<ScorchedGirlEntity> SCORCHED_GIRL;
    public static EntityType<HappyTeddyBearEntity> HAPPY_TEDDY;
    public static EntityType<FairyFestivalEntity> FAIRY_FESTIVAL;
    public static EntityType<PunishingBirdEntity> PUNISHING_BIRD;
    public static EntityType<TodaysShyLookEntity> TODAYS_SHY_LOOK;
    public static EntityType<LadyFacingTheWallEntity> LADY_FACING_THE_WALL;
    public static EntityType<CrumblingArmorEntity> CRUMBLING_ARMOR;
    public static EntityType<OldLadyEntity> OLD_LADY;
    public static EntityType<SpiderBudEntity> SPIDER_BUD;
    public static EntityType<VoidDreamEntity> VOID_DREAM;
    public static EntityType<BeautyBeastEntity> BEAUTY_BEAST;
    public static EntityType<MeatLanternEntity> MEAT_LANTERN;
    public static EntityType<FragmentEntity> FRAGMENT;
    public static EntityType<MhzEntity> MHZ;
    public static EntityType<RedShoesEntity> RED_SHOES;
    public static EntityType<SingingMachineEntity> SINGING_MACHINE;
    public static EntityType<PorccubusEntity> PORCCUBUS;
    public static EntityType<LaetitiaEntity> LAETITIA;
    public static EntityType<LaetitiaFriendEntity> LAETITIA_FRIEND;
    public static EntityType<AllAroundHelperEntity> ALL_AROUND_HELPER;
    public static EntityType<TreeSapEntity> TREE_SAP;

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, LobCorp.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void init() {
        ONE_SIN = register("one_sin", EntityType.Builder.of(OneSinEntity::new, MobCategory.MISC)
                .sized(1.0f, 2.6f).clientTrackingRange(10));
        FORSAKEN_MURDERER = register("forsaken_murderer", EntityType.Builder.of(ForsakenMurdererEntity::new, MobCategory.MISC)
                .sized(0.8f, 1.9f).clientTrackingRange(10));

        SCORCHED_GIRL = register("scorched_girl", EntityType.Builder.of(ScorchedGirlEntity::new, MobCategory.MISC)
                .sized(0.6f, 1.4f).clientTrackingRange(10).fireImmune());
        HAPPY_TEDDY = register("happy_teddy_bear", EntityType.Builder.of(HappyTeddyBearEntity::new, MobCategory.MISC)
                .sized(1.0f, 1.6f).clientTrackingRange(10));
        FAIRY_FESTIVAL = register("fairy_festival", EntityType.Builder.of(FairyFestivalEntity::new, MobCategory.MISC)
                .sized(1.2f, 2.2f).clientTrackingRange(10));
        // 격리 중에는 횃대 위에 앉아 있으므로 기본 크기가 크고, 탈출 시 엔티티가 스스로 작은 판정으로 바꾼다
        PUNISHING_BIRD = register("punishing_bird", EntityType.Builder.of(PunishingBirdEntity::new, MobCategory.MISC)
                .sized(0.6f, 1.2f).clientTrackingRange(10));
        TODAYS_SHY_LOOK = register("todays_shy_look", EntityType.Builder.of(TodaysShyLookEntity::new, MobCategory.MISC)
                .sized(1.4f, 2.0f).clientTrackingRange(10));
        LADY_FACING_THE_WALL = register("lady_facing_the_wall", EntityType.Builder.of(LadyFacingTheWallEntity::new, MobCategory.MISC)
                .sized(0.8f, 1.9f).clientTrackingRange(10));
        CRUMBLING_ARMOR = register("crumbling_armor", EntityType.Builder.of(CrumblingArmorEntity::new, MobCategory.MISC)
                .sized(1.0f, 1.9f).clientTrackingRange(10));
        OLD_LADY = register("old_lady", EntityType.Builder.of(OldLadyEntity::new, MobCategory.MISC)
                .sized(0.9f, 1.6f).clientTrackingRange(10));
        SPIDER_BUD = register("spider_bud", EntityType.Builder.of(SpiderBudEntity::new, MobCategory.MISC)
                .sized(1.2f, 0.9f).clientTrackingRange(10));
        VOID_DREAM = register("void_dream", EntityType.Builder.of(VoidDreamEntity::new, MobCategory.MISC)
                .sized(0.9f, 0.9f).clientTrackingRange(10));
        BEAUTY_BEAST = register("beauty_and_the_beast", EntityType.Builder.of(BeautyBeastEntity::new, MobCategory.MISC)
                .sized(1.0f, 1.6f).clientTrackingRange(10));
        MEAT_LANTERN = register("meat_lantern", EntityType.Builder.of(MeatLanternEntity::new, MobCategory.MISC)
                .sized(1.6f, 1.4f).clientTrackingRange(10));
        FRAGMENT = register("fragment_of_universe", EntityType.Builder.of(FragmentEntity::new, MobCategory.MISC)
                .sized(1.0f, 1.0f).clientTrackingRange(10));
        MHZ = register("mhz", EntityType.Builder.of(MhzEntity::new, MobCategory.MISC)
                .sized(0.9f, 1.0f).clientTrackingRange(10));
        RED_SHOES = register("red_shoes", EntityType.Builder.of(RedShoesEntity::new, MobCategory.MISC)
                .sized(0.6f, 1.0f).clientTrackingRange(10));
        SINGING_MACHINE = register("singing_machine", EntityType.Builder.of(SingingMachineEntity::new, MobCategory.MISC)
                .sized(1.2f, 1.4f).clientTrackingRange(10));
        PORCCUBUS = register("porccubus", EntityType.Builder.of(PorccubusEntity::new, MobCategory.MISC)
                .sized(1.0f, 2.0f).clientTrackingRange(10));
        LAETITIA = register("laetitia", EntityType.Builder.of(LaetitiaEntity::new, MobCategory.MISC)
                .sized(0.6f, 1.4f).clientTrackingRange(10));
        LAETITIA_FRIEND = register("laetitia_friend", EntityType.Builder.of(LaetitiaFriendEntity::new, MobCategory.MONSTER)
                .sized(0.9f, 1.2f).clientTrackingRange(10));
        ALL_AROUND_HELPER = register("all_around_helper", EntityType.Builder.of(AllAroundHelperEntity::new, MobCategory.MISC)
                .sized(0.9f, 1.2f).clientTrackingRange(10));
        TREE_SAP = register("tree_sap", EntityType.Builder.of(TreeSapEntity::new, MobCategory.MISC)
                .sized(0.8f, 1.3f).clientTrackingRange(10));

        FabricDefaultAttributeRegistry.register(ONE_SIN, OneSinEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCORCHED_GIRL, ScorchedGirlEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(HAPPY_TEDDY, HappyTeddyBearEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(FAIRY_FESTIVAL, FairyFestivalEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(FORSAKEN_MURDERER, ForsakenMurdererEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(PUNISHING_BIRD, PunishingBirdEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(TODAYS_SHY_LOOK, TodaysShyLookEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(LADY_FACING_THE_WALL, LadyFacingTheWallEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(CRUMBLING_ARMOR, CrumblingArmorEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(OLD_LADY, OldLadyEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SPIDER_BUD, SpiderBudEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(VOID_DREAM, VoidDreamEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(BEAUTY_BEAST, BeautyBeastEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MEAT_LANTERN, MeatLanternEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(FRAGMENT, FragmentEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MHZ, MhzEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(RED_SHOES, RedShoesEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SINGING_MACHINE, SingingMachineEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(PORCCUBUS, PorccubusEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(LAETITIA, LaetitiaEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(LAETITIA_FRIEND, LaetitiaFriendEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ALL_AROUND_HELPER, AllAroundHelperEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(TREE_SAP, TreeSapEntity.createAttributes());
    }
}
