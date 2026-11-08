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

/** 검열됨의 하수인. 작은 인간형, 머리는 검은 막대로 덮여 있다. */
public class CensoredMinionModel extends EntityModel<CensoredMinionModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("censored_minion"), "main");

    public static class State extends LivingEntityRenderState {
    }

    private final ModelPart body, head, armL, armR, legL, legR;

    public CensoredMinionModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        armL = body.getChild("arm_l");
        armR = body.getChild("arm_r");
        legL = root.getChild("leg_l");
        legR = root.getChild("leg_r");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 몸통 (u0 v0, 8x12x4)
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -12, -2, 8, 12, 4),
                PartPose.offset(0, 12, 0));

        // 머리 (u0 v16, 8x8x8)
        body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4, -8, -4, 8, 8, 8),
                PartPose.offset(0, -12, 0));

        // 팔 (u0 v32 / u8 v32, 3x10x3)
        body.addOrReplaceChild("arm_l", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-1.5f, 0, -1.5f, 3, 10, 3),
                PartPose.offset(5.5f, -11, 0));
        body.addOrReplaceChild("arm_r", CubeListBuilder.create()
                .texOffs(8, 32).addBox(-1.5f, 0, -1.5f, 3, 10, 3),
                PartPose.offset(-5.5f, -11, 0));

        // 다리 (u0 v48 / u8 v48, 3x12x3)
        root.addOrReplaceChild("leg_l", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-1.5f, 0, -1.5f, 3, 12, 3),
                PartPose.offset(2, 12, 0));
        root.addOrReplaceChild("leg_r", CubeListBuilder.create()
                .texOffs(8, 48).addBox(-1.5f, 0, -1.5f, 3, 12, 3),
                PartPose.offset(-2, 12, 0));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float swing = Mth.cos(s.walkAnimationPos * 0.6662f) * 0.8f * s.walkAnimationSpeed;
        legL.xRot = swing;
        legR.xRot = -swing;
        armL.xRot = -swing * 0.7f;
        armR.xRot = swing * 0.7f;
        armL.zRot = 0.08f;
        armR.zRot = -0.08f;
        body.xRot = 0.05f + Mth.sin(t * 0.07f) * 0.03f;
        head.xRot = -0.05f + Mth.sin(t * 0.06f) * 0.06f;
        head.yRot = Mth.sin(t * 0.04f) * 0.2f;
    }
}
