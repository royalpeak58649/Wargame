package kr.lobcorp.client.render;

import kr.lobcorp.LobCorp;
import kr.lobcorp.client.model.BeautyBeastModel;
import kr.lobcorp.client.model.CrumblingArmorModel;
import kr.lobcorp.client.model.FairyFestivalModel;
import kr.lobcorp.client.model.FragmentModel;
import kr.lobcorp.client.model.HappyTeddyBearModel;
import kr.lobcorp.client.model.LadyFacingTheWallModel;
import kr.lobcorp.client.model.MeatLanternModel;
import kr.lobcorp.client.model.OldLadyModel;
import kr.lobcorp.client.model.PunishingBirdModel;
import kr.lobcorp.client.model.ScorchedGirlModel;
import kr.lobcorp.client.model.SpiderBudModel;
import kr.lobcorp.client.model.TodaysShyLookModel;
import kr.lobcorp.client.model.VoidDreamModel;
import kr.lobcorp.entity.BeautyBeastEntity;
import kr.lobcorp.entity.CrumblingArmorEntity;
import kr.lobcorp.entity.FairyFestivalEntity;
import kr.lobcorp.entity.FragmentEntity;
import kr.lobcorp.entity.HappyTeddyBearEntity;
import kr.lobcorp.entity.LadyFacingTheWallEntity;
import kr.lobcorp.entity.MeatLanternEntity;
import kr.lobcorp.entity.OldLadyEntity;
import kr.lobcorp.entity.PunishingBirdEntity;
import kr.lobcorp.entity.ScorchedGirlEntity;
import kr.lobcorp.entity.SpiderBudEntity;
import kr.lobcorp.entity.TodaysShyLookEntity;
import kr.lobcorp.entity.VoidDreamEntity;
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
