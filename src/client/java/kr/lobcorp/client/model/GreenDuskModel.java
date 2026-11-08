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

/** 녹색 황혼. 움직이지 않는 거대한 공장 탑, 옆으로 뻗은 기계 팔. */
public class GreenDuskModel extends EntityModel<GreenDuskModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("green_dusk"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart base, body, chimney, rightArm, leftArm;

    public GreenDuskModel(ModelPart root) {
        super(root);
        base = root.getChild("base");
        body = root.getChild("body");
        chimney = root.getChild("chimney");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 넓은 받침 (u0 v0, 24x8x24)
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-12, 0, -12, 24, 8, 24), PartPose.offset(0, 16, 0));
        // 탑 몸통 (u0 v34, 16x24x16)
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 34)
                .addBox(-8, -24, -8, 16, 24, 16), PartPose.offset(0, 16, 0));
        // 굴뚝 (u0 v76, 6x10x6)
        root.addOrReplaceChild("chimney", CubeListBuilder.create().texOffs(0, 76)
                .addBox(-3, -10, -3, 6, 10, 6), PartPose.offset(0, -8, 0));
        // 양옆 기계 팔 (u64 v34, 4x16x4)
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(64, 34)
                .addBox(-2, 0, -2, 4, 16, 4), PartPose.offset(-10, 0, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(64, 34)
                .addBox(-2, 0, -2, 4, 16, 4), PartPose.offset(10, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 기계 팔은 느리게 회전한다
        rightArm.xRot = Mth.sin(t * 0.05f) * 0.5f;
        leftArm.xRot = Mth.sin(t * 0.05f + 3.1416f) * 0.5f;
        rightArm.zRot = Mth.cos(t * 0.04f) * 0.2f;
        leftArm.zRot = -Mth.cos(t * 0.04f) * 0.2f;
        // 굴뚝은 살짝 흔들린다
        chimney.zRot = Mth.sin(t * 0.03f) * 0.05f;
    }
}
