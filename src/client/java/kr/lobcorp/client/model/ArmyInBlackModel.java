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

/** 검은 군단. 나란히 서서 행진하는 장난감 병정 세 명. */
public class ArmyInBlackModel extends EntityModel<ArmyInBlackModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("army_in_black"), "main");

    public static class State extends LivingEntityRenderState {
    }

    private final ModelPart[] soldiers = new ModelPart[3];
    private final ModelPart[] legs = new ModelPart[3];
    private final ModelPart[] hats = new ModelPart[3];

    public ArmyInBlackModel(ModelPart root) {
        super(root);
        for (int i = 0; i < 3; i++) {
            soldiers[i] = root.getChild("soldier" + i);
            legs[i] = soldiers[i].getChild("legs");
            hats[i] = soldiers[i].getChild("hat");
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 세 명이 나란히 선다 (가운데가 살짝 앞)
        float[] xs = {-6f, 0f, 6f};
        float[] zs = {0f, -1.5f, 0f};
        for (int i = 0; i < 3; i++) {
            PartDefinition s = root.addOrReplaceChild("soldier" + i, CubeListBuilder.create(),
                    PartPose.offset(xs[i], 24, zs[i]));

            // 다리 (u0 v0, 2x4x2)
            s.addOrReplaceChild("legs", CubeListBuilder.create()
                    .texOffs(0, 0).addBox(-2, -4, -1, 2, 4, 2)
                    .texOffs(8, 0).addBox(0, -4, -1, 2, 4, 2),
                    PartPose.offset(0, 0, 0));

            // 몸통 (u0 v8, 4x6x3) + 가슴의 분홍 하트
            s.addOrReplaceChild("body", CubeListBuilder.create()
                    .texOffs(0, 8).addBox(-2, -10, -1.5f, 4, 6, 3),
                    PartPose.offset(0, 0, 0));

            // 머리 (u0 v20, 4x4x4)
            s.addOrReplaceChild("head", CubeListBuilder.create()
                    .texOffs(0, 20).addBox(-2, -14, -2, 4, 4, 4),
                    PartPose.offset(0, 0, 0));

            // 높은 모자 (u0 v32, 4x4x4)
            s.addOrReplaceChild("hat", CubeListBuilder.create()
                    .texOffs(0, 32).addBox(-2, -18, -2, 4, 4, 4),
                    PartPose.offset(0, 0, 0));
        }

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float swing = Mth.cos(s.walkAnimationPos * 0.6662f) * 0.6f * s.walkAnimationSpeed;
        for (int i = 0; i < 3; i++) {
            float phase = i * 1.1f;
            // 행진하는 듯한 위아래 흔들림
            soldiers[i].y = 24 - Math.abs(Mth.sin(t * 0.15f + phase)) * 0.8f;
            soldiers[i].yRot = Mth.sin(t * 0.05f + phase) * 0.12f;
            legs[i].xRot = swing * (i % 2 == 0 ? 1f : -1f);
            hats[i].zRot = Mth.sin(t * 0.08f + phase) * 0.06f;
        }
    }
}
