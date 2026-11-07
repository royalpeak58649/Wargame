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

/** 프릴 달린 드레스에 뾰족한 모자를 쓴, 선물 상자를 안고 웃는 작은 소녀. */
public class LaetitiaModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("laetitia"), "main");

    private final ModelPart head, hat, rightArm, leftArm, present, dress;

    public LaetitiaModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        hat = root.getChild("hat");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        present = root.getChild("present");
        dress = root.getChild("dress");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1, 0, -1, 2, 5, 2), PartPose.offset(-1.5f, 19, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1, 0, -1, 2, 5, 2), PartPose.offset(1.5f, 19, 0));
        root.addOrReplaceChild("dress", CubeListBuilder.create().texOffs(0, 8)
                .addBox(-4, 0, -3, 8, 7, 6), PartPose.offset(0, 12, 0));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 8)
                .addBox(-3, 0, -2, 6, 5, 4), PartPose.offset(0, 7, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 22)
                .addBox(-1, 0, -1, 2, 5, 2), PartPose.offset(-3.5f, 8, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 22)
                .addBox(-1, 0, -1, 2, 5, 2), PartPose.offset(3.5f, 8, 0));
        root.addOrReplaceChild("present", CubeListBuilder.create().texOffs(12, 22)
                .addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.offset(0, 12, -4));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-3, -6, -3, 6, 6, 6), PartPose.offset(0, 7, 0));
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(24, 32)
                .addBox(-4, -1, -4, 8, 1, 8), PartPose.offset(0, 1, 0));
        hat.addOrReplaceChild("cone", CubeListBuilder.create().texOffs(24, 42)
                .addBox(-2, -5, -2, 4, 5, 4), PartPose.offset(0, 0, 0));
        hat.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(40, 42)
                .addBox(-1, -2, -1, 2, 2, 2), PartPose.offset(0, -5, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 조용한 흔들림
        head.yRot = Mth.sin(t * 0.05f) * 0.12f;
        head.zRot = Mth.sin(t * 0.035f) * 0.06f;
        hat.yRot = head.yRot;
        hat.zRot = head.zRot;
        dress.zRot = Mth.sin(t * 0.03f) * 0.03f;
        rightArm.xRot = -0.9f;
        leftArm.xRot = -0.9f;
        rightArm.zRot = 0.25f;
        leftArm.zRot = -0.25f;
        present.yRot = Mth.sin(t * 0.04f) * 0.2f;
    }
}
