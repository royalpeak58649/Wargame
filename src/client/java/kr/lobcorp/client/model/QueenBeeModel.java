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

/** 밀랍과 살로 빚은 벌집 위에 앉은 거대한 여왕벌. 날개를 느리게 펄럭인다. */
public class QueenBeeModel extends EntityModel<QueenBeeModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("queen_bee"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart abdomen, head, rightWing, leftWing, rightAntenna, leftAntenna;

    public QueenBeeModel(ModelPart root) {
        super(root);
        abdomen = root.getChild("abdomen");
        head = root.getChild("head");
        rightWing = root.getChild("right_wing");
        leftWing = root.getChild("left_wing");
        rightAntenna = root.getChild("right_antenna");
        leftAntenna = root.getChild("left_antenna");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 벌집 받침: 바닥에 깔린 밀랍 덩어리
        root.addOrReplaceChild("hive", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-8, 0, -8, 16, 6, 16), PartPose.offset(0, 18, 0));
        // 부푼 배: 줄무늬
        root.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-5, -4, -6, 10, 8, 12), PartPose.offset(0, 12, 4));
        // 가슴
        root.addOrReplaceChild("thorax", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(0, 12, -4));
        // 머리: 큰 붉은 눈
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 24)
                .addBox(-2.5f, -2.5f, -2.5f, 5, 5, 5), PartPose.offset(0, 12, -9));
        head.addOrReplaceChild("eyes", CubeListBuilder.create()
                .texOffs(48, 24).addBox(-2.5f, -1.5f, -2.6f, 2, 3, 1)
                .texOffs(48, 24).addBox(0.5f, -1.5f, -2.6f, 2, 3, 1), PartPose.ZERO);
        // 왕관처럼 솟은 더듬이 네 개
        root.addOrReplaceChild("right_antenna", CubeListBuilder.create().texOffs(56, 24)
                .addBox(-0.5f, -5, -0.5f, 1, 5, 1), PartPose.offset(-1.5f, 10, -9));
        root.addOrReplaceChild("left_antenna", CubeListBuilder.create().texOffs(56, 24)
                .addBox(-0.5f, -5, -0.5f, 1, 5, 1), PartPose.offset(1.5f, 10, -9));
        root.addOrReplaceChild("right_antenna2", CubeListBuilder.create().texOffs(56, 24)
                .addBox(-0.5f, -4, -0.5f, 1, 4, 1), PartPose.offsetAndRotation(-2.5f, 10, -9, 0, 0, 0.35f));
        root.addOrReplaceChild("left_antenna2", CubeListBuilder.create().texOffs(56, 24)
                .addBox(-0.5f, -4, -0.5f, 1, 4, 1), PartPose.offsetAndRotation(2.5f, 10, -9, 0, 0, -0.35f));
        // 반투명한 날개
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 56)
                .addBox(-8, 0, 0, 8, 1, 12), PartPose.offset(-2, 9, -4));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 56)
                .addBox(0, 0, 0, 8, 1, 12), PartPose.offset(2, 9, -4));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float flap = Mth.sin(t * 0.25f) * 0.35f;
        rightWing.zRot = -0.25f - flap;
        leftWing.zRot = 0.25f + flap;
        rightWing.yRot = -0.15f;
        leftWing.yRot = 0.15f;
        abdomen.xRot = 0.12f + Mth.sin(t * 0.05f) * 0.03f;
        head.yRot = Mth.sin(t * 0.03f) * 0.15f;
        rightAntenna.zRot = 0.2f + Mth.sin(t * 0.08f) * 0.1f;
        leftAntenna.zRot = -0.2f - Mth.sin(t * 0.08f) * 0.1f;
    }
}
