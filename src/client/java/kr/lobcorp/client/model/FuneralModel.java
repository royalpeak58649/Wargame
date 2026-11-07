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

/** 검은 상복을 입고 관을 머리에 쓴 사내. 주위를 흰 나비들이 맴돈다. */
public class FuneralModel extends EntityModel<FuneralModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("funeral"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
    }

    private final ModelPart head, rightArm, leftArm;
    private final ModelPart[] butterflies;

    public FuneralModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        butterflies = new ModelPart[]{
                root.getChild("butterfly0"),
                root.getChild("butterfly1"),
                root.getChild("butterfly2"),
                root.getChild("butterfly3"),
                root.getChild("butterfly4")
        };
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, 0, -2, 8, 12, 4), PartPose.offset(0, 12, 0));
        // 흰 셔츠와 넥타이 줄
        root.addOrReplaceChild("shirt", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1, 0, -2.1f, 2, 8, 0.2f), PartPose.offset(0, 12, 0));
        root.addOrReplaceChild("tie", CubeListBuilder.create().texOffs(28, 0)
                .addBox(-0.5f, 0, -2.2f, 1, 6, 0.2f), PartPose.offset(0, 12, 0));
        // 관 머리
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-3, -9, -2, 6, 9, 4), PartPose.offset(0, 12, 0));
        head.addOrReplaceChild("cross_v", CubeListBuilder.create().texOffs(20, 16)
                .addBox(-0.5f, -7, -2.1f, 1, 5, 0.2f), PartPose.ZERO);
        head.addOrReplaceChild("cross_h", CubeListBuilder.create().texOffs(22, 16)
                .addBox(-1.5f, -6, -2.1f, 3, 1, 0.2f), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(-5, 12, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(5, 12, 0));
        root.addOrReplaceChild("right_gun", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-0.5f, 0, -2, 1, 2, 4), PartPose.offset(-5, 22, 0));
        root.addOrReplaceChild("left_gun", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-0.5f, 0, -2, 1, 2, 4), PartPose.offset(5, 22, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 30)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(-2, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 30)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(2, 12, 0));
        // 흰 나비들
        for (int i = 0; i < 5; i++) {
            root.addOrReplaceChild("butterfly" + i, CubeListBuilder.create().texOffs(48, 0)
                    .addBox(-1, -0.5f, -1, 2, 1, 2), PartPose.offset(0, 10, 0));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        head.xRot = 0.05f + Mth.sin(t * 0.03f) * 0.02f;
        rightArm.xRot = -0.9f;
        leftArm.xRot = -0.9f;
        rightArm.zRot = 0.1f;
        leftArm.zRot = -0.1f;
        if (s.breached) {
            rightArm.xRot = -1.4f;
            leftArm.xRot = -1.4f;
        }
        for (int i = 0; i < butterflies.length; i++) {
            ModelPart b = butterflies[i];
            float phase = i * 1.7f;
            b.x = Mth.cos(t * 0.06f + phase) * 1.1f;
            b.y = 8f + Mth.sin(t * 0.09f + phase) * 1.2f;
            b.z = Mth.sin(t * 0.05f + phase) * 1.1f;
            b.yRot = t * 0.2f + phase;
            b.zRot = Mth.sin(t * 0.4f + phase) * 0.6f;
        }
    }
}
