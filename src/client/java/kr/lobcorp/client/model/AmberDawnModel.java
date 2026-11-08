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

/** 호박색 여명. 마디진 작은 벌레가 몸을 꿈틀거린다. */
public class AmberDawnModel extends EntityModel<AmberDawnModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("amber_dawn"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart head, seg1, seg2, seg3;

    public AmberDawnModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        seg1 = root.getChild("seg1");
        seg2 = root.getChild("seg2");
        seg3 = root.getChild("seg3");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 둥근 입 테두리가 달린 머리
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2, -2, -4, 4, 4, 4)
                .texOffs(0, 10).addBox(-1, -1, -5, 2, 2, 1), PartPose.offset(0, 21, -6));
        // 몸통 마디 3개
        root.addOrReplaceChild("seg1", CubeListBuilder.create().texOffs(0, 14)
                .addBox(-2, -2, 0, 4, 4, 4), PartPose.offset(0, 21, -2));
        root.addOrReplaceChild("seg2", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-1.5f, -1.5f, 0, 3, 3, 3), PartPose.offset(0, 21, 2));
        root.addOrReplaceChild("seg3", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-1.5f, -1.5f, 0, 3, 3, 3), PartPose.offset(0, 21, 5));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 꿈틀거림: 마디마다 위상이 늦게 따라온다
        head.yRot = Mth.sin(t * 0.25f) * 0.25f;
        head.xRot = Mth.sin(t * 0.12f) * 0.12f;
        seg1.yRot = Mth.sin(t * 0.25f - 0.6f) * 0.3f;
        seg2.yRot = Mth.sin(t * 0.25f - 1.2f) * 0.35f;
        seg3.yRot = Mth.sin(t * 0.25f - 1.8f) * 0.4f;
        seg1.y = 21 + Mth.sin(t * 0.25f) * 0.4f;
        seg2.y = 21 + Mth.sin(t * 0.25f - 0.6f) * 0.4f;
        seg3.y = 21 + Mth.sin(t * 0.25f - 1.2f) * 0.4f;
    }
}
