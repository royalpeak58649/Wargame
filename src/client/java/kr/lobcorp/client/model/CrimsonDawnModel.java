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

/** 진홍색 여명. 큰 머리에 고깔모자를 쓴 작은 광대. */
public class CrimsonDawnModel extends EntityModel<CrimsonDawnModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("crimson_dawn"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg;

    public CrimsonDawnModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 짧은 몸통
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3, 0, -2, 6, 6, 4), PartPose.offset(0, 10, 0));
        // 큰 머리 + 둥근 코 + 고깔모자(2단)
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-4, -8, -4, 8, 8, 8)
                .texOffs(32, 10).addBox(-1, -4, -5, 2, 2, 1)
                .texOffs(32, 14).addBox(-3, -10, -3, 6, 2, 6)
                .texOffs(32, 22).addBox(-1, -13, -1, 2, 3, 2), PartPose.offset(0, 10, 0));
        // 팔
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 26)
                .addBox(-1, 0, -1, 2, 6, 2), PartPose.offset(-4, 11, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 26)
                .addBox(-1, 0, -1, 2, 6, 2), PartPose.offset(4, 11, 0));
        // 다리
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(12, 26)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-1.5f, 16, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(12, 26)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(1.5f, 16, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 걷기 스윙
        float swing = Mth.cos(s.walkAnimationPos * 0.6662f) * 0.8f * s.walkAnimationSpeed;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        // 팔은 흔들흔들
        rightArm.xRot = -swing * 0.4f - 0.4f;
        leftArm.xRot = swing * 0.4f - 0.4f;
        rightArm.zRot = -0.4f + Mth.sin(t * 0.2f) * 0.5f;
        leftArm.zRot = 0.4f - Mth.sin(t * 0.2f) * 0.5f;
        // 고개를 갸웃갸웃
        head.zRot = Mth.sin(t * 0.1f) * 0.15f;
        head.yRot = Mth.sin(t * 0.07f) * 0.2f;
    }
}
