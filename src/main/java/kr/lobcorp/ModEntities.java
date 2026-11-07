package kr.lobcorp;

import kr.lobcorp.entity.CrumblingArmorEntity;
import kr.lobcorp.entity.FairyFestivalEntity;
import kr.lobcorp.entity.ForsakenMurdererEntity;
import kr.lobcorp.entity.HappyTeddyBearEntity;
import kr.lobcorp.entity.LadyFacingTheWallEntity;
import kr.lobcorp.entity.ScorchedGirlEntity;
import kr.lobcorp.entity.SpiderBudEntity;
import kr.lobcorp.entity.OldLadyEntity;
import kr.lobcorp.entity.OneSinEntity;
import kr.lobcorp.entity.PunishingBirdEntity;
import kr.lobcorp.entity.TodaysShyLookEntity;
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
    }
}
