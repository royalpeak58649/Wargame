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

/** 웅크린 사람의 몸에서 자라난 어린 나무. 뿌리가 발밑을 붙잡고 있다. */
public class SaplingModel extends EntityModel<SaplingModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("sapling"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart trunk, crown, leaf_right, leaf_left, leaf_back;

    public SaplingModel(ModelPart root) {
        super(root);
        trunk = root.getChild("trunk");
        crown = root.getChild("crown");
        leaf_right = root.getChild("leaf_right");
        leaf_left = root.getChild("leaf_left");
        leaf_back = root.getChild("leaf_back");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 웅크린 몸: 밑동
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, 0, -3, 8, 6, 6), PartPose.offset(0, 18, 0));
        // 웅크린 몸: 굽은 등
        root.addOrReplaceChild("hunch", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-3, -5, -3, 6, 5, 6), PartPose.offset(0, 18, 0));
        // 뿌리: 발밑을 붙잡는다
        root.addOrReplaceChild("root_front", CubeListBuilder.create().texOffs(28, 0)
                .addBox(-1, 0, 0, 2, 1, 5), PartPose.offset(0, 23, 2));
        root.addOrReplaceChild("root_right", CubeListBuilder.create().texOffs(28, 6)
                .addBox(0, 0, -1, 5, 1, 2), PartPose.offset(3, 23, 0));
        root.addOrReplaceChild("root_left", CubeListBuilder.create().texOffs(28, 10)
                .addBox(-5, 0, -1, 5, 1, 2), PartPose.offset(-3, 23, 0));
        // 가는 줄기
        root.addOrReplaceChild("trunk", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-1.5f, -12, -1.5f, 3, 12, 3), PartPose.offset(0, 18, 0));
        // 줄기 끝의 잎
        root.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(12, 24)
                .addBox(-3, -3, -3, 6, 3, 6), PartPose.offset(0, 6, 0));
        root.addOrReplaceChild("leaf_right", CubeListBuilder.create().texOffs(36, 24)
                .addBox(0, -1, -2, 5, 2, 4), PartPose.offset(2, 8, 0));
        root.addOrReplaceChild("leaf_left", CubeListBuilder.create().texOffs(36, 30)
                .addBox(-5, -1, -2, 5, 2, 4), PartPose.offset(-2, 10, 0));
        root.addOrReplaceChild("leaf_back", CubeListBuilder.create().texOffs(36, 36)
                .addBox(-2, -1, 0, 4, 2, 5), PartPose.offset(0, 12, 2));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 바람에 아주 약하게 흔들린다
        trunk.zRot = Mth.sin(t * 0.05f) * 0.06f;
        crown.zRot = trunk.zRot * 1.4f;
        leaf_right.zRot = Mth.sin(t * 0.05f + 0.6f) * 0.1f;
        leaf_left.zRot = -Mth.sin(t * 0.05f + 0.6f) * 0.1f;
        leaf_back.xRot = Mth.sin(t * 0.05f + 1.2f) * 0.08f;
    }
}
