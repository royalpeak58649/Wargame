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

/** 얼음 왕좌에 앉은 눈의 여왕. 서리가 내려앉은 왕관과 긴 드레스. */
public class SnowQueenModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("snow_queen"), "main");

    private final ModelPart head, rightArm, leftArm, crown;

    public SnowQueenModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        crown = head.getChild("crown");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 얼음 왕좌
        root.addOrReplaceChild("throne_seat", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5, 0, -4, 10, 2, 8), PartPose.offset(0, 22, 0));
        root.addOrReplaceChild("throne_back", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-5, -14, -1, 10, 14, 2), PartPose.offset(0, 22, -4));
        root.addOrReplaceChild("throne_arm_right", CubeListBuilder.create().texOffs(24, 10)
                .addBox(-1, -3, -4, 1, 3, 8), PartPose.offset(-5, 22, 0));
        root.addOrReplaceChild("throne_arm_left", CubeListBuilder.create().texOffs(24, 10)
                .addBox(0, -3, -4, 1, 3, 8), PartPose.offset(5, 22, 0));

        // 긴 드레스
        root.addOrReplaceChild("gown", CubeListBuilder.create().texOffs(0, 26)
                .addBox(-4, 0, -3, 8, 10, 6), PartPose.offset(0, 12, 0));
        // 몸통
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 26)
                .addBox(-3, 0, -1.5f, 6, 6, 3), PartPose.offset(0, 6, 0));
        // 팔
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(46, 26)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-4, 7, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(46, 26)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(4, 7, 0));

        // 머리
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 42)
                .addBox(-2.5f, -5, -2.5f, 5, 5, 5), PartPose.offset(0, 6, 0));
        // 고드름 왕관: 다섯 개의 뾰족한 조각
        PartDefinition crown = head.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(20, 42).addBox(-2.5f, -3, -2.5f, 1, 3, 1)
                .texOffs(20, 42).addBox(-1.5f, -3, -2.5f, 1, 3, 1)
                .texOffs(20, 42).addBox(-0.5f, -4, -2.5f, 1, 4, 1)
                .texOffs(20, 42).addBox(0.5f, -3, -2.5f, 1, 3, 1)
                .texOffs(20, 42).addBox(1.5f, -3, -2.5f, 1, 3, 1), PartPose.ZERO);
        crown.addOrReplaceChild("crown_band", CubeListBuilder.create().texOffs(28, 42)
                .addBox(-2.5f, -1, -2.5f, 5, 1, 5), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        head.xRot = 0.05f + Mth.sin(t * 0.02f) * 0.02f;
        head.yRot = Mth.sin(t * 0.01f) * 0.08f;
        rightArm.xRot = -0.15f;
        leftArm.xRot = -0.15f;
        rightArm.zRot = 0.08f;
        leftArm.zRot = -0.08f;
        crown.yRot = Mth.sin(t * 0.015f) * 0.05f;
    }
}
