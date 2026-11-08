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

/** 녹아내리는 사랑. 분홍 슬라임 소녀 — 흘러내리는 치마 + 머리카락 + 방울. */
public class MeltingLoveModel extends EntityModel<MeltingLoveModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("melting_love"), "main");

    public static class State extends LivingEntityRenderState {
    }

    private final ModelPart base, torso, head, hair, armL, armR, dripL, dripR, dripF;

    public MeltingLoveModel(ModelPart root) {
        super(root);
        base = root.getChild("base");
        torso = base.getChild("torso");
        head = torso.getChild("head");
        hair = head.getChild("hair");
        armL = torso.getChild("arm_l");
        armR = torso.getChild("arm_r");
        dripL = base.getChild("drip_l");
        dripR = base.getChild("drip_r");
        dripF = base.getChild("drip_f");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 흘러내리는 치마 (u0 v0, 16x10x16)
        PartDefinition base = root.addOrReplaceChild("base", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8, -10, -8, 16, 10, 16),
                PartPose.offset(0, 24, 0));

        // 몸통 (u0 v32, 8x10x6)
        PartDefinition torso = base.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-4, -10, -3, 8, 10, 6),
                PartPose.offset(0, -10, 0));

        // 머리 (u0 v52, 8x8x8)
        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 52).addBox(-4, -8, -4, 8, 8, 8),
                PartPose.offset(0, -10, 0));

        // 머리카락 (u0 v72, 10x10x10)
        head.addOrReplaceChild("hair", CubeListBuilder.create()
                .texOffs(0, 72).addBox(-5, -9, -5, 10, 10, 10),
                PartPose.offset(0, 0, 0));

        // 팔 (u0 v96 / u8 v96, 3x10x3)
        torso.addOrReplaceChild("arm_l", CubeListBuilder.create()
                .texOffs(0, 96).addBox(-1.5f, 0, -1.5f, 3, 10, 3),
                PartPose.offset(5.5f, -9, 0));
        torso.addOrReplaceChild("arm_r", CubeListBuilder.create()
                .texOffs(8, 96).addBox(-1.5f, 0, -1.5f, 3, 10, 3),
                PartPose.offset(-5.5f, -9, 0));

        // 방울 (u0 v112 / u6 v112 / u12 v112, 3x6x3)
        base.addOrReplaceChild("drip_l", CubeListBuilder.create()
                .texOffs(0, 112).addBox(-1.5f, 0, -1.5f, 3, 6, 3),
                PartPose.offset(7, 0, 3));
        base.addOrReplaceChild("drip_r", CubeListBuilder.create()
                .texOffs(6, 112).addBox(-1.5f, 0, -1.5f, 3, 6, 3),
                PartPose.offset(-7, 0, -2));
        base.addOrReplaceChild("drip_f", CubeListBuilder.create()
                .texOffs(12, 112).addBox(-1.5f, 0, -1.5f, 3, 6, 3),
                PartPose.offset(1, 0, 7));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float wob = Mth.sin(t * 0.06f);
        base.xScale = 1f + wob * 0.04f;
        base.zScale = 1f - wob * 0.04f;
        base.yScale = 1f - wob * 0.03f;
        torso.xRot = 0.05f + Mth.sin(t * 0.05f) * 0.05f;
        torso.zRot = wob * 0.05f;
        head.xRot = -0.05f + Mth.sin(t * 0.04f) * 0.06f;
        head.yRot = Mth.sin(t * 0.03f) * 0.25f;
        hair.xRot = Mth.sin(t * 0.045f) * 0.05f;
        float swing = Mth.cos(s.walkAnimationPos * 0.6662f) * 0.5f * s.walkAnimationSpeed;
        armL.xRot = -swing * 0.6f + Mth.sin(t * 0.07f) * 0.1f;
        armR.xRot = swing * 0.6f - Mth.sin(t * 0.07f) * 0.1f;
        armL.zRot = 0.12f;
        armR.zRot = -0.12f;
        dripL.y = Mth.sin(t * 0.1f) * 0.6f;
        dripR.y = Mth.cos(t * 0.11f) * 0.6f;
        dripF.y = Mth.sin(t * 0.09f + 1f) * 0.6f;
    }
}
