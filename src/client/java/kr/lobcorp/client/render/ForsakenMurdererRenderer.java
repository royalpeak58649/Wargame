package kr.lobcorp.client.render;

import kr.lobcorp.LobCorp;
import kr.lobcorp.client.model.ForsakenMurdererModel;
import kr.lobcorp.client.model.MurdererRenderState;
import kr.lobcorp.entity.ForsakenMurdererEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class ForsakenMurdererRenderer extends MobRenderer<ForsakenMurdererEntity, MurdererRenderState, ForsakenMurdererModel> {
    private static final Identifier TEXTURE = LobCorp.id("textures/entity/forsaken_murderer.png");

    public ForsakenMurdererRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ForsakenMurdererModel(ctx.bakeLayer(ForsakenMurdererModel.LAYER)), 0.5f);
    }

    @Override
    public MurdererRenderState createRenderState() {
        return new MurdererRenderState();
    }

    @Override
    public void extractRenderState(ForsakenMurdererEntity entity, MurdererRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.breached = entity.isBreached();
        state.attackAnim = entity.attackAnimTicks > 0 ? 1f - (entity.attackAnimTicks - partialTick) / 10f : 0f;
    }

    @Override
    public Identifier getTextureLocation(MurdererRenderState state) {
        return TEXTURE;
    }
}
