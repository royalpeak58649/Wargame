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

/** 흔들의자에 앉아 하염없이 흔들리는 늙은 여인. 고독 상태에서는 흔들림이 멈추고 고개가 떨어진다. */
public class OldLadyModel extends EntityModel<OldLadyModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("old_lady"), "main");

    public static class State extends LivingEntityRenderState {
        public boolean solitude;
    }

    private final ModelPart rocker, head;

    public OldLadyModel(ModelPart root) {
        super(root);
        rocker = root.getChild("rocker");
        head = rocker.getChild("head");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rocker = root.addOrReplaceChild("rocker", CubeListBuilder.create(), PartPose.offset(0, 24, 0));

        // 의자: 좌판 / 등받이 / 다리 / 휘어진 발판
        rocker.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -9, -4, 8, 1, 8), PartPose.ZERO);
        rocker.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 9)
                .addBox(-4, -21, 3, 8, 12, 1), PartPose.ZERO);
        rocker.addOrReplaceChild("leg_front", CubeListBuilder.create().texOffs(18, 9)
                .addBox(-3.5f, -8, -3.5f, 1, 7, 1)
                .addBox(2.5f, -8, -3.5f, 1, 7, 1), PartPose.ZERO);
        rocker.addOrReplaceChild("leg_back", CubeListBuilder.create().texOffs(22, 9)
                .addBox(-3.5f, -8, 2.5f, 1, 7, 1)
                .addBox(2.5f, -8, 2.5f, 1, 7, 1), PartPose.ZERO);
        rocker.addOrReplaceChild("runner_right", CubeListBuilder.create().texOffs(26, 9)
                .addBox(-0.5f, -1, -5, 1, 2, 10), PartPose.offset(-3, 0, 0));
        rocker.addOrReplaceChild("runner_left", CubeListBuilder.create().texOffs(26, 9)
                .addBox(-0.5f, -1, -5, 1, 2, 10), PartPose.offset(3, 0, 0));

        // 여인: 머리 + 보닛 / 몸통 / 허벅지 / 정강이 / 팔
        PartDefinition head = rocker.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-3, -6, -3, 6, 6, 6), PartPose.offset(0, -15, 0));
        head.addOrReplaceChild("bonnet", CubeListBuilder.create().texOffs(24, 24)
                .addBox(-3.5f, -3, -3.5f, 7, 3, 7), PartPose.offset(0, 0, 0));
        rocker.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 36)
                .addBox(-3, -9, -2, 6, 7, 4), PartPose.offset(0, -2, 0));
        rocker.addOrReplaceChild("thigh", CubeListBuilder.create().texOffs(20, 36)
                .addBox(-3, -1, -5, 6, 2, 5), PartPose.offset(0, -2, 0));
        rocker.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(36, 36)
                .addBox(-2.5f, 0, -1, 2, 6, 2)
                .addBox(0.5f, 0, -1, 2, 6, 2), PartPose.offset(0, -1, -4));
        rocker.addOrReplaceChild("arms", CubeListBuilder.create().texOffs(44, 36)
                .addBox(-4, -8, -3, 1, 6, 2)
                .addBox(3, -8, -3, 1, 6, 2), PartPose.offset(0, -2, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        if (s.solitude) {
            rocker.xRot = 0f;
            head.xRot = 0.55f;
            head.zRot = 0.12f;
        } else {
            rocker.xRot = Mth.sin(t * 0.08f) * 0.08f;
            head.xRot = 0.15f + Mth.sin(t * 0.04f) * 0.02f;
            head.zRot = 0f;
        }
    }
}
