package kr.lobcorp;

import kr.lobcorp.entity.AllAroundHelperEntity;
import kr.lobcorp.entity.AlriuneEntity;
import kr.lobcorp.entity.BeautyBeastEntity;
import kr.lobcorp.entity.BigBadWolfEntity;
import kr.lobcorp.entity.BigBirdEntity;
import kr.lobcorp.entity.BlackSwanEntity;
import kr.lobcorp.entity.ChildGalaxyEntity;
import kr.lobcorp.entity.CrumblingArmorEntity;
import kr.lobcorp.entity.FairyFestivalEntity;
import kr.lobcorp.entity.FetusEntity;
import kr.lobcorp.entity.ForsakenMurdererEntity;
import kr.lobcorp.entity.FragmentEntity;
import kr.lobcorp.entity.FreischutzEntity;
import kr.lobcorp.entity.FuneralEntity;
import kr.lobcorp.entity.HappyTeddyBearEntity;
import kr.lobcorp.entity.JudgementBirdEntity;
import kr.lobcorp.entity.KingGreedEntity;
import kr.lobcorp.entity.KnightDespairEntity;
import kr.lobcorp.entity.LadyFacingTheWallEntity;
import kr.lobcorp.entity.LaetitiaEntity;
import kr.lobcorp.entity.LaetitiaFriendEntity;
import kr.lobcorp.entity.LittleRedEntity;
import kr.lobcorp.entity.MeatLanternEntity;
import kr.lobcorp.entity.MhzEntity;
import kr.lobcorp.entity.NotesEntity;
import kr.lobcorp.entity.ScarecrowEntity;
import kr.lobcorp.entity.SchadenfreudeEntity;
import kr.lobcorp.entity.ScorchedGirlEntity;
import kr.lobcorp.entity.ShelterEntity;
import kr.lobcorp.entity.SingingMachineEntity;
import kr.lobcorp.entity.SnowQueenEntity;
import kr.lobcorp.entity.SnowWhiteAppleEntity;
import kr.lobcorp.entity.LittlePrinceEntity;
import kr.lobcorp.entity.LittlePrinceMinionEntity;
import kr.lobcorp.entity.ParasiteTreeEntity;
import kr.lobcorp.entity.SaplingEntity;
import kr.lobcorp.entity.NakedNestEntity;
import kr.lobcorp.entity.NakedWormEntity;
import kr.lobcorp.entity.DreamingCurrentEntity;
import kr.lobcorp.entity.BurrowingHeavenEntity;
import kr.lobcorp.entity.SpiderBudEntity;
import kr.lobcorp.entity.OldLadyEntity;
import kr.lobcorp.entity.OneSinEntity;
import kr.lobcorp.entity.PorccubusEntity;
import kr.lobcorp.entity.PortraitEntity;
import kr.lobcorp.entity.PunishingBirdEntity;
import kr.lobcorp.entity.QueenBeeEntity;
import kr.lobcorp.entity.QueenHatredEntity;
import kr.lobcorp.entity.RedShoesEntity;
import kr.lobcorp.entity.RudoltaEntity;
import kr.lobcorp.entity.TodaysShyLookEntity;
import kr.lobcorp.entity.TreeSapEntity;
import kr.lobcorp.entity.VoidDreamEntity;
import kr.lobcorp.entity.WoodsmanEntity;
import kr.lobcorp.entity.WorkerBeeEntity;
import kr.lobcorp.entity.YangEntity;
import kr.lobcorp.entity.YinEntity;
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
    public static EntityType<ShelterEntity> SHELTER;
    public static EntityType<PortraitEntity> PORTRAIT;
    public static EntityType<NotesEntity> NOTES;
    public static EntityType<ChildGalaxyEntity> CHILD_GALAXY;
    public static EntityType<FreischutzEntity> FREISCHUTZ;
    public static EntityType<FetusEntity> NAMELESS_FETUS;
    public static EntityType<SnowQueenEntity> SNOW_QUEEN;
    public static EntityType<WoodsmanEntity> WOODSMAN;
    public static EntityType<ScarecrowEntity> SCARECROW;
    public static EntityType<RudoltaEntity> RUDOLTA;
    public static EntityType<FuneralEntity> FUNERAL;
    public static EntityType<SchadenfreudeEntity> SCHADENFREUDE;
    public static EntityType<BigBirdEntity> BIG_BIRD;
    public static EntityType<JudgementBirdEntity> JUDGEMENT_BIRD;
    public static EntityType<QueenHatredEntity> QUEEN_HATRED;
    public static EntityType<KnightDespairEntity> KNIGHT_DESPAIR;
    public static EntityType<KingGreedEntity> KING_GREED;
    public static EntityType<LittleRedEntity> LITTLE_RED;
    public static EntityType<BigBadWolfEntity> BIG_BAD_WOLF;
    public static EntityType<AlriuneEntity> ALRIUNE;
    public static EntityType<QueenBeeEntity> QUEEN_BEE;
    public static EntityType<WorkerBeeEntity> WORKER_BEE;
    public static EntityType<BlackSwanEntity> BLACK_SWAN;
    public static EntityType<YinEntity> YIN;
    public static EntityType<YangEntity> YANG;
    public static EntityType<SnowWhiteAppleEntity> SNOW_WHITE_APPLE;
    public static EntityType<LittlePrinceEntity> LITTLE_PRINCE;
    public static EntityType<LittlePrinceMinionEntity> LITTLE_PRINCE_MINION;
    public static EntityType<ParasiteTreeEntity> PARASITE_TREE;
    public static EntityType<SaplingEntity> SAPLING;
    public static EntityType<NakedNestEntity> NAKED_NEST;
    public static EntityType<NakedWormEntity> NAKED_WORM;
    public static EntityType<DreamingCurrentEntity> DREAMING_CURRENT;
    public static EntityType<BurrowingHeavenEntity> BURROWING_HEAVEN;

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
        SHELTER = register("shelter", EntityType.Builder.of(ShelterEntity::new, MobCategory.MISC)
                .sized(1.2f, 0.6f).clientTrackingRange(10));
        PORTRAIT = register("portrait", EntityType.Builder.of(PortraitEntity::new, MobCategory.MISC)
                .sized(1.0f, 1.6f).clientTrackingRange(10));
        NOTES = register("notes", EntityType.Builder.of(NotesEntity::new, MobCategory.MISC)
                .sized(0.8f, 1.2f).clientTrackingRange(10));
        CHILD_GALAXY = register("child_galaxy", EntityType.Builder.of(ChildGalaxyEntity::new, MobCategory.MISC)
                .sized(0.6f, 1.2f).clientTrackingRange(10));
        FREISCHUTZ = register("freischutz", EntityType.Builder.of(FreischutzEntity::new, MobCategory.MISC)
                .sized(0.7f, 2.2f).clientTrackingRange(10));
        NAMELESS_FETUS = register("nameless_fetus", EntityType.Builder.of(FetusEntity::new, MobCategory.MISC)
                .sized(1.6f, 1.2f).clientTrackingRange(10));
        SNOW_QUEEN = register("snow_queen", EntityType.Builder.of(SnowQueenEntity::new, MobCategory.MISC)
                .sized(0.8f, 2.2f).clientTrackingRange(10));
        WOODSMAN = register("woodsman", EntityType.Builder.of(WoodsmanEntity::new, MobCategory.MISC)
                .sized(0.9f, 2.0f).clientTrackingRange(10));
        SCARECROW = register("scarecrow", EntityType.Builder.of(ScarecrowEntity::new, MobCategory.MISC)
                .sized(0.9f, 2.2f).clientTrackingRange(10));
        RUDOLTA = register("rudolta", EntityType.Builder.of(RudoltaEntity::new, MobCategory.MISC)
                .sized(1.4f, 1.8f).clientTrackingRange(10));
        FUNERAL = register("funeral", EntityType.Builder.of(FuneralEntity::new, MobCategory.MISC)
                .sized(0.8f, 2.0f).clientTrackingRange(10));
        SCHADENFREUDE = register("schadenfreude", EntityType.Builder.of(SchadenfreudeEntity::new, MobCategory.MISC)
                .sized(0.9f, 1.9f).clientTrackingRange(10));
        BIG_BIRD = register("big_bird", EntityType.Builder.of(BigBirdEntity::new, MobCategory.MISC)
                .sized(1.6f, 3.0f).clientTrackingRange(10));
        JUDGEMENT_BIRD = register("judgement_bird", EntityType.Builder.of(JudgementBirdEntity::new, MobCategory.MISC)
                .sized(0.8f, 3.2f).clientTrackingRange(10));
        QUEEN_HATRED = register("queen_of_hatred", EntityType.Builder.of(QueenHatredEntity::new, MobCategory.MISC)
                .sized(0.6f, 1.8f).clientTrackingRange(10));
        KNIGHT_DESPAIR = register("knight_of_despair", EntityType.Builder.of(KnightDespairEntity::new, MobCategory.MISC)
                .sized(0.7f, 2.4f).clientTrackingRange(10));
        KING_GREED = register("king_of_greed", EntityType.Builder.of(KingGreedEntity::new, MobCategory.MISC)
                .sized(0.6f, 1.8f).clientTrackingRange(10));
        LITTLE_RED = register("little_red", EntityType.Builder.of(LittleRedEntity::new, MobCategory.MISC)
                .sized(0.6f, 1.8f).clientTrackingRange(10));
        BIG_BAD_WOLF = register("big_bad_wolf", EntityType.Builder.of(BigBadWolfEntity::new, MobCategory.MISC)
                .sized(1.0f, 2.4f).clientTrackingRange(10));
        ALRIUNE = register("alriune", EntityType.Builder.of(AlriuneEntity::new, MobCategory.MISC)
                .sized(0.7f, 2.4f).clientTrackingRange(10));
        QUEEN_BEE = register("queen_bee", EntityType.Builder.of(QueenBeeEntity::new, MobCategory.MISC)
                .sized(1.6f, 1.6f).clientTrackingRange(10));
        WORKER_BEE = register("worker_bee", EntityType.Builder.of(WorkerBeeEntity::new, MobCategory.MONSTER)
                .sized(0.8f, 1.4f).clientTrackingRange(10));
        BLACK_SWAN = register("black_swan", EntityType.Builder.of(BlackSwanEntity::new, MobCategory.MISC)
                .sized(0.7f, 2.0f).clientTrackingRange(10));
        YIN = register("yin", EntityType.Builder.of(YinEntity::new, MobCategory.MISC)
                .sized(0.9f, 0.9f).clientTrackingRange(10));
        YANG = register("yang", EntityType.Builder.of(YangEntity::new, MobCategory.MISC)
                .sized(0.9f, 0.9f).clientTrackingRange(10));
        SNOW_WHITE_APPLE = register("snow_white_apple", EntityType.Builder.of(SnowWhiteAppleEntity::new, MobCategory.MISC)
                .sized(1.4f, 1.4f).clientTrackingRange(10));
        LITTLE_PRINCE = register("little_prince", EntityType.Builder.of(LittlePrinceEntity::new, MobCategory.MISC)
                .sized(1.2f, 1.8f).clientTrackingRange(10));
        LITTLE_PRINCE_MINION = register("little_prince_minion", EntityType.Builder.of(LittlePrinceMinionEntity::new, MobCategory.MONSTER)
                .sized(0.8f, 1.4f).clientTrackingRange(10));
        PARASITE_TREE = register("parasite_tree", EntityType.Builder.of(ParasiteTreeEntity::new, MobCategory.MISC)
                .sized(1.6f, 3.0f).clientTrackingRange(10));
        SAPLING = register("sapling", EntityType.Builder.of(SaplingEntity::new, MobCategory.MONSTER)
                .sized(0.8f, 1.2f).clientTrackingRange(10));
        NAKED_NEST = register("naked_nest", EntityType.Builder.of(NakedNestEntity::new, MobCategory.MISC)
                .sized(1.6f, 1.6f).clientTrackingRange(10));
        NAKED_WORM = register("naked_worm", EntityType.Builder.of(NakedWormEntity::new, MobCategory.MONSTER)
                .sized(0.6f, 0.6f).clientTrackingRange(10));
        DREAMING_CURRENT = register("dreaming_current", EntityType.Builder.of(DreamingCurrentEntity::new, MobCategory.MISC).sized(1.0f, 1.0f).clientTrackingRange(10));
        BURROWING_HEAVEN = register("burrowing_heaven", EntityType.Builder.of(BurrowingHeavenEntity::new, MobCategory.MISC)
                .sized(1.2f, 3.2f).clientTrackingRange(10));

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
        FabricDefaultAttributeRegistry.register(SHELTER, ShelterEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(PORTRAIT, PortraitEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(NOTES, NotesEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(CHILD_GALAXY, ChildGalaxyEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(FREISCHUTZ, FreischutzEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(NAMELESS_FETUS, FetusEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SNOW_QUEEN, SnowQueenEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WOODSMAN, WoodsmanEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCARECROW, ScarecrowEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(RUDOLTA, RudoltaEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(FUNERAL, FuneralEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCHADENFREUDE, SchadenfreudeEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(BIG_BIRD, BigBirdEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(JUDGEMENT_BIRD, JudgementBirdEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(QUEEN_HATRED, QueenHatredEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(KNIGHT_DESPAIR, KnightDespairEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(KING_GREED, KingGreedEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(LITTLE_RED, LittleRedEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(BIG_BAD_WOLF, BigBadWolfEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ALRIUNE, AlriuneEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(QUEEN_BEE, QueenBeeEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WORKER_BEE, WorkerBeeEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(BLACK_SWAN, BlackSwanEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(YIN, YinEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(YANG, YangEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SNOW_WHITE_APPLE, SnowWhiteAppleEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(LITTLE_PRINCE, LittlePrinceEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(LITTLE_PRINCE_MINION, LittlePrinceMinionEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(PARASITE_TREE, ParasiteTreeEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SAPLING, SaplingEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(NAKED_NEST, NakedNestEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(NAKED_WORM, NakedWormEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(DREAMING_CURRENT, DreamingCurrentEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(BURROWING_HEAVEN, BurrowingHeavenEntity.createAttributes());
    }
}
