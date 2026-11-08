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

/** 검은 군인. 검은 제복에 가슴의 커다란 하트. */
public class ArmySoldierModel extends EntityModel<ArmySoldierModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("army_soldier"), "main");

    public static class State extends LivingEntityRenderState {
    }

    private final ModelPart body, head, hat, armL, armR, legL, legR;

    public ArmySoldierModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = root.getChild("head");
        hat = head.getChild("hat");
        armL = root.getChild("arm_l");
        armR = root.getChild("arm_r");
        legL = root.getChild("leg_l");
        legR = root.getChild("leg_r");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 몸통 (u0 v0, 8x12x4) — 가슴의 큰 하트
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -12, -2, 8, 12, 4),
                PartPose.offset(0, 12, 0));

        // 머리 (u0 v16, 8x8x8)
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4, -8, -4, 8, 8, 8),
                PartPose.offset(0, 0, 0));

        // 높은 모자 (u0 v32, 6x8x6)
        head.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-3, -8, -3, 6, 8, 6),
                PartPose.offset(0, -8, 0));

        // 팔 (u0 v48 / u8 v48, 3x12x3)
        root.addOrReplaceChild("arm_l", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-1.5f, 0, -1.5f, 3, 12, 3),
                PartPose.offset(5.5f, 12, 0));
        root.addOrReplaceChild("arm_r", CubeListBuilder.create()
                .texOffs(8, 48).addBox(-1.5f, 0, -1.5f, 3, 12, 3),
                PartPose.offset(-5.5f, 12, 0));

        // 다리 (u0 v64 / u8 v64, 3x12x3)
        root.addOrReplaceChild("leg_l", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-1.5f, 0, -1.5f, 3, 12, 3),
                PartPose.offset(2, 12, 0));
        root.addOrReplaceChild("leg_r", CubeListBuilder.create()
                .texOffs(8, 64).addBox(-1.5f, 0, -1.5f, 3, 12, 3),
                PartPose.offset(-2, 12, 0));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float swing = Mth.cos(s.walkAnimationPos * 0.6662f) * 0.8f * s.walkAnimationSpeed;
        legL.xRot = swing;
        legR.xRot = -swing;
        armL.xRot = -swing * 0.7f;
        armR.xRot = swing * 0.7f;
        armL.zRot = 0.08f;
        armR.zRot = -0.08f;
        body.xRot = 0.04f + Mth.sin(t * 0.07f) * 0.03f;
        head.xRot = -0.04f + Mth.sin(t * 0.06f) * 0.05f;
        head.yRot = Mth.sin(t * 0.04f) * 0.2f;
        hat.zRot = Mth.sin(t * 0.05f) * 0.04f;
    }
}
