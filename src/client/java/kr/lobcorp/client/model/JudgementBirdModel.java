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

/** 검은 사제복을 걸친 키 크고 마른 새. 앞에 금빛 저울을 들고 있다. */
public class JudgementBirdModel extends EntityModel<JudgementBirdModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("judgement_bird"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public float judge; // 0~1
    }

    private final ModelPart neck, head, beak, scales, leftPan, rightPan;

    public JudgementBirdModel(ModelPart root) {
        super(root);
        neck = root.getChild("neck");
        head = neck.getChild("head");
        beak = head.getChild("beak");
        scales = root.getChild("scales");
        leftPan = scales.getChild("left_pan");
        rightPan = scales.getChild("right_pan");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 마른 다리 두 개
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-0.5f, 0, -0.5f, 1, 18, 1), PartPose.offset(-1.5f, 24, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(4, 0)
                .addBox(-0.5f, 0, -0.5f, 1, 18, 1), PartPose.offset(1.5f, 24, 0));

        // 긴 사제복
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 20)
                .addBox(-3, 0, -2, 6, 14, 4), PartPose.offset(0, 6, 0));

        // 가느다란 팔
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(20, 20)
                .addBox(-0.5f, 0, -0.5f, 1, 14, 1), PartPose.offset(-3.5f, 7, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 20)
                .addBox(-0.5f, 0, -0.5f, 1, 14, 1), PartPose.offset(3.5f, 7, 0));

        // 비뚤어진 목
        PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(28, 20)
                .addBox(-1, -6, -1, 2, 6, 2), PartPose.offsetAndRotation(0, 6, 0, 0.35f, 0, 0.12f));

        // 붕대로 감싼 자루 머리
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-2.5f, -6, -2.5f, 5, 6, 5), PartPose.offsetAndRotation(0, -6, 0, 0.5f, 0, 0));

        // 긴 부리
        head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(20, 40)
                .addBox(-0.5f, -0.5f, -6, 1, 1, 6), PartPose.offset(0, -2, -2.5f));

        // 금빛 저울
        PartDefinition scales = root.addOrReplaceChild("scales", CubeListBuilder.create().texOffs(36, 40)
                .addBox(-4, 0, -0.5f, 8, 1, 1), PartPose.offset(0, 12, -4));
        scales.addOrReplaceChild("left_pan", CubeListBuilder.create().texOffs(36, 44)
                .addBox(-1.5f, 0, -1.5f, 3, 1, 3), PartPose.offset(-3.5f, 3, 0));
        scales.addOrReplaceChild("right_pan", CubeListBuilder.create().texOffs(48, 44)
                .addBox(-1.5f, 0, -1.5f, 3, 1, 3), PartPose.offset(3.5f, 3, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float j = Math.min(1f, s.judge);

        // 목과 머리는 앞으로 숙여 있다
        neck.xRot = 0.35f + Mth.sin(t * 0.03f) * 0.02f;
        head.xRot = 0.5f;
        beak.xRot = 0.1f;

        // 저울은 좌우로 천천히 기운다
        scales.zRot = Mth.sin(t * 0.05f) * 0.12f;
        leftPan.y = 3 + Mth.sin(t * 0.05f) * 0.6f;
        rightPan.y = 3 - Mth.sin(t * 0.05f) * 0.6f;

        if (j > 0) {
            // 심판: 저울을 높이 들어 올리고 접시가 수평을 잃는다
            scales.y = Mth.lerp(j, 12f, 4f);
            scales.zRot = Mth.lerp(j, scales.zRot, 0.35f);
            leftPan.y = Mth.lerp(j, leftPan.y, 3f);
            rightPan.y = Mth.lerp(j, rightPan.y, 5f);
            neck.xRot = Mth.lerp(j, neck.xRot, 0.1f);
            head.xRot = Mth.lerp(j, head.xRot, 0.2f);
        }
    }
}
