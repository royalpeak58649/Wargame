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

/** 받침대 위에 놓인 붉은 구두 한 켤레. 아주 느리게 흔들린다. */
public class RedShoesModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("red_shoes"), "main");

    private final ModelPart leftShoe, rightShoe;

    public RedShoesModel(ModelPart root) {
        super(root);
        leftShoe = root.getChild("left_shoe");
        rightShoe = root.getChild("right_shoe");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 받침대
        root.addOrReplaceChild("pedestal", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, 0, -4, 8, 2, 8), PartPose.offset(0, 22, 0));
        // 왼쪽 구두: 몸통 + 굽 + 끈
        PartDefinition left = root.addOrReplaceChild("left_shoe", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-1.5f, -2, -3, 3, 2, 6), PartPose.offset(-2, 22, 0));
        left.addOrReplaceChild("left_heel", CubeListBuilder.create().texOffs(18, 10)
                .addBox(-0.5f, 0, 2, 1, 2, 1), PartPose.ZERO);
        left.addOrReplaceChild("left_strap", CubeListBuilder.create().texOffs(22, 10)
                .addBox(-1.5f, -2.5f, -1, 3, 1, 1), PartPose.ZERO);
        // 오른쪽 구두
        PartDefinition right = root.addOrReplaceChild("right_shoe", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-1.5f, -2, -3, 3, 2, 6), PartPose.offset(2, 22, 0));
        right.addOrReplaceChild("right_heel", CubeListBuilder.create().texOffs(18, 10)
                .addBox(-0.5f, 0, 2, 1, 2, 1), PartPose.ZERO);
        right.addOrReplaceChild("right_strap", CubeListBuilder.create().texOffs(22, 10)
                .addBox(-1.5f, -2.5f, -1, 3, 1, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState s) {
        super.setupAnim(s);
        float sway = Mth.sin(s.ageInTicks * 0.05f) * 0.03f;
        leftShoe.yRot = sway;
        rightShoe.yRot = -sway;
        leftShoe.zRot = sway * 0.5f;
        rightShoe.zRot = -sway * 0.5f;
    }
}
