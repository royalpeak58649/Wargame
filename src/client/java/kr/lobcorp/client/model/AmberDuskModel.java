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

/** 호박색 황혼. 땅에 납작 엎드린 거대한 벌레, 앞머리에 넓은 아가리. */
public class AmberDuskModel extends EntityModel<AmberDuskModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("amber_dusk"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart head, jaw, seg1, seg2, seg3;

    public AmberDuskModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        jaw = root.getChild("jaw");
        seg1 = root.getChild("seg1");
        seg2 = root.getChild("seg2");
        seg3 = root.getChild("seg3");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 둥근 머리 마디 (u0 v0, 20x14x12) + 등 가시 (u0 v26, 4x4x4)
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-10, -7, -12, 20, 14, 12)
                .texOffs(0, 26).addBox(-2, -11, -8, 4, 4, 4), PartPose.offset(0, 17, -12));
        // 넓은 아가리 (u64 v0, 18x4x10) + 이빨 (u64 v14, 16x2x1)
        root.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-9, 0, -10, 18, 4, 10)
                .texOffs(64, 14).addBox(-8, -2, -10, 16, 2, 1), PartPose.offset(0, 17, -12));
        // 몸통 마디 3개 (u0 v34 / u0 v60 / u0 v86, 20x14x12)
        root.addOrReplaceChild("seg1", CubeListBuilder.create().texOffs(0, 34)
                .addBox(-10, -7, 0, 20, 14, 12)
                .texOffs(0, 60).addBox(-2, -11, 4, 4, 4, 4), PartPose.offset(0, 17, -2));
        root.addOrReplaceChild("seg2", CubeListBuilder.create().texOffs(0, 60)
                .addBox(-10, -7, 0, 20, 14, 12)
                .texOffs(0, 86).addBox(-2, -11, 4, 4, 4, 4), PartPose.offset(0, 17, 10));
        root.addOrReplaceChild("seg3", CubeListBuilder.create().texOffs(0, 86)
                .addBox(-10, -7, 0, 20, 14, 12)
                .texOffs(0, 112).addBox(-2, -11, 4, 4, 4, 4), PartPose.offset(0, 17, 22));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 마디마다 위상을 늦춰 좌우로 꿈틀거린다
        head.yRot = Mth.sin(t * 0.2f) * 0.18f;
        seg1.yRot = Mth.sin(t * 0.2f + 1.0f) * 0.22f;
        seg2.yRot = Mth.sin(t * 0.2f + 2.0f) * 0.26f;
        seg3.yRot = Mth.sin(t * 0.2f + 3.0f) * 0.3f;
        // 아가리는 천천히 벌렸다 닫힌다
        jaw.xRot = 0.15f + Mth.sin(t * 0.1f) * 0.15f;
    }
}
