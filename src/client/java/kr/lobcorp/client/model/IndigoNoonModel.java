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

/** 쪽빛 정오. 후드를 눌러쓴 채 몸을 숙인 청소부, 긴 팔 끝에 칼날. */
public class IndigoNoonModel extends EntityModel<IndigoNoonModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("indigo_noon"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg;

    public IndigoNoonModel(ModelPart root) {
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
        // 길게 늘어진 망토 몸통
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, 0, -3, 8, 12, 6), PartPose.offset(0, 6, 0));
        // 후드 + 그늘진 얼굴
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 18)
                .addBox(-4, -7, -4, 8, 7, 8)
                .texOffs(32, 18).addBox(-3, -5, -5, 6, 4, 1), PartPose.offset(0, 6, 0));
        // 긴 팔 + 칼날
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 34)
                .addBox(-1, 0, -1, 2, 12, 2)
                .texOffs(8, 34).addBox(-1, 12, -1, 2, 4, 1), PartPose.offset(-5, 7, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 34)
                .addBox(-1, 0, -1, 2, 12, 2)
                .texOffs(8, 34).addBox(-1, 12, -1, 2, 4, 1), PartPose.offset(5, 7, 0));
        // 다리
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 34)
                .addBox(-1, 0, -1, 2, 6, 2), PartPose.offset(-2, 18, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 34)
                .addBox(-1, 0, -1, 2, 6, 2), PartPose.offset(2, 18, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 걷기 스윙
        float swing = Mth.cos(s.walkAnimationPos * 0.6662f) * 0.7f * s.walkAnimationSpeed;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightArm.xRot = -swing * 0.5f;
        leftArm.xRot = swing * 0.5f;
        // 몸을 숙인 자세
        body.xRot = 0.25f;
        head.xRot = -0.15f + Mth.sin(t * 0.05f) * 0.08f;
        head.yRot = Mth.sin(t * 0.04f) * 0.2f;
    }
}
