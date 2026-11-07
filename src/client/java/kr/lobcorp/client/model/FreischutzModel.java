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

/** 어두운 코트에 넓은 챙모자를 눌러쓴 마른 사냥꾼. 한쪽 눈이 붉게 빛난다. */
public class FreischutzModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("freischutz"), "main");

    private final ModelPart head, hat, rightArm, leftArm, musket;

    public FreischutzModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        hat = head.getChild("hat");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        musket = root.getChild("musket");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(-1.5f, 24, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(8, 0)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(1.5f, 24, 0));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-3, 0, -1.5f, 6, 10, 3), PartPose.offset(0, 12, 0));
        root.addOrReplaceChild("coat", CubeListBuilder.create().texOffs(20, 16)
                .addBox(-4, 0, -2, 8, 6, 4), PartPose.offset(0, 22, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-2.5f, -6, -2.5f, 5, 6, 5), PartPose.offset(0, 12, 0));
        head.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(24, 32).addBox(-5, -1, -5, 10, 1, 10)
                .texOffs(24, 44).addBox(-2.5f, -4, -2.5f, 5, 3, 5), PartPose.offset(0, -6, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(44, 0)
                .addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(-4, 13, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(52, 0)
                .addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(4, 13, 0));
        root.addOrReplaceChild("musket", CubeListBuilder.create().texOffs(0, 48)
                .addBox(-0.5f, -8, -0.5f, 1, 16, 1), PartPose.offsetAndRotation(4, 16, -2, -0.9f, 0, 0.15f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        head.yRot = Mth.sin(t * 0.01f) * 0.12f;
        head.xRot = 0.05f;
        hat.yRot = head.yRot;
        rightArm.xRot = -0.15f;
        leftArm.xRot = -0.35f;
        leftArm.yRot = 0.25f;
        musket.xRot = -0.9f + Mth.sin(t * 0.02f) * 0.02f;
    }
}
