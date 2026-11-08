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

/** 종말새의 알. 종류에 따라 부리 / 팔 / 눈이 드러난다. */
public class ApocalypseEggModel extends EntityModel<ApocalypseEggModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("apocalypse_egg"), "main");

    public static class State extends LivingEntityRenderState {
        public int kind;
    }

    private final ModelPart egg, beak, armL, armR;
    private final ModelPart[] eyes = new ModelPart[4];

    public ApocalypseEggModel(ModelPart root) {
        super(root);
        egg = root.getChild("egg");
        beak = egg.getChild("beak");
        armL = egg.getChild("arm_l");
        armR = egg.getChild("arm_r");
        for (int i = 0; i < 4; i++) {
            eyes[i] = egg.getChild("eye" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 알 (u0 v0, 12x16x12)
        PartDefinition egg = root.addOrReplaceChild("egg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6, -16, -6, 12, 16, 12),
                PartPose.offset(0, 24, 0));

        // 부리 (u0 v32, 4x3x4) — kind 0
        egg.addOrReplaceChild("beak", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-2, -1.5f, -6, 4, 3, 4),
                PartPose.offset(0, -10, 0));

        // 긴 팔 두 개 (u0 v44 / u8 v44, 2x12x2) — kind 1
        egg.addOrReplaceChild("arm_l", CubeListBuilder.create()
                .texOffs(0, 44).addBox(0, -6, -1, 2, 12, 2),
                PartPose.offset(6, -8, 0));
        egg.addOrReplaceChild("arm_r", CubeListBuilder.create()
                .texOffs(8, 44).addBox(-2, -6, -1, 2, 12, 2),
                PartPose.offset(-6, -8, 0));

        // 눈 여러 개 (u0 v60 / u4 v60 / u8 v60 / u12 v60, 2x2x1) — kind 2
        float[][] eyePos = {{-3, -12, -6}, {1, -12, -6}, {-3, -8, -6}, {1, -8, -6}};
        for (int i = 0; i < 4; i++) {
            egg.addOrReplaceChild("eye" + i, CubeListBuilder.create()
                    .texOffs(i * 4, 60).addBox(-1, -1, -0.5f, 2, 2, 1),
                    PartPose.offset(eyePos[i][0], eyePos[i][1], eyePos[i][2]));
        }

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        beak.visible = s.kind == 0;
        armL.visible = s.kind == 1;
        armR.visible = s.kind == 1;
        for (ModelPart eye : eyes) {
            eye.visible = s.kind == 2;
        }
        // 느린 맥동
        float pulse = 1f + Mth.sin(t * 0.06f) * 0.05f;
        egg.xScale = pulse;
        egg.zScale = pulse;
        egg.yScale = 2f - pulse;
        egg.y = 24 - Math.abs(Mth.sin(t * 0.06f)) * 0.8f;
        egg.yRot = Mth.sin(t * 0.02f) * 0.15f;
        beak.xRot = Mth.sin(t * 0.1f) * 0.1f;
        armL.zRot = -0.2f + Mth.sin(t * 0.08f) * 0.15f;
        armR.zRot = 0.2f - Mth.sin(t * 0.08f) * 0.15f;
    }
}
