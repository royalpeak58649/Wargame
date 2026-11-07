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

/** 별이 박힌 커다란 망토를 뒤집어쓴 작은 아이. 두 손에 빛나는 조약돌을 쥐고 있다. */
public class ChildGalaxyModel extends EntityModel<ChildGalaxyModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("child_galaxy"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public int counter = 1;
    }

    private final ModelPart cloak, hood, rightArm, leftArm, pebbles;

    public ChildGalaxyModel(ModelPart root) {
        super(root);
        cloak = root.getChild("cloak");
        hood = root.getChild("hood");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        pebbles = root.getChild("pebbles");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 다리
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1, 0, -1, 2, 4, 2), PartPose.offset(-1.2f, 20, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1, 0, -1, 2, 4, 2), PartPose.offset(1.2f, 20, 0));
        // 몸통
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 8)
                .addBox(-2.5f, 0, -1.5f, 5, 5, 3), PartPose.offset(0, 15, 0));
        // 커다란 망토
        PartDefinition cloak = root.addOrReplaceChild("cloak", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-4, 0, -2.5f, 8, 9, 5), PartPose.offset(0, 11, 0));
        cloak.addOrReplaceChild("hem", CubeListBuilder.create().texOffs(26, 16)
                .addBox(-4.5f, 0, -3, 9, 2, 6), PartPose.offset(0, 9, 0));
        // 두건
        PartDefinition hood = root.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(0, 30)
                .addBox(-3.5f, -6, -3, 7, 6, 6), PartPose.offset(0, 11, 0));
        hood.addOrReplaceChild("face", CubeListBuilder.create().texOffs(26, 30)
                .addBox(-2, -4, -3.6f, 4, 4, 1), PartPose.ZERO);
        // 팔
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 30)
                .addBox(-1, 0, -1, 2, 4, 2), PartPose.offset(-3.5f, 13, -1));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(36, 30)
                .addBox(-1, 0, -1, 2, 4, 2), PartPose.offset(3.5f, 13, -1));
        // 손에 쥔 조약돌 세 개
        PartDefinition pebbles = root.addOrReplaceChild("pebbles", CubeListBuilder.create(), PartPose.offset(0, 17, -2.5f));
        pebbles.addOrReplaceChild("pebble0", CubeListBuilder.create().texOffs(44, 30)
                .addBox(-0.5f, -0.5f, -0.5f, 1, 1, 1), PartPose.offset(-1.2f, 0, 0));
        pebbles.addOrReplaceChild("pebble1", CubeListBuilder.create().texOffs(44, 30)
                .addBox(-0.5f, -0.5f, -0.5f, 1, 1, 1), PartPose.offset(0, -0.4f, -0.4f));
        pebbles.addOrReplaceChild("pebble2", CubeListBuilder.create().texOffs(44, 30)
                .addBox(-0.5f, -0.5f, -0.5f, 1, 1, 1), PartPose.offset(1.2f, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 아이는 조용히 서서 조약돌을 내려다본다
        hood.xRot = 0.18f + Mth.sin(t * 0.03f) * 0.02f;
        hood.yRot = Mth.sin(t * 0.015f) * 0.08f;
        rightArm.xRot = -0.9f;
        leftArm.xRot = -0.9f;
        rightArm.yRot = 0.35f;
        leftArm.yRot = -0.35f;
        cloak.xRot = Mth.sin(t * 0.02f) * 0.02f;
        // 카운터가 높을수록 조약돌이 더 밝게 떠오른다
        float lift = Math.min(1f, s.counter / 5f);
        pebbles.y = 17f - lift * 0.6f + Mth.sin(t * 0.08f) * 0.15f;
        pebbles.yRot = t * 0.05f;
    }
}
