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

/** 등이 굽은 사람-벌. 뒤로 줄무늬 배가 달려 있고 날개를 빠르게 떤다. */
public class WorkerBeeModel extends EntityModel<WorkerBeeModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("worker_bee"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart head, abdomen, rightArm, leftArm, rightLeg, leftLeg, rightWing, leftWing;

    public WorkerBeeModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        abdomen = root.getChild("abdomen");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        rightWing = root.getChild("right_wing");
        leftWing = root.getChild("left_wing");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 가슴
        root.addOrReplaceChild("thorax", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3, 0, -2, 6, 6, 4), PartPose.offset(0, 10, 0));
        // 뒤로 뻗은 줄무늬 배
        root.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-3, -3, 0, 6, 6, 8), PartPose.offset(0, 13, 2));
        // 벌 머리
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 0)
                .addBox(-2.5f, -2.5f, -2.5f, 5, 5, 5), PartPose.offset(0, 9, -4));
        head.addOrReplaceChild("eyes", CubeListBuilder.create()
                .texOffs(44, 0).addBox(-2.5f, -1.5f, -2.6f, 2, 2, 1)
                .texOffs(44, 0).addBox(0.5f, -1.5f, -2.6f, 2, 2, 1), PartPose.ZERO);
        head.addOrReplaceChild("antennae", CubeListBuilder.create()
                .texOffs(50, 0).addBox(-1.5f, -3, -0.5f, 1, 3, 1)
                .texOffs(50, 0).addBox(0.5f, -3, -0.5f, 1, 3, 1), PartPose.ZERO);
        // 팔: 끝에 침
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-4, 11, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(4, 11, 0));
        // 다리
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(12, 28)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-1.5f, 16, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(12, 28)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(1.5f, 16, 0));
        // 작은 날개
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(24, 28)
                .addBox(-5, 0, 0, 5, 1, 6), PartPose.offset(-2, 9, 1));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(24, 28)
                .addBox(0, 0, 0, 5, 1, 6), PartPose.offset(2, 9, 1));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float flap = Mth.sin(t * 1.2f) * 0.6f;
        rightWing.zRot = -0.3f - flap;
        leftWing.zRot = 0.3f + flap;
        rightWing.yRot = -0.2f;
        leftWing.yRot = 0.2f;
        // 걷기 스윙
        float swing = Mth.cos(s.walkAnimationPos * 0.6662f) * 0.8f * s.walkAnimationSpeed;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightArm.xRot = -swing * 0.6f - 0.3f;
        leftArm.xRot = swing * 0.6f - 0.3f;
        // 등이 굽은 자세
        head.xRot = 0.25f;
        abdomen.xRot = -0.2f + Mth.sin(t * 0.1f) * 0.05f;
    }
}
