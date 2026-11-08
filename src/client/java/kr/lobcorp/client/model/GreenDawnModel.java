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

/** 녹색 여명. 가느다란 로봇, 한쪽 팔은 칼날이다. */
public class GreenDawnModel extends EntityModel<GreenDawnModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("green_dawn"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg;

    public GreenDawnModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 몸통
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3, 0, -2, 6, 8, 4), PartPose.offset(0, 8, 0));
        // 각진 머리 + 바이저
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-3, -6, -3, 6, 6, 6)
                .texOffs(24, 12).addBox(-2, -4, -4, 4, 1, 1), PartPose.offset(0, 8, 0));
        // 팔: 오른팔은 칼날로 끝난다
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-1, 0, -1, 2, 8, 2)
                .texOffs(12, 24).addBox(-1, 8, -1, 2, 4, 1), PartPose.offset(-4, 9, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(4, 9, 0));
        // 다리
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(20, 24)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-1.5f, 16, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(20, 24)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(1.5f, 16, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 걷기 스윙
        float swing = Mth.cos(s.walkAnimationPos * 0.6662f) * 0.8f * s.walkAnimationSpeed;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightArm.xRot = -swing * 0.6f;
        leftArm.xRot = swing * 0.6f;
        // 머리는 좌우로 살짝 스캔한다
        head.yRot = Mth.sin(t * 0.06f) * 0.3f;
        body.xRot = 0.05f;
    }
}
