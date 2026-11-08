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

/** 아무것도 없는. 붉은 살점 인간형 → 알 → 완전체(팔 칼날 + 어깨 가시). */
public class NothingThereModel extends EntityModel<NothingThereModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("nothing_there"), "main");

    public static class State extends LivingEntityRenderState {
        public int phase; // 0 격리, 1 1형태, 2 알, 3 완전체
    }

    private final ModelPart body, head, mouth, armL, armR, legL, legR, egg, blade, shoulderSpikes;

    public NothingThereModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        mouth = head.getChild("mouth");
        armL = body.getChild("arm_l");
        armR = body.getChild("arm_r");
        legL = root.getChild("leg_l");
        legR = root.getChild("leg_r");
        egg = root.getChild("egg");
        blade = armR.getChild("blade");
        shoulderSpikes = body.getChild("shoulder_spikes");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 몸통 (u0 v0, 10x14x6)
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5, -14, -3, 10, 14, 6),
                PartPose.offset(0, 10, 0));

        // 머리 (u0 v20, 8x8x8) + 큰 아가리 (u32 v20, 6x4x2)
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-4, -8, -4, 8, 8, 8),
                PartPose.offset(0, -14, 0));
        head.addOrReplaceChild("mouth", CubeListBuilder.create()
                .texOffs(32, 20).addBox(-3, -2, -1, 6, 4, 2),
                PartPose.offset(0, 2, -4));

        // 긴 팔 (u0 v36 / u16 v36, 4x18x4)
        body.addOrReplaceChild("arm_l", CubeListBuilder.create()
                .texOffs(0, 36).addBox(-2, 0, -2, 4, 18, 4),
                PartPose.offset(7, -13, 0));
        PartDefinition armR = body.addOrReplaceChild("arm_r", CubeListBuilder.create()
                .texOffs(16, 36).addBox(-2, 0, -2, 4, 18, 4),
                PartPose.offset(-7, -13, 0));
        // 팔 칼날 (u32 v36, 2x14x2) — 완전체에서만
        armR.addOrReplaceChild("blade", CubeListBuilder.create()
                .texOffs(32, 36).addBox(-1, 0, -1, 2, 14, 2),
                PartPose.offset(0, 18, 0));
        // 어깨 가시 (u40 v36, 8x4x4) — 완전체에서만
        body.addOrReplaceChild("shoulder_spikes", CubeListBuilder.create()
                .texOffs(40, 36).addBox(-4, -4, -2, 8, 4, 4),
                PartPose.offset(0, -14, 0));

        // 다리 (u0 v60 / u12 v60, 4x10x4)
        root.addOrReplaceChild("leg_l", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-2, 0, -2, 4, 10, 4),
                PartPose.offset(3, 14, 0));
        root.addOrReplaceChild("leg_r", CubeListBuilder.create()
                .texOffs(12, 60).addBox(-2, 0, -2, 4, 10, 4),
                PartPose.offset(-3, 14, 0));

        // 알 (u0 v76, 16x16x16) — 2단계에서만
        root.addOrReplaceChild("egg", CubeListBuilder.create()
                .texOffs(0, 76).addBox(-8, -16, -8, 16, 16, 16),
                PartPose.offset(0, 24, 0));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        boolean humanoid = s.phase != 2;
        body.visible = humanoid;
        head.visible = humanoid;
        mouth.visible = humanoid;
        armL.visible = humanoid;
        armR.visible = humanoid;
        legL.visible = humanoid;
        legR.visible = humanoid;
        blade.visible = s.phase == 3;
        shoulderSpikes.visible = s.phase == 3;
        egg.visible = s.phase == 2;

        if (s.phase == 2) {
            // 알이 느리게 부풀었다 줄어든다
            egg.y = 24 + Mth.sin(t * 0.3f) * 0.8f;
            egg.xRot = Mth.sin(t * 0.1f) * 0.05f;
            return;
        }

        float swing = Mth.cos(s.walkAnimationPos * 0.6662f) * 0.7f * s.walkAnimationSpeed;
        legL.xRot = swing;
        legR.xRot = -swing;
        armL.xRot = -swing * 0.6f;
        armR.xRot = swing * 0.6f;
        armL.zRot = 0.1f;
        armR.zRot = -0.1f;
        body.xRot = 0.1f + Mth.sin(t * 0.06f) * 0.04f;
        head.xRot = -0.1f + Mth.sin(t * 0.05f) * 0.08f;
        head.yRot = Mth.sin(t * 0.03f) * 0.3f;
        mouth.xRot = 0.2f + Mth.sin(t * 0.12f) * 0.15f;
        if (s.phase == 3) {
            // 완전체: 칼날을 크게 휘두른다
            armR.xRot = -0.6f + Mth.sin(t * 0.15f) * 0.5f;
            armR.zRot = -0.3f;
            blade.xRot = 0.4f;
            head.xRot = -0.25f;
        }
    }
}
