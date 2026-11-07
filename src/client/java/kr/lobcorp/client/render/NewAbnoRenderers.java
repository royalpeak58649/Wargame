package kr.lobcorp.client.render;

import kr.lobcorp.LobCorp;
import kr.lobcorp.client.model.AllAroundHelperModel;
import kr.lobcorp.client.model.BeautyBeastModel;
import kr.lobcorp.client.model.BigBirdModel;
import kr.lobcorp.client.model.ChildGalaxyModel;
import kr.lobcorp.client.model.CrumblingArmorModel;
import kr.lobcorp.client.model.FairyFestivalModel;
import kr.lobcorp.client.model.FetusModel;
import kr.lobcorp.client.model.FragmentModel;
import kr.lobcorp.client.model.FreischutzModel;
import kr.lobcorp.client.model.FuneralModel;
import kr.lobcorp.client.model.HappyTeddyBearModel;
import kr.lobcorp.client.model.LadyFacingTheWallModel;
import kr.lobcorp.client.model.LaetitiaFriendModel;
import kr.lobcorp.client.model.LaetitiaModel;
import kr.lobcorp.client.model.MeatLanternModel;
import kr.lobcorp.client.model.MhzModel;
import kr.lobcorp.client.model.NotesModel;
import kr.lobcorp.client.model.OldLadyModel;
import kr.lobcorp.client.model.PorccubusModel;
import kr.lobcorp.client.model.PortraitModel;
import kr.lobcorp.client.model.PunishingBirdModel;
import kr.lobcorp.client.model.RedShoesModel;
import kr.lobcorp.client.model.RudoltaModel;
import kr.lobcorp.client.model.ScarecrowModel;
import kr.lobcorp.client.model.SchadenfreudeModel;
import kr.lobcorp.client.model.ScorchedGirlModel;
import kr.lobcorp.client.model.ShelterModel;
import kr.lobcorp.client.model.SingingMachineModel;
import kr.lobcorp.client.model.SnowQueenModel;
import kr.lobcorp.client.model.SpiderBudModel;
import kr.lobcorp.client.model.TodaysShyLookModel;
import kr.lobcorp.client.model.TreeSapModel;
import kr.lobcorp.client.model.VoidDreamModel;
import kr.lobcorp.client.model.WoodsmanModel;
import kr.lobcorp.entity.AllAroundHelperEntity;
import kr.lobcorp.entity.BeautyBeastEntity;
import kr.lobcorp.entity.BigBirdEntity;
import kr.lobcorp.entity.ChildGalaxyEntity;
import kr.lobcorp.entity.CrumblingArmorEntity;
import kr.lobcorp.entity.FairyFestivalEntity;
import kr.lobcorp.entity.FetusEntity;
import kr.lobcorp.entity.FragmentEntity;
import kr.lobcorp.entity.FreischutzEntity;
import kr.lobcorp.entity.FuneralEntity;
import kr.lobcorp.entity.HappyTeddyBearEntity;
import kr.lobcorp.entity.LadyFacingTheWallEntity;
import kr.lobcorp.entity.LaetitiaEntity;
import kr.lobcorp.entity.LaetitiaFriendEntity;
import kr.lobcorp.entity.MeatLanternEntity;
import kr.lobcorp.entity.MhzEntity;
import kr.lobcorp.entity.NotesEntity;
import kr.lobcorp.entity.OldLadyEntity;
import kr.lobcorp.entity.PorccubusEntity;
import kr.lobcorp.entity.PortraitEntity;
import kr.lobcorp.entity.PunishingBirdEntity;
import kr.lobcorp.entity.RedShoesEntity;
import kr.lobcorp.entity.RudoltaEntity;
import kr.lobcorp.entity.ScarecrowEntity;
import kr.lobcorp.entity.SchadenfreudeEntity;
import kr.lobcorp.entity.ScorchedGirlEntity;
import kr.lobcorp.entity.ShelterEntity;
import kr.lobcorp.entity.SingingMachineEntity;
import kr.lobcorp.entity.SnowQueenEntity;
import kr.lobcorp.entity.SpiderBudEntity;
import kr.lobcorp.entity.TodaysShyLookEntity;
import kr.lobcorp.entity.TreeSapEntity;
import kr.lobcorp.entity.VoidDreamEntity;
import kr.lobcorp.entity.WoodsmanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

