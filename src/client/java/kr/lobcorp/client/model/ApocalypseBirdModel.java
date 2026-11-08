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
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** 종말새. 온몸에 눈이 달린 거대한 어둠의 새. */
public class ApocalypseBirdModel extends EntityModel<ApocalypseBirdModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("apocalypse_bird"), "main");

    public static class State extends LivingEntityRenderState {
    }

    private final ModelPart body, neck, head, beak, wingL, wingR, tail, scale;
    private final ModelPart[] eyes = new ModelPart[6];

    public ApocalypseBirdModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        neck = body.getChild("neck");
        head = neck.getChild("head");
        beak = head.getChild("beak");
        wingL = body.getChild("wing_l");
        wingR = body.getChild("wing_r");
        tail = body.getChild("tail");
        scale = body.getChild("scale");
        for (int i = 0; i < 6; i++) {
            eyes[i] = body.getChild("eye" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 몸통 (u0 v0, 24x20x20)
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-12, -20, -10, 24, 20, 20),
                PartPose.offset(0, 20, 0));

        // 긴 목 (u0 v40, 6x14x6)
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-3, -14, -3, 6, 14, 6),
                PartPose.offset(0, -20, -6));

        // 머리 (u0 v60, 10x10x14)
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-5, -10, -7, 10, 10, 14),
                PartPose.offset(0, -14, 0));

        // 부리 (u0 v84, 4x3x6)
        head.addOrReplaceChild("beak", CubeListBuilder.create()
                .texOffs(0, 84).addBox(-2, -2, -6, 4, 3, 6),
                PartPose.offset(0, -3, -7));

        // 날개 (u0 v96 / u56 v96, 4x28x24)
        body.addOrReplaceChild("wing_l", CubeListBuilder.create()
                .texOffs(0, 96).addBox(0, -14, -12, 4, 28, 24),
                PartPose.offset(12, -10, 0));
        body.addOrReplaceChild("wing_r", CubeListBuilder.create()
                .texOffs(56, 96).addBox(-4, -14, -12, 4, 28, 24),
                PartPose.offset(-12, -10, 0));

        // 긴 꼬리 (u0 v24, 4x4x20)
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-2, -2, 0, 4, 4, 20),
                PartPose.offset(0, -6, 10));

        // 몸통의 눈 (u0 v20 / u4 v20 / u8 v20, 2x2x1)
        float[][] eyePos = {{-8, -16, -10}, {-3, -12, -10}, {2, -16, -10}, {7, -12, -10}, {-6, -6, -10}, {4, -6, -10}};
        for (int i = 0; i < 6; i++) {
            body.addOrReplaceChild("eye" + i, CubeListBuilder.create()
                    .texOffs((i % 3) * 4, 20).addBox(-1, -1, -0.5f, 2, 2, 1),
                    PartPose.offset(eyePos[i][0], eyePos[i][1], eyePos[i][2]));
        }

        // 아래로 늘어진 황금 저울 (u0 v48, 1x12x1 + u4 v48, 6x1x1)
        PartDefinition scale = body.addOrReplaceChild("scale", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-0.5f, 0, -0.5f, 1, 12, 1)
                .texOffs(4, 48).addBox(-3, 12, -0.5f, 6, 1, 1),
                PartPose.offset(0, 0, 0));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float flap = Mth.sin(t * 0.08f) * 0.4f;
        wingL.zRot = -0.15f - flap;
        wingR.zRot = 0.15f + flap;
        wingL.xRot = Mth.sin(t * 0.05f) * 0.12f;
        wingR.xRot = Mth.sin(t * 0.05f) * 0.12f;
        body.y = 20 + Mth.sin(t * 0.04f) * 1.2f;
        neck.xRot = Mth.sin(t * 0.05f) * 0.1f;
        head.yRot = Mth.sin(t * 0.03f) * 0.25f;
        head.xRot = Mth.sin(t * 0.045f) * 0.08f;
        beak.xRot = 0.05f + Mth.sin(t * 0.1f) * 0.05f;
        tail.xRot = -0.15f + Mth.sin(t * 0.06f) * 0.1f;
        tail.yRot = Mth.sin(t * 0.04f) * 0.15f;
        scale.zRot = Mth.sin(t * 0.07f) * 0.2f;
        for (int i = 0; i < 6; i++) {
            eyes[i].xScale = 1f + Mth.sin(t * 0.15f + i) * 0.15f;
            eyes[i].yScale = 1f + Mth.sin(t * 0.15f + i) * 0.15f;
        }
    }
}
