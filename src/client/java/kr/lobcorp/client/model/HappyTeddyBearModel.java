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

/** 앉아서 앞에 놓인 액자를 바라보는 낡은 곰인형. 포옹 때 두 팔을 앞으로 감는다. */
public class HappyTeddyBearModel extends EntityModel<HappyTeddyBearModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("happy_teddy_bear"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public float hug; // 0~1
    }

    private final ModelPart head, rightArm, leftArm;

    public HappyTeddyBearModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, 0, -3, 8, 9, 6), PartPose.offset(0, 12, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 0)
                .addBox(-4, -7, -3, 8, 7, 6), PartPose.offset(0, 12, 0));
        head.addOrReplaceChild("ears", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-4, -9, -1, 2, 2, 1)
                .texOffs(56, 0).addBox(2, -9, -1, 2, 2, 1), PartPose.ZERO);
        head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-2, -3, -5, 4, 3, 2), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 15)
                .addBox(-1.5f, 0, -1.5f, 3, 7, 3), PartPose.offset(-5, 13, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 15)
                .addBox(-1.5f, 0, -1.5f, 3, 7, 3), PartPose.offset(5, 13, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(12, 15)
                .addBox(-2, 0, -5, 4, 3, 5), PartPose.offset(-2.5f, 21, -1));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(12, 15)
                .addBox(-2, 0, -5, 4, 3, 5), PartPose.offset(2.5f, 21, -1));
        // 곰인형이 하염없이 바라보는 액자
        root.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(30, 15)
                .addBox(-3, -7, 0, 6, 7, 1), PartPose.offsetAndRotation(0, 24, -10, 0.25f, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        head.xRot = 0.35f + Mth.sin(t * 0.02f) * 0.015f; // 액자를 내려다본다
        head.zRot = 0.08f;
        rightArm.xRot = -0.35f;
        leftArm.xRot = -0.35f;
        rightArm.zRot = 0.15f;
        leftArm.zRot = -0.15f;
        if (s.hug > 0) {
            float h = Math.min(1f, s.hug * 3f);
            rightArm.xRot = Mth.lerp(h, -0.35f, -1.5f);
            leftArm.xRot = Mth.lerp(h, -0.35f, -1.5f);
            rightArm.yRot = -0.7f * h;
            leftArm.yRot = 0.7f * h;
            head.xRot = Mth.lerp(h, 0.35f, 0f);
        }
    }
}
