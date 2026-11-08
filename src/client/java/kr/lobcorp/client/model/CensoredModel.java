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

/** 검열됨. 온몸이 검은 검열 막대로 덮인 거구. */
public class CensoredModel extends EntityModel<CensoredModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("censored"), "main");

    public static class State extends LivingEntityRenderState {
    }

    private final ModelPart body, head, armL, armR, tentacleL, tentacleR, legL, legR;

    public CensoredModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        armL = body.getChild("arm_l");
        armR = body.getChild("arm_r");
        tentacleL = body.getChild("tentacle_l");
        tentacleR = body.getChild("tentacle_r");
        legL = root.getChild("leg_l");
        legR = root.getChild("leg_r");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 몸통 (u0 v0, 16x24x12)
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8, -24, -6, 16, 24, 12),
                PartPose.offset(0, 8, 0));

        // 머리 (u0 v36, 10x10x10)
        body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 36).addBox(-5, -10, -5, 10, 10, 10),
                PartPose.offset(0, -24, 0));

        // 굵은 팔 (u0 v56 / u16 v56, 6x20x6)
        body.addOrReplaceChild("arm_l", CubeListBuilder.create()
                .texOffs(0, 56).addBox(-3, 0, -3, 6, 20, 6),
                PartPose.offset(10, -22, 0));
        body.addOrReplaceChild("arm_r", CubeListBuilder.create()
                .texOffs(16, 56).addBox(-3, 0, -3, 6, 20, 6),
                PartPose.offset(-10, -22, 0));

        // 촉수 (u32 v56 / u40 v56, 4x16x4)
        body.addOrReplaceChild("tentacle_l", CubeListBuilder.create()
                .texOffs(32, 56).addBox(-2, 0, -2, 4, 16, 4),
                PartPose.offset(5, -2, 6));
        body.addOrReplaceChild("tentacle_r", CubeListBuilder.create()
                .texOffs(40, 56).addBox(-2, 0, -2, 4, 16, 4),
                PartPose.offset(-5, -2, 6));

        // 다리 (u0 v84 / u12 v84, 6x16x6)
        root.addOrReplaceChild("leg_l", CubeListBuilder.create()
                .texOffs(0, 84).addBox(-3, 0, -3, 6, 16, 6),
                PartPose.offset(4, 8, 0));
        root.addOrReplaceChild("leg_r", CubeListBuilder.create()
                .texOffs(12, 84).addBox(-3, 0, -3, 6, 16, 6),
                PartPose.offset(-4, 8, 0));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float swing = Mth.cos(s.walkAnimationPos * 0.6662f) * 0.5f * s.walkAnimationSpeed;
        legL.xRot = swing;
        legR.xRot = -swing;
        armL.xRot = -swing * 0.5f + Mth.sin(t * 0.05f) * 0.1f;
        armR.xRot = swing * 0.5f - Mth.sin(t * 0.05f) * 0.1f;
        armL.zRot = 0.15f;
        armR.zRot = -0.15f;
        tentacleL.xRot = 0.3f + Mth.sin(t * 0.08f) * 0.25f;
        tentacleR.xRot = 0.3f + Mth.cos(t * 0.08f) * 0.25f;
        tentacleL.zRot = Mth.sin(t * 0.06f) * 0.2f;
        tentacleR.zRot = -Mth.sin(t * 0.06f) * 0.2f;
        body.xRot = 0.08f + Mth.sin(t * 0.04f) * 0.05f;
        body.zRot = Mth.sin(t * 0.03f) * 0.04f;
        head.xRot = -0.1f + Mth.sin(t * 0.05f) * 0.06f;
        head.yRot = Mth.sin(t * 0.025f) * 0.25f;
    }
}
