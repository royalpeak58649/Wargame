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

/** 보라색 여명. 공중에 떠 있는 과일, 아래로 뿌리가 늘어져 있다. */
public class VioletDawnModel extends EntityModel<VioletDawnModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("violet_dawn"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart body, body2, body3, stem, root1, root2, root3, root4;

    public VioletDawnModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        body2 = root.getChild("body2");
        body3 = root.getChild("body3");
        stem = root.getChild("stem");
        root1 = root.getChild("root1");
        root2 = root.getChild("root2");
        root3 = root.getChild("root3");
        root4 = root.getChild("root4");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 둥근 과일 몸통: 겹친 상자 3개
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -4, -4, 8, 8, 8), PartPose.offset(0, 12, 0));
        root.addOrReplaceChild("body2", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(0, 12, 0));
        root.addOrReplaceChild("body3", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-2, -2, -2, 4, 4, 4), PartPose.offset(0, 12, 0));
        // 꼭지
        root.addOrReplaceChild("stem", CubeListBuilder.create().texOffs(24, 16)
                .addBox(-1, -3, -1, 2, 3, 2), PartPose.offset(0, 8, 0));
        // 아래로 늘어진 뿌리 네 가닥
        root.addOrReplaceChild("root1", CubeListBuilder.create().texOffs(24, 22)
                .addBox(-0.5f, 0, -0.5f, 1, 6, 1), PartPose.offset(-2, 16, -2));
        root.addOrReplaceChild("root2", CubeListBuilder.create().texOffs(24, 22)
                .addBox(-0.5f, 0, -0.5f, 1, 6, 1), PartPose.offset(2, 16, -2));
        root.addOrReplaceChild("root3", CubeListBuilder.create().texOffs(24, 22)
                .addBox(-0.5f, 0, -0.5f, 1, 6, 1), PartPose.offset(-2, 16, 2));
        root.addOrReplaceChild("root4", CubeListBuilder.create().texOffs(24, 22)
                .addBox(-0.5f, 0, -0.5f, 1, 6, 1), PartPose.offset(2, 16, 2));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 몸 전체가 둥실둥실
        float bob = Mth.sin(t * 0.1f) * 1.5f;
        body.y = 12 + bob;
        body2.y = 12 + bob;
        body3.y = 12 + bob;
        stem.y = 8 + bob;
        body.yRot = Mth.sin(t * 0.05f) * 0.2f;
        body2.yRot = body.yRot;
        body3.yRot = body.yRot;
        // 뿌리는 흔들흔들
        root1.xRot = Mth.sin(t * 0.15f) * 0.3f;
        root1.zRot = Mth.cos(t * 0.15f) * 0.3f;
        root2.xRot = Mth.sin(t * 0.15f + 1.0f) * 0.3f;
        root2.zRot = Mth.cos(t * 0.15f + 1.0f) * 0.3f;
        root3.xRot = Mth.sin(t * 0.15f + 2.0f) * 0.3f;
        root3.zRot = Mth.cos(t * 0.15f + 2.0f) * 0.3f;
        root4.xRot = Mth.sin(t * 0.15f + 3.0f) * 0.3f;
        root4.zRot = Mth.cos(t * 0.15f + 3.0f) * 0.3f;
    }
}
