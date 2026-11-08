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

/** 녹색 정오. 육중한 골렘, 한쪽 팔은 총열, 다른 쪽은 회전하는 원형 톱. */
public class GreenNoonModel extends EntityModel<GreenNoonModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("green_noon"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart head, body, rightArm, leftArm, saw, rightLeg, leftLeg;

    public GreenNoonModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        saw = leftArm.getChild("saw");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 넓은 몸통
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5, 0, -3, 10, 10, 6), PartPose.offset(0, 6, 0));
        // 작은 머리 + 눈
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-3, -5, -3, 6, 5, 6)
                .texOffs(24, 16).addBox(-2, -3, -4, 4, 1, 1), PartPose.offset(0, 6, 0));
        // 오른팔: 총열로 끝난다
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-2, 0, -2, 4, 8, 4)
                .texOffs(16, 28).addBox(-1, 8, -1, 2, 6, 2), PartPose.offset(-6, 7, 0));
        // 왼팔: 끝에 회전하는 원형 톱
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-2, 0, -2, 4, 8, 4)
                .texOffs(28, 28).addBox(-4, 8, -1, 8, 8, 1), PartPose.offset(6, 7, 0));
        root.getChild("left_arm").addOrReplaceChild("saw", CubeListBuilder.create().texOffs(46, 28)
                .addBox(-5, -5, -1, 10, 10, 1), PartPose.offset(0, 12, 0));
        // 두꺼운 다리
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-2, 0, -2, 4, 8, 4), PartPose.offset(-3, 16, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-2, 0, -2, 4, 8, 4), PartPose.offset(3, 16, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 느린 걷기
        float swing = Mth.cos(s.walkAnimationPos * 0.4f) * 0.5f * s.walkAnimationSpeed;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightArm.xRot = -swing * 0.4f;
        leftArm.xRot = swing * 0.4f;
        // 톱날 회전
        saw.zRot = t * 0.5f;
        // 머리는 천천히 좌우로
        head.yRot = Mth.sin(t * 0.04f) * 0.25f;
        body.xRot = 0.08f;
    }
}
