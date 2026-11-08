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

/** 사무직. 평범한 사무복 차림의 인간형. */
public class ClerkModel extends EntityModel<ClerkModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("clerk"), "main");

    public static class State extends LivingEntityRenderState {
        public boolean panic;
        public int skin;
    }

    private final ModelPart body, head, armL, armR, legL, legR;

    public ClerkModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = root.getChild("head");
        armL = root.getChild("arm_l");
        armR = root.getChild("arm_r");
        legL = root.getChild("leg_l");
        legR = root.getChild("leg_r");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 머리 (u0 v0, 8x8x8)
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8),
                PartPose.offset(0, 0, 0));

        // 몸통 (u16 v16, 8x12x4)
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4),
                PartPose.offset(0, 0, 0));

        // 오른팔 (u40 v16, 4x12x4)
        root.addOrReplaceChild("arm_r", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-2, 0, -2, 4, 12, 4),
                PartPose.offset(-6, 0, 0));

        // 왼팔 (u32 v48, 4x12x4)
        root.addOrReplaceChild("arm_l", CubeListBuilder.create()
                .texOffs(32, 48).addBox(-2, 0, -2, 4, 12, 4),
                PartPose.offset(6, 0, 0));

        // 오른다리 (u0 v16, 4x12x4)
        root.addOrReplaceChild("leg_r", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4),
                PartPose.offset(-2, 12, 0));

        // 왼다리 (u16 v48, 4x12x4)
        root.addOrReplaceChild("leg_l", CubeListBuilder.create()
                .texOffs(16, 48).addBox(-2, 0, -2, 4, 12, 4),
                PartPose.offset(2, 12, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float swing = Mth.cos(s.walkAnimationPos * 0.6662f) * 0.7f * s.walkAnimationSpeed;
        legL.xRot = swing;
        legR.xRot = -swing;
        if (s.panic) {
            // 두 팔을 머리 위로 치켜들고 부들부들 떤다
            float shake = Mth.sin(t * 0.9f) * 0.12f;
            armL.xRot = -2.8f + shake;
            armR.xRot = -2.8f - shake;
            armL.zRot = 0.25f + shake * 0.5f;
            armR.zRot = -0.25f - shake * 0.5f;
            body.xRot = 0.05f;
            head.xRot = -0.1f + Mth.sin(t * 0.7f) * 0.08f;
            head.yRot = Mth.sin(t * 0.5f) * 0.25f;
        } else {
            armL.xRot = -swing * 0.6f;
            armR.xRot = swing * 0.6f;
            armL.zRot = 0.06f;
            armR.zRot = -0.06f;
            body.xRot = 0.02f + Mth.sin(t * 0.06f) * 0.02f;
            head.xRot = -0.02f + Mth.sin(t * 0.05f) * 0.04f;
            head.yRot = Mth.sin(t * 0.035f) * 0.15f;
        }
    }
}
