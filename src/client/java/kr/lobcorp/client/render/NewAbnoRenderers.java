package kr.lobcorp.client.render;

import kr.lobcorp.LobCorp;
import kr.lobcorp.client.model.FairyFestivalModel;
import kr.lobcorp.client.model.HappyTeddyBearModel;
import kr.lobcorp.client.model.PunishingBirdModel;
import kr.lobcorp.client.model.ScorchedGirlModel;
import kr.lobcorp.entity.FairyFestivalEntity;
import kr.lobcorp.entity.HappyTeddyBearEntity;
import kr.lobcorp.entity.PunishingBirdEntity;
import kr.lobcorp.entity.ScorchedGirlEntity;
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
}
