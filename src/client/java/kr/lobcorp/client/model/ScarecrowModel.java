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

/** 지혜를 찾는 허수아비: 기둥에 매달린 짚 인형. 탈출하면 기둥을 버리고 걸어 다닌다. */
public class ScarecrowModel extends EntityModel<ScarecrowModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("scarecrow"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
        public float swing; // 0~1
    }

    private final ModelPart pole, body, head, rightArm, leftArm, rightLeg, leftLeg;

    public ScarecrowModel(ModelPart root) {
        super(root);
        pole = root.getChild("pole");
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
        // 기둥: 격리 중에만 보인다
        root.addOrReplaceChild("pole", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1, -12, -1, 2, 24, 2), PartPose.offset(0, 24, 0));
        // 짚 몸통 + 기운 셔츠
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 26)
                .addBox(-3, 0, -2, 6, 8, 4), PartPose.offset(0, 12, 0));
        // 부대 머리
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(20, 26)
                .addBox(-3, -7, -3, 6, 7, 6), PartPose.offset(0, 12, 0));
        head.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(44, 26).addBox(-4, -1, -4, 8, 1, 8)
                .texOffs(44, 36).addBox(-2, -4, -2, 4, 3, 4), PartPose.offset(0, -7, 0));
        // 팔: 격리 중에는 옆으로 뻗고, 탈출하면 휘두른다
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-4, 13, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(8, 40)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(4, 13, 0));
        // 짚 다리
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 40)
                .addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(-1.5f, 20, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 40)
                .addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(1.5f, 20, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        pole.visible = !s.breached;
        head.xRot = 0.12f + Mth.sin(t * 0.03f) * 0.03f;
        head.zRot = Mth.sin(t * 0.02f) * 0.04f;
        if (!s.breached) {
            // 기둥에 매달려 옆으로 뻗은 팔
            rightArm.xRot = 0f;
            leftArm.xRot = 0f;
            rightArm.zRot = 1.45f;
            leftArm.zRot = -1.45f;
            rightLeg.xRot = 0.05f;
            leftLeg.xRot = -0.05f;
        } else {
            float walk = Mth.cos(s.walkAnimationPos * 0.6662f) * 0.7f * s.walkAnimationSpeed;
            rightLeg.xRot = walk;
            leftLeg.xRot = -walk;
            rightArm.zRot = 0.1f;
            leftArm.zRot = -0.1f;
            rightArm.xRot = -walk * 0.6f;
            leftArm.xRot = walk * 0.6f;
            if (s.swing > 0) {
                float sw = Math.min(1f, s.swing * 3f);
                rightArm.xRot = Mth.lerp(sw, rightArm.xRot, -2.2f);
                rightArm.zRot = Mth.lerp(sw, rightArm.zRot, 0.3f);
            }
        }
    }
}
