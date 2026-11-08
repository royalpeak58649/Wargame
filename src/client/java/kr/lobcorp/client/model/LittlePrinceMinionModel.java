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

/** 등이 굽은 어린 왕자-1. 등과 머리에 버섯이 돋아 있다. */
public class LittlePrinceMinionModel extends EntityModel<LittlePrinceMinionModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("little_prince_minion"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg;

    public LittlePrinceMinionModel(ModelPart root) {
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
        // 머리
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-3, -6, -3, 6, 6, 6), PartPose.offset(0, 8, 0));
        // 머리에 돋은 버섯
        head.addOrReplaceChild("head_mushroom", CubeListBuilder.create()
                .texOffs(24, 12).addBox(-2, -2, -2, 4, 2, 4)
                .texOffs(24, 18).addBox(-1, 0, -1, 2, 1, 2), PartPose.offset(0, -6, 0));
        // 등에 돋은 버섯들
        root.addOrReplaceChild("back_mushroom", CubeListBuilder.create().texOffs(24, 20)
                .addBox(-2, -1, 0, 4, 2, 3), PartPose.offset(0, 10, 2));
        root.addOrReplaceChild("back_mushroom2", CubeListBuilder.create().texOffs(24, 25)
                .addBox(-1, -1, 0, 2, 2, 2), PartPose.offset(1.5f, 14, 2));
        // 긴 팔
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(-4, 9, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(4, 9, 0));
        // 다리
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(12, 24)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-1.5f, 16, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(12, 24)
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
        rightArm.xRot = -swing * 0.6f - 0.3f;
        leftArm.xRot = swing * 0.6f - 0.3f;
        // 등이 굽은 자세
        head.xRot = 0.3f + Mth.sin(t * 0.08f) * 0.05f;
        body.xRot = 0.12f;
    }
}
