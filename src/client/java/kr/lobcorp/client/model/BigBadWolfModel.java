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

/** 두 발로 선 거대한 회색 늑대. 입에는 양털이 물려 있고, 눈은 슬프다. */
public class BigBadWolfModel extends EntityModel<BigBadWolfModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("big_bad_wolf"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
        public float howl; // 0~1
        public float bite; // 0~1
    }

    private final ModelPart root;
    private final ModelPart body, head, snout, fleece, rightArm, leftArm, rightLeg, leftLeg;

    public BigBadWolfModel(ModelPart root) {
        super(root);
        this.root = root;
        body = root.getChild("body");
        head = root.getChild("head");
        snout = head.getChild("snout");
        fleece = head.getChild("fleece");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 몸통: 회색 털, 가슴은 밝다
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5, -10, -3, 10, 10, 6), PartPose.offset(0, 14, 0));
        root.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-4, -8, -4, 8, 7, 2), PartPose.offset(0, 14, 0));
        // 머리 + 긴 주둥이 + 귀 + 물고 있는 양털
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-3, -6, -3.5f, 6, 6, 7), PartPose.offset(0, 4, 0));
        head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(26, 16)
                .addBox(-2, -2, -4, 4, 3, 4), PartPose.offset(0, -1, -3.5f));
        head.addOrReplaceChild("ears", CubeListBuilder.create()
                .texOffs(42, 16).addBox(-3, -8, -1, 2, 3, 1)
                .texOffs(42, 16).addBox(1, -8, -1, 2, 3, 1), PartPose.ZERO);
        head.addOrReplaceChild("fleece", CubeListBuilder.create().texOffs(48, 16)
                .addBox(-2, -1, -2, 4, 3, 2), PartPose.offset(0, -2, -7.5f));
        // 긴 팔 + 발톱
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 30)
                .addBox(-1.5f, 0, -1.5f, 3, 11, 3), PartPose.offset(-6, 5, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 30)
                .addBox(-1.5f, 0, -1.5f, 3, 11, 3), PartPose.offset(6, 5, 0));
        root.addOrReplaceChild("claws", CubeListBuilder.create().texOffs(12, 30)
                .addBox(-2, 0, -2, 4, 1, 4), PartPose.offset(0, 16, 0));
        // 발가락으로 선 다리
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(24, 30)
                .addBox(-1.5f, 0, -1.5f, 3, 10, 3), PartPose.offset(-2.5f, 14, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 30)
                .addBox(-1.5f, 0, -1.5f, 3, 10, 3), PartPose.offset(2.5f, 14, 0));
        root.addOrReplaceChild("feet", CubeListBuilder.create().texOffs(36, 30)
                .addBox(-2, 0, -3, 4, 1, 5), PartPose.offset(0, 23, 0));
        // 꼬리
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(48, 30)
                .addBox(-1, 0, 0, 2, 6, 2), PartPose.offsetAndRotation(0, 14, 3, 0.6f, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float walk = s.walkAnimationSpeed;
        float pos = s.walkAnimationPos;
        // 슬픈 눈: 머리를 조금 숙인다
        head.xRot = 0.12f + Mth.sin(t * 0.03f) * 0.02f;
        head.yRot = Mth.sin(t * 0.02f) * 0.08f;
        rightArm.xRot = Mth.cos(pos * 0.6662f) * 0.7f * walk;
        leftArm.xRot = Mth.cos(pos * 0.6662f + Mth.PI) * 0.7f * walk;
        rightArm.zRot = 0.08f;
        leftArm.zRot = -0.08f;
        rightLeg.xRot = Mth.cos(pos * 0.6662f + Mth.PI) * 0.8f * walk;
        leftLeg.xRot = Mth.cos(pos * 0.6662f) * 0.8f * walk;
        if (s.breached) {
            head.xRot -= 0.1f;
            rightArm.xRot -= 0.25f;
            leftArm.xRot -= 0.25f;
        }
        if (s.howl > 0) {
            float h = Math.min(1f, s.howl * 2f);
            head.xRot = Mth.lerp(h, head.xRot, -0.9f);
            snout.xRot = Mth.lerp(h, 0f, -0.2f);
            fleece.visible = h < 0.5f;
        } else {
            snout.xRot = 0f;
            fleece.visible = true;
        }
        if (s.bite > 0) {
            float b = Math.min(1f, s.bite * 2f);
            head.xRot = Mth.lerp(b, head.xRot, 0.35f);
            snout.xRot = Mth.lerp(b, 0f, 0.4f);
        }
    }
}
