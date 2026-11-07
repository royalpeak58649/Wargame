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

/** 자물쇠와 사슬로 뒤덮인 상자형 몸. 가슴의 열쇠 구멍 눈은 바라볼 때만 빛난다. */
public class SchadenfreudeModel extends EntityModel<SchadenfreudeModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("schadenfreude"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean watched;
        public boolean breached;
        public float spin; // 0~1 (톱날 회전)
    }

    private final ModelPart head, rightArm, leftArm, rightLeg, leftLeg;
    private final ModelPart pupil, rightBlade, leftBlade;

    public SchadenfreudeModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        ModelPart chest = root.getChild("chest");
        pupil = chest.getChild("pupil");
        rightBlade = rightArm.getChild("blade");
        leftBlade = leftArm.getChild("blade");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 몸통: 두꺼운 철판 상자
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5, 0, -3, 10, 12, 6), PartPose.offset(0, 6, 0));

        // 가슴의 열쇠 구멍 눈 판
        PartDefinition chest = root.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-2, 0, -1, 4, 6, 1), PartPose.offset(0, 8, -3.05f));
        chest.addOrReplaceChild("pupil", CubeListBuilder.create().texOffs(42, 0)
                .addBox(-1, 0, -0.5f, 2, 2, 1), PartPose.offset(0, 1, -0.6f));

        // 사슬에 감긴 작은 머리
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 20)
                .addBox(-2.5f, -4, -2.5f, 5, 4, 5), PartPose.offset(0, 6, 0));

        // 팔 + 끝에 달린 둥근 톱날
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(20, 20)
                .addBox(-1.5f, 0, -1.5f, 3, 11, 3), PartPose.offset(-6.5f, 7, 0));
        rightArm.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(32, 20)
                .addBox(-3, 0, -0.5f, 6, 6, 1), PartPose.offset(0, 11, 0));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(20, 20)
                .addBox(-1.5f, 0, -1.5f, 3, 11, 3), PartPose.offset(6.5f, 7, 0));
        leftArm.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(32, 20)
                .addBox(-3, 0, -0.5f, 6, 6, 1), PartPose.offset(0, 11, 0));

        // 다리
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-1.5f, 0, -1.5f, 3, 10, 3), PartPose.offset(-2.5f, 14, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(12, 32)
                .addBox(-1.5f, 0, -1.5f, 3, 10, 3), PartPose.offset(2.5f, 14, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;

        head.yRot = Mth.sin(t * 0.03f) * 0.08f;
        head.xRot = 0.05f;

        rightArm.xRot = -0.15f;
        leftArm.xRot = -0.15f;
        rightArm.zRot = 0.12f;
        leftArm.zRot = -0.12f;

        rightLeg.xRot = Mth.cos(s.walkAnimationPos * 0.6f) * 0.5f * s.walkAnimationSpeed;
        leftLeg.xRot = Mth.cos(s.walkAnimationPos * 0.6f + (float) Math.PI) * 0.5f * s.walkAnimationSpeed;

        // 열쇠 구멍 속 눈동자는 바라볼 때만 보인다
        pupil.visible = s.watched;

        // 톱날: 탈출해 바라보이는 동안에만 회전
        float spin = (s.breached && s.watched) ? t * 0.9f : 0f;
        rightBlade.zRot = spin;
        leftBlade.zRot = -spin;
    }
}
