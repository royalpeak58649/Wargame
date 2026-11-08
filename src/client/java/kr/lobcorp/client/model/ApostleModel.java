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

/** 사도. 두루마기를 입고 긴 창을 든 인간형. */
public class ApostleModel extends EntityModel<ApostleModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("apostle"), "main");

    public static class State extends LivingEntityRenderState {
    }

    private final ModelPart body, head, halo, armL, armR, legL, legR, spear;

    public ApostleModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = root.getChild("head");
        halo = head.getChild("halo");
        armL = root.getChild("arm_l");
        armR = root.getChild("arm_r");
        legL = root.getChild("leg_l");
        legR = root.getChild("leg_r");
        spear = armR.getChild("spear");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 두루마기 몸통 (u0 v0, 8x16x6)
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -16, -3, 8, 16, 6),
                PartPose.offset(0, 16, 0));

        // 머리 (u0 v24, 8x8x8)
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-4, -8, -4, 8, 8, 8),
                PartPose.offset(0, 0, 0));

        // 작은 고리 (u0 v40, 8x1x8)
        head.addOrReplaceChild("halo", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-4, -1, -4, 8, 1, 8),
                PartPose.offset(0, -10, 0));

        // 팔 (u0 v52 / u8 v52, 3x12x3)
        root.addOrReplaceChild("arm_l", CubeListBuilder.create()
                .texOffs(0, 52).addBox(-1.5f, 0, -1.5f, 3, 12, 3),
                PartPose.offset(5.5f, 12, 0));
        PartDefinition armR = root.addOrReplaceChild("arm_r", CubeListBuilder.create()
                .texOffs(8, 52).addBox(-1.5f, 0, -1.5f, 3, 12, 3),
                PartPose.offset(-5.5f, 12, 0));

        // 긴 창 (u0 v68, 1x24x1)
        armR.addOrReplaceChild("spear", CubeListBuilder.create()
                .texOffs(0, 68).addBox(-0.5f, -6, -0.5f, 1, 24, 1),
                PartPose.offset(0, 10, -1));

        // 다리 (u0 v96 / u8 v96, 3x12x3)
        root.addOrReplaceChild("leg_l", CubeListBuilder.create()
                .texOffs(0, 96).addBox(-1.5f, 0, -1.5f, 3, 12, 3),
                PartPose.offset(2, 12, 0));
        root.addOrReplaceChild("leg_r", CubeListBuilder.create()
                .texOffs(8, 96).addBox(-1.5f, 0, -1.5f, 3, 12, 3),
                PartPose.offset(-2, 12, 0));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float swing = Mth.cos(s.walkAnimationPos * 0.6662f) * 0.7f * s.walkAnimationSpeed;
        legL.xRot = swing;
        legR.xRot = -swing;
        armL.xRot = -swing * 0.6f;
        armR.xRot = swing * 0.4f - 0.15f;
        armL.zRot = 0.08f;
        armR.zRot = -0.08f;
        spear.xRot = -0.1f;
        body.xRot = 0.03f + Mth.sin(t * 0.06f) * 0.03f;
        head.xRot = -0.03f + Mth.sin(t * 0.05f) * 0.05f;
        head.yRot = Mth.sin(t * 0.035f) * 0.2f;
        halo.yRot = t * 0.03f;
    }
}
