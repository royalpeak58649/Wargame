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

/** 달의 울음. 평소에는 달 얼굴이 새겨진 그랜드 피아노, 탈출하면 검은 코트를 입은 달머리 형체가 된다. */
public class IlPiantoModel extends EntityModel<IlPiantoModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("il_pianto"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
    }

    private final ModelPart piano, pianoBody, pianoLid, pianoLegLeft, pianoLegRight, pianoLegBack, pianoKeys;
    private final ModelPart moonFace, moonFaceEyeLeft, moonFaceEyeRight, moonFaceTear;
    private final ModelPart figure, figureBody, figureHead, figureArmLeft, figureArmRight;

    public IlPiantoModel(ModelPart root) {
        super(root);
        piano = root.getChild("piano");
        pianoBody = piano.getChild("piano_body");
        pianoLid = piano.getChild("piano_lid");
        pianoLegLeft = piano.getChild("piano_leg_left");
        pianoLegRight = piano.getChild("piano_leg_right");
        pianoLegBack = piano.getChild("piano_leg_back");
        pianoKeys = piano.getChild("piano_keys");
        moonFace = piano.getChild("moon_face");
        moonFaceEyeLeft = moonFace.getChild("moon_eye_left");
        moonFaceEyeRight = moonFace.getChild("moon_eye_right");
        moonFaceTear = moonFace.getChild("moon_tear");
        figure = root.getChild("figure");
        figureBody = figure.getChild("figure_body");
        figureHead = figure.getChild("figure_head");
        figureArmLeft = figure.getChild("figure_arm_left");
        figureArmRight = figure.getChild("figure_arm_right");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── 피아노 (u0 v0 ~) ──
        PartDefinition piano = root.addOrReplaceChild("piano", CubeListBuilder.create(),
                PartPose.offset(0, 24, 0));
        // 몸통 12x5x10 (u0 v0)
        piano.addOrReplaceChild("piano_body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-6, -5, -5, 12, 5, 10), PartPose.offset(0, 0, 0));
        // 들어올린 뚜껑 12x1x10 (u0 v16)
        piano.addOrReplaceChild("piano_lid", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-6, -1, -5, 12, 1, 10), PartPose.offset(0, -5, 0));
        // 다리 세 개 1x6x1 (u0 v28)
        piano.addOrReplaceChild("piano_leg_left", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-0.5f, 0, -0.5f, 1, 6, 1), PartPose.offset(-5, 0, -4));
        piano.addOrReplaceChild("piano_leg_right", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-0.5f, 0, -0.5f, 1, 6, 1), PartPose.offset(5, 0, -4));
        piano.addOrReplaceChild("piano_leg_back", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-0.5f, 0, -0.5f, 1, 6, 1), PartPose.offset(0, 0, 4));
        // 건반 10x1x2 (u0 v36)
        piano.addOrReplaceChild("piano_keys", CubeListBuilder.create().texOffs(0, 36)
                .addBox(-5, -1, -1, 10, 1, 2), PartPose.offset(0, -5, -6));
        // 뚜껑 앞면의 초승달 얼굴 (u0 v40 / u8 v40 / u16 v40)
        PartDefinition moonFace = piano.addOrReplaceChild("moon_face", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-3, -3, -0.5f, 6, 6, 1), PartPose.offset(0, -8, -5.5f));
        moonFace.addOrReplaceChild("moon_eye_left", CubeListBuilder.create().texOffs(8, 40)
                .addBox(-0.5f, -0.5f, -0.5f, 1, 1, 1), PartPose.offset(-1.5f, -1, -0.5f));
        moonFace.addOrReplaceChild("moon_eye_right", CubeListBuilder.create().texOffs(8, 40)
                .addBox(-0.5f, -0.5f, -0.5f, 1, 1, 1), PartPose.offset(1.5f, -1, -0.5f));
        moonFace.addOrReplaceChild("moon_tear", CubeListBuilder.create().texOffs(16, 40)
                .addBox(-0.5f, 0, -0.5f, 1, 3, 1), PartPose.offset(1.5f, 0.5f, -0.5f));

        // ── 형체 (u24 v0 ~) ──
        PartDefinition figure = root.addOrReplaceChild("figure", CubeListBuilder.create(),
                PartPose.offset(0, 24, 0));
        // 긴 검은 코트 6x14x4 (u24 v0)
        figure.addOrReplaceChild("figure_body", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-3, -14, -2, 6, 14, 4), PartPose.offset(0, 0, 0));
        // 큰 초승달 머리 8x8x2 (u24 v20)
        figure.addOrReplaceChild("figure_head", CubeListBuilder.create().texOffs(24, 20)
                .addBox(-4, -8, -1, 8, 8, 2), PartPose.offset(0, -14, 0));
        // 긴 팔 2x12x2 (u24 v32)
        figure.addOrReplaceChild("figure_arm_left", CubeListBuilder.create().texOffs(24, 32)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(-3, -13, 0));
        figure.addOrReplaceChild("figure_arm_right", CubeListBuilder.create().texOffs(24, 32)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(3, -13, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        piano.visible = !s.breached;
        figure.visible = s.breached;

        if (s.breached) {
            // 형체: 팔이 천천히 흔들리고 머리가 흔들린다
            figureArmLeft.xRot = Mth.sin(t * 0.12f) * 0.35f - 0.15f;
            figureArmRight.xRot = -Mth.sin(t * 0.12f) * 0.35f - 0.15f;
            figureArmLeft.zRot = 0.12f + Mth.sin(t * 0.09f) * 0.1f;
            figureArmRight.zRot = -0.12f - Mth.sin(t * 0.09f) * 0.1f;
            figureHead.yRot = Mth.sin(t * 0.05f) * 0.25f;
            figureBody.yRot = Mth.sin(t * 0.04f) * 0.08f;
            figureBody.y = Mth.sin(t * 0.07f) * 0.4f;
            return;
        }
        // 피아노: 뚜껑이 아주 천천히 숨쉬듯 열렸다 닫힌다
        pianoLid.xRot = -0.35f + Mth.sin(t * 0.05f) * 0.06f;
        pianoLid.y = -5 - Mth.sin(t * 0.05f) * 0.15f;
        moonFace.yRot = Mth.sin(t * 0.03f) * 0.12f;
        moonFaceTear.y = 0.5f + Mth.sin(t * 0.08f) * 0.3f;
    }
}
