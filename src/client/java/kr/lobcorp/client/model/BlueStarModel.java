package kr.lobcorp.client.model;

import kr.lobcorp.LobCorp;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** 푸른 별. 떠다니는 심장/별 핵 + 네 개의 가시. */
public class BlueStarModel extends EntityModel<BlueStarModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("blue_star"), "main");

    public static class State extends LivingEntityRenderState {
    }

    private final ModelPart core, spikeN, spikeS, spikeE, spikeW;

    public BlueStarModel(ModelPart root) {
        super(root);
        core = root.getChild("core");
        spikeN = core.getChild("spike_n");
        spikeS = core.getChild("spike_s");
        spikeE = core.getChild("spike_e");
        spikeW = core.getChild("spike_w");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 핵 (u0 v0, 12x12x12)
        PartDefinition core = root.addOrReplaceChild("core", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6, -6, -6, 12, 12, 12),
                PartPose.offset(0, 8, 0));

        // 가시 네 개 (u0 v24 / u8 v24 / u16 v24 / u24 v24, 4x4x4)
        core.addOrReplaceChild("spike_n", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-2, -2, -2, 4, 4, 4),
                PartPose.offset(0, -8, 0));
        core.addOrReplaceChild("spike_s", CubeListBuilder.create()
                .texOffs(8, 24).addBox(-2, -2, -2, 4, 4, 4),
                PartPose.offset(0, 8, 0));
        core.addOrReplaceChild("spike_e", CubeListBuilder.create()
                .texOffs(16, 24).addBox(-2, -2, -2, 4, 4, 4),
                PartPose.offset(8, 0, 0));
        core.addOrReplaceChild("spike_w", CubeListBuilder.create()
                .texOffs(24, 24).addBox(-2, -2, -2, 4, 4, 4),
                PartPose.offset(-8, 0, 0));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        core.yRot = t * 0.05f;
        core.xRot = Mth.sin(t * 0.03f) * 0.15f;
        core.y = 8 + Mth.sin(t * 0.08f) * 1.5f;
        float pulse = 1f + Mth.sin(t * 0.12f) * 0.06f;
        core.xScale = pulse;
        core.yScale = pulse;
        core.zScale = pulse;
        spikeN.xRot = Mth.sin(t * 0.1f) * 0.2f;
        spikeS.xRot = -Mth.sin(t * 0.1f) * 0.2f;
        spikeE.zRot = Mth.sin(t * 0.09f) * 0.2f;
        spikeW.zRot = -Mth.sin(t * 0.09f) * 0.2f;
    }
}
