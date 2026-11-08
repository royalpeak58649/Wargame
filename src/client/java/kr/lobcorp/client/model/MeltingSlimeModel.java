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

/** 점액 덩어리. 큰 개체는 머리 위에 하트 왕관이 보인다. */
public class MeltingSlimeModel extends EntityModel<MeltingSlimeModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("melting_slime"), "main");

    public static class State extends LivingEntityRenderState {
        public boolean big;
    }

    private final ModelPart blob, eyeL, eyeR, crown;

    public MeltingSlimeModel(ModelPart root) {
        super(root);
        blob = root.getChild("blob");
        eyeL = blob.getChild("eye_l");
        eyeR = blob.getChild("eye_r");
        crown = blob.getChild("crown");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 몸통 (u0 v0, 12x10x12)
        PartDefinition blob = root.addOrReplaceChild("blob", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6, -10, -6, 12, 10, 12),
                PartPose.offset(0, 24, 0));

        // 눈 (u0 v24 / u4 v24, 2x2x2)
        blob.addOrReplaceChild("eye_l", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-1, -1, -1, 2, 2, 2),
                PartPose.offset(2.5f, -6, -6));
        blob.addOrReplaceChild("eye_r", CubeListBuilder.create()
                .texOffs(4, 24).addBox(-1, -1, -1, 2, 2, 2),
                PartPose.offset(-2.5f, -6, -6));

        // 하트 왕관 (u0 v32, 6x4x4) — 큰 개체에서만
        blob.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-3, -4, -2, 6, 4, 4),
                PartPose.offset(0, -10, 0));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        crown.visible = s.big;
        // 찌그러졌다 튀어오르는 바운스
        float bounce = Mth.sin(t * 0.2f);
        blob.xScale = 1f + bounce * 0.08f;
        blob.zScale = 1f + bounce * 0.08f;
        blob.yScale = 1f - bounce * 0.1f;
        blob.y = 24 - Math.abs(bounce) * 1.5f;
        blob.yRot = Mth.sin(t * 0.03f) * 0.15f;
        eyeL.y = -6 + Mth.sin(t * 0.1f) * 0.2f;
        eyeR.y = -6 + Mth.cos(t * 0.1f) * 0.2f;
        crown.yRot = Mth.sin(t * 0.05f) * 0.2f;
    }
}
