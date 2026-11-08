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

/** 불새. 진홍빛 몸통에 커다란 날개 두 장, 길게 흐르는 꼬리 깃. */
public class FirebirdModel extends EntityModel<FirebirdModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("firebird"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart body, neck, head, beak, wingL, wingR, tail;

    public FirebirdModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        neck = body.getChild("neck");
        head = neck.getChild("head");
        beak = head.getChild("beak");
        wingL = body.getChild("wing_l");
        wingR = body.getChild("wing_r");
        tail = body.getChild("tail");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 몸통 (u0 v0, 6x6x8)
        PartDefinition bodyPart = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3, -3, -4, 6, 6, 8), PartPose.offset(0, 14, 0));
        // 목 (u0 v16, 3x4x3) 과 머리 (u0 v24, 4x4x4), 부리 (u16 v24, 2x1x3)
        PartDefinition neckPart = bodyPart.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-1.5f, -4, -1.5f, 3, 4, 3), PartPose.offset(0, -3, -3));
        PartDefinition headPart = neckPart.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-2, -4, -2, 4, 4, 4), PartPose.offset(0, -4, 0));
        headPart.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(16, 24)
                .addBox(-1, -1, -3, 2, 1, 3), PartPose.offset(0, -1, -2));
        // 왼쪽 날개: 어깨 → 중간 → 끝 (넓고 얇은 판 세 장)
        PartDefinition wingLPart = bodyPart.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(0, 32)
                .addBox(0, -1, -3, 10, 1, 6), PartPose.offset(3, -2, 0));
        PartDefinition wingLMid = wingLPart.addOrReplaceChild("wing_l_mid", CubeListBuilder.create().texOffs(0, 40)
                .addBox(0, -1, -3, 10, 1, 6), PartPose.offset(10, 0, 0));
        wingLMid.addOrReplaceChild("wing_l_tip", CubeListBuilder.create().texOffs(0, 48)
                .addBox(0, -1, -2, 8, 1, 4), PartPose.offset(10, 0, 0));
        // 오른쪽 날개
        PartDefinition wingRPart = bodyPart.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(24, 32)
                .addBox(-10, -1, -3, 10, 1, 6), PartPose.offset(-3, -2, 0));
        PartDefinition wingRMid = wingRPart.addOrReplaceChild("wing_r_mid", CubeListBuilder.create().texOffs(24, 40)
                .addBox(-10, -1, -3, 10, 1, 6), PartPose.offset(-10, 0, 0));
        wingRMid.addOrReplaceChild("wing_r_tip", CubeListBuilder.create().texOffs(24, 48)
                .addBox(-8, -1, -2, 8, 1, 4), PartPose.offset(-10, 0, 0));
        // 꼬리 깃 세 장 (u32 v0 / u32 v8 / u32 v16, 각 4x1x8)
        PartDefinition tailPart = bodyPart.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-2, -1, 0, 4, 1, 8), PartPose.offset(0, -1, 4));
        tailPart.addOrReplaceChild("tail_mid", CubeListBuilder.create().texOffs(32, 8)
                .addBox(-2, -1, 0, 4, 1, 8), PartPose.offset(0, 0, 8));
        tailPart.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(32, 16)
                .addBox(-2, -1, 0, 4, 1, 8), PartPose.offset(0, 0, 8));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 날개는 크게 퍼덕인다
        float flap = 0.3f + Mth.sin(t * 0.3f) * 0.5f;
        wingL.zRot = flap;
        wingR.zRot = -flap;
        // 꼬리는 좌우로 흔들린다
        tail.yRot = Mth.sin(t * 0.12f) * 0.35f;
        tail.xRot = Mth.sin(t * 0.09f + 1.2f) * 0.12f;
        // 머리는 살짝 끄덕인다
        neck.xRot = Mth.sin(t * 0.15f) * 0.1f;
        head.yRot = Mth.sin(t * 0.07f) * 0.2f;
        beak.xRot = Mth.sin(t * 0.4f) * 0.1f;
        // 몸통은 아주 미세하게 흔들린다
        body.zRot = Mth.sin(t * 0.05f) * 0.04f;
    }
}
