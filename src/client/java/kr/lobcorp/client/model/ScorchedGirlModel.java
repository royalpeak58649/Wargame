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

/** 새까맣게 탄 소녀. 가슴을 꿰뚫은 성냥. 격리 중에는 구석에 웅크리고, 탈출하면 앞으로 달려든다. */
public class ScorchedGirlModel extends EntityModel<ScorchedGirlModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("scorched_girl"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
        public float fuse; // 0~1
    }

    private final ModelPart body, head, rightArm, leftArm, rightLeg, leftLeg;

    public ScorchedGirlModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = root.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3, -6, -3, 6, 6, 6), PartPose.offset(0, 11, 0));
        head.addOrReplaceChild("tail_l", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1, 0, -1, 2, 5, 2), PartPose.offsetAndRotation(3, -5, 2, 0.3f, 0, -0.35f));
        head.addOrReplaceChild("tail_r", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1, 0, -1, 2, 5, 2), PartPose.offsetAndRotation(-3, -5, 2, 0.3f, 0, 0.35f));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-2, 0, -1.5f, 4, 6, 3), PartPose.offset(0, 11, 0));
        // 성냥: 등 뒤에서 가슴 앞으로 비스듬히 꿰뚫고, 불붙은 머리가 위로 솟는다
        PartDefinition match = body.addOrReplaceChild("match", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-0.5f, -9, -0.5f, 1, 12, 1), PartPose.offsetAndRotation(1, 3, 0, -0.55f, 0, 0.15f));
        match.addOrReplaceChild("match_head", CubeListBuilder.create().texOffs(36, 0)
                .addBox(-1, -2, -1, 2, 2, 2), PartPose.offset(0, -9, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(16, 12)
                .addBox(-1, 0, -1, 2, 6, 2), PartPose.offset(-3, 11.5f, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(16, 12)
                .addBox(-1, 0, -1, 2, 6, 2), PartPose.offset(3, 11.5f, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(24, 12)
                .addBox(-1, 0, -1, 2, 7, 2), PartPose.offset(-1, 17, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 12)
                .addBox(-1, 0, -1, 2, 7, 2), PartPose.offset(1, 17, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks, walk = s.walkAnimationPos, speed = Math.min(1f, s.walkAnimationSpeed);
        if (!s.breached) {
            // 구석에서 겁먹은 듯 웅크리고 있다
            head.xRot = 0.45f;
            body.xRot = 0.15f;
            rightArm.xRot = -0.9f;
            leftArm.xRot = -0.9f;
            rightArm.zRot = -0.35f;
            leftArm.zRot = 0.35f;
            return;
        }
        // 직원에게 달려든다: 몸을 숙이고 팔을 뒤로
        body.xRot = 0.35f;
        head.xRot = -0.1f;
        head.yRot = s.yRot * Mth.DEG_TO_RAD * 0.5f;
        rightLeg.xRot = Mth.cos(walk * 0.9f) * 1.1f * speed;
        leftLeg.xRot = -Mth.cos(walk * 0.9f) * 1.1f * speed;
        rightArm.xRot = 0.9f;
        leftArm.xRot = 0.9f;
        if (s.fuse > 0) {
            // 터지기 직전: 덜덜 떨면서 부풀어 오른다
            float shake = Mth.sin(t * 3.1f) * 0.6f * s.fuse;
            body.x = shake;
            head.x = shake;
            float swell = 1f + 0.35f * s.fuse;
            body.xScale = swell;
            body.zScale = swell;
            head.xScale = swell;
            head.yScale = swell;
            head.zScale = swell;
            rightArm.xRot = -2.6f;
            leftArm.xRot = -2.6f;
        }
    }
}
