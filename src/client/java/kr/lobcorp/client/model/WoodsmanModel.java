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

/** 주석으로 된 나무꾼. 가슴에 심장이 들어갈 빈자리가 뚫려 있고, 오른손에 커다란 도끼를 쥐고 있다. */
public class WoodsmanModel extends EntityModel<WoodsmanModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("woodsman"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
        public float swing; // 0~1
    }

    private final ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg, axe;

    public WoodsmanModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        axe = rightArm.getChild("axe");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, 0, -2.5f, 8, 9, 5), PartPose.offset(0, 9, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-3, -7, -3, 6, 7, 6), PartPose.offset(0, 9, 0));
        // 깔때기를 엎어 놓은 모자
        head.addOrReplaceChild("hat_brim", CubeListBuilder.create().texOffs(28, 16)
                .addBox(-2, -8, -2, 4, 1, 4), PartPose.ZERO);
        head.addOrReplaceChild("hat_top", CubeListBuilder.create().texOffs(28, 22)
                .addBox(-1, -11, -1, 2, 3, 2), PartPose.ZERO);
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1.5f, 0, -1.5f, 3, 9, 3), PartPose.offset(-5.5f, 9.5f, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1.5f, 0, -1.5f, 3, 9, 3), PartPose.offset(5.5f, 9.5f, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(36, 0)
                .addBox(-1.5f, 0, -1.5f, 3, 10, 3), PartPose.offset(-2, 14, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(36, 0)
                .addBox(-1.5f, 0, -1.5f, 3, 10, 3), PartPose.offset(2, 14, 0));
        // 오른손에 쥔 도끼: 자루 + 날
        PartDefinition axe = rightArm.addOrReplaceChild("axe", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-0.5f, -8, -0.5f, 1, 16, 1), PartPose.offsetAndRotation(0, 8, 0, 0, 0, 0.15f));
        axe.addOrReplaceChild("axe_head", CubeListBuilder.create().texOffs(8, 32)
                .addBox(-2.5f, -8, -0.5f, 5, 4, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        head.yRot = s.yRot * Mth.DEG_TO_RAD;
        head.xRot = s.xRot * Mth.DEG_TO_RAD;
        if (s.breached) {
            // 느릿하게 걷는다
            float walk = s.walkAnimationSpeed;
            float pos = s.walkAnimationPos;
            rightLeg.xRot = Mth.cos(pos * 0.5f) * 0.5f * walk;
            leftLeg.xRot = Mth.cos(pos * 0.5f + Mth.PI) * 0.5f * walk;
            rightArm.xRot = -0.25f + Mth.cos(pos * 0.5f + Mth.PI) * 0.3f * walk;
            leftArm.xRot = -0.25f + Mth.cos(pos * 0.5f) * 0.3f * walk;
            rightArm.zRot = 0.1f;
            leftArm.zRot = -0.1f;
        } else {
            // 격리 중에는 도끼를 세워 들고 서 있다
            rightArm.xRot = -0.35f;
            leftArm.xRot = -0.35f;
            rightArm.zRot = 0.12f;
            leftArm.zRot = -0.12f;
            rightLeg.xRot = 0;
            leftLeg.xRot = 0;
        }
        if (s.swing > 0) {
            float sw = Math.min(1f, s.swing * 2f);
            rightArm.xRot = Mth.lerp(sw, rightArm.xRot, -2.2f);
            rightArm.zRot = Mth.lerp(sw, rightArm.zRot, 0f);
            body.xRot = Mth.lerp(sw, 0f, 0.15f);
        } else {
            body.xRot = 0f;
        }
        axe.xRot = 0f;
        // 가슴의 빈자리가 살짝 벌어졌다 닫힌다
        body.zRot = Mth.sin(t * 0.03f) * 0.01f;
    }
}
