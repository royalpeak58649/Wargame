package kr.lobcorp.client.render;

import kr.lobcorp.LobCorp;
import kr.lobcorp.client.model.OneSinModel;
import kr.lobcorp.entity.OneSinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

public class OneSinRenderer extends MobRenderer<OneSinEntity, LivingEntityRenderState, OneSinModel> {
    private static final Identifier TEXTURE = LobCorp.id("textures/entity/one_sin.png");

    public OneSinRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new OneSinModel(ctx.bakeLayer(OneSinModel.LAYER)), 0.0f);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public void extractRenderState(OneSinEntity entity, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.lightCoords = LightCoordsUtil.FULL_BRIGHT; // 은은하게 빛난다
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
