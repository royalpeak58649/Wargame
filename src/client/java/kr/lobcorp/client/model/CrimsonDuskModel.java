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
import net.minecraft.util.Mth;

/** 진홍색 황혼. 거대한 광대, 둥근 배와 뾰족한 모자. */
public class CrimsonDuskModel extends EntityModel<CrimsonDuskModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("crimson_dusk"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart head, hat, body, rightArm, leftArm, rightLeg, leftLeg;

    public CrimsonDuskModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        hat = root.getChild("hat");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 둥근 배 몸통 (u0 v0, 20x20x16)
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-10, -20, -8, 20, 20, 16), PartPose.offset(0, 8, 0));
        // 큰 머리 (u0 v40, 16x16x16) + 빨간 코 (u0 v76, 4x4x4)
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-8, -16, -8, 16, 16, 16)
                .texOffs(0, 76).addBox(-2, -8, -10, 4, 4, 4), PartPose.offset(0, -12, 0));
        // 뾰족한 모자 (u64 v0, 14x10x14) + 꼭대기 (u64 v28, 6x8x6)
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-7, -10, -7, 14, 10, 14)
                .texOffs(64, 28).addBox(-3, -18, -3, 6, 8, 6), PartPose.offset(0, -12, 0));
        // 긴 팔 (u64 v44, 6x20x6)
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(64, 44)
                .addBox(-3, 0, -3, 6, 20, 6), PartPose.offset(-11, -10, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(64, 44)
                .addBox(-3, 0, -3, 6, 20, 6), PartPose.offset(11, -10, 0));
        // 다리 (u64 v72, 6x12x6)
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(64, 72)
                .addBox(-3, 0, -3, 6, 12, 6), PartPose.offset(-5, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(64, 72)
                .addBox(-3, 0, -3, 6, 12, 6), PartPose.offset(5, 12, 0));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 걷기 스윙
        float swing = Mth.cos(s.walkAnimationPos * 0.6662f) * 0.7f * s.walkAnimationSpeed;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightArm.xRot = -swing * 0.6f;
        leftArm.xRot = swing * 0.6f;
        // 광대처럼 흔들리는 몸과 고개
        body.zRot = Mth.sin(t * 0.06f) * 0.06f;
        head.xRot = Mth.sin(t * 0.08f) * 0.1f;
        head.yRot = Mth.sin(t * 0.05f) * 0.25f;
        hat.zRot = Mth.sin(t * 0.07f) * 0.12f;
    }
}
