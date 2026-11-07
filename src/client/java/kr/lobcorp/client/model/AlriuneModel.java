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

/** 꽃잎과 가지로 빚은 키 큰 여인. 탈출하면 등 뒤의 꽃잎이 하나씩 피어난다. */
public class AlriuneModel extends EntityModel<AlriuneModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("alriune"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public int petals; // 0~3
        public boolean breached;
    }

    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart petal1;
    private final ModelPart petal2;
    private final ModelPart petal3;

    public AlriuneModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        petal1 = root.getChild("petal1");
        petal2 = root.getChild("petal2");
        petal3 = root.getChild("petal3");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 꽃으로 지은 긴 드레스
        root.addOrReplaceChild("dress", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, 0, -3, 8, 16, 6), PartPose.offset(0, 8, 0));
        // 몸통
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 0)
                .addBox(-2.5f, 0, -1.5f, 5, 6, 3), PartPose.offset(0, 2, 0));
        // 가느다란 가지 같은 팔
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(44, 0)
                .addBox(-0.5f, 0, -0.5f, 1, 10, 1), PartPose.offset(-3, 3, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(44, 0)
                .addBox(-0.5f, 0, -0.5f, 1, 10, 1), PartPose.offset(3, 3, 0));
        // 얼굴이 없는 머리
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-2.5f, -6, -2.5f, 5, 6, 5), PartPose.offset(0, 2, 0));
        // 꽃잎 왕관
        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(20, 24)
                .addBox(-3, -8, -3, 6, 2, 6), PartPose.ZERO);
        // 등 뒤의 큰 꽃잎 세 장
        root.addOrReplaceChild("petal1", CubeListBuilder.create().texOffs(0, 36)
                .addBox(-3, -8, 0, 6, 8, 1), PartPose.offsetAndRotation(-2, 6, 3, 0.25f, 0.5f, 0.15f));
        root.addOrReplaceChild("petal2", CubeListBuilder.create().texOffs(16, 36)
                .addBox(-3, -8, 0, 6, 8, 1), PartPose.offsetAndRotation(2, 6, 3, 0.25f, -0.5f, -0.15f));
        root.addOrReplaceChild("petal3", CubeListBuilder.create().texOffs(32, 36)
                .addBox(-3, -8, 0, 6, 8, 1), PartPose.offsetAndRotation(0, 5, 3.5f, 0.35f, 0f, 0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 아주 느리게 흔들린다
        head.yRot = Mth.sin(t * 0.015f) * 0.12f;
        head.xRot = 0.05f + Mth.sin(t * 0.02f) * 0.02f;
        rightArm.xRot = -0.15f + Mth.sin(t * 0.02f) * 0.05f;
        leftArm.xRot = -0.15f - Mth.sin(t * 0.02f) * 0.05f;
        rightArm.zRot = 0.12f;
        leftArm.zRot = -0.12f;

        if (s.breached) {
            petal1.visible = s.petals >= 1;
            petal2.visible = s.petals >= 2;
            petal3.visible = s.petals >= 3;
            float open = s.petals / 3f;
            petal1.yRot = 0.5f + open * 0.4f;
            petal2.yRot = -0.5f - open * 0.4f;
            petal3.xRot = 0.35f + open * 0.3f;
        } else {
            // 격리 중에는 세 장 모두 작게 접혀 있다
            petal1.visible = true;
            petal2.visible = true;
            petal3.visible = true;
            petal1.yRot = 0.2f;
            petal2.yRot = -0.2f;
            petal3.xRot = 0.1f;
        }
    }
}