/** 불타버린 소녀 / 행복한 테디베어 / 요정의 축제 렌더러 */
public final class NewAbnoRenderers {
    private NewAbnoRenderers() {}

    public static class ScorchedGirl extends MobRenderer<ScorchedGirlEntity, ScorchedGirlModel.State, ScorchedGirlModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/scorched_girl.png");

        public ScorchedGirl(EntityRendererProvider.Context ctx) {
            super(ctx, new ScorchedGirlModel(ctx.bakeLayer(ScorchedGirlModel.LAYER)), 0.35f);
        }

        @Override
        public ScorchedGirlModel.State createRenderState() {
            return new ScorchedGirlModel.State();
        }

        @Override
        public void extractRenderState(ScorchedGirlEntity e, ScorchedGirlModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.breached = e.isBreached();
            s.fuse = e.fuseClient > 0 ? 1f - (e.fuseClient - pt) / 25f : 0f;
        }

        @Override
        public Identifier getTextureLocation(ScorchedGirlModel.State s) {
            return TEX;
        }
    }

    public static class Teddy extends MobRenderer<HappyTeddyBearEntity, HappyTeddyBearModel.State, HappyTeddyBearModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/happy_teddy_bear.png");

        public Teddy(EntityRendererProvider.Context ctx) {
            super(ctx, new HappyTeddyBearModel(ctx.bakeLayer(HappyTeddyBearModel.LAYER)), 0.5f);
        }

        @Override
        public HappyTeddyBearModel.State createRenderState() {
            return new HappyTeddyBearModel.State();
        }

        @Override
        public void extractRenderState(HappyTeddyBearEntity e, HappyTeddyBearModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.hug = e.hugTicks > 0 ? (e.hugTicks - pt) / 40f : 0f;
        }

        @Override
        public Identifier getTextureLocation(HappyTeddyBearModel.State s) {
            return TEX;
        }
    }

    public static class Fairy extends MobRenderer<FairyFestivalEntity, LivingEntityRenderState, FairyFestivalModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/fairy_festival.png");

        public Fairy(EntityRendererProvider.Context ctx) {
            super(ctx, new FairyFestivalModel(ctx.bakeLayer(FairyFestivalModel.LAYER)), 0f);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public void extractRenderState(FairyFestivalEntity e, LivingEntityRenderState s, float pt) {
            super.extractRenderState(e, s, pt);
            s.lightCoords = LightCoordsUtil.FULL_BRIGHT; // 요정들은 스스로 빛난다
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState s) {
            return TEX;
        }
    }

    public static class PunishingBird extends MobRenderer<PunishingBirdEntity, PunishingBirdModel.State, PunishingBirdModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/punishing_bird.png");

        public PunishingBird(EntityRendererProvider.Context ctx) {
            super(ctx, new PunishingBirdModel(ctx.bakeLayer(PunishingBirdModel.LAYER)), 0.3f);
        }

        @Override
        public PunishingBirdModel.State createRenderState() {
            return new PunishingBirdModel.State();
        }

        @Override
        public void extractRenderState(PunishingBirdEntity e, PunishingBirdModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.breached = e.isBreached();
            s.angry = e.isAngry();
            s.peck = e.peckAnimTicks > 0 ? (e.peckAnimTicks - pt) / 8f : 0f;
        }

        @Override
        public Identifier getTextureLocation(PunishingBirdModel.State s) {
            return TEX;
        }
    }

    public static class WallLady extends MobRenderer<LadyFacingTheWallEntity, LivingEntityRenderState, LadyFacingTheWallModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/lady_facing_the_wall.png");

        public WallLady(EntityRendererProvider.Context ctx) {
            super(ctx, new LadyFacingTheWallModel(ctx.bakeLayer(LadyFacingTheWallModel.LAYER)), 0.5f);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public void extractRenderState(LadyFacingTheWallEntity e, LivingEntityRenderState s, float pt) {
            super.extractRenderState(e, s, pt);
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState s) {
            return TEX;
        }
    }

    public static class Armor extends MobRenderer<CrumblingArmorEntity, LivingEntityRenderState, CrumblingArmorModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/crumbling_armor.png");

        public Armor(EntityRendererProvider.Context ctx) {
            super(ctx, new CrumblingArmorModel(ctx.bakeLayer(CrumblingArmorModel.LAYER)), 0.6f);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public void extractRenderState(CrumblingArmorEntity e, LivingEntityRenderState s, float pt) {
            super.extractRenderState(e, s, pt);
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState s) {
            return TEX;
        }
    }

    public static class OldLady extends MobRenderer<OldLadyEntity, OldLadyModel.State, OldLadyModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/old_lady.png");

        public OldLady(EntityRendererProvider.Context ctx) {
            super(ctx, new OldLadyModel(ctx.bakeLayer(OldLadyModel.LAYER)), 0.5f);
        }

        @Override
        public OldLadyModel.State createRenderState() {
            return new OldLadyModel.State();
        }

        @Override
        public void extractRenderState(OldLadyEntity e, OldLadyModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.solitude = e.isSolitude();
        }

        @Override
        public Identifier getTextureLocation(OldLadyModel.State s) {
            return TEX;
        }
    }

    public static class SpiderBud extends MobRenderer<SpiderBudEntity, SpiderBudModel.State, SpiderBudModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/spider_bud.png");

        public SpiderBud(EntityRendererProvider.Context ctx) {
            super(ctx, new SpiderBudModel(ctx.bakeLayer(SpiderBudModel.LAYER)), 0f);
        }

        @Override
        public SpiderBudModel.State createRenderState() {
            return new SpiderBudModel.State();
        }

        @Override
        public void extractRenderState(SpiderBudEntity e, SpiderBudModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.cocoons = e.getCocoons();
        }

        @Override
        public Identifier getTextureLocation(SpiderBudModel.State s) {
            return TEX;
        }
    }

    public static class VoidDream extends MobRenderer<VoidDreamEntity, VoidDreamModel.State, VoidDreamModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/void_dream.png");

        public VoidDream(EntityRendererProvider.Context ctx) {
            super(ctx, new VoidDreamModel(ctx.bakeLayer(VoidDreamModel.LAYER)), 0.3f);
        }

        @Override
        public VoidDreamModel.State createRenderState() {
            return new VoidDreamModel.State();
        }

        @Override
        public void extractRenderState(VoidDreamEntity e, VoidDreamModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.breached = e.isBreached();
            s.rooster = e.isRooster();
            if (s.breached) s.lightCoords = LightCoordsUtil.FULL_BRIGHT;
        }

        @Override
        public Identifier getTextureLocation(VoidDreamModel.State s) {
            return TEX;
        }
    }

    public static class BeautyBeast extends MobRenderer<BeautyBeastEntity, BeautyBeastModel.State, BeautyBeastModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/beauty_and_the_beast.png");

        public BeautyBeast(EntityRendererProvider.Context ctx) {
            super(ctx, new BeautyBeastModel(ctx.bakeLayer(BeautyBeastModel.LAYER)), 0.7f);
        }

        @Override
        public BeautyBeastModel.State createRenderState() {
            return new BeautyBeastModel.State();
        }

        @Override
        public void extractRenderState(BeautyBeastEntity e, BeautyBeastModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.wounded = e.isWounded();
            s.lightCoords = LightCoordsUtil.FULL_BRIGHT; // 초록 눈이 스스로 빛난다
        }

        @Override
        public Identifier getTextureLocation(BeautyBeastModel.State s) {
            return TEX;
        }
    }

    public static class MeatLantern extends MobRenderer<MeatLanternEntity, MeatLanternModel.State, MeatLanternModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/meat_lantern.png");

        public MeatLantern(EntityRendererProvider.Context ctx) {
            super(ctx, new MeatLanternModel(ctx.bakeLayer(MeatLanternModel.LAYER)), 0.8f);
        }

        @Override
        public MeatLanternModel.State createRenderState() {
            return new MeatLanternModel.State();
        }

        @Override
        public void extractRenderState(MeatLanternEntity e, MeatLanternModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.breached = e.isBreached();
            s.bite = e.getBiteTicks() > 0 ? (e.getBiteTicks() - pt) / 30f : 0f;
            s.lightCoords = LightCoordsUtil.FULL_BRIGHT; // 꽃이 스스로 빛난다
        }

        @Override
        public Identifier getTextureLocation(MeatLanternModel.State s) {
            return TEX;
        }
    }

    public static class Fragment extends MobRenderer<FragmentEntity, FragmentModel.State, FragmentModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/fragment_of_universe.png");

        public Fragment(EntityRendererProvider.Context ctx) {
            super(ctx, new FragmentModel(ctx.bakeLayer(FragmentModel.LAYER)), 0.5f);
        }

        @Override
        public FragmentModel.State createRenderState() {
            return new FragmentModel.State();
        }

        @Override
        public void extractRenderState(FragmentEntity e, FragmentModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.breached = e.isBreached();
            s.singing = e.getSingTicks() > 0;
            if (s.breached) s.lightCoords = LightCoordsUtil.FULL_BRIGHT; // 반점과 하트가 스스로 빛난다
        }

        @Override
        public Identifier getTextureLocation(FragmentModel.State s) {
            return TEX;
        }
    }

    public static class Mhz extends MobRenderer<MhzEntity, MhzModel.State, MhzModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/mhz.png");

        public Mhz(EntityRendererProvider.Context ctx) {
            super(ctx, new MhzModel(ctx.bakeLayer(MhzModel.LAYER)), 0.4f);
        }

        @Override
        public MhzModel.State createRenderState() {
            return new MhzModel.State();
        }

        @Override
        public void extractRenderState(MhzEntity e, MhzModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.distorting = e.getRadius() > 0;
            s.lightCoords = LightCoordsUtil.FULL_BRIGHT; // 스피커 틈으로 새는 빛
        }

        @Override
        public Identifier getTextureLocation(MhzModel.State s) {
            return TEX;
        }
    }

    public static class RedShoes extends MobRenderer<RedShoesEntity, LivingEntityRenderState, RedShoesModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/red_shoes.png");

        public RedShoes(EntityRendererProvider.Context ctx) {
            super(ctx, new RedShoesModel(ctx.bakeLayer(RedShoesModel.LAYER)), 0.3f);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public void extractRenderState(RedShoesEntity e, LivingEntityRenderState s, float pt) {
            super.extractRenderState(e, s, pt);
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState s) {
            return TEX;
        }
    }

    public static class SingingMachine extends MobRenderer<SingingMachineEntity, SingingMachineModel.State, SingingMachineModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/singing_machine.png");

        public SingingMachine(EntityRendererProvider.Context ctx) {
            super(ctx, new SingingMachineModel(ctx.bakeLayer(SingingMachineModel.LAYER)), 0.7f);
        }

        @Override
        public SingingMachineModel.State createRenderState() {
            return new SingingMachineModel.State();
        }

        @Override
        public void extractRenderState(SingingMachineEntity e, SingingMachineModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.open = e.getMusicTicks() > 0;
        }

        @Override
        public Identifier getTextureLocation(SingingMachineModel.State s) {
            return TEX;
        }
    }

    public static class Porccubus extends MobRenderer<PorccubusEntity, PorccubusModel.State, PorccubusModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/porccubus.png");

        public Porccubus(EntityRendererProvider.Context ctx) {
            super(ctx, new PorccubusModel(ctx.bakeLayer(PorccubusModel.LAYER)), 0.5f);
        }

        @Override
        public PorccubusModel.State createRenderState() {
            return new PorccubusModel.State();
        }

        @Override
        public void extractRenderState(PorccubusEntity e, PorccubusModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.breached = e.isBreached();
        }

        @Override
        public Identifier getTextureLocation(PorccubusModel.State s) {
            return TEX;
        }
    }

    public static class Laetitia extends MobRenderer<LaetitiaEntity, LivingEntityRenderState, LaetitiaModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/laetitia.png");

        public Laetitia(EntityRendererProvider.Context ctx) {
            super(ctx, new LaetitiaModel(ctx.bakeLayer(LaetitiaModel.LAYER)), 0.4f);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public void extractRenderState(LaetitiaEntity e, LivingEntityRenderState s, float pt) {
            super.extractRenderState(e, s, pt);
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState s) {
            return TEX;
        }
    }

    public static class LaetitiaFriend extends MobRenderer<LaetitiaFriendEntity, LivingEntityRenderState, LaetitiaFriendModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/laetitia_friend.png");

        public LaetitiaFriend(EntityRendererProvider.Context ctx) {
            super(ctx, new LaetitiaFriendModel(ctx.bakeLayer(LaetitiaFriendModel.LAYER)), 0.6f);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public void extractRenderState(LaetitiaFriendEntity e, LivingEntityRenderState s, float pt) {
            super.extractRenderState(e, s, pt);
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState s) {
            return TEX;
        }
    }

    public static class AllAroundHelper extends MobRenderer<AllAroundHelperEntity, AllAroundHelperModel.State, AllAroundHelperModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/all_around_helper.png");

        public AllAroundHelper(EntityRendererProvider.Context ctx) {
            super(ctx, new AllAroundHelperModel(ctx.bakeLayer(AllAroundHelperModel.LAYER)), 0.5f);
        }

        @Override
        public AllAroundHelperModel.State createRenderState() {
            return new AllAroundHelperModel.State();
        }

        @Override
        public void extractRenderState(AllAroundHelperEntity e, AllAroundHelperModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.phase = e.getPhase();
            s.breached = e.isBreached();
        }

        @Override
        public Identifier getTextureLocation(AllAroundHelperModel.State s) {
            return TEX;
        }
    }

    public static class TreeSap extends MobRenderer<TreeSapEntity, LivingEntityRenderState, TreeSapModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/tree_sap.png");

        public TreeSap(EntityRendererProvider.Context ctx) {
            super(ctx, new TreeSapModel(ctx.bakeLayer(TreeSapModel.LAYER)), 0.5f);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public void extractRenderState(TreeSapEntity e, LivingEntityRenderState s, float pt) {
            super.extractRenderState(e, s, pt);
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState s) {
            return TEX;
        }
    }

    public static class Shelter extends MobRenderer<ShelterEntity, LivingEntityRenderState, ShelterModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/shelter.png");

        public Shelter(EntityRendererProvider.Context ctx) {
            super(ctx, new ShelterModel(ctx.bakeLayer(ShelterModel.LAYER)), 0.0f);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public void extractRenderState(ShelterEntity e, LivingEntityRenderState s, float pt) {
            super.extractRenderState(e, s, pt);
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState s) {
            return TEX;
        }
    }

    public static class Portrait extends MobRenderer<PortraitEntity, LivingEntityRenderState, PortraitModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/portrait.png");

        public Portrait(EntityRendererProvider.Context ctx) {
            super(ctx, new PortraitModel(ctx.bakeLayer(PortraitModel.LAYER)), 0.4f);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public void extractRenderState(PortraitEntity e, LivingEntityRenderState s, float pt) {
            super.extractRenderState(e, s, pt);
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState s) {
            return TEX;
        }
    }

    public static class Notes extends MobRenderer<NotesEntity, LivingEntityRenderState, NotesModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/notes.png");

        public Notes(EntityRendererProvider.Context ctx) {
            super(ctx, new NotesModel(ctx.bakeLayer(NotesModel.LAYER)), 0.4f);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public void extractRenderState(NotesEntity e, LivingEntityRenderState s, float pt) {
            super.extractRenderState(e, s, pt);
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState s) {
            return TEX;
        }
    }

    public static class ChildGalaxy extends MobRenderer<ChildGalaxyEntity, ChildGalaxyModel.State, ChildGalaxyModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/child_galaxy.png");

        public ChildGalaxy(EntityRendererProvider.Context ctx) {
            super(ctx, new ChildGalaxyModel(ctx.bakeLayer(ChildGalaxyModel.LAYER)), 0.4f);
        }

        @Override
        public ChildGalaxyModel.State createRenderState() {
            return new ChildGalaxyModel.State();
        }

        @Override
        public void extractRenderState(ChildGalaxyEntity e, ChildGalaxyModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.counter = e.getQliphoth();
            s.lightCoords = LightCoordsUtil.FULL_BRIGHT; // 조약돌과 별이 스스로 빛난다
        }

        @Override
        public Identifier getTextureLocation(ChildGalaxyModel.State s) {
            return TEX;
        }
    }

    public static class Freischutz extends MobRenderer<FreischutzEntity, LivingEntityRenderState, FreischutzModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/freischutz.png");

        public Freischutz(EntityRendererProvider.Context ctx) {
            super(ctx, new FreischutzModel(ctx.bakeLayer(FreischutzModel.LAYER)), 0.5f);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public void extractRenderState(FreischutzEntity e, LivingEntityRenderState s, float pt) {
            super.extractRenderState(e, s, pt);
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState s) {
            return TEX;
        }
    }

    public static class Fetus extends MobRenderer<FetusEntity, FetusModel.State, FetusModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/nameless_fetus.png");

        public Fetus(EntityRendererProvider.Context ctx) {
            super(ctx, new FetusModel(ctx.bakeLayer(FetusModel.LAYER)), 0.8f);
        }

        @Override
        public FetusModel.State createRenderState() {
            return new FetusModel.State();
        }

        @Override
        public void extractRenderState(FetusEntity e, FetusModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.cry = e.getCry() > 0 ? 1f : 0f;
        }

        @Override
        public Identifier getTextureLocation(FetusModel.State s) {
            return TEX;
        }
    }

    public static class SnowQueen extends MobRenderer<SnowQueenEntity, LivingEntityRenderState, SnowQueenModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/snow_queen.png");

        public SnowQueen(EntityRendererProvider.Context ctx) {
            super(ctx, new SnowQueenModel(ctx.bakeLayer(SnowQueenModel.LAYER)), 0.7f);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public void extractRenderState(SnowQueenEntity e, LivingEntityRenderState s, float pt) {
            super.extractRenderState(e, s, pt);
            s.lightCoords = LightCoordsUtil.FULL_BRIGHT; // 얼음과 왕관이 스스로 빛난다
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState s) {
            return TEX;
        }
    }

    public static class Woodsman extends MobRenderer<WoodsmanEntity, WoodsmanModel.State, WoodsmanModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/woodsman.png");

        public Woodsman(EntityRendererProvider.Context ctx) {
            super(ctx, new WoodsmanModel(ctx.bakeLayer(WoodsmanModel.LAYER)), 0.6f);
        }

        @Override
        public WoodsmanModel.State createRenderState() {
            return new WoodsmanModel.State();
        }

        @Override
        public void extractRenderState(WoodsmanEntity e, WoodsmanModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.breached = e.isBreached();
            s.swing = e.swingTicks > 0 ? (e.swingTicks - pt) / 12f : 0f;
        }

        @Override
        public Identifier getTextureLocation(WoodsmanModel.State s) {
            return TEX;
        }
    }

    public static class Scarecrow extends MobRenderer<ScarecrowEntity, ScarecrowModel.State, ScarecrowModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/scarecrow.png");

        public Scarecrow(EntityRendererProvider.Context ctx) {
            super(ctx, new ScarecrowModel(ctx.bakeLayer(ScarecrowModel.LAYER)), 0.5f);
        }

        @Override
        public ScarecrowModel.State createRenderState() {
            return new ScarecrowModel.State();
        }

        @Override
        public void extractRenderState(ScarecrowEntity e, ScarecrowModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.breached = e.isBreached();
            s.swing = e.swingTicks > 0 ? (e.swingTicks - pt) / 12f : 0f;
        }

        @Override
        public Identifier getTextureLocation(ScarecrowModel.State s) {
            return TEX;
        }
    }

    public static class Rudolta extends MobRenderer<RudoltaEntity, RudoltaModel.State, RudoltaModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/rudolta.png");

        public Rudolta(EntityRendererProvider.Context ctx) {
            super(ctx, new RudoltaModel(ctx.bakeLayer(RudoltaModel.LAYER)), 0.7f);
        }

        @Override
        public RudoltaModel.State createRenderState() {
            return new RudoltaModel.State();
        }

        @Override
        public void extractRenderState(RudoltaEntity e, RudoltaModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.walk = s.walkAnimationPos;
            s.lightCoords = LightCoordsUtil.FULL_BRIGHT; // 붉은 코와 뿔의 전구가 스스로 빛난다
        }

        @Override
        public Identifier getTextureLocation(RudoltaModel.State s) {
            return TEX;
        }
    }

    public static class Funeral extends MobRenderer<FuneralEntity, FuneralModel.State, FuneralModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/funeral.png");

        public Funeral(EntityRendererProvider.Context ctx) {
            super(ctx, new FuneralModel(ctx.bakeLayer(FuneralModel.LAYER)), 0.5f);
        }

        @Override
        public FuneralModel.State createRenderState() {
            return new FuneralModel.State();
        }

        @Override
        public void extractRenderState(FuneralEntity e, FuneralModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.breached = e.isBreached();
        }

        @Override
        public Identifier getTextureLocation(FuneralModel.State s) {
            return TEX;
        }
    }

    public static class Schadenfreude extends MobRenderer<SchadenfreudeEntity, SchadenfreudeModel.State, SchadenfreudeModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/schadenfreude.png");

        public Schadenfreude(EntityRendererProvider.Context ctx) {
            super(ctx, new SchadenfreudeModel(ctx.bakeLayer(SchadenfreudeModel.LAYER)), 0.6f);
        }

        @Override
        public SchadenfreudeModel.State createRenderState() {
            return new SchadenfreudeModel.State();
        }

        @Override
        public void extractRenderState(SchadenfreudeEntity e, SchadenfreudeModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.watched = e.isWatched();
            s.breached = e.isBreached();
            s.spin = e.getSpinTicks() > 0 ? 1f : 0f;
        }

        @Override
        public Identifier getTextureLocation(SchadenfreudeModel.State s) {
            return TEX;
        }
    }

    public static class BigBird extends MobRenderer<BigBirdEntity, BigBirdModel.State, BigBirdModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/big_bird.png");

        public BigBird(EntityRendererProvider.Context ctx) {
            super(ctx, new BigBirdModel(ctx.bakeLayer(BigBirdModel.LAYER)), 1.0f);
        }

        @Override
        public BigBirdModel.State createRenderState() {
            return new BigBirdModel.State();
        }

        @Override
        public void extractRenderState(BigBirdEntity e, BigBirdModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.lamp = e.isBreached() ? 1f : 0f;
            s.lightCoords = LightCoordsUtil.FULL_BRIGHT; // 수십 개의 눈과 등불이 스스로 빛난다
        }

        @Override
        public Identifier getTextureLocation(BigBirdModel.State s) {
            return TEX;
        }
    }

    public static class ShyLook extends MobRenderer<TodaysShyLookEntity, TodaysShyLookModel.State, TodaysShyLookModel> {
        private static final Identifier TEX = LobCorp.id("textures/entity/todays_shy_look.png");

        public ShyLook(EntityRendererProvider.Context ctx) {
            super(ctx, new TodaysShyLookModel(ctx.bakeLayer(TodaysShyLookModel.LAYER)), 0.4f);
        }

        @Override
        public TodaysShyLookModel.State createRenderState() {
            return new TodaysShyLookModel.State();
        }

        @Override
        public void extractRenderState(TodaysShyLookEntity e, TodaysShyLookModel.State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.face = e.getFace();
        }

        @Override
        public Identifier getTextureLocation(TodaysShyLookModel.State s) {
            return TEX;
        }
    }
}
