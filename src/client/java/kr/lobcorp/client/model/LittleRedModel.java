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

/** 붉은 두건의 여성 용병. 도끼를 든 오른팔과 권총을 쏘는 자세. */
public class LittleRedModel extends EntityModel<LittleRedModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("little_red"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
        public boolean berserk;
        public float shot; // 0~1
    }

    private final ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg, cape, hood;

    public LittleRedModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        cape = root.getChild("cape");
        hood = root.getChild("hood");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3, 0, -1.5f, 6, 8, 3), PartPose.offset(0, 12, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-3, -6, -3, 6, 6, 6), PartPose.offset(0, 12, 0));
        head.addOrReplaceChild("scar", CubeListBuilder.create().texOffs(24, 12)
                .addBox(-3, -5, -3.1f, 2, 1, 0), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1, 0, -1, 2, 9, 2), PartPose.offset(-4, 12.5f, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-1, 0, -1, 2, 9, 2), PartPose.offset(4, 12.5f, 0));
        // 오른손의 도끼
        root.addOrReplaceChild("cleaver", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-0.5f, 7, -3, 1, 6, 3), PartPose.offset(-4, 12.5f, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(-1.5f, 14, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(8, 24)
                .addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(1.5f, 14, 0));
        // 두건과 망토
        root.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(0, 36)
                .addBox(-3.5f, -7, -3.5f, 7, 7, 7), PartPose.offset(0, 12, 0));
        root.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(28, 36)
                .addBox(-4, 0, 0, 8, 14, 1), PartPose.offset(0, 12, 1.5f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float swing = s.walkAnimationPos;
        float speed = Math.min(1f, s.walkAnimationSpeed);
        rightLeg.xRot = Mth.cos(swing * 0.6662f) * 0.9f * speed;
        leftLeg.xRot = Mth.cos(swing * 0.6662f + (float) Math.PI) * 0.9f * speed;
        rightArm.xRot = Mth.cos(swing * 0.6662f + (float) Math.PI) * 0.7f * speed;
        leftArm.xRot = Mth.cos(swing * 0.6662f) * 0.7f * speed;
        head.yRot = s.yRot * (float) (Math.PI / 180.0);
        head.xRot = s.xRot * (float) (Math.PI / 180.0);
        cape.xRot = 0.12f + Mth.sin(t * 0.08f) * 0.04f + speed * 0.25f;
        hood.xRot = head.xRot * 0.5f;
        hood.yRot = head.yRot * 0.5f;
        if (s.breached) {
            // 달려들 때 도끼를 치켜든다
            rightArm.xRot = -1.2f + Mth.sin(t * 0.4f) * 0.3f;
            rightArm.zRot = 0.2f;
        }
        if (s.berserk) {
            rightArm.xRot = -1.6f + Mth.sin(t * 0.9f) * 0.5f;
            head.xRot = -0.2f;
        }
        if (s.shot > 0) {
            leftArm.xRot = -1.5f;
            leftArm.yRot = 0.1f;
        }
    }
}
