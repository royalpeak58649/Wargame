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

/** 벽을 향해 서서 흐느적거리는, 머리카락이 바닥까지 늘어진 여자. */
public class LadyFacingTheWallModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("lady_facing_the_wall"), "main");

    private final ModelPart head;

    public LadyFacingTheWallModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3, 0, -1.5f, 6, 11, 3), PartPose.offset(0, 11, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-3, -7, -3, 6, 7, 6), PartPose.offset(0, 11, 0));
        // 뒤로 늘어진 긴 머리 (+z 쪽)
        head.addOrReplaceChild("hair_back", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-4, -7, 3, 8, 22, 2), PartPose.ZERO);
        // 양옆으로 흘러내리는 머리
        head.addOrReplaceChild("hair_right", CubeListBuilder.create().texOffs(20, 32)
                .addBox(-4, -7, -3, 1, 16, 7), PartPose.ZERO);
        head.addOrReplaceChild("hair_left", CubeListBuilder.create().texOffs(20, 32)
                .addBox(3, -7, -3, 1, 16, 7), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 32)
                .addBox(-1, 0, -1, 2, 11, 2), PartPose.offset(-4, 11, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(36, 32)
                .addBox(-1, 0, -1, 2, 11, 2), PartPose.offset(4, 11, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(44, 32)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(-1.5f, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(44, 32)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(1.5f, 12, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        head.xRot = 0.25f; // 벽에 이마를 대고 있다
        head.zRot = Mth.sin(t * 0.9f * 0.01f) * 0.03f; // 아주 작은 떨림
    }
}
