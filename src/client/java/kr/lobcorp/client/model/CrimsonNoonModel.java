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

/** 진홍색 정오. 키 큰 링마스터 광대, 긴 연미복과 실크햇. */
public class CrimsonNoonModel extends EntityModel<CrimsonNoonModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("crimson_noon"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg;

    public CrimsonNoonModel(ModelPart root) {
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
        // 긴 연미복 몸통
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, 0, -2, 8, 12, 4), PartPose.offset(0, 4, 0));
        // 큰 머리 + 코 + 실크햇(2단)
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-4, -8, -4, 8, 8, 8)
                .texOffs(32, 16).addBox(-1, -4, -5, 2, 2, 1)
                .texOffs(32, 20).addBox(-4, -10, -4, 8, 2, 8)
                .texOffs(32, 30).addBox(-3, -18, -3, 6, 8, 6), PartPose.offset(0, 4, 0));
        // 긴 팔
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(-5, 5, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(5, 5, 0));
        // 다리
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(8, 32)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-2, 16, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(8, 32)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(2, 16, 0));
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
        // 팔을 크게 흔든다
        rightArm.xRot = -swing * 0.5f - 0.3f;
        leftArm.xRot = swing * 0.5f - 0.3f;
        rightArm.zRot = -0.5f + Mth.sin(t * 0.15f) * 0.6f;
        leftArm.zRot = 0.5f - Mth.sin(t * 0.15f) * 0.6f;
        // 고개를 갸웃갸웃
        head.zRot = Mth.sin(t * 0.08f) * 0.12f;
        head.yRot = Mth.sin(t * 0.05f) * 0.25f;
    }
}
